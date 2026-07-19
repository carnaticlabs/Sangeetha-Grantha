"""TRACK-129: resource-lifecycle regressions.

These pin the *lifecycle* guarantees, not extraction output: a document handle
must not survive an exception raised mid-extraction, and the OCR path must open
the PDF once rather than once per page.
"""

from __future__ import annotations

import fitz
import pytest

from src.extractor import PdfExtractor
from src.ocr_fallback import OcrFallback


@pytest.fixture()
def three_page_pdf(tmp_path):
    path = tmp_path / "sample.pdf"
    doc = fitz.open()
    for i in range(3):
        page = doc.new_page()
        page.insert_text((72, 100), f"page {i} vAtApi gaNapatim bhajEham", fontsize=12)
    doc.save(str(path))
    doc.close()
    return path


def test_extract_document_closes_handle_when_a_page_raises(three_page_pdf, monkeypatch) -> None:
    """An exception mid-extraction must still close the document."""
    opened: list[fitz.Document] = []
    real_open = fitz.open

    def tracking_open(*args, **kwargs):
        doc = real_open(*args, **kwargs)
        opened.append(doc)
        return doc

    monkeypatch.setattr(fitz, "open", tracking_open)

    extractor = PdfExtractor()
    monkeypatch.setattr(extractor, "_extract_page", lambda *_a, **_k: (_ for _ in ()).throw(RuntimeError("boom")))

    with pytest.raises(RuntimeError, match="boom"):
        extractor.extract_document(three_page_pdf)

    assert opened, "expected the extractor to open the document"
    assert all(doc.is_closed for doc in opened), "document handle leaked on the exception path"


def test_is_text_extractable_closes_handle(three_page_pdf, monkeypatch) -> None:
    opened: list[fitz.Document] = []
    real_open = fitz.open

    def tracking_open(*args, **kwargs):
        doc = real_open(*args, **kwargs)
        opened.append(doc)
        return doc

    monkeypatch.setattr(fitz, "open", tracking_open)

    PdfExtractor().is_text_extractable(three_page_pdf)

    assert opened and all(doc.is_closed for doc in opened)


def _install_fake_ocr_stack(monkeypatch) -> None:
    """A working stand-in for pytesseract + PIL so the render path really runs.

    Without this the OCR stack is absent, both old and new code return early,
    and the open-count assertion below would not discriminate between them.
    """
    import sys
    from types import SimpleNamespace

    fake_tess = SimpleNamespace(image_to_string=lambda _img, lang=None, config=None: "ocr-text")
    fake_image_mod = SimpleNamespace(frombytes=lambda _mode, _size, _data: object())
    fake_pil = SimpleNamespace(Image=fake_image_mod)

    monkeypatch.setitem(sys.modules, "pytesseract", fake_tess)
    monkeypatch.setitem(sys.modules, "PIL", fake_pil)
    monkeypatch.setitem(sys.modules, "PIL.Image", fake_image_mod)


def test_ocr_opens_the_document_once_for_a_multi_page_range(three_page_pdf, monkeypatch) -> None:
    """TRACK-129: single-open OCR — three pages must not mean four opens."""
    _install_fake_ocr_stack(monkeypatch)

    opens: list[str] = []
    real_open = fitz.open

    def counting_open(*args, **kwargs):
        opens.append(str(args[0]) if args else "")
        return real_open(*args, **kwargs)

    monkeypatch.setattr(fitz, "open", counting_open)

    result = OcrFallback().extract_document_text(three_page_pdf, page_range=(0, 2))

    # Previously: one open to count pages, then one per page = 4.
    assert len(opens) == 1, f"expected a single fitz.open, got {len(opens)}"
    assert sorted(result) == [0, 1, 2], "every requested page must be represented"
    assert all(v == "ocr-text" for v in result.values())


def test_ocr_missing_stack_still_returns_one_entry_per_page(three_page_pdf, monkeypatch) -> None:
    """Degraded-path behaviour preserved: empty strings, not an empty dict."""
    monkeypatch.setitem(__import__("sys").modules, "pytesseract", None)

    result = OcrFallback().extract_document_text(three_page_pdf, page_range=(0, 1))

    assert result == {0: "", 1: ""}
