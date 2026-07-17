"""Regression tests for the Tamil translation-trailer stripper (ADR-015).

thyagaraja-vaibhavam Tamil variants append an inline word-by-word Tamil *meaning*
after the lyric. Marker splitting leaves it glued to the final charanam, blowing
the last section up to 5-18x its siblings. `strip_refrain_trailer` cuts everything
after the final "(refrain)" cue in the last section, but only when the removed tail
is natural-language prose (no inline HK pronunciation digits) — so real lyric is
never at risk.
"""

from __future__ import annotations

from pathlib import Path

from src.diacritic_normalizer import normalize_garbled_diacritics
from src.html_extractor import HtmlTextExtractor
from src.structure_parser import (
    DetectedSection,
    SectionType,
    StructureParser,
    strip_refrain_trailer,
)

FIXTURE_DIR = Path(__file__).parent / "fixtures" / "html"


def _sec(order: int, type_: SectionType, text: str) -> DetectedSection:
    return DetectedSection(section_type=type_, order=order, label=type_.value.title(),
                           text=text, start_pos=0, end_pos=len(text))


# A transliterated charanam (digit-bearing lines) followed by a Tamil prose trailer.
_LYRIC_CHARANAM = (
    "யோகி3 ஹ்ரு2த3ய நீவே க3தி(ய)னு ஜன\n"
    "பா4க3வத ப்ரிய த்யாக3ராஜ நுத\n"
    "நா(கா3)சலமுபை பா3கு3க3 நெலகொன்ன (வேங்க)"
)
_TAMIL_TRAILER = (
    "வேங்கடேசா! உன்னை சேவிக்க\n"
    "பதினாயிரம் கண்கள் வேணுமய்யா\n"
    "மங்களமான, தெய்வீக உருவமேற்றுக்கொண்ட வேங்கடேசா!"
)


class TestStripRefrainTrailerUnit:
    def test_prose_trailer_after_lyric_is_cut(self) -> None:
        secs = [
            _sec(1, SectionType.PALLAVI, "வேங்கடேஸ1 நினு ஸேவிம்பனு பதி3"),
            _sec(2, SectionType.ANUPALLAVI, "பங்க(ஜா)க்ஷ ... கொன்ன (வேங்க)"),
            _sec(3, SectionType.CHARANAM, _LYRIC_CHARANAM + "\n" + _TAMIL_TRAILER),
        ]
        out = strip_refrain_trailer(secs)
        assert out[-1].text.rstrip().endswith("(வேங்க)")
        assert "பதினாயிரம்" not in out[-1].text  # the Tamil prose meaning is gone

    def test_clean_lyric_without_trailer_untouched(self) -> None:
        """A last section that is all transliterated lyric is never trimmed."""
        secs = [
            _sec(1, SectionType.PALLAVI, "வேங்கடேஸ1 நினு"),
            _sec(2, SectionType.ANUPALLAVI, "பங்க(ஜா)க்ஷ (வேங்க)"),
            _sec(3, SectionType.CHARANAM, _LYRIC_CHARANAM),
        ]
        assert strip_refrain_trailer(secs) == secs

    def test_single_trailing_line_not_cut(self) -> None:
        """One short line after the lyric is not a multi-line prose block — keep it."""
        secs = [
            _sec(1, SectionType.PALLAVI, "பா3 ரகு3"),
            _sec(2, SectionType.CHARANAM, _LYRIC_CHARANAM + "\nஇராமா!"),
        ]
        assert strip_refrain_trailer(secs) == secs

    def test_latin_variant_never_stripped(self) -> None:
        """Latin has no Indic pronunciation digits, so the boundary can't be found."""
        secs = [
            _sec(1, SectionType.PALLAVI, "vEnkaTESa ninu"),
            _sec(2, SectionType.CHARANAM, "yOgi hRdaya nelakonna (vEnka)\nprose one\nprose two here"),
        ]
        assert strip_refrain_trailer(secs) == secs


class TestVenkatesaFixture:
    def _parse(self):
        html = (FIXTURE_DIR / "tv_venkatesa_tamil_trailer.html").read_text(encoding="utf-8")
        txt = normalize_garbled_diacritics(HtmlTextExtractor().extract(html, base_url="http://x/y").text)
        return {v.language: v for v in StructureParser().parse(txt).lyric_variants}

    def test_tamil_last_section_trimmed_to_refrain(self) -> None:
        variants = self._parse()
        ta = variants["ta"]
        assert [s.section_type.value for s in ta.sections] == [
            "PALLAVI", "ANUPALLAVI", "CHARANAM", "CHARANAM", "CHARANAM"]
        # last section ends at the refrain cue, and is no longer a 5-18x trailer blob
        assert ta.sections[-1].text.rstrip().endswith("(வேங்க)")
        en_last = len(variants["en"].sections[-1].text)
        assert len(ta.sections[-1].text) <= 2 * en_last

    def test_other_scripts_unaffected(self) -> None:
        variants = self._parse()
        for lang in ("en", "sa", "te", "kn", "ml"):
            assert len(variants[lang].sections) == 5
