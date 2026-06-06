from dataclasses import dataclass

from src.gemini_enricher import GeminiEnricherConfig, GeminiMetadataEnricher
from src.schema import CanonicalExtraction, CanonicalRaga, ExtractionMethod


@dataclass
class _FakeResponse:
    text: str


class _FakeClient:
    def __init__(self, payload: str) -> None:
        self.payload = payload

    def generate_content(self, _prompt: str, generation_config=None):  # noqa: ANN001
        assert generation_config is not None
        return _FakeResponse(text=self.payload)


def _build_extraction() -> CanonicalExtraction:
    return CanonicalExtraction(
        title="Akhilandesvari",
        composer="Unknown",
        ragas=[CanonicalRaga(name="Unknown")],
        tala="Unknown",
        sourceUrl="https://example.com",
        sourceName="fixture",
        sourceTier=5,
        extractionMethod=ExtractionMethod.HTML_JSOUP,
    )


def test_enricher_updates_missing_fields_and_marks_applied() -> None:
    client = _FakeClient(
        '{"composer":"Muttuswami Dikshitar","raga":"Dwijavanti","tala":"Adi",'
        '"deity":"Akhilandesvari","temple":"Tiruvanaikaval","confidence":0.93}'
    )
    enricher = GeminiMetadataEnricher(
        GeminiEnricherConfig(enabled=True, api_key="test-key", model="gemini-2.5-flash"),
        client=client,
    )
    extraction = _build_extraction()

    result = enricher.enrich(extraction, "source-text", source_format="HTML")

    assert result is not None
    assert result.applied is True
    assert "composer" in result.fields_updated
    assert extraction.composer == "Muttuswami Dikshitar"
    assert extraction.ragas[0].name == "Dwijavanti"
    assert extraction.extraction_method == ExtractionMethod.HTML_JSOUP_GEMINI


def test_enricher_is_noop_when_disabled() -> None:
    enricher = GeminiMetadataEnricher(
        GeminiEnricherConfig(enabled=False, api_key="", model="gemini-2.5-flash"),
        client=None,
    )
    extraction = _build_extraction()

    result = enricher.enrich(extraction, "source-text", source_format="HTML")

    assert result is None
    assert extraction.composer == "Unknown"


def test_batch_returns_none_list_when_disabled() -> None:
    enricher = GeminiMetadataEnricher(
        GeminiEnricherConfig(enabled=False, api_key="", model="gemini-2.5-flash"),
        client=None,
    )
    items = [(_build_extraction(), "text", "HTML"), (_build_extraction(), "text", "HTML")]

    results = enricher.enrich_batch(items)

    assert results == [None, None]


def test_batch_falls_back_to_sync_without_raw_client() -> None:
    client = _FakeClient(
        '{"composer":"Dikshitar","raga":"Shankarabharanam","tala":"Adi","confidence":0.9}'
    )
    enricher = GeminiMetadataEnricher(
        GeminiEnricherConfig(enabled=True, api_key="test-key", model="gemini-2.5-flash"),
        client=client,
    )
    ext1 = _build_extraction()
    ext2 = _build_extraction()

    results = enricher.enrich_batch([(ext1, "text1", "HTML"), (ext2, "text2", "HTML")])

    assert len(results) == 2
    assert all(r is not None and r.applied for r in results)
    assert ext1.composer == "Dikshitar"
    assert ext2.composer == "Dikshitar"


def test_enricher_provider_label_is_google_genai() -> None:
    client = _FakeClient('{"composer":"Dikshitar","confidence":0.9}')
    enricher = GeminiMetadataEnricher(
        GeminiEnricherConfig(enabled=True, api_key="test-key", model="gemini-2.5-flash"),
        client=client,
    )
    result = enricher.enrich(_build_extraction(), "text", source_format="HTML")

    assert result is not None
    assert result.provider == "google-genai"
