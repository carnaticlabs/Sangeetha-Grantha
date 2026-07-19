#!/usr/bin/env python
"""Re-split lyric variants to match their template, via the audit-logged API.

Only acts on krithis the triage tool classifies ``resplit-to-template`` — i.e.
the CURRENT extraction pipeline, run on the live source, reproduces the canonical
template section-for-section. For each non-template variant it writes the freshly
parsed sections through POST /v1/admin/variants/{id}/sections (audit action
UPDATE_LYRIC_VARIANT_SECTIONS). It NEVER modifies the en/Latin template variant,
never touches the DB directly, and never fabricates lyric.

Safety gates (all must hold, per variant, or the variant is skipped):
  * krithi classification == resplit-to-template
  * parsed section count == template section count
  * parsed section TYPES == template section types, in order
  * language != en  (template variant is left untouched)

Usage:
    python scripts/section_repair.py --krithi <uuid> [--krithi <uuid> ...]         # dry-run
    python scripts/section_repair.py --from-report .triage-cache/triage_report.json  # all resplit candidates
    python scripts/section_repair.py ... --apply
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
from pathlib import Path
from typing import Any

import httpx
import psycopg
from psycopg.rows import dict_row

# Backend enforces a GLOBAL 100 req/min limit (all endpoints, per host). Throttle
# below it and retry on 429 so a run never stops half-way through a krithi.
_THROTTLE_S = 1.05
_last_request_at = 0.0


def _request(method: str, url: str, **kwargs) -> httpx.Response:
    global _last_request_at
    for attempt in range(6):
        wait = _THROTTLE_S - (time.monotonic() - _last_request_at)
        if wait > 0:
            time.sleep(wait)
        resp = httpx.request(method, url, timeout=30, **kwargs)
        _last_request_at = time.monotonic()
        if resp.status_code != 429:
            resp.raise_for_status()
            return resp
        retry_after = int(resp.headers.get("Retry-After", "61") or "61")
        print(f"    …429 rate-limited, waiting {retry_after}s (attempt {attempt + 1}/6)")
        time.sleep(retry_after + 1)
    resp.raise_for_status()
    return resp


_WORKER_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(_WORKER_ROOT))
sys.path.insert(0, str(Path(__file__).resolve().parent))

from section_triage import DB_URL, triage  # noqa: E402

from src.html_extractor import HtmlTextExtractor  # noqa: E402
from src.structure_parser import StructureParser  # noqa: E402

API = os.environ.get("SANGITA_API", "http://localhost:8080")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "dev-admin-token")
ADMIN_EMAIL = os.environ.get("ADMIN_EMAIL", "admin@sangitagrantha.org")


def _template_sections(conn, krithi_id: str) -> list[tuple[int, str, str]]:
    """(order_index, section_type, section_id) ordered."""
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            "SELECT order_index, section_type::text AS t, id::text AS sid "
            "FROM krithi_sections WHERE krithi_id=%s ORDER BY order_index",
            (krithi_id,),
        )
        return [(r["order_index"], r["t"], r["sid"]) for r in cur.fetchall()]


def _variant_ids(conn, krithi_id: str) -> dict[str, str]:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            "SELECT language, id::text AS vid FROM krithi_lyric_variants WHERE krithi_id=%s",
            (krithi_id,),
        )
        return {r["language"]: r["vid"] for r in cur.fetchall()}


def _stored_blob(conn, variant_id: str) -> str:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            "SELECT string_agg(ls.text, chr(10) ORDER BY s.order_index) AS blob "
            "FROM krithi_lyric_sections ls JOIN krithi_sections s ON s.id=ls.section_id "
            "WHERE ls.lyric_variant_id=%s",
            (variant_id,),
        )
        row = cur.fetchone()
        return row["blob"] if row and row["blob"] else ""


# Marker tokens the parser strips into headers (never lyric): P/A/C abbreviations
# and the full-word "svara sahitya" label, any script. Removed before the
# content-preservation comparison so stripping a label is not counted as loss.
_MARKER_RE = re.compile(
    r"स्वर\s*साहित्य|ஸ்வர\s*ஸாஹித்ய|స్వర\s*సాహిత్య|ಸ್ವರ\s*ಸಾಹಿತ್ಯ|സ്വര\s*സാഹിത്യ|"
    r"svara\s*sahitya|swarasahitya|"
    r"[पअचపఅచಪಅಚപഅചபஅசPACpac]\d*[.:)\-]"
)


def _content_preserved(old_blob: str, new_text: str, threshold: float = 0.97) -> tuple[bool, float]:
    """True if the re-split keeps ~all of the stored blob's lyric characters.

    Guards against the canonical mapping silently DROPPING a parsed section whose
    type has no slot in an (undercounting) template — e.g. a pallavi that the
    template omits. Markers are stripped from both sides first.
    """

    def strip(s: str) -> str:
        return re.sub(r"\s+", "", _MARKER_RE.sub("", s))

    old = strip(old_blob)
    new = strip(new_text)
    if not old:
        return True, 1.0
    ratio = len(new) / len(old)
    return ratio >= threshold, ratio


def _token() -> str:
    r = _request(
        "POST",
        f"{API}/v1/auth/token",
        json={
            "adminToken": ADMIN_TOKEN,
            "email": ADMIN_EMAIL,
            "roles": ["ADMIN", "CURATOR"],
        },
    )
    return r.json()["token"]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--krithi", action="append", default=[])
    ap.add_argument("--from-report", help="triage JSON; repairs every resplit-to-template entry")
    ap.add_argument("--apply", action="store_true", help="write via API (default: dry-run)")
    args = ap.parse_args()

    ids = list(args.krithi)
    if args.from_report:
        rep = json.loads(Path(args.from_report).read_text("utf-8"))
        ids += [r["krithi_id"] for r in rep if r["classification"] == "resplit-to-template"]
    ids = list(dict.fromkeys(ids))
    if not ids:
        ap.error("provide --krithi or --from-report")

    extractor, parser = HtmlTextExtractor(), StructureParser()
    token = _token() if args.apply else None
    written = skipped = 0

    with psycopg.connect(DB_URL) as conn:
        for kid in ids:
            res = triage(conn, kid, extractor, parser)
            if res.classification != "resplit-to-template":
                print(f"SKIP {res.title} [{kid}] — {res.classification}: {res.note}")
                continue
            template = _template_sections(conn, kid)
            tmpl_types = [t for _, t, _ in template]
            sid_by_order = {oi: sid for oi, _, sid in template}
            variant_ids = _variant_ids(conn, kid)
            # Baseline: the en/Latin template's last section is clean lyric (the
            # English word-by-word meaning lives in a separate, boundary-excluded
            # block). Indic transliterations expand it ~1.3-1.6x; a variant whose
            # last section is far larger carries an inline translation trailer
            # (thyagaraja-vaibhavam Tamil) and must NOT be auto-split.
            en_variant = next(iter(_reparse(res, "en")), None)
            en_last_len = len(en_variant.sections[-1].text) if en_variant and en_variant.sections else 0
            print(f"\n{res.title} [{kid}] template={tmpl_types}")

            for lang, vp in res.parsed.items():
                if lang == "en":
                    continue  # never modify the template variant
                if res.stored_counts.get(lang) == len(template):
                    continue  # already matches template — leave it alone (minimal writes)
                if vp.n_sections != len(template) or vp.types != tmpl_types:
                    print(f"  {lang}: SKIP (parsed {vp.types} != template)")
                    skipped += 1
                    continue
                variant = next((v for v in _reparse(res, lang)), None)
                sections = variant.sections if variant else []
                if len(sections) != len(template):
                    print(f"  {lang}: SKIP (section objects mismatch)")
                    skipped += 1
                    continue
                vid = variant_ids.get(lang)
                if not vid:
                    print(f"  {lang}: SKIP (no variant id)")
                    skipped += 1
                    continue
                stored_blob = _stored_blob(conn, vid)
                # Drop-detection gate for UNSPLIT blobs (markers intact): if the
                # blob's own inline markers yield MORE sections than the template,
                # canonical mapping would silently discard the surplus (e.g. a
                # pallavi the Latin template omits). Fewer markers is fine — that's
                # a truncated blob the source re-extraction refills (content gate
                # then confirms nothing is lost).
                if res.stored_counts.get(lang) == 1:
                    blob_n = len(StructureParser().parse(stored_blob).sections)
                    if blob_n > len(template):
                        print(
                            f"  {lang}: SKIP (blob has {blob_n} markers > "
                            f"template {len(template)} — would drop a section)"
                        )
                        skipped += 1
                        continue
                # Trailer guard: a last section far larger than the en baseline is
                # an inline translation trailer, not lyric — defer for review.
                if en_last_len and len(sections[-1].text) > 2.0 * en_last_len:
                    ratio_l = len(sections[-1].text) / en_last_len
                    print(f"  {lang}: SKIP (last section {ratio_l:.1f}x en — likely translation trailer)")
                    skipped += 1
                    continue
                # Content-preservation gate: never drop lyric to fit a template.
                new_text = "\n".join(s.text for s in sections)
                ok, ratio = _content_preserved(stored_blob, new_text)
                if not ok:
                    print(f"  {lang}: SKIP (content loss — re-split keeps {ratio:.0%} of stored blob)")
                    skipped += 1
                    continue
                payload = {
                    "sections": [{"sectionId": sid_by_order[i + 1], "text": s.text} for i, s in enumerate(sections)]
                }
                if args.apply:
                    resp = _request(
                        "POST",
                        f"{API}/v1/admin/variants/{vid}/sections",
                        headers={"Authorization": f"Bearer {token}"},
                        json=payload,
                    )
                    print(f"  {lang}: {len(sections)} sections -> HTTP {resp.status_code}")
                else:
                    print(f"  {lang}: {len(sections)} sections -> DRY-RUN")
                written += 1

    print(f"\n{'APPLIED' if args.apply else 'DRY-RUN'}: {written} variant writes, {skipped} skipped")
    return 0


# triage() already parsed the source; re-run the parser once more to hand back the
# variant objects (with section text) rather than only the summary in TriageResult.
_CACHE: dict[Any, Any] = {}


def _reparse(res, lang):
    from section_triage import _fetch  # noqa: E402

    key = res.source_url
    if key not in _CACHE:
        from src.diacritic_normalizer import normalize_garbled_diacritics

        html = _fetch(res.source_url)
        parsed = StructureParser().parse(
            normalize_garbled_diacritics(HtmlTextExtractor().extract(html, base_url=res.source_url).text)
        )
        _CACHE[key] = parsed
    for v in _CACHE[key].lyric_variants:
        if v.language == lang:
            yield v


if __name__ == "__main__":
    raise SystemExit(main())
