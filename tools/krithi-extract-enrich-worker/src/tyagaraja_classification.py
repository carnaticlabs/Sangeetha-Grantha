"""Tyagaraja classification master for TRACK-147.

The shared claim vocabulary matches TRACK-146's accepted contract. This module does not
write to the database and requires explicit human approval plus a hashed witness before
any claim becomes seedable.
"""

from __future__ import annotations

import csv
import hashlib
import json
from collections import Counter, defaultdict
from enum import StrEnum
from pathlib import Path
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator

TRACK147_COMPOSER_ID = "b1bdccd8-8cb1-4a14-a9e6-7ffc843cd7b6"
SNAPSHOT_DATE = "2026-10-01"
EXPECTED_IDENTITIES = 675
EXPECTED_SOURCE_ROWS = 684
EXPECTED_SUBJECTS = 684
EXPECTED_SINGLE_SOURCE_SUBJECTS = 667
EXPECTED_COLLISION_SUBJECTS = 17
EXPECTED_COLLISION_IDENTITIES = 8
EXPECTED_GHANA_CANDIDATES = 5
EXPECTED_DESADI_WITNESSES = 11
EXPECTED_MISSING_PALLAVI_IDENTITIES = 27
EXPECTED_MISSING_CHARANAM_IDENTITIES = 4
SOURCE_CSV_PATH = "database/for_import/Thyagaraja-Krithi-For-Import.csv"
SNAPSHOT_JSON_PATH = "database/data/track147-catalogue-snapshot.json"
EXPECTED_INVENTORY_ROWS = 687
EXPECTED_CROSS_COMPOSER_ROWS = 2
EXPECTED_NOT_IMPORTED_ROWS = 1
GHANA_GROUP_VALUE = "tyagaraja-ghana-raga-pancharatnam"
GHANA_LOCATOR = "database/migrations/R__seed_07_canonical_cycle_tags.sql"
# Aradhana concert order. Not a composer sequence.
GHANA_RAGA_PERFORMANCE_ORDER: dict[str, int] = {
    "Nāṭṭai": 1,
    "Gowla": 2,
    "Ārabhi": 3,
    "Varāli": 4,
    "Sri": 5,
}


class TyagarajaClassificationError(ValueError):
    """The master breaks TRACK-147 accounting or claim rules."""


class EvidenceKind(StrEnum):
    TEXTUAL_EVIDENCE = "textual_evidence"
    SCHOLARLY_INTERPRETATION = "scholarly_interpretation"
    DOCUMENTED_TRADITION = "documented_tradition"
    AUTOMATED_SUGGESTION = "automated_suggestion"


class ReviewState(StrEnum):
    UNREVIEWED = "unreviewed"
    REVIEWED = "reviewed"
    UNRESOLVED = "unresolved"
    DISPUTED = "disputed"
    REJECTED = "rejected"


class ClaimAxis(StrEnum):
    DEITY = "deity"
    GEOGRAPHY = "geography"
    THEME = "theme"
    GROUP = "group"
    RITUAL = "ritual"
    DRAMATIC = "dramatic"


class SubjectKind(StrEnum):
    CATALOGUE_IDENTITY = "catalogue_identity"
    SOURCE_WITNESS = "source_witness"


class GeographyState(StrEnum):
    UNREVIEWED = "UNREVIEWED"
    SPECIFIC_SHRINE_SUPPORTED = "SPECIFIC_SHRINE_SUPPORTED"
    NON_SPECIFIC_ADDRESS = "NON_SPECIFIC_ADDRESS"
    UNRESOLVED = "UNRESOLVED"
    DISPUTED = "DISPUTED"


class WitnessStatus(StrEnum):
    FETCHED = "fetched"
    UNAVAILABLE = "unavailable"
    DATABASE_SNAPSHOT = "database_snapshot"


class GroupType(StrEnum):
    FORMAL_GROUP = "formal_group"
    KSHETRA_GROUP = "kshetra_group"
    RITUAL_COLLECTION = "ritual_collection"
    DRAMATIC_WORK = "dramatic_work"
    THEMATIC_COLLECTION = "thematic_collection"


class DramaticItemType(StrEnum):
    DARU = "DARU"
    KIRTANA = "KIRTANA"
    PADYA = "PADYA"
    VACANA = "VACANA"
    DVIPADA = "DVIPADA"
    UNESTABLISHED = "UNESTABLISHED"


class Approval(BaseModel):
    """Explicit human review decision for a classification claim."""

    model_config = ConfigDict(extra="forbid")

    reviewer: str = Field(min_length=1)
    accepted_at: str = Field(min_length=1)
    decision: Literal["approve", "reject"]
    note: str = Field(min_length=1)
    application: Literal["apply", "deferred"] = "apply"
    deferral_reason: str | None = None

    @model_validator(mode="after")
    def explained_deferral(self) -> Approval:
        if self.application == "deferred" and (self.decision != "approve" or not (self.deferral_reason or "").strip()):
            raise ValueError("only approved claims can be deferred, and deferral requires a reason")
        if self.application == "apply" and self.deferral_reason:
            raise ValueError("deferral reason given for a claim marked for application")
        return self


class Witness(BaseModel):
    """Source witness artifact or bibliographic entry."""

    model_config = ConfigDict(extra="forbid")

    id: str = Field(min_length=1)
    url: str = Field(min_length=1)
    sha256: str | None = Field(default=None, pattern=r"^[a-f0-9]{64}$")
    accessed_at: str
    status: WitnessStatus
    description: str

    @model_validator(mode="after")
    def content_hash(self) -> Witness:
        if self.status != WitnessStatus.UNAVAILABLE and not self.sha256:
            raise ValueError(f"available witness {self.id} requires artifact SHA-256")
        return self


class ImportCensusRow(BaseModel):
    """One mapped thyagaraja-vaibhavam import row joined to its catalogue identity."""

    model_config = ConfigDict(extra="forbid")

    krithi_id: str
    title: str
    primary_language: str
    musical_form: str
    workflow_state: str
    is_ragamalika: bool
    canonical_raga: str
    canonical_tala: str
    has_deity: bool
    has_temple: bool
    import_id: str
    raw_raga: str
    raw_tala: str
    source_url: str
    import_status: str
    pancharatnam_tag: bool
    evidence_id: str | None
    pallavi_count: int
    anupallavi_count: int
    charanam_count: int
    swara_sahitya_count: int


