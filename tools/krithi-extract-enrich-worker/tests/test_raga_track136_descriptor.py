"""TRACK-136 raga descriptor-stripping regressions.

`normalize_for_matching(..., "raga")` must drop a trailing raga-descriptor word so
"Sri rAgaM" folds to the raga NAME (`sri`), matching the keeper — the DB row
`SrI rAgaM` had stranded on its own key. A *bare* descriptor with no preceding
name token must never collapse to an empty key. Mirrors SQL raga_match_key().
"""

from src.normalizer import normalize_for_matching


def test_trailing_raga_descriptor_is_stripped() -> None:
    assert normalize_for_matching("Sri rAgaM", "raga") == normalize_for_matching("Sri", "raga") == "sri"
    assert normalize_for_matching("Shree ragam", "raga") == "sri"


def test_bare_raga_descriptor_not_emptied() -> None:
    # No preceding name token → survives intact, never an empty key.
    assert normalize_for_matching("Ragam", "raga") == "ragam"
    # Ends in the letters "raga" but not as a separate word → left alone.
    assert normalize_for_matching("Ragamalika", "raga") == "ragamalika"
