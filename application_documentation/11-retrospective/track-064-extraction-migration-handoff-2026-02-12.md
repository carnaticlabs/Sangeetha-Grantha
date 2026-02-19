| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-19 |
| **Author** | Sangita Grantha Team |

# TRACK-064 Unified Extraction Migration Handoff (2026-02-12)

---

## Purpose
This document captures the implementation status, validation runs, detailed non-convergence analysis, and next actions for the extraction unification work (HTML + PDF + queue/worker/ingest).

Use this as the starting context for the next session.

## Follow-up (2026-02-13)
- Continued session handover: [TRACK-064 Key-Collision Milestone Handover (2026-02-13)](track-064-key-collision-handover-2026-02-13.md)

## Snapshot (as of February 12, 2026)
- Phase-1 HTML extraction slice is wired across DB + Kotlin import flow + Python worker.
- Route-level import scrape behavior is moved to queue-first HTML extraction (`202 Accepted` + enqueue side effect).
- OCR fallback for Sanskrit/garbled Devanagari PDFs is implemented in worker heuristics.
- Metadata parsing for blog-format inline `rAgaM ... tALaM ...` lines is implemented.
- Matching logic has been tightened (including better raga-aware tie breaking), but specific A-series items still do not converge.
- A reproducible focused 3-row convergence run completed with `notConverged=3`.

---

## Implemented Fixes

## 1) Database / migrations
- `database/migrations/31__extraction_queue_html_support.sql`
  - Extends `extraction_queue.source_format` check constraint to include `HTML`.
- `database/migrations/32__normalize_entity_resolution_cache_confidence.sql`
  - Normalizes `entity_resolution_cache.confidence` to integer range `0..100`.
- `database/migrations/33__fix_krithi_lyric_variants_lyrics_index.sql`
  - Replaces unsafe text btree index with `md5(lyrics)` index to avoid large-row index failures.
- `database/migrations/34__repair_krithi_lyric_variants_lyrics_index.sql`
  - Reinforces safe index shape (`idx_krithi_lyric_variants_lyrics_md5`).

## 2) Kotlin backend (queue-first import + matching)
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/ImportService.kt`
  - Import flow can enqueue HTML extraction tasks when source is URL-based and inline payload is absent.
  - Audit action added: `ENQUEUE_HTML_EXTRACTION_FROM_IMPORT`.
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/ImportRoutes.kt`
  - `/v1/admin/imports/scrape` now enqueues via import service instead of inline scrape path.
- `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/routes/ImportRoutesTest.kt`
  - Tests assert `202 Accepted`, HTML task enqueue, idempotency, and no direct web-scraper call.
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/ExtractionResultProcessor.kt`
  - Candidate merge includes metadata + exact + near-title.
  - Scoring includes compressed title ratio, metadata match, known-raga preference, evidence count.
  - Raga-aware tie-break behavior strengthened.
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiRepository.kt`
  - Duplicate and near-title candidate queries support space-insensitive matching and bounded prefix fallback.

## 3) Python extractor (HTML + OCR fallback + metadata parsing)
- `tools/krithi-extract-enrich-worker/src/html_extractor.py`
  - Added structured HTML extractor (boilerplate removal, preferred selectors, link preservation).
- `tools/krithi-extract-enrich-worker/src/worker.py`
  - Handles `source_format=HTML`.
  - Adds `_should_force_ocr_for_garbled_devanagari(...)` heuristic and routes PDFs to OCR when text is corrupted.
  - Maps extraction methods correctly (`HTML_JSOUP`, `PDF_PYMUPDF`, `PDF_OCR`).
- `tools/krithi-extract-enrich-worker/src/metadata_parser.py`
  - Title normalization strips known blog prefixes (for example `Guru Guha Vaibhavam: ...`).
  - Handles inline metadata forms such as `rAgaM kumudakriyA - tALaM - rUpakaM`.
  - Supports using first line metadata when `title_hint` is present.

## 4) CLI E2E and regression capability
- `tools/sangita-cli/src/commands/test.rs`
  - Scenario support for:
    - `blogspot-html`
    - `akhila-three-source`
    - `dikshitar-a-series`
  - Includes local fixture HTTP serving for PDF files and convergence analysis helpers.
- `tools/sangita-cli/README.md`
  - Updated examples for extraction E2E and multi-source scenarios.

## 5) New tests
- `tools/krithi-extract-enrich-worker/tests/test_html_extractor.py`
- `tools/krithi-extract-enrich-worker/tests/test_metadata_parser.py`
- `tools/krithi-extract-enrich-worker/tests/test_worker.py`

