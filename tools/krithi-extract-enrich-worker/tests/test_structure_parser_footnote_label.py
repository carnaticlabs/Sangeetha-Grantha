"""TRACK-149: a footnote digit before the inline P label must not hide the pallavi."""

from src.structure_parser import StructureParser

SOURCE = """paripAlaya paripAlaya-rItigauLa
1 P paripAlaya paripAlaya paripAlaya
raghu nAtha
C1 tanuvE nIk(a)nuvaina sadanamaurA
raghu nAtha (pari)
C2 sthira cittamu vara cAmIkara pIThamu
raghu nAtha (pari)
"""


def _types(text: str) -> list[str]:
    return [str(s.section_type.value) for s in StructureParser().parse(text).sections]


def test_leading_footnote_digit_still_marks_the_pallavi():
    assert _types(SOURCE)[:1] == ["PALLAVI"]
    assert _types(SOURCE).count("CHARANAM") == 2


def test_label_after_the_digit_is_not_part_of_the_lyric():
    sections = StructureParser().parse(SOURCE).sections
    pallavi = next(s for s in sections if s.section_type.value == "PALLAVI")
    assert pallavi.text.startswith("paripAlaya")


def test_footnote_after_the_label_is_unchanged():
    text = SOURCE.replace("1 P paripAlaya", "P 1paripAlaya")
    assert _types(text)[:1] == ["PALLAVI"]


def test_digit_glued_to_the_label_is_also_accepted():
    assert _types(SOURCE.replace("1 P paripAlaya", "1P paripAlaya"))[:1] == ["PALLAVI"]
