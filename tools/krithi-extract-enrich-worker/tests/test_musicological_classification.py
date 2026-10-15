"""Integrity and abstention proofs for the shared classification master."""

from copy import deepcopy
from pathlib import Path

import pytest
from pydantic import ValidationError

from src.musicological_classification import ClassificationMaster, RhythmObservation, load_master

ROOT = Path(__file__).resolve().parents[3]
MASTER = ROOT / "database/data/track146-syama-sastri-master.json"


def test_complete_accounting_and_no_unapproved_application():
    master = load_master(MASTER)
    master.verify_source(ROOT)
    assert len(master.rows) == 71
    assert len(master.compositions) == 70
    assert master.rows[0].subject_id == master.rows[21].subject_id
    manifest = master.application_manifest()
    assert manifest["status"] == "ready"
    assert len(manifest["changes"]) == 18
    assert sum(c.review_state == "reviewed" for c in master.claims) == 18
    assert sum(c.review_state == "unreviewed" for c in master.claims) == 40
    assert all(c.approval is not None for c in master.claims if c.review_state == "reviewed")
    assert all(c.approval is None for c in master.claims if c.review_state in ("unresolved", "unreviewed", "disputed"))
    assert len([w for w in master.witnesses if w.status == "fetched"]) == 78


@pytest.mark.parametrize(
    "mutation", ["duplicate_row", "missing_row", "orphan_claim", "wrong_composer", "duplicate_uuid"]
)
def test_broken_master_is_rejected(mutation):
    data = load_master(MASTER).model_dump(mode="json")
    if mutation == "duplicate_row":
        data["rows"][1]["row"] = 1
    elif mutation == "missing_row":
        data["rows"].pop()
    elif mutation == "orphan_claim":
        data["claims"][0]["subject_id"] = "missing"
    elif mutation == "wrong_composer":
        data["compositions"][0]["composer_id"] = "00000000-0000-0000-0000-000000000000"
    else:
        data["compositions"][1]["canonical_id"] = data["compositions"][0]["canonical_id"]
    with pytest.raises(ValidationError):
        ClassificationMaster.model_validate(data)


def test_approval_requires_human_and_traceable_witness():
    data = load_master(MASTER).model_dump(mode="json")
    data["claims"][0]["review_state"] = "reviewed"
    with pytest.raises(ValidationError, match="human approval"):
        ClassificationMaster.model_validate(data)
    data["claims"][0]["approval"] = {
        "reviewer": "Fixture curator",
        "accepted_at": "2026-10-01",
        "decision": "approve",
        "note": "Test only",
    }
    data["claims"][0]["evidence_kind"] = "automated_suggestion"
    with pytest.raises(ValidationError, match="automated-only"):
        ClassificationMaster.model_validate(data)
    data["claims"][0]["evidence_kind"] = "textual_evidence"
    witness_id = data["claims"][0]["witness_id"]
    witness = next(w for w in data["witnesses"] if w["id"] == witness_id)
    witness["status"] = "unavailable"
    witness["sha256"] = None
    with pytest.raises(ValidationError, match="hashed witness"):
        ClassificationMaster.model_validate(data)


def test_conflicting_approved_forms_are_rejected():
    data = load_master(MASTER).model_dump(mode="json")
    form = next(c for c in data["claims"] if c["axis"] == "form")
    form["review_state"] = "reviewed"
    form["approval"] = {
        "reviewer": "Fixture curator",
        "accepted_at": "2026-10-01",
        "decision": "approve",
        "note": "Test only",
    }
    other = deepcopy(form)
    other["id"] += ":conflict"
    other["value"] = "VARNAM" if form["value"] != "VARNAM" else "KRITHI"
    data["claims"].append(other)
    with pytest.raises(ValidationError, match="conflicting canonical form"):
        ClassificationMaster.model_validate(data)


def test_generation_and_identities_survive_input_reordering():
    master = load_master(MASTER)
    data = master.model_dump(mode="json")
    for name in ("rows", "compositions", "witnesses", "claims"):
        data[name].reverse()
    reordered = ClassificationMaster.model_validate(data)
    assert reordered.render() == master.render()
    assert reordered.application_manifest() == master.application_manifest()


def test_source_tala_does_not_invent_rhythm():
    assert RhythmObservation(source_tala="Capu").eduppu is None
    with pytest.raises(ValidationError, match="units, origin and convention"):
        RhythmObservation(eduppu=0.5)
    observation = RhythmObservation(eduppu=0.5, units="akshara", counting_origin="samam", convention="edition A")
    assert observation.eduppu == 0.5


def test_gitams_and_varnams_are_explicit_review_items():
    master = load_master(MASTER)
    claims = [c for c in master.claims if c.axis == "form"]
    assert sum(c.value == "VARNAM" for c in claims) == 3
    assert sum(c.value == "UNESTABLISHED" for c in claims) == 5
    alternate = [c for c in master.claims if c.axis == "related_setting"]
    assert len(alternate) == 1
    assert "not evidence of a Ragamalika" in alternate[0].summary


def test_checked_in_outputs_match_master():
    master = load_master(MASTER)
    path = ROOT / "application_documentation/01-requirements/features/syama-sastri-classification-master.md"
    assert path.read_text() == master.render()


def test_snapshot_hash_is_current():
    import hashlib

    master = load_master(MASTER)
    witness = next(w for w in master.witnesses if w.id == master.database_snapshot_witness)
    assert hashlib.sha256((ROOT / witness.url).read_bytes()).hexdigest() == witness.sha256


def test_retrieval_probes_require_exact_uuid_and_human_expected_identity():
    from scripts.evaluate_retrieval import evaluate_item, load_benchmarks

    benchmarks = load_benchmarks()
    # Unapproved probe SS-146-12 must not be loaded without human approval
    assert not any(item["id"] == "SS-146-12" for item in benchmarks)
    # Approved probes are loaded
    assert any(item["id"] == "SS-146-01" for item in benchmarks)
    item = {"expected_krithi_id": "expected", "expected_composer": "Syama Sastri"}
    assert evaluate_item(item, [{"id": "sibling", "composer": "Syama Sastri"}]) == (False, 0.0)
    assert evaluate_item(item, [{"id": "expected", "composer": "Syama Sastri"}]) == (True, 1.0)
