"""TRACK-133 follow-up: per-variant segmentation of a pure ragamalika.

``mAdhavO mAM pAtu`` (Dikshitar, the Dashavatara Ragamalika) has ten avatara
stanzas, each in its own raga and each carrying a Madhyamakala Sahitya. The
canonical structure (built from the transliterated Latin text) segments into ten
``OTHER`` stanzas via the RAGA_SEGMENT boundary logic. Previously the *variant*
side did not: the Latin ``<raga> rAgaM`` pattern never matched the Indic-script
headers (Devanagari ``रागं``, Tamil ``ராகம்``, ...), and the leading-OTHER
promotion in ``_sections_from_variant_blocks`` folded every stanza into one, so
each of the six variants collapsed to a single section (canon 10 vs variant 1).

The fixture is the real html_extractor output for the live source page, captured
once — no network. These tests pin that all six script variants now segment into
the same ten sections as canon, in order, with no stanza dropped.
"""

from __future__ import annotations

from pathlib import Path

from src.structure_parser import StructureParser

FIXTURE = Path(__file__).parent / "fixtures" / "structure_parser" / "madhavo_mam_patu_multiscript.txt"

# First (Matsya) and last (Kalki) avatara markers per script — used to assert the
# variant sections stay in stanza order and the final stanza is never dropped.
_MATSYA = ("matsy", "मत्स्य", "మత్స్", "மத்ஸ்", "ಮತ್ಸ್", "മത്സ്")
_KALKI = ("kali", "कलि", "కలి", "கலி", "ಕಲಿ", "കലി")

_EXPECTED_SCRIPTS = {"latin", "devanagari", "tamil", "telugu", "kannada", "malayalam"}


def _parse():
    return StructureParser().parse(FIXTURE.read_text(encoding="utf-8"))


def test_canonical_structure_has_ten_stanzas() -> None:
    result = _parse()
    assert len(result.sections) == 10
    assert {s.section_type.value for s in result.sections} == {"OTHER"}


def test_all_six_variants_segment_into_ten_aligned_sections() -> None:
    result = _parse()

    assert {v.script for v in result.lyric_variants} == _EXPECTED_SCRIPTS
    assert len(result.lyric_variants) == 6

    for variant in result.lyric_variants:
        sections = variant.sections
        # Every variant matches the canonical section count exactly.
        assert len(sections) == len(result.sections) == 10, (variant.script, len(sections))
        # Orders are 1..10, contiguous and aligned with canon.
        assert [s.order for s in sections] == [c.order for c in result.sections]
        # First stanza is Matsya, last is Kalki — proves order and no dropped stanza.
        assert any(m in sections[0].text for m in _MATSYA), variant.script
        assert any(k in sections[9].text for k in _KALKI), variant.script


def test_variant_stanzas_fold_in_madhyamakala_sahitya() -> None:
    """Where the Madhyamakala Sahitya marker is recognized it is demoted into its
    parent stanza (never counted as an extra section). Demotion is all-or-nothing
    per script: a variant has the MKS block in all ten stanzas or in none — it is
    never partial, which would signal a mis-segmentation.

    Latin (the canonical-bearing script), Devanagari and Tamil fold it into all ten.
    The Tamil marker form ``(மத் 4 யம கால ஸாஹித்யம்)`` — the aspirated dha written
    with a spaced-out grantha numeral — is recognized alongside the other scripts
    (TRACK-133 BUG 2 fix), so Tamil demotes exactly like Devanagari rather than
    leaving the MKS text inline.
    """
    result = _parse()
    by_script = {v.script: v for v in result.lyric_variants}

    for script, variant in by_script.items():
        with_mks = sum("[Madhyama Kala Sahitya]" in s.text for s in variant.sections)
        assert with_mks in (0, len(variant.sections)), (script, with_mks)

    for script in ("latin", "devanagari", "tamil"):
        variant = by_script[script]
        assert all("[Madhyama Kala Sahitya]" in s.text for s in variant.sections), script


def test_indic_raga_header_gating_off_without_pure_ragamalika() -> None:
    """The Indic raga-header boundary only fires in pure-ragamalika mode: a document
    with ordinary P/A/C structure keeps the probe dormant, so a lyric line ending in
    an Indic raga word is not treated as a section boundary."""
    parser = StructureParser()
    parser._build_blocks("पल्लवि\nहरि राग\nअनुपल्लवि\nचरण")
    assert parser._raga_segment_enabled is False
