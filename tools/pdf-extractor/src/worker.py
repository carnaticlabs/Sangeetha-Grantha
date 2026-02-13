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
from hashlib import sha256
from pathlib import Path
from typing import Any
from uuid import UUID

import httpx

from .config import ExtractorConfig, load_config
from .db import ExtractionQueueDB, ExtractionTask
from .diacritic_normalizer import cleanup_raga_tala_name, normalize_garbled_diacritics
from .extractor import PdfExtractor
from .extractor import DocumentContent
from .html_extractor import HtmlTextExtractor
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
        self.html_extractor = HtmlTextExtractor()
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
            elif task.source_format == "HTML":
                results = self._extract_html(task)
            elif task.source_format == "DOCX":
                results = self._extract_docx(task)
            elif task.source_format == "IMAGE":
                results = self._extract_image(task)
            else:
                raise ValueError(f"Unsupported source format: {task.source_format}")

            duration_ms = int((time.monotonic() - start_time) * 1000)
            result_dicts = [r.to_json_dict() for r in results]
            extraction_method = (
                results[0].extraction_method
                if results
                else self._extraction_method_for_format(task.source_format)
            )

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

    def _extraction_method_for_format(self, source_format: str) -> ExtractionMethod:
        if source_format == "PDF":
            return ExtractionMethod.PDF_PYMUPDF
        if source_format == "HTML":
            return ExtractionMethod.HTML_JSOUP
        if source_format == "DOCX":
            return ExtractionMethod.DOCX_PYTHON
        if source_format == "IMAGE":
            return ExtractionMethod.PDF_OCR
        raise ValueError(f"Unsupported source format for extraction method mapping: {source_format}")

    def _default_extension_for_format(self, source_format: str) -> str:
        if source_format == "PDF":
            return ".pdf"
        if source_format == "HTML":
            return ".html"
        if source_format == "DOCX":
            return ".docx"
        if source_format == "IMAGE":
            return ".img"
        return ".bin"

    def _truncate_utf8(self, value: str, max_bytes: int = 1800) -> str:
        """Trim text to a safe UTF-8 byte size for indexed DB columns."""
        encoded = value.encode("utf-8")
        if len(encoded) <= max_bytes:
            return value
        return encoded[:max_bytes].decode("utf-8", errors="ignore").rstrip()

    def _truncate_lyric_variants(
        self,
        variants: list[CanonicalLyricVariant],
    ) -> list[CanonicalLyricVariant]:
        trimmed: list[CanonicalLyricVariant] = []
        for variant in variants:
            sections = [
                CanonicalLyricSection(
                    sectionOrder=section.section_order,
                    text=self._truncate_utf8(section.text),
                )
                for section in variant.sections
                if section.text.strip()
            ]
            if not sections:
                continue
            trimmed.append(
                CanonicalLyricVariant(
                    language=variant.language,
                    script=variant.script,
                    sections=sections,
                )
            )
        return trimmed

    def _download_source(self, url: str, source_format: str = "PDF") -> Path:
        """Download a source document to the cache directory."""
        cache_dir = Path(self.config.cache_dir)
        cache_dir.mkdir(parents=True, exist_ok=True)

        # Use URL hash as filename
        url_hash = sha256(url.encode()).hexdigest()[:16]
        extension = Path(url).suffix or self._default_extension_for_format(source_format)
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
        pdf_path = self._download_source(task.source_url, source_format="PDF")

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
        if self._should_force_ocr_for_garbled_devanagari(document):
            logger.info(
                "Forcing OCR due to garbled Devanagari text",
                extra={"task_id": str(task.id)},
            )
            return self._extract_pdf_ocr(task, pdf_path, page_range)

        # Segment into individual Krithis
        segments = self.page_segmenter.segment(document)

        # Process each segment
        results: list[CanonicalExtraction] = []
        composer_hint = task.request_payload.get("composerHint", "")

        for segment in segments:
            # Step 1: Normalize garbled diacritics in the body text
            normalized_body = normalize_garbled_diacritics(segment.body_text)

            # Parse metadata from header (uses normalized text internally if needed)
            metadata = self.metadata_parser.parse(
                normalized_body[:500],  # First 500 chars likely contain header
                title_hint=segment.title_text,
            )

            # Parse lyric structure and metadata boundaries from normalized text.
            parse_result = self.structure_parser.parse(normalized_body)
            canonical_sections = self.structure_parser.to_canonical_sections(parse_result.sections)
            lyric_variants = self.structure_parser.to_canonical_lyric_variants(
                parse_result.lyric_variants
            )
            metadata_boundaries = self.structure_parser.to_canonical_metadata_boundaries(
                parse_result.metadata_boundaries
            )

            if not lyric_variants and parse_result.sections:
                fallback_script = self.transliterator.detect_script(normalized_body) or "devanagari"
                fallback_language = "sa" if fallback_script == "devanagari" else "en"
                lyric_variants = [
                    CanonicalLyricVariant(
                        language=fallback_language,
                        script=fallback_script,
                        sections=self.structure_parser.to_canonical_lyric_sections(
                            parse_result.sections
                        ),
                    )
                ]
            lyric_variants = self._truncate_lyric_variants(lyric_variants)
            primary_script = (
                lyric_variants[0].script
                if lyric_variants
                else (self.transliterator.detect_script(normalized_body) or "devanagari")
            )

            # Apply name cleanup to raga/tala (defensive — MetadataParser
            # already normalises, but this ensures clean output even when
            # metadata comes from other parsers or fallback paths).
            raga_name = cleanup_raga_tala_name(metadata.raga) if metadata.raga else "Unknown"
            tala_name = cleanup_raga_tala_name(metadata.tala) if metadata.tala else "Unknown"

            # For Devanagari titles, produce an IAST alternate title for matching
            alternate_title = metadata.alternate_title
            if primary_script == "devanagari" and not alternate_title:
                iast_title = self.transliterator.transliterate(
                    metadata.title, "devanagari", "iast"
                )
                if iast_title:
                    alternate_title = iast_title

            # Build canonical extraction
            extraction = CanonicalExtraction(
                title=metadata.title,
                alternateTitle=alternate_title,
                composer=metadata.composer or composer_hint or "Unknown",
                musicalForm=MusicalForm.KRITHI,
                ragas=[CanonicalRaga(name=raga_name)],
                tala=tala_name,
                sections=canonical_sections,
                lyricVariants=lyric_variants,
                metadataBoundaries=metadata_boundaries,
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

    def _should_force_ocr_for_garbled_devanagari(self, document: DocumentContent) -> bool:
        """Detect broken Devanagari extraction and force OCR fallback."""
        page_text = "\n".join(page.text for page in document.pages if page.text)
        if not page_text.strip():
            return False

        non_space_chars = [ch for ch in page_text if not ch.isspace()]
        if not non_space_chars:
            return False

        replacement_count = page_text.count("\uFFFD")
        replacement_ratio = replacement_count / max(1, len(non_space_chars))
        devanagari_count = sum(1 for ch in page_text if 0x0900 <= ord(ch) <= 0x097F)

        detected_script = self.transliterator.detect_script(page_text)
        looks_devanagari = detected_script == "devanagari" or devanagari_count >= 20
        devanagari_garbled = looks_devanagari and replacement_ratio >= 0.05 and replacement_count >= 20

        # Defensive fallback: heavily corrupted text can lose enough script
        # signal to mis-detect script, but still be unusable for parsing.
        globally_garbled = replacement_ratio >= 0.20 and replacement_count >= 200

        return devanagari_garbled or globally_garbled

    def _extract_pdf_ocr(
        self,
        task: ExtractionTask,
        pdf_path: Path,
        page_range: tuple[int, int] | None,
    ) -> list[CanonicalExtraction]:
        """Extract from scanned PDF using OCR fallback."""
        page_texts = self.ocr_fallback.extract_document_text(str(pdf_path), page_range)

        if not page_texts:
            logger.warning("OCR produced no text", extra={"task_id": str(task.id)})
            return []

        checksum = sha256(pdf_path.read_bytes()).hexdigest()
        results: list[CanonicalExtraction] = []

        for page_num in sorted(page_texts.keys()):
            page_text = page_texts.get(page_num, "")
            if not page_text.strip():
                continue

            metadata = self.metadata_parser.parse(page_text[:500])
            parse_result = self.structure_parser.parse(page_text)
            canonical_sections = self.structure_parser.to_canonical_sections(parse_result.sections)
            lyric_variants = self.structure_parser.to_canonical_lyric_variants(
                parse_result.lyric_variants
            )
            metadata_boundaries = self.structure_parser.to_canonical_metadata_boundaries(
                parse_result.metadata_boundaries
            )

            if not lyric_variants and parse_result.sections:
                fallback_script = self.transliterator.detect_script(page_text) or "devanagari"
                fallback_language = "sa" if fallback_script == "devanagari" else "en"
                lyric_variants = [
                    CanonicalLyricVariant(
                        language=fallback_language,
                        script=fallback_script,
                        sections=self.structure_parser.to_canonical_lyric_sections(
                            parse_result.sections
                        ),
                    )
                ]
            lyric_variants = self._truncate_lyric_variants(lyric_variants)
            primary_script = (
                lyric_variants[0].script
                if lyric_variants
                else (self.transliterator.detect_script(page_text) or "devanagari")
            )
            alternate_title = metadata.alternate_title
            if primary_script == "devanagari" and not alternate_title:
                iast_title = self.transliterator.transliterate(
                    metadata.title, "devanagari", "iast"
                )
                if iast_title:
                    alternate_title = iast_title

            results.append(
                CanonicalExtraction(
                title=metadata.title,
                alternateTitle=alternate_title,
                composer=metadata.composer or task.request_payload.get("composerHint", "Unknown"),
                musicalForm=MusicalForm.KRITHI,
                ragas=[CanonicalRaga(name=metadata.raga or "Unknown")],
                tala=metadata.tala or "Unknown",
                sections=canonical_sections,
                lyricVariants=lyric_variants,
                metadataBoundaries=metadata_boundaries,
                deity=metadata.deity,
                temple=metadata.temple,
                sourceUrl=task.source_url,
                sourceName=task.source_name or "unknown",
                sourceTier=task.source_tier or 5,
                extractionMethod=ExtractionMethod.PDF_OCR,
                extractionTimestamp=datetime.now(timezone.utc).isoformat(),
                pageRange=str(page_num + 1),
                checksum=checksum,
            )
            )

        return results

    def _extract_html(self, task: ExtractionTask) -> list[CanonicalExtraction]:
        """Extract one canonical composition from an HTML source page."""
        html_path = self._download_source(task.source_url, source_format="HTML")
        html_bytes = html_path.read_bytes()
        html_content = html_bytes.decode("utf-8", errors="ignore")
        extracted = self.html_extractor.extract(html_content, base_url=task.source_url)

        if not extracted.text.strip():
            logger.warning("HTML extraction produced no text", extra={"task_id": str(task.id)})
            return []

        normalized_body = normalize_garbled_diacritics(extracted.text)
        metadata = self.metadata_parser.parse(
            normalized_body[:500],
            title_hint=extracted.title,
        )

        parse_result = self.structure_parser.parse(normalized_body)
        canonical_sections = self.structure_parser.to_canonical_sections(parse_result.sections)
        lyric_variants = self.structure_parser.to_canonical_lyric_variants(
            parse_result.lyric_variants
        )
        metadata_boundaries = self.structure_parser.to_canonical_metadata_boundaries(
            parse_result.metadata_boundaries
        )
        if not lyric_variants and parse_result.sections:
            fallback_script = self.transliterator.detect_script(normalized_body) or "latin"
            fallback_language = "sa" if fallback_script == "devanagari" else "en"
            lyric_variants = [
                CanonicalLyricVariant(
                    language=fallback_language,
                    script=fallback_script,
                    sections=self.structure_parser.to_canonical_lyric_sections(
                        parse_result.sections
                    ),
                )
            ]
        lyric_variants = self._truncate_lyric_variants(lyric_variants)

        raga_name = cleanup_raga_tala_name(metadata.raga) if metadata.raga else "Unknown"
        tala_name = cleanup_raga_tala_name(metadata.tala) if metadata.tala else "Unknown"

        return [
            CanonicalExtraction(
                title=metadata.title,
                alternateTitle=metadata.alternate_title,
                composer=metadata.composer or task.request_payload.get("composerHint", "Unknown"),
                musicalForm=MusicalForm.KRITHI,
                ragas=[CanonicalRaga(name=raga_name)],
                tala=tala_name,
                sections=canonical_sections,
                lyricVariants=lyric_variants,
                metadataBoundaries=metadata_boundaries,
                deity=metadata.deity,
                temple=metadata.temple,
                templeLocation=metadata.temple_location,
                sourceUrl=task.source_url,
                sourceName=task.source_name or "unknown",
                sourceTier=task.source_tier or 5,
                extractionMethod=ExtractionMethod.HTML_JSOUP,
                extractionTimestamp=datetime.now(timezone.utc).isoformat(),
                checksum=sha256(html_bytes).hexdigest(),
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
