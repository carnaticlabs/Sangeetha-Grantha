"""TRACK-139: Sri-ranjani stays distinct; viloma headers are not forward ragas."""

from __future__ import annotations

from src.structure_parser import StructureParser


def test_sri_ranjani_honorific_is_not_stripped() -> None:
    result = StructureParser().parse("nATa rAgaM\nfirst\nSrI ranjani rAgaM\nsecond\n")
    names = [sub.raga_name for sub in result.ragamalika_subsections]
    assert "SrI ranjani" in names
    assert "ranjani" not in names


def test_viloma_header_is_not_a_forward_raga() -> None:
    result = StructureParser().parse("nATa rAgaM\nforward\nvilOma - mOhana rAgaM\nreverse\n")
    viloma = [sub for sub in result.ragamalika_subsections if sub.is_viloma]
    forward = [sub.raga_name for sub in result.ragamalika_subsections if not sub.is_viloma]
    assert [sub.raga_name for sub in viloma] == ["mOhana"]
    assert "vilOma - mOhana" not in forward
    assert forward == ["nATa"]
