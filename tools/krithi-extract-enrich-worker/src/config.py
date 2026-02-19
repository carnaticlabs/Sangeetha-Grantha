"""Environment-based configuration for the Krithi extraction/enrichment worker."""

from __future__ import annotations

import os
import socket
from dataclasses import dataclass, field


def _env_bool(name: str, default: bool) -> bool:
    value = os.environ.get(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class ExtractorConfig:
    """Configuration loaded from environment variables."""

    # Database
    database_url: str = field(
        default_factory=lambda: os.environ.get(
            "DATABASE_URL",
            "postgresql://postgres:postgres@localhost:5432/sangita_grantha",
        )
    )

    # Gemini API
    gemini_api_key: str = field(
        default_factory=lambda: os.environ.get("SG_GEMINI_API_KEY", "")
    )
    gemini_model: str = field(
        default_factory=lambda: os.environ.get("SG_GEMINI_MODEL", "gemini-2.0-flash")
    )
    enable_gemini_enrichment: bool = field(
        default_factory=lambda: _env_bool("SG_ENABLE_GEMINI_ENRICHMENT", False)
    )

    # Identity candidate discovery
    enable_identity_discovery: bool = field(
        default_factory=lambda: _env_bool("SG_ENABLE_IDENTITY_DISCOVERY", True)
    )
    identity_candidate_min_score: int = field(
        default_factory=lambda: int(os.environ.get("SG_IDENTITY_MIN_SCORE", "60"))
    )
    identity_candidate_max_count: int = field(
        default_factory=lambda: int(os.environ.get("SG_IDENTITY_MAX_COUNT", "5"))
    )
    identity_cache_ttl_seconds: int = field(
        default_factory=lambda: int(os.environ.get("SG_IDENTITY_CACHE_TTL_SECONDS", "900"))
    )

    # Worker behaviour
    poll_interval_s: int = field(
        default_factory=lambda: int(os.environ.get("EXTRACTION_POLL_INTERVAL_S", "5"))
    )
    batch_size: int = field(
        default_factory=lambda: int(os.environ.get("EXTRACTION_BATCH_SIZE", "10"))
    )
    max_concurrent: int = field(
        default_factory=lambda: int(os.environ.get("EXTRACTION_MAX_CONCURRENT", "3"))
    )

    # Logging
    log_level: str = field(
        default_factory=lambda: os.environ.get("LOG_LEVEL", "INFO")
    )

    # Cache
    cache_dir: str = field(
        default_factory=lambda: os.environ.get("EXTRACTION_CACHE_DIR", "/app/cache")
    )

    # Identity (for claimed_by tracking)
    hostname: str = field(default_factory=socket.gethostname)

    # Extractor version tag
    extractor_version: str = (
        f"krithi-extract-enrich-worker:{os.environ.get('EXTRACTOR_VERSION', '1.0.0')}"
    )


def load_config() -> ExtractorConfig:
    """Load configuration from environment variables."""
    return ExtractorConfig()
