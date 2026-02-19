"""Tests for section structure detection."""

import json
from pathlib import Path

from src.schema import SectionType
from src.structure_parser import StructureParser


def test_standard_pac_structure() -> None:
    """Detect standard Pallavi-Anupallavi-Charanam structure."""
    parser = StructureParser()
    text = """
Pallavi
Vatapi Ganapatim Bhaje aham
varanasidhi vinayakam

Anupallavi
bhoota adi samsevita gajaananam
paravyoma vihara

Charanam
hariharaa putram aham
puraa kuumbha sambhava
    """
    sections = parser.parse_sections(text)

    assert len(sections) == 3
    assert sections[0].section_type == SectionType.PALLAVI
    assert sections[1].section_type == SectionType.ANUPALLAVI
    assert sections[2].section_type == SectionType.CHARANAM


def test_samashti_charanam_structure() -> None:
    """Detect Samashti Charanam (common in Dikshitar compositions)."""
    parser = StructureParser()
    text = """
Pallavi
Sri Nathadi Guruguham

Anupallavi
Nathadi guru guham upasmahe

Samashti Charanam
pranatharthi haram
prapancha virahitam
    """
    sections = parser.parse_sections(text)

    assert len(sections) == 3
    assert sections[2].section_type == SectionType.SAMASHTI_CHARANAM


def test_multiple_charanams() -> None:
    """Detect multiple numbered Charanams (common in Thyagaraja)."""
    parser = StructureParser()
    text = """
Pallavi
Endaro Mahanubhavulu

Anupallavi
chanduru varnu

Charanam 1
sree raghu veerundu

Charanam 2
kanakana ruchiraa

Charanam 3
sarasa sama dana
    """
    sections = parser.parse_sections(text)

    # Should detect Pallavi + Anupallavi + 3 Charanams = 5 sections
    assert len(sections) >= 4
    pallavi_count = sum(1 for s in sections if s.section_type == SectionType.PALLAVI)
    anupallavi_count = sum(1 for s in sections if s.section_type == SectionType.ANUPALLAVI)
    charanam_count = sum(1 for s in sections if s.section_type == SectionType.CHARANAM)
    assert pallavi_count == 1
    assert anupallavi_count == 1
    assert charanam_count >= 2


def test_chittaswaram_detection() -> None:
    """Detect Chittaswaram section."""
    parser = StructureParser()
    text = """
Pallavi
Some text here

Anupallavi
More text

Charanam
Charanam lyrics

Chittaswaram
S R G M P D N S
    """
    sections = parser.parse_sections(text)

    chittaswaram_sections = [s for s in sections if s.section_type == SectionType.CHITTASWARAM]
    assert len(chittaswaram_sections) == 1


def test_empty_text() -> None:
    """Empty text returns no sections."""
    parser = StructureParser()
    sections = parser.parse_sections("")
    assert sections == []


def test_no_labels_returns_other() -> None:
    """Text without section labels is classified as OTHER."""
    parser = StructureParser()
    text = "This is just some plain text without any section labels."
    sections = parser.parse_sections(text)

    assert len(sections) == 1
    assert sections[0].section_type == SectionType.OTHER


def test_garbled_charanam_detection() -> None:
    """Detect garbled 'caran. am' from Utopia-font PDFs."""
    parser = StructureParser()
    text = """
pallavi
akhil¯an. d. e´svari raks.a m¯a ˙m

anupallavi
nikhilalokanity¯atmike vimale

caran. am
lambodara guruguha p¯ujite
    """
    sections = parser.parse_sections(text)

    assert len(sections) == 3
    assert sections[0].section_type == SectionType.PALLAVI
    assert sections[1].section_type == SectionType.ANUPALLAVI
    assert sections[2].section_type == SectionType.CHARANAM


