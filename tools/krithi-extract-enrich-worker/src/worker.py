"""Database-queue polling worker for the PDF extraction service.

Production entry point: polls the `extraction_queue` table for PENDING tasks,
claims them using SELECT ... FOR UPDATE SKIP LOCKED, processes them through
the appropriate extraction strategy, and writes results back.

Format-specific parsing lives in :mod:`src.extraction_strategies`; this module
is solely the coordinator — polling, task claiming, dispatch, enrichment
finalization, and error handling.

Usage:
    python -m src.worker

Environment variables:
    DATABASE_URL: PostgreSQL connection string
    SG_GEMINI_API_KEY: Gemini API key for LLM refinement
    SG_ENABLE_GEMINI_ENRICHMENT: Enable optional Gemini metadata fill for missing fields
    SG_ENABLE_IDENTITY_DISCOVERY: Enable RapidFuzz identity candidates for composer/raga
    EXTRACTION_POLL_INTERVAL_S: Seconds between poll attempts (default: 5)
    LOG_LEVEL: Logging level (default: INFO)
"""

import logging
import signal
import sys
import time
import traceback
from types import FrameType

from .config import ExtractorConfig, load_config
from .db import ExtractionQueueDB, ExtractionTask
from .extraction_strategies import (
    DocxExtractionStrategy,
    ExtractionStrategy,
    HtmlExtractionStrategy,
    ImageExtractionStrategy,
    PdfExtractionStrategy,
)
from .extractor import PdfExtractor
from .gemini_enricher import GeminiEnricherConfig, GeminiMetadataEnricher
from .html_extractor import HtmlTextExtractor
from .identity_candidates import IdentityCandidateDiscovery, ReferenceEntity
from .metadata_parser import MetadataParser
from .normalizer import normalize_for_matching
from .ocr_fallback import OcrFallback
from .page_segmenter import PageSegmenter
from .schema import CanonicalExtraction, CanonicalIdentityCandidates
from .structure_parser import StructureParser
from .transliterator import Transliterator

logger = logging.getLogger(__name__)


