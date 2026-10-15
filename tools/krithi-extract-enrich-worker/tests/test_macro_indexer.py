"""Composer attribution and approval gates for existing macro search infrastructure."""

from src.embeddings.context_formatter import format_composition_overview
from src.embeddings.macro_indexer import reviewed_cycle_composer

SLUG = "syama-sastri-swarajathi-ratnatrayam"
APPROVAL = {"decision": "approve", "reviewer": "Fixture curator", "accepted_at": "2026-10-01", "note": "Test only"}
ATTRS = {"provenance_owner": "TRACK-146", "review_state": "reviewed", "claim_id": "fixture", "approval": APPROVAL}


def test_macro_composer_is_derived_and_mixed_groups_abstain():
    assert (
        reviewed_cycle_composer("kamalamba-navavarnam", [{"composer_name": "Muthuswami Dikshitar"}])
        == "Muthuswami Dikshitar"
    )
    assert reviewed_cycle_composer(SLUG, []) is None
    assert (
        reviewed_cycle_composer(SLUG, [{"composer_name": "Syama Sastri"}, {"composer_name": "Subbaraya Sastri"}])
        is None
    )
    assert reviewed_cycle_composer(SLUG, [{"composer_name": "Syama Sastri"}]) is None
    assert (
        reviewed_cycle_composer(SLUG, [{"composer_name": "Syama Sastri", "discriminative_attributes": ATTRS}])
        == "Syama Sastri"
    )


def test_sastri_cycle_abstains_without_individual_approval():
    overview = format_composition_overview(
        title="Kamakshi anudinamu",
        composer="Syama Sastri",
        raga="Bhairavi",
        musical_form="SWARAJATHI",
        tags=["Swarajathi Ratnatrayam", "Navaratnamalika"],
        cycle_info={"slug": SLUG, "display_name": "Ratnatrayam", "attrs": {}},
    )
    assert "[Cycle:" not in overview
    assert "Ratnatrayam" not in overview
    assert "Navaratnamalika" not in overview
    assert "[Kshetra:" not in overview


def test_approved_cycle_preserves_budget_and_short_passage():
    overview = format_composition_overview(
        title="Kamakshi anudinamu",
        composer="Syama Sastri",
        raga="Bhairavi",
        musical_form="SWARAJATHI",
        primary_lyrics="Kamakshi",
        cycle_info={"slug": SLUG, "display_name": "Ratnatrayam", "attrs": ATTRS},
    )
    assert "[Cycle: Ratnatrayam]" in overview
    assert len(overview.splitlines()[0].split()) <= 60
    assert "Kamakshi" in overview
    other = format_composition_overview(
        title="Kamakshi",
        composer="Subbaraya Sastri",
        cycle_info={"slug": SLUG, "display_name": "Ratnatrayam", "attrs": ATTRS},
    )
    assert "[Cycle:" not in other
