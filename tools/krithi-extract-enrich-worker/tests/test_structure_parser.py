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





    # MKS is demoted into the preceding Charanam (Rule 1: MKS is never top-level)
    assert len(sections) == 2
    assert sections[0].section_type == SectionType.PALLAVI
    assert sections[1].section_type == SectionType.CHARANAM
    assert "[Madhyama Kala Sahitya]" in sections[1].text
    assert "Speedy lyrics here" in sections[1].text


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


def test_fixture_dikshitar_multi_variant() -> None:
    """TRACK-100: Dikshitar blog produces 6 lyric variants (en + 5 Indic scripts)."""
    parser = StructureParser()
    fixture_dir = Path(__file__).parent / "fixtures" / "structure_parser"
    text = (fixture_dir / "dikshitar_multi_variant.txt").read_text(encoding="utf-8")
    expected = json.loads(
        (fixture_dir / "dikshitar_multi_variant.expected.json").read_text(encoding="utf-8")
    )

    result = parser.parse(text)

    assert [s.section_type.value for s in result.sections] == expected["sections"]
    assert [v.script for v in result.lyric_variants] == expected["variantScripts"]
    assert [v.language for v in result.lyric_variants] == expected["variantLanguages"]
    assert [b.label for b in result.metadata_boundaries] == expected["metadataBoundaryLabels"]
    # No "Back" navigation text in any variant
    assert all(
        "back" != section.text.strip().lower()
        for variant in result.lyric_variants
        for section in variant.sections
    )
    # No Word Division blocks creating duplicate variants
    word_div_variants = [v for v in result.lyric_variants if v.script == "word_division"]
    assert len(word_div_variants) == 0


def test_fixture_tyagaraja_multi_variant() -> None:
    """TRACK-100: Tyagaraja blog with P/A/C abbreviations produces multi-script variants."""
    parser = StructureParser()
    fixture_dir = Path(__file__).parent / "fixtures" / "structure_parser"
    text = (fixture_dir / "tyagaraja_multi_variant.txt").read_text(encoding="utf-8")
    expected = json.loads(
        (fixture_dir / "tyagaraja_multi_variant.expected.json").read_text(encoding="utf-8")
    )

    result = parser.parse(text)

    assert [s.section_type.value for s in result.sections] == expected["sections"]
    assert [v.script for v in result.lyric_variants] == expected["variantScripts"]
    assert [v.language for v in result.lyric_variants] == expected["variantLanguages"]
    assert [b.label for b in result.metadata_boundaries] == expected["metadataBoundaryLabels"]


def test_fixture_syama_sastri_numbered() -> None:
    """TRACK-100: Syama Sastri numbered charanams produce clean section text."""
    parser = StructureParser()
    fixture_dir = Path(__file__).parent / "fixtures" / "structure_parser"
    text = (fixture_dir / "syama_sastri_numbered.txt").read_text(encoding="utf-8")
    expected = json.loads(
        (fixture_dir / "syama_sastri_numbered.expected.json").read_text(encoding="utf-8")
    )

    result = parser.parse(text)

    assert [s.section_type.value for s in result.sections] == expected["sections"]
    assert [v.script for v in result.lyric_variants] == expected["variantScripts"]
    assert [v.language for v in result.lyric_variants] == expected["variantLanguages"]
    assert [b.label for b in result.metadata_boundaries] == expected["metadataBoundaryLabels"]


def test_multi_variant_canonical_mapping() -> None:
    """TRACK-100: Each Indic variant's sections map to the English canonical skeleton."""
    parser = StructureParser()
    fixture_dir = Path(__file__).parent / "fixtures" / "structure_parser"
    text = (fixture_dir / "dikshitar_multi_variant.txt").read_text(encoding="utf-8")

    result = parser.parse(text)

    canonical_types = [s.section_type for s in result.sections]
    for variant in result.lyric_variants:
        variant_types = [s.section_type for s in variant.sections]
        # Each variant's section types should be a subset of the canonical skeleton
        for vt in variant_types:
            assert vt in canonical_types, f"Variant {variant.script} has unexpected section type {vt}"


