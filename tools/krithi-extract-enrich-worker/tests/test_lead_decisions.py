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
)
from src.musicological_classification import ClassificationMaster
from src.tyagaraja_classification import ReviewState, load_master, validate_master

ROOT = Path(__file__).resolve().parents[3]
DATA = ROOT / "database/data"
SYAMA = DATA / "track146-syama-sastri-master.json"
TYAGARAJA = DATA / "track147-tyagaraja-master.json"


def _proposals(name: str) -> dict[str, dict[str, Any]]:
    return json.loads((DATA / name).read_text(encoding="utf-8"))


def test_every_unreviewed_lead_has_exactly_one_proposal():
    syama = json.loads(SYAMA.read_text(encoding="utf-8"))
    unreviewed = {c["id"] for c in syama["claims"] if c["review_state"] == "unreviewed"}
    assert set(_proposals("track148-lead-proposals.json")) == unreviewed

    tyagaraja = load_master(TYAGARAJA)
    valued = {
        c.id for s in tyagaraja.subjects for c in s.claims if c.review_state == ReviewState.UNREVIEWED and c.value
    }
    assert set(_proposals("track149-lead-proposals.json")) == valued


def test_proposals_alone_approve_nothing():
    syama = ClassificationMaster.model_validate_json(SYAMA.read_text(encoding="utf-8"))
    assert sum(c.review_state == "reviewed" for c in syama.claims) == 18
    tyagaraja = load_master(TYAGARAJA)
    assert not any(
        c.review_state == ReviewState.REVIEWED and c.approval and c.approval.reviewer == "proposal"
        for s in tyagaraja.subjects
        for c in s.claims
    )


def test_syama_decisions_validate_and_defer_application():
    raw = json.loads(SYAMA.read_text(encoding="utf-8"))
    proposals = _proposals("track148-lead-proposals.json")
    decisions = decisions_from_proposals(proposals, "Test Reviewer", "2026-11-04")
    apply_to_raw_syama(raw, decisions)
    master = ClassificationMaster.model_validate(raw)
    approved = [c for c in master.claims if c.approval and c.approval.reviewer == "Test Reviewer"]
    assert len(approved) == sum(p["disposition"] == "approve" for p in proposals.values())
    assert all(c.approval and c.approval.application == "deferred" for c in approved)
    assert not any(c.review_state == "unreviewed" for c in master.claims)
    # Madurai shrine approvals move the composition's derived geography state
    assert any(s.geography_state == "SPECIFIC_SHRINE_SUPPORTED" for s in master.compositions)


def test_tyagaraja_decisions_validate_and_defer_application():
    master = load_master(TYAGARAJA)
    proposals = _proposals("track149-lead-proposals.json")
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
