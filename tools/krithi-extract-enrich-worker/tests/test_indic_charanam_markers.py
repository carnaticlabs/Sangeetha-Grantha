"""TRACK-133 (round 2): a second inline-Indic charanam-marker gap.

The Bucket-B swara-marker fix cleared 20 krithis; the musicologist's adjudication
of the residual surfaced a distinct, one-class-over gap: a charanam/section marker
in the Indic scripts is not detected, so the charanam(s) collapse into the preceding
block even though the full sahitya is present. Three undetected forms (all analogues
of ``INLINE_INDIC_PAC_PATTERNS`` / ``INLINE_INDIC_SWARA_PATTERNS`` on the same
``_detect_section_header`` seam):

* form 1 — bare ``ca`` akshara, no digit/period (``च`` / ``ச`` / ``చ`` / ``ಚ`` / ``ച``)
  heading a line with lyric glued on — ``kaNTa jUDumi``, ``enta bhAgyamu``.
* form 2 — full-word inline charanam in the dental-nasal spelling (``చరనం``) — the
  charanams in ``ennEramum un pAda`` merged into the anupallavi.
* form 3 — digit WITHOUT trailing period (``च4 …`` vs the period-terminated ``च4.``
  already handled) — ``rAma Eva daivataM``.

Plus a false-positive guard: a sahitya line that merely opens with the word
``caraNam`` (a ``tvac-caraNam`` hyphenation continuation) must NOT fake a header —
``ramA ramaNa rArA`` was over-split in en/ta. The guard keeps re-extract from
reintroducing that split.
"""

from __future__ import annotations

from pathlib import Path

import pytest

from src.structure_parser import StructureParser

FIXTURE_DIR = Path(__file__).parent / "fixtures" / "structure_parser"


def _section_types(fixture: str) -> list[str]:
    text = (FIXTURE_DIR / fixture).read_text(encoding="utf-8")
    return [s.section_type.value for s in StructureParser().parse(text).sections]


# --- form 1: bare akshara charanam marker (calibrated to REAL source) ----------
#
# Real extracted charanam markers (from the live re-extract capture): the charanam
# akshara stands as its own token followed by whitespace. Pallavi/anupallavi are
# UNMARKED in these documents, so the marker is self-gated on the bare-token line,
# not on a bare P/A twin.


def _detect(parser: StructureParser, line: str, *, block_text: str) -> str | None:
    """Detect a header the way the parser does — probe-gating is per-block."""
    parser._build_blocks(block_text)
    m = parser._detect_section_header(line)
    return m.label if m else None


def test_real_bare_charanam_markers_detected() -> None:
    """The real bare-``ca`` markers (enta ``च मुन्नु``, kaNTa-ta ``ச அल``) fire."""
    parser = StructureParser()
    enta_sa = "चॆन्त जेरि सौजन्युडै\nचिन्त बाग तॊलगिञ्चि\nच मुन्नु नी समीपमुन वॆलयु"
    assert _detect(parser, "च मुन्नु नी समीपमुन वॆलयु", block_text=enta_sa) == "CHARANAM"
    kanta_ta = "ச அल நாடு3 ஸௌமித்ரி பாத3 ஸேவ"
    assert _detect(parser, "ச அல நாடு3 ஸௌமித்ரி பாத3 ஸேவ", block_text=kanta_ta) == "CHARANAM"


def test_real_lyric_words_are_not_bare_markers() -> None:
    """A vowel-matra-bound akshara that heads a lyric word (real: ``चॆन्त``/``चिन्त``/
    ``செந்த``/``சிந்த``) must NOT be read as a bare charanam marker — the matra binds
    with no space, so the token discriminator rejects it."""
    parser = StructureParser()
    enta_sa = "चॆन्त जेरि\nचिन्त बाग\nच मुन्नु नी"
    assert _detect(parser, "चॆन्त जेरि सौजन्युडै", block_text=enta_sa) is None
    assert _detect(parser, "चिन्त बाग तॊलगिञ्चि", block_text=enta_sa) is None
    enta_ta = "செந்த ஜேரி\nசிந்த பா3க3\nச முன்னு நீ"
    assert _detect(parser, "செந்த ஜேரி ஸௌஜன்யுடை3", block_text=enta_ta) is None
    assert _detect(parser, "சிந்த பா3க3 தொலகி3ஞ்சி", block_text=enta_ta) is None


def test_bare_charanam_self_gated_on_marker_presence() -> None:
    """The bare-``ca`` pattern activates only when a bare-token line is present."""
    parser = StructureParser()
    # A bare charanam-token line IS a marker context -> enabled.
    parser._build_blocks("ச அல நாடு3 ஸௌமித்ரி")
    assert parser._inline_indic_bare_enabled is True
    # A document with no bare-token line (matra-bound word only) -> disabled.
    parser._build_blocks("சிந்த பா3க3 தொலகி3ஞ்சி ப்3ரோசிதிவி")
    assert parser._inline_indic_bare_enabled is False


