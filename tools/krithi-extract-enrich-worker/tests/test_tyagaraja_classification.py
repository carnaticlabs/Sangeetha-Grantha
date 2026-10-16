"""TRACK-147 classification master: accounting, collisions, witnesses, formatter, and approvals."""

from __future__ import annotations

from pathlib import Path
from typing import Any

import pytest
from pydantic import ValidationError

from src.embeddings.context_formatter import MAX_HEADER_WORDS, format_composition_overview, format_cycle_overview
from src.embeddings.macro_indexer import reviewed_cycle_composer
from src.tyagaraja_classification import (
    GHANA_GROUP_VALUE,
    SNAPSHOT_DATE,
    Approval,
    ClaimAxis,
    ClassificationClaim,
    EvidenceKind,
    GeographyState,
    ImportCensusRow,
    ReviewState,
    SubjectKind,
    TyagarajaClassificationError,
    WitnessStatus,
    assert_track147_census,
    build_application_manifest,
    build_master,
    catalogue_subject_id,
    derive_geography_state,
    load_manifest,
    load_master,
    render_markdown,
    validate_master,
    verify_source,
)

ROOT = Path(__file__).resolve().parents[3]
MASTER_PATH = ROOT / "database" / "data" / "track147-tyagaraja-master.json"
MARKDOWN_PATH = (
    ROOT / "application_documentation" / "01-requirements" / "features" / "tyagaraja-classification-master.md"
)
MANIFEST_PATH = ROOT / "database" / "data" / "track147-tyagaraja-application-manifest.json"
TYAGARAJA_SLUG = "tyagaraja-pancharatnam"
TYAGARAJA_APPROVAL = {
    "decision": "approve",
    "reviewer": "Seshadri",
    "accepted_at": "2026-10-15",
    "note": "Approved canonical Ghana-raga Pancharatnam membership.",
}
TYAGARAJA_ATTRS = {
    "provenance_owner": "TRACK-147",
    "review_state": "reviewed",
    "claim_id": "tyagaraja:krithi:2e115182-dcc2-4a41-962c-8381821a7489:group",
    "performance_order": 1,
    "theme": "Rama Bhakti & Nama Mahima",
    "mudra": "Tyagaraja",
    "approval": TYAGARAJA_APPROVAL,
}


def _row(
    krithi_id: str,
    import_id: str,
    *,
    title: str = "Example",
    raw_raga: str = "Saveri",
    raw_tala: str = "Adi",
    canonical_raga: str = "Sāveri",
    pancharatnam_tag: bool = False,
) -> ImportCensusRow:
    return ImportCensusRow(
        krithi_id=krithi_id,
        title=title,
        primary_language="en",
        musical_form="KRITHI",
        workflow_state="published",
        is_ragamalika=False,
        canonical_raga=canonical_raga,
        canonical_tala="Adi",
        has_deity=False,
        has_temple=False,
        import_id=import_id,
        raw_raga=raw_raga,
        raw_tala=raw_tala,
        source_url=f"https://thyagaraja-vaibhavam.blogspot.com/{import_id}",
        import_status="approved",
        pancharatnam_tag=pancharatnam_tag,
        evidence_id=None,
        pallavi_count=1,
        anupallavi_count=1,
        charanam_count=1,
        swara_sahitya_count=0,
    )


def test_subject_ids_do_not_depend_on_input_order() -> None:
    rows = [
        _row("krithi-a", "import-a"),
        _row("krithi-b", "import-b", title="Second"),
    ]
    forward = build_master(rows, snapshot_date=SNAPSHOT_DATE)
    backward = build_master(list(reversed(rows)), snapshot_date=SNAPSHOT_DATE)
    assert [subject.id for subject in forward.subjects] == [subject.id for subject in backward.subjects]
    assert forward.subjects[0].id == catalogue_subject_id("krithi-a")