class ExtractionWorker:
    """Queue coordinator: polls extraction_queue and dispatches tasks to strategies."""

    def __init__(self, config: ExtractorConfig) -> None:
        self.config = config
        self.db = ExtractionQueueDB(config)
        self.gemini_enricher = GeminiMetadataEnricher(
            GeminiEnricherConfig(
                enabled=config.enable_gemini_enrichment,
                api_key=config.gemini_api_key,
                model=config.gemini_model,
            )
        )
        self._identity_discovery: IdentityCandidateDiscovery | None = None
        self._identity_catalog_loaded_at_monotonic = 0.0
        self._shutdown = False

        structure_parser = StructureParser()
        metadata_parser = MetadataParser()
        transliterator = Transliterator()
        self.pdf_strategy = PdfExtractionStrategy(
            config,
            self._finalize_extraction,
            pdf_extractor=PdfExtractor(),
            page_segmenter=PageSegmenter(),
            ocr_fallback=OcrFallback(),
            structure_parser=structure_parser,
            metadata_parser=metadata_parser,
            transliterator=transliterator,
        )
        self.html_strategy = HtmlExtractionStrategy(
            config,
            self._finalize_extraction,
            html_extractor=HtmlTextExtractor(),
            structure_parser=structure_parser,
            metadata_parser=metadata_parser,
            transliterator=transliterator,
        )
        self.strategies: dict[str, ExtractionStrategy] = {
            strategy.source_format: strategy
            for strategy in (
                self.pdf_strategy,
                self.html_strategy,
                DocxExtractionStrategy(config, self._finalize_extraction),
                ImageExtractionStrategy(config, self._finalize_extraction),
            )
        }

    def run(self) -> None:
        """Main polling loop. Runs until SIGTERM/SIGINT."""
        logger.info(
            "Worker starting",
            extra={
                "hostname": self.config.hostname,
                "poll_interval_s": self.config.poll_interval_s,
                "version": self.config.extractor_version,
            },
        )

        self.db.connect()

        # Register signal handlers for graceful shutdown
        signal.signal(signal.SIGTERM, self._signal_handler)
        signal.signal(signal.SIGINT, self._signal_handler)

        while not self._shutdown:
            try:
                task = self.db.claim_pending_task()
                if task:
                    self._process_task(task)
                else:
                    time.sleep(self.config.poll_interval_s)
            except KeyboardInterrupt:
                logger.info("Keyboard interrupt received, shutting down")
                break
            except Exception:
                logger.exception("Unexpected error in worker loop")
                # Rollback to clear any aborted transaction state
                try:
                    self.db.conn.rollback()
                except Exception:
                    pass
                time.sleep(self.config.poll_interval_s)

        self.close()
        logger.info("Worker stopped")

    def close(self) -> None:
        """Release worker-owned resources: strategy HTTP pools, then the DB."""
        for strategy in self.strategies.values():
            strategy.close()
        self.db.close()

    def _signal_handler(self, signum: int, frame: FrameType | None) -> None:
        """Handle SIGTERM/SIGINT for graceful shutdown."""
        logger.info(f"Received signal {signum}, initiating graceful shutdown")
        self._shutdown = True

    def _process_task(self, task: ExtractionTask) -> None:
        """Process a single extraction task."""
        start_time = time.monotonic()
        logger.info(
            "Processing task",
            extra={
                "task_id": str(task.id),
                "source_url": task.source_url,
                "source_format": task.source_format,
                "attempt": task.attempts,
            },
        )

        try:
            strategy = self.strategies.get(task.source_format)
            if strategy is None:
                raise ValueError(f"Unsupported source format: {task.source_format}")

            results = strategy.extract(task)

            duration_ms = int((time.monotonic() - start_time) * 1000)
            result_dicts = [r.to_json_dict() for r in results]
            extraction_method = results[0].extraction_method if results else strategy.default_extraction_method

            # Calculate average confidence
            avg_confidence = 0.7  # Default confidence for pattern-matched extraction

            self.db.mark_done(
                task_id=task.id,
                result_payload=result_dicts,
                extraction_method=extraction_method.value,
                confidence=avg_confidence,
                duration_ms=duration_ms,
            )

            logger.info(
                "Task completed successfully",
                extra={
                    "task_id": str(task.id),
                    "result_count": len(results),
                    "duration_ms": duration_ms,
                },
            )

        except Exception as e:
            duration_ms = int((time.monotonic() - start_time) * 1000)
            logger.error(
                "Task %s failed after %dms: %s",
                task.id,
                duration_ms,
                e,
                exc_info=True,
            )
            error_detail = {
                "message": str(e),
                "type": type(e).__name__,
                "traceback": traceback.format_exc(),
                "duration_ms": duration_ms,
                "attempt": task.attempts,
            }
            self.db.mark_failed(task.id, error_detail)

    def _finalize_extraction(
        self,
        extraction: CanonicalExtraction,
        source_text: str,
        source_format: str,
    ) -> CanonicalExtraction:
        """Apply optional Phase 3 identity and enrichment signals."""
        identity_candidates = self._discover_identity_candidates(extraction)
        if identity_candidates is not None:
            extraction.identity_candidates = identity_candidates

        enrichment = self.gemini_enricher.enrich(
            extraction,
            source_text,
            source_format=source_format,
        )
        if enrichment is not None:
            extraction.metadata_enrichment = enrichment

        primary_raga = extraction.ragas[0].name if extraction.ragas else ""
        extraction.title_normalized = normalize_for_matching(extraction.title, "title")
        extraction.composer_normalized = normalize_for_matching(extraction.composer, "composer")
        extraction.raga_normalized = normalize_for_matching(primary_raga, "raga")
        extraction.tala_normalized = normalize_for_matching(extraction.tala, "tala")

        return extraction

    def _discover_identity_candidates(
        self,
        extraction: CanonicalExtraction,
    ) -> CanonicalIdentityCandidates | None:
        if not self.config.enable_identity_discovery:
            return None

        discovery = self._get_identity_discovery()
        if discovery is None:
            return None

        composer = extraction.composer if extraction.composer else None
        raga_names = [raga.name for raga in extraction.ragas if raga.name]
        return discovery.discover(composer=composer, ragas=raga_names)

    def _get_identity_discovery(self) -> IdentityCandidateDiscovery | None:
        now = time.monotonic()
        ttl_seconds = max(30, self.config.identity_cache_ttl_seconds)
        should_refresh = (
            self._identity_discovery is None or (now - self._identity_catalog_loaded_at_monotonic) >= ttl_seconds
        )
        if not should_refresh:
            return self._identity_discovery

        if not self.db.is_connected:
            return self._identity_discovery

        try:
            composer_rows = self.db.list_composer_reference_rows()
            raga_rows = self.db.list_raga_reference_rows()
        except Exception:
            logger.warning(
                "Failed loading identity reference rows; keeping previous cache",
                exc_info=True,
            )
            return self._identity_discovery

        composers = [
            ReferenceEntity(
                entity_id=row["entity_id"],
                name=row["name"],
                aliases=tuple(row.get("aliases") or []),
            )
            for row in composer_rows
            if row.get("entity_id") and row.get("name")
        ]
        ragas = [
            ReferenceEntity(
                entity_id=row["entity_id"],
                name=row["name"],
                aliases=(),
            )
            for row in raga_rows
            if row.get("entity_id") and row.get("name")
        ]
        self._identity_discovery = IdentityCandidateDiscovery(
            composers=composers,
            ragas=ragas,
            min_score=self.config.identity_candidate_min_score,
            max_candidates=self.config.identity_candidate_max_count,
        )
        self._identity_catalog_loaded_at_monotonic = now
        return self._identity_discovery


def health_check() -> None:
    """Health check function called by Docker HEALTHCHECK."""
    config = load_config()
    db = ExtractionQueueDB(config)
    try:
        db.connect()
        if not db.health_check():
            sys.exit(1)
    except Exception:
        sys.exit(1)
    finally:
        db.close()


def main() -> None:
    """Entry point for the worker process."""
    config = load_config()

    # Configure logging
    logging.basicConfig(
        level=getattr(logging, config.log_level.upper(), logging.INFO),
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
        stream=sys.stdout,
    )

    worker = ExtractionWorker(config)
    worker.run()


if __name__ == "__main__":
    main()
