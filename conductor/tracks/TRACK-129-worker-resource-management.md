| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |

# TRACK-129: Worker Resource Management — fitz Context Managers, Single-Open OCR, Shared HTTP Client

**Goal:** Close the resource-lifecycle gaps in the extraction pipeline: PyMuPDF documents opened without try/finally leak handles on mid-extraction exceptions; the OCR path reopens the PDF once per page at 300 DPI (the slowest path in the system); every download constructs a fresh `httpx.Client`, discarding connection pooling.

**Origin:** Python best-practices evaluation, 2026-07-19 (item 4, "Resource management" + "HTTP").

## Definition of Done

* Every `fitz.open(...)` in `src/extractor.py` and `src/ocr_fallback.py` uses `with fitz.open(...) as doc:` (or try/finally) — no leaked document handles on any exception path.
* `OcrFallback.extract_document_text` opens the document once and renders/OCRs all requested pages from that single handle; `extract_page_text` remains for single-page callers but delegates to the shared implementation.
* Extraction strategies share one long-lived `httpx.Client` (per strategy or injected per worker) with explicit timeout configuration; client lifecycle is tied to worker shutdown.
* `PdfExtractor.is_text_extractable` no longer does a full second text pass when `extract_document` runs immediately after — either fold the check into one open, or document why two passes stay (measured, not assumed).
* No behaviour change to extraction output — golden/regression fixtures byte-identical.

## Inputs

* `@tools/krithi-extract-enrich-worker/src/extractor.py`
* `@tools/krithi-extract-enrich-worker/src/ocr_fallback.py`
* `@tools/krithi-extract-enrich-worker/src/extraction_strategies.py` (`_download_source`)
* `@tools/krithi-extract-enrich-worker/tests/test_worker.py` (OCR path tests)

## Out of Scope

* Parallel OCR (`concurrent.futures`) — note timing before/after; parallelism only if a real batch shows it's warranted, and then as its own track.
* Retry policy on downloads (record as follow-up; keep current no-retry semantics).
* The worker concurrency knobs (TRACK-131 decides implement-vs-delete).

## Steps

1. Convert all `fitz.open` sites to context managers; add a test that an exception mid-page doesn't leak (e.g. via `fitz.TOOLS.store_size` or a mock asserting `close`).
2. Restructure `OcrFallback` to single-open; time a multi-page scanned fixture before/after and record the numbers in this file.
3. Introduce the shared `httpx.Client` (module- or instance-scoped), close it on worker shutdown; keep `timeout=120.0, follow_redirects=True` semantics.
4. Decide and implement the `is_text_extractable` single-pass question; document the decision inline.

## Deliverables

* Modified `extractor.py`, `ocr_fallback.py`, `extraction_strategies.py`, `worker.py` (client lifecycle).
* New leak-regression test; timing note in this track file.

## Verification

* `uv run pytest` fully green, including CAT-B regression fixtures unchanged.
* `uv run ruff check` / `uv run mypy` clean on touched files.
* W3 integration tests (respx-intercepted downloads) still pass — proves the shared client still routes through httpx transports respx can stub.
