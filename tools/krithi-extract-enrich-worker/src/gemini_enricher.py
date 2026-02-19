"""Optional Gemini metadata enrichment for canonical extractions."""

from __future__ import annotations

import json
import logging
import random
import re
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

logger = logging.getLogger(__name__)


class GeminiModelClient(Protocol):
    def generate_content(self, prompt: str, generation_config: dict[str, Any] | None = None) -> Any:
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


class GeminiMetadataEnricher:
    """Enriches extracted metadata using Gemini 1.5 Pro/Flash."""

    def __init__(self, config: GeminiEnricherConfig, client: GeminiModelClient | None = None) -> None:
        self._config = config
        self._client = client
        if self._config.enabled and self._client is None:
            try:
                import google.generativeai as genai

                genai.configure(api_key=self._config.api_key)
                self._client = genai.GenerativeModel(self._config.model)
            except ImportError:
                logger.error("google-generativeai not installed")
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
                provider="google-generativeai",
                model=self._config.model,
                applied=False,
                warnings=["gemini_client_unavailable"],
            )

        prompt = self._build_prompt(extraction, source_text)
        
        # Schema definition for Gemini
        schema = {
            "type": "object",
            "properties": {
                "composer": {"type": "string"},
                "raga": {"type": "string"},
                "ragaMudra": {"type": "string", "description": "The specific phrase in lyrics naming the raga"},
                "tala": {"type": "string"},
                "deity": {"type": "string"},
                "temple": {"type": "string"},
                "templeLocation": {"type": "string"},
                "confidence": {"type": "number"}
            }
        }

        suggestion: _GeminiSuggestion | None = None
        max_retries = 5
        for attempt in range(max_retries + 1):
            try:
                response = self._client.generate_content(
                    prompt,
                    generation_config={
                        "response_mime_type": "application/json",
                        "response_schema": schema,
                    },
                )
                suggestion = self._parse_response(response)
                break  # Success, exit retry loop
            except Exception as exc:
                error_msg = str(exc)
                is_rate_limit = "429" in error_msg or "ResourceExhausted" in error_msg

                if is_rate_limit:
                    if attempt < max_retries:
                        delay = 2 * (2 ** attempt) + random.uniform(0, 1)
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
                            provider="google-generativeai",
                            model=self._config.model,
                            applied=False,
                            warnings=["gemini_error:max_retries_exceeded"],
                        )
                else:
                    logger.warning("Gemini enrichment failed: %s", exc, exc_info=True)
                    return CanonicalMetadataEnrichment(
                        provider="google-generativeai",
                        model=self._config.model,
                        applied=False,
                        warnings=[f"gemini_error:{type(exc).__name__}"],
                    )

        if suggestion is None:
            return CanonicalMetadataEnrichment(
                provider="google-generativeai",
                model=self._config.model,
                applied=False,
                warnings=["gemini_error:unknown_failure"],
            )

        fields_updated: list[str] = []
        # Apply logic: Only update if original is missing/Unknown.
        
        if self._is_missing(extraction.composer) and suggestion.composer:
            extraction.composer = suggestion.composer
            fields_updated.append("composer")

        primary_raga = extraction.ragas[0].name if extraction.ragas else "Unknown"
        if self._is_missing(primary_raga) and suggestion.raga:
            # Use suggested raga. If ragaMudra is present, it's supporting evidence.
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

        return CanonicalMetadataEnrichment(
            provider="google-generativeai",
            model=self._config.model,
            applied=True,
            confidence=suggestion.confidence or 0.8,
            fieldsUpdated=fields_updated,
        )

    def _is_missing(self, value: str | None) -> bool:
        """Check if a metadata field is missing or generic."""
        if not value:
            return True
        v = value.strip().lower()
        return v in ("", "unknown", "none", "null")

    def _parse_response(self, response: Any) -> _GeminiSuggestion | None:
        try:
            text = response.text
            # With response_schema, text should be valid JSON.
            # But safe to strip markdown just in case.
            cleaned = text.strip()
            if cleaned.startswith("```json"):
                cleaned = cleaned[7:-3].strip()
            elif cleaned.startswith("```"):
                cleaned = cleaned[3:-3].strip()
            
            data = json.loads(cleaned)
            return _GeminiSuggestion(**data)
        except Exception:
            # logger.warning("Failed to parse Gemini JSON: %s", response.text)
            # Log inside enrich instead
            raise

    def _build_prompt(self, extraction: CanonicalExtraction, source_text: str) -> str:
        snippet = self._extract_metadata_snippet(source_text)

        return (
            "You are an expert Musicologist specializing in Carnatic Music, specifically the kritis of Trinity of Carnatic Music.\n"
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

    def _extract_metadata_snippet(self, source_text: str) -> str:
        """Extract metadata-relevant context (headers + lyrics) for inference."""
        # Simple truncation to preserve context (Sahitya) for inference.
        return source_text[:4000]

    def _extract_text(self, response: Any) -> str:
        direct = getattr(response, "text", None)
        if isinstance(direct, str) and direct.strip():
            return direct

        candidates = getattr(response, "candidates", None) or []
        for candidate in candidates:
            content = getattr(candidate, "content", None)
            parts = getattr(content, "parts", None) or []
            for part in parts:
                text = getattr(part, "text", None)
                if isinstance(text, str) and text.strip():
                    return text
        raise ValueError("Gemini response did not contain text")

    def _is_missing(self, value: str | None) -> bool:
        if value is None:
            return True
        normalized = value.strip().lower()
        return normalized in {"", "unknown", "na", "n/a", "none", "null"}
