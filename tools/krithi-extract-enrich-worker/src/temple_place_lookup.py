"""TRACK-154: one-time Google place-id lookup for temples, triage, and seed generation.

Three stages, only the first touches the network:

* ``lookup``  -- Places API (New) Text Search per target, answers kept in a LOCAL working file.
* ``triage``  -- applies the auto-confirm rule, writes a local review file and a coverage report.
* ``build``   -- turns the curator-edited review file into the committed JSON (place ids only, no
  Google names/addresses/coordinates) and regenerates ``R__seed_12_temple_place_review.sql``.

Google terms posture (Spec flagged concern 1): the only Google value that is ever committed is the
``place_id``. Names and addresses live in the local candidate/review files and are deleted after the
review. No location or geometry field is ever requested. The API key comes from the environment,
travels in a request header only, and is redacted from every log line, error and file.
"""

from __future__ import annotations

import json
import logging
import os
import re
import subprocess
from datetime import UTC, datetime
from pathlib import Path
from typing import Literal

import httpx
from pydantic import BaseModel, ConfigDict, Field, field_validator

from src.normalizer import normalize_for_matching

logger = logging.getLogger(__name__)

API_KEY_ENV = "GOOGLE_PLACES_API_KEY"
MAX_CALLS_ENV = "PLACES_MAX_CALLS"
DEFAULT_MAX_CALLS = 150
SEARCH_URL = "https://places.googleapis.com/v1/places:searchText"
PAGE_SIZE = 5
# Minimal mask: identity + the two strings needed to review a match. Nothing geometric.
FIELD_MASK = "places.id,places.displayName,places.formattedAddress"
_FORBIDDEN_MASK_TOKENS = ("location", "viewport", "geometry", "plusCode", "rating", "photos", "googleMapsUri")
SUB_SHRINE_KINDS = frozenset({"SANNIDHI", "MANDAPAM"})
AUTO_ACTOR = "system:TRACK-154:auto"
SEED_ACTOR = "system:TRACK-154"
SEED_FILE_NAME = "R__seed_12_temple_place_review.sql"
ReviewState = Literal["CONFIRMED", "LOCALITY_ONLY", "UNRESOLVED"]
_DOLLAR_TAG = "$track154$"


# ─── Models ───────────────────────────────────────────────────────────────────────────────────


class _Model(BaseModel):
    # Google may add fields over time; ignoring unknown keys also guarantees a stray
    # `location` in a response can never reach a file.
    model_config = ConfigDict(extra="ignore")


class TempleTarget(_Model):
    name: str
    name_normalized: str
    city: str | None = None
    state: str | None = None
    country: str | None = None
    place_kind: str = "COMPLEX"
    aliases: list[str] = Field(default_factory=list)

    @property
    def key(self) -> str:
        return natural_key(self.name_normalized, self.city, self.state, self.country)

    @property
    def is_lookup_target(self) -> bool:
        return self.place_kind not in SUB_SHRINE_KINDS


class Candidate(_Model):
    place_id: str
    display_name: str = ""
    formatted_address: str = ""
    rank: int


class QueryRecord(_Model):
    key: str
    pass_no: int
    query: str
    status: Literal["ok", "error"]
    candidates: list[Candidate] = Field(default_factory=list)
    error: str | None = None
    at: str


class CandidateFile(_Model):
    version: int = 1
    calls_made: int = 0
    records: list[QueryRecord] = Field(default_factory=list)

    def answered(self, key: str, pass_no: int) -> bool:
        return any(r.key == key and r.pass_no == pass_no and r.status == "ok" for r in self.records)

    def get(self, key: str, pass_no: int) -> QueryRecord | None:
        found = [r for r in self.records if r.key == key and r.pass_no == pass_no and r.status == "ok"]
        return found[-1] if found else None