def test_canonical_section_conversion() -> None:
    """Verify conversion to canonical section DTOs."""
    parser = StructureParser()
    text = """
Pallavi
Some text

Anupallavi
More text
    """
    detected = parser.parse_sections(text)
    canonical = parser.to_canonical_sections(detected)

    assert len(canonical) == 2
    assert canonical[0].type == SectionType.PALLAVI
    assert canonical[0].order == 1
    assert canonical[1].type == SectionType.ANUPALLAVI
    assert canonical[1].order == 2


def test_madhyama_kala_detection() -> None:


    """Detect Madhyama Kala sections in various formats."""


    parser = StructureParser()


    text = """Pallavi


Some text





Charanam


Regular charanam lyrics


(madhyama kAla sAhityam)


Speedy lyrics here


"""


    sections = parser.parse_sections(text)





    # Should detect Pallavi, Charanam, and Madhyama Kala


    assert len(sections) == 3


    assert sections[0].section_type == SectionType.PALLAVI


    assert sections[1].section_type == SectionType.CHARANAM


    assert sections[2].section_type == SectionType.MADHYAMA_KALA


    assert "Speedy lyrics" in sections[2].text


def test_parse_contract_tracks_metadata_boundaries() -> None:
    """Parser contract should expose metadata boundaries and isolate lyric text."""
    parser = StructureParser()
    text = """
Pallavi
akhilandesvari raksha mam

Charanam
siva sankari jagadambike

Meaning
This prose block should never become lyric payload.
"""

    result = parser.parse(text)

    assert len(result.sections) == 2
    assert len(result.metadata_boundaries) == 1
    assert result.metadata_boundaries[0].label == "MEANING"
    assert all("prose block" not in section.text.lower() for section in result.sections)


def test_metadata_boundaries_map_to_canonical_aliases() -> None:
    """Metadata boundaries should serialize with canonical camelCase aliases."""
    parser = StructureParser()
    text = """
Pallavi
vatapi ganapatim bhaje

Notes
Editorial note text.
"""

    result = parser.parse(text)
    canonical = parser.to_canonical_metadata_boundaries(result.metadata_boundaries)

    assert len(canonical) == 1
    payload = canonical[0].model_dump(by_alias=True)
    assert payload["label"] == "NOTES"
    assert "startOffset" in payload
    assert "endOffset" in payload


def test_fixture_kotlin_parity_multiscript() -> None:
    parser = StructureParser()
    fixture_dir = Path(__file__).parent / "fixtures" / "structure_parser"
    text = (fixture_dir / "kotlin_parity_multiscript.txt").read_text(encoding="utf-8")
    expected = json.loads(
        (fixture_dir / "kotlin_parity_multiscript.expected.json").read_text(
            encoding="utf-8"
        )
    )

    result = parser.parse(text)

    assert [section.section_type.value for section in result.sections] == expected["sections"]
    assert [variant.script for variant in result.lyric_variants] == expected["variantScripts"]
    assert [variant.language for variant in result.lyric_variants] == expected["variantLanguages"]
    assert [boundary.label for boundary in result.metadata_boundaries] == expected[
        "metadataBoundaryLabels"
    ]
    assert all(
        "explanatory prose" not in section.text.lower()
        for variant in result.lyric_variants
        for section in variant.sections
    )


def test_fixture_kotlin_parity_tamil_headers() -> None:
    parser = StructureParser()
    fixture_dir = Path(__file__).parent / "fixtures" / "structure_parser"
    text = (fixture_dir / "kotlin_parity_tamil_headers.txt").read_text(encoding="utf-8")
    expected = json.loads(
        (fixture_dir / "kotlin_parity_tamil_headers.expected.json").read_text(
            encoding="utf-8"
        )
    )

    result = parser.parse(text)

    assert [section.section_type.value for section in result.sections] == expected["sections"]
    assert [variant.script for variant in result.lyric_variants] == expected["variantScripts"]
    assert [variant.language for variant in result.lyric_variants] == expected["variantLanguages"]
    assert [boundary.label for boundary in result.metadata_boundaries] == expected[
        "metadataBoundaryLabels"
    ]




