"""Owner decisions on unreviewed classification leads (TRACK-148 Syama Sastri, TRACK-149 Tyagaraja).

A proposal file records what an agent recommends. Nothing in it is an approval. A decision file is made
from a proposal file by a named human (the reviewer) and is the only input that can move a lead to
`reviewed`. Approvals made here are always `application: deferred`: the non-cycle application path
(TRACK-148 design option D1) and any membership seed are separate work, so nothing here reaches the
catalogue.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator

from src.musicological_classification import Claim, derive_geography_state
from src.tyagaraja_classification import (
    Approval as TyagarajaApproval,
)
from src.tyagaraja_classification import (
    EvidenceKind,
    ReviewState,
    TyagarajaClassificationMaster,
)
from src.tyagaraja_classification import (
    derive_geography_state as derive_tyagaraja_geography_state,
)

UnresolvedReason = Literal["edition_inaccessible", "no_public_source", "sources_conflict"]

DEFERRAL_REASONS = {
    "group": "Kshetra Pancharatnam membership needs a reviewed seed (R__seed successor); not yet written.",
    "default": "Non-cycle claim: no application path exists until the TRACK-148 D1 search-metadata path is built.",
}


class LeadDecision(BaseModel):
    model_config = ConfigDict(extra="forbid")

    claim_id: str
    disposition: Literal["approve", "unresolved", "disputed"]
    unresolved_reason: UnresolvedReason | None = None
    witness_id: str | None = None
    locator: str | None = None
    note: str = Field(min_length=1)
    # An approval is `deferred` until its application path exists; the owner-visible lift is recorded here so the
    # generator (which rebuilds the master from this file) keeps it.
    application: Literal["apply", "deferred"] = "deferred"
    lift_reason: str | None = None

    @model_validator(mode="after")
    def complete(self) -> LeadDecision:
        if self.application == "apply" and not (self.disposition == "approve" and self.lift_reason):
            raise ValueError(f"{self.claim_id}: lifting a deferral needs an approval and a reason")
        if self.disposition == "approve":
            if not (self.witness_id and self.locator):
                raise ValueError(f"{self.claim_id}: approval needs a witness and an exact locator")
            if self.unresolved_reason:
                raise ValueError(f"{self.claim_id}: an approved claim has no unresolved reason")
        elif not self.unresolved_reason:
            raise ValueError(f"{self.claim_id}: {self.disposition} needs a reason")
        return self


class LeadDecisionFile(BaseModel):
    model_config = ConfigDict(extra="forbid")

    reviewer: str = Field(min_length=1)
    accepted_at: str = Field(min_length=1)
    decisions: list[LeadDecision]

    @model_validator(mode="after")
    def unique_claims(self) -> LeadDecisionFile:
        ids = [d.claim_id for d in self.decisions]
        if len(ids) != len(set(ids)):
            raise ValueError("a claim has more than one decision")
        return self


def decisions_from_proposals(
    proposals: dict[str, dict[str, Any]], reviewer: str, accepted_at: str, excluded: set[str] | None = None
) -> LeadDecisionFile:
    """Turn proposals into decisions made by `reviewer`. Excluded claims get no decision and stay unreviewed."""
    excluded = excluded or set()
    unknown = excluded - proposals.keys()
    if unknown:
        raise ValueError(f"excluded claims are not in the proposals: {sorted(unknown)}")
    decisions = []
    for claim_id, p in proposals.items():
        if claim_id in excluded:
            continue
        decisions.append(
            LeadDecision(
                claim_id=claim_id,
                disposition=p["disposition"],
                unresolved_reason=p.get("unresolved_reason"),
                witness_id=p.get("witness_id"),
                locator=p.get("locator"),
                note=p["note"],
            )
        )
    return LeadDecisionFile(reviewer=reviewer, accepted_at=accepted_at, decisions=decisions)


def load_decisions(path: Path) -> LeadDecisionFile:
    return LeadDecisionFile.model_validate(json.loads(path.read_text(encoding="utf-8")))


def _deferral(axis: str) -> str:
    return DEFERRAL_REASONS.get(axis, DEFERRAL_REASONS["default"])


def apply_to_raw_syama(raw: dict[str, Any], decisions: LeadDecisionFile) -> int:
    """Apply decisions to the raw Syama master dict. Only `unreviewed` claims may be decided."""
    claims = {c["id"]: c for c in raw["claims"]}
    for d in decisions.decisions:
        claim = claims.get(d.claim_id)
        if claim is None:
            raise ValueError(f"{d.claim_id}: no such claim")
        if claim["review_state"] != "unreviewed":
            raise ValueError(f"{d.claim_id}: only an unreviewed lead can be decided (now {claim['review_state']})")
        if d.disposition == "approve":
            claim.update(
                review_state="reviewed",
                witness_id=d.witness_id,
                locator=d.locator,
                uncertainty=f"Approved from a public witness; {d.note}",
                approval={
                    "reviewer": decisions.reviewer,
                    "accepted_at": decisions.accepted_at,
                    "decision": "approve",
                    "note": d.note,
                    "application": "deferred",
                    "deferral_reason": _deferral(claim["axis"]),
                },
            )
        else:
            claim.update(
                review_state=d.disposition,
                unresolved_reason=d.unresolved_reason,
                uncertainty=f"{decisions.accepted_at}: {d.note}",
            )
    by_subject: dict[str, list[Claim]] = {}
    for c in raw["claims"]:
        by_subject.setdefault(c["subject_id"], []).append(Claim.model_validate(c))
    for composition in raw["compositions"]:
        composition["geography_state"] = derive_geography_state(by_subject.get(composition["id"], []))
    return len(decisions.decisions)


def apply_to_tyagaraja(master: TyagarajaClassificationMaster, decisions: LeadDecisionFile) -> int:
    """Apply decisions to a Tyagaraja master in memory. Only `unreviewed` claims may be decided."""
    claims = {c.id: c for s in master.subjects for c in s.claims}
    for d in decisions.decisions:
        claim = claims.get(d.claim_id)
        if claim is None:
            raise ValueError(f"{d.claim_id}: no such claim")
        if claim.review_state != ReviewState.UNREVIEWED:
            raise ValueError(f"{d.claim_id}: only an unreviewed lead can be decided (now {claim.review_state})")
        if d.disposition == "approve":
            claim.review_state = ReviewState.REVIEWED
            claim.witness_id = d.witness_id
            claim.locator = d.locator
            claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
            claim.uncertainty = f"Approved from a public witness; {d.note}"
            claim.reviewer_decision = d.note
            claim.approval = TyagarajaApproval(
                reviewer=decisions.reviewer,
                accepted_at=decisions.accepted_at,
                decision="approve",
                note=d.note if d.application == "deferred" else f"{d.note} Deferral lifted: {d.lift_reason}",
                application=d.application,
                deferral_reason=_deferral(claim.axis.value) if d.application == "deferred" else None,
            )
        else:
            claim.review_state = ReviewState.UNRESOLVED if d.disposition == "unresolved" else ReviewState.DISPUTED
            claim.unresolved_reason = d.unresolved_reason
            claim.uncertainty = f"{decisions.accepted_at}: {d.note}"
    for subject in master.subjects:
        subject.geography_state = derive_tyagaraja_geography_state(subject.claims)
    return len(decisions.decisions)


def lift_deferral_raw_syama(raw: dict[str, Any], claim_ids: list[str], reason: str) -> int:
    """Move approved Syama claims from `application: deferred` to `apply`.

    The approver, date and decision note stay as the reviewer recorded them. Only the application mode changes,
    and the reason is appended to the note so the change is visible on the claim itself.
    """
    claims = {c["id"]: c for c in raw["claims"]}
    for claim_id in claim_ids:
        claim = claims.get(claim_id)
        if claim is None:
            raise ValueError(f"{claim_id}: no such claim")
        approval = claim.get("approval")
        if claim["review_state"] != "reviewed" or not approval or approval.get("application") != "deferred":
            raise ValueError(f"{claim_id}: only an approved, deferred claim can have its deferral lifted")
        approval["application"] = "apply"
        approval.pop("deferral_reason", None)
        approval["note"] = f"{approval['note']} Deferral lifted: {reason}"
    return len(claim_ids)
