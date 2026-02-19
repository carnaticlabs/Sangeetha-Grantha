"""Tesseract OCR integration for scanned PDF pages.

Invoked as a fallback when PyMuPDF text extraction returns empty or garbled
text (>50% non-printable characters). Supports Indic language packs:
Sanskrit (Devanagari), Tamil, Telugu, Kannada, Malayalam.
"""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Optional

import fitz  # PyMuPDF — for rendering pages to images

logger = logging.getLogger(__name__)

# Language codes for Tesseract Indic packs
TESSERACT_LANG_MAP = {
    "sa": "san",   # Sanskrit
    "ta": "tam",   # Tamil
    "te": "tel",   # Telugu
    "kn": "kan",   # Kannada
    "ml": "mal",   # Malayalam
    "hi": "hin",   # Hindi
    "en": "eng",   # English
}


class OcrFallback:
    """OCR extraction for scanned or image-based PDF pages."""

    def __init__(self, languages: list[str] | None = None, dpi: int = 300) -> None:
        """Initialize OCR with specified languages.

        Args:
            languages: List of language codes (sa, ta, te, kn, ml, en).
                       Defaults to Sanskrit + English.
            dpi: Resolution for rendering PDF pages to images.
        """
        self.languages = languages or ["sa", "en"]
        self.dpi = dpi

        # Build Tesseract language string
        tesseract_langs = [TESSERACT_LANG_MAP.get(lang, lang) for lang in self.languages]
        self.tesseract_lang_str = "+".join(tesseract_langs)

    def extract_page_text(self, pdf_path: str | Path, page_number: int) -> str:
        """Extract text from a single PDF page using OCR.

        Args:
            pdf_path: Path to the PDF file.
            page_number: 0-based page number.

        Returns:
            Extracted text from OCR.
        """
        try:
            import pytesseract
            from PIL import Image
        except ImportError:
            logger.error("pytesseract or Pillow not installed; OCR unavailable")
            return ""

        doc = fitz.open(str(pdf_path))
        page = doc[page_number]

        # Render page to image at specified DPI
        mat = fitz.Matrix(self.dpi / 72, self.dpi / 72)
        pix = page.get_pixmap(matrix=mat)

        # Convert to PIL Image
        img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)

        # Run OCR
        text = pytesseract.image_to_string(
            img,
            lang=self.tesseract_lang_str,
            config="--psm 6",  # Assume uniform block of text
        )

        doc.close()
        logger.info(
            "OCR extracted text",
            extra={
                "page": page_number,
                "text_length": len(text),
                "languages": self.tesseract_lang_str,
            },
        )

        return text

    def extract_document_text(
        self,
        pdf_path: str | Path,
        page_range: Optional[tuple[int, int]] = None,
    ) -> dict[int, str]:
        """Extract text from multiple pages using OCR.

        Args:
            pdf_path: Path to the PDF file.
            page_range: Optional (start, end) page numbers (0-based, inclusive).

        Returns:
            Dict mapping page number to extracted text.
        """
        doc = fitz.open(str(pdf_path))
        total_pages = len(doc)
        doc.close()

        start = page_range[0] if page_range else 0
        end = page_range[1] if page_range else total_pages - 1
        end = min(end, total_pages - 1)

        results: dict[int, str] = {}
        for page_num in range(start, end + 1):
            text = self.extract_page_text(pdf_path, page_num)
            results[page_num] = text

        return results

    @staticmethod
    def is_garbled(text: str, threshold: float = 0.5) -> bool:
        """Check if text appears to be garbled (OCR needed).

        Args:
            text: Extracted text to check.
            threshold: Proportion of non-printable characters that triggers OCR.

        Returns:
            True if text appears garbled and OCR should be used.
        """
        if not text or len(text.strip()) < 10:
            return True

        printable_count = sum(1 for c in text if c.isprintable() or c.isspace())
        return printable_count / len(text) < threshold