class ReviewEntry(_Model):
    name: str
    name_normalized: str
    city: str | None = None
    state: str | None = None
    country: str | None = None
    place_kind: str = "COMPLEX"
    triage: str
    risk: int
    candidates: list[Candidate] = Field(default_factory=list)
    retry_candidates: list[Candidate] = Field(default_factory=list)
    decision: ReviewState | None = None
    google_place_id: str | None = None
    reviewed_by: str | None = None
    reviewed_at: str | None = None
    note: str = ""


class Signoff(_Model):
    reviewer: str = ""
    date: str = ""
    statement: str = ""


class ReviewFile(_Model):
    version: int = 1
    signoff: Signoff = Field(default_factory=Signoff)
    entries: list[ReviewEntry] = Field(default_factory=list)


class PlacedEntry(_Model):
    """One committed placement. Carries the place id and approval only; no Google content."""

    name: str
    name_normalized: str
    city: str | None = None
    state: str | None = None
    country: str | None = None
    review_state: ReviewState
    google_place_id: str | None = None
    reviewed_by: str
    reviewed_at: str
    note: str = ""

    @field_validator("reviewed_at")
    @classmethod
    def _utc_midnight_for_dates(cls, value: str) -> str:
        # A bare date would be read in each session's time zone and look "changed" on rerun.
        return f"{value}T00:00:00Z" if re.fullmatch(r"\d{4}-\d{2}-\d{2}", value) else value


class Approval(_Model):
    reviewer: str
    date: str
    scope: str


class PlaceReviewData(_Model):
    track: str = "TRACK-154"
    version: int = 1
    approvals: list[Approval]
    entries: list[PlacedEntry]


class CoverageReport(_Model):
    total: int
    counts: dict[str, int]


def natural_key(name_normalized: str, city: str | None, state: str | None, country: str | None) -> str:
    return "|".join(part if part is not None else "" for part in (name_normalized, city, state, country))


# ─── Redaction and the Places client ──────────────────────────────────────────────────────────


def redact(text: str, api_key: str | None) -> str:
    """Remove the API key (raw and URL-encoded) from ``text``."""
    if not api_key:
        return text
    return text.replace(api_key, "[REDACTED]")


class CallCeilingReached(RuntimeError):
    """Raised before a call that would exceed the ceiling; the run stops cleanly."""


class PlacesError(RuntimeError):
    """A failed Places call. The message is already redacted."""

    def __init__(self, message: str, status_code: int | None = None) -> None:
        super().__init__(message)
        self.status_code = status_code

    @property
    def is_fatal(self) -> bool:
        """Auth/permission failures repeat for every target, so the run aborts."""
        return self.status_code in (401, 403)


def max_calls_from_env(env: dict[str, str] | None = None) -> int:
    raw = (env if env is not None else os.environ).get(MAX_CALLS_ENV, "").strip()
    return int(raw) if raw else DEFAULT_MAX_CALLS


class PlacesClient:
    """Places API (New) Text Search with a hard call ceiling and key redaction."""

    def __init__(
        self,
        api_key: str,
        max_calls: int,
        http: httpx.Client | None = None,
        calls_already_made: int = 0,
    ) -> None:
        if not api_key:
            raise ValueError(f"{API_KEY_ENV} is not set")
        if any(tok.lower() in FIELD_MASK.lower() for tok in _FORBIDDEN_MASK_TOKENS):
            raise ValueError("field mask must not request location or geometry fields")
        self._api_key = api_key
        self._http = http or httpx.Client(timeout=30.0)
        self.max_calls = max_calls
        self.calls_made = calls_already_made

    def __repr__(self) -> str:
        return f"PlacesClient(calls_made={self.calls_made}, max_calls={self.max_calls})"

    def search(self, query: str) -> list[Candidate]:
        if self.calls_made >= self.max_calls:
            raise CallCeilingReached(f"call ceiling {self.max_calls} reached ({self.calls_made} made)")
        self.calls_made += 1  # counted before the request: a failed call may still be billed
        try:
            response = self._http.post(
                SEARCH_URL,
                headers={"X-Goog-Api-Key": self._api_key, "X-Goog-FieldMask": FIELD_MASK},
                json={"textQuery": query, "pageSize": PAGE_SIZE},
            )
        except httpx.HTTPError as exc:
            raise PlacesError(redact(f"{type(exc).__name__}: {exc}", self._api_key)) from None
        if response.status_code != 200:
            body = redact(response.text[:300], self._api_key)
            raise PlacesError(f"HTTP {response.status_code}: {body}", response.status_code)
        try:
            payload = response.json()
        except ValueError:
            raise PlacesError("response was not JSON") from None
        places = payload.get("places") or []
        return [
            Candidate(
                place_id=str(p["id"]),
                display_name=(p.get("displayName") or {}).get("text", ""),
                formatted_address=p.get("formattedAddress", ""),
                rank=i + 1,
            )
            for i, p in enumerate(places[:PAGE_SIZE])
            if p.get("id")
        ]


