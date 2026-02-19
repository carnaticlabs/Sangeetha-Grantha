"""Krithi boundary detection in anthology PDFs.

In anthology-format PDFs (like guruguha.org's mdskt.pdf with ~484 Krithis),
each composition starts with a title in bold/larger font, followed by Raga/Tala
metadata. This module detects those boundaries using font-size analysis and
structural pattern matching.
"""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass, field
from typing import Optional

from .extractor import DocumentContent, PageContent, TextBlock

logger = logging.getLogger(__name__)


@dataclass
class KrithiSegment:
    """A segment of a PDF corresponding to a single Krithi."""

    start_page: int  # 0-based
    end_page: int  # 0-based, inclusive
    title_text: str
    header_blocks: list[TextBlock] = field(default_factory=list)
    body_blocks: list[TextBlock] = field(default_factory=list)
    body_text: str = ""

    @property
    def page_range_str(self) -> str:
        """Human-readable page range (1-based)."""
        if self.start_page == self.end_page:
            return str(self.start_page + 1)
        return f"{self.start_page + 1}-{self.end_page + 1}"


class PageSegmenter:
    """Detect Krithi boundaries in anthology PDFs.

    Uses font-size analysis to identify title lines (typically the largest font
    on the page) and structural patterns (Raga/Tala headers following titles)
    to segment the document into individual Krithi blocks.
    """

    # Common patterns for Raga/Tala metadata lines
    RAGA_PATTERN = re.compile(
        r"(?:raga|raaga|rāga|r\u00AFa+ga|राग)\s*[\u02D9.]?\s*[\u1E41m]?\s*[:—–-]?\s*",
        re.IGNORECASE,
    )
    TALA_PATTERN = re.compile(
        r"(?:tala|taala|tāla|t\u00AFal\.?\s*a|ताल)\s*[\u02D9.]?\s*[\u1E41m]?\s*[:—–-]?\s*",
        re.IGNORECASE,
    )

    # Pattern for Krithi title detection (heuristic: title is followed by Raga/Tala).
    # Handles standard ASCII, Unicode diacritics, and transliterated Sanskrit
    # with standalone macrons (¯) found in academic PDF encodings (e.g. r¯aga).
    METADATA_LINE_PATTERN = re.compile(
        r"(?:r[\u00AFā]?a+ga|rāga|राग"
        r"|t[\u00AFā]?a+l[.\u1E37]?\s*a|tāla|ताल"
        r"|deity|kshetra|temple)",
        re.IGNORECASE,
    )

    # Dotted leader pattern used to detect table-of-contents pages
    _TOC_LEADER_PATTERN = re.compile(r"(?:\.\s){4,}")

    # Minimum number of dotted leaders to classify a page as TOC
    _TOC_LEADER_MIN_COUNT = 3

    def __init__(
        self,
        title_font_size_threshold: float = 1.3,
        min_body_length: int = 20,
    ) -> None:
        """Initialize the page segmenter.

        Args:
            title_font_size_threshold: Minimum ratio of title font size to body
                                        font size to detect a title.
            min_body_length: Minimum text length for a valid Krithi body.
        """
        self.title_font_size_threshold = title_font_size_threshold
        self.min_body_length = min_body_length

    def segment(self, document: DocumentContent) -> list[KrithiSegment]:
        """Segment a multi-Krithi PDF into individual Krithi blocks.

        Args:
            document: Extracted document content from PdfExtractor.

        Returns:
            List of KrithiSegment, one per detected composition.
        """
        if not document.pages:
            return []

        # Step 1: Determine the dominant body font size across the document
        body_font_size = self._detect_body_font_size(document)

        # Step 2: Find title boundaries (lines with significantly larger font)
        title_positions = self._find_title_positions(document, body_font_size)

        if not title_positions:
            logger.warning("No title boundaries detected; treating entire document as one segment")
            return [self._single_segment(document)]

        # Step 3: Build segments from title positions
        segments = self._build_segments(document, title_positions)

        logger.info(
            "Segmentation complete",
            extra={
                "total_pages": document.total_pages,
                "segments_found": len(segments),
                "body_font_size": body_font_size,
            },
        )

        return segments

    def _detect_body_font_size(self, document: DocumentContent) -> float:
        """Determine the most common (body) font size in the document."""
        size_counts: dict[float, int] = {}
        for page in document.pages:
            for block in page.blocks:
                # Round to nearest 0.5 to group similar sizes
                rounded_size = round(block.font_size * 2) / 2
                if rounded_size > 0:
                    size_counts[rounded_size] = size_counts.get(rounded_size, 0) + len(block.text)

        if not size_counts:
            return 12.0  # Default fallback

        # Most common size by character count is the body font
        return max(size_counts, key=size_counts.get)  # type: ignore[arg-type]

    def _find_title_positions(
        self,
        document: DocumentContent,
        body_font_size: float,
    ) -> list[tuple[int, TextBlock]]:
        """Find text blocks that appear to be Krithi titles.

        Uses adaptive thresholds: tries the configured threshold first, then
        progressively relaxes if no titles are found.  This handles PDFs where
        titles are only slightly larger than body text (e.g. 17pt bold titles
        with 14.5pt body gives a 1.19× ratio, below the default 1.3×).

        Returns list of (page_index, TextBlock) tuples.
        """
        # Build list of thresholds to try (strict → relaxed)
        thresholds = [self.title_font_size_threshold]
        for fallback in (1.15, 1.05):
            if fallback < self.title_font_size_threshold:
                thresholds.append(fallback)

        for threshold in thresholds:
            min_title_size = body_font_size * threshold
            candidates: list[tuple[int, TextBlock]] = []

            for page_idx, page in enumerate(document.pages):
                # Skip pages that look like table-of-contents
                if self._is_toc_page(page):
                    continue

                for block in page.blocks:
                    if block.font_size >= min_title_size and block.is_bold:
                        if self._has_metadata_nearby(page, block):
                            candidates.append((page_idx, block))

            # Deduplicate close title blocks (handles repeated/italic title lines)
            deduplicated = self._deduplicate_title_positions(candidates)

            if len(deduplicated) >= 2:
                logger.info(
                    "Title detection succeeded",
                    extra={
                        "threshold_ratio": threshold,
                        "min_title_size": round(min_title_size, 2),
                        "body_font_size": body_font_size,
                        "raw_candidates": len(candidates),
                        "deduplicated_titles": len(deduplicated),
                    },
                )
                return deduplicated

        # TRACK-060: Fallback — no bold candidates found at any threshold.
        # Try font-size-only detection with metadata proximity as gatekeeper.
        # This handles Devanagari PDFs where fonts lack "Bold" in their name
        # and PyMuPDF flags may not indicate bold.
        return self._find_title_positions_by_size_only(document, body_font_size)

    def _find_title_positions_by_size_only(
        self,
        document: DocumentContent,
        body_font_size: float,
    ) -> list[tuple[int, TextBlock]]:
        """Fallback title detection using font size only (no bold requirement).

        TRACK-060: For PDFs where bold detection fails (e.g. Devanagari fonts),
        find title candidates by font size alone, using metadata proximity
        (_has_metadata_nearby) as a strict gatekeeper to avoid false positives.

        Tries progressively relaxed thresholds (1.3×, 1.15×, 1.05×).
        """
        thresholds = [self.title_font_size_threshold]
        for fallback in (1.15, 1.05):
            if fallback < self.title_font_size_threshold:
                thresholds.append(fallback)

        for threshold in thresholds:
            min_title_size = body_font_size * threshold
            candidates: list[tuple[int, TextBlock]] = []

            for page_idx, page in enumerate(document.pages):
                if self._is_toc_page(page):
                    continue

                for block in page.blocks:
                    # Size check only — no bold requirement
                    if block.font_size >= min_title_size:
                        # Strict gatekeeper: must have raga/tala metadata nearby
                        if self._has_metadata_nearby(page, block):
                            candidates.append((page_idx, block))

            deduplicated = self._deduplicate_title_positions(candidates)

            if len(deduplicated) >= 2:
                logger.info(
                    "Fallback title detection (size-only) succeeded",
                    extra={
                        "threshold_ratio": threshold,
                        "min_title_size": round(min_title_size, 2),
                        "body_font_size": body_font_size,
                        "raw_candidates": len(candidates),
                        "deduplicated_titles": len(deduplicated),
                    },
                )
                return deduplicated

        return []

    def _is_toc_page(self, page: PageContent) -> bool:
        """Detect table-of-contents pages by dotted leader patterns.

        TOC pages in anthology PDFs typically contain many dotted leader
        lines (". . . . . . . . . . 17") connecting titles to page numbers.
        """
        leader_count = len(self._TOC_LEADER_PATTERN.findall(page.text))
        return leader_count >= self._TOC_LEADER_MIN_COUNT

    def _deduplicate_title_positions(
        self,
        positions: list[tuple[int, TextBlock]],
    ) -> list[tuple[int, TextBlock]]:
        """Merge title blocks that are close together on the same page.

        Anthology PDFs often repeat the title (bold + bold-italic) or place
        a separate number block (e.g. "1") at the same y-position as the
        title text.  This groups nearby blocks and picks the best
        representative (longest meaningful text) from each group.
        """
        if not positions:
            return []

        deduplicated: list[tuple[int, TextBlock]] = []
        current_group: list[tuple[int, TextBlock]] = [positions[0]]

        for pos in positions[1:]:
            page_idx, block = pos
            prev_page, prev_block = current_group[-1]

            # Same page and vertically close → same title group
            if page_idx == prev_page and abs(block.y0 - prev_block.y0) < 60:
                current_group.append(pos)
            else:
                deduplicated.append(self._pick_best_title(current_group))
                current_group = [pos]

        deduplicated.append(self._pick_best_title(current_group))

        return deduplicated

    @staticmethod
    def _pick_best_title(
        group: list[tuple[int, TextBlock]],
    ) -> tuple[int, TextBlock]:
        """Pick the best representative title from a group of close blocks.

        Prefers the block with the longest meaningful text, skipping
        number-only blocks (like "1", "42") that are Krithi serial numbers
        rather than actual titles.
        """
        # Filter out number-only blocks when there are text blocks available
        text_blocks = [
            (idx, b) for idx, b in group if not b.text.strip().isdigit()
        ]
        candidates = text_blocks if text_blocks else group
        return max(candidates, key=lambda x: len(x[1].text))

    def _has_metadata_nearby(self, page: PageContent, title_block: TextBlock) -> bool:
        """Check if a title block is followed by Raga/Tala metadata."""
        # Look at blocks below the title on the same page
        for block in page.blocks:
            if block.y0 > title_block.y1 and block.y0 < title_block.y1 + 100:
                if self.METADATA_LINE_PATTERN.search(block.text):
                    return True
        # If we're near the top of a page, it's likely a title even without nearby metadata
        if title_block.y0 < page.height * 0.15:
            return True
        return False

    def _build_segments(
        self,
        document: DocumentContent,
        title_positions: list[tuple[int, TextBlock]],
    ) -> list[KrithiSegment]:
        """Build KrithiSegments from detected title positions."""
        segments: list[KrithiSegment] = []

        for i, (page_idx, title_block) in enumerate(title_positions):
            # End page is the page before the next title, or the last page
            if i + 1 < len(title_positions):
                next_page_idx = title_positions[i + 1][0]
                # If next title is on the same page, end on this page
                end_page = max(page_idx, next_page_idx - 1) if next_page_idx > page_idx else page_idx
            else:
                end_page = len(document.pages) - 1

            # Collect body blocks (everything between this title and the next)
            body_blocks: list[TextBlock] = []
            body_text_parts: list[str] = []

            for p_idx in range(page_idx, end_page + 1):
                page = document.pages[p_idx]
                for block in page.blocks:
                    if p_idx == page_idx and block.y0 <= title_block.y1:
                        continue  # Skip title block itself
                    if p_idx == end_page and i + 1 < len(title_positions):
                        next_title = title_positions[i + 1][1]
                        if page_idx == title_positions[i + 1][0] and block.y0 >= next_title.y0:
                            break
                    body_blocks.append(block)
                    body_text_parts.append(block.text)

            segment = KrithiSegment(
                start_page=page_idx,
                end_page=end_page,
                title_text=title_block.text.strip(),
                header_blocks=[title_block],
                body_blocks=body_blocks,
                body_text="\n".join(body_text_parts),
            )
            segments.append(segment)

        return segments

    def _single_segment(self, document: DocumentContent) -> KrithiSegment:
        """Create a single segment for the entire document (fallback)."""
        all_blocks = [b for p in document.pages for b in p.blocks]
        full_text = "\n".join(p.text for p in document.pages)
        title = all_blocks[0].text.strip() if all_blocks else "Unknown"

        return KrithiSegment(
            start_page=0,
            end_page=len(document.pages) - 1,
            title_text=title,
            header_blocks=all_blocks[:1],
            body_blocks=all_blocks[1:],
            body_text=full_text,
        )
