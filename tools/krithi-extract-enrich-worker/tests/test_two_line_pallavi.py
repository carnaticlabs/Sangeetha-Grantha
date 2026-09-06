"""TRACK-139: two-line pallavi must not be split at a hyphenation wrap.

Thyagaraja-vaibhavam HTML wraps ``kani(y)-A`` across a ``<br />``:

    P alakal(a)llalADaga kani(y)-
    A rAN-muni(y)eTu pongenO

    A celuvu mIraganu

The leading ``A`` on the wrapped line is the rest of the word, not an Anupallavi
marker. True structure is P, A, C = 3 (Utsava Sampradaya: two-line pallavi).
"""

from __future__ import annotations

from src.schema import SectionType
from src.structure_parser import StructureParser

ALAKALALLALAADAGA_WRAPPED = """\
P alakal(a)llalADaga kani(y)-
A rAN-muni(y)eTu pongenO

A celuvu mIraganu
mArIcuni madam(a)NacE vELa (alaka)

C muni kanu saiga telisi Sivadhanuvunu viricE samaya-muna
tyAgarAjavinutuni mOmuna ranjillu (alaka)
"""


def test_hyphen_wrapped_pallavi_line_is_not_anupallavi() -> None:
    result = StructureParser().parse(ALAKALALLALAADAGA_WRAPPED)
    types = [s.section_type for s in result.sections]
    assert types == [SectionType.PALLAVI, SectionType.ANUPALLAVI, SectionType.CHARANAM], types
    pallavi = result.sections[0].text
    assert "kani(y)-" in pallavi
    assert "rAN-muni" in pallavi
    assert "celuvu" not in pallavi
    assert "celuvu" in result.sections[1].text


def test_real_anupallavi_after_non_hyphen_line_still_splits() -> None:
    """A genuine ``A lyric`` header after a pallavi that does not end with '-' still splits."""
    text = """\
P alakalallalADaga kaniyA rAN-muniyeTu pongenO
A celuvu mIraganu
C muni kanu saiga telisi
"""
    result = StructureParser().parse(text)
    types = [s.section_type for s in result.sections]
    assert types == [SectionType.PALLAVI, SectionType.ANUPALLAVI, SectionType.CHARANAM], types