# ─── Local working files ──────────────────────────────────────────────────────────────────────


def assert_untracked_location(path: Path) -> None:
    """Refuse to write Google-sourced files anywhere git would pick them up."""
    resolved = path.resolve()
    probe = resolved.parent
    probe.mkdir(parents=True, exist_ok=True)
    top = subprocess.run(
        ["git", "-C", str(probe), "rev-parse", "--show-toplevel"], capture_output=True, text=True, check=False
    )
    if top.returncode != 0:
        return  # not inside a repository
    ignored = subprocess.run(
        ["git", "-C", top.stdout.strip(), "check-ignore", "-q", str(resolved)], check=False
    ).returncode
    if ignored != 0:
        raise ValueError(f"{path} is inside a git repository and not ignored; use a path under storage/")


def _atomic_write(path: Path, text: str) -> None:
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(text, encoding="utf-8")
    tmp.chmod(0o600)
    os.replace(tmp, path)


def load_candidates(path: Path) -> CandidateFile:
    return CandidateFile.model_validate_json(path.read_text("utf-8")) if path.exists() else CandidateFile()


def save_candidates(path: Path, data: CandidateFile) -> None:
    _atomic_write(path, data.model_dump_json(indent=2))


def load_targets(path: Path) -> list[TempleTarget]:
    return [TempleTarget.model_validate(item) for item in json.loads(path.read_text("utf-8"))]


# ─── Stage 1: lookup ──────────────────────────────────────────────────────────────────────────


def build_query(target: TempleTarget, pass_no: int) -> str:
    """Pass 1: catalogue name, city, state. Pass 2: first alias with locality, else the bare name."""
    if pass_no == 1 or not target.aliases:
        if pass_no == 1:
            return ", ".join(p for p in (target.name, target.city, target.state) if p)
        return target.name
    return ", ".join(p for p in (target.aliases[0], target.city, target.state) if p)


class RunSummary(BaseModel):
    calls_this_run: int
    calls_total: int
    answered: int
    skipped_resumed: int
    failed: list[str]
    stopped_reason: str | None = None


