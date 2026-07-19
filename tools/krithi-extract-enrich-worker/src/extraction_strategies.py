"""Format-specific extraction strategies.

Each strategy encapsulates the parsing pipeline for one source format
(PDF, HTML, DOCX, IMAGE). The worker stays a queue-polling coordinator:
it claims tasks, dispatches to the matching strategy by ``source_format``,
and persists the outcome.

Enrichment (identity discovery, Gemini metadata fill, normalized matching
keys) remains the worker's concern and is injected as a ``finalize``
callback applied to every extraction a strategy produces.
"""

from __future__ import annotations

import logging
from abc import ABC, abstractmethod
from collections.abc import Callable
from datetime import UTC, datetime
from hashlib import sha256
from pathlib import Path
from typing import ClassVar

import httpx

from .config import ExtractorConfig
from .db import ExtractionTask
from .diacritic_normalizer import cleanup_raga_tala_name
from .extractor import DocumentContent, PdfExtractor
from .heuristics import infer_composer_from_url, is_valid_segment_title
from .html_extractor import HtmlTextExtractor
from .metadata_parser import MetadataParser
from .normalizer import normalize_garbled_diacritics
from .ocr_fallback import OcrFallback
from .page_segmenter import PageSegmenter
from .schema import (
    CanonicalExtraction,
    CanonicalLyricSection,
    CanonicalLyricVariant,
    CanonicalRaga,
    ExtractionMethod,
    MusicalForm,
)
from .structure_parser import StructureParser, StructureParseResult
from .transliterator import Transliterator

logger = logging.getLogger(__name__)

# Applied by the worker to every extraction a strategy produces:
# (extraction, source_text, source_format) -> enriched extraction.
FinalizeExtraction = Callable[[CanonicalExtraction, str, str], CanonicalExtraction]


def parse_page_range(value: str | None) -> tuple[int, int] | None:
    """Parse a 1-based page range like "3-7" or "5" into a 0-based inclusive tuple.

    TRACK-130: this lived twice, here and in `cli.py`. Single definition now.
    Returns None for empty or unparseable input, matching prior behaviour.
    """
    if not value:
        return None
    parts = value.split("-")
    if len(parts) == 2:
        return (int(parts[0]) - 1, int(parts[1]) - 1)
    if len(parts) == 1:
        page_num = int(parts[0]) - 1
        return (page_num, page_num)
    return None


class ExtractionStrategy(ABC):
    """Base class for format-specific extraction pipelines."""

    source_format: ClassVar[str]
    default_extraction_method: ClassVar[ExtractionMethod]
    default_extension: ClassVar[str] = ".bin"

    def __init__(self, config: ExtractorConfig, finalize: FinalizeExtraction) -> None:
        self.config = config
        self._finalize = finalize
        self._http_client: httpx.Client | None = None

    @property
    def http_client(self) -> httpx.Client:
        """Long-lived client so connections pool across downloads (TRACK-129).

        Created lazily and rebuilt if it was closed, so a strategy stays usable
        after `close()`. Timeout/redirect semantics are unchanged.
        """
        if self._http_client is None or self._http_client.is_closed:
            self._http_client = httpx.Client(timeout=120.0, follow_redirects=True)
        return self._http_client

    def close(self) -> None:
        """Release the pooled HTTP connections. Tied to worker shutdown."""
        if self._http_client is not None and not self._http_client.is_closed:
            self._http_client.close()
        self._http_client = None

    @abstractmethod
    def extract(self, task: ExtractionTask) -> list[CanonicalExtraction]:
        """Extract canonical compositions from the task's source document."""

    def _download_source(self, url: str) -> Path:
        """Download a source document to the cache directory, or resolve a local file path."""
        # Support local file paths (bare paths and file:// URIs)
        local_path = self._resolve_local_path(url)
        if local_path is not None:
            if not local_path.exists():
                raise FileNotFoundError(f"Local source file not found: {local_path}")
            logger.info(f"Using local source file: {local_path}")
            return local_path

        cache_dir = Path(self.config.cache_dir)
        cache_dir.mkdir(parents=True, exist_ok=True)

        # Use URL hash as filename
        url_hash = sha256(url.encode()).hexdigest()[:16]
        extension = Path(url).suffix or self.default_extension
        cached_path = cache_dir / f"{url_hash}{extension}"

        if cached_path.exists():
            logger.debug(f"Using cached artifact: {cached_path}")
            return cached_path

        logger.info(f"Downloading source: {url}")
        response = self.http_client.get(url)
        response.raise_for_status()
        cached_path.write_bytes(response.content)

        return cached_path

    @staticmethod
    def _resolve_local_path(url: str) -> Path | None:
        """Return a Path if the URL is a local file reference, else None."""
        if url.startswith("file://"):
            return Path(url[7:])
        if url.startswith("/"):
            return Path(url)
        return None

    @staticmethod
    def _filter_empty_lyric_sections(
        variants: list[CanonicalLyricVariant],
    ) -> list[CanonicalLyricVariant]:
        """Drop sections with no text (and variants left empty).

        The full lyric text is preserved verbatim. The stored ``text`` column is
        not indexed (the indexed ``normalized_text`` column is populated
        separately), so there is no byte-size ceiling to defend against here.
        Byte-truncating the lyric silently cut multi-section blobs mid-composition
        for Indic scripts (~3 bytes/char) — see CAT-B regression fixtures.
        """
        filtered: list[CanonicalLyricVariant] = []
        for variant in variants:
            sections = [
                CanonicalLyricSection(
                    section_order=section.section_order,
                    text=section.text,
                )
                for section in variant.sections
                if section.text.strip()
            ]
            if not sections:
                continue
            filtered.append(
                CanonicalLyricVariant(
                    language=variant.language,
                    script=variant.script,
                    sections=sections,
                )
            )
        return filtered


