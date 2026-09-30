"""Unit tests for the Gemini Embedding 2 context formatter and embedder client."""

from unittest.mock import MagicMock

from src.embeddings.context_formatter import (
    format_composition_overview,
    format_cycle_overview,
    format_kshetra_overview,
    format_raga_header,
    format_section_passage,
    strip_diacritics,
)
from src.embeddings.gemini_embedder import GeminiEmbedder


def test_format_composition_overview_all_fields():
    result = format_composition_overview(
        title="Vātāpi Gaṇapatim",
        composer="Muthuswami Dikshitar",
        raga="Hamsadhvani",
        tala="Adi",
        deity="Ganesha",
        kshetra="Tiruvarur",
        language="Sanskrit",
        script="Devanagari",
        primary_lyrics="vātāpi gaṇapatiṁ bhajēham\nvāraṇa vadanam varaprada śrī",
    )
    assert "[Composition: Vātāpi Gaṇapatim]" in result
    assert "[Composer: Muthuswami Dikshitar]" in result
    assert "[Raga: Hamsadhvani]" in result
    assert "[Tala: Adi]" in result
    assert "[Deity: Ganesha]" in result
    assert "[Kshetra: Tiruvarur]" in result
    assert "[Language: Sanskrit, Script: Devanagari]" in result
    assert "Sahitya:\nvātāpi gaṇapatiṁ bhajēham\nvāraṇa vadanam varaprada śrī" in result


def test_format_composition_overview_minimal():
    result = format_composition_overview(
        title="Endaro Mahanubhavulu",
        composer="Tyagaraja",
    )
    assert "[Composition: Endaro Mahanubhavulu]" in result
    assert "[Composer: Tyagaraja]" in result
    assert "[Raga:" not in result
    assert "Sahitya:" not in result


def test_format_composition_overview_with_tags():
    result = format_composition_overview(
        title="SrI kALahastISa",
        composer="Muthuswami Dikshitar",
        raga="Huseni",
        tags=["Pancha Bhuta Sthala Krithis"],
        primary_lyrics="SrI kALahastISa vAyu liGgam",
    )
    assert "[Composition: SrI kALahastISa]" in result
    assert "[Composer: Muthuswami Dikshitar]" in result
    assert "[Thematic Group / Tags: Pancha Bhuta Sthala Krithis]" in result
    assert "Sahitya:\nSrI kALahastISa vAyu liGgam" in result


def test_format_section_passage():
    result = format_section_passage(
        title="Vātāpi Gaṇapatim",
        composer="Muthuswami Dikshitar",
        section_type="ANUPALLAVI",
        section_text="karāmbuja pāśa bījāpūra\nkalpakā kalitāvadāta",
        raga="Hamsadhvani",
        tala="Adi",
        deity="Ganesha",
        kshetra="Tiruvarur",
        language="sa",
        script="devanagari",
    )
    assert "[Composition: Vātāpi Gaṇapatim]" in result
    assert "[Composer: Muthuswami Dikshitar]" in result
    assert "[Section: ANUPALLAVI]" in result
    assert "[Raga: Hamsadhvani]" in result
    assert "[Deity: Ganesha]" in result
    assert "Text:\nkarāmbuja pāśa bījāpūra\nkalpakā kalitāvadāta" in result


def test_strip_diacritics():
    assert strip_diacritics("Chārukesi") == "Charukesi"
    assert strip_diacritics("Śaṅkarābharaṇam") == "Sankarabharanam"
    assert strip_diacritics("Kalyāṇi") == "Kalyani"
    assert strip_diacritics("Māyāmāḷavagowḷa") == "Mayamalavagowla"
    assert strip_diacritics("Hamsadhvani") == "Hamsadhvani"
    assert strip_diacritics("") == ""


def test_format_raga_header_with_and_without_diacritics():
    # Diacritic raga aliased
    assert format_raga_header("Chārukesi") == "[Raga: Chārukesi / Charukesi]"
    assert format_raga_header("Śaṅkarābharaṇam") == "[Raga: Śaṅkarābharaṇam / Sankarabharanam]"
    # Pure ASCII raga not duplicated
    assert format_raga_header("Hamsadhvani") == "[Raga: Hamsadhvani]"
    assert format_raga_header("Kharaharapriya") == "[Raga: Kharaharapriya]"
    # Multi-raga list with diacritics
    assert format_raga_header("Kāmbōji, Kalyāṇi") == "[Raga: Kāmbōji, Kalyāṇi / Kamboji, Kalyani]"


def test_format_composition_overview_with_diacritic_raga():
    result = format_composition_overview(
        title="Aada Modi Galadae",
        composer="Tyagaraja",
        raga="Chārukesi",
        tala="Adi",
    )
    assert "[Composition: Aada Modi Galadae]" in result
    assert "[Composer: Tyagaraja]" in result
    assert "[Raga: Chārukesi / Charukesi]" in result
    assert "[Tala: Adi]" in result