def test_numbered_charanam_text_has_no_leading_number() -> None:
    """TRACK-101: 'caraNam 1\\nlyric text' -> section text is 'lyric text', not '1\\nlyric text'."""
    parser = StructureParser()
    text = """
pallavi
O jagadamba nanu brova

caraNam 1
mANikya mayamaiyunna paadamu

caraNam 2
kunda radanA sundara charaNa
"""
    sections = parser.parse_sections(text)
    charanams = [s for s in sections if s.section_type == SectionType.CHARANAM]
    for c in charanams:
        assert not c.text.strip().startswith("1")
        assert not c.text.strip().startswith("2")


def test_numbered_swara_sahitya_text_clean() -> None:
    """TRACK-101: 'svara sAhitya 2\\nswara text' -> section text is 'swara text'."""
    parser = StructureParser()
    text = """
pallavi
some lyrics

svara sahitya 2
swara text here
"""
    sections = parser.parse_sections(text)
    swara = [s for s in sections if s.section_type == SectionType.SWARA_SAHITYA]
    assert len(swara) == 1
    assert swara[0].text.strip() == "swara text here"


def test_compound_word_division_header_detected() -> None:
    """TRACK-102: 'English - Word Division' is detected as WORD_DIVISION, not ENGLISH."""
    parser = StructureParser()
    header = parser._detect_language_header("English - Word Division")
    assert header is not None
    assert header.label == "WORD_DIVISION"


def test_compound_devanagari_word_division() -> None:
    """TRACK-102: 'Devanagari - Word Division' is WORD_DIVISION metadata."""
    parser = StructureParser()
    header = parser._detect_language_header("Devanagari - Word Division")
    assert header is not None
    assert header.label == "WORD_DIVISION"


def test_standalone_back_is_boilerplate() -> None:
    """TRACK-103: Standalone 'Back' line is filtered as boilerplate."""
    parser = StructureParser()
    assert parser._is_boilerplate("Back") is True
    assert parser._is_boilerplate("back") is True


def test_meaning_of_kriti_is_boilerplate() -> None:
    """TRACK-103: 'Meaning of Kriti-1' navigation link is filtered."""
    parser = StructureParser()
    assert parser._is_boilerplate("Meaning of Kriti-1") is True
    assert parser._is_boilerplate("Meaning of Kriti") is True


def test_per_script_variations_excluded_from_lyrics() -> None:
    """TRACK-104: 'variations' block within a language section doesn't appear in variant text."""
    parser = StructureParser()
    fixture_dir = Path(__file__).parent / "fixtures" / "structure_parser"
    text = (fixture_dir / "dikshitar_multi_variant.txt").read_text(encoding="utf-8")

    result = parser.parse(text)

    for variant in result.lyric_variants:
        for section in variant.sections:
            assert "vatapi - vatabi" not in section.text.lower(), (
                f"Variations content leaked into {variant.script} {section.label}"
            )


