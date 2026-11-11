"""Lead proposals recommend; only a named reviewer's decision file can approve (TRACK-148, TRACK-149)."""

import json
from copy import deepcopy
from pathlib import Path
from typing import Any

import pytest
from pydantic import ValidationError

from src.lead_decisions import (
    LeadDecision,
    LeadDecisionFile,
    apply_to_raw_syama,
    apply_to_tyagaraja,
    decisions_from_proposals,
    load_decisions,
)
from src.musicological_classification import ClassificationMaster
from src.tyagaraja_classification import (
    ReviewState,
    TyagarajaClassificationMaster,
    load_master,
    validate_master,
)

ROOT = Path(__file__).resolve().parents[3]
DATA = ROOT / "database/data"
SYAMA = DATA / "track146-syama-sastri-master.json"
TYAGARAJA = DATA / "track147-tyagaraja-master.json"


def _proposals(name: str) -> dict[str, dict[str, Any]]:
    return json.loads((DATA / name).read_text(encoding="utf-8"))


def _decisions(name: str) -> LeadDecisionFile:
    return load_decisions(DATA / name)


def _undecide_syama(raw: dict[str, Any], decided: set[str]) -> None:
    """Return decided Syama claims to their pre-decision state so the mechanics can be exercised again."""
    for claim in raw["claims"]:
        if claim["id"] in decided:
            claim.update(review_state="unreviewed", approval=None, unresolved_reason=None)


def _undecide_tyagaraja(master: TyagarajaClassificationMaster, decided: set[str]) -> None:
    for subject in master.subjects:
        for claim in subject.claims:
            if claim.id in decided:
                claim.review_state = ReviewState.UNREVIEWED
                claim.approval = None
                claim.reviewer_decision = None
                claim.unresolved_reason = None


def test_decision_files_cover_exactly_the_proposals():
    for proposals, decisions in (
        ("track148-lead-proposals.json", "track148-lead-decisions.json"),
        ("track149-lead-proposals.json", "track149-lead-decisions.json"),
    ):
        recorded = _decisions(decisions)
        assert {d.claim_id for d in recorded.decisions} == set(_proposals(proposals))
        assert recorded.reviewer == "Seshadri"


def test_masters_hold_exactly_the_recorded_decisions():
    syama = ClassificationMaster.model_validate_json(SYAMA.read_text(encoding="utf-8"))
    by_id = {c.id: c for c in syama.claims}
    for d in _decisions("track148-lead-decisions.json").decisions:
        claim = by_id[d.claim_id]
        if d.disposition == "approve":
            assert claim.review_state == "reviewed" and claim.approval is not None
            assert claim.approval.reviewer == "Seshadri"
            # A lifted deferral must say so on the claim; otherwise the claim is still deferred.
            lifted = claim.approval.application == "apply" and "Deferral lifted:" in claim.approval.note
            assert claim.approval.application == "deferred" or lifted
            assert (claim.witness_id, claim.locator) == (d.witness_id, d.locator)
        else:
            assert claim.review_state == d.disposition and claim.unresolved_reason == d.unresolved_reason
    assert not any(c.review_state == "unreviewed" for c in syama.claims)

    tyagaraja = load_master(TYAGARAJA)
    claims = {c.id: c for s in tyagaraja.subjects for c in s.claims}
    for d in _decisions("track149-lead-decisions.json").decisions:
        tclaim = claims[d.claim_id]
        if d.disposition == "approve":
            assert tclaim.review_state == ReviewState.REVIEWED and tclaim.approval is not None
            assert tclaim.approval.reviewer == "Seshadri" and tclaim.approval.application == d.application
        else:
            assert tclaim.review_state.value == d.disposition and tclaim.unresolved_reason == d.unresolved_reason
    assert not any(c.review_state == ReviewState.UNREVIEWED and c.value for c in claims.values())