@pytest.mark.parametrize("marker", ["च", "చ", "ச", "ಚ", "ച"])
def test_bare_charanam_all_five_scripts(marker: str) -> None:
    parser = StructureParser()
    line = f"{marker} test lyric here"
    assert _detect(parser, line, block_text=line) == "CHARANAM", f"{marker!r} not detected"


# --- form 2: full-word inline charanam, dental-nasal spelling ------------------


def test_enneramum_dental_na_charanam_segments() -> None:
    """ennEramum un pAda: dental-na ``చరనం`` charanams no longer merge into the
    anupallavi -> P + A + 4C = 6."""
    assert _section_types("enneramum_un_pada_fullword_charanam.txt") == [
        "PALLAVI",
        "ANUPALLAVI",
        "CHARANAM",
        "CHARANAM",
        "CHARANAM",
        "CHARANAM",
    ]


@pytest.mark.parametrize(
    "header",
    # Tamil covers BOTH the alveolar ``ன`` (U+0BA9, the real ennEramum-ta spelling
    # ``சரனம்``) and the dental ``ந`` (U+0BA8) forms.
    ["चरनं", "சரனம்", "சரநம்", "చరనం", "ಚರನಂ", "ചരനം"],
)
def test_dental_na_charanam_header_detected(header: str) -> None:
    """The dental-nasal charanam spelling is recognised in every script."""
    parser = StructureParser()
    m = parser._detect_section_header(header)
    assert m is not None and m.label == "CHARANAM", f"{header!r} -> {m}"


def test_real_tamil_charanam_marker_with_ordinal_and_superscript_body() -> None:
    """ennEramum (ta): the real ``சரனம் 1`` marker (alveolar ``ன``) fires, and the
    interleaved superscript pronunciation digits in the body (``ஆதி 3 ஸ 1 க்தி``)
    do not interfere with the marker match."""
    parser = StructureParser()
    block = "சரனம் 1\nஆதி 3 ஸ 1 க்தி உந்தன் மஹிமையை\nசரனம் 2\nதாமரை இலை-மேல்"
    parser._build_blocks(block)
    for header in ("சரனம் 1", "சரனம் 2", "சரனம் 3", "சரனம் 4"):
        m = parser._detect_section_header(header)
        assert m is not None and m.label == "CHARANAM", f"{header!r} -> {m}"


# --- form 3: digit without trailing period ------------------------------------


def test_rama_eva_daivatam_digit_no_period_segments() -> None:
    """rAma Eva daivataM (Devanagari): ``च4`` (no period) charanam markers split
    -> P + 6C = 7 (charanam 4 no longer merges into charanam 3)."""
    assert _section_types("rama_eva_daivatam_digit_no_period.txt") == [
        "PALLAVI",
        "CHARANAM",
        "CHARANAM",
        "CHARANAM",
        "CHARANAM",
        "CHARANAM",
        "CHARANAM",
    ]


@pytest.mark.parametrize(
    "header",
    ["च4 सुर तारक", "ச4 ஸுர", "చ4 సుర", "ಚ4 ಸುರ", "ച4 സുര"],
)
def test_digit_no_period_charanam_detected_when_enabled(header: str) -> None:
    parser = StructureParser()
    parser._build_blocks(header)
    assert parser._inline_indic_digit_enabled is True
    m = parser._detect_section_header(header)
    assert m is not None and m.label == "CHARANAM", f"{header!r} -> {m}"


def test_digit_marker_gated_off_by_default() -> None:
    parser = StructureParser()
    assert getattr(parser, "_inline_indic_digit_enabled", False) is False


# --- caraNam-in-sahitya false-positive guard ----------------------------------


def test_rama_ramana_rara_caranam_guard_not_over_split() -> None:
    """ramA ramaNa rArA: the ``tvac-caraNam`` continuation line must NOT fake an
    eighth charanam -> P + 6C = 7, not 8."""
    types = _section_types("rama_ramana_rara_caranam_guard.txt")
    assert types == ["PALLAVI"] + ["CHARANAM"] * 6, types
    assert types.count("CHARANAM") == 6


def test_caranam_lyric_line_is_not_a_header() -> None:
    """A line opening with the word caraNam but continuing with sahitya is lyric."""
    parser = StructureParser()
    assert parser._detect_section_header("caraNam bhava tAraNambu cEsunu") is None
    # Tamil hyphenation continuation.
    assert parser._detect_section_header("சரணம் ப4வ தாரணம்பு") is None


def test_standalone_caranam_header_still_detected() -> None:
    """A genuine standalone charanam header (optionally with an ordinal) is kept."""
    parser = StructureParser()
    for header in ("caraNam", "caraNam 2", "caraNam 4A", "charaNam"):
        m = parser._detect_section_header(header)
        assert m is not None and m.label == "CHARANAM", f"{header!r} -> {m}"
