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
        r"(?:raga|raaga|rāga|राग)\s*[:—–-]?\s*",
        re.IGNORECASE,
    )
    TALA_PATTERN = re.compile(
        r"(?:tala|taala|tāla|ताल)\s*[:—–-]?\s*",
        re.IGNORECASE,
    )

    # Pattern for Krithi title detection (heuristic: title is followed by Raga/Tala)
    METADATA_LINE_PATTERN = re.compile(
        r"(?:raga|tala|raaga|taala|rāga|tāla|deity|kshetra|temple)",
        re.IGNORECASE,
    )

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

        Returns list of (page_index, TextBlock) tuples.
        """
        min_title_size = body_font_size * self.title_font_size_threshold
        title_positions: list[tuple[int, TextBlock]] = []

        for page_idx, page in enumerate(document.pages):
            for block in page.blocks:
                if block.font_size >= min_title_size and block.is_bold:
                    # Check if followed by Raga/Tala metadata within nearby blocks
                    if self._has_metadata_nearby(page, block):
                        title_positions.append((page_idx, block))

        return title_positions

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