def test_syama_mechanics_validate_and_defer_application():
    raw = json.loads(SYAMA.read_text(encoding="utf-8"))
    proposals = _proposals("track148-lead-proposals.json")
    _undecide_syama(raw, set(proposals))
    apply_to_raw_syama(raw, decisions_from_proposals(proposals, "Test Reviewer", "2026-11-04"))
    master = ClassificationMaster.model_validate(raw)
    approved = [c for c in master.claims if c.approval and c.approval.reviewer == "Test Reviewer"]
    assert len(approved) == sum(p["disposition"] == "approve" for p in proposals.values())
    assert all(c.approval and c.approval.application == "deferred" for c in approved)
    assert any(s.geography_state == "SPECIFIC_SHRINE_SUPPORTED" for s in master.compositions)


def test_tyagaraja_mechanics_validate_and_defer_application():
    master = load_master(TYAGARAJA)
    proposals = _proposals("track149-lead-proposals.json")
    _undecide_tyagaraja(master, set(proposals))
    apply_to_tyagaraja(master, decisions_from_proposals(proposals, "Test Reviewer", "2026-11-04"))
    validate_master(master)
    approved = [c for s in master.subjects for c in s.claims if c.approval and c.approval.reviewer == "Test Reviewer"]
    assert len(approved) == sum(p["disposition"] == "approve" for p in proposals.values())
    assert all(c.approval and c.approval.application == "deferred" for c in approved)


def test_excluded_claims_stay_unreviewed():
    proposals = _proposals("track148-lead-proposals.json")
    keep = next(k for k, v in proposals.items() if v["disposition"] == "approve")
    decisions = decisions_from_proposals(proposals, "Test Reviewer", "2026-11-04", {keep})
    raw = json.loads(SYAMA.read_text(encoding="utf-8"))
    _undecide_syama(raw, set(proposals))
    apply_to_raw_syama(raw, decisions)
    assert next(c for c in raw["claims"] if c["id"] == keep)["review_state"] == "unreviewed"
    with pytest.raises(ValueError):
        decisions_from_proposals(proposals, "Test Reviewer", "2026-11-04", {"SS-9999:none:1"})


def test_decision_rules():
    with pytest.raises(ValidationError):
        LeadDecision(claim_id="c", disposition="approve", witness_id="w", note="n")
    with pytest.raises(ValidationError):
        LeadDecision(claim_id="c", disposition="unresolved", note="n")
    with pytest.raises(ValidationError):
        LeadDecision(
            claim_id="c",
            disposition="approve",
            witness_id="w",
            locator="p",
            unresolved_reason="no_public_source",
            note="n",
        )
    ok = LeadDecision(claim_id="c", disposition="unresolved", unresolved_reason="no_public_source", note="n")
    with pytest.raises(ValidationError):
        LeadDecisionFile(reviewer="r", accepted_at="d", decisions=[ok, deepcopy(ok)])
    with pytest.raises(ValidationError):
        LeadDecisionFile(reviewer=" ".strip(), accepted_at="d", decisions=[ok])


def test_only_unreviewed_leads_can_be_decided():
    raw = json.loads(SYAMA.read_text(encoding="utf-8"))
    reviewed = next(c["id"] for c in raw["claims"] if c["review_state"] == "reviewed")
    decisions = LeadDecisionFile(
        reviewer="r",
        accepted_at="d",
        decisions=[
            LeadDecision(claim_id=reviewed, disposition="unresolved", unresolved_reason="no_public_source", note="n")
        ],
    )
    with pytest.raises(ValueError):
        apply_to_raw_syama(raw, decisions)


def test_lifted_decision_needs_a_reason():
    import pytest

    from src.lead_decisions import LeadDecision

    base = {"claim_id": "c", "disposition": "approve", "witness_id": "w", "locator": "l", "note": "n"}
    with pytest.raises(ValueError):
        LeadDecision(**base, application="apply")
    assert LeadDecision(**base, application="apply", lift_reason="seed built").application == "apply"