class _TextPipelineStrategy(ExtractionStrategy):
    """Shared plumbing for strategies that run the text parsing pipeline."""

    def __init__(
        self,
        config: ExtractorConfig,
        finalize: FinalizeExtraction,
        *,
        structure_parser: StructureParser,
        metadata_parser: MetadataParser,
        transliterator: Transliterator,
    ) -> None:
        super().__init__(config, finalize)
        self.structure_parser = structure_parser
        self.metadata_parser = metadata_parser
        self.transliterator = transliterator

    def _build_lyric_variants(
        self,
        parse_result: StructureParseResult,
        source_text: str,
        default_script: str,
    ) -> list[CanonicalLyricVariant]:
        """Canonical lyric variants with the single-script fallback, empties dropped."""
        lyric_variants = self.structure_parser.to_canonical_lyric_variants(parse_result.lyric_variants)
        if not lyric_variants and parse_result.sections:
            fallback_script = self.transliterator.detect_script(source_text) or default_script
            fallback_language = "sa" if fallback_script == "devanagari" else "en"
            lyric_variants = [
                CanonicalLyricVariant(
                    language=fallback_language,
                    script=fallback_script,
                    sections=self.structure_parser.to_canonical_lyric_sections(parse_result.sections),
                )
            ]
        return self._filter_empty_lyric_sections(lyric_variants)

    @staticmethod
    def _build_ragas(parse_result: StructureParseResult, raga_name: str) -> list[CanonicalRaga]:
        """Ragas list from ragamalika subsections if detected, else the single raga."""
        if parse_result.ragamalika_subsections:
            ragas = [
                CanonicalRaga(
                    name=sub.raga_name,
                    order=sub.order,
                    section=sub.parent_section_type.value.lower(),
                )
                for sub in parse_result.ragamalika_subsections
                if not sub.is_viloma
            ]
            if not ragas:
                ragas = [CanonicalRaga(name=raga_name)]
        else:
            ragas = [CanonicalRaga(name=raga_name)]
        return ragas

    def _derive_alternate_title(
        self,
        title: str,
        alternate_title: str | None,
        primary_script: str,
    ) -> str | None:
        """For Devanagari titles, produce an IAST alternate title for matching."""
        if primary_script == "devanagari" and not alternate_title:
            iast_title = self.transliterator.transliterate(title, "devanagari", "iast")
            if iast_title:
                return iast_title
        return alternate_title