def test_format_section_passage_with_diacritic_raga():
    result = format_section_passage(
        title="Aada Modi Galadae",
        composer="Tyagaraja",
        section_type="PALLAVI",
        section_text="Aada modi galadae ramayya maa maata",
        raga="Chārukesi",
    )
    assert "[Composition: Aada Modi Galadae]" in result
    assert "[Section: PALLAVI]" in result
    assert "[Raga: Chārukesi / Charukesi]" in result


def test_gemini_embedder_document_call():
    embedder = GeminiEmbedder(api_key="test-fake-key", dimensions=768)

    mock_client = MagicMock()
    mock_resp = MagicMock()
    # Mock return 768-D vector
    mock_resp.embeddings = [MagicMock(values=[0.1] * 768)]
    mock_client.models.embed_content.return_value = mock_resp

    embedder._client = mock_client

    vec = embedder.embed_document("Sample krithi text", title="Sample Title")
    assert len(vec) == 768

    mock_client.models.embed_content.assert_called_once()
    _, kwargs = mock_client.models.embed_content.call_args
    assert kwargs["model"] == "gemini-embedding-2"
    assert kwargs["contents"] == "Sample krithi text"
    config = kwargs["config"]
    assert config.output_dimensionality == 768
    assert config.task_type == "RETRIEVAL_DOCUMENT"
    assert config.title == "Sample Title"


def test_gemini_embedder_query_call():
    embedder = GeminiEmbedder(api_key="test-fake-key", dimensions=768)

    mock_client = MagicMock()
    mock_resp = MagicMock()
    mock_resp.embeddings = [MagicMock(values=[0.05] * 768)]
    mock_client.models.embed_content.return_value = mock_resp

    embedder._client = mock_client

    vec = embedder.embed_query("Dikshitar songs on Shiva in Chidambaram")
    assert len(vec) == 768

    mock_client.models.embed_content.assert_called_once()
    _, kwargs = mock_client.models.embed_content.call_args
    assert kwargs["model"] == "gemini-embedding-2"
    assert kwargs["contents"] == "Dikshitar songs on Shiva in Chidambaram"
    config = kwargs["config"]
    assert config.output_dimensionality == 768
    assert config.task_type == "RETRIEVAL_QUERY"


def test_kamalamba_6th_vs_7th_enclosure_discrimination():
    # 6th Enclosure: Kamalambikayastava, Punnagavarali, SHASHTHI, Sarvarakshakara, Nigarbha
    doc_6th = format_composition_overview(
        title="Kamalāmbikāyāstava",
        composer="Muthuswami Dikshitar",
        raga="Punnāgavarāḷi",
        tala="Rupaka",
        deity="Kamalamba",
        kshetra="Tiruvarur",
        musical_form="KRITHI",
        vibhakti_case="SHASHTHI",
        cycle_info={
            "slug": "kamalamba-navavarnam",
            "sequence_order": 6,
            "role": "CORE",
            "axis_value": "Sarvarakshakara",
            "attrs": {"avarana": 6, "yogini": "Nigarbha"},
        },
        primary_lyrics="kamalāmbikāyāstava bhakto'ham",
    )
    assert "[Cycle: Kamalamba Navavarnam; Avarana: 6; Chakra: Sarvarakshakara; Yogini: Nigarbha]" in doc_6th
    assert "[Grammar: Shashthi]" in doc_6th
    assert "[Raga: Punnāgavarāḷi / Punnagavarali]" in doc_6th

    # 7th Enclosure: Sri Kamalambikayam, Sahana, SAPTAMI, Sarvarogahara, Rahasya
    doc_7th = format_composition_overview(
        title="Śrī Kamalāmbikāyāṁ",
        composer="Muthuswami Dikshitar",
        raga="Sahāna",
        tala="Triputa",
        deity="Kamalamba",
        kshetra="Tiruvarur",
        musical_form="KRITHI",
        vibhakti_case="SAPTAMI",
        cycle_info={
            "slug": "kamalamba-navavarnam",
            "sequence_order": 7,
            "role": "CORE",
            "axis_value": "Sarvarogahara",
            "attrs": {"avarana": 7, "yogini": "Rahasya"},
        },
        primary_lyrics="śrī kamalāmbikāyāṁ bhaktiṁ karomi",
    )
    assert "[Cycle: Kamalamba Navavarnam; Avarana: 7; Chakra: Sarvarogahara; Yogini: Rahasya]" in doc_7th
    assert "[Grammar: Saptami]" in doc_7th

    # 5th Enclosure: Sri Kamalambayah, Bhairavi, PANCHAMI
    doc_5th = format_composition_overview(
        title="Śrī Kamalāmbāyāḥ",
        composer="Muthuswami Dikshitar",
        raga="Bhairavi",
        tala="Misra Jhampa",
        deity="Kamalamba",
        kshetra="Tiruvarur",
        musical_form="KRITHI",
        vibhakti_case="PANCHAMI",
        cycle_info={
            "slug": "kamalamba-navavarnam",
            "sequence_order": 5,
            "role": "CORE",
            "axis_value": "Sarvarthasadhaka",
            "attrs": {"avarana": 5, "yogini": "Kulotta"},
        },
        primary_lyrics="śrī kamalāmbāyāḥ paraṁ nahi re re citta",
    )
    assert "[Grammar: Panchami]" in doc_5th
    assert "Sarvarakshakara" not in doc_5th
    assert "Sarvarogahara" not in doc_5th


