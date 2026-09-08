"""Unit tests for the Gemini Embedding 2 context formatter and embedder client."""

from unittest.mock import MagicMock

from src.embeddings.context_formatter import (
    format_composition_overview,
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
