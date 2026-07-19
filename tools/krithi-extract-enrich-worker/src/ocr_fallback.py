"""Tesseract OCR integration for scanned PDF pages.

Invoked as a fallback when PyMuPDF text extraction returns empty or garbled
text (>50% non-printable characters). Supports Indic language packs:
Sanskrit (Devanagari), Tamil, Telugu, Kannada, Malayalam.
"""

from __future__ import annotations

import logging
from pathlib import Path

import fitz  # PyMuPDF — for rendering pages to images

logger = logging.getLogger(__name__)

# Language codes for Tesseract Indic packs
TESSERACT_LANG_MAP = {
    "sa": "san",  # Sanskrit
    "ta": "tam",  # Tamil
    "te": "tel",  # Telugu
    "kn": "kan",  # Kannada
    "ml": "mal",  # Malayalam
    "hi": "hin",  # Hindi
    "en": "eng",  # English
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

        Delegates to the shared single-open implementation (TRACK-129).

        Args:
            pdf_path: Path to the PDF file.
            page_number: 0-based page number.

        Returns:
            Extracted text from OCR.
        """
        return self.extract_document_text(pdf_path, (page_number, page_number)).get(page_number, "")

    def extract_document_text(
        self,
        pdf_path: str | Path,
        page_range: tuple[int, int] | None = None,
    ) -> dict[int, str]:
        """Extract text from multiple pages using OCR.

        TRACK-129: the document is opened **once** and every requested page is
        rendered and OCR'd from that single handle. Previously this reopened the
        PDF per page, which on the 300-DPI OCR path is the slowest operation in
        the system.

        Args:
            pdf_path: Path to the PDF file.
            page_range: Optional (start, end) page numbers (0-based, inclusive).

        Returns:
            Dict mapping page number to extracted text.
        """
        results: dict[int, str] = {}

        with fitz.open(str(pdf_path)) as doc:
            total_pages = len(doc)

            start = page_range[0] if page_range else 0
            end = page_range[1] if page_range else total_pages - 1
            end = min(end, total_pages - 1)

            try:
                import pytesseract
                from PIL import Image
            except ImportError:
                # Preserve the previous degraded behaviour: without the OCR stack
                # the caller still gets one empty string per requested page, not {}.
                logger.error("pytesseract or Pillow not installed; OCR unavailable")
                return dict.fromkeys(range(start, end + 1), "")

            mat = fitz.Matrix(self.dpi / 72, self.dpi / 72)

            for page_num in range(start, end + 1):
                page = doc[page_num]
                pix = page.get_pixmap(matrix=mat)
                img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)

                text: str = pytesseract.image_to_string(
                    img,
                    lang=self.tesseract_lang_str,
                    config="--psm 6",  # Assume uniform block of text
                )

                logger.info(
                    "OCR extracted text",
                    extra={
                        "page": page_num,
                        "text_length": len(text),
                        "languages": self.tesseract_lang_str,
                    },
                )
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