class ClassificationClaim(BaseModel):
    """One axis on one classification subject. Seedable only after human approval."""

    model_config = ConfigDict(extra="forbid")

    id: str
    axis: ClaimAxis
    value: str | None = None
    witness_id: str | None = None
    locator: str | None = None
    excerpt: str | None = None
    evidence_kind: EvidenceKind | None = None
    review_state: ReviewState
    uncertainty: str | None = None
    reviewer_decision: str | None = None
    approval: Approval | None = None
    group_type: GroupType | None = None
    performance_order: int | None = None
    ritual_function: str | None = None
    dramatic_work: str | None = None
    dramatic_item_type: DramaticItemType | None = None
    dramatic_speaker: str | None = None
    dramatic_addressee: str | None = None
    dramatic_division: str | None = None
    mudra: str | None = None

    @model_validator(mode="after")
    def review_rules(self) -> ClassificationClaim:
        if self.value is not None and (self.evidence_kind is None or not self.locator):
            raise ValueError(f"claim {self.id} has a value without evidence kind and locator")
        has_approval = bool(self.reviewer_decision) or (
            self.approval is not None and self.approval.decision == "approve"
        )
        if self.review_state == ReviewState.REVIEWED and not has_approval:
            raise ValueError(f"claim {self.id} is reviewed without a human approval")
        if has_approval and self.review_state != ReviewState.REVIEWED:
            raise ValueError(f"claim {self.id} has an approval but is not reviewed")
        if self.evidence_kind == EvidenceKind.AUTOMATED_SUGGESTION and self.review_state == ReviewState.REVIEWED:
            raise ValueError(f"claim {self.id} cannot become reviewed from an automated suggestion alone")
        if self.axis in {ClaimAxis.DEITY, ClaimAxis.THEME} and self.value is not None and not self.excerpt:
            raise ValueError(f"claim {self.id} needs a passage excerpt")
        if self.axis == ClaimAxis.DEITY and self.value and "rama" in self.value.casefold() and not self.excerpt:
            raise ValueError(f"claim {self.id} assigns Rama without a passage")
        if (
            self.axis == ClaimAxis.GEOGRAPHY
            and self.value == "NO_PLACE_STATED"
            and self.review_state == ReviewState.REVIEWED
        ):
            raise ValueError(
                f"claim {self.id} cannot approve NO_PLACE_STATED; approve NON_SPECIFIC_ADDRESS or a specific shrine"
            )
        return self

    @property
    def is_seedable(self) -> bool:
        has_approval = bool(self.reviewer_decision) or (
            self.approval is not None and self.approval.decision == "approve"
        )
        return self.review_state == ReviewState.REVIEWED and has_approval and self.value is not None


class SectionFlags(BaseModel):
    model_config = ConfigDict(extra="forbid")

    pallavi_count: int = Field(ge=0)
    anupallavi_count: int = Field(ge=0)
    charanam_count: int = Field(ge=0)
    swara_sahitya_count: int = Field(ge=0)


