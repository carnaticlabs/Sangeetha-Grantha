import time
from dataclasses import dataclass

import httpx
import respx

from src.gemini_enricher import GeminiEnricherConfig, GeminiMetadataEnricher
from src.schema import CanonicalExtraction, CanonicalRaga, ExtractionMethod


@dataclass
class _FakeResponse:
    text: str


class _FakeClient:
    def __init__(self, payload: str) -> None:
        self.payload = payload

    def generate_content(self, _prompt: str):
        # TRACK-128 removed the accepted-and-ignored generation_config parameter;
        # the wrapper builds GenerateContentConfig itself.
        return _FakeResponse(text=self.payload)


def _build_extraction() -> CanonicalExtraction:
    return CanonicalExtraction(
        title="Akhilandesvari",
        composer="Unknown",
        ragas=[CanonicalRaga(name="Unknown")],
        tala="Unknown",
        source_url="https://example.com",
        source_name="fixture",
        source_tier=5,
        extraction_method=ExtractionMethod.HTML_JSOUP,
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
    client = _FakeClient('{"composer":"Dikshitar","raga":"Shankarabharanam","tala":"Adi","confidence":0.9}')
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


# ── TRACK-128 characterisation tests ────────────────────────────────────────
# Written BEFORE the hardening changes to pin observable behaviour. Gemini is
# stubbed at the HTTP layer (respx intercepts the SDK's httpx transport) so the
# real google-genai error types flow through, exactly as in the W3 contract.

GEMINI_HOST = "generativelanguage.googleapis.com"


def _live_enricher() -> GeminiMetadataEnricher:
    """An enricher backed by the real SDK client (stubbed at HTTP by respx)."""
    return GeminiMetadataEnricher(GeminiEnricherConfig(enabled=True, api_key="test-key", model="gemini-2.5-flash"))


@respx.mock(assert_all_called=False)
def test_rate_limit_retries_then_degrades(respx_mock, monkeypatch) -> None:
    respx_mock.route(host=GEMINI_HOST).mock(return_value=httpx.Response(429, text="rate limited"))
    slept: list[float] = []
    monkeypatch.setattr(time, "sleep", lambda d: slept.append(d))

    extraction = _build_extraction()
    before = extraction.to_json_dict()
    result = _live_enricher().enrich(extraction, "source-text", source_format="HTML")

    assert result is not None
    assert result.applied is False
    assert result.warnings == ["gemini_error:max_retries_exceeded"]
    assert len(slept) == 5, f"expected 5 backoff sleeps, got {len(slept)}"
    assert extraction.to_json_dict() == before


@respx.mock(assert_all_called=False)
def test_server_error_degrades_without_retrying(respx_mock, monkeypatch) -> None:
    respx_mock.route(host=GEMINI_HOST).mock(return_value=httpx.Response(500, text="boom"))
    slept: list[float] = []
    monkeypatch.setattr(time, "sleep", lambda d: slept.append(d))

    result = _live_enricher().enrich(_build_extraction(), "source-text", source_format="HTML")

    assert result is not None
    assert result.applied is False
    assert result.warnings == ["gemini_error:ServerError"]
    assert slept == [], "non-rate-limit errors must not retry"


@respx.mock(assert_all_called=False)
def test_malformed_json_degrades_without_touching_metadata(respx_mock) -> None:
    respx_mock.route(host=GEMINI_HOST).mock(
        return_value=httpx.Response(
            200,
            json={"candidates": [{"content": {"parts": [{"text": "NOT {{{ JSON"}], "role": "model"}}]},
        )
    )

    extraction = _build_extraction()
    before = extraction.to_json_dict()
    result = _live_enricher().enrich(extraction, "source-text", source_format="HTML")

    assert result is not None
    assert result.applied is False
    assert result.warnings and all(w.startswith("gemini_error:") for w in result.warnings)
    assert extraction.to_json_dict() == before


class _FakeBatchJob:
    def __init__(self, name: str, state: str) -> None:
        self.name = name
        self.state = state


class _FakeBatches:
    """Batch endpoint returning FEWER results than requests — the mismatch case."""

    def __init__(self, n_results: int, payload: str) -> None:
        self.n_results = n_results
        self.payload = payload

    def create(self, model=None, requests=None):  # noqa: ANN001, ARG002
        return _FakeBatchJob("batches/fake", "SUCCEEDED")

    def get(self, name=None):  # noqa: ANN001, ARG002
        return _FakeBatchJob("batches/fake", "SUCCEEDED")

    def list_results(self, name=None):  # noqa: ANN001, ARG002
        return [_FakeResponse(text=self.payload) for _ in range(self.n_results)]


class _FakeRawClient:
    def __init__(self, batches: _FakeBatches) -> None:
        self.batches = batches


def test_batch_count_mismatch_is_surfaced_not_silently_truncated(monkeypatch) -> None:
    """Three inputs, two results back: every input must still get a result."""
    monkeypatch.setattr(time, "sleep", lambda _d: None)
    enricher = GeminiMetadataEnricher(
        GeminiEnricherConfig(enabled=True, api_key="test-key", model="gemini-2.5-flash"),
        client=_FakeClient('{"composer":"Dikshitar","confidence":0.9}'),
    )
    enricher._raw_client = _FakeRawClient(
        _FakeBatches(n_results=2, payload='{"composer":"Dikshitar","confidence":0.9}')
    )
    items = [
        (_build_extraction(), "t1", "HTML"),
        (_build_extraction(), "t2", "HTML"),
        (_build_extraction(), "t3", "HTML"),
    ]

    results = enricher.enrich_batch(items)

    assert len(results) == len(items), "results must align 1:1 with inputs, never truncate"
    tail = results[-1]
    assert tail is not None
    assert tail.applied is False
    assert any("mismatch" in w for w in tail.warnings), f"expected a mismatch warning, got {tail.warnings}"


def test_applied_is_false_when_nothing_changed() -> None:
    """A model answer that updates no field is not an 'applied' enrichment."""
    client = _FakeClient('{"composer":"Someone Else","raga":"Kalyani","confidence":0.9}')
    enricher = GeminiMetadataEnricher(
        GeminiEnricherConfig(enabled=True, api_key="test-key", model="gemini-2.5-flash"),
        client=client,
    )
    # Already fully populated, so nothing is "missing" for the model to fill.
    extraction = CanonicalExtraction(
        title="Akhilandesvari",
        composer="Muttuswami Dikshitar",
        ragas=[CanonicalRaga(name="Dwijavanti")],
        tala="Adi",
        source_url="https://example.com",
        source_name="fixture",
        source_tier=5,
        extraction_method=ExtractionMethod.HTML_JSOUP,
    )

    result = enricher.enrich(extraction, "source-text", source_format="HTML")

    assert result is not None
    assert result.fields_updated == []
    assert result.applied is False
    assert extraction.composer == "Muttuswami Dikshitar"
    assert extraction.extraction_method == ExtractionMethod.HTML_JSOUP


@respx.mock(assert_all_called=False)
def test_sdk_parsed_payload_is_used_for_valid_response(respx_mock) -> None:
    """A well-formed response is consumed via response.parsed, not re-parsed text."""
    respx_mock.route(host=GEMINI_HOST).mock(
        return_value=httpx.Response(
            200,
            json={
                "candidates": [
                    {
                        "content": {
                            "parts": [{"text": '{"composer":"Muttuswami Dikshitar","confidence":0.91}'}],
                            "role": "model",
                        }
                    }
                ]
            },
        )
    )

    extraction = _build_extraction()
    result = _live_enricher().enrich(extraction, "source-text", source_format="HTML")

    assert result is not None
    assert result.applied is True
    assert "composer" in result.fields_updated
    assert extraction.composer == "Muttuswami Dikshitar"
    assert result.confidence == 0.91
