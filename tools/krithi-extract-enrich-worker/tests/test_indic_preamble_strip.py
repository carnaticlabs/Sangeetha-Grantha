"""Regression tests for the Indic transliteration-key preamble stripper (ADR-015).

Govindan blogs print a Modified-Harvard-Kyoto pronunciation chart at the top of
each script block. For Devanagari it was already filtered; the Tamil chart slipped
through and was head-captured into the pallavi (section 1). These lines are
impossible in single-script lyric, so `_is_boilerplate` now drops all three forms.
"""

from __future__ import annotations

from pathlib import Path

import pytest

from src.diacritic_normalizer import normalize_garbled_diacritics
from src.html_extractor import HtmlTextExtractor
from src.structure_parser import StructureParser

FIXTURE_DIR = Path(__file__).parent / "fixtures" / "html"


class TestPreambleLinesFiltered:
    @pytest.mark.parametrize("line", [
        "க,ச,ட,த,ப - 2-ख छ ठ थ फ; 3-ग ड द ब; 4-घ झ ढ ध भ",  # comma consonant chart
        "ஸ1 श - शिव - சிவன்",                                  # Tamil+Devanagari mapping row
        "ரு2 ऋ - कृप - கிருபை",                               # Tamil+Devanagari mapping row
        "(ச3 - ஜ)",                                           # parenthesised sandhi mapping
    ])
    def test_preamble_line_is_boilerplate(self, line: str) -> None:
        assert StructureParser()._is_boilerplate(line) is True

    @pytest.mark.parametrize("line", [
        "வேங்கடேஸ1 நினு ஸேவிம்பனு பதி3",       # Tamil pallavi lyric
        "நா(கா3)சலமுபை பா3கு3க3 நெலகொன்ன (வேங்க)",  # Tamil charanam w/ refrain
        "कामाक्षि अनुदिनमु मरवकने नी",           # Devanagari lyric (single script)
        "vEnkaTESa ninu sEvimpanu padi",         # Latin lyric
    ])
    def test_lyric_line_is_not_boilerplate(self, line: str) -> None:
        assert StructureParser()._is_boilerplate(line) is False


class TestVenkatesaPallaviClean:
    def test_tamil_pallavi_has_no_preamble(self) -> None:
        html = (FIXTURE_DIR / "tv_venkatesa_tamil_trailer.html").read_text(encoding="utf-8")
        txt = normalize_garbled_diacritics(HtmlTextExtractor().extract(html, base_url="http://x/y").text)
        variants = {v.language: v for v in StructureParser().parse(txt).lyric_variants}
        ta_p1 = variants["ta"].sections[0].text
        assert "," not in ta_p1.split("\n")[0]           # no consonant chart
        assert "श" not in ta_p1 and "ऋ" not in ta_p1      # no Devanagari key rows
        assert ta_p1.startswith("வேங்கடேஸ")               # starts at the real pallavi
        # Now within ~2x of the smallest sibling pallavi (was ~3x with the preamble).
        smallest = min(len(variants[l].sections[0].text) for l in ("te", "kn", "ml"))
        assert len(ta_p1) <= 2 * smallest
