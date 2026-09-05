"""TRACK-133: recover missing Indic C5 headers without treating every refrain as a boundary."""

import json
from pathlib import Path

import pytest

from src.structure_parser import StructureParser

FIXTURES = Path(__file__).parent / "fixtures" / "structure_parser"
EXPECTED = json.loads((FIXTURES / "rama_ramana_rara_echo.expected.json").read_text())
MERGED = (FIXTURES / "rama_ramana_rara_internal_echo.txt").read_text()
EXPLICIT = (FIXTURES / "rama_ramana_rara_explicit_charanams.txt").read_text()
INDIC = ["sa", "te", "kn", "ml"]


def snapshot(sections):
    return [{"type": s.section_type.value, "order": s.order, "label": s.label, "text": s.text} for s in sections]


@pytest.mark.parametrize("language", INDIC)
@pytest.mark.parametrize("post_boundary", [False, True])
def test_internal_echo_restores_c4_c5_c6(language, post_boundary):
    text = MERGED.replace("\nDevanagari\n", "\nMeaning\nExplanatory prose.\nDevanagari\n") if post_boundary else MERGED
    parsed = StructureParser().parse(text)
    variant = next(v for v in parsed.lyric_variants if v.language == language)
    assert snapshot(variant.sections) == EXPECTED[language]
    assert snapshot(parsed.sections) == EXPECTED["en"]
    # The new C5 must retain its own source span, after the C4 closing refrain.
    c4, c5, c6 = variant.sections[4:]
    assert c4.end_pos <= c5.start_pos < c5.end_pos <= c6.start_pos
    assert text[c5.start_pos : c5.end_pos] == c5.text


@pytest.mark.parametrize("language", ["en", "ta"])
def test_english_tamil_and_caranam_continuation_unchanged(language):
    variant = next(v for v in StructureParser().parse(MERGED).lyric_variants if v.language == language)
    assert snapshot(variant.sections) == EXPECTED[language]


@pytest.mark.parametrize("language", ["en", "ta", *INDIC])
def test_explicit_headers_and_terminal_refrains_unchanged(language):
    variant = next(v for v in StructureParser().parse(EXPLICIT).lyric_variants if v.language == language)
    assert snapshot(variant.sections) == EXPECTED[language]


def small_variant(body, *, canonical_charanams=2):
    canonical = "English\nPallavi\nrama come\n" + "".join(f"Charanam\nverse {i}\n" for i in range(canonical_charanams))
    return canonical + f"Telugu\nPallavi\nరమా రమణ రారా (రమా)\nCharanam\n{body}\n"


@pytest.mark.parametrize(
    "body,count",
    [
        ("మొదటి (రమా)\nతరువాత (రమా)", 1),  # no deficit: ordinary internal refrain
        ("మొదటి\nచివరి (రమా)", 2),  # terminal refrain only
        ("మొదటి (రమా) తరువాత\nచివరి (రమా)", 2),  # inline parenthetical
        ("మొదటి (రమా)\nతరువాత", 2),  # continuation has no closing echo
        ("మొదటి (వేరే)\nతరువాత (వేరే)", 2),  # not a pallavi echo
        ("మొదటి (రమా)\nరెండవ (రమా)\nమూడవ (రమా)", 2),  # ambiguous excess candidates
        ("మొదటి (రమా)\nతరువాత (రమా)", 3),  # candidates cannot fill deficit
    ],
)
def test_ordinary_or_ambiguous_refrains_do_not_split(body, count):
    parsed = StructureParser().parse(small_variant(body, canonical_charanams=count))
    variant = next(v for v in parsed.lyric_variants if v.language == "te")
    assert len(variant.sections) == 2
    assert variant.sections[-1].section_type.value == "CHARANAM"
    assert variant.sections[-1].text == body


def test_anupallavi_echoes_are_not_split():
    body = "మొదటి (రమా)\nతరువాత (రమా)"
    text = f"English\nPallavi\nrama come\nAnupallavi\nverse 0\nTelugu\nPallavi\nరమా రమణ రారా (రమా)\nAnupallavi\n{body}\n"
    variant = next(v for v in StructureParser().parse(text).lyric_variants if v.language == "te")
    assert len(variant.sections) == 2
    assert variant.sections[1].section_type.value == "ANUPALLAVI"
    assert variant.sections[1].text == body


def test_mixed_true_and_false_cuts_across_blocks_are_rejected():
    """A spurious mid-stanza echo plus a real glue must not sum into a repair."""
    spurious = "మొదటి (రమా)\nఇంకా (రమా)"
    glued = "నిజం నాలుగు (రమా)\nఐదు (రమా)"
    text = (
        "English\nPallavi\nrama come\n"
        + "".join(f"Charanam\nverse {i}\n" for i in range(4))
        + f"Telugu\nPallavi\nరమా రమణ రారా (రమా)\nCharanam\n{spurious}\nCharanam\n{glued}\n"
    )
    variant = next(v for v in StructureParser().parse(text).lyric_variants if v.language == "te")
    assert [s.section_type.value for s in variant.sections] == ["PALLAVI", "CHARANAM", "CHARANAM"]
    assert variant.sections[1].text == spurious
    assert variant.sections[2].text == glued


def test_single_glued_block_restores_two_missing_headings():
    body = "మొదటి (రమా)\nరెండవ (రమా)\nమూడవ (రమా)"
    variant = next(
        v
        for v in StructureParser().parse(small_variant(body, canonical_charanams=3)).lyric_variants
        if v.language == "te"
    )
    assert [s.text for s in variant.sections[1:]] == body.split("\n")
    assert [s.section_type.value for s in variant.sections] == ["PALLAVI", "CHARANAM", "CHARANAM", "CHARANAM"]


def test_missing_pallavi_echo_does_not_split():
    text = small_variant("మొదటి (రమా)\nతరువాత (రమా)").replace("రమా రమణ రారా (రమా)", "వేరే పల్లవి")
    variant = next(v for v in StructureParser().parse(text).lyric_variants if v.language == "te")
    assert len(variant.sections) == 2


def test_repaired_text_reparsed_with_explicit_headers_is_stable():
    parsed = StructureParser().parse(MERGED)
    rebuilt = "\n".join(
        label + "\n" + "\n".join(s.section_type.value.title() + "\n" + s.text for s in v.sections)
        for label, v in zip(
            ["English", "Devanagari", "Tamil", "Telugu", "Kannada", "Malayalam"], parsed.lyric_variants, strict=True
        )
    )
    assert {v.language: snapshot(v.sections) for v in StructureParser().parse(rebuilt).lyric_variants} == EXPECTED
