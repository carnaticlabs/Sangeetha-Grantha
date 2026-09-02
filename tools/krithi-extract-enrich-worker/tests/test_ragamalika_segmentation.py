"""TRACK-133 (round 2): pure-ragamalika raga-header segmentation.

``mAdhavO mAM pAtu`` is the Dashavatara Ragamalika — ten avatara stanzas, each in
its own raga and each carrying a Madhyamakala Sahitya. It never segmented because
the parser did not read ``<raga> rAgaM`` lines as section boundaries, so the whole
lyric stayed one unsegmented blob (canon 0 sections). These tests pin the text
segmentation only — the ``is_ragamalika`` flag and the ordered ``krithi_ragas`` rows
are metadata owned by postgres-engineer, out of the splitter's scope.

The segmentation is context-gated: it fires only for a *pure* ragamalika (multiple
raga headers, no ordinary P/A/C structure). An ordinary ragamalika whose raga markers
are nested inside pallavi/anupallavi/charanam (e.g. ``SrI viSva nAthaM``) keeps its
P/A/C segmentation with ragas as metadata subsections.
"""

from __future__ import annotations

from pathlib import Path

from src.structure_parser import StructureParser

FIXTURE_DIR = Path(__file__).parent / "fixtures" / "structure_parser"


def test_madhavo_dashavatara_splits_into_ten_stanzas() -> None:
    """The ten avatara stanzas each become their own section (Madhyamakala Sahitya
    attached to its parent stanza, not counted separately)."""
    text = (FIXTURE_DIR / "madhavo_mam_patu_ragamalika.txt").read_text(encoding="utf-8")
    result = StructureParser().parse(text)

    assert len(result.sections) == 10, [s.section_type.value for s in result.sections]
    # Section typing per stanza is a lakshana call left downstream -> emit OTHER.
    assert {s.section_type.value for s in result.sections} == {"OTHER"}
    # Each stanza carries its Madhyamakala Sahitya inline (demoted into the parent).
    assert all("[Madhyama Kala Sahitya]" in s.text for s in result.sections)
    # Single variant, matching the DB (one script only).
    assert len(result.lyric_variants) == 1
    assert len(result.lyric_variants[0].sections) == 10


def test_raga_segmentation_gated_off_for_ordinary_ragamalika() -> None:
    """The existing ragamalika fixture (raga markers nested in P/A/C) is unaffected:
    it still segments as PALLAVI/ANUPALLAVI/CHARANAM, ragas remaining subsections."""
    text = (FIXTURE_DIR / "ragamalika_multi_variant.txt").read_text(encoding="utf-8")
    parser = StructureParser()
    parser._build_blocks(text)
    assert parser._raga_segment_enabled is False

    result = parser.parse(text)
    assert [s.section_type.value for s in result.sections] == ["PALLAVI", "ANUPALLAVI", "CHARANAM"]
    # Raga markers are still captured as metadata subsections, not boundaries.
    assert len(result.ragamalika_subsections) >= 5


def test_raga_segment_probe_requires_multiple_headers_and_no_pac() -> None:
    parser = StructureParser()
    # A single raga header is not enough to switch on pure-ragamalika mode.
    parser._build_blocks("nATa rAgaM\nmAdhavO mAM pAtu")
    assert parser._raga_segment_enabled is False
