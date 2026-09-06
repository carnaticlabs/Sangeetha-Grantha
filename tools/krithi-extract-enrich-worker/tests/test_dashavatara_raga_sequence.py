"""TRACK-139: Dashavatara ragamalika raga sequence from stanza headers.

Segmentation into ten OTHER stanzas is already pinned by
``test_ragamalika_segmentation``. This pins the ordered raga names that
Kotlin ingestion writes to ``krithi_ragas`` (replacing V61 SQL).
"""

from __future__ import annotations

from pathlib import Path

from src.extraction_strategies import _TextPipelineStrategy
from src.structure_parser import StructureParser

FIXTURE = Path(__file__).parent / "fixtures" / "structure_parser" / "madhavo_mam_patu_ragamalika.txt"

# Adjudicated sequence (TRACK-133 Round 3 / V61): honorific "SrI gauLa" → gauLa;
# bare "SrI" is the raga Sri.
EXPECTED_RAGAS = [
    "nATa",
    "gauLa",
    "SrI",
    "Arabhi",
    "varALi",
    "kEdAra",
    "vasanta",
    "suraTi",
    "saurAshTra",
    "madhyamAvati",
]


def test_madhavo_stanza_headers_emit_ordered_raga_subsections() -> None:
    result = StructureParser().parse(FIXTURE.read_text(encoding="utf-8"))
    names = [sub.raga_name for sub in result.ragamalika_subsections if not sub.is_viloma]
    assert names == EXPECTED_RAGAS, names
    assert [sub.order for sub in result.ragamalika_subsections] == list(range(1, 11))


def test_build_ragas_uses_dashavatara_sequence_not_unknown() -> None:
    result = StructureParser().parse(FIXTURE.read_text(encoding="utf-8"))
    ragas = _TextPipelineStrategy._build_ragas(result, "Unknown")
    assert [r.name for r in ragas] == EXPECTED_RAGAS
    assert [r.order for r in ragas] == list(range(1, 11))
