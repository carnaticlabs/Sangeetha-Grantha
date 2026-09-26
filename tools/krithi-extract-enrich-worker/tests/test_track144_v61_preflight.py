from pathlib import Path
from uuid import UUID

from src.track144_v61_preflight import (
    LATIN_PALLAVI,
    LYRICS_PREFIX,
    KrithiIdentity,
    LyricVariantView,
    PreflightInput,
    assess,
    is_contaminated,
    load_snapshot,
    snapshot_still_matches,
    write_snapshot,
)


def _krithi(title: str = "Marugelaraa", composer: str = "Tyagaraja") -> KrithiIdentity:
    return KrithiIdentity(krithi_id=UUID("00000000-0000-4000-8000-000000000001"), title=title, composer=composer)


def _variant(script: str, lyrics: str, pallavi_text: str | None = None) -> LyricVariantView:
    return LyricVariantView(
        variant_id=UUID("00000000-0000-4000-8000-000000000002"),
        krithi_id=UUID("00000000-0000-4000-8000-000000000001"),
        title="Marugelaraa",
        composer="Tyagaraja",
        script=script,
        language="te" if script == "telugu" else "en",
        lyrics=lyrics,
        pallavi_text=pallavi_text,
    )


def test_v65_keeps_v61_immutable_and_covers_the_review_gaps():
    root = Path(__file__).resolve().parents[3]
    v65 = (root / "database/migrations/V65__track144_embedding_cohort_audit_and_script_recovery.sql").read_text()
    v61 = (root / "database/migrations/V61__catalogue_raga_and_metadata_repairs.sql").read_text()
    assert "REINDEX INDEX idx_audit_entity_time" in v65
    assert "'OBSERVE'" in v65
    assert "retrospective_observation" in v65
    assert "prior_beat_count" in v65
    assert "jsonb_build_object('beat_count', null" not in v65
    assert "STALE_TRACK_144_NEEDS_REBUILD" in v65
    assert "Merged corrupt Isra Capu/Chapu to Misra Capu" in v65
    assert "Merged corrupt Ad to Adi" in v65
    assert "Standardized to canonical Catusra Ekam" in v65
    assert "Standardized Nottuswara English meter to canonical Catusra Ekam" in v65
    assert "v.script <> 'latin'" in v65
    assert LATIN_PALLAVI in v65
    assert "track144_embedding_cohort" in v65
    assert "v.script <> 'latin'" not in v61
    assert "script = 'latin'" not in v61


def test_latin_only_catalogue_does_not_block_after_v61():
    decision = assess(
        PreflightInput(
            v61_applied=True,
            v65_applied=False,
            krithis=[_krithi()],
            variants=[_variant("latin", LYRICS_PREFIX + "existing")],
        )
    )
    assert decision.blocks_migration is False
    assert decision.needs_recovery is False
    assert is_contaminated(_variant("latin", LYRICS_PREFIX + "existing")) is False


def test_ambiguous_title_blocks_before_v61():
    other = _krithi().model_copy(
        update={"krithi_id": UUID("00000000-0000-4000-8000-000000000003"), "composer": "Other"}
    )
    decision = assess(PreflightInput(v61_applied=False, v65_applied=False, krithis=[_krithi(), other], variants=[]))
    assert decision.blocks_migration is True
    assert any("scalar subquery" in reason for reason in decision.reasons)


def test_non_latin_variant_requires_a_snapshot_before_v61():
    telugu = _variant("telugu", "తెలుగు పాఠం")
    blocked = assess(PreflightInput(v61_applied=False, v65_applied=False, krithis=[_krithi()], variants=[telugu]))
    assert blocked.blocks_migration is True

    acknowledged = assess(
        PreflightInput(
            v61_applied=False,
            v65_applied=False,
            krithis=[_krithi()],
            variants=[telugu],
            snapshot_acknowledged=True,
        )
    )
    assert acknowledged.blocks_migration is False


def test_contaminated_non_latin_row_needs_v65():
    contaminated = _variant("telugu", LYRICS_PREFIX + "తెలుగు", pallavi_text=LATIN_PALLAVI)
    assert is_contaminated(contaminated) is True
    decision = assess(
        PreflightInput(
            v61_applied=True,
            v65_applied=False,
            krithis=[_krithi()],
            variants=[contaminated],
        )
    )
    assert decision.needs_recovery is True
    assert decision.blocks_migration is False


def test_snapshot_round_trip_detects_unrestored_latin_text(tmp_path):
    original = _variant("tamil", "தமிழ் பாடல்")
    path = tmp_path / "snapshot.json"
    write_snapshot(path, [original, _variant("latin", "marug(E)lar(A) O rAghav(A)")])
    loaded = load_snapshot(path)
    assert len(loaded.variants) == 1
    assert snapshot_still_matches(loaded, [original]) == []
    dirty = original.model_copy(update={"lyrics": LYRICS_PREFIX + original.lyrics, "pallavi_text": LATIN_PALLAVI})
    problems = snapshot_still_matches(loaded, [dirty])
    assert any("Latin pallavi" in problem for problem in problems)
    assert any("lyrics differ" in problem for problem in problems)
