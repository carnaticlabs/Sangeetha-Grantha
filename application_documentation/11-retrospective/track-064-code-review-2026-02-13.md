| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.1 |
| **Last Updated** | 2026-02-19 |
| **Author** | Claude Code Review |

# TRACK-064 Code Review & Validation (2026-02-13)

## Purpose
Independent review of all code changes made under TRACK-064 (Unified Extraction Engine Migration) and the key-collision milestone, assessing alignment with the track plan, code quality, and identifying any course corrections needed.

---

## Overall Verdict: Good direction, but Phase 1 vertical slice has a critical gap

The architecture is sound — intelligence is moving to Python, Kotlin is becoming a pure ingestor, and the CLI harness is well-built. However, there are items that need attention before moving forward.

---

## Critical Items (fix before next phase)

### 1. HTML Import Flow is not wired end-to-end
- `ImportService.kt` enqueues HTML extraction tasks correctly.
- Python worker extracts and marks tasks `DONE`.
- **But nothing consumes the `DONE`/`INGESTED` result to actually create the Krithi and link it back to the import record.**
- This means Phase 1's checkbox "Successfully flow one Blogspot URL through Python and see it created as a Krithi in the database" is only partially met — extraction works, but Krithi creation from that extraction is missing the final wiring.

### 2. Collision detection SQL may not match `CanonicalExtraction` schema
- `test.rs` queries `raw_extraction::jsonb->>'raga'` as a scalar string.
- But the Python `CanonicalExtraction` stores `ragas` as a JSON **array** (`ragas: [CanonicalRaga]`).
- This likely explains the `metadataMissingRows=4` in the 200-row scan.
- Fix: extract via `raw_extraction::jsonb->'ragas'->0->>'name'`.

### 3. Missing E2E test for the full HTML path
- Unit tests exist for import/review flows, but no integration test covers: HTML submit -> Python extraction -> Krithi creation.

---

## High Priority (address soon)

| Item | Detail |
|---|---|
| **Missing composite index** | `krithi_source_evidence(krithi_id, source_url)` — used for duplicate checks but has no index |
| **Batch counter race condition** | `BulkImportRepository` counter increments use client-side delta math; concurrent task creation could drift counters |
| **Python tests are scaffolded but empty** | `test_html_extractor.py`, `test_metadata_parser.py`, `test_worker.py` exist but need implementation |

---

## What's Working Well

- **Migration quality is excellent** — Migration 31 (HTML format support) and 32 (confidence normalization) are well-written and defensive.
- **Python extraction pipeline** — `html_extractor.py`, `metadata_parser.py`, and `worker.py` are clean, with good diacritic normalization and fallback strategies.
- **CLI test harness** — The `dikshitar-key-collision` scenario with `--max-rows`, `--fail-on-collision`, and non-blocking collision flagging is well-designed.
- **Key-collision hypothesis is validated** — 4 collision groups in 200 rows is a manageable rate; the identity key `(first-10-chars, raga, tala)` is discriminative enough for production.

---

## Track Alignment

| Phase | Status | Notes |
|---|---|---|
| **Phase 0**: E2E Harness | **Done** | Solid foundation |
| **Phase 1**: Vertical Slice | **~70%** | Python side works; Kotlin consumption of results is the gap |
| **Phase 2**: Heuristic Consolidation | Not started | `KrithiStructureParser.kt` (100+ Indic regexes) still in Kotlin |
| **Phase 3**: Identity & Enrichment | Not started | Gemini still in Kotlin |
| **Phase 4**: Orchestration & Cleanup | Not started | Expected |

---

## Recommended Next Steps (priority order)

1. **Close the Phase 1 gap** — Wire the path from `INGESTED` extraction result -> Krithi creation -> import record update. This is the critical missing link.
2. **Fix collision detection SQL** to handle the `ragas` array format.
3. **Run the full-file collision scan** (as planned in the handover) once the SQL fix is in place — the `metadataMissingRows` count should drop.
4. **Add the composite index** migration for source evidence.
5. **Flesh out Python tests** before starting Phase 2.

---

## Detailed Findings

### Code Quality Assessment

| Area | Grade | Notes |
|---|---|---|
| Code Quality | B+ | Solid structure, good practices, but incomplete flows |
| Track Alignment | 70% | Phase 1 incomplete, Phases 2-4 not started |
| Test Coverage | C+ | Good unit coverage, missing E2E coverage |
| Security | B | No major vulnerabilities, but needs hardening |
| Migration Quality | A | Well-written, defensive |

### Security Notes
- All SQL queries use parameterized statements (no injection risk).
- Download cache uses SHA256 hashing for filenames (no path traversal).
- **URL validation is missing** in `ImportService.submitImports` — no check that submitted URLs are HTTP/HTTPS or domain-filtered.
- **No rate limiting** on extraction submission — queue could be flooded.

### Additional Medium-Priority Items
- Worker retry logic lacks exponential backoff; transient network errors burn through all attempts quickly.
- No warning log when lyric truncation happens in `KrithiRepository.createLyricVariant`.
- `compose.yaml` volume mount for Python code (Phase 4 item) not yet added.

---

## Files Reviewed
- `tools/sangita-cli/src/commands/test.rs`
- `tools/sangita-cli/README.md`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/ImportService.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/ExtractionResultProcessor.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/KrithiCreationFromExtractionService.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/scraping/KrithiStructureParser.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/ImportRoutes.kt`
- `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/ImportServiceTest.kt`
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/BulkImportRepository.kt`
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiRepository.kt`
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/SourceEvidenceRepository.kt`
- `tools/krithi-extract-enrich-worker/src/worker.py`
- `tools/krithi-extract-enrich-worker/src/metadata_parser.py`
- `tools/krithi-extract-enrich-worker/src/html_extractor.py`
- `tools/krithi-extract-enrich-worker/tests/test_html_extractor.py`
- `tools/krithi-extract-enrich-worker/tests/test_metadata_parser.py`
- `tools/krithi-extract-enrich-worker/tests/test_worker.py`
- `database/migrations/31__extraction_queue_html_support.sql`
- `database/migrations/32__normalize_entity_resolution_cache_confidence.sql`
- `modules/frontend/sangita-admin-web/src/pages/ImportReview.tsx`
