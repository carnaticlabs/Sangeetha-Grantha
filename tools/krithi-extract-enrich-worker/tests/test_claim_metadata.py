"""D1: approved non-cycle claims reach search headers through the manifests (TRACK-148)."""

import json
from pathlib import Path
from typing import Any

from src.embeddings.claim_metadata import claim_clauses, load_approved_claims
from src.embeddings.context_formatter import MAX_HEADER_WORDS, format_composition_overview, format_section_passage

ROOT = Path(__file__).resolve().parents[3]
KRITHI = "11111111-1111-1111-1111-111111111111"


def _manifest(tmp_path: Path, changes: list[dict[str, Any]], name: str = "m.json") -> Path:
    path = tmp_path / name
    path.write_text(json.dumps({"changes": changes}), encoding="utf-8")
    return path


def _change(claim_id: str, axis: str, value: str, **approval: Any) -> dict[str, Any]:
    base = {"reviewer": "Seshadri", "accepted_at": "2026-11-03", "decision": "approve", "note": "n"}
    return {
        "claim_id": claim_id,
        "canonical_id": KRITHI,
        "axis": axis,
        "value": value,
        "approval": {**base, **approval},
    }


def test_only_applicable_approved_claims_are_loaded(tmp_path):
    path = _manifest(
        tmp_path,
        [
            _change("SS-0001:mudra:1", "mudra", "Syama Krishna", application="apply"),
            _change("SS-0001:laya:1", "laya", "Misra Chapu", application="deferred", deferral_reason="x"),
            _change("SS-0001:group:1", "group", "some-cycle", application="apply"),
            _change("SS-0001:bhava:1", "bhava", "Apatya Bhava", reviewer=""),
            _change("SS-0001:raga_character:1", "raga_character", "Rare", decision="reject"),
        ],
    )
    assert load_approved_claims((path,)) == {KRITHI: {"Syama Sastri": {"mudra": ["Syama Krishna"]}}}


def test_missing_manifest_and_unknown_claim_prefix_abstain(tmp_path):
    assert load_approved_claims((tmp_path / "absent.json",)) == {}
    path = _manifest(tmp_path, [_change("other:1", "mudra", "x", application="apply")])
    assert load_approved_claims((path,)) == {}


def test_clauses_follow_priority_order_and_abstain():
    claims = {"Syama Sastri": {"bhava": ["Apatya Bhava"], "mudra": ["Syama Krishna"]}}
    assert claim_clauses("Syama Sastri", claims) == ["[Mudra: Syama Krishna]", "[Bhava: Apatya Bhava]"]
    assert claim_clauses("Syama Sastri", None) == []
    assert claim_clauses("Syama Sastri", {}) == []
    assert claim_clauses("Muthuswami Dikshitar", claims) == []


def test_header_carries_the_clause_and_abstains_without_one():
    kwargs: dict[str, Any] = {
        "title": "Marivere",
        "composer": "Syama Sastri",
        "raga": "Anandabhairavi",
        "primary_lyrics": "text",
    }
    claims = {"Syama Sastri": {"mudra": ["Syama Krishna"]}}
    assert "[Mudra: Syama Krishna]" in format_composition_overview(**kwargs, approved_claims=claims)
    assert format_composition_overview(**kwargs, approved_claims=None) == format_composition_overview(**kwargs)
    assert "[Mudra: Syama Krishna]" not in format_composition_overview(**kwargs)
    passage = format_section_passage(
        title="Marivere", composer="Syama Sastri", section_type="PALLAVI", section_text="t", approved_claims=claims
    )
    assert "[Mudra: Syama Krishna]" in passage


def test_another_composers_claims_never_render():
    claims = {"Tyagaraja": {"theme": ["Nama Mahima"]}}
    out = format_composition_overview(title="T", composer="Syama Sastri", primary_lyrics="x", approved_claims=claims)
    assert "Nama Mahima" not in out


def test_header_budget_holds_with_every_clause():
    claims = {
        "Syama Sastri": {
            "mudra": ["Syama Krishna Sodari"],
            "laya": ["Viloma Chapu (4+3)"],
            "raga_character": ["Rare janya affinity"],
            "bhava": ["Apatya Bhava (Child-Mother Devotion)"],
        }
    }
    out = format_composition_overview(
        title="A very long composition title for the budget",
        composer="Syama Sastri",
        raga="Anandabhairavi",
        tala="Misra Chapu",
        deity="Meenakshi",
        kshetra="Madurai",
        tags=["one", "two", "three"],
        primary_lyrics="body",
        approved_claims=claims,
    )
    header = out.split("\n")[0]
    assert len(header.split()) <= MAX_HEADER_WORDS


def test_a_clause_already_in_the_cycle_header_is_not_repeated():
    cycle = {
        "slug": "syama-sastri-navaratnamalika",
        "display_name": "Navaratnamalika",
        "attrs": {
            "provenance_owner": "TRACK-146",
            "review_state": "reviewed",
            "claim_id": "SS-0001:group:1",
            "approval": {"decision": "approve", "reviewer": "r", "accepted_at": "d", "note": "n"},
            "mudra": "Syama Krishna",
        },
    }
    claims = {"Syama Sastri": {"mudra": ["Syama Krishna"]}}
    out = format_composition_overview(
        title="T", composer="Syama Sastri", cycle_info=cycle, primary_lyrics="x", approved_claims=claims
    )
    assert out.count("Mudra: Syama Krishna") == 1


def test_real_manifests_apply_only_the_lifted_mudras():
    approved = load_approved_claims()
    syama = json.loads((ROOT / "database/data/track146-syama-sastri-application-manifest.json").read_text("utf-8"))
    lifted = {
        c["canonical_id"] for c in syama["changes"] if c["axis"] == "mudra" and c["approval"]["application"] == "apply"
    }
    assert lifted and lifted <= set(approved)
    deferred = [c for c in syama["changes"] if c["approval"]["application"] == "deferred"]
    assert deferred
    for change in deferred:
        values = approved.get(change["canonical_id"], {}).get("Syama Sastri", {}).get(change["axis"], [])
        assert change["value"] not in values


def test_the_four_deferred_mudras_were_lifted_with_their_approval_intact():
    syama = json.loads((ROOT / "database/data/track146-syama-sastri-master.json").read_text("utf-8"))
    claims = {c["id"]: c for c in syama["claims"]}
    for claim_id in ("SS-0032:mudra:1", "SS-0034:mudra:1", "SS-0046:mudra:1", "SS-0051:mudra:1"):
        approval = claims[claim_id]["approval"]
        assert approval["application"] == "apply" and approval["reviewer"] == "Seshadri"
        assert "deferral_reason" not in approval and "Deferral lifted" in approval["note"]


def test_both_embedding_scripts_pass_the_claims_to_both_formatters():
    for name in ("embed_catalogue.py", "batch_embed_catalogue.py"):
        text = (ROOT / "tools/krithi-extract-enrich-worker/scripts" / name).read_text(encoding="utf-8")
        assert text.count("approved_claims=APPROVED_CLAIMS.get(krithi_id)") == 2, name
