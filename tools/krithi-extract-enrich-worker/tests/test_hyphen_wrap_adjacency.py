"""TRACK-139: hyphen-wrap suppression only applies to the adjacent physical line."""

from __future__ import annotations

from src.schema import SectionType
from src.structure_parser import StructureParser


def test_hyphen_then_blank_then_anupallavi_still_splits() -> None:
    text = """\
P alakalallalADaga kani(y)-

A celuvu mIraganu
C muni kanu saiga telisi
"""
    result = StructureParser().parse(text)
    types = [s.section_type for s in result.sections]
    assert types == [SectionType.PALLAVI, SectionType.ANUPALLAVI, SectionType.CHARANAM], types
