| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-13 |
| **Author** | Sangita Grantha Architect |

# TRACK-064: Unified Extraction Engine Migration — Implementation Summary

## Purpose

Consolidates all composition extraction logic (HTML and PDF) into the Python extraction service, eliminating the "Heuristic Split" between Kotlin and Python. The Kotlin backend becomes a "Pure Ingestor" while Python owns all parsing intelligence.

## Scope

Covers Phase 0 (E2E Harness), Phase 1 (HTML Vertical Slice), and Phase 2 (Heuristic Consolidation) of the Unified Extraction Engine migration.

## Code Changes Summary

### Database Migrations (SQL)

| File | Change |
|:---|:---|
| `database/migrations/31__extraction_queue_html_support.sql` | Add `HTML` format support to `extraction_queue` table |
| `database/migrations/32__normalize_entity_resolution_cache_confidence.sql` | Normalize confidence column in entity resolution cache |
| `database/migrations/33__fix_krithi_lyric_variants_lyrics_index.sql` | Fix `krithi_lyric_variants` lyrics index |
| `database/migrations/34__repair_krithi_lyric_variants_lyrics_index.sql` | Repair index (follow-up fix) |
| `database/migrations/35__add_krithi_source_evidence_krithi_source_url_index.sql` | Composite index on `(krithi_id, source_url)` for lookup performance |

### Kotlin Backend — API Services

| File | Change |
|:---|:---|
| `ImportRoutes.kt` | Route extraction submissions through queue instead of direct scraping |
| `ExtractionResultProcessor.kt` | Consume DONE/INGESTED extraction results, create/update Krithi records, link import records |
| `ImportService.kt` | Submit PENDING HTML tasks to extraction queue; handle ingest-consumption gap |
| `KrithiCreationFromExtractionService.kt` | Refactored to support canonical extraction payload shape |
| `KrithiStructureParser.kt` | Minimal changes; runtime parsing now delegated to Python |

### Kotlin Backend — DAL Repositories

| File | Change |
|:---|:---|
| `BulkImportRepository.kt` | Support extraction queue task tracking |
| `ExtractionQueueRepository.kt` | New query methods for HTML task lifecycle |
| `ImportRepository.kt` | Import record linkage after extraction ingestion |
| `KrithiRepository.kt` | Broader candidate search for fuzzy matching, compressed title matching |
| `SourceEvidenceRepository.kt` | Source evidence creation for extraction pipeline |

### Kotlin Backend — Tests

| File | Change |
|:---|:---|
| `ImportServiceTest.kt` | Tests for URL submission and extraction queue integration |
| `ExtractionResultProcessorTest.kt` | **[NEW]** E2E integration test for extraction result ingestion flow |
| `ImportRoutesTest.kt` | **[NEW]** Route-level tests for import endpoints |

### Shared Domain (KMP)

| File | Change |
|:---|:---|
| `CanonicalExtractionDto.kt` | Added `metadataBoundaries`, extended `LyricVariantDto` for multi-script support |

### Python Extraction Service

| File | Change |
|:---|:---|
| `html_extractor.py` | **[NEW]** BeautifulSoup4-based HTML extraction, ported from `HtmlTextExtractor.kt` |
| `structure_parser.py` | Full heuristic consolidation: 100+ Indic script regex rules, metadata hard-stops, multi-script variant splitting |
| `metadata_parser.py` | Enhanced metadata extraction from HTML titles and headings |
| `schema.py` | Extended canonical extraction schema with `metadataBoundaries` and variant fields |
| `worker.py` | Routes HTML/PDF/PDF-OCR flows through Python structure parser |
| `cli.py` | CLI extensions for extraction testing and validation |
| `pyproject.toml` | Added `beautifulsoup4` dependency |
| `tests/test_html_extractor.py` | **[NEW]** HTML extraction tests |
| `tests/test_metadata_parser.py` | **[NEW]** Metadata parser tests |
| `tests/test_worker.py` | **[NEW]** Worker integration tests |
| `tests/test_schema.py` | Schema validation tests |
| `tests/test_structure_parser.py` | Fixture-backed parity tests for Kotlin regex port |
| `tests/fixtures/structure_parser/` | **[NEW]** Test fixtures for multi-script and Tamil header parity checks |

### Rust CLI — E2E Harness & Commands

| File | Change |
|:---|:---|
| `src/commands/test.rs` | **Major**: E2E extraction harness (`test extraction-e2e`) with queue lifecycle validation, metadata coverage assertions, scenario support (`blogspot-html`, `pdf-smoke`) |
| `src/commands/extraction.rs` | Extraction command improvements |
| `src/commands/db.rs` | Database command refinements |
| `src/commands/dev.rs` | Dev workflow improvements (`--keep-services`, `--skip-migrations`) |
| `src/commands/commit.rs` | Commit command updates |
| `src/commands/docs.rs` | Docs command updates |
| `src/commands/net.rs` | Network command updates |
| `src/commands/setup.rs` | Setup command updates |
| `src/database/manager.rs` | Database manager improvements for extraction E2E |
| `src/services.rs` | Service orchestration for extraction pipeline |
| `fixtures/extraction/` | **[NEW]** PDF test fixtures for E2E scenarios |
| `README.md` | Updated CLI documentation |

### Frontend

| File | Change |
|:---|:---|
| `ImportReview.tsx` | Extraction status display in import review UI |

### Documentation & Architecture

| File | Change |
|:---|:---|
| `005-centralized-extraction-service.md` | **[NEW]** ADR for centralized extraction service architecture |
| `extraction-consolidation-analysis-2026-02.md` | **[NEW]** Analysis of extraction consolidation strategy |
| `track-064-code-review-2026-02-13.md` | **[NEW]** Phase 1 code review findings |
| `track-064-extraction-migration-handoff-2026-02-12.md` | **[NEW]** Handoff document for migration |
| `track-064-key-collision-handover-2026-02-13.md` | **[NEW]** Key collision metadata fix handover |
| `remediation-technical-retrospective-2026-02-12.md` | Updated with TRACK-064 migration context |

## Validation Results

- Python tests: `97 passed` (`uv run python -m pytest`)
- Rust CLI: `cargo check` passed
- Kotlin backend tests: `ExtractionResultProcessorTest` + `ImportServiceTest` passed
- E2E: `blogspot-html` scenario passed (`minSections=11`, `minVariants=1`)
- E2E: `pdf-smoke` scenario passed (`minSections=3`, `minVariants=1`)
- Metadata coverage: `total_rows=200`, `keyed_rows=200`, `metadata_missing_rows=0`

## Commit Reference

```
Ref: application_documentation/10-implementations/track-064-unified-extraction-engine-implementation.md
```
