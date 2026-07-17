"""Regression tests for inline Indic P/A/C abbreviation section headers.

thyagaraja-vaibhavam blog Indic variants label sections with abbreviations in
each script — "प." (pallavi), "अ." (anupallavi), "च1." (charanam N). Before
these patterns were added, such variants collapsed into a single unsplit blob
and never matched their multi-section template. Like the Latin inline P/A/C
patterns they are GATED: only active when the document actually contains such
markers, so full-word-header documents are unaffected.
"""

from __future__ import annotations

import pytest

from src.structure_parser import StructureParser

# Minimal synthetic blobs, one per script, with P / A / C1 / C2 markers.
_BLOBS = {
    "devanagari": "प. पल्लवि पदमु\nअ. अनुपल्लवि पदमु\nच1. प्रथम चरणमु\nच2. द्वितीय चरणमु",
    "telugu": "ప. పల్లవి పదము\nఅ. అనుపల్లవి పదము\nచ1. ప్రథమ చరణము\nచ2. ద్వితీయ చరణము",
    "kannada": "ಪ. ಪಲ್ಲವಿ ಪದಮು\nಅ. ಅನುಪಲ್ಲವಿ ಪದಮು\nಚ1. ಪ್ರಥಮ ಚರಣಮು\nಚ2. ದ್ವಿತೀಯ ಚರಣಮು",
    "malayalam": "പ. പല്ലവി പദമു\nഅ. അനുപല്ലവി പദമു\nച1. പ്രഥമ ചരണമു\nച2. ദ്വിതീയ ചരണമു",
    "tamil": "ப. பல்லவி பதமு\nஅ. அனுபல்லவி பதமு\nச1. ப்ரதம சரணமு\nச2. த்3விதீய சரணமு",
}


class TestIndicPacSplitting:
    @pytest.mark.parametrize("script,blob", list(_BLOBS.items()))
    def test_blob_splits_into_pac(self, script: str, blob: str) -> None:
        sections = StructureParser().parse(blob).sections
        types = [s.section_type.value for s in sections]
        assert types == ["PALLAVI", "ANUPALLAVI", "CHARANAM", "CHARANAM"], (
            f"{script}: {types}"
        )

    @pytest.mark.parametrize("header,expected", [
        ("प. foo", "PALLAVI"), ("अ. foo", "ANUPALLAVI"), ("च1.foo", "CHARANAM"),
        ("ప. foo", "PALLAVI"), ("చ2. foo", "CHARANAM"),
        ("ಪ. foo", "PALLAVI"), ("ച3.foo", "CHARANAM"), ("ப. foo", "PALLAVI"),
    ])
    def test_detect_when_enabled(self, header: str, expected: str) -> None:
        parser = StructureParser()
        parser._inline_indic_pac_enabled = True
        m = parser._detect_section_header(header)
        assert m is not None and m.label == expected, f"{header!r} -> {m}"


class TestGatingPreventsFalsePositives:
    def test_disabled_by_default(self) -> None:
        """Without the gate, a lyric line beginning with प/अ/च is not a header."""
        parser = StructureParser()
        assert getattr(parser, "_inline_indic_pac_enabled", False) is False
        # A full-word-header document must not activate the abbreviation patterns.
        blob = "पल्लवि\nकामाक्षि पदमु\nचरणम्\nप्रथम चरणमु"
        types = [s.section_type.value for s in StructureParser().parse(blob).sections]
        assert types == ["PALLAVI", "CHARANAM"], types

    def test_bare_letter_without_period_not_matched(self) -> None:
        """The period is the disambiguator — 'प foo' (no period) is lyric."""
        parser = StructureParser()
        parser._inline_indic_pac_enabled = True
        assert parser._detect_section_header("प रामुनि") is None
