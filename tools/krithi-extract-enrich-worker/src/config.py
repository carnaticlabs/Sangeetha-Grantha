"""Environment-based configuration for the Krithi extraction/enrichment worker."""

import socket

from pydantic import Field, computed_field
from pydantic_settings import BaseSettings, SettingsConfigDict


class ExtractorConfig(BaseSettings):
    """Configuration loaded from environment variables."""

    # `frozen=True` preserves the immutability the previous
    # `@dataclass(frozen=True)` provided — config is read-only once loaded.
    # NOTE (TRACK-131 deviation): `env_file` is an *additional* config source
    # beyond the DoD's "same env var names, same defaults". Real environment
    # variables still take precedence over the file; flagged for review.
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        frozen=True,
    )

    # Database
    database_url: str = Field(
        default="postgresql://postgres:postgres@localhost:5432/sangita_grantha",
        validation_alias="DATABASE_URL",
    )

    # Gemini API
    gemini_api_key: str = Field(default="", validation_alias="SG_GEMINI_API_KEY")
    gemini_model: str = Field(default="gemini-2.5-flash", validation_alias="SG_GEMINI_MODEL")
    enable_gemini_enrichment: bool = Field(default=False, validation_alias="SG_ENABLE_GEMINI_ENRICHMENT")

    # Identity candidate discovery
    enable_identity_discovery: bool = Field(default=True, validation_alias="SG_ENABLE_IDENTITY_DISCOVERY")
    identity_candidate_min_score: int = Field(default=60, ge=0, le=100, validation_alias="SG_IDENTITY_MIN_SCORE")
    identity_candidate_max_count: int = Field(default=5, ge=1, validation_alias="SG_IDENTITY_MAX_COUNT")
    identity_cache_ttl_seconds: int = Field(default=900, ge=0, validation_alias="SG_IDENTITY_CACHE_TTL_SECONDS")

    # Worker behaviour
    poll_interval_s: int = Field(default=5, ge=1, validation_alias="EXTRACTION_POLL_INTERVAL_S")

    # Logging
    log_level: str = Field(default="INFO", validation_alias="LOG_LEVEL")

    # Cache
    cache_dir: str = Field(default="/app/cache", validation_alias="EXTRACTION_CACHE_DIR")

    # Identity (for claimed_by tracking)
    hostname: str = Field(default_factory=socket.gethostname)

    # Extractor version tag
    extractor_version_tag: str = Field(default="1.0.0", validation_alias="EXTRACTOR_VERSION")

    @computed_field  # type: ignore[prop-decorator]
    @property
    def extractor_version(self) -> str:
        return f"krithi-extract-enrich-worker:{self.extractor_version_tag}"


def load_config() -> ExtractorConfig:
    """Load configuration from environment variables."""
    return ExtractorConfig()
