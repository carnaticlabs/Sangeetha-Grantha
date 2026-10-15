"""Shared evidence contract and deterministic corpus concordance (TRACK-146).

Offline only: validation never approves claims or mutates the canonical catalogue.
"""

from __future__ import annotations

import csv
import hashlib
import json
from collections import Counter
from pathlib import Path
from typing import Literal, Self, TypedDict
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator

ReviewState = Literal["unreviewed", "reviewed", "unresolved", "disputed", "rejected"]
EvidenceKind = Literal["textual_evidence", "scholarly_interpretation", "documented_tradition", "automated_suggestion"]
GeographyState = Literal["UNREVIEWED", "SPECIFIC_SHRINE_SUPPORTED", "NON_SPECIFIC_ADDRESS", "UNRESOLVED", "DISPUTED"]
Form = Literal["KRITHI", "VARNAM", "SWARAJATHI", "UNESTABLISHED"]

# Assigned once and never derived from catalogue UUIDs, row numbers or titles.
MASTER_ID_PATTERN = r"^[A-Z]{2,4}-\d{4}$"
# Geography sentinels; any other geography value names a candidate place.
NO_PLACE_STATED = "NO_PLACE_STATED"
NON_SPECIFIC_ADDRESS = "NON_SPECIFIC_ADDRESS"


