"""Regression: Latin P/A/C markers followed by a footnote-reference digit.

Govindan blog pages render section markers as
``<span style="font-size:180%">P</span><sup>1</sup>giripai`` — i.e. the text
"P 1giripai". The inline pallavi rule used to require a letter immediately after
"P ", so the footnote digit caused the pallavi to be head-captured and dropped,
under-counting the template (e.g. giripai nelakonna). The lookahead now tolerates
the digit, which is then stripped from the section text.
"""

from __future__ import annotations

from src.structure_parser import StructureParser

_GIRIPAI_LATIN = (
    "P 1giripai nelakonna rAmuni\nguri tappaka kaNTi\n\n"
    "A parivArulu viri suraTulacE\nnilabaDi visarucu kosarucu sEvimpaga (giri)\n\n"
    "C pulak(A)nkituDai Anand-\n(A)Sruvula nimpucu mATal(A)Da valen(a)ni\n"
    "2kaluvarinca kani 3padi pUTalapai\nkAcedan(a)nu tyAgarAja vinutuni (giri)"
)


def test_pallavi_with_footnote_digit_detected() -> None:
    sections = StructureParser().parse(_GIRIPAI_LATIN).sections
    assert [s.section_type.value for s in sections] == ["PALLAVI", "ANUPALLAVI", "CHARANAM"]
    # The footnote "1" must be stripped from the pallavi text.
    assert sections[0].text.startswith("giripai nelakonna rAmuni"), sections[0].text[:30]


def test_inline_p_marker_requires_separating_space() -> None:
    """A lyric word beginning with 'P' (no separating space) is not a P marker."""
    parser = StructureParser()
    parser._inline_pa_enabled = True
    # 'Priya' → 'P' immediately followed by 'r', no space: inline rule must not fire.
    assert parser._detect_section_header("Priya rAma nAmamu") is None
