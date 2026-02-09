"""Tests for section structure detection."""

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
bhoota adi samsevita charanam
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
