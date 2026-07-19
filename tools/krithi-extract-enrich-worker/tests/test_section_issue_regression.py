"""Regression tests for section-detection false positives in Indic scripts.

The single-letter Indic abbreviation patterns (e.g. ப → PALLAVI) in the
structure parser previously matched lyric lines starting with those letters
(e.g. "ப 4 ரதாக்..." where ப + space triggered the pattern). This caused
23 section issues across 16 Dikshitar krithis — the affected variant's
section was mislabeled and dropped during extraction.

These tests run the full extraction pipeline (HTML → normalize → parse)
against fixture HTML files that reproduce the exact patterns from the
guru-guha.blogspot.com source pages.
"""

from __future__ import annotations

from pathlib import Path

import pytest

from src.diacritic_normalizer import normalize_garbled_diacritics
from src.html_extractor import HtmlTextExtractor
from src.structure_parser import StructureParser

FIXTURE_DIR = Path(__file__).parent / "fixtures" / "html"


class _ParsedFixture:
    def __init__(self, html_path: Path) -> None:
        html = html_path.read_text(encoding="utf-8")
        extractor = HtmlTextExtractor()
        extracted = extractor.extract(html, base_url="http://example.com/test.html")
        normalized = normalize_garbled_diacritics(extracted.text)
        parser = StructureParser()
        result = parser.parse(normalized)
        self.sections = [s.section_type.value for s in result.sections]
        self.variants = {v.language: [s.section_type.value for s in v.sections] for v in result.lyric_variants}


def _extract_and_parse(html_path: Path) -> dict[str, list[str]]:
    """Run the full pipeline on a fixture and return section types per language."""
    return _ParsedFixture(html_path).variants


class TestIndicSingleLetterFalsePositive:
    """Tamil lyric text starting with ப/அ/ச must not be misdetected as headers."""

    def test_tamil_anupallavi_not_swallowed(self) -> None:
        """ப 4 ரதாக்... line must not trigger PALLAVI — it's Anupallavi content."""
        variants = _extract_and_parse(FIXTURE_DIR / "indic_single_letter_false_positive.html")
        assert "ta" in variants, "Tamil variant not found in parse result"
        assert variants["ta"] == ["PALLAVI", "ANUPALLAVI", "CHARANAM"], (
            f"Tamil sections {variants['ta']} — expected [P, A, C]; "
            "single-letter abbreviation likely consumed lyric text as header"
        )

    def test_english_sections_unaffected(self) -> None:
        """English sections (first block, no language header) become top-level sections."""
        parsed = _ParsedFixture(FIXTURE_DIR / "indic_single_letter_false_positive.html")
        # The first Latin block without a language header becomes result.sections
        assert parsed.sections == ["PALLAVI", "ANUPALLAVI", "CHARANAM"]

    def test_telugu_sections_unaffected(self) -> None:
        """Telugu sections should parse correctly too."""
        variants = _extract_and_parse(FIXTURE_DIR / "indic_single_letter_false_positive.html")
        assert "te" in variants, f"Telugu variant not found, got: {list(variants.keys())}"
        assert variants["te"] == ["PALLAVI", "ANUPALLAVI", "CHARANAM"]

    def test_all_languages_have_three_sections(self) -> None:
        """Every language variant in the fixture must have exactly 3 sections."""
        variants = _extract_and_parse(FIXTURE_DIR / "indic_single_letter_false_positive.html")
        for lang, sections in variants.items():
            assert len(sections) == 3, f"{lang} has {len(sections)} sections {sections}, expected 3"


class TestIndicCharanamFalsePositive:
    """Tamil Charanam text starting with ப must not be misdetected as Pallavi."""

    def test_tamil_charanam_not_swallowed(self) -> None:
        """ப 4 க்த நாக 3... line must not trigger PALLAVI — it's Charanam content."""
        variants = _extract_and_parse(FIXTURE_DIR / "indic_charanam_false_positive.html")
        assert "ta" in variants, "Tamil variant not found in parse result"
        assert variants["ta"] == ["PALLAVI", "ANUPALLAVI", "CHARANAM"], (
            f"Tamil sections {variants['ta']} — expected [P, A, C]; "
            "single-letter abbreviation likely consumed Charanam text as PALLAVI"
        )

    def test_all_languages_have_three_sections(self) -> None:
        """Every language variant in the fixture must have exactly 3 sections."""
        variants = _extract_and_parse(FIXTURE_DIR / "indic_charanam_false_positive.html")
        for lang, sections in variants.items():
            assert len(sections) == 3, f"{lang} has {len(sections)} sections {sections}, expected 3"