class LookupRunner:
    def __init__(self, client: PlacesClient, store: Path) -> None:
        self._client = client
        self._store = store
        self._data = load_candidates(store)
        client.calls_made = max(client.calls_made, self._data.calls_made)

    @property
    def data(self) -> CandidateFile:
        return self._data

    def run(self, targets: list[TempleTarget], pass_no: int = 1) -> RunSummary:
        started = self._client.calls_made
        answered = skipped = 0
        failed: list[str] = []
        stopped: str | None = None
        selected = [t for t in targets if t.is_lookup_target]
        if pass_no == 2:
            pending = {e.key for e in self._pass2_keys(selected)}
            selected = [t for t in selected if t.key in pending]
        for target in selected:
            if self._data.answered(target.key, pass_no):
                skipped += 1
                continue
            query = build_query(target, pass_no)
            try:
                record = self._attempt(target, pass_no, query)
            except CallCeilingReached as exc:
                stopped = str(exc)
                break
            if record.status == "ok":
                answered += 1
            else:
                failed.append(target.key)
            if record.status == "error" and record.error and record.error.startswith("FATAL"):
                stopped = record.error
                break
        self._data.calls_made = self._client.calls_made
        save_candidates(self._store, self._data)
        summary = RunSummary(
            calls_this_run=self._client.calls_made - started,
            calls_total=self._client.calls_made,
            answered=answered,
            skipped_resumed=skipped,
            failed=failed,
            stopped_reason=stopped,
        )
        logger.info(
            "lookup pass %d done: %d calls this run, %d total, %d answered, %d resumed, %d failed",
            pass_no,
            summary.calls_this_run,
            summary.calls_total,
            answered,
            skipped,
            len(failed),
        )
        return summary

    def _pass2_keys(self, targets: list[TempleTarget]) -> list[TempleTarget]:
        """Retry only targets pass 1 could not auto-confirm."""
        return [t for t in targets if classify(t, self._data.get(t.key, 1)).triage != "AUTO_CONFIRM"]

    def _attempt(self, target: TempleTarget, pass_no: int, query: str) -> QueryRecord:
        now = datetime.now(UTC).isoformat(timespec="seconds")
        try:
            candidates = self._client.search(query)
            record = QueryRecord(
                key=target.key, pass_no=pass_no, query=query, status="ok", candidates=candidates, at=now
            )
            logger.info("lookup ok: %r -> %d candidate(s)", query, len(candidates))
        except CallCeilingReached:
            raise
        except PlacesError as exc:
            message = ("FATAL " if exc.is_fatal else "") + str(exc)
            record = QueryRecord(key=target.key, pass_no=pass_no, query=query, status="error", error=message, at=now)
            logger.warning("lookup failed: %r: %s", query, exc)
        self._data.records.append(record)
        self._data.calls_made = self._client.calls_made
        save_candidates(self._store, self._data)  # resume point after every call
        return record


# ─── Stage 2: triage ──────────────────────────────────────────────────────────────────────────


class Classification(BaseModel):
    triage: str
    risk: int
    place_id: str | None = None


def _tokens(text: str) -> str:
    return " " + normalize_for_matching(text, "temple") + " "


def name_matches(target: TempleTarget, candidate: Candidate) -> bool:
    return bool(candidate.display_name) and (
        normalize_for_matching(candidate.display_name, "temple") == normalize_for_matching(target.name, "temple")
    )


def locality_matches(target: TempleTarget, candidate: Candidate) -> bool:
    """The catalogue city appears, as whole words, in Google's formatted address."""
    if not target.city:
        return False
    return _tokens(target.city) in _tokens(candidate.formatted_address)


def classify(target: TempleTarget, record: QueryRecord | None) -> Classification:
    """Auto-confirm only for exactly one candidate whose name and locality both match."""
    if record is None:
        return Classification(triage="NOT_LOOKED_UP", risk=4)
    candidates = record.candidates
    if not candidates:
        return Classification(triage="NO_CANDIDATES", risk=3)
    if len(candidates) == 1:
        only = candidates[0]
        if name_matches(target, only) and locality_matches(target, only):
            return Classification(triage="AUTO_CONFIRM", risk=5, place_id=only.place_id)
        return Classification(triage="SINGLE_MISMATCH", risk=1)
    plausible = [c for c in candidates if name_matches(target, c)]
    if len(plausible) >= 2:
        return Classification(triage="AMBIGUOUS_MATCHES", risk=0)
    return Classification(triage="MULTIPLE_CANDIDATES", risk=2)