Recent worker/parser verification:
- `uv run pytest tests/test_metadata_parser.py tests/test_worker.py` -> passed (`9 passed`).

---

## Validation Runs Performed

## Blogspot HTML scenario (direct CLI)
Ran multiple `extraction-e2e --scenario blogspot-html` tasks for specific URLs; each reached `INGESTED` and created evidence.

Examples:
- `abhyaambaam-bhaktim` -> task `7dac8668-cf87-49f2-a932-04a1d1c94622`
- `abhayaambikaayaah-anyam` -> task `f640a360-e59e-4ee6-89e2-597868c62403`
- `abhayaambikaayai` -> task `a7b9d470-f3fa-4715-87e2-12a3402176be`

## Focused 3-row convergence run (latest)
Command:
```bash
cargo run --manifest-path /Users/seshadri/project/sangeetha-grantha/tools/sangita-cli/Cargo.toml -- \
  test extraction-e2e \
  --scenario dikshitar-a-series \
  --csv-path /tmp/dikshitar-a-next-batch.csv \
  --english-pdf-path /Users/seshadri/Downloads/mdeng-A-series.pdf \
  --sanskrit-pdf-path /Users/seshadri/Downloads/mdskt-A-series.pdf \
  --skip-migrations \
  --skip-extraction-start \
  --timeout-seconds 300
```

Run summary:
- `totalRows=3`
- `successfulRows=3`
- `failedRows=0`
- `converged=0`
- `partial=0`
- `notConverged=3`
- `englishPdfKrithis=24`
- `sanskritPdfKrithis=25`

PDF baseline task ids from this run:
- English: `45b8e077-aabf-4f4f-bfb7-73754654d4d2`
- Sanskrit: `ab325d75-8568-431f-8030-ea2e017b731b`

HTML task ids from this run:
- `abhyaambaam-bhaktim` -> `4afe50c0-7f21-4320-855f-02a6c294279a`
- `abhayaambikaayaah-anyam` -> `b4f7f1c7-178e-4b44-a878-f31e89c60c95`
- `abhayaambikaayai` -> `95c45693-0cca-4344-8117-fa29a1546375`

---

## Detailed Non-Convergence Analysis (Current Batch)

### Requested pairs
- `http://guru-guha.blogspot.com/2007/08/dikshitar-kriti-abhyaambaam-bhaktim.html`
  - English page 11, Sanskrit page 13
- `http://guru-guha.blogspot.com/2007/08/dikshitar-kriti-abhayaambikaayaah-anyam.html`
  - English page 12, Sanskrit page 14
- `http://guru-guha.blogspot.com/2007/08/dikshitar-kriti-abhayaambikaayai.html`
  - English page 13, Sanskrit page 16

### Row 1: `abhyaambaam-bhaktim`
- HTML mapped to krithi: `d30e1f33-b8ac-45cc-88e2-519388986ea1` (`Abhyaambaayaam Bhaktim`)
- PDF evidence:
  - English p11 -> `64508e83-5fc3-433f-8bd3-721ba0fff7f6`
  - Sanskrit p13 -> `64508e83-5fc3-433f-8bd3-721ba0fff7f6`
- Result: **no convergence** (HTML id != PDF id)

### Row 2: `abhayaambikaayaah-anyam`
- HTML mapped to krithi: `553e819e-20e3-4fcc-aa02-f29c1f1f7629`
- PDF evidence:
  - English p12 -> `2429a37c-fec9-4ec0-8f93-5c93aefd9206`
  - Sanskrit p14 -> `2429a37c-fec9-4ec0-8f93-5c93aefd9206`
- Result: **no convergence** (HTML id != PDF id)

### Row 3: `abhayaambikaayai`
- HTML mapped to krithi: `b021a5e5-6371-4c35-88d0-597c41574318`
- PDF payload contains expected titles:
  - English index/page 13 title present in payload: `abhayambikayai`
  - Sanskrit index/page 16 title present in payload: `abhayambikayai` (Devanagari)
- But evidence rows for those exact pages were not persisted in `krithi_source_evidence` in this run.
- Result: **no convergence** (and page-evidence gap remains)

---

## Additional Findings

## Evidence insertion/page-index gaps still observed
For run tag `1770913249`:
- English missing evidence pages: `10`, `13`
- Sanskrit missing evidence page: `16`

This matches the repeated symptom: `result_count = 26`, but evidence rows are lower (`24/25`).