class PdfExtractionStrategy(_TextPipelineStrategy):
    """PDF extraction via PyMuPDF, with OCR fallback for scanned/garbled documents."""

    source_format = "PDF"
    default_extraction_method = ExtractionMethod.PDF_PYMUPDF
    default_extension = ".pdf"

    def __init__(
        self,
        config: ExtractorConfig,
        finalize: FinalizeExtraction,
        *,
        pdf_extractor: PdfExtractor,
        page_segmenter: PageSegmenter,
        ocr_fallback: OcrFallback,
        structure_parser: StructureParser,
        metadata_parser: MetadataParser,
        transliterator: Transliterator,
    ) -> None:
        super().__init__(
            config,
            finalize,
            structure_parser=structure_parser,
            metadata_parser=metadata_parser,
            transliterator=transliterator,
        )
        self.pdf_extractor = pdf_extractor
        self.page_segmenter = page_segmenter
        self.ocr_fallback = ocr_fallback

    def extract(self, task: ExtractionTask) -> list[CanonicalExtraction]:
        """Extract Krithis from a PDF document."""
        # Download PDF
        pdf_path = self._download_source(task.source_url)

        # Parse page range from task
        page_range = parse_page_range(task.page_range)

        # Check if text is extractable (vs. scanned)
        use_ocr = not self.pdf_extractor.is_text_extractable(str(pdf_path))

        if use_ocr:
            logger.info("PDF requires OCR", extra={"task_id": str(task.id)})
            return self._extract_ocr(task, pdf_path, page_range)

        # Extract text with PyMuPDF
        document = self.pdf_extractor.extract_document(str(pdf_path), page_range)
        if self._should_force_ocr_for_garbled_devanagari(document):
            logger.info(
                "Forcing OCR due to garbled Devanagari text",
                extra={"task_id": str(task.id)},
            )
            return self._extract_ocr(task, pdf_path, page_range)

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
            metadata_boundaries = self.structure_parser.to_canonical_metadata_boundaries(
                parse_result.metadata_boundaries
            )
            lyric_variants = self._build_lyric_variants(parse_result, normalized_body, default_script="devanagari")
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

            ragas = self._build_ragas(parse_result, raga_name)
            alternate_title = self._derive_alternate_title(metadata.title, metadata.alternate_title, primary_script)

            # Build canonical extraction
            extraction = CanonicalExtraction(
                title=metadata.title,
                alternate_title=alternate_title,
                composer=metadata.composer or composer_hint or infer_composer_from_url(task.source_url) or "Unknown",
                musical_form=MusicalForm.KRITHI,
                ragas=ragas,
                tala=tala_name,
                sections=canonical_sections,
                lyric_variants=lyric_variants,
                metadata_boundaries=metadata_boundaries,
                deity=metadata.deity,
                temple=metadata.temple,
                temple_location=metadata.temple_location,
                source_url=task.source_url,
                source_name=task.source_name or "unknown",
                source_tier=task.source_tier or 5,
                extraction_method=ExtractionMethod.PDF_PYMUPDF,
                extraction_timestamp=datetime.now(UTC).isoformat(),
                page_range=segment.page_range_str,
                checksum=document.checksum,
            )

            if not is_valid_segment_title(extraction.title):
                logger.info(
                    "Skipping invalid segment title: %s",
                    extraction.title,
                    extra={"task_id": str(task.id)},
                )
                continue

            results.append(self._finalize(extraction, normalized_body, "PDF"))

        return results

    def _should_force_ocr_for_garbled_devanagari(self, document: DocumentContent) -> bool:
        """Detect broken Devanagari extraction and force OCR fallback."""
        page_text = "\n".join(page.text for page in document.pages if page.text)
        if not page_text.strip():
            return False

        non_space_chars = [ch for ch in page_text if not ch.isspace()]
        if not non_space_chars:
            return False

        replacement_count = page_text.count("\ufffd")
        replacement_ratio = replacement_count / max(1, len(non_space_chars))
        devanagari_count = sum(1 for ch in page_text if 0x0900 <= ord(ch) <= 0x097F)

        detected_script = self.transliterator.detect_script(page_text)
        looks_devanagari = detected_script == "devanagari" or devanagari_count >= 20
        devanagari_garbled = looks_devanagari and replacement_ratio >= 0.05 and replacement_count >= 20

        # Defensive fallback: heavily corrupted text can lose enough script
        # signal to mis-detect script, but still be unusable for parsing.
        globally_garbled = replacement_ratio >= 0.20 and replacement_count >= 200

        return devanagari_garbled or globally_garbled

    def _extract_ocr(
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
            metadata_boundaries = self.structure_parser.to_canonical_metadata_boundaries(
                parse_result.metadata_boundaries
            )
            lyric_variants = self._build_lyric_variants(parse_result, page_text, default_script="devanagari")
            primary_script = (
                lyric_variants[0].script
                if lyric_variants
                else (self.transliterator.detect_script(page_text) or "devanagari")
            )
            alternate_title = self._derive_alternate_title(metadata.title, metadata.alternate_title, primary_script)

            extraction = CanonicalExtraction(
                title=metadata.title,
                alternate_title=alternate_title,
                composer=metadata.composer
                or task.request_payload.get("composerHint")
                or infer_composer_from_url(task.source_url)
                or "Unknown",
                musical_form=MusicalForm.KRITHI,
                ragas=[CanonicalRaga(name=metadata.raga or "Unknown")],
                tala=metadata.tala or "Unknown",
                sections=canonical_sections,
                lyric_variants=lyric_variants,
                metadata_boundaries=metadata_boundaries,
                deity=metadata.deity,
                temple=metadata.temple,
                source_url=task.source_url,
                source_name=task.source_name or "unknown",
                source_tier=task.source_tier or 5,
                extraction_method=ExtractionMethod.PDF_OCR,
                extraction_timestamp=datetime.now(UTC).isoformat(),
                page_range=str(page_num + 1),
                checksum=checksum,
            )
            results.append(self._finalize(extraction, page_text, "PDF"))

        return results


class HtmlExtractionStrategy(_TextPipelineStrategy):
    """HTML extraction for blog/source pages holding one composition."""

    source_format = "HTML"
    default_extraction_method = ExtractionMethod.HTML_JSOUP
    default_extension = ".html"

    def __init__(
        self,
        config: ExtractorConfig,
        finalize: FinalizeExtraction,
        *,
        html_extractor: HtmlTextExtractor,
        structure_parser: StructureParser,
        metadata_parser: MetadataParser,
        transliterator: Transliterator,
    ) -> None:
        super().__init__(
            config,
            finalize,
            structure_parser=structure_parser,
            metadata_parser=metadata_parser,
            transliterator=transliterator,
        )
        self.html_extractor = html_extractor

    def extract(self, task: ExtractionTask) -> list[CanonicalExtraction]:
        """Extract one canonical composition from an HTML source page."""
        html_path = self._download_source(task.source_url)
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
        metadata_boundaries = self.structure_parser.to_canonical_metadata_boundaries(parse_result.metadata_boundaries)
        lyric_variants = self._build_lyric_variants(parse_result, normalized_body, default_script="latin")

        raga_hint = task.request_payload.get("ragaHint")
        raga_name = (
            cleanup_raga_tala_name(metadata.raga)
            if metadata.raga
            else (cleanup_raga_tala_name(raga_hint) if raga_hint else "Unknown")
        )
        tala_name = cleanup_raga_tala_name(metadata.tala) if metadata.tala else "Unknown"

        ragas = self._build_ragas(parse_result, raga_name)

        extraction = CanonicalExtraction(
            title=metadata.title,
            alternate_title=metadata.alternate_title,
            composer=metadata.composer
            or task.request_payload.get("composerHint")
            or infer_composer_from_url(task.source_url)
            or "Unknown",
            musical_form=MusicalForm.KRITHI,
            ragas=ragas,
            tala=tala_name,
            sections=canonical_sections,
            lyric_variants=lyric_variants,
            metadata_boundaries=metadata_boundaries,
            deity=metadata.deity,
            temple=metadata.temple,
            temple_location=metadata.temple_location,
            source_url=task.source_url,
            source_name=task.source_name or "unknown",
            source_tier=task.source_tier or 5,
            extraction_method=ExtractionMethod.HTML_JSOUP,
            extraction_timestamp=datetime.now(UTC).isoformat(),
            checksum=sha256(html_bytes).hexdigest(),
        )
        return [self._finalize(extraction, normalized_body, "HTML")]


class DocxExtractionStrategy(ExtractionStrategy):
    """Extract from DOCX documents. Placeholder for future implementation."""

    source_format = "DOCX"
    default_extraction_method = ExtractionMethod.DOCX_PYTHON
    default_extension = ".docx"

    def extract(self, task: ExtractionTask) -> list[CanonicalExtraction]:
        raise NotImplementedError("DOCX extraction not yet implemented")


class ImageExtractionStrategy(ExtractionStrategy):
    """Extract from image files using OCR. Placeholder for future implementation."""

    source_format = "IMAGE"
    default_extraction_method = ExtractionMethod.PDF_OCR
    default_extension = ".img"

    def extract(self, task: ExtractionTask) -> list[CanonicalExtraction]:
        raise NotImplementedError("Image extraction not yet implemented")