def triage(targets: list[TempleTarget], data: CandidateFile, now: datetime | None = None) -> ReviewFile:
    """Build the review file; auto-confirmations are pre-filled, everything else awaits a curator."""
    stamp = (now or datetime.now(UTC)).date().isoformat()
    entries: list[ReviewEntry] = []
    for t in (t for t in targets if t.is_lookup_target):
        first = data.get(t.key, 1)
        retry = data.get(t.key, 2)
        c = classify(t, first)
        entry = ReviewEntry(
            name=t.name,
            name_normalized=t.name_normalized,
            city=t.city,
            state=t.state,
            country=t.country,
            place_kind=t.place_kind,
            triage=c.triage,
            risk=c.risk,
            candidates=first.candidates if first else [],
            retry_candidates=retry.candidates if retry else [],
        )
        if c.triage == "AUTO_CONFIRM":
            entry.decision = "CONFIRMED"
            entry.google_place_id = c.place_id
            entry.reviewed_by = AUTO_ACTOR
            entry.reviewed_at = stamp
        entries.append(entry)
    _demote_duplicate_auto_confirmations(entries)
    entries.sort(key=lambda e: (e.risk, e.name_normalized))
    return ReviewFile(entries=entries)


def _demote_duplicate_auto_confirmations(entries: list[ReviewEntry]) -> None:
    by_place: dict[str, list[ReviewEntry]] = {}
    for e in entries:
        if e.decision == "CONFIRMED" and e.google_place_id:
            by_place.setdefault(e.google_place_id, []).append(e)
    for group in by_place.values():
        if len(group) > 1:
            for e in group:
                e.decision = None
                e.google_place_id = None
                e.reviewed_by = None
                e.reviewed_at = None
                e.triage = "DUPLICATE_PLACE_ID"
                e.risk = 0


def auto_confirmed_lines(review: ReviewFile) -> list[str]:
    """One line per auto-confirmation with Google's name and address, for the curator to eyeball.

    Name and town matching cannot tell two places in one town apart, so the sign-off must cover these.
    """
    lines = []
    for e in review.entries:
        if e.reviewed_by == AUTO_ACTOR and e.candidates:
            c = e.candidates[0]
            lines.append(f"{e.name} ({e.city}, {e.state}) -> {c.display_name} | {c.formatted_address} | {c.place_id}")
    return lines


def coverage_report(review: ReviewFile, all_targets: int | None = None) -> CoverageReport:
    counts = {"CONFIRMED": 0, "LOCALITY_ONLY": 0, "UNRESOLVED": 0, "PENDING_REVIEW": 0}
    for e in review.entries:
        counts[e.decision or "PENDING_REVIEW"] += 1
    return CoverageReport(total=all_targets if all_targets is not None else len(review.entries), counts=counts)


# ─── Stage 3: build ───────────────────────────────────────────────────────────────────────────


class BuildError(ValueError):
    pass


def build_place_data(review: ReviewFile) -> PlaceReviewData:
    """Validate a curator-edited review file and strip it to what may be committed."""
    problems: list[str] = []
    signoff = review.signoff
    if not (signoff.reviewer.strip() and signoff.date.strip()):
        problems.append("signoff.reviewer and signoff.date are required")
    entries: list[PlacedEntry] = []
    seen_places: dict[str, str] = {}
    for e in review.entries:
        label = e.name_normalized
        if e.place_kind in SUB_SHRINE_KINDS:
            problems.append(f"{label}: sub-shrines inherit their parent's place and cannot be reviewed")
            continue
        if e.decision is None:
            problems.append(f"{label}: no decision")
            continue
        if e.decision == "CONFIRMED" and not e.google_place_id:
            problems.append(f"{label}: CONFIRMED needs google_place_id")
        if e.decision == "UNRESOLVED" and e.google_place_id:
            problems.append(f"{label}: UNRESOLVED must not carry a google_place_id")
        if e.decision == "CONFIRMED" and e.google_place_id:
            if e.google_place_id in seen_places:
                problems.append(f"{label}: place id already confirmed for {seen_places[e.google_place_id]}")
            seen_places[e.google_place_id] = label
        reviewed_by = e.reviewed_by or signoff.reviewer
        reviewed_at = e.reviewed_at or signoff.date
        if not (reviewed_by and reviewed_at):
            problems.append(f"{label}: missing approver or date")
            continue
        entries.append(
            PlacedEntry(
                name=e.name,
                name_normalized=e.name_normalized,
                city=e.city,
                state=e.state,
                country=e.country,
                review_state=e.decision,
                google_place_id=e.google_place_id,
                reviewed_by=reviewed_by,
                reviewed_at=reviewed_at,
                note=e.note,
            )
        )
    if problems:
        raise BuildError("; ".join(problems))
    entries.sort(key=lambda p: natural_key(p.name_normalized, p.city, p.state, p.country))
    approval = Approval(
        reviewer=signoff.reviewer,
        date=signoff.date,
        scope=signoff.statement or "Reviewed temple place placements",
    )
    return PlaceReviewData(approvals=[approval], entries=entries)