class Record(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Approval(Record):
    reviewer: str = Field(min_length=1)
    accepted_at: str = Field(min_length=1)
    decision: Literal["approve", "reject"]
    note: str = Field(min_length=1)
    application: Literal["apply", "deferred"] = "apply"
    deferral_reason: str | None = None

    @model_validator(mode="after")
    def explained_deferral(self) -> Self:
        if self.application == "deferred" and (self.decision != "approve" or not (self.deferral_reason or "").strip()):
            raise ValueError("only approved claims can be deferred, and deferral requires a reason")
        if self.application == "apply" and self.deferral_reason:
            raise ValueError("deferral reason given for a claim marked for application")
        return self


class Witness(Record):
    id: str = Field(min_length=1)
    url: str = Field(min_length=1)
    sha256: str | None = Field(default=None, pattern=r"^[a-f0-9]{64}$")
    accessed_at: str
    status: Literal["fetched", "unavailable", "database_snapshot"]
    description: str

    @model_validator(mode="after")
    def content_hash(self) -> Self:
        if self.status != "unavailable" and not self.sha256:
            raise ValueError("available witnesses require artifact SHA-256")
        return self


class RhythmObservation(Record):
    source_tala: str | None = None
    eduppu: float | None = Field(default=None, allow_inf_nan=False)
    arudi: float | None = Field(default=None, allow_inf_nan=False)
    subdivision: str | None = None
    kalai: int | None = Field(default=None, gt=0)
    units: str | None = None
    counting_origin: str | None = None
    convention: str | None = None

    @model_validator(mode="after")
    def explicit_convention(self) -> Self:
        if any(x is not None for x in (self.eduppu, self.arudi, self.subdivision, self.kalai)):
            if not all((self.units, self.counting_origin, self.convention)):
                raise ValueError("interpreted rhythm requires units, origin and convention")
        return self


class Claim(Record):
    id: str
    subject_id: str
    axis: Literal[
        "attribution",
        "form",
        "raga",
        "tala",
        "addressee",
        "devotional_address",
        "geography",
        "group",
        "related_setting",
        "structure",
        "rhythm",
        "laya",
        "raga_character",
        "bhava",
        "mudra",
    ]
    value: str = Field(min_length=1)
    witness_id: str
    locator: str = Field(min_length=1)
    summary: str = Field(min_length=1)
    evidence_kind: EvidenceKind
    review_state: ReviewState
    uncertainty: str
    approval: Approval | None = None
    group_type: Literal["formal_group", "thematic_collection", "related_musical_setting"] | None = None
    rhythm: RhythmObservation | None = None

    @model_validator(mode="after")
    def adjudication(self) -> Self:
        if self.review_state == "reviewed" and (not self.approval or self.approval.decision != "approve"):
            raise ValueError("reviewed requires explicit human approval")
        if self.approval and self.approval.decision == "approve" and self.review_state != "reviewed":
            raise ValueError("approved claims must be reviewed")
        if self.evidence_kind == "automated_suggestion" and self.review_state == "reviewed":
            raise ValueError("automated-only evidence cannot become canonical")
        if self.axis == "group" and not self.group_type:
            raise ValueError("group claims require a group type")
        if self.axis == "rhythm" and not self.rhythm:
            raise ValueError("rhythm claims require a witness observation")
        if self.axis == "form" and self.value not in ("KRITHI", "VARNAM", "SWARAJATHI", "UNESTABLISHED"):
            raise ValueError("unknown form")
        if self.axis == "geography" and self.value == NO_PLACE_STATED and self.review_state == "reviewed":
            raise ValueError("an unstated place is an observation; approve NON_SPECIFIC_ADDRESS or a specific shrine")
        return self


def derive_geography_state(claims: list[Claim]) -> GeographyState:
    """Geography state follows the subject's geography claims; it is never set independently."""
    geography = [c for c in claims if c.axis == "geography"]
    if not geography:
        return "UNREVIEWED"
    if any(c.review_state == "disputed" for c in geography):
        return "DISPUTED"
    approved = {c.value == NON_SPECIFIC_ADDRESS for c in geography if c.review_state == "reviewed"}
    if approved == {True, False}:
        raise ValueError("approved specific and non-specific geography claims conflict")
    if approved == {False}:
        return "SPECIFIC_SHRINE_SUPPORTED"
    if approved == {True}:
        return "NON_SPECIFIC_ADDRESS"
    return "UNRESOLVED"


def geography_lead(claims: list[Claim]) -> str:
    places = sorted({c.value for c in claims if c.axis == "geography"} - {NO_PLACE_STATED, NON_SPECIFIC_ADDRESS})
    if places:
        return "named place: " + "; ".join(places)
    if any(c.axis == "geography" and c.value == NON_SPECIFIC_ADDRESS for c in claims):
        return "non-specific address candidate"
    if any(c.axis == "geography" for c in claims):
        return "no place stated"
    return "not researched"


class SourceRow(Record):
    row: int = Field(gt=0)
    title: str
    raw_raga: str
    url: str
    subject_id: str | None
    disposition: Literal["mapped_source", "alias_candidate", "excluded", "unresolved"]
    reason: str = Field(min_length=1)


class CatalogueObservation(Record):
    form: Form
    raga: str | None
    tala: str | None
    language: str | None
    raga_rows: int = Field(ge=0)
    section_count: int = Field(ge=0)
    lyric_variants: int = Field(ge=0)
    notation_variants: int = Field(ge=0)
    deity_id: UUID | None = None
    temple_id: UUID | None = None
    section_types: list[str]


class Composition(Record):
    id: str = Field(pattern=MASTER_ID_PATTERN)
    canonical_id: UUID | None
    composer_id: UUID
    title: str
    aliases: list[str]
    review_state: ReviewState
    unresolved_reason: str
    geography_state: GeographyState
    observed: CatalogueObservation
    findings: list[str]

    @model_validator(mode="after")
    def unresolved_is_explained(self) -> Self:
        if self.review_state in ("unresolved", "disputed") and not self.unresolved_reason.strip():
            raise ValueError("unresolved/disputed identity requires a reason")
        return self


class ApplicationManifest(TypedDict):
    track: str
    snapshot_at: str
    status: Literal["ready", "awaiting_claim_approval"]
    provenance_owner: str
    changes: list[dict[str, object]]


class ClassificationMaster(Record):
    schema_version: Literal["1.1"]
    track: str
    snapshot_at: str
    composer: str
    composer_id: UUID
    source_path: str
    source_sha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    source_row_count: int = Field(gt=0)
    database_snapshot_witness: str
    rows: list[SourceRow]
    compositions: list[Composition]
    witnesses: list[Witness]
    claims: list[Claim]
    limitations: list[str]

    @model_validator(mode="after")
    def accounting(self) -> Self:
        def unique(values: list[str], label: str) -> None:
            if len(values) != len(set(values)):
                raise ValueError(f"duplicate {label}")

        unique([x.id for x in self.compositions], "composition ID")
        unique([str(x.canonical_id) for x in self.compositions if x.canonical_id], "canonical UUID")
        unique([x.id for x in self.witnesses], "witness ID")
        unique([x.id for x in self.claims], "claim ID")
        if sorted(x.row for x in self.rows) != list(range(1, self.source_row_count + 1)):
            raise ValueError("source rows must be accounted exactly once")
        subjects = {x.id: x for x in self.compositions}
        witnesses = {x.id: x for x in self.witnesses}
        if self.database_snapshot_witness not in witnesses:
            raise ValueError("missing database snapshot witness")
        if any(x.composer_id != self.composer_id for x in self.compositions):
            raise ValueError("cross-composer identity")
        if any(x.subject_id is not None and x.subject_id not in subjects for x in self.rows):
            raise ValueError("orphan source row")
        if any(x.subject_id is None and x.disposition not in ("excluded", "unresolved") for x in self.rows):
            raise ValueError("mapped source row requires identity")
        approved_forms: dict[str, set[str]] = {}
        claims_by_subject: dict[str, list[Claim]] = {x: [] for x in subjects}
        for claim in self.claims:
            if claim.subject_id not in subjects or claim.witness_id not in witnesses:
                raise ValueError("orphan claim")
            if not claim.id.startswith(f"{claim.subject_id}:"):
                raise ValueError(f"claim {claim.id} must be anchored to its subject master ID")
            claims_by_subject[claim.subject_id].append(claim)
            if claim.review_state == "reviewed":
                if not subjects[claim.subject_id].canonical_id or not witnesses[claim.witness_id].sha256:
                    raise ValueError("approved claim requires canonical identity and hashed witness")
                if witnesses[claim.witness_id].status == "unavailable":
                    raise ValueError("unavailable witness cannot approve claim")
                if claim.axis == "form":
                    approved_forms.setdefault(claim.subject_id, set()).add(claim.value)
        if any(len(forms) > 1 for forms in approved_forms.values()):
            raise ValueError("conflicting canonical form approvals")
        for subject in self.compositions:
            derived = derive_geography_state(claims_by_subject[subject.id])
            if subject.geography_state != derived:
                raise ValueError(
                    f"{subject.id} geography_state {subject.geography_state} contradicts its claims ({derived})"
                )
        return self

    def verify_source(self, repository_root: Path) -> None:
        source = (repository_root / self.source_path).resolve()
        if not source.is_relative_to(repository_root.resolve()):
            raise ValueError("source path escapes repository")
        if hashlib.sha256(source.read_bytes()).hexdigest() != self.source_sha256:
            raise ValueError("source snapshot hash changed")
        with source.open(encoding="utf-8", newline="") as handle:
            source_rows = list(csv.DictReader(handle))
        if len(source_rows) != self.source_row_count:
            raise ValueError("source row count differs from artifact")
        for witness in self.witnesses:
            if witness.status == "database_snapshot":
                artifact = (repository_root / witness.url).resolve()
                if not artifact.is_relative_to(repository_root.resolve()):
                    raise ValueError("witness path escapes repository")
                if hashlib.sha256(artifact.read_bytes()).hexdigest() != witness.sha256:
                    raise ValueError("database snapshot hash changed")
        for row in self.rows:
            original = source_rows[row.row - 1]
            if (row.title, row.raw_raga, row.url) != (original["Krithi"], original["Raga"], original["Hyperlink"]):
                raise ValueError("source-row values differ from artifact")

    def application_manifest(self) -> ApplicationManifest:
        subjects = {x.id: x for x in self.compositions}
        changes: list[dict[str, object]] = [
            {
                "claim_id": c.id,
                "subject_id": c.subject_id,
                "canonical_id": str(subjects[c.subject_id].canonical_id),
                "composer_id": str(self.composer_id),
                "axis": c.axis,
                "value": c.value,
                "witness_id": c.witness_id,
                "approval": c.approval.model_dump(exclude_none=True) if c.approval else None,
            }
            for c in sorted(self.claims, key=lambda x: x.id)
            if c.review_state == "reviewed"
        ]
        return {
            "track": self.track,
            "snapshot_at": self.snapshot_at,
            "status": "ready" if changes else "awaiting_claim_approval",
            "provenance_owner": self.track,
            "changes": changes,
        }

    def render(self) -> str:
        def cell(value: object) -> str:
            return str(value).replace("|", "\\|").replace("\n", " ")

        membership_count = sum(c.axis == "group" and c.review_state == "reviewed" for c in self.claims)
        claims_by_subject: dict[str, list[Claim]] = {x.id: [] for x in self.compositions}
        for claim in self.claims:
            claims_by_subject[claim.subject_id].append(claim)
        leads = {x.id: geography_lead(claims_by_subject[x.id]) for x in self.compositions}
        output = [
            "| Metadata | Value |",
            "|:---|:---|",
            "| **Status** | Research snapshot — unresolved claims |",
            "| **Version** | 1.0.0 |",
            f"| **Last Updated** | {self.snapshot_at[:10]} |",
            "| **Author** | Sangita Grantha Team |",
            "",
            f"# {self.composer} classification concordance",
            "",
            f"Snapshot: `{self.snapshot_at}`. Input SHA-256: `{self.source_sha256}`.",
            "",
            f"{len(self.rows)} source rows; {len(self.compositions)} distinct catalogue identities; "
            f"{membership_count} approved group membership claims; "
            f"{len(self.witnesses)} witnesses. Catalogue values below are observations, not approved claims.",
            "",
            "## Limitations",
            "",
            *[f"- {x}" for x in self.limitations],
            "",
            "## Composition concordance",
            "",
            "Master IDs are persistent; the catalogue UUID in parentheses is the current, rebindable binding.",
            "",
            "| Identity / title | Source rows | Stored form / raga / tala | Review / geography | Findings |",
            "|:---|:---|:---|:---|:---|",
        ]
        for subject in sorted(self.compositions, key=lambda x: (x.title.casefold(), x.id)):
            rows = ", ".join(str(x.row) for x in sorted(self.rows, key=lambda x: x.row) if x.subject_id == subject.id)
            output.append(
                "| "
                + " | ".join(
                    cell(x)
                    for x in (
                        f"{subject.id} · {subject.title} ({subject.canonical_id or 'unbound'})",
                        rows,
                        f"{subject.observed.form} / {subject.observed.raga} / {subject.observed.tala}",
                        f"{subject.review_state} / {subject.geography_state} ({leads[subject.id]})",
                        "; ".join(subject.findings),
                    )
                )
                + " |"
            )
        output += [
            "",
            "## Claim concordance",
            "",
            "| Subject | Axis / value | Witness / locator | State | Evidence summary / uncertainty |",
            "|:---|:---|:---|:---|:---|",
        ]
        for claim in sorted(self.claims, key=lambda x: (x.subject_id, x.axis, x.id)):
            output.append(
                "| "
                + " | ".join(
                    cell(x)
                    for x in (
                        claim.subject_id,
                        f"{claim.axis}: {claim.value}",
                        f"{claim.witness_id}: {claim.locator}",
                        claim.review_state,
                        f"{claim.summary} {claim.uncertainty}",
                    )
                )
                + " |"
            )
        output += ["", "## Stored-field distributions", ""]
        for axis in ("form", "raga", "tala"):
            counts = Counter(str(getattr(x.observed, axis)) for x in self.compositions)
            output += [f"### {axis.title()}", "", "| Value | Distinct catalogue identities |", "|:---|---:|"]
            output += [f"| {cell(k)} | {v} |" for k, v in sorted(counts.items())]
            output.append("")
        output += ["## Geography distribution", "", "| State | Evidence | Distinct catalogue identities |"]
        output.append("|:---|:---|---:|")
        geography = Counter(
            (x.geography_state, "named place" if leads[x.id].startswith("named place") else leads[x.id])
            for x in self.compositions
        )
        output += [f"| {state} | {lead} | {count} |" for (state, lead), count in sorted(geography.items())]
        output.append("")
        output += [
            "## Rhythmic annex",
            "",
            "Source-stated tala is not eduppu, arudi, kalai or subdivision evidence.",
            "",
        ]
        for claim in sorted(self.claims, key=lambda x: x.id):
            if claim.rhythm:
                output.append(
                    f"- {claim.subject_id}: {claim.rhythm.model_dump_json(exclude_none=True)}; "
                    f"{claim.witness_id}; {claim.review_state}."
                )
        output += ["", "## Unresolved register", ""]
        output += [
            f"- {s.id} · {s.title}: {s.unresolved_reason}"
            for s in sorted(self.compositions, key=lambda x: x.title.casefold())
            if s.review_state != "reviewed"
        ]
        output += [
            "",
            "## Bibliography and witness register",
            "",
            "| Witness | Source | Artifact SHA-256 | Status |",
            "|:---|:---|:---|:---|",
        ]
        output += [
            f"| {cell(w.id)} | {cell(w.url)} | {w.sha256 or 'unavailable'} | {w.status} |"
            for w in sorted(self.witnesses, key=lambda x: x.id)
        ]
        return "\n".join(output) + "\n"


def load_master(path: Path) -> ClassificationMaster:
    return ClassificationMaster.model_validate(json.loads(path.read_text(encoding="utf-8")))
