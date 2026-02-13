from src.config import ExtractorConfig
from src.extractor import DocumentContent, PageContent
from src.html_extractor import ExtractedHtmlContent
from src.metadata_parser import KrithiMetadata
from src.schema import ExtractionMethod
from src.worker import ExtractionWorker


def _build_worker() -> ExtractionWorker:
    return ExtractionWorker(ExtractorConfig())


def test_force_ocr_for_garbled_devanagari_text() -> None:
    worker = _build_worker()
    text = "अ" * 40 + ("\uFFFD" * 30)
    document = DocumentContent(
        pages=[PageContent(page_number=0, text=text)],
        total_pages=1,
        checksum="x",
        source_path="dummy.pdf",
    )
    assert worker._should_force_ocr_for_garbled_devanagari(document) is True


def test_do_not_force_ocr_for_clean_latin_text() -> None:
    worker = _build_worker()
    text = "akhilandesvari raksha mam agama sampradaya nipune"
    document = DocumentContent(
        pages=[PageContent(page_number=0, text=text)],
        total_pages=1,
        checksum="x",
        source_path="dummy.pdf",
    )
    assert worker._should_force_ocr_for_garbled_devanagari(document) is False


def test_force_ocr_for_globally_garbled_text_even_without_devanagari_signal() -> None:
    worker = _build_worker()
    text = ("A" * 300) + ("\uFFFD" * 300)
    document = DocumentContent(
        pages=[PageContent(page_number=0, text=text)],
        total_pages=1,
        checksum="x",
        source_path="dummy.pdf",
    )
    assert worker._should_force_ocr_for_garbled_devanagari(document) is True


def test_extract_pdf_ocr_emits_per_page_results(tmp_path) -> None:
    worker = _build_worker()
    task = type("Task", (), {})()
    task.source_url = "https://example.com/mdskt-A-series.pdf"
    task.source_name = "fixture"
    task.source_tier = 5
    task.request_payload = {"composerHint": "Muttuswami Dikshitar"}

    pdf_path = tmp_path / "fixture.pdf"
    pdf_path.write_bytes(b"dummy-pdf")

    page_texts = {
        0: "अखिलान्डेश्वरि रक्ष माम\nराग : जुझावन्ति\nताल : आदि",
        1: "अखिलान्डेश्वरो रक्षतु\nराग : कर्नाटक शुद्धसावेरी\nताल : रूपक",
    }

    worker.ocr_fallback.extract_document_text = lambda *_args, **_kwargs: page_texts
    worker.transliterator.transliterate = lambda text, _from, _to: f"{text}-iast"

    results = worker._extract_pdf_ocr(task, pdf_path, page_range=None)

    assert len(results) == 2
    assert [r.page_range for r in results] == ["1", "2"]
    assert all(r.extraction_method == ExtractionMethod.PDF_OCR for r in results)


def test_extract_html_includes_metadata_boundaries(tmp_path) -> None:
    worker = _build_worker()
    task = type("Task", (), {})()
    task.source_url = "https://example.com/akhilandesvari"
    task.source_name = "fixture"
    task.source_tier = 5
    task.request_payload = {"composerHint": "Muttuswami Dikshitar"}

    html_path = tmp_path / "fixture.html"
    html_path.write_text("<html><body>fixture</body></html>", encoding="utf-8")
    worker._download_source = lambda *_args, **_kwargs: html_path

    extracted_text = """
Pallavi
akhilandesvari raksha mam

Charanam
siva sankari jagadambike

Meaning
This prose block should not be inside lyric sections.
"""
    worker.html_extractor.extract = lambda *_args, **_kwargs: ExtractedHtmlContent(
        text=extracted_text,
        title="akhilandesvari raksha mam",
    )
    worker.metadata_parser.parse = lambda *_args, **_kwargs: KrithiMetadata(
        title="akhilandesvari raksha mam",
        raga="Dwijavanti",
        tala="Adi",
        composer="Muttuswami Dikshitar",
    )

    results = worker._extract_html(task)

    assert len(results) == 1
    extraction = results[0]
    assert len(extraction.metadata_boundaries) == 1
    assert extraction.metadata_boundaries[0].label == "MEANING"
    assert extraction.lyric_variants
    assert all(
        "prose block" not in section.text.lower()
        for section in extraction.lyric_variants[0].sections
    )


def test_extract_html_splits_multiscript_variants(tmp_path) -> None:
    worker = _build_worker()
    task = type("Task", (), {})()
    task.source_url = "https://example.com/multiscript"
    task.source_name = "fixture"
    task.source_tier = 5
    task.request_payload = {"composerHint": "Muttuswami Dikshitar"}

    html_path = tmp_path / "fixture.html"
    html_path.write_text("<html><body>fixture</body></html>", encoding="utf-8")
    worker._download_source = lambda *_args, **_kwargs: html_path

    extracted_text = """
English
Pallavi
akhilandesvari raksha mam

Devanagari
पल्लवि
अखिलाण्डेश्वरि रक्ष माम्

Notes
metadata block should not be lyrics
"""
    worker.html_extractor.extract = lambda *_args, **_kwargs: ExtractedHtmlContent(
        text=extracted_text,
        title="akhilandesvari",
    )
    worker.metadata_parser.parse = lambda *_args, **_kwargs: KrithiMetadata(
        title="akhilandesvari",
        raga="Dwijavanti",
        tala="Adi",
        composer="Muttuswami Dikshitar",
    )

    results = worker._extract_html(task)

    assert len(results) == 1
    extraction = results[0]
    scripts = sorted(variant.script for variant in extraction.lyric_variants)
    assert scripts == ["devanagari", "latin"]
    assert len(extraction.metadata_boundaries) == 1
    assert extraction.metadata_boundaries[0].label == "NOTES"
    assert all(
        "metadata block" not in section.text.lower()
        for variant in extraction.lyric_variants
        for section in variant.sections
    )