class ClassificationSubject(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: str
    subject_kind: SubjectKind
    krithi_id: str | None = None
    mapped_krithi_id: str | None = None
    import_id: str
    evidence_id: str | None = None
    title: str
    canonical_raga: str
    canonical_tala: str
    raw_raga: str
    raw_tala: str
    source_url: str
    import_status: str
    primary_language: str
    musical_form: str
    workflow_state: str
    is_ragamalika: bool
    geography_state: GeographyState
    deity_state: Literal["UNREVIEWED", "REVIEWED", "UNRESOLVED", "DISPUTED"] = "UNREVIEWED"
    section_flags: SectionFlags
    structure_audit: list[str]
    tala_witness_note: str | None = None
    claims: list[ClassificationClaim]


class CollisionEntry(BaseModel):
    model_config = ConfigDict(extra="forbid")

    krithi_id: str
    title: str
    disposition: Literal["collision_not_classified"]
    witness_subject_ids: list[str]
    source_settings: list[str]


class ExternalGap(BaseModel):
    model_config = ConfigDict(extra="forbid")

    label: str
    reported_count: int
    status: Literal["unresolved_external"]
    note: str


class InventoryDisposition(StrEnum):
    CLASSIFICATION_SUBJECT = "classification_subject"
    CROSS_COMPOSER_MAPPING = "cross_composer_mapping"
    NOT_IMPORTED = "not_imported"


class SourceInventoryRow(BaseModel):
    """One CSV row. The row number is a locator, not a composition identity."""

    model_config = ConfigDict(extra="forbid")

    row: int = Field(gt=0)
    title: str
    raw_raga: str
    url: str
    disposition: InventoryDisposition
    subject_id: str | None = None
    import_id: str | None = None
    mapped_krithi_id: str | None = None
    mapped_composer: str | None = None
    note: str | None = None

    @model_validator(mode="after")
    def disposition_fields(self) -> SourceInventoryRow:
        if self.disposition == InventoryDisposition.CLASSIFICATION_SUBJECT:
            if not self.subject_id or self.import_id or self.mapped_krithi_id or self.mapped_composer:
                raise ValueError(f"row {self.row} must reference a subject only")
        elif self.subject_id:
            raise ValueError(f"row {self.row} is not a classification subject")
        if self.disposition == InventoryDisposition.CROSS_COMPOSER_MAPPING and not (
            self.import_id and self.mapped_krithi_id and self.mapped_composer and self.note
        ):
            raise ValueError(f"row {self.row} needs the import, mapped identity, composer and note")
        if self.disposition == InventoryDisposition.NOT_IMPORTED and (
            self.import_id or self.mapped_krithi_id or not self.note
        ):
            raise ValueError(f"row {self.row} is not imported and needs a note only")
        return self


class SourceInventory(BaseModel):
    model_config = ConfigDict(extra="forbid")

    path: str
    sha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    row_count: int = Field(gt=0)
    rows: list[SourceInventoryRow]


class Accounting(BaseModel):
    model_config = ConfigDict(extra="forbid")

    catalogue_identities: int
    source_rows: int
    single_source_subjects: int
    collision_subjects: int
    collision_identities: int
    classification_subjects: int


class TyagarajaClassificationMaster(BaseModel):
    model_config = ConfigDict(extra="forbid")

    snapshot_date: str
    composer: Literal["Tyagaraja"] = "Tyagaraja"
    composer_id: str
    database_snapshot_witness: str | None = None
    accounting: Accounting
    witnesses: list[Witness] = Field(default_factory=list)
    limitations: list[str] = Field(default_factory=list)
    subjects: list[ClassificationSubject]
    collision_register: list[CollisionEntry]
    source_inventory: SourceInventory | None = None
    external_gap: list[ExternalGap]


class ApplicationManifest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    track: Literal["TRACK-147"] = "TRACK-147"
    snapshot_at: str = SNAPSHOT_DATE
    status: Literal["ready", "awaiting_claim_approval"] = "awaiting_claim_approval"
    provenance_owner: Literal["TRACK-147"] = "TRACK-147"
    approvals: list[str] = Field(default_factory=list)
    changes: list[dict[str, Any]] = Field(default_factory=list)


def catalogue_subject_id(krithi_id: str) -> str:
    return f"tyagaraja:krithi:{krithi_id}"


def witness_subject_id(import_id: str) -> str:
    return f"tyagaraja:import:{import_id}"


def derive_geography_state(claims: list[ClassificationClaim]) -> GeographyState:
    """Derive geography state deterministically from the subject's geography claims."""
    geo_claims = [c for c in claims if c.axis == ClaimAxis.GEOGRAPHY]
    if not geo_claims or all(c.value is None and c.review_state == ReviewState.UNREVIEWED for c in geo_claims):
        return GeographyState.UNREVIEWED
    if any(c.review_state == ReviewState.DISPUTED for c in geo_claims):
        return GeographyState.DISPUTED
    reviewed = [c for c in geo_claims if c.review_state == ReviewState.REVIEWED and c.value is not None]
    if reviewed:
        non_specific = {c.value == "NON_SPECIFIC_ADDRESS" for c in reviewed}
        if non_specific == {True, False}:
            raise TyagarajaClassificationError("approved specific and non-specific geography claims conflict")
        if non_specific == {True}:
            return GeographyState.NON_SPECIFIC_ADDRESS
        return GeographyState.SPECIFIC_SHRINE_SUPPORTED
    return GeographyState.UNRESOLVED


def build_application_manifest(master: TyagarajaClassificationMaster) -> ApplicationManifest:
    """Build the deterministic application manifest from reviewed claims."""
    validate_master(master)
    changes: list[dict[str, Any]] = []
    approvals: list[str] = []
    for subject in master.subjects:
        for claim in subject.claims:
            if not claim.is_seedable:
                continue
            if subject.subject_kind != SubjectKind.CATALOGUE_IDENTITY or not subject.krithi_id:
                raise TyagarajaClassificationError(
                    f"seedable claim {claim.id} is attached to non-identity subject {subject.id}"
                )
            approvals.append(claim.id)
            approval_payload = (
                claim.approval.model_dump(exclude_none=True)
                if claim.approval
                else {
                    "reviewer": "Seshadri",
                    "accepted_at": master.snapshot_date,
                    "decision": "approve",
                    "note": claim.reviewer_decision or "Approved",
                    "application": "apply",
                }
            )
            entry: dict[str, Any] = {
                "claim_id": claim.id,
                "subject_id": subject.id,
                "canonical_id": subject.krithi_id,
                "composer_id": master.composer_id,
                "axis": claim.axis.value,
                "value": claim.value,
                "witness_id": claim.witness_id,
                "approval": approval_payload,
            }
            if claim.performance_order is not None:
                entry["performance_order"] = claim.performance_order
            if claim.mudra is not None:
                entry["mudra"] = claim.mudra
            if claim.ritual_function is not None:
                entry["ritual_function"] = claim.ritual_function
            if claim.dramatic_work is not None:
                entry["dramatic_work"] = claim.dramatic_work
            changes.append(entry)

    changes.sort(key=lambda item: str(item["claim_id"]))
    approvals.sort()
    return ApplicationManifest(
        track="TRACK-147",
        snapshot_at=master.snapshot_date,
        status="ready" if changes else "awaiting_claim_approval",
        provenance_owner="TRACK-147",
        approvals=approvals,
        changes=changes,
    )


def build_master(rows: list[ImportCensusRow], *, snapshot_date: str = SNAPSHOT_DATE) -> TyagarajaClassificationMaster:
    """Build subjects from import rows. Ids follow UUIDs, not input order."""

    grouped: dict[str, list[ImportCensusRow]] = defaultdict(list)
    for row in rows:
        grouped[row.krithi_id].append(row)

    collision_ids: set[str] = set()
    for krithi_id, group in grouped.items():
        if len(group) == 1:
            continue
        if _settings_disagree(group):
            collision_ids.add(krithi_id)
            continue
        raise TyagarajaClassificationError(
            f"krithi {krithi_id} has multiple imports that agree on raga and tala; "
            "the plan does not classify that grain"
        )

    subjects: list[ClassificationSubject] = []
    collisions: list[CollisionEntry] = []
    for krithi_id, group in grouped.items():
        if krithi_id in collision_ids:
            witnesses = [_witness_subject(row) for row in group]
            witnesses.sort(key=lambda subject: subject.id)
            subjects.extend(witnesses)
            collisions.append(
                CollisionEntry(
                    krithi_id=krithi_id,
                    title=group[0].title,
                    disposition="collision_not_classified",
                    witness_subject_ids=[subject.id for subject in witnesses],
                    source_settings=[f"{subject.raw_raga} / {subject.raw_tala}" for subject in witnesses],
                )
            )
        else:
            subjects.append(_catalogue_subject(group[0]))

    subjects.sort(key=lambda subject: subject.id)
    collisions.sort(key=lambda entry: entry.krithi_id)
    master = TyagarajaClassificationMaster(
        snapshot_date=snapshot_date,
        composer_id=TRACK147_COMPOSER_ID,
        accounting=Accounting(
            catalogue_identities=len(grouped),
            source_rows=len(rows),
            single_source_subjects=len(grouped) - len(collision_ids),
            collision_subjects=sum(len(group) for krithi_id, group in grouped.items() if krithi_id in collision_ids),
            collision_identities=len(collision_ids),
            classification_subjects=len(subjects),
        ),
        subjects=subjects,
        collision_register=collisions,
        external_gap=_external_gap(),
    )
    validate_master(master)
    return master


def validate_master(master: TyagarajaClassificationMaster) -> None:
    """Check internal consistency. Does not require the live 684-subject census."""

    subjects = master.subjects
    ids = [subject.id for subject in subjects]
    if len(ids) != len(set(ids)):
        raise TyagarajaClassificationError("subject ids are not unique")
    if ids != sorted(ids):
        raise TyagarajaClassificationError("subjects are not ordered by stable id")

    witnesses_by_id: dict[str, Witness] = {}
    if master.witnesses:
        w_ids = [w.id for w in master.witnesses]
        if len(w_ids) != len(set(w_ids)):
            raise TyagarajaClassificationError("witness ids are not unique")
        witnesses_by_id = {w.id: w for w in master.witnesses}
        if master.database_snapshot_witness:
            snap_w = witnesses_by_id.get(master.database_snapshot_witness)
            if snap_w is None or snap_w.status != WitnessStatus.DATABASE_SNAPSHOT:
                raise TyagarajaClassificationError("missing or invalid database snapshot witness")

    collision_ids = {entry.krithi_id for entry in master.collision_register}
    if len(collision_ids) != len(master.collision_register):
        raise TyagarajaClassificationError("collision register repeats a krithi id")
    for entry in master.collision_register:
        if entry.disposition != "collision_not_classified":
            raise TyagarajaClassificationError(f"{entry.krithi_id} is not marked collision_not_classified")
        if catalogue_subject_id(entry.krithi_id) in ids:
            raise TyagarajaClassificationError(f"collapsed krithi {entry.krithi_id} is a classification subject")

    catalogue = [subject for subject in subjects if subject.subject_kind == SubjectKind.CATALOGUE_IDENTITY]
    witnesses = [subject for subject in subjects if subject.subject_kind == SubjectKind.SOURCE_WITNESS]
    if any(subject.krithi_id in collision_ids for subject in subjects):
        raise TyagarajaClassificationError("a claim subject uses a collapsed krithi id")
    if any(subject.krithi_id is None or subject.mapped_krithi_id is not None for subject in catalogue):
        raise TyagarajaClassificationError("catalogue subjects must carry krithi_id only")
    if any(subject.krithi_id is not None or subject.mapped_krithi_id not in collision_ids for subject in witnesses):
        raise TyagarajaClassificationError("witness subjects must point at a collision identity")

    witness_ids_by_krithi: dict[str, list[str]] = defaultdict(list)
    for subject in witnesses:
        mapped = subject.mapped_krithi_id
        if mapped is None:
            raise TyagarajaClassificationError(f"{subject.id} is missing mapped_krithi_id")
        witness_ids_by_krithi[mapped].append(subject.id)
    for entry in master.collision_register:
        if entry.witness_subject_ids != witness_ids_by_krithi.get(entry.krithi_id, []):
            raise TyagarajaClassificationError(f"collision witnesses do not match for {entry.krithi_id}")

    accounting = master.accounting
    expected = Accounting(
        catalogue_identities=len(catalogue) + len(collision_ids),
        source_rows=len(subjects),
        single_source_subjects=len(catalogue),
        collision_subjects=len(witnesses),
        collision_identities=len(collision_ids),
        classification_subjects=len(subjects),
    )
    if accounting != expected:
        raise TyagarajaClassificationError(f"accounting {accounting} does not match subjects {expected}")

    for subject in subjects:
        _validate_subject_claims(subject, collision_ids, witnesses_by_id)

    labels = [gap.label for gap in master.external_gap]
    if labels != ["track093_target_692"]:
        raise TyagarajaClassificationError("external gap register is missing or reordered")
    for gap in master.external_gap:
        if gap.status != "unresolved_external":
            raise TyagarajaClassificationError(f"external gap {gap.label} is not unresolved")
        if gap.reported_count != 692:
            raise TyagarajaClassificationError(f"external gap {gap.label} invented a count")

    if master.source_inventory is not None:
        _validate_inventory(master.source_inventory, subjects)


def _validate_inventory(inventory: SourceInventory, subjects: list[ClassificationSubject]) -> None:
    if [row.row for row in inventory.rows] != list(range(1, inventory.row_count + 1)):
        raise TyagarajaClassificationError("source inventory must list every CSV row once, in order")
    urls = [row.url for row in inventory.rows]
    if len(urls) != len(set(urls)):
        raise TyagarajaClassificationError("source inventory repeats a URL")
    by_id = {subject.id: subject for subject in subjects}
    referenced: list[str] = []
    for row in inventory.rows:
        if row.disposition != InventoryDisposition.CLASSIFICATION_SUBJECT:
            if row.url in {subject.source_url for subject in subjects}:
                raise TyagarajaClassificationError(f"row {row.row} hides a classification subject")
            continue
        subject = by_id.get(row.subject_id or "")
        if subject is None or subject.source_url != row.url:
            raise TyagarajaClassificationError(f"row {row.row} does not match its subject URL")
        referenced.append(subject.id)
    if sorted(referenced) != sorted(by_id):
        raise TyagarajaClassificationError("every classification subject needs exactly one source row")


def verify_source(master: TyagarajaClassificationMaster, repository_root: Path) -> None:
    """Fail if the hashed CSV or database snapshot no longer matches the recorded inventory."""

    inventory = master.source_inventory
    if inventory is None:
        raise TyagarajaClassificationError("master has no source inventory")
    root_resolved = repository_root.resolve()
    source = (repository_root / inventory.path).resolve()
    if not source.is_relative_to(root_resolved):
        raise TyagarajaClassificationError("source path escapes repository")
    if hashlib.sha256(source.read_bytes()).hexdigest() != inventory.sha256:
        raise TyagarajaClassificationError("source CSV hash changed")
    with source.open(encoding="utf-8", newline="") as handle:
        source_rows = list(csv.DictReader(handle))
    if len(source_rows) != inventory.row_count:
        raise TyagarajaClassificationError("source CSV row count differs from inventory")
    for row in inventory.rows:
        original = source_rows[row.row - 1]
        if (row.title, row.raw_raga, row.url) != (original["Krithi"], original["Raga"], original["Hyperlink"]):
            raise TyagarajaClassificationError(f"source CSV row {row.row} differs from inventory")

    for witness in master.witnesses:
        if witness.status == WitnessStatus.DATABASE_SNAPSHOT:
            artifact = (repository_root / witness.url).resolve()
            if not artifact.is_relative_to(root_resolved):
                raise TyagarajaClassificationError("snapshot witness path escapes repository")
            if not artifact.exists():
                raise TyagarajaClassificationError(f"snapshot witness file missing: {witness.url}")
            if hashlib.sha256(artifact.read_bytes()).hexdigest() != witness.sha256:
                raise TyagarajaClassificationError("database snapshot hash changed")


def build_source_inventory(
    master: TyagarajaClassificationMaster,
    csv_path: Path,
    *,
    relative_path: str = SOURCE_CSV_PATH,
    outside_rows: dict[str, SourceInventoryRow],
) -> SourceInventory:
    """Map CSV rows to subjects by exact URL.

    Rows outside the subjects need a curated entry keyed by URL; its row, title, raga and URL
    are taken from the CSV.
    """

    content = csv_path.read_bytes()
    with csv_path.open(encoding="utf-8", newline="") as handle:
        source_rows = list(csv.DictReader(handle))
    subject_by_url = {subject.source_url: subject.id for subject in master.subjects}
    rows: list[SourceInventoryRow] = []
    for number, original in enumerate(source_rows, start=1):
        url = original["Hyperlink"]
        if url in subject_by_url:
            rows.append(
                SourceInventoryRow(
                    row=number,
                    title=original["Krithi"],
                    raw_raga=original["Raga"],
                    url=url,
                    disposition=InventoryDisposition.CLASSIFICATION_SUBJECT,
                    subject_id=subject_by_url[url],
                )
            )
        elif url in outside_rows:
            located = {"row": number, "title": original["Krithi"], "raw_raga": original["Raga"], "url": url}
            rows.append(outside_rows[url].model_copy(update=located))
        else:
            raise TyagarajaClassificationError(f"CSV row {number} has no subject or curated disposition")
    return SourceInventory(
        path=relative_path,
        sha256=hashlib.sha256(content).hexdigest(),
        row_count=len(source_rows),
        rows=rows,
    )


def assert_track147_census(master: TyagarajaClassificationMaster) -> None:
    """Fail unless this master is the 2026-10-01 Tyagaraja snapshot with valid provenance."""

    validate_master(master)
    accounting = master.accounting
    if master.snapshot_date != SNAPSHOT_DATE or master.composer_id != TRACK147_COMPOSER_ID:
        raise TyagarajaClassificationError("snapshot date or composer id does not match TRACK-147")
    if accounting.classification_subjects != EXPECTED_SUBJECTS:
        raise TyagarajaClassificationError(f"expected {EXPECTED_SUBJECTS} subjects")
    if accounting.catalogue_identities != EXPECTED_IDENTITIES:
        raise TyagarajaClassificationError(f"expected {EXPECTED_IDENTITIES} identities")
    if accounting.source_rows != EXPECTED_SOURCE_ROWS:
        raise TyagarajaClassificationError(f"expected {EXPECTED_SOURCE_ROWS} source rows")
    if accounting.single_source_subjects != EXPECTED_SINGLE_SOURCE_SUBJECTS:
        raise TyagarajaClassificationError("single-source subject count drifted")
    if accounting.collision_subjects != EXPECTED_COLLISION_SUBJECTS:
        raise TyagarajaClassificationError("collision subject count drifted")
    if accounting.collision_identities != EXPECTED_COLLISION_IDENTITIES:
        raise TyagarajaClassificationError("collision identity count drifted")

    inventory = master.source_inventory
    if inventory is None or inventory.path != SOURCE_CSV_PATH:
        raise TyagarajaClassificationError("source inventory is missing")
    if inventory.row_count != EXPECTED_INVENTORY_ROWS:
        raise TyagarajaClassificationError(f"expected {EXPECTED_INVENTORY_ROWS} CSV rows")
    dispositions = Counter(row.disposition for row in inventory.rows)
    if dispositions[InventoryDisposition.CROSS_COMPOSER_MAPPING] != EXPECTED_CROSS_COMPOSER_ROWS:
        raise TyagarajaClassificationError("cross-composer row count drifted")
    if dispositions[InventoryDisposition.NOT_IMPORTED] != EXPECTED_NOT_IMPORTED_ROWS:
        raise TyagarajaClassificationError("not-imported row count drifted")

    ghana = [
        subject
        for subject in master.subjects
        if any(claim.axis == ClaimAxis.GROUP and claim.value == GHANA_GROUP_VALUE for claim in subject.claims)
    ]
    if len(ghana) != EXPECTED_GHANA_CANDIDATES:
        raise TyagarajaClassificationError("Ghana-raga candidate count is not five")
    if any(subject.subject_kind != SubjectKind.CATALOGUE_IDENTITY for subject in ghana):
        raise TyagarajaClassificationError("Ghana-raga candidate is not a single catalogue identity")
    for subject in ghana:
        claim = next(
            claim for claim in subject.claims if claim.axis == ClaimAxis.GROUP and claim.value == GHANA_GROUP_VALUE
        )
        if claim.review_state == ReviewState.UNREVIEWED:
            if claim.evidence_kind != EvidenceKind.AUTOMATED_SUGGESTION or claim.is_seedable:
                raise TyagarajaClassificationError("unreviewed Ghana-raga candidate must be an automated suggestion")
        elif claim.review_state == ReviewState.REVIEWED:
            if claim.evidence_kind == EvidenceKind.AUTOMATED_SUGGESTION or not claim.is_seedable:
                raise TyagarajaClassificationError(
                    "reviewed Ghana-raga membership requires non-heuristic evidence and human approval"
                )
            expected_order = GHANA_RAGA_PERFORMANCE_ORDER.get(subject.canonical_raga)
            if claim.performance_order != expected_order:
                raise TyagarajaClassificationError(
                    f"Ghana-raga performance_order for {subject.title} does not match Aradhana convention"
                )

    for subject in master.subjects:
        for claim in subject.claims:
            if claim.is_seedable and subject.subject_kind != SubjectKind.CATALOGUE_IDENTITY:
                raise TyagarajaClassificationError(
                    f"seedable claim {claim.id} is attached to collision witness {subject.id}"
                )

    if _identity_count(master, "missing_pallavi") != EXPECTED_MISSING_PALLAVI_IDENTITIES:
        raise TyagarajaClassificationError("missing pallavi identity count drifted")
    if _identity_count(master, "missing_charanam") != EXPECTED_MISSING_CHARANAM_IDENTITIES:
        raise TyagarajaClassificationError("missing charanam identity count drifted")
    desadi = [subject for subject in master.subjects if subject.tala_witness_note]
    if len(desadi) != EXPECTED_DESADI_WITNESSES:
        raise TyagarajaClassificationError("desadi witness count drifted")


def render_markdown(master: TyagarajaClassificationMaster) -> str:
    """Render the concordance. The same master always produces the same text."""

    accounting = master.accounting
    all_claims = [claim for subject in master.subjects for claim in subject.claims]
    reviewed_claims = [claim for claim in all_claims if claim.review_state == ReviewState.REVIEWED]
    approved_group_claims = [
        claim
        for claim in reviewed_claims
        if claim.axis == ClaimAxis.GROUP and (claim.approval is None or claim.approval.application == "apply")
    ]
    deferred_claims = [
        claim for claim in reviewed_claims if claim.approval is not None and claim.approval.application == "deferred"
    ]
    unresolved_claims = [claim for claim in all_claims if claim.review_state == ReviewState.UNRESOLVED]
    unreviewed_leads = [
        claim for claim in all_claims if claim.review_state == ReviewState.UNREVIEWED and claim.value is not None
    ]
    status_line = (
        "Implemented — multi-witness classification & Ghana-raga Pancharatnam approved"
        if reviewed_claims
        else "Draft — classification skeleton"
    )
    updated_date = "2026-10-15" if reviewed_claims else master.snapshot_date

    lines = [
        "| Metadata | Value |",
        "|:---|:---|",
        f"| **Status** | {status_line} |",
        "| **Version** | 1.1.0 |" if reviewed_claims else "| **Version** | 1.0.0 |",
        f"| **Last Updated** | {updated_date} |",
        "| **Author** | Sangita Grantha Team |",
        "",
        "# Tyagaraja classification master",
        "",
        "---",
        "",
        (
            f"Generated deterministically from the TRACK-147 structured master (`{master.snapshot_date}` snapshot). "
            f"Covers {accounting.catalogue_identities} distinct catalogue identities, "
            f"{accounting.classification_subjects} classification subjects "
            f"({accounting.single_source_subjects} single-source identities and "
            f"{accounting.collision_subjects} collision source witnesses), "
            f"{len(all_claims)} structured claims ({len(reviewed_claims)} human-approved: "
            f"{len(approved_group_claims)} applied group memberships, "
            f"{len(reviewed_claims) - len(approved_group_claims) - len(deferred_claims)} supporting cycle claims, "
            f"{len(deferred_claims)} deferred non-cycle approvals; "
            f"{len(unresolved_claims)} source-observed `unresolved` claims; "
            f"{len(unreviewed_leads)} edition-dependent `unreviewed` leads), and "
            f"{len(master.witnesses)} registered witnesses."
            if reviewed_claims
            else "Generated from the TRACK-147 structured master. "
            "Claims in this document are unreviewed. None are approved for seeding."
        ),
        "",
        "Related track: [TRACK-147](../../../conductor/tracks/TRACK-147-tyagaraja-musicological-classification.md). "
        "Shared contract: [musicological-classification-contract.md](./musicological-classification-contract.md). "
        "Structured master: [track147-tyagaraja-master.json](../../../database/data/track147-tyagaraja-master.json). "
        "Application manifest: [track147-tyagaraja-application-manifest.json]"
        "(../../../database/data/track147-tyagaraja-application-manifest.json).",
        "",
        "> [!IMPORTANT]",
        "> Eight catalogue identities are collisions. They are listed in the collision register "
        "and are not classification subjects. "
        "Their 17 source rows are the classification subjects.",
        "",
        "## Accounting",
        "",
        f"Snapshot date: {master.snapshot_date}. Composer id `{master.composer_id}`.",
        "",
        "| Unit | Count |",
        "|:---|---:|",
        f"| Catalogue identities | {accounting.catalogue_identities} |",
        f"| Source rows | {accounting.source_rows} |",
        f"| Single-source subjects | {accounting.single_source_subjects} |",
        f"| Collision subjects | {accounting.collision_subjects} |",
        f"| Collision identities, not classified | {accounting.collision_identities} |",
        f"| Classification subjects | {accounting.classification_subjects} |",
    ]
    if master.witnesses:
        lines.extend(
            [
                f"| Registered witnesses | {len(master.witnesses)} |",
                f"| Total classification claims | {len(all_claims)} |",
                f"| Approved (`reviewed`) claims | {len(reviewed_claims)} |",
                f"| Source-observed (`unresolved`) claims | {len(unresolved_claims)} |",
                f"| Edition-lead (`unreviewed`) claims | {len(unreviewed_leads)} |",
            ]
        )

    if master.limitations:
        lines.extend(["", "## Limitations and epistemic guardrails", ""])
        for item in master.limitations:
            lines.append(f"- {item}")

    lines.extend(
        [
            "",
            "## Collision register",
            "",
            "Each row is one source page. The catalogue title is the current collapsed identity.",
            "",
            "| Catalogue title | Source setting | Subject | Import status |",
            "|:---|:---|:---|:---|",
        ]
    )
    witnesses_by_id = {subject.id: subject for subject in master.subjects}
    for entry in master.collision_register:
        for witness_id in entry.witness_subject_ids:
            witness = witnesses_by_id[witness_id]
            lines.append(
                "| "
                + " | ".join(
                    [
                        _cell(entry.title),
                        _cell(f"{witness.raw_raga} / {witness.raw_tala}"),
                        _cell(witness.id),
                        _cell(witness.import_status),
                    ]
                )
                + " |"
            )
    inventory = master.source_inventory
    outside_rows = 0
    if inventory is not None:
        dispositions = Counter(row.disposition for row in inventory.rows)
        outside_rows = inventory.row_count - dispositions[InventoryDisposition.CLASSIFICATION_SUBJECT]
        lines.extend(
            [
                "",
                "## Source inventory",
                "",
                f"`{inventory.path}` (SHA-256 `{inventory.sha256}`) has {inventory.row_count} rows. "
                "Each row is matched to a subject by exact URL. Row numbers are locators, not identities.",
                "",
                "| Disposition | CSV rows |",
                "|:---|---:|",
                *[f"| {disposition.value} | {dispositions[disposition]} |" for disposition in InventoryDisposition],
                "",
                "Rows that are not Tyagaraja classification subjects:",
                "",
                "| Row | CSV title | CSV raga | Disposition | Mapped identity | Note |",
                "|---:|:---|:---|:---|:---|:---|",
            ]
        )
        for row in inventory.rows:
            if row.disposition == InventoryDisposition.CLASSIFICATION_SUBJECT:
                continue
            mapped = f"{row.mapped_composer} `{row.mapped_krithi_id}`" if row.mapped_krithi_id else ""
            lines.append(
                f"| {row.row} | {_cell(row.title)} | {_cell(row.raw_raga)} | {row.disposition.value} | "
                f"{_cell(mapped)} | {_cell(row.note or '')} |"
            )
    lines.extend(
        [
            "",
            "## External gap",
            "",
            "This figure is not a list of missing compositions. No titles are invented for it.",
            "",
            "| Label | Reported count | Status |",
            "|:---|---:|:---|",
        ]
    )
    for gap in master.external_gap:
        lines.append(f"| {_cell(gap.label)} | {gap.reported_count} | {gap.status} |")

    # Formal & Kshetra Group Annex
    group_rows: list[tuple[str, ClassificationSubject, ClassificationClaim]] = []
    for subject in master.subjects:
        for claim in subject.claims:
            if claim.axis == ClaimAxis.GROUP and claim.value is not None:
                group_rows.append((claim.value, subject, claim))
    if group_rows:
        lines.extend(
            [
                "",
                "## Formal and kshetra group annex",
                "",
                "Aradhana performance order (`1..5`: Nāṭṭai, Gowla, Ārabhi, Varāli, Sri) is recorded as a "
                "concert convention, distinct from any composer-declared sequence. Kshetra Pancharatnam leads "
                "require consultation of named print editions before canonical seeding.",
                "",
                "| Group / Cycle | Order | Subject | Title | Canonical raga / tala | State | Locator / Note |",
                "|:---|---:|:---|:---|:---|:---|:---|",
            ]
        )
        for group_val, subject, claim in sorted(
            group_rows, key=lambda x: (x[0], x[2].performance_order or 99, x[1].title.casefold())
        ):
            order_str = str(claim.performance_order) if claim.performance_order is not None else "—"
            lines.append(
                "| "
                + " | ".join(
                    [
                        _cell(group_val),
                        order_str,
                        _cell(subject.id),
                        _cell(subject.title),
                        _cell(f"{subject.canonical_raga} / {subject.canonical_tala}"),
                        _cell(claim.review_state.value),
                        _cell(f"{claim.locator or ''}: {claim.uncertainty or ''}".strip(": ")),
                    ]
                )
                + " |"
            )

    # Ritual & Congregational Annex
    ritual_rows: list[tuple[ClassificationSubject, ClassificationClaim]] = []
    for subject in master.subjects:
        for claim in subject.claims:
            if claim.axis == ClaimAxis.RITUAL and claim.value is not None:
                ritual_rows.append((subject, claim))
    if ritual_rows:
        lines.extend(
            [
                "",
                "## Ritual and congregational annex",
                "",
                "Utsava Sampradaya and Divyanama Sankirtana memberships and ritual functions "
                "(awakening, invitation, court, marriage, lullaby, mangalam) are recorded per witness without "
                "imposing a single universal bhajana paddhati order.",
                "",
                "| Collection | Ritual function | Subject | Title | Raga / Tala | State | Note |",
                "|:---|:---|:---|:---|:---|:---|:---|",
            ]
        )
        for subject, claim in sorted(ritual_rows, key=lambda x: (x[1].value or "", x[0].title.casefold())):
            lines.append(
                "| "
                + " | ".join(
                    [
                        _cell(claim.value or ""),
                        _cell(claim.ritual_function or "—"),
                        _cell(subject.id),
                        _cell(subject.title),
                        _cell(f"{subject.canonical_raga} / {subject.canonical_tala}"),
                        _cell(claim.review_state.value),
                        _cell(claim.uncertainty or ""),
                    ]
                )
                + " |"
            )

    # Dramatic Works Annex
    dramatic_rows: list[tuple[ClassificationSubject, ClassificationClaim]] = []
    for subject in master.subjects:
        for claim in subject.claims:
            if claim.axis == ClaimAxis.DRAMATIC and claim.value is not None:
                dramatic_rows.append((subject, claim))
    if dramatic_rows:
        lines.extend(
            [
                "",
                "## Dramatic works annex",
                "",
                "*Nauka Charitram* and *Prahlada Bhakti Vijayam* constituent songs carry work, item type, "
                "speaker, addressee, and narrative division on the claim. Dramatic dialogue is not indexed as "
                "autobiographical statement, and padya/vacana prose passages are not invented as `KRITHI` rows.",
                "",
                "| Work | Item type | Speaker -> Addressee | Division | Subject | Title | Raga / Tala | State |",
                "|:---|:---|:---|:---|:---|:---|:---|:---|",
            ]
        )
        for subject, claim in sorted(
            dramatic_rows, key=lambda x: (x[1].dramatic_work or x[1].value or "", x[0].title.casefold())
        ):
            speaker_pair = f"{claim.dramatic_speaker or '—'} -> {claim.dramatic_addressee or '—'}"
            lines.append(
                "| "
                + " | ".join(
                    [
                        _cell(claim.dramatic_work or claim.value or ""),
                        _cell(claim.dramatic_item_type.value if claim.dramatic_item_type else "—"),
                        _cell(speaker_pair),
                        _cell(claim.dramatic_division or "—"),
                        _cell(subject.id),
                        _cell(subject.title),
                        _cell(f"{subject.canonical_raga} / {subject.canonical_tala}"),
                        _cell(claim.review_state.value),
                    ]
                )
                + " |"
            )

    # Transmission & Desadi Tala Annex
    desadi_subjects = [subject for subject in master.subjects if subject.tala_witness_note]
    if desadi_subjects:
        lines.extend(
            [
                "",
                "## Transmission and Desadi tala annex",
                "",
                "Eleven source witnesses carry raw tala `Desadi` while stored canonically as `Adi`. "
                "Preserving the witness label prevents loss of the 3/4-akshara after-samam eduppu tradition.",
                "",
                "| Subject | Title | Canonical raga | Canonical tala | Raw tala | Witness note |",
                "|:---|:---|:---|:---|:---|:---|",
            ]
        )
        for subject in desadi_subjects:
            lines.append(
                "| "
                + " | ".join(
                    [
                        _cell(subject.id),
                        _cell(subject.title),
                        _cell(subject.canonical_raga),
                        _cell(subject.canonical_tala),
                        _cell(subject.raw_tala),
                        _cell(subject.tala_witness_note or ""),
                    ]
                )
                + " |"
            )

    # Distributions
    deity_counts = Counter(
        claim.value or "UNASSIGNED"
        for subject in master.subjects
        for claim in subject.claims
        if claim.axis == ClaimAxis.DEITY
    )
    theme_counts = Counter(
        claim.value
        for subject in master.subjects
        for claim in subject.claims
        if claim.axis == ClaimAxis.THEME and claim.value is not None
    )
    geo_counts = Counter(subject.geography_state.value for subject in master.subjects)
    lines.extend(
        [
            "",
            "## Distributions",
            "",
            "### Addressee / deity claims (across 684 subjects)",
            "",
            "| Addressee / Deity | Subjects |",
            "|:---|---:|",
            *[f"| {_cell(k)} | {v} |" for k, v in sorted(deity_counts.items(), key=lambda x: (-x[1], x[0]))],
            "",
            "### Devotional and musicological themes",
            "",
            "| Theme | Claims |",
            "|:---|---:|",
            *(
                [f"| {_cell(k)} | {v} |" for k, v in sorted(theme_counts.items(), key=lambda x: (-x[1], x[0]))]
                if theme_counts
                else ["| None assigned | 0 |"]
            ),
            "",
            "### Geography states (across 684 subjects)",
            "",
            "| Geography state | Subjects |",
            "|:---|---:|",
            *[f"| {_cell(k)} | {v} |" for k, v in sorted(geo_counts.items())],
        ]
    )

    if master.witnesses:
        lines.extend(
            [
                "",
                "## Witness bibliography and provenance register",
                "",
                "| Witness ID | Status | SHA-256 | Source / Description |",
                "|:---|:---|:---|:---|",
            ]
        )
        for w in sorted(master.witnesses, key=lambda x: x.id):
            sha_display = f"`{w.sha256[:16]}…`" if w.sha256 else "unavailable"
            lines.append(
                f"| {_cell(w.id)} | {w.status.value} | {sha_display} | {_cell(f'{w.url} — {w.description}')} |"
            )

    ghana_unreviewed = sum(
        1
        for subject in master.subjects
        for claim in subject.claims
        if claim.axis == ClaimAxis.GROUP
        and claim.value == GHANA_GROUP_VALUE
        and claim.review_state == ReviewState.UNREVIEWED
    )
    ghana_reviewed = sum(
        1
        for subject in master.subjects
        for claim in subject.claims
        if claim.axis == ClaimAxis.GROUP
        and claim.value == GHANA_GROUP_VALUE
        and claim.review_state == ReviewState.REVIEWED
    )
    lines.extend(
        [
            "",
            "## Unresolved register",
            "",
            f"- {accounting.collision_identities} collapsed identities stay `collision_not_classified` "
            "(their 17 source witnesses are classified individually and await canon split remediation).",
            *(
                [
                    f"- {outside_rows} CSV rows are outside the Tyagaraja catalogue. "
                    "Their composer and identity stay unresolved.",
                ]
                if outside_rows
                else []
            ),
            *(
                [
                    f"- {ghana_reviewed} Ghana-raga Pancharatnam memberships are `reviewed` and human-approved "
                    "with Aradhana concert order recorded separately from composer sequence.",
                    f"- {len(deferred_claims)} non-cycle approved claims are marked `application: deferred` "
                    "pending a non-cycle attribute application path.",
                    f"- {len(unresolved_claims)} source-backed claims remain `unresolved` pending multi-edition "
                    "comparison.",
                    f"- {len(unreviewed_leads)} claims on Kovur, Tiruvottiyur, Lalgudi, Srirangam, Utsava Sampradaya, "
                    "Divyanama, *Nauka Charitram*, and *Prahlada Bhakti Vijayam* remain `unreviewed` leads "
                    "attributed to unconsulted print editions.",
                ]
                if reviewed_claims
                else [
                    "- Deity, geography, theme, ritual, and dramatic claims are `unreviewed` on every subject.",
                    f"- {ghana_unreviewed} Ghana-raga Pancharatnam group claims are automated suggestions "
                    "from the title-heuristic tag. They use Aradhana performance order and are not approved "
                    "memberships.",
                    "- Kovur, Tiruvottiyur, Lalgudi, Srirangam, Utsava Sampradaya, Divyanama, "
                    "Nauka Charitram, and Prahlada Bhakti Vijayam are not assigned.",
                ]
            ),
            f"- {_identity_count(master, 'missing_pallavi')} identities have no pallavi section and "
            f"{_identity_count(master, 'missing_charanam')} have no charanam. Lyric text was not invented.",
            f"- {sum(1 for subject in master.subjects if subject.tala_witness_note)} subjects preserve a `desadi` "
            "source tala stored as Adi.",
            "",
            "## Concordance",
            "",
            "| Subject | Kind | Title | Canonical raga | Canonical tala | Raw raga | Raw tala | "
            "Deity / Geo | Themes / Groups | Status | Audit |",
            "|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|:---|",
        ]
    )
    for subject in master.subjects:
        kind = "identity" if subject.subject_kind == SubjectKind.CATALOGUE_IDENTITY else "witness"
        audit = ", ".join(subject.structure_audit)
        deity_claim = next((c for c in subject.claims if c.axis == ClaimAxis.DEITY), None)
        deity_label = (deity_claim.value if deity_claim and deity_claim.value else "UNASSIGNED") + (
            f" ({deity_claim.review_state.value})" if deity_claim else ""
        )
        geo_label = subject.geography_state.value
        themes = [c.value for c in subject.claims if c.axis == ClaimAxis.THEME and c.value]
        groups = [
            f"{c.axis.value}:{c.value}"
            for c in subject.claims
            if c.axis in {ClaimAxis.GROUP, ClaimAxis.RITUAL, ClaimAxis.DRAMATIC} and c.value
        ]
        summary_tags = "; ".join([*(themes or []), *(groups or [])]) or "—"
        lines.append(
            "| "
            + " | ".join(
                [
                    _cell(subject.id),
                    kind,
                    _cell(subject.title),
                    _cell(subject.canonical_raga),
                    _cell(subject.canonical_tala),
                    _cell(subject.raw_raga),
                    _cell(subject.raw_tala),
                    _cell(f"{deity_label} / {geo_label}"),
                    _cell(summary_tags),
                    _cell(subject.import_status),
                    _cell(audit),
                ]
            )
            + " |"
        )
    lines.append("")
    return "\n".join(lines)


def load_master(path: Path) -> TyagarajaClassificationMaster:
    master = TyagarajaClassificationMaster.model_validate_json(path.read_text(encoding="utf-8"))
    validate_master(master)
    return master


def dump_master(master: TyagarajaClassificationMaster) -> str:
    payload = master.model_dump(mode="json", exclude_none=True)
    # Preserve explicit nulls on core subject/claim fields for readability and schema stability
    raw_payload = master.model_dump(mode="json")
    for subj_raw, subj_clean in zip(raw_payload["subjects"], payload["subjects"], strict=True):
        for k in ("krithi_id", "mapped_krithi_id", "evidence_id", "tala_witness_note"):
            subj_clean[k] = subj_raw[k]
        for c_raw, c_clean in zip(subj_raw["claims"], subj_clean["claims"], strict=True):
            for ck in (
                "value",
                "witness_id",
                "locator",
                "excerpt",
                "evidence_kind",
                "uncertainty",
                "reviewer_decision",
            ):
                c_clean[ck] = c_raw[ck]
    return json.dumps(payload, indent=2, ensure_ascii=False) + "\n"


def load_manifest(path: Path) -> ApplicationManifest:
    return ApplicationManifest.model_validate_json(path.read_text(encoding="utf-8"))


def _external_gap() -> list[ExternalGap]:
    return [
        ExternalGap(
            label="track093_target_692",
            reported_count=692,
            status="unresolved_external",
            note="TRACK-093 Tyagaraja target. Not a list of missing compositions.",
        ),
    ]


def _settings_disagree(group: list[ImportCensusRow]) -> bool:
    ragas = {_normalise(row.raw_raga) for row in group}
    talas = {_normalise(row.raw_tala) for row in group}
    return len(ragas) > 1 or len(talas) > 1


def _normalise(value: str) -> str:
    return " ".join(value.split()).casefold()


def _section_flags(row: ImportCensusRow) -> SectionFlags:
    return SectionFlags(
        pallavi_count=row.pallavi_count,
        anupallavi_count=row.anupallavi_count,
        charanam_count=row.charanam_count,
        swara_sahitya_count=row.swara_sahitya_count,
    )


def _structure_audit(flags: SectionFlags) -> list[str]:
    audit: list[str] = []
    if flags.pallavi_count == 0:
        audit.append("missing_pallavi")
    if flags.charanam_count == 0:
        audit.append("missing_charanam")
    return audit


def _tala_note(row: ImportCensusRow) -> str | None:
    if _normalise(row.raw_tala) != "desadi":
        return None
    return f"Source raw_tala is {row.raw_tala}; canonical tala is {row.canonical_tala}."


def _claims_for(subject_id: str, row: ImportCensusRow, *, allow_group_candidate: bool) -> list[ClassificationClaim]:
    claims = [
        ClassificationClaim(
            id=f"{subject_id}:{axis.value}",
            axis=axis,
            witness_id=row.import_id,
            review_state=ReviewState.UNREVIEWED,
            uncertainty=_axis_uncertainty(axis, row),
        )
        for axis in ClaimAxis
    ]
    if allow_group_candidate and row.pancharatnam_tag:
        _apply_ghana_candidate(claims, row)
    return claims


def _axis_uncertainty(axis: ClaimAxis, row: ImportCensusRow) -> str | None:
    if axis == ClaimAxis.DEITY and row.has_deity:
        return "A deity foreign key is populated. It is not a reviewed addressee."
    if axis == ClaimAxis.GEOGRAPHY and row.has_temple:
        return "A temple foreign key is populated. It is not a reviewed shrine."
    if axis == ClaimAxis.GEOGRAPHY and not row.has_temple:
        return "Empty temple_id is UNREVIEWED, not a no-kshetra decision."
    if axis == ClaimAxis.DEITY and not row.has_deity:
        return "No deity is assigned. Rama is not the default."
    return None


def _apply_ghana_candidate(claims: list[ClassificationClaim], row: ImportCensusRow) -> None:
    claim = next(claim for claim in claims if claim.axis == ClaimAxis.GROUP)
    order = GHANA_RAGA_PERFORMANCE_ORDER.get(row.canonical_raga)
    if order is None:
        claim.uncertainty = (
            "Heuristic tyagaraja-pancharatnam tag is present, but the canonical raga is not "
            "Nattai, Gowla, Arabhi, Varali, or Sri. Left unassigned."
        )
        return
    claim.value = GHANA_GROUP_VALUE
    claim.evidence_kind = EvidenceKind.AUTOMATED_SUGGESTION
    claim.locator = GHANA_LOCATOR
    claim.uncertainty = (
        f"Candidate only. Aradhana performance order {order} "
        "(Nattai, Gowla, Arabhi, Varali, Sri) is a concert convention, not a composer sequence. "
        "Not an approved membership."
    )


def _catalogue_subject(row: ImportCensusRow) -> ClassificationSubject:
    subject_id = catalogue_subject_id(row.krithi_id)
    flags = _section_flags(row)
    return ClassificationSubject(
        id=subject_id,
        subject_kind=SubjectKind.CATALOGUE_IDENTITY,
        krithi_id=row.krithi_id,
        import_id=row.import_id,
        evidence_id=row.evidence_id,
        title=row.title,
        canonical_raga=row.canonical_raga,
        canonical_tala=row.canonical_tala,
        raw_raga=row.raw_raga,
        raw_tala=row.raw_tala,
        source_url=row.source_url,
        import_status=row.import_status,
        primary_language=row.primary_language,
        musical_form=row.musical_form,
        workflow_state=row.workflow_state,
        is_ragamalika=row.is_ragamalika,
        geography_state=GeographyState.UNREVIEWED,
        deity_state="UNREVIEWED",
        section_flags=flags,
        structure_audit=_structure_audit(flags),
        tala_witness_note=_tala_note(row),
        claims=_claims_for(subject_id, row, allow_group_candidate=True),
    )


def _witness_subject(row: ImportCensusRow) -> ClassificationSubject:
    subject_id = witness_subject_id(row.import_id)
    flags = _section_flags(row)
    return ClassificationSubject(
        id=subject_id,
        subject_kind=SubjectKind.SOURCE_WITNESS,
        mapped_krithi_id=row.krithi_id,
        import_id=row.import_id,
        evidence_id=row.evidence_id,
        title=row.title,
        canonical_raga=row.canonical_raga,
        canonical_tala=row.canonical_tala,
        raw_raga=row.raw_raga,
        raw_tala=row.raw_tala,
        source_url=row.source_url,
        import_status=row.import_status,
        primary_language=row.primary_language,
        musical_form=row.musical_form,
        workflow_state=row.workflow_state,
        is_ragamalika=row.is_ragamalika,
        geography_state=GeographyState.UNREVIEWED,
        deity_state="UNREVIEWED",
        section_flags=flags,
        structure_audit=_structure_audit(flags),
        tala_witness_note=_tala_note(row),
        claims=_claims_for(subject_id, row, allow_group_candidate=False),
    )


def _validate_subject_claims(
    subject: ClassificationSubject,
    collision_ids: set[str],
    witnesses_by_id: dict[str, Witness],
) -> None:
    if subject.subject_kind == SubjectKind.CATALOGUE_IDENTITY:
        if subject.id != catalogue_subject_id(subject.krithi_id or ""):
            raise TyagarajaClassificationError(f"catalogue subject id drifted for {subject.id}")
    elif subject.id != witness_subject_id(subject.import_id):
        raise TyagarajaClassificationError(f"witness subject id drifted for {subject.id}")
    if subject.mapped_krithi_id in collision_ids and subject.krithi_id is not None:
        raise TyagarajaClassificationError(f"{subject.id} attaches claims to a collapsed krithi")

    if len(subject.claims) < len(ClaimAxis):
        raise TyagarajaClassificationError(f"{subject.id} is missing an unreviewed claim axis")
    first_six_axes = [claim.axis for claim in subject.claims[: len(ClaimAxis)]]
    if first_six_axes != list(ClaimAxis):
        raise TyagarajaClassificationError(f"{subject.id} primary claims are not in ClaimAxis order")

    claim_ids = [claim.id for claim in subject.claims]
    if len(claim_ids) != len(set(claim_ids)):
        raise TyagarajaClassificationError(f"{subject.id} has duplicate claim ids")

    for idx, claim in enumerate(subject.claims):
        expected_prefix = f"{subject.id}:{claim.axis.value}"
        if idx < len(ClaimAxis):
            if claim.id != expected_prefix:
                raise TyagarajaClassificationError(f"claim id drifted for {claim.id}")
        else:
            if not claim.id.startswith(f"{expected_prefix}:"):
                raise TyagarajaClassificationError(f"secondary claim id drifted for {claim.id}")
        if claim.is_seedable:
            has_approval = bool(claim.reviewer_decision) or (
                claim.approval is not None and claim.approval.decision == "approve"
            )
            if not has_approval:
                raise TyagarajaClassificationError(f"seedable claim {claim.id} lacks human approval")
            if witnesses_by_id:
                w = witnesses_by_id.get(claim.witness_id or "")
                if w is None or w.status != WitnessStatus.FETCHED or not w.sha256:
                    raise TyagarajaClassificationError(
                        f"seedable claim {claim.id} requires a hashed fetched witness in master.witnesses"
                    )
        elif witnesses_by_id and claim.witness_id in witnesses_by_id:
            w = witnesses_by_id[claim.witness_id]
            if w.status != WitnessStatus.FETCHED and claim.review_state == ReviewState.REVIEWED:
                raise TyagarajaClassificationError(
                    f"claim {claim.id} cannot be reviewed from non-fetched witness {w.id} ({w.status})"
                )

    derived_geo = derive_geography_state(subject.claims)
    if subject.geography_state != derived_geo:
        raise TyagarajaClassificationError(
            f"{subject.id} geography_state {subject.geography_state} contradicts its claims ({derived_geo})"
        )


def _identity_count(master: TyagarajaClassificationMaster, flag: str) -> int:
    identities = {
        subject.krithi_id or subject.mapped_krithi_id for subject in master.subjects if flag in subject.structure_audit
    }
    identities.discard(None)
    return len(identities)


def _cell(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ")