def test_disagreeing_imports_are_witnesses_not_one_identity() -> None:
    rows = [
        _row("krithi-jaya", "import-1", title="jaya mangaLaM", raw_raga="mOhanaM", raw_tala="Unknown"),
        _row("krithi-jaya", "import-2", title="jaya mangaLaM", raw_raga="ghaNTA", raw_tala="Jhampa"),
    ]
    master = build_master(rows, snapshot_date=SNAPSHOT_DATE)
    assert master.accounting.collision_identities == 1
    assert master.accounting.classification_subjects == 2
    assert all(subject.krithi_id != "krithi-jaya" for subject in master.subjects)
    assert {subject.raw_raga for subject in master.subjects} == {"mOhanaM", "ghaNTA"}

    forged = master.model_copy(deep=True)
    forged.collision_register[0].witness_subject_ids = []
    with pytest.raises(TyagarajaClassificationError):
        validate_master(forged)


def test_ghana_raga_candidate_in_skeleton_is_not_seedable() -> None:
    row = _row(
        "krithi-jagadananda",
        "import-jagadananda",
        title="jagadAnanda kAraka",
        canonical_raga="Nāṭṭai",
        pancharatnam_tag=True,
    )
    master = build_master([row], snapshot_date=SNAPSHOT_DATE)
    claim = next(claim for claim in master.subjects[0].claims if claim.axis == ClaimAxis.GROUP)
    assert claim.value == GHANA_GROUP_VALUE
    assert claim.review_state == ReviewState.UNREVIEWED
    assert claim.is_seedable is False


def test_reviewed_claim_without_human_approval_is_rejected() -> None:
    with pytest.raises(ValidationError):
        ClassificationClaim(
            id="tyagaraja:krithi:example:deity",
            axis=ClaimAxis.DEITY,
            value="Hanuman",
            locator="edition:page-1",
            excerpt="anjaneya",
            evidence_kind=EvidenceKind.TEXTUAL_EVIDENCE,
            review_state=ReviewState.REVIEWED,
        )


def test_automated_suggestion_cannot_be_reviewed() -> None:
    with pytest.raises(ValidationError):
        ClassificationClaim(
            id="tyagaraja:krithi:example:group",
            axis=ClaimAxis.GROUP,
            value=GHANA_GROUP_VALUE,
            locator="R__seed_07",
            evidence_kind=EvidenceKind.AUTOMATED_SUGGESTION,
            review_state=ReviewState.REVIEWED,
            reviewer_decision="Approved",
        )


def test_deferral_validation_rules() -> None:
    with pytest.raises(ValidationError):
        Approval(
            reviewer="Seshadri",
            accepted_at="2026-10-15",
            decision="approve",
            note="Approved",
            application="deferred",
        )
    with pytest.raises(ValidationError):
        Approval(
            reviewer="Seshadri",
            accepted_at="2026-10-15",
            decision="approve",
            note="Approved",
            application="apply",
            deferral_reason="Should not be present on apply",
        )
    valid = Approval(
        reviewer="Seshadri",
        accepted_at="2026-10-15",
        decision="approve",
        note="Approved",
        application="deferred",
        deferral_reason="Deferred to TRACK-149",
    )
    assert valid.application == "deferred"


def test_rama_deity_without_a_passage_is_rejected() -> None:
    with pytest.raises(ValidationError):
        ClassificationClaim(
            id="tyagaraja:krithi:example:deity",
            axis=ClaimAxis.DEITY,
            value="Rama",
            locator="edition:page-1",
            evidence_kind=EvidenceKind.TEXTUAL_EVIDENCE,
            review_state=ReviewState.UNREVIEWED,
        )


