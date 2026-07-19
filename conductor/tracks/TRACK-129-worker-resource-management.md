| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.1.0 |
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

## Outcome (2026-07-19)

### Byte-identical output (the track's hard constraint)

Output was pinned **before** any change: a SHA-256 manifest over 6 HTML fixtures
through `HtmlTextExtractor` + `StructureParser`, plus a synthetic PDF through
`extract_document` (full and page-ranged), `is_text_extractable`, and
`is_garbled`. After all three source files changed, the manifest is **identical**.

One caveat worth recording: the first pin run reported a diff on the PDF entry.
That was a defect in the *harness*, not the code — the script regenerated the PDF
each run and PyMuPDF embeds a creation timestamp, so the file bytes (and the
checksum in the payload) changed every run. Confirmed by running the pin twice
against untouched code and seeing the same diff. The harness now generates the
PDF once and reuses it, and is verified deterministic across runs before being
trusted.

### Leak and single-open regressions

`tests/test_resource_management.py` (4 tests). Both new guarantees were proved
genuinely regressive by reverting `src/` to HEAD and re-running:

| Test | vs. HEAD | vs. this track |
|:---|:---|:---|
| handle closed when a page raises mid-extraction | **fail** | pass |
| OCR opens the document once for a 3-page range | **fail** | pass |
| `is_text_extractable` closes its handle | pass | pass |
| missing OCR stack still yields one entry per page | pass | pass |

The single-open test needed a working fake `pytesseract`/`PIL` to discriminate:
with the OCR stack absent, both old and new code return early and the open count
is 1 either way. The first version of that test passed against HEAD and was
therefore worthless; it was rewritten before being trusted.

### Timing

`extract_document_text` over 20 pages at 300 DPI, Tesseract stubbed to a constant
so the measurement isolates the document-open + render cost this track changed:

| | median of 5 |
|:---|:---|
| Before (reopen per page) | 48 ms |
| After (single open) | 23 ms |

~52% off the non-OCR overhead, i.e. ~1.25 ms/page of redundant opening removed.
Real Tesseract still dominates end-to-end; parallel OCR stays out of scope as the
track directs.

### `is_text_extractable` — decision: two passes stay, measured

Measured on a 50-page text PDF: the check costs **10.3 ms** against **28.6 ms**
for `extract_document` (~26% of combined). It was *not* folded, because the two
passes are not equivalent: the check scans **every** page with plain `get_text()`
to decide whether the document needs OCR, while `extract_document` scans only
`page_range` in `"dict"` mode. Folding would silently narrow the OCR routing
decision to the requested page range — a behaviour change the DoD forbids. The
reasoning and the numbers are recorded inline in the method docstring.

### Shared HTTP client

One lazily-created `httpx.Client` per strategy (`timeout=120.0`,
`follow_redirects=True` unchanged), released by `ExtractionWorker.close()`, which
`run()` now calls on shutdown. The W3 integration tests pass unmodified, which is
the evidence that respx still intercepts a long-lived client — the main risk of
this change.

### Verification

`ruff check` 0, `ruff format --check` clean (52 files), `mypy` 0,
`pytest` 220 unit + 18 integration passing.

### Follow-up (deliberately not done here)

`src/velthuis_decoder.py:277` opens a document and closes it only on the success
path, so it leaks the handle when `xref_stream_raw`/`zlib.decompress` raises. It
is a real instance of this track's bug class, but this DoD names only
`extractor.py` and `ocr_fallback.py`, so it was left alone rather than widening
scope. One-line fix when someone wants it.
