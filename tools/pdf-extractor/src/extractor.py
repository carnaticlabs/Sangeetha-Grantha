"""PDF text extraction using PyMuPDF (fitz).

Primary extraction engine for born-digital PDFs. Extracts full Unicode text
with font-size and positional data, supporting Devanagari, Tamil, Telugu,
Kannada, Malayalam, and Latin scripts.
"""

from __future__ import annotations

import hashlib
import logging
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import fitz  # PyMuPDF

logger = logging.getLogger(__name__)


@dataclass
class TextBlock:
    """A block of text extracted from a PDF page with positional metadata."""

    text: str
    page_number: int  # 0-based
    x0: float
    y0: float
    x1: float
    y1: float
    font_size: float
    font_name: str
    is_bold: bool = False

    @property
    def height(self) -> float:
        return self.y1 - self.y0

    @property
    def width(self) -> float:
        return self.x1 - self.x0


@dataclass
class PageContent:
    """Extracted content from a single PDF page."""

    page_number: int  # 0-based
    text: str
    blocks: list[TextBlock] = field(default_factory=list)
    width: float = 0.0
    height: float = 0.0


@dataclass
class DocumentContent:
    """Full extracted content from a PDF document."""

    pages: list[PageContent]
    total_pages: int
    checksum: str  # SHA-256 of the PDF file
    source_path: str


class PdfExtractor:
    """Extract text and positional data from PDF documents using PyMuPDF."""

    def extract_document(
        self,
        pdf_path: str | Path,
        page_range: Optional[tuple[int, int]] = None,
    ) -> DocumentContent:
        """Extract text from a PDF document.

        Args:
            pdf_path: Path to the PDF file.
            page_range: Optional (start, end) page numbers (0-based, inclusive).
                        If None, extracts all pages.

        Returns:
            DocumentContent with extracted text and positional metadata.
        """
        pdf_path = Path(pdf_path)
        checksum = self._compute_checksum(pdf_path)

        doc = fitz.open(str(pdf_path))
        total_pages = len(doc)

        start_page = page_range[0] if page_range else 0
        end_page = page_range[1] if page_range else total_pages - 1
        end_page = min(end_page, total_pages - 1)

        pages: list[PageContent] = []

        for page_num in range(start_page, end_page + 1):
            page = doc[page_num]
            page_content = self._extract_page(page, page_num)
            pages.append(page_content)

        doc.close()

        logger.info(
            "Extracted PDF",
            extra={
                "path": str(pdf_path),
                "pages_extracted": len(pages),
                "total_pages": total_pages,
            },
        )

        return DocumentContent(
            pages=pages,
            total_pages=total_pages,
            checksum=checksum,
            source_path=str(pdf_path),
        )

    def _extract_page(self, page: fitz.Page, page_number: int) -> PageContent:
        """Extract text blocks from a single PDF page."""
        # Get page dimensions
        rect = page.rect
        width, height = rect.width, rect.height

        # Extract text with positional data using "dict" mode for font info
        page_dict = page.get_text("dict", flags=fitz.TEXT_PRESERVE_WHITESPACE)

        blocks: list[TextBlock] = []
        full_text_parts: list[str] = []

        for block in page_dict.get("blocks", []):
            if block.get("type") != 0:  # Skip image blocks
                continue

            for line in block.get("lines", []):
                line_text_parts: list[str] = []

                for span in line.get("spans", []):
                    text = span.get("text", "").strip()
                    if not text:
                        continue

                    font_size = span.get("size", 0.0)
                    font_name = span.get("font", "")
                    is_bold = "Bold" in font_name or "bold" in font_name

                    bbox = span.get("bbox", (0, 0, 0, 0))
                    text_block = TextBlock(
                        text=text,
                        page_number=page_number,
                        x0=bbox[0],
                        y0=bbox[1],
                        x1=bbox[2],
                        y1=bbox[3],
                        font_size=font_size,
                        font_name=font_name,
                        is_bold=is_bold,
                    )
                    blocks.append(text_block)
                    line_text_parts.append(text)

                if line_text_parts:
                    full_text_parts.append(" ".join(line_text_parts))

        full_text = "\n".join(full_text_parts)

        return PageContent(
            page_number=page_number,
            text=full_text,
            blocks=blocks,
            width=width,
            height=height,
        )

    def _compute_checksum(self, file_path: Path) -> str:
        """Compute SHA-256 checksum of a file."""
        sha256 = hashlib.sha256()
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(8192), b""):
                sha256.update(chunk)
        return sha256.hexdigest()

    def is_text_extractable(self, pdf_path: str | Path) -> bool:
        """Check if a PDF has extractable text (vs. scanned/image-only).

        Returns True if at least 50% of pages have non-trivial text content.
        """
        doc = fitz.open(str(pdf_path))
        text_pages = 0

        for page in doc:
            text = page.get_text().strip()
            if len(text) > 50:  # More than just page numbers/headers
                text_pages += 1

        doc.close()

        return text_pages > len(doc) * 0.5 if len(doc) > 0 else False