def test_tyagaraja_formatter_and_macro_gates() -> None:
    def header_line(attrs: dict[str, Any], composer: str = "Tyagaraja") -> str:
        return format_composition_overview(
            title="jagadAnanda kAraka",
            composer=composer,
            raga="Nāṭṭai",
            tala="Adi",
            musical_form="KRITHI",
            tags=["Tyagaraja Ghana Raga Pancharatnam", "tyagaraja-pancharatnam"],
            primary_lyrics="jagadAnanda kAraka jaya jAnakI prANa nAyaka",
            cycle_info={
                "slug": TYAGARAJA_SLUG,
                "display_name": "Tyagaraja Ghana Raga Pancharatnam",
                "role": "CORE",
                "attrs": attrs,
            },
        ).splitlines()[0]

    approved_line = header_line(TYAGARAJA_ATTRS)
    assert "Cycle: Tyagaraja Ghana Raga Pancharatnam" in approved_line
    assert "Aradhana Order: 1" in approved_line
    assert "Theme: Rama Bhakti & Nama Mahima" in approved_line
    assert "Mudra: Tyagaraja" in approved_line
    assert len(approved_line.split()) <= MAX_HEADER_WORDS

    # Unapproved or wrong-composer must abstain
    unapproved_line = header_line({**TYAGARAJA_ATTRS, "review_state": "unresolved"})
    assert "Pancharatnam" not in unapproved_line
    assert "Aradhana Order" not in unapproved_line
    assert "Pancharatnam" not in header_line(TYAGARAJA_ATTRS, composer="Muthuswami Dikshitar")

    # Macro indexer gates
    assert (
        reviewed_cycle_composer(
            "tyagaraja-vibhakti",
            [{"composer_name": "Muthuswami Dikshitar"}],
        )
        == "Muthuswami Dikshitar"
    )
    assert reviewed_cycle_composer(TYAGARAJA_SLUG, [{"composer_name": "Tyagaraja"}]) is None
    assert (
        reviewed_cycle_composer(
            TYAGARAJA_SLUG,
            [{"composer_name": "Tyagaraja", "discriminative_attributes": TYAGARAJA_ATTRS}],
        )
        == "Tyagaraja"
    )

    cycle_doc = format_cycle_overview(
        cycle_name="Tyagaraja Ghana Raga Pancharatnam",
        composer="Tyagaraja",
        description="The five Ghana-raga Pancharatnam compositions of Saint Tyagaraja.",
        members=[
            {
                "sequence_order": 1,
                "title": "jagadAnanda kAraka",
                "raga": "Nāṭṭai",
                "role": "CORE",
                "discriminative_attributes": TYAGARAJA_ATTRS,
            }
        ],
    )
    assert "[Cycle: Tyagaraja Ghana Raga Pancharatnam] [Composer: Tyagaraja]" in cycle_doc
    assert "Aradhana Order: 1" in cycle_doc


def test_live_master_accounts_for_every_source_row_and_witness() -> None:
    master = load_master(MASTER_PATH)
    assert_track147_census(master)
    verify_source(master, ROOT)
    rendered = render_markdown(master)
    assert rendered == MARKDOWN_PATH.read_text(encoding="utf-8")
    manifest = load_manifest(MANIFEST_PATH)
    assert manifest == build_application_manifest(master)
    assert manifest.status == "ready"
    assert len(manifest.changes) >= 15

    # Verify witness registry integrity
    assert master.database_snapshot_witness == "db-snapshot"
    witnesses_by_id = {w.id: w for w in master.witnesses}
    assert "db-snapshot" in witnesses_by_id
    assert witnesses_by_id["db-snapshot"].status == WitnessStatus.DATABASE_SNAPSHOT
    assert any(w.status == WitnessStatus.UNAVAILABLE for w in master.witnesses)

    collapsed = {entry.krithi_id for entry in master.collision_register}
    for subject in master.subjects:
        assert subject.krithi_id not in collapsed
        assert subject.geography_state == derive_geography_state(subject.claims)
        for claim in subject.claims:
            for krithi_id in collapsed:
                assert not claim.id.startswith(catalogue_subject_id(krithi_id))
            if claim.is_seedable:
                assert subject.subject_kind == SubjectKind.CATALOGUE_IDENTITY
                assert claim.witness_id in witnesses_by_id
                assert witnesses_by_id[claim.witness_id].status != WitnessStatus.UNAVAILABLE
                assert witnesses_by_id[claim.witness_id].sha256 is not None
