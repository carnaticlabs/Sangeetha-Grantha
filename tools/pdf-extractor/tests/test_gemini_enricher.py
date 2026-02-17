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
        GeminiEnricherConfig(enabled=True, api_key="test-key", model="gemini-2.0-flash"),
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
        GeminiEnricherConfig(enabled=False, api_key="", model="gemini-2.0-flash"),
        client=None,
    )
    extraction = _build_extraction()

    result = enricher.enrich(extraction, "source-text", source_format="HTML")

    assert result is None
    assert extraction.composer == "Unknown"
