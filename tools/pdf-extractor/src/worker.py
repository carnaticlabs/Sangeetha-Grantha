"""Database-queue polling worker for the PDF extraction service.

Production entry point: polls the `extraction_queue` table for PENDING tasks,
claims them using SELECT ... FOR UPDATE SKIP LOCKED, processes them through
the appropriate extractor, and writes results back.

Usage:
    python -m src.worker

Environment variables:
    DATABASE_URL: PostgreSQL connection string
    SG_GEMINI_API_KEY: Gemini API key for LLM refinement
    EXTRACTION_POLL_INTERVAL_S: Seconds between poll attempts (default: 5)
    EXTRACTION_BATCH_SIZE: Max tasks to process per cycle (default: 10)
    EXTRACTION_MAX_CONCURRENT: Max concurrent extractions (default: 3)
    LOG_LEVEL: Logging level (default: INFO)
"""

from __future__ import annotations

import json
import logging
import signal
import sys
import time
import traceback
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from uuid import UUID

import httpx

from .config import ExtractorConfig, load_config
from .db import ExtractionQueueDB, ExtractionTask
from .extractor import PdfExtractor
from .metadata_parser import MetadataParser
from .ocr_fallback import OcrFallback
from .page_segmenter import PageSegmenter
from .schema import (
    CanonicalExtraction,
    CanonicalLyricSection,
    CanonicalLyricVariant,
    CanonicalRaga,
    CanonicalSection,
    ExtractionMethod,
    MusicalForm,
)
from .structure_parser import StructureParser
from .transliterator import Transliterator

logger = logging.getLogger(__name__)