def test_indic_single_letter_not_false_positive() -> None:
    """Lyric lines starting with Indic letters must not be misdetected as section headers.

    Regression: 'ப 4 ரதாக் 3 ரஜ:' (Tamil Anupallavi text starting with ப)
    was falsely matched by the single-letter 'ப' → PALLAVI abbreviation pattern,
    causing 23 section issues across 16 krithis.
    """
    parser = StructureParser()
    text = (
        "pallavi\n"
        "ஸ்ரீ ராம சந்த் 3 ரோ ரக்ஷது மாம்\n"
        "ராக்ஷஸாதி 3 ஹரோ ரகு 4 வர:\n"
        "அனுபல்லவி\n"
        "ப 4 ரதாக் 3 ரஜ: கௌஸி 1 க யாக 3 ரக்ஷக: தாடகாந்தக:\n"
        "சரணம்\n"
        "மிதி 2 லா நக 3 ர ப்ரவேஸ 1\n"
    )
    result = parser.parse(text)
    types = [s.section_type.value for s in result.sections]
    assert types == ["PALLAVI", "ANUPALLAVI", "CHARANAM"], (
        f"Expected 3 sections [P, A, C] but got {types} — "
        "single-letter Indic abbreviation pattern likely matched lyric text"
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


def test_fixture_unlabeled_leading_section() -> None:
    """Devanagari variant with Pallavi text directly after language header (no पल्लवि header).

    Reproduces the amba nIlAyatAkshi issue: the parser produces an UNLABELED block
    for the leading Pallavi text, which gets type OTHER and can't match canonical
    PALLAVI. The fix assigns leading OTHER sections to the first unmatched canonical.
    """
    parser = StructureParser()
    fixture_dir = Path(__file__).parent / "fixtures" / "structure_parser"
    text = (fixture_dir / "unlabeled_leading_section.txt").read_text(encoding="utf-8")
    expected = json.loads(
        (fixture_dir / "unlabeled_leading_section.expected.json").read_text(
            encoding="utf-8"
        )
    )

    result = parser.parse(text)

    assert [section.section_type.value for section in result.sections] == expected["sections"]
    assert [variant.script for variant in result.lyric_variants] == expected["variantScripts"]
    assert [variant.language for variant in result.lyric_variants] == expected["variantLanguages"]

    deva_variant = [v for v in result.lyric_variants if v.script == "devanagari"][0]
    assert len(deva_variant.sections) == 3, (
        f"Devanagari variant must have 3 sections (P+A+C), got {len(deva_variant.sections)}. "
        "Leading unlabeled block should map to first canonical section (Pallavi)."
    )
    assert deva_variant.sections[0].section_type == SectionType.PALLAVI
    assert deva_variant.sections[1].section_type == SectionType.ANUPALLAVI
    assert deva_variant.sections[2].section_type == SectionType.CHARANAM


def test_ragamalika_produces_three_sections_not_split() -> None:
    """Ragamalika raga subsections should NOT split into separate sections.

    Each structural section (P/A/C) stays as one section with raga markers
    in the text. The raga subsection metadata is captured separately.
    """
    parser = StructureParser()
    text = """\
Pallavi
1. SrI rAgaM
SrI viSva nAthaM bhajEhaM
caturdaSa bhuvana rUpa rAga mAlikAbharaNa dharaNAntaHkaraNam
2. Arabhi rAgaM
Srita jana saMsAra bhItyApahaM
AdhyAtmikAdi tApa traya manO-bhItyApaham

Anupallavi
3. gauri rAgaM
SrI viSAlAkshI gaurISaM sakala nishkaLa rUpa
4. nATa rAgaM
citra viSva nATaka prakASaM
5. gauLa rAgaM
gOvindAdi vinuta gauLAngaM
6. mOhana rAgaM
guru guha sammOhana-kara lingaM
vilOma - mOhana rAgaM
virinci vishNu rudra mUrti-mayam
vilOma - gauLa rAgaM
vishaya pancaka rahitaM abhayam
vilOma - nATa rAgaM
niratiSaya sukhada nipuNa-taram
vilOma - gauri rAgaM
nigama sAraM ISvaraM amaram
vilOma - Arabhi rAgaM
smara haraM parama SivaM atulam
vilOma - SrI rAgaM
sarasa sadaya hRdaya nilayaM aniSam

Charanam
7. sAma rAgaM
sadASivaM sAma gAna vinutaM
8. lalita rAgaM
sanmAtraM lalita hRdaya viditaM
"""
    result = parser.parse(text)

    # Must produce exactly 3 structural sections
    assert len(result.sections) == 3
    assert result.sections[0].section_type == SectionType.PALLAVI
    assert result.sections[0].label == "Pallavi"
    assert result.sections[1].section_type == SectionType.ANUPALLAVI
    assert result.sections[1].label == "Anupallavi"
    assert result.sections[2].section_type == SectionType.CHARANAM
    assert result.sections[2].label == "Charanam"

    # Raga markers preserved in section text
    assert "1. SrI rAgaM" in result.sections[0].text
    assert "2. Arabhi rAgaM" in result.sections[0].text
    assert "vilOma - mOhana rAgaM" in result.sections[1].text

    # Ragamalika subsections detected as metadata
    assert len(result.ragamalika_subsections) > 0
    forward_subs = [s for s in result.ragamalika_subsections if not s.is_viloma]
    viloma_subs = [s for s in result.ragamalika_subsections if s.is_viloma]
    assert len(forward_subs) == 8  # ragas 1-8
    assert len(viloma_subs) == 6  # viloma in anupallavi

    # Forward ragas have correct parent section types
    assert forward_subs[0].raga_name == "SrI"
    assert forward_subs[0].parent_section_type == SectionType.PALLAVI
    assert forward_subs[2].raga_name == "gauri"
    assert forward_subs[2].parent_section_type == SectionType.ANUPALLAVI
    assert forward_subs[6].raga_name == "sAma"
    assert forward_subs[6].parent_section_type == SectionType.CHARANAM

    # Viloma ragas have correct parent section type
    assert viloma_subs[0].raga_name == "mOhana"
    assert viloma_subs[0].parent_section_type == SectionType.ANUPALLAVI

    # Orders are sequential across all subsections
    assert result.ragamalika_subsections[0].order == 1
    assert result.ragamalika_subsections[-1].order == len(result.ragamalika_subsections)


def test_non_ragamalika_unchanged() -> None:
    """Standard krithis without raga subsection markers still work."""
    parser = StructureParser()
    text = """\
Pallavi
Vatapi Ganapatim Bhaje aham

Anupallavi
bhoota adi samsevita

Charanam
hariharaa putram
"""
    result = parser.parse(text)
    assert len(result.sections) == 3
    assert result.ragamalika_subsections == []


def test_ragamalika_multi_variant_pallavi_not_truncated() -> None:
    """Indic variants with title line before explicit pallavi header must merge both.

    Reproduces a bug where a language header (e.g. Devanagari) contained
    an inline title line followed by an explicit pallavi block. The
    leading-OTHER-promotion created two PALLAVI entries; the type-queue
    matcher picked only the first (title-only), discarding the actual
    raga subsection content for ragas 1-2.
    """
    parser = StructureParser()
    fixture_dir = Path(__file__).parent / "fixtures" / "structure_parser"
    text = (fixture_dir / "ragamalika_multi_variant.txt").read_text(encoding="utf-8")
    expected = json.loads(
        (fixture_dir / "ragamalika_multi_variant.expected.json").read_text(
            encoding="utf-8"
        )
    )

    result = parser.parse(text)

    assert [s.section_type.value for s in result.sections] == expected["sections"]
    assert [v.script for v in result.lyric_variants] == expected["variantScripts"]
    assert [v.language for v in result.lyric_variants] == expected["variantLanguages"]

    for variant in result.lyric_variants:
        assert len(variant.sections) == 3, (
            f"{variant.script} variant must have 3 sections, got {len(variant.sections)}"
        )
        pallavi = variant.sections[0]
        assert pallavi.section_type == SectionType.PALLAVI

        assert "1." in pallavi.text, (
            f"{variant.script} Pallavi missing raga 1 content (got {len(pallavi.text)} chars)"
        )
        assert "2." in pallavi.text, (
            f"{variant.script} Pallavi missing raga 2 content (got {len(pallavi.text)} chars)"
        )


# =========================================================================
# Inline section label abbreviations (thyagaraja-vaibhavam blog format)
# =========================================================================


def test_inline_pac_labels_thyagaraja_format() -> None:
    """Inline P/A/C1/C2/C3 labels from thyagaraja-vaibhavam blog produce correct sections."""
    parser = StructureParser()
    text = (
        "P ataDE dhanyuDurA O manasA\n"
        "A satata yAna suta dhRtamaina sItA\n"
        "pati pAda yugamula satatamu smariyincu (ataDE)\n"
        "C1 venuka tIka tana manasu ranjillaga\n"
        "ghanamaina nAma kIrtana paruDain(a)TTi (ataDE)\n"
        "C2 tumburu vale tana tambura paTTi\n"
        "day(A)mbudhi sannidhAnambuna naTiyincu (ataDE)\n"
        "C3 sAyaku sujanula bAyaka tAnu(n)-\n"
        "upAyamunanu proddhu hAyiga gaDipina (ataDE)\n"
    )
    sections = parser.parse_sections(text)

    assert len(sections) == 5
    assert sections[0].section_type == SectionType.PALLAVI
    assert sections[1].section_type == SectionType.ANUPALLAVI
    assert sections[2].section_type == SectionType.CHARANAM
    assert sections[3].section_type == SectionType.CHARANAM
    assert sections[4].section_type == SectionType.CHARANAM

    assert sections[0].text.startswith("ataDE dhanyuDurA")
    assert sections[1].text.startswith("satata yAna suta")
    assert "pati pAda yugamula" in sections[1].text
    assert sections[2].text.startswith("venuka tIka")
    assert "ghanamaina" in sections[2].text
    assert sections[3].text.startswith("tumburu vale")
    assert sections[4].text.startswith("sAyaku sujanula")


def test_inline_c_numbered_up_to_c12() -> None:
    """C1 through C12 inline labels are detected (Tyagaraja multi-charanam krithis)."""
    parser = StructureParser()
    text = (
        "P endu kaugalinturA\n"
        "A andamaina kunda radana\n"
        "C1 first charanam text\n"
        "C2 second charanam text\n"
        "C10 tenth charanam text\n"
        "C12 twelfth charanam text\n"
    )
    sections = parser.parse_sections(text)
    charanams = [s for s in sections if s.section_type == SectionType.CHARANAM]
    assert len(charanams) == 4
    assert charanams[0].text == "first charanam text"
    assert charanams[3].text == "twelfth charanam text"


def test_inline_a_with_footnote_digit() -> None:
    """'A 1 satyamaina...' — digit after A is a footnote marker, not part of the label."""
    parser = StructureParser()
    text = (
        "P nitya rUpa evari pANDityam(E)mi naDucurA\n"
        "A 1 satyamaina(y)Ajna mIra sAmarthyamu kalavADu\n"
        "C1 kOri kOri paluku tIrulanu jUci\n"
    )
    sections = parser.parse_sections(text)
    assert len(sections) == 3
    assert sections[0].section_type == SectionType.PALLAVI
    assert sections[1].section_type == SectionType.ANUPALLAVI
    # The footnote digit should be stripped (TRACK-101 residual number strip)
    assert sections[1].text.startswith("satyamaina")
    assert sections[2].section_type == SectionType.CHARANAM


def test_inline_labels_no_false_positive_lowercase() -> None:
    """Lowercase 'p' and 'a' at line start must NOT trigger inline P/A patterns.

    Even in a document with C1/C2/... labels, lowercase initial letters are lyric text.
    """
    parser = StructureParser()
    text = (
        "P ataDE dhanyuDurA O manasA\n"
        "A satata yAna suta dhRtamaina sItA\n"
        "pati pAda yugamula satatamu smariyincu\n"
        "akhila lOka nAyaka\n"
        "C1 venuka tIka tana manasu ranjillaga\n"
    )
    sections = parser.parse_sections(text)

    assert sections[0].section_type == SectionType.PALLAVI
    assert sections[1].section_type == SectionType.ANUPALLAVI
    # "pati pAda..." and "akhila..." are continuations of A, not new sections
    assert "pati pAda" in sections[1].text
    assert "akhila" in sections[1].text
    assert sections[2].section_type == SectionType.CHARANAM


def test_inline_labels_no_false_positive_indic_script() -> None:
    """Indic lyric lines must not trigger inline P/A/C patterns.

    Regression guard: Tamil 'ப 4 ரதாக்...' starts with ப (pa) which must not
    match the inline P pattern, and 'அ' must not match inline A.
    """
    parser = StructureParser()
    text = (
        "pallavi\n"
        "ஸ்ரீ ராம சந்த் 3 ரோ ரக்ஷது மாம்\n"
        "அனுபல்லவி\n"
        "ப 4 ரதாக் 3 ரஜ: கௌஸி 1 க யாக 3 ரக்ஷக:\n"
        "சரணம்\n"
        "மிதி 2 லா நக 3 ர ப்ரவேஸ 1\n"
    )
    result = parser.parse(text)
    types = [s.section_type.value for s in result.sections]
    assert types == ["PALLAVI", "ANUPALLAVI", "CHARANAM"], (
        f"Expected 3 sections [P, A, C] but got {types} — "
        "Indic text falsely matched inline label pattern"
    )


def test_inline_labels_no_false_positive_continuation_line() -> None:
    """A lyric continuation line starting with HK long-vowel 'A' must not
    become a new section when full-word headers are already in use.

    'A' in HK = long vowel ā. Lines like 'A jagadamba' are lyric text,
    not section labels, when full-word headers (Pallavi/Charanam) are present.
    """
    parser = StructureParser()
    text = (
        "Pallavi\n"
        "O jagadamba nanu brova\n"
        "A jagadamba sadA brova rAvu\n"
        "Charanam\n"
        "sAra sAra guNa vilAsini\n"
    )
    sections = parser.parse_sections(text)
    # Both lyric lines belong to Pallavi (A is a continuation, not a label)
    assert len(sections) == 2, (
        f"Expected 2 sections but got {len(sections)} — "
        "'A jagadamba' was falsely split into a separate section"
    )
    assert sections[0].section_type == SectionType.PALLAVI
    assert "jagadamba" in sections[0].text


def test_inline_c_standalone_without_digit_unchanged() -> None:
    """Standalone 'C' without a digit still requires end-of-line (existing behavior)."""
    parser = StructureParser()
    text = "P\nsome pallavi text\nC\nsome charanam text\n"
    sections = parser.parse_sections(text)
    assert any(s.section_type == SectionType.PALLAVI for s in sections)
    assert any(s.section_type == SectionType.CHARANAM for s in sections)


def test_inline_pac_single_charanam() -> None:
    """Inline P/A/C (no digit) for krithis with only one charanam."""
    parser = StructureParser()
    text = (
        "P ADa mODi galadE rAmayya mATal\n"
        "A tODu nIDa nIvEyanucunu bhakti\n"
        "kUDina pAdamu paTTina nAtO mATal\n"
        "C caduvulanni telisi SankarAmSuDai\n"
        "sadayuDAShuga sambhavuDu mrokka\n"
    )
    sections = parser.parse_sections(text)
    assert len(sections) == 3
    assert sections[0].section_type == SectionType.PALLAVI
    assert sections[0].text.startswith("ADa mODi")
    assert sections[1].section_type == SectionType.ANUPALLAVI
    assert "kUDina pAdamu" in sections[1].text
    assert sections[2].section_type == SectionType.CHARANAM
    assert sections[2].text.startswith("caduvulanni")


def test_inline_c_space_digit_format() -> None:
    """'C 1 vara giri...' — space between C and digit is tolerated."""
    parser = StructureParser()
    text = (
        "P varada rAja ninu kOri\n"
        "A surulu munulu bhU-surulu\n"
        "C 1 vara giri vaikuNThamaTa\n"
    )
    sections = parser.parse_sections(text)
    assert len(sections) == 3
    assert sections[0].section_type == SectionType.PALLAVI
    assert sections[1].section_type == SectionType.ANUPALLAVI
    assert sections[2].section_type == SectionType.CHARANAM
    assert sections[2].text.startswith("vara giri")

