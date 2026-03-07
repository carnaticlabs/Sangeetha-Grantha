"""Tests for consolidated matching normalizer."""

from src.normalizer import normalize_for_matching, normalize_garbled_diacritics


TITLE_PAIRS = [
    # Required examples from request.
    ("akhilandasvari raksa mam", "akhilandesvari raksa mam", True),
    ("anatabalakrsna", "anantabalakrsna", True),
    ("anarakamasrayamyaham", "angarakamasrayamyaham", True),
    ("kamalamba bajare", "kamalambam bajare", True),
    ("laksimi narasima", "laksmi narasima", True),
    ("ramacandarena", "ramacandrena", True),
    # Additional normalization_gap rows (similarity_score > 90) from
    # database/analysis/missed_sanskrit_variants.csv.
    ("mahalaksimi karunarasalahari", "mahalaksmi karunarasalahari", True),
    ("varalaksimi namastubyam", "varalaksmi namastubyam", True),
    ("srigananata baja re", "srigananatam baja re", True),
    ("sankacakragadapanim", "sankhacakragadapanim", True),
    ("candra baja manasa", "candram baja manasa", True),
    ("tyagaraja baja re", "tyagarajam baja re", True),
    ("kamalamba baja re", "kamalambam baja re", True),
    ("hiranmayi laksmim", "hiranmayim laksmim", True),
    ("srikrsna baja re", "srikrsnam baja re", True),
    ("srikrsna baja manasa", "srikrsnam baja manasa", True),
    ("ramanata bajeham", "ramanatam bajeham", True),
    ("tyagesa baja re", "tyagesam baja re", True),
    ("kamaksi ma pahi", "kamaksi mam pahi", True),
    ("akhilandasvaro raksatu", "akhilandesvaro raksatu", True),
    ("rajarajendaracola", "rajarajendracola", True),
    ("sacidanandamaya", "saccidanandamaya", True),
    ("suryamurtem", "suryamurte", True),
    ("saranragapriye", "sarangaragapriye", True),
    ("srikantimitim", "srikantimatim", True),
    # Must not match.
    ("tyagaraja", "kamalamba", False),
]

COMPOSER_TESTS = [
    ("Muthuswami Dikshitar", "muttusvami diksitar"),
    ("Muthuswami Dikshithar", "muttusvami diksitar"),
    ("Thyagaraja", "tyagaraja"),
    ("Shyama Shastri", "syama sastri"),
    ("Shyama Shastry", "syama sastri"),
]

RAGA_TESTS = [
    ("Kalyaani", "kalyani"),
    ("Shankarabharanam", "sankarabaranam"),
    ("Kedara Gaula", "kedaragaula"),
]

TALA_TESTS = [
    ("Rupakam", "rupaka"),
    ("Desadi", "adi"),
    ("Khanda Chapu", "kanda capu"),
]


def test_title_pairs() -> None:
    assert len(TITLE_PAIRS) >= 20
    for left, right, should_match in TITLE_PAIRS:
        left_norm = normalize_for_matching(left, "title")
        right_norm = normalize_for_matching(right, "title")
        assert (left_norm == right_norm) is should_match


def test_composer_normalization() -> None:
    for text, expected in COMPOSER_TESTS:
        assert normalize_for_matching(text, "composer") == expected


def test_raga_normalization() -> None:
    for text, expected in RAGA_TESTS:
        assert normalize_for_matching(text, "raga") == expected


def test_tala_normalization() -> None:
    for text, expected in TALA_TESTS:
        assert normalize_for_matching(text, "tala") == expected


def test_transliteration_collapse_order_longest_match_first() -> None:
    assert normalize_for_matching("chhaya", "composer") == "caya"


def test_garbled_diacritic_cleanup_is_available() -> None:
    assert normalize_garbled_diacritics("r\u00AFaga \u02D9m") == "rāgaṁ"

