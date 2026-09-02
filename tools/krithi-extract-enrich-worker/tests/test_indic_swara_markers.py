"""TRACK-133: Indic-script inline swara-sahitya ordinal markers must segment.

The thyagaraja-vaibhavam blog labels each swara-sahitya sub-block in the
English/IAST variant with the full words ``svara sAhitya N``, but in the
transliterated variants with the abbreviated cluster ``sva`` + ordinal +
period on the same line (``स्व1.``, ``ஸ்வ4(A).``). Before this fix only the
lone bare ``स्वर साहित्य`` group title was detected, so every ``sva N``
sub-block collapsed into a single ``SWARA_SAHITYA`` section and the Indic
variant under-segmented relative to the English canonical skeleton
(e.g. sAdhincenE: 11 canon vs 5 Indic).

These tests pin the segmentation for all five scripts, guard the Bucket-A
krithis that already parsed correctly, and guard against false positives on
lyric lines that merely begin with the ``sva`` cluster.
"""

from __future__ import annotations

import json
from pathlib import Path

from src.structure_parser import StructureParser

FIXTURE_DIR = Path(__file__).parent / "fixtures" / "structure_parser"


def test_fixture_swara_sahitya_multi_variant() -> None:
    """Every script variant segments the swara-sahitya block to the canonical count."""
    parser = StructureParser()
    text = (FIXTURE_DIR / "tyagaraja_swara_sahitya_multi_variant.txt").read_text(encoding="utf-8")
    expected = json.loads(
        (FIXTURE_DIR / "tyagaraja_swara_sahitya_multi_variant.expected.json").read_text(encoding="utf-8")
    )

    result = parser.parse(text)

    assert [s.section_type.value for s in result.sections] == expected["sections"]
    assert [v.script for v in result.lyric_variants] == expected["variantScripts"]
    assert [v.language for v in result.lyric_variants] == expected["variantLanguages"]
    assert [b.label for b in result.metadata_boundaries] == expected["metadataBoundaryLabels"]

    # The core regression: no variant collapses the swara-sahitya sub-blocks.
    for variant in result.lyric_variants:
        assert len(variant.sections) == expected["perVariantSectionCount"], (
            f"variant {variant.language} has {len(variant.sections)} sections "
            f"{[s.section_type.value for s in variant.sections]} — expected "
            f"{expected['perVariantSectionCount']}; sva-ordinal markers not segmented"
        )
        # Exactly three SWARA_SAHITYA sections, matching the English "svara sAhitya 1..3".
        swara = [s for s in variant.sections if s.section_type.value == "SWARA_SAHITYA"]
        assert len(swara) == 3, f"variant {variant.language}: {len(swara)} swara sections, expected 3"


def _detect(parser: StructureParser, line: str, *, block_text: str) -> str | None:
    """Detect a header the way the parser does — probe-gating is per-block."""
    parser._build_blocks(block_text)  # sets the context-gated probe flags
    match = parser._detect_section_header(line)
    return match.label if match else None


def test_sva_ordinal_marker_detected_when_present() -> None:
    """A ``स्व2.`` marker is a SWARA_SAHITYA header once the block enables the probe."""
    parser = StructureParser()
    block = "स्व1. देवकि वसु देवुल\nस्व2. रंगेशुडु सद्गंगा"
    assert _detect(parser, "स्व2. रंगेशुडु सद्गंगा", block_text=block) == "SWARA_SAHITYA"


def test_sva_ordinal_with_paren_suffix_detected() -> None:
    """The ``(A)`` continuation form ``स्व4(A).`` is also a SWARA_SAHITYA header."""
    parser = StructureParser()
    block = "स्व4. वनितल सदा\nस्व4(A). सारासारुडु सनक"
    assert _detect(parser, "स्व4(A). सारासारुडु सनक", block_text=block) == "SWARA_SAHITYA"


def test_all_scripts_sva_marker_detected() -> None:
    """The ordinal marker is recognised in every one of the five scripts."""
    parser = StructureParser()
    for marker in ("स्व2.", "ஸ்வ2.", "స్వ2.", "ಸ್ವ2.", "സ്വ2."):
        line = f"{marker} test lyric"
        assert _detect(parser, line, block_text=line) == "SWARA_SAHITYA", f"{marker!r} not detected"


def test_bare_sva_word_is_not_a_swara_marker() -> None:
    """A lyric word beginning with the ``sva`` cluster but lacking digit+period
    must NOT be misread as a swara-sahitya header (false-positive guard)."""
    parser = StructureParser()
    # "svapna" (dream) / "svaprakasha" — starts with स्व, no ordinal + period.
    block = "स्वप्न दर्शन\nस्वप्रकाश रूप"
    assert _detect(parser, "स्वप्न दर्शन", block_text=block) is None
    assert _detect(parser, "स्वप्रकाश रूप", block_text=block) is None


def test_probe_off_leaves_full_word_header_only() -> None:
    """A block using only the full-word ``स्वर साहित्य`` header (no ordinals)
    keeps the existing single-header behaviour — the new inline patterns stay
    dormant because the probe never fires."""
    parser = StructureParser()
    block = "स्वर साहित्य\nरंगेशुडु सद्गंगा जनकुडु"
    parser._build_blocks(block)
    assert parser._inline_indic_swara_enabled is False
    assert parser._detect_section_header("स्वर साहित्य") is not None
