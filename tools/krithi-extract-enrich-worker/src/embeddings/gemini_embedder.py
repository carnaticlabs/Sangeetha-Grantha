"""Gemini Embedding 2 client wrapper with Matryoshka Representation Learning (MRL).

Produces 768-dimensional normalized dense vectors using Google DeepMind's Gemini Embedding 2 model.
Supports asymmetric task types:
- RETRIEVAL_DOCUMENT: for catalog compositions and section passages
- RETRIEVAL_QUERY: for user natural-language queries
"""

from __future__ import annotations

import logging
import os
import random
import time
from pathlib import Path
from typing import Any

from google import genai
from google.genai import types

try:
    from google.genai import errors as genai_errors
except ImportError:
    genai_errors = None  # type: ignore


def _find_api_key_in_config() -> str | None:
    """Searches config/local.env and config/development.env for Gemini API key."""
    cur = Path.cwd()
    candidates_dirs = [cur, cur.parent, cur.parent.parent]
    for directory in candidates_dirs:
        for fname in ["config/local.env", "config/development.env"]:
            env_file = directory / fname
            if env_file.is_file():
                try:
                    for line in env_file.read_text(encoding="utf-8").splitlines():
                        line = line.strip()
                        if not line or line.startswith("#") or "=" not in line:
                            continue
                        k, v = line.split("=", 1)
                        clean_k = k.strip()
                        if clean_k in ("SG_GEMINI_API_KEY", "GEMINI_API_KEY"):
                            clean_v = v.strip().strip("'\"")
                            if clean_v:
                                return clean_v
                except OSError:
                    continue
    return None


logger = logging.getLogger(__name__)

DEFAULT_MODEL = "gemini-embedding-2"
DEFAULT_DIMENSIONS = 768
MAX_RETRIES = 5
INITIAL_BACKOFF_SECONDS = 1.0


class GeminiEmbedder:
    """High-level client for generating Gemini Embedding 2 vectors."""

    def __init__(
        self,
        api_key: str | None = None,
        model: str = DEFAULT_MODEL,
        dimensions: int = DEFAULT_DIMENSIONS,
        vertexai: bool = False,
        project_id: str | None = None,
        location: str = "us-central1",
    ):
        self.model = model
        self.dimensions = dimensions
        self.vertexai = vertexai

        resolved_api_key = api_key or os.environ.get("SG_GEMINI_API_KEY") or os.environ.get("GEMINI_API_KEY")
        if not resolved_api_key:
            try:
                from ..config import ExtractorConfig

                resolved_api_key = ExtractorConfig().gemini_api_key or None
            except Exception:
                pass
        if not resolved_api_key:
            resolved_api_key = _find_api_key_in_config()

        self._resolved_api_key = resolved_api_key
        self._project_id = project_id
        self._location = location
        self._client: genai.Client | None = None

    @property
    def client(self) -> genai.Client:
        if self._client is None:
            http_opts = types.HttpOptions(timeout=30000)  # 30-second socket timeout
            if self.vertexai or (self._project_id and not self._resolved_api_key):
                logger.info(
                    "Initializing GeminiEmbedder using Vertex AI (project=%s, location=%s)",
                    self._project_id,
                    self._location,
                )
                self._client = genai.Client(
                    vertexai=True,
                    project=self._project_id,
                    location=self._location,
                    http_options=http_opts,
                )
            else:
                if not self._resolved_api_key:
                    raise ValueError(
                        "No Gemini API key provided. Please set SG_GEMINI_API_KEY or "
                        "GEMINI_API_KEY environment variable."
                    )
                self._client = genai.Client(
                    api_key=self._resolved_api_key,
                    http_options=http_opts,
                )
        return self._client

    def embed_document(self, text: str, title: str | None = None) -> list[float]:
        """Generates a 768-D embedding for a catalogue document chunk."""
        return self._embed_single_with_retry(text, task_type="RETRIEVAL_DOCUMENT", title=title)

    def embed_query(self, query: str) -> list[float]:
        """Generates a 768-D embedding for an information-seeking user query."""
        return self._embed_single_with_retry(query, task_type="RETRIEVAL_QUERY")

    def _embed_single_with_retry(
        self,
        content: str,
        task_type: str,
        title: str | None = None,
    ) -> list[float]:
        """Executes embed_content with exponential backoff on transient/rate-limit errors."""
        config_kwargs: dict[str, Any] = {
            "task_type": task_type,
            "output_dimensionality": self.dimensions,
        }
        if title:
            config_kwargs["title"] = title

        config = types.EmbedContentConfig(**config_kwargs)

        backoff = INITIAL_BACKOFF_SECONDS
        for attempt in range(1, MAX_RETRIES + 1):
            try:
                response = self.client.models.embed_content(
                    model=self.model,
                    contents=content,
                    config=config,
                )
                if not response.embeddings or not response.embeddings[0].values:
                    raise ValueError(f"Empty embedding returned from model {self.model}")
                return response.embeddings[0].values

            except Exception as exc:
                if not _is_retryable(exc) or attempt == MAX_RETRIES:
                    logger.error("Embedding failed after %d attempts: %s", attempt, exc)
                    raise
                sleep_time = backoff + random.uniform(0.1, 0.5)
                logger.warning(
                    "Transient embedding error on attempt %d/%d (%s). Retrying in %.2f seconds.",
                    attempt,
                    MAX_RETRIES,
                    exc,
                    sleep_time,
                )
                time.sleep(sleep_time)
                backoff *= 2.0
        raise RuntimeError(f"Failed to embed content after {MAX_RETRIES} attempts")


def _is_retryable(exc: BaseException) -> bool:
    if genai_errors and isinstance(exc, genai_errors.APIError):
        return exc.code in {429, 500, 502, 503, 504}
    text = str(exc).lower()
    return any(token in text for token in ("429", "quota", "unavailable", "timeout", "temporarily"))