## Metadata improvements validated
- Inline raga/tala extraction for patterns like
  - `rAgaM kumudakriyA - tALaM - rUpakaM`
  - now parsed and normalized as `Kumudakriya / Rupakam`.
- Blog title normalization strips known prefixes and reduces noisy title mismatches.

## OCR fallback behavior validated
- Sanskrit baseline extraction used OCR method in run logs (`method=PDF_OCR`).
- Garbled-Devanagari detection logic is active in worker path.

---

## Likely Root Causes (Prioritized)

## 1) PDF payload-to-evidence persistence mismatch (highest impact)
- Some PDF payload items are not becoming evidence rows.
- This directly blocks convergence for rows where the missing pages carry key variants.

## 2) Existing duplicate/canonical split across previously created Krithis
- HTML rows are currently mapping to already-created HTML-side IDs, while PDF rows map to a different canonical line.
- Needs stronger merge/reconciliation strategy during match or post-ingest dedup.

## 3) Remaining title-variant normalization gaps
- Variants like `... yai` vs `... yA` and short-truncated HTML titles (`... aSva`) can still steer matches toward existing non-canonical entries.

---

## Recommended Next Session Plan

1. Fix PDF evidence persistence gap first.
   - Trace from `extraction_queue.result_payload` -> `ExtractionResultProcessor` loop -> `krithi_source_evidence` inserts.
   - Add deterministic test asserting `evidence_count == result_count` for a controlled multi-page fixture.

2. Re-run focused 3-row batch immediately after step 1.
   - Same command and same page mappings.
   - Validate whether row 3 can converge once English p13 + Sanskrit p16 evidence is actually present.

3. Add targeted match reconciliation for known A-series duplicates.
   - Include raga-aware preference and script-variant affinity in tie-break when title scores are close.
   - Avoid creating/retaining split IDs for same composition across sources.

4. Keep the 3-row CSV scenario as a permanent regression test (fast signal) plus Akhila 3-source as broader guardrail.

---

## Useful Queries/Checks (for next session)

1. Verify missing pages for a run:
```sql
WITH eng AS (
  SELECT page_range::int AS p
  FROM krithi_source_evidence
  WHERE source_url = '<eng-url-with-run>'
    AND page_range ~ '^[0-9]+$'
),
skt AS (
  SELECT page_range::int AS p
  FROM krithi_source_evidence
  WHERE source_url = '<skt-url-with-run>'
    AND page_range ~ '^[0-9]+$'
)
SELECT 'eng' AS src, gs AS missing_page
FROM generate_series(1,26) gs
WHERE gs NOT IN (SELECT p FROM eng)
UNION ALL
SELECT 'skt' AS src, gs AS missing_page
FROM generate_series(1,26) gs
WHERE gs NOT IN (SELECT p FROM skt)
ORDER BY src, missing_page;
```

2. Map HTML source URL to current krithi id:
```sql
SELECT source_url, krithi_id, extracted_at, raw_extraction->>'title' AS extracted_title
FROM krithi_source_evidence
WHERE source_url LIKE 'http://guru-guha.blogspot.com/2007/08/dikshitar-kriti-abh%'
ORDER BY extracted_at DESC;
```

3. Inspect PDF payload titles by index:
```sql
WITH task AS (
  SELECT result_payload
  FROM extraction_queue
  WHERE id = '<pdf-task-id>'::uuid
)
SELECT ordinality AS idx, elem->>'title' AS title
FROM task, jsonb_array_elements(task.result_payload) WITH ORDINALITY AS t(elem, ordinality)
ORDER BY ordinality;
```

---

## Files Most Relevant for Next Debug Pass
- `database/migrations/31__extraction_queue_html_support.sql`
- `database/migrations/33__fix_krithi_lyric_variants_lyrics_index.sql`
- `database/migrations/34__repair_krithi_lyric_variants_lyrics_index.sql`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/ImportService.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/ImportRoutes.kt`
- `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/routes/ImportRoutesTest.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/ExtractionResultProcessor.kt`
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiRepository.kt`
- `tools/krithi-extract-enrich-worker/src/worker.py`
- `tools/krithi-extract-enrich-worker/src/metadata_parser.py`
- `tools/krithi-extract-enrich-worker/src/html_extractor.py`
- `tools/krithi-extract-enrich-worker/tests/test_metadata_parser.py`
- `tools/krithi-extract-enrich-worker/tests/test_worker.py`
- `tools/sangita-cli/src/commands/test.rs`
- `tools/sangita-cli/README.md`
