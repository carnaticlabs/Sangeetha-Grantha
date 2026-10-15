"""TRACK-147 classification master: accounting, collisions, and unapproved claims."""

from __future__ import annotations

from pathlib import Path

import pytest
from pydantic import ValidationError

from src.tyagaraja_classification import (
    GHANA_GROUP_VALUE,
    SNAPSHOT_DATE,
    ApplicationManifest,
    ClaimAxis,
    ClassificationClaim,
    EvidenceKind,
    ImportCensusRow,
    ReviewState,
    TyagarajaClassificationError,
    assert_track147_census,
    build_master,
    catalogue_subject_id,
    load_manifest,
    load_master,
    render_markdown,
    validate_master,
)

ROOT = Path(__file__).resolve().parents[3]
MASTER_PATH = ROOT / "database" / "data" / "track147-tyagaraja-master.json"
MARKDOWN_PATH = (
    ROOT / "application_documentation" / "01-requirements" / "features" / "tyagaraja-classification-master.md"
)
MANIFEST_PATH = ROOT / "database" / "data" / "track147-tyagaraja-application-manifest.json"


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


def test_subject_ids_do_not_depend_on_input_order():
    rows = [
        _row("krithi-a", "import-a"),
        _row("krithi-b", "import-b", title="Second"),
    ]
    forward = build_master(rows, snapshot_date=SNAPSHOT_DATE)
    backward = build_master(list(reversed(rows)), snapshot_date=SNAPSHOT_DATE)
    assert [subject.id for subject in forward.subjects] == [subject.id for subject in backward.subjects]
    assert forward.subjects[0].id == catalogue_subject_id("krithi-a")


def test_disagreeing_imports_are_witnesses_not_one_identity():
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


def test_ghana_raga_candidate_is_not_seedable():
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


def test_reviewed_claim_without_human_approval_is_rejected():
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


def test_rama_deity_without_a_passage_is_rejected():
    with pytest.raises(ValidationError):
        ClassificationClaim(
            id="tyagaraja:krithi:example:deity",
            axis=ClaimAxis.DEITY,
            value="Rama",
            locator="edition:page-1",
            evidence_kind=EvidenceKind.TEXTUAL_EVIDENCE,
            review_state=ReviewState.UNREVIEWED,
        )


def test_live_master_accounts_for_every_source_row():
    master = load_master(MASTER_PATH)
    assert_track147_census(master)
    rendered = render_markdown(master)
    assert rendered == MARKDOWN_PATH.read_text(encoding="utf-8")
    manifest = load_manifest(MANIFEST_PATH)
    assert manifest == ApplicationManifest(track="TRACK-147", approvals=[])
    collapsed = {entry.krithi_id for entry in master.collision_register}
    for subject in master.subjects:
        assert subject.krithi_id not in collapsed
        for claim in subject.claims:
            assert not claim.is_seedable
            for krithi_id in collapsed:
                assert not claim.id.startswith(catalogue_subject_id(krithi_id))