class TestSingleLetterAbbreviationsStillWork:
    """Legitimate single-letter abbreviation headers must still be detected."""

    @pytest.mark.parametrize(
        "header,expected",
        [
            ("P", "PALLAVI"),
            ("A", "ANUPALLAVI"),
            ("C", "CHARANAM"),
            ("P.", "PALLAVI"),
            ("A:", "ANUPALLAVI"),
            ("C -", "CHARANAM"),
        ],
    )
    def test_latin_abbreviations(self, header: str, expected: str) -> None:
        parser = StructureParser()
        match = parser._detect_section_header(header)
        assert match is not None, f"'{header}' should be detected as {expected}"
        assert match.label == expected

    @pytest.mark.parametrize(
        "header,expected",
        [
            ("ப", "PALLAVI"),
            ("அ", "ANUPALLAVI"),
            ("ச", "CHARANAM"),
            ("ப.", "PALLAVI"),
            ("அ:", "ANUPALLAVI"),
        ],
    )
    def test_tamil_abbreviations(self, header: str, expected: str) -> None:
        parser = StructureParser()
        match = parser._detect_section_header(header)
        assert match is not None, f"'{header}' should be detected as {expected}"
        assert match.label == expected

    @pytest.mark.parametrize(
        "header,expected",
        [
            ("ప", "PALLAVI"),
            ("అ", "ANUPALLAVI"),
            ("చ", "CHARANAM"),
        ],
    )
    def test_telugu_abbreviations(self, header: str, expected: str) -> None:
        parser = StructureParser()
        match = parser._detect_section_header(header)
        assert match is not None, f"'{header}' should be detected as {expected}"
        assert match.label == expected

    @pytest.mark.parametrize(
        "lyric_line",
        [
            "ப 4 ரதாக் 3 ரஜ: கௌஸி 1 க யாக 3 ரக்ஷக:",
            "அப 4 யாம்பா 3 ஜக 3 த 3 ம்பா 3 ரக்ஷது",
            "ப 4 க்த நாக 3 லிங்க 3 பரிபாலினீ",
            "భరతాగ్రజః కౌశిక యాగ రక్షకః",
            "P followed by actual content should not match",
            "A long line starting with A letter",
        ],
    )
    def test_lyric_lines_not_detected_as_headers(self, lyric_line: str) -> None:
        """Lines with content after the initial letter must NOT match."""
        parser = StructureParser()
        match = parser._detect_section_header(lyric_line)
        if match is not None:
            assert match.label not in ("PALLAVI", "ANUPALLAVI", "CHARANAM"), (
                f"'{lyric_line[:40]}...' falsely matched as {match.label}"
            )


class TestFullPipelineWithFixtures:
    """End-to-end extraction pipeline tests using HTML fixtures."""

    def test_pipeline_sri_ramachandro_all_variants_complete(self) -> None:
        """Full pipeline on SrI rAma candrO fixture produces 3 sections in all languages."""
        variants = _extract_and_parse(FIXTURE_DIR / "indic_single_letter_false_positive.html")

        assert len(variants) >= 2, f"Expected at least 2 language variants, got {len(variants)}"

        for lang, sections in variants.items():
            assert "PALLAVI" in sections, f"{lang} missing PALLAVI"
            assert "CHARANAM" in sections, f"{lang} missing CHARANAM"
            assert "ANUPALLAVI" in sections, f"{lang} missing ANUPALLAVI"

    def test_pipeline_abhayamba_all_variants_complete(self) -> None:
        """Full pipeline on abhayAmbA fixture: English top-level + Tamil variant, all 3 sections."""
        parsed = _ParsedFixture(FIXTURE_DIR / "indic_charanam_false_positive.html")

        # English block (first, headerless) becomes top-level sections
        assert parsed.sections == ["PALLAVI", "ANUPALLAVI", "CHARANAM"]

        # Tamil variant detected with all 3 sections
        assert "ta" in parsed.variants, f"Tamil variant not found, got: {list(parsed.variants.keys())}"
        assert parsed.variants["ta"] == ["PALLAVI", "ANUPALLAVI", "CHARANAM"]
