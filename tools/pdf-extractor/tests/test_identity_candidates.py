from src.identity_candidates import (
    IdentityCandidateDiscovery,
    ReferenceEntity,
    normalize_identity_text,
    normalize_raga_text,
)


def test_normalization_collapses_diacritics_and_transliteration() -> None:
    assert normalize_identity_text("Muthuswāmi Dikshitar") == "mutuswami diksitar"
    assert normalize_raga_text("Kedāra Gaula") == "kedaragaula"


def test_composer_alias_match_scores_high() -> None:
    discovery = IdentityCandidateDiscovery(
        composers=[
            ReferenceEntity(
                entity_id="c-1",
                name="Muttuswami Dikshitar",
                aliases=("dikshitar",),
            )
        ],
        ragas=[],
        min_score=60,
        max_candidates=5,
    )

    result = discovery.discover(composer="Dikshitar", ragas=None)
    assert len(result.composers) == 1
    assert result.composers[0].entity_id == "c-1"
    assert result.composers[0].matched_on == "alias"
    assert result.composers[0].confidence == "HIGH"


def test_raga_candidates_are_ranked_and_limited() -> None:
    discovery = IdentityCandidateDiscovery(
        composers=[],
        ragas=[
            ReferenceEntity(entity_id="r-1", name="Dwijavanti"),
            ReferenceEntity(entity_id="r-2", name="Dwijavanthi"),
            ReferenceEntity(entity_id="r-3", name="Kalyani"),
        ],
        min_score=50,
        max_candidates=2,
    )

    result = discovery.discover(composer=None, ragas=["Dwijavanti"])
    assert len(result.ragas) == 2
    assert result.ragas[0].entity_id in {"r-1", "r-2"}
    assert result.ragas[0].score >= result.ragas[1].score
