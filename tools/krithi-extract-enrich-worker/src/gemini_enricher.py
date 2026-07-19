"""Optional Gemini metadata enrichment for canonical extractions.

Supports two modes:
- **Synchronous** (interactive): `enrich()` — one request per call.
- **Batch** (backfill/import): `enrich_batch()` — submits to the Batch API at ~50% cost.

**Quota note (TRACK-128):** when the Batch API is unavailable or a batch job does
not succeed, `enrich_batch` falls back to N sequential *sync* calls. That is a
deliberate availability-over-cost trade, but it spends full sync quota for the
whole batch — potentially twice if the batch job was already partially billed.
Every such fallback logs at WARNING with the item count so the spend is visible.
"""

from __future__ import annotations

import json
import logging
import random
import time
from dataclasses import dataclass
from typing import Any, Protocol

from pydantic import BaseModel, Field

from .diacritic_normalizer import cleanup_raga_tala_name
from .schema import (
    CanonicalExtraction,
    CanonicalMetadataEnrichment,
    CanonicalRaga,
    ExtractionMethod,
)

try:  # pragma: no cover - exercised via the real SDK in tests
    from google.genai import errors as genai_errors
except ImportError:  # pragma: no cover - SDK absent; enrichment is optional
    genai_errors = None  # type: ignore[assignment]

logger = logging.getLogger(__name__)

PROVIDER_LABEL = "google-genai"

BATCH_POLL_INTERVAL_S = 30
BATCH_MAX_POLL_ATTEMPTS = 2880  # 24h / 30s

HTTP_TOO_MANY_REQUESTS = 429
# Applied when the model returns no confidence of its own.
DEFAULT_CONFIDENCE = 0.8


class GeminiModelClient(Protocol):
    def generate_content(self, prompt: str) -> Any:
        """Generate model content for the given prompt."""


class _GeminiSuggestion(BaseModel):
    composer: str | None = None
    raga: str | None = None
    raga_mudra: str | None = Field(None, alias="ragaMudra")
    tala: str | None = None
    deity: str | None = None
    temple: str | None = None
    temple_location: str | None = Field(None, alias="templeLocation")
    confidence: float | None = Field(None, ge=0.0, le=1.0)

    model_config = {"populate_by_name": True}


@dataclass(frozen=True)
class GeminiEnricherConfig:
    enabled: bool
    api_key: str
    model: str


class _GenaiClientWrapper:
    """Wraps the google-genai Client to match the GeminiModelClient protocol."""

    def __init__(self, client: Any, model: str, response_schema: type[BaseModel]) -> None:
        self._client = client
        self._model = model
        self._response_schema = response_schema

    def generate_content(self, prompt: str) -> Any:
        from google.genai.types import GenerateContentConfig

        config = GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=self._response_schema,
        )
        response = self._client.models.generate_content(
            model=self._model,
            contents=prompt,
            config=config,
        )
        return response


