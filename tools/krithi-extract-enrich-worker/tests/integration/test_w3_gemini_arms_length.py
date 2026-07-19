"""W3 (TRACK-113): the AI layer kept at arm's length — stubbed at HTTP.

Two sides of the contract, both pinned against real behavior:

- Malformed Gemini output does NOT fail the job. Enrichment degrades to
  `applied=false` with a `gemini_error:*` warning and the canonical metadata
  is left untouched — no partial writes into the extraction. (The original
  W3 wording assumed enrichment failures fail the job; the implemented
  contract is stricter arm's-length: the AI phase can never sink a task.)
- A failing SOURCE fetch (the non-optional HTTP dependency) DOES fail the
  job: status FAILED with structured diagnostics and an empty result side.

Gemini is stubbed at the HTTP layer (respx intercepts the google-genai SDK's
httpx transport), not by mocking the enricher — the SDK stays in the loop.
"""

from __future__ import annotations

import json

import httpx
import respx

from src.config import ExtractorConfig
from src.gemini_enricher import GeminiEnricherConfig, GeminiMetadataEnricher
from src.schema import CanonicalExtraction
from src.worker import ExtractionWorker

from .conftest import GOLDEN_FIXTURE, fetch_task_row, insert_pending_task

GEMINI_HOST = "generativelanguage.googleapis.com"


def _golden_extraction() -> CanonicalExtraction:
    return CanonicalExtraction.model_validate(json.loads(GOLDEN_FIXTURE.read_text(encoding="utf-8")))


def _enricher() -> GeminiMetadataEnricher:
    return GeminiMetadataEnricher(GeminiEnricherConfig(enabled=True, api_key="test-key", model="gemini-2.5-flash"))


@respx.mock(assert_all_called=False)
def test_malformed_gemini_output_degrades_without_touching_metadata(respx_mock) -> None:
    respx_mock.route(host=GEMINI_HOST).mock(
        return_value=httpx.Response(
            200,
            json={"candidates": [{"content": {"parts": [{"text": "THIS IS {{{ NOT JSON"}], "role": "model"}}]},
        )
    )

    extraction = _golden_extraction()
    before = extraction.to_json_dict()

    enrichment = _enricher().enrich(extraction, "source text", source_format="HTML")

    assert enrichment is not None
    assert enrichment.applied is False
    assert enrichment.warnings, "diagnostic warning expected"
    assert all(w.startswith("gemini_error:") for w in enrichment.warnings)
    # Arm's length: the canonical metadata was not partially mutated
    assert extraction.to_json_dict() == before


@respx.mock(assert_all_called=False)
def test_gemini_http_error_degrades_with_diagnostics(respx_mock) -> None:
    respx_mock.route(host=GEMINI_HOST).mock(return_value=httpx.Response(500, text="boom"))

    extraction = _golden_extraction()
    enrichment = _enricher().enrich(extraction, "source text", source_format="HTML")

    assert enrichment is not None
    assert enrichment.applied is False
    assert enrichment.warnings


def test_failing_source_fetch_marks_job_failed_with_diagnostics(
    queue_db, database_url: str, tmp_path, monkeypatch
) -> None:
    """The non-AI HTTP dependency failing must fail the job — no partial writes."""
    monkeypatch.setenv("DATABASE_URL", database_url)
    monkeypatch.setenv("EXTRACTION_CACHE_DIR", str(tmp_path / "cache"))
    monkeypatch.setenv("SG_ENABLE_GEMINI_ENRICHMENT", "false")
    monkeypatch.setenv("SG_ENABLE_IDENTITY_DISCOVERY", "false")

    task_id = insert_pending_task(
        queue_db, source_url="https://unreachable.example.org/krithi/500", source_format="HTML"
    )

    worker = ExtractionWorker(ExtractorConfig())
    try:
        task = worker.db.claim_pending_task()
        assert task is not None and task.id == task_id

        with respx.mock(assert_all_called=False) as respx_mock:
            respx_mock.route(host="unreachable.example.org").mock(
                return_value=httpx.Response(500, text="upstream exploded")
            )
            worker._process_task(task)

        row = fetch_task_row(worker.db, task_id)
        assert row["status"] == "FAILED"
        assert row["error_detail"]["type"] == "HTTPStatusError"
        assert "500" in row["error_detail"]["message"]
        assert row["error_detail"]["traceback"]
        assert row["error_detail"]["attempt"] == 1
        assert row["last_error_at"] is not None
        # No partial writes on failure
        assert row["result_payload"] is None
        assert row["result_count"] is None
        assert row["extraction_method"] is None
    finally:
        worker.db.close()


def test_recovered_source_fetch_completes_the_same_task(queue_db, database_url: str, tmp_path, monkeypatch) -> None:
    """Sanity companion: with a healthy source the same pipeline lands on DONE."""
    monkeypatch.setenv("DATABASE_URL", database_url)
    monkeypatch.setenv("EXTRACTION_CACHE_DIR", str(tmp_path / "cache"))
    monkeypatch.setenv("SG_ENABLE_GEMINI_ENRICHMENT", "false")
    monkeypatch.setenv("SG_ENABLE_IDENTITY_DISCOVERY", "false")

    html = """
    <html><head><title>vAtApi gaNapatim</title></head><body>
    <h1>vAtApi gaNapatim</h1>
    <p>rAgaM: hamsadhvani tALaM: Adi Composer: Muthuswami Dikshitar</p>
    <b>pallavi</b>
    <p>vAtApi gaNapatiM bhajE'haM vAraNAsyaM varapradaM shrI</p>
    <b>anupallavi</b>
    <p>bhUtAdi saMsEvita caraNaM bhUta bhautika prapanca bharaNaM</p>
    <b>caraNam</b>
    <p>purA kumbha sambhava munivara prapUjitaM trikONa madhyagataM</p>
    </body></html>
    """

    task_id = insert_pending_task(queue_db, source_url="https://healthy.example.org/krithi/ok", source_format="HTML")

    worker = ExtractionWorker(ExtractorConfig())
    try:
        task = worker.db.claim_pending_task()
        assert task is not None

        with respx.mock(assert_all_called=False) as respx_mock:
            respx_mock.route(host="healthy.example.org").mock(return_value=httpx.Response(200, text=html))
            worker._process_task(task)

        row = fetch_task_row(worker.db, task_id)
        assert row["status"] == "DONE"
        assert row["result_count"] >= 1
        assert row["error_detail"] is None
        first = row["result_payload"][0]
        assert first["title"]
        assert first["sourceUrl"] == "https://healthy.example.org/krithi/ok"
    finally:
        worker.db.close()