def test_nottusvara_tune_clause():
    doc = format_composition_overview(
        title="Santataṁ Pāhi Mām",
        composer="Muthuswami Dikshitar",
        raga="Sankarabharanam",
        tala="Rupaka",
        deity="Parvati",
        musical_form="NOTTUSVARA",
        cycle_info={
            "slug": "nottusvara-sahitya",
            "role": "CORE",
            "axis_value": "God Save the King",
        },
        primary_lyrics="santataṁ pāhi māṁ saṅgīta śyāmale",
    )
    assert "[Cycle: Nottusvara Sahitya; Tune: God Save the King]" in doc
    assert "[Musical Form: NOTTUSVARA]" in doc


def test_strict_60_word_header_budget():
    doc = format_composition_overview(
        title="A Very Long Detailed Title That Extends Quite A Bit Further Than Usual",
        composer="Muthuswami Dikshitar",
        raga="Sankarabharanam",
        tala="Chaturasra Jati Rupaka Tala",
        deity="Lord Sundaresvara and Goddess Meenakshi",
        kshetra="Madurai Meenakshi Sundareswarar Temple Complex",
        mandalam="PANDYA",
        bhuta="PRITHVI",
        deity_posture="STHANAKA",
        vibhakti_case="SARVA_VIBHAKTI",
        vibhakti_stem="Sundaresvara",
        cycle_info={
            "slug": "kamalamba-navavarnam",
            "sequence_order": 6,
            "role": "CORE",
            "axis_value": "Sarvarakshakara",
            "attrs": {"avarana": 6, "yogini": "Nigarbha"},
        },
        occasion_note="Composed during the grand Chithirai festival procession",
        structural_features=["Samashti Charanam", "Swara Sahitya", "Manipravala"],
        yati_pattern="GOPUCCHA",
        tags=["Pancha Bhuta Sthala", "Navagraha", "Vibhakti Series", "Temple Kshetra"],
        primary_lyrics="Sample text for testing word limits.",
    )
    # Extract header (everything before [Language:... or Sahitya:)
    header_part = doc.split("Sahitya:")[0].split("[Language:")[0].strip()
    word_count = len(header_part.split())
    assert word_count <= 60, f"Header exceeded 60 words: {word_count} words ({header_part})"


def test_macro_cycle_overview_formatter():
    members = [
        {"title": "Dhyana Kriti", "raga": "Todi", "role": "DHYANA", "axis_value": None, "vibhakti_case": None},
        {"title": "Kamalambike", "raga": "Anandabhairavi", "role": "CORE", "axis_value": "Trailokyamohana", "vibhakti_case": "PRATHAMA"},
    ]
    overview = format_cycle_overview(
        cycle_name="Kamalamba Navavarnam",
        composer="Muthuswami Dikshitar",
        description="The 11 Sri Vidya esoteric compositions at Tiruvarur.",
        members=members,
    )
    assert "[Cycle: Kamalamba Navavarnam]" in overview
    assert "[Composer: Muthuswami Dikshitar]" in overview
    assert "Trailokyamohana" in overview
    assert "Case: Prathama" in overview


def test_macro_kshetra_overview_gap_formatter():
    overview = format_kshetra_overview(
        temple_name="Bhagavati Amman Temple",
        city="Kanyakumari",
        state="Tamil Nadu",
        mandalam="PANDYA",
        bhuta=None,
        deity_posture="STHANAKA",
        notes="Documented gap shrine: historically visited by Dikshitar; no extant kritis preserved.",
        kritis=[],
        is_gap=True,
    )
    assert "[Kshetra: Bhagavati Amman Temple (Kanyakumari)]" in overview
    assert "[Mandalam: Pandya]" in overview
    assert "[Posture: Sthanaka]" in overview
    assert "No Muthuswami Dikshitar compositions are attested for this temple" in overview