class ExtractionWorker:
    """Main worker that polls extraction_queue and processes tasks."""

    def __init__(self, config: ExtractorConfig) -> None:
        self.config = config
        self.db = ExtractionQueueDB(config)
        self.pdf_extractor = PdfExtractor()
        self.page_segmenter = PageSegmenter()
        self.structure_parser = StructureParser()
        self.metadata_parser = MetadataParser()
        self.ocr_fallback = OcrFallback()
        self.transliterator = Transliterator()
        self._shutdown = False

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
                time.sleep(self.config.poll_interval_s)

        self.db.close()
        logger.info("Worker stopped")

    def _signal_handler(self, signum: int, frame: Any) -> None:
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
            if task.source_format == "PDF":
                results = self._extract_pdf(task)
            elif task.source_format == "DOCX":
                results = self._extract_docx(task)
            elif task.source_format == "IMAGE":
                results = self._extract_image(task)
            else:
                raise ValueError(f"Unsupported source format: {task.source_format}")

            duration_ms = int((time.monotonic() - start_time) * 1000)
            result_dicts = [r.to_json_dict() for r in results]

            # Calculate average confidence
            avg_confidence = 0.7  # Default confidence for pattern-matched extraction

            self.db.mark_done(
                task_id=task.id,
                result_payload=result_dicts,
                extraction_method=ExtractionMethod.PDF_PYMUPDF.value
                if task.source_format == "PDF"
                else ExtractionMethod.DOCX_PYTHON.value,
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
            error_detail = {
                "message": str(e),
                "type": type(e).__name__,
                "traceback": traceback.format_exc(),
                "duration_ms": duration_ms,
                "attempt": task.attempts,
            }
            self.db.mark_failed(task.id, error_detail)

    def _download_source(self, url: str) -> Path:
        """Download a source document to the cache directory."""
        cache_dir = Path(self.config.cache_dir)
        cache_dir.mkdir(parents=True, exist_ok=True)

        # Use URL hash as filename
        import hashlib

        url_hash = hashlib.sha256(url.encode()).hexdigest()[:16]
        extension = Path(url).suffix or ".pdf"
        cached_path = cache_dir / f"{url_hash}{extension}"

        if cached_path.exists():
            logger.debug(f"Using cached artifact: {cached_path}")
            return cached_path

        logger.info(f"Downloading source: {url}")
        with httpx.Client(timeout=120.0, follow_redirects=True) as client:
            response = client.get(url)
            response.raise_for_status()
            cached_path.write_bytes(response.content)

        return cached_path

    def _extract_pdf(self, task: ExtractionTask) -> list[CanonicalExtraction]:
        """Extract Krithis from a PDF document."""
        # Download PDF
        pdf_path = self._download_source(task.source_url)

        # Parse page range from task
        page_range = None
        if task.page_range:
            parts = task.page_range.split("-")
            if len(parts) == 2:
                page_range = (int(parts[0]) - 1, int(parts[1]) - 1)  # Convert to 0-based
            elif len(parts) == 1:
                page_num = int(parts[0]) - 1
                page_range = (page_num, page_num)

        # Check if text is extractable (vs. scanned)
        use_ocr = not self.pdf_extractor.is_text_extractable(str(pdf_path))

        if use_ocr:
            logger.info("PDF requires OCR", extra={"task_id": str(task.id)})
            return self._extract_pdf_ocr(task, pdf_path, page_range)

        # Extract text with PyMuPDF
        document = self.pdf_extractor.extract_document(str(pdf_path), page_range)

        # Segment into individual Krithis
        segments = self.page_segmenter.segment(document)

        # Process each segment
        results: list[CanonicalExtraction] = []
        composer_hint = task.request_payload.get("composerHint", "")

        for segment in segments:
            # Parse metadata from header
            metadata = self.metadata_parser.parse(
                segment.body_text[:500],  # First 500 chars likely contain header
                title_hint=segment.title_text,
            )

            # Parse section structure
            detected_sections = self.structure_parser.parse_sections(segment.body_text)
            canonical_sections = self.structure_parser.to_canonical_sections(detected_sections)
            lyric_sections = self.structure_parser.to_canonical_lyric_sections(detected_sections)

            # Detect script
            script = self.transliterator.detect_script(segment.body_text) or "devanagari"
            language = "sa" if script == "devanagari" else "en"

            # Build canonical extraction
            extraction = CanonicalExtraction(
                title=metadata.title,
                alternateTitle=metadata.alternate_title,
                composer=metadata.composer or composer_hint or "Unknown",
                musicalForm=MusicalForm.KRITHI,
                ragas=[CanonicalRaga(name=metadata.raga or "Unknown")],
                tala=metadata.tala or "Unknown",
                sections=canonical_sections,
                lyricVariants=[
                    CanonicalLyricVariant(
                        language=language,
                        script=script,
                        sections=lyric_sections,
                    )
                ]
                if lyric_sections
                else [],
                deity=metadata.deity,
                temple=metadata.temple,
                templeLocation=metadata.temple_location,
                sourceUrl=task.source_url,
                sourceName=task.source_name or "unknown",
                sourceTier=task.source_tier or 5,
                extractionMethod=ExtractionMethod.PDF_PYMUPDF,
                extractionTimestamp=datetime.now(timezone.utc).isoformat(),
                pageRange=segment.page_range_str,
                checksum=document.checksum,
            )
            results.append(extraction)

        return results

    def _extract_pdf_ocr(
        self,
        task: ExtractionTask,
        pdf_path: Path,
        page_range: tuple[int, int] | None,
    ) -> list[CanonicalExtraction]:
        """Extract from scanned PDF using OCR fallback."""
        page_texts = self.ocr_fallback.extract_document_text(str(pdf_path), page_range)

        # For OCR, treat the entire text as a single extraction
        # (segmentation is less reliable on OCR'd text)
        full_text = "\n".join(page_texts.values())

        if not full_text.strip():
            logger.warning("OCR produced no text", extra={"task_id": str(task.id)})
            return []

        metadata = self.metadata_parser.parse(full_text[:500])
        detected_sections = self.structure_parser.parse_sections(full_text)
        canonical_sections = self.structure_parser.to_canonical_sections(detected_sections)
        lyric_sections = self.structure_parser.to_canonical_lyric_sections(detected_sections)

        script = self.transliterator.detect_script(full_text) or "devanagari"
        language = "sa" if script == "devanagari" else "en"

        start_page = min(page_texts.keys()) if page_texts else 0
        end_page = max(page_texts.keys()) if page_texts else 0
        page_range_str = f"{start_page + 1}-{end_page + 1}" if start_page != end_page else str(start_page + 1)

        return [
            CanonicalExtraction(
                title=metadata.title,
                composer=metadata.composer or task.request_payload.get("composerHint", "Unknown"),
                musicalForm=MusicalForm.KRITHI,
                ragas=[CanonicalRaga(name=metadata.raga or "Unknown")],
                tala=metadata.tala or "Unknown",
                sections=canonical_sections,
                lyricVariants=[
                    CanonicalLyricVariant(language=language, script=script, sections=lyric_sections)
                ]
                if lyric_sections
                else [],
                deity=metadata.deity,
                temple=metadata.temple,
                sourceUrl=task.source_url,
                sourceName=task.source_name or "unknown",
                sourceTier=task.source_tier or 5,
                extractionMethod=ExtractionMethod.PDF_OCR,
                extractionTimestamp=datetime.now(timezone.utc).isoformat(),
                pageRange=page_range_str,
            )
        ]

    def _extract_docx(self, task: ExtractionTask) -> list[CanonicalExtraction]:
        """Extract from DOCX documents. Placeholder for future implementation."""
        raise NotImplementedError("DOCX extraction not yet implemented")

    def _extract_image(self, task: ExtractionTask) -> list[CanonicalExtraction]:
        """Extract from image files using OCR. Placeholder for future implementation."""
        raise NotImplementedError("Image extraction not yet implemented")


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