def canonical_json(data: PlaceReviewData) -> str:
    return json.dumps(data.model_dump(mode="json"), indent=2, ensure_ascii=False, sort_keys=True) + "\n"


_SEED_TEMPLATE = """-- Repeatable seed for TRACK-154: reviewed Google place ids for temples.
-- GENERATED by tools/krithi-extract-enrich-worker/scripts/temple_place_lookup.py build.
-- Do not edit by hand: edit database/data/track154-temple-place-review.json and regenerate.
-- Only the place_id is stored (no Google name, address or coordinate). latitude/longitude are
-- never written here, so hand-entered coordinates survive every rerun.
-- Sub-shrines (SANNIDHI/MANDAPAM) inherit their parent's place and must not appear as placed rows.
-- Ref: application_documentation/04-database/schema.md

DO $$
DECLARE
    v_data CONSTANT jsonb := __PAYLOAD__::jsonb;
    v_actor_meta CONSTANT jsonb := jsonb_build_object(
        'actor', '__ACTOR__',
        'migration', '__FILE__',
        'provenance_owner', 'TRACK-154'
    );
    rec RECORD;
    t RECORD;
    v_new_by text;
    v_last_after jsonb;
    v_current jsonb;
    v_before jsonb;
    v_after jsonb;
    v_applied int := 0;
    v_skipped_absent int := 0;
    v_protected int := 0;
BEGIN
    FOR rec IN
        SELECT * FROM jsonb_to_recordset(v_data -> 'entries') AS x(
            name_normalized text, city text, state text, country text,
            review_state text, google_place_id text,
            reviewed_by text, reviewed_at timestamptz
        )
    LOOP
        SELECT * INTO t FROM temples
         WHERE name_normalized = rec.name_normalized
           AND city IS NOT DISTINCT FROM rec.city
           AND state IS NOT DISTINCT FROM rec.state
           AND country IS NOT DISTINCT FROM rec.country;

        IF NOT FOUND THEN
            RAISE NOTICE 'TRACK-154: temple % (%, %) not found; skipped',
                rec.name_normalized, rec.city, rec.state;
            v_skipped_absent := v_skipped_absent + 1;
            CONTINUE;
        END IF;

        IF t.place_kind IN ('SANNIDHI', 'MANDAPAM') THEN
            RAISE EXCEPTION 'TRACK-154: sub-shrine % must not carry its own place review', rec.name_normalized;
        END IF;

        v_before := jsonb_build_object(
            'google_place_id', t.google_place_id,
            'place_review_state', t.place_review_state,
            'place_reviewed_by', t.place_reviewed_by,
            'place_reviewed_at', to_char(t.place_reviewed_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
        );
        v_after := jsonb_build_object(
            'google_place_id', rec.google_place_id,
            'place_review_state', rec.review_state,
            'place_reviewed_by', rec.reviewed_by,
            'place_reviewed_at', to_char(rec.reviewed_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
        );

        IF v_before = v_after THEN
            CONTINUE; -- already applied: no write, no audit row
        END IF;

        -- Overwrite only unreviewed rows, or rows still exactly as this seed last wrote them.
        IF t.place_review_state IS NOT NULL THEN
            SELECT a.diff -> 'after' INTO v_last_after
              FROM audit_log a
             WHERE a.entity_table = 'temples' AND a.entity_id = t.id
               AND a.action = 'SET_TEMPLE_PLACE_REVIEW'
               AND a.metadata ->> 'actor' = '__ACTOR__'
             ORDER BY a.changed_at DESC, a.id DESC
             LIMIT 1;
            IF v_last_after IS DISTINCT FROM v_before THEN
                RAISE NOTICE 'TRACK-154: temple % has a place review set by % outside this seed; left unchanged',
                    rec.name_normalized, t.place_reviewed_by;
                v_protected := v_protected + 1;
                CONTINUE;
            END IF;
        END IF;

        UPDATE temples
           SET google_place_id = rec.google_place_id,
               place_review_state = rec.review_state::place_review_state_enum,
               place_reviewed_by = rec.reviewed_by,
               place_reviewed_at = rec.reviewed_at,
               updated_at = NOW()
         WHERE id = t.id;

        INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
        VALUES (
            gen_random_uuid(), 'temples', t.id, NULL, 'SET_TEMPLE_PLACE_REVIEW',
            jsonb_build_object('before', v_before, 'after', v_after),
            v_actor_meta
        );
        v_applied := v_applied + 1;
    END LOOP;

    RAISE NOTICE 'TRACK-154 place review: % applied, % absent, % protected', v_applied, v_skipped_absent, v_protected;
END
$$;
"""


