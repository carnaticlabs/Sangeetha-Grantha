"""Environment-based configuration for the PDF extraction service."""

from __future__ import annotations

import os
import socket
from dataclasses import dataclass, field


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
    extractor_version: str = f"pdf-extractor:{os.environ.get('EXTRACTOR_VERSION', '1.0.0')}"


def load_config() -> ExtractorConfig:
    """Load configuration from environment variables."""
    return ExtractorConfig()