class GeminiMetadataEnricher:
    """Enriches extracted metadata using Gemini via the unified google-genai SDK."""

    def __init__(self, config: GeminiEnricherConfig, client: GeminiModelClient | None = None) -> None:
        self._config = config
        self._client = client
        self._raw_client: Any = None
        if self._config.enabled and self._client is None:
            try:
                from google import genai

                self._raw_client = genai.Client(api_key=self._config.api_key)
                self._client = _GenaiClientWrapper(self._raw_client, self._config.model, _GeminiSuggestion)
            except ImportError:
                logger.error("google-genai not installed")
            except Exception as e:
                logger.error(f"Failed to initialize Gemini client: {e}")

    def enrich(
        self,
        extraction: CanonicalExtraction,
        source_text: str,
        *,
        source_format: str,
    ) -> CanonicalMetadataEnrichment | None:
        if not self._config.enabled:
            return None

        if self._client is None:
            return CanonicalMetadataEnrichment(
                provider=PROVIDER_LABEL,
                model=self._config.model,
                applied=False,
                warnings=["gemini_client_unavailable"],
            )

        prompt = self._build_prompt(extraction, source_text)

        suggestion: _GeminiSuggestion | None = None
        max_retries = 5
        for attempt in range(max_retries + 1):
            try:
                response = self._client.generate_content(prompt)
                suggestion = self._parse_response(response)
                break
            except Exception as exc:
                if self._is_rate_limit(exc):
                    if attempt < max_retries:
                        delay = 2 * (2**attempt) + random.uniform(0, 1)
                        logger.warning(
                            "Gemini 429 ResourceExhausted. Retrying in %.2fs (Attempt %d/%d)",
                            delay,
                            attempt + 1,
                            max_retries,
                        )
                        time.sleep(delay)
                        continue
                    else:
                        logger.warning(
                            "Gemini enrichment failed after %d retries: %s",
                            max_retries,
                            exc,
                        )
                        return CanonicalMetadataEnrichment(
                            provider=PROVIDER_LABEL,
                            model=self._config.model,
                            applied=False,
                            warnings=["gemini_error:max_retries_exceeded"],
                        )
                else:
                    logger.warning("Gemini enrichment failed: %s", exc, exc_info=True)
                    return CanonicalMetadataEnrichment(
                        provider=PROVIDER_LABEL,
                        model=self._config.model,
                        applied=False,
                        warnings=[f"gemini_error:{type(exc).__name__}"],
                    )

        if suggestion is None:
            return CanonicalMetadataEnrichment(
                provider=PROVIDER_LABEL,
                model=self._config.model,
                applied=False,
                warnings=["gemini_error:unknown_failure"],
            )

        fields_updated = self._apply_suggestion(extraction, suggestion, source_format)

        return CanonicalMetadataEnrichment(
            provider=PROVIDER_LABEL,
            model=self._config.model,
            # "applied" means the extraction actually changed, not merely that
            # the model answered (TRACK-128).
            applied=bool(fields_updated),
            confidence=suggestion.confidence or DEFAULT_CONFIDENCE,
            fields_updated=fields_updated,
        )

    def _is_rate_limit(self, exc: Exception) -> bool:
        """True only for a typed SDK rate-limit error — never a substring sniff."""
        if genai_errors is not None and isinstance(exc, genai_errors.APIError):
            return bool(exc.code == HTTP_TOO_MANY_REQUESTS)
        return False

    def _is_missing(self, value: str | None) -> bool:
        if value is None:
            return True
        normalized = value.strip().lower()
        return normalized in {"", "unknown", "na", "n/a", "none", "null"}

    def _parse_response(self, response: Any) -> _GeminiSuggestion | None:
        # The request sets response_schema, so the SDK has already validated the
        # payload into _GeminiSuggestion. Prefer that over re-parsing the text.
        parsed = getattr(response, "parsed", None)
        if isinstance(parsed, _GeminiSuggestion):
            return parsed

        logger.warning(
            "Gemini response had no SDK-parsed payload (parsed=%r); falling back to text parsing",
            type(parsed).__name__ if parsed is not None else None,
        )
        text = response.text
        cleaned = text.strip()
        if cleaned.startswith("```json"):
            cleaned = cleaned[7:-3].strip()
        elif cleaned.startswith("```"):
            cleaned = cleaned[3:-3].strip()

        data = json.loads(cleaned)
        return _GeminiSuggestion(**data)

    def _build_prompt(self, extraction: CanonicalExtraction, source_text: str) -> str:
        snippet = source_text[:4000]

        return (
            "You are an expert Musicologist specializing in Carnatic Music, "
            "specifically the kritis of Trinity of Carnatic Music.\n"
            "Analyze the text for:\n"
            "1. Vaggeyakara (Composer) signature (e.g., 'Guruguha').\n"
            "2. Raga Mudra (The raga name often hidden in lyrics like 'jujāvanti' for Dvijavanti).\n"
            "3. Kshetra (Temple) and Deity details.\n\n"
            f"Title: {extraction.title}\n"
            "Source Text:\n"
            f"{snippet}\n\n"
            "Instructions:\n"
            "- Extract the metadata.\n"
            "- If the Raga is not explicitly named, infer it from the Raga Mudra.\n"
            "- If the Temple is not named, infer it from the Deity and Title (common in Dikshitar's Kshethra kritis)."
        )

    # ── Batch Mode (50% cost for bulk/backfill enrichment) ──────────────────

    def enrich_batch(
        self,
        items: list[tuple[CanonicalExtraction, str, str]],
    ) -> list[CanonicalMetadataEnrichment | None]:
        """Batch-enrich multiple extractions via the Gemini Batch API.

        Each item is (extraction, source_text, source_format).
        Returns enrichment results in the same order as input.
        Falls back to sequential sync calls if Batch API is unavailable.
        """
        if not self._config.enabled:
            return [None] * len(items)

        if self._raw_client is None:
            return self._sync_fallback(items, reason="no raw client")

        try:
            return self._run_batch(items)
        except Exception as exc:
            return self._sync_fallback(items, reason=f"batch API failed: {exc}")

    def _sync_fallback(
        self,
        items: list[tuple[CanonicalExtraction, str, str]],
        *,
        reason: str,
    ) -> list[CanonicalMetadataEnrichment | None]:
        """Run the batch sequentially through the sync API.

        This spends full (non-discounted) sync quota for every item, so it is
        logged loudly with the count rather than degrading quietly.
        """
        logger.warning(
            "Batch enrichment falling back to %d sequential SYNC calls (%s) — "
            "this spends full sync quota for the whole batch",
            len(items),
            reason,
        )
        return [self.enrich(ext, text, source_format=fmt) for ext, text, fmt in items]

    def _run_batch(
        self,
        items: list[tuple[CanonicalExtraction, str, str]],
    ) -> list[CanonicalMetadataEnrichment | None]:
        from google.genai.types import GenerateContentConfig

        config = GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=_GeminiSuggestion,
        )

        requests = []
        for extraction, source_text, _ in items:
            prompt = self._build_prompt(extraction, source_text)
            requests.append(prompt)

        batch_job = self._raw_client.batches.create(
            model=self._config.model,
            requests=[{"contents": [{"parts": [{"text": p}]}], "config": config} for p in requests],
        )

        logger.info("Batch job submitted: %s (%d requests)", batch_job.name, len(requests))

        for _ in range(BATCH_MAX_POLL_ATTEMPTS):
            time.sleep(BATCH_POLL_INTERVAL_S)
            batch_job = self._raw_client.batches.get(name=batch_job.name)
            if batch_job.state in ("SUCCEEDED", "FAILED", "CANCELLED"):
                break

        if batch_job.state != "SUCCEEDED":
            logger.error("Batch job %s ended with state: %s", batch_job.name, batch_job.state)
            return self._sync_fallback(items, reason=f"batch job state {batch_job.state}")

        results: list[CanonicalMetadataEnrichment | None] = []
        responses = list(self._raw_client.batches.list_results(name=batch_job.name))

        # A short result set must never silently drop inputs: pair off what we
        # got, then account for the shortfall explicitly (TRACK-128).
        aligned = min(len(responses), len(items))
        if len(responses) != len(items):
            logger.error(
                "Batch job %s returned %d results for %d requests — %d input(s) unaccounted for",
                batch_job.name,
                len(responses),
                len(items),
                len(items) - aligned,
            )

        paired = zip(responses[:aligned], items[:aligned], strict=True)
        for i, (response, (extraction, _, source_format)) in enumerate(paired):
            try:
                suggestion = self._parse_response(response)
                if suggestion is None:
                    results.append(
                        CanonicalMetadataEnrichment(
                            provider=PROVIDER_LABEL,
                            model=self._config.model,
                            applied=False,
                            warnings=["batch_parse_failure"],
                        )
                    )
                    continue

                fields_updated = self._apply_suggestion(extraction, suggestion, source_format)
                results.append(
                    CanonicalMetadataEnrichment(
                        provider=PROVIDER_LABEL,
                        model=self._config.model,
                        applied=bool(fields_updated),
                        confidence=suggestion.confidence or DEFAULT_CONFIDENCE,
                        fields_updated=fields_updated,
                    )
                )
            except Exception as exc:
                logger.warning("Batch result %d parse error: %s", i, exc)
                results.append(
                    CanonicalMetadataEnrichment(
                        provider=PROVIDER_LABEL,
                        model=self._config.model,
                        applied=False,
                        warnings=[f"batch_error:{type(exc).__name__}"],
                    )
                )

        for _ in range(len(items) - aligned):
            results.append(
                CanonicalMetadataEnrichment(
                    provider=PROVIDER_LABEL,
                    model=self._config.model,
                    applied=False,
                    warnings=["batch_count_mismatch"],
                )
            )

        return results

    def _apply_suggestion(
        self,
        extraction: CanonicalExtraction,
        suggestion: _GeminiSuggestion,
        source_format: str,
    ) -> list[str]:
        """Apply suggestion fields to extraction; return list of updated field names."""
        fields_updated: list[str] = []

        if self._is_missing(extraction.composer) and suggestion.composer:
            extraction.composer = suggestion.composer
            fields_updated.append("composer")

        primary_raga = extraction.ragas[0].name if extraction.ragas else "Unknown"
        if self._is_missing(primary_raga) and suggestion.raga:
            extraction.ragas = [CanonicalRaga(name=cleanup_raga_tala_name(suggestion.raga))]
            fields_updated.append("raga")

        if self._is_missing(extraction.tala) and suggestion.tala:
            extraction.tala = cleanup_raga_tala_name(suggestion.tala)
            fields_updated.append("tala")

        if self._is_missing(extraction.deity) and suggestion.deity:
            extraction.deity = suggestion.deity
            fields_updated.append("deity")

        if self._is_missing(extraction.temple) and suggestion.temple:
            extraction.temple = suggestion.temple
            fields_updated.append("temple")

        if self._is_missing(extraction.temple_location) and suggestion.temple_location:
            extraction.temple_location = suggestion.temple_location
            fields_updated.append("templeLocation")

        if fields_updated and source_format == "HTML" and extraction.extraction_method == ExtractionMethod.HTML_JSOUP:
            extraction.extraction_method = ExtractionMethod.HTML_JSOUP_GEMINI

        return fields_updated