def render_seed_sql(data: PlaceReviewData) -> str:
    """Embed the reviewed JSON as a dollar-quoted JSONB literal in the repeatable seed."""
    payload = canonical_json(data)
    if _DOLLAR_TAG in payload or "${" in payload:
        raise BuildError("payload contains a dollar-quote tag or a Flyway placeholder")
    literal = f"{_DOLLAR_TAG}{payload}{_DOLLAR_TAG}"
    return (
        _SEED_TEMPLATE.replace("__PAYLOAD__", literal)
        .replace("__ACTOR__", SEED_ACTOR)
        .replace("__FILE__", SEED_FILE_NAME)
    )


_LITERAL_RE = re.compile(re.escape(_DOLLAR_TAG) + r"(.*?)" + re.escape(_DOLLAR_TAG), re.DOTALL)


def extract_embedded_json(seed_sql: str) -> str:
    match = _LITERAL_RE.search(seed_sql)
    if not match:
        raise BuildError("no embedded JSON literal found in seed SQL")
    return match.group(1)


def embedded_matches_json(seed_sql: str, json_text: str) -> bool:
    """Drift check: the SQL's embedded literal is exactly the committed JSON, canonicalised."""
    return bool(json.loads(extract_embedded_json(seed_sql)) == json.loads(json_text))


# ─── CLI helpers ──────────────────────────────────────────────────────────────────────────────


def export_targets(conn: object) -> list[TempleTarget]:
    """Read-only export of temples (plus aliases) from a psycopg connection."""
    rows = conn.execute(  # type: ignore[attr-defined]
        "SELECT t.name, t.name_normalized, t.city, t.state, t.country, t.place_kind::text, "
        "COALESCE(array_agg(n.name ORDER BY n.is_primary DESC, n.name) FILTER (WHERE n.name IS NOT NULL "
        "AND n.name <> t.name), '{}') "
        "FROM temples t LEFT JOIN temple_names n ON n.temple_id = t.id "
        "GROUP BY t.id ORDER BY t.name_normalized"
    ).fetchall()
    return [
        TempleTarget(
            name=r[0], name_normalized=r[1], city=r[2], state=r[3], country=r[4], place_kind=r[5], aliases=list(r[6])
        )
        for r in rows
    ]
