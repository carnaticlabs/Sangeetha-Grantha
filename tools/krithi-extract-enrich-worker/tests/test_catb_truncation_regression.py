"""Regression tests for CAT-B swara-sAhitya truncation (Syama Sastri kritis).

Three Syama Sastri kritis on syamakrishnavaibhavam.blogspot.com store each
language variant as a sequence of inline "svara sAhitya N" sections. Two defects
combined to corrupt them on import:

1. ``ExtractionWorker`` byte-truncated every lyric section to 1800 bytes "for
   indexed DB columns". Indic scripts run ~3 bytes/char, so a whole-variant blob
   was silently cut mid-composition — dropping the final svara-sAhitya sections,
   including Syama Sastri's closing "SyAma kRshNa" mudra.
2. The structure parser only recognised the *Latin* ``svara sahitya`` header, so
   Indic variants never split into sections and collapsed into one oversized
   PALLAVI blob — which is exactly what made the truncation bite.

These tests parse saved snapshots of the source pages (not the live blog) and
assert every language variant recovers the full, correctly-ordered section set
with the closing mudra intact.
"""

from __future__ import annotations

import re
from pathlib import Path

import pytest

from src.config import ExtractorConfig
from src.diacritic_normalizer import normalize_garbled_diacritics
from src.html_extractor import HtmlTextExtractor
from src.schema import CanonicalLyricSection, CanonicalLyricVariant
from src.structure_parser import StructureParser
from src.worker import ExtractionWorker

FIXTURE_DIR = Path(__file__).parent / "fixtures" / "html"

# fixture -> expected canonical section count (PALLAVI + swara-sAhitya sections),
# matching the English/Latin template each krithi is published against.
CATB_FIXTURES = [
    ("catb_kamakshi_anudinamu.html", 9),
    ("catb_kamakshi_ni_pada.html", 12),
    ("catb_rave_hima_giri_kumari.html", 7),
]

EXPECTED_LANGUAGES = {"en", "sa", "ta", "te", "kn", "ml"}


def _parse_fixture(name: str):
    html = (FIXTURE_DIR / name).read_text(encoding="utf-8")
    extracted = HtmlTextExtractor().extract(html, base_url="http://example.com/x.html")
    normalized = normalize_garbled_diacritics(extracted.text)
    return StructureParser().parse(normalized)


class TestCatBFullSectionSet:
    @pytest.mark.parametrize("fixture,expected", CATB_FIXTURES)
    def test_all_variants_recover_full_ordered_sections(self, fixture: str, expected: int) -> None:
        result = _parse_fixture(fixture)
        variants = {v.language: v for v in result.lyric_variants}

        # Every script variant present on the page must be recovered.
        assert EXPECTED_LANGUAGES.issubset(variants.keys()), (
            f"{fixture}: missing variants {EXPECTED_LANGUAGES - variants.keys()}"
        )

        for lang in EXPECTED_LANGUAGES:
            sections = variants[lang].sections
            types = [s.section_type.value for s in sections]
            # Full count (regression: Indic variants used to collapse to 1 blob).
            assert len(sections) == expected, (
                f"{fixture}/{lang}: {len(sections)} sections, expected {expected}: {types}"
            )
            # Correct order: PALLAVI first, every remaining section swara-sAhitya.
            assert types[0] == "PALLAVI", f"{fixture}/{lang}: first section {types[0]}"
            assert set(types[1:]) == {"SWARA_SAHITYA"}, f"{fixture}/{lang}: {types}"
            assert [s.order for s in sections] == list(range(1, expected + 1))

    @pytest.mark.parametrize("fixture,expected", CATB_FIXTURES)
    def test_closing_mudra_present_not_truncated(self, fixture: str, expected: int) -> None:
        """The final section must retain Syama Sastri's "SyAma kRshNa" mudra.

        This is the text the 1800-byte truncation used to drop. Checked on the
        Latin variant, whose transliteration is script-independent.
        """
        result = _parse_fixture(fixture)
        en = next(v for v in result.lyric_variants if v.language == "en")
        last = re.sub(r"\s+", " ", en.sections[-1].text)
        assert re.search(r"SyAma\s*kRshNa", last, re.IGNORECASE), (
            f"{fixture}: closing mudra missing from final section — truncated? {last!r}"
        )


class TestWorkerDoesNotTruncateLyricSections:
    """Directly locks the removal of the 1800-byte per-section byte cap."""

    def test_long_indic_section_preserved_verbatim(self) -> None:
        worker = ExtractionWorker(ExtractorConfig())
        # ~2500-byte Devanagari section (well over the old 1800-byte cap).
        long_text = "श्री कामाक्षि " * 200
        assert len(long_text.encode("utf-8")) > 1800
        variants = [
            CanonicalLyricVariant(
                language="sa",
                script="devanagari",
                sections=[CanonicalLyricSection(section_order=1, text=long_text)],
            )
        ]
        out = worker.pdf_strategy._filter_empty_lyric_sections(variants)
        assert out[0].sections[0].text == long_text, "lyric section was truncated"

    def test_empty_sections_still_filtered(self) -> None:
        worker = ExtractionWorker(ExtractorConfig())
        variants = [
            CanonicalLyricVariant(
                language="en",
                script="latin",
                sections=[
                    CanonicalLyricSection(section_order=1, text="pallavi text"),
                    CanonicalLyricSection(section_order=2, text="   "),
                ],
            )
        ]
        out = worker.pdf_strategy._filter_empty_lyric_sections(variants)
        assert len(out[0].sections) == 1
        assert out[0].sections[0].text == "pallavi text"
