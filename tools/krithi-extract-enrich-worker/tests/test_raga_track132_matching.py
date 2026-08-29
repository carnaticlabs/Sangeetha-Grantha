"""TRACK-132 raga matching-key regressions.

Adjudicated MERGE pairs must share a key; DISTINCT pairs and the Ranjani
family must not collapse. Homonyms that share a name-key (Kalāvathi/Kalāvati,
Shreemati/Srimati) are disambiguated by mela in TRACK-136, not here.
"""

from src.normalizer import normalize_for_matching

RAGA_MERGE_PAIRS = [
    ("yadukula kAmbhOji", "Yadukula Kāmbhoji"),
    ("dEva manOhari", "Deva Manohari"),
    ("haMsa dhvani", "Hamsadhwani"),
    ("aThANa", "Atāna"),
    ("ghurjari", "Gurjari"),
    ("khamAs", "Kamās"),
    ("madhyamAvati", "Madhyamāvathi"),
    ("Pūrvi", "Poorvi"),
    ("cakravAkaM", "Chakravākam"),
    ("Suddha sAvEri", "Suddha Sāveri"),
    ("karNATaka kApi", "Karnātaka Kāpi"),
    ("Andali", "Andhali"),
]

RAGA_DISTINCT_PAIRS = [
    ("Kanadā", "Kannada"),
    ("Bhairavi", "Bhairava"),
    ("Bhairavi", "Bhairavam"),
    ("Abhogi", "Bhogi"),
]


def test_raga_merge_pairs_share_a_matching_key() -> None:
    for left, right in RAGA_MERGE_PAIRS:
        assert normalize_for_matching(left, "raga") == normalize_for_matching(right, "raga"), (
            f"{left!r} and {right!r} must fold to the same raga key"
        )


def test_raga_distinct_pairs_keep_separate_keys() -> None:
    for left, right in RAGA_DISTINCT_PAIRS:
        assert normalize_for_matching(left, "raga") != normalize_for_matching(right, "raga"), (
            f"{left!r} and {right!r} must not fold onto each other"
        )


def test_ranjani_family_stays_three_keys() -> None:
    keys = {normalize_for_matching(name, "raga") for name in ("Ranjani", "Niranjani", "Shreeranjani")}
    assert len(keys) == 3, f"digraphs must be mapped not deleted, got {keys}"
