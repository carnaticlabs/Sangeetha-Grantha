| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-15 |
| **Author** | Sangeetha Grantha Team |

# Goal

Converge on `CanonicalExtractionDto` as the single payload format for `imported_krithis.parsed_payload` and deprecate `ScrapedKrithiMetadata`. Eliminate the format schism that caused TRACK-094's silent lyric persistence failure.

# Background — The Format Schism

The import pipeline has two competing payload formats that evolved independently across different tracks:

| Format | Introduced | Producer | Location |
|:---|:---|:---|:---|
| `ScrapedKrithiMetadata` | TRACK-001 (original bulk import) | Kotlin `WebScrapingService` | `backend/api/.../services/WebScrapingService.kt` |
| `CanonicalExtractionDto` | TRACK-041 (enhanced sourcing) | Python extraction worker | `shared/domain/.../import/CanonicalExtractionDto.kt` |

### How the schism formed

1. **TRACK-001–013**: Built the full import pipeline around `ScrapedKrithiMetadata` — Kotlin scrapes HTML, parses sections, stores in `parsed_payload`, `LyricVariantPersistenceService` reads it at approval time
2. **TRACK-041**: Introduced `CanonicalExtractionDto` as the "universal output contract" for all extraction adapters. Defined in shared domain module.
3. **TRACK-064**: Migrated extraction to Python worker. `ExtractionResultProcessor` stores `CanonicalExtractionDto` in `parsed_payload`.
4. **Gap**: `LyricVariantPersistenceService` was never updated to read `CanonicalExtractionDto`. The old format deserialization failed silently.

### Impact

- 67 krithis approved with zero lyrics/sections persisted (TRACK-093/094)
- Every future Python extraction will have the same failure unless the dual-format bridge (added in TRACK-094) remains

# Implementation Plan

## Phase 1: Audit & Document (current state)
- [x] Inventory all code paths that read/write `imported_krithis.parsed_payload` (8 files identified)
- [x] Document which paths produce which format (see "How the schism formed" above)

## Phase 2: Deprecate ScrapedKrithiMetadata
- [x] Mark `ScrapedKrithiMetadata`, `ScrapedSectionDto`, `ScrapedLyricVariantDto`, `ScrapedTempleDetails` as `@Deprecated` with `ReplaceWith` annotations
- [x] Add KDoc explaining migration path to `CanonicalExtractionDto`
- [x] `LyricVariantPersistenceService` dual-format bridge (TRACK-094) handles both formats correctly
- [x] Integration tests (TRACK-095) verify both canonical and legacy paths work

## Phase 3: Remove Dead Kotlin Scraper from Active Code Paths
- [x] Verified `ScrapeWorker` no longer calls `IWebScraper.scrapeKrithi()` — extraction delegated to Python worker since TRACK-064
- [x] Removed `IWebScraper` parameter from `ImportRoutes` (was unused, kept only for backward-compat)
- [x] Removed `IWebScraper` parameter from `ScrapeWorker` constructor
- [x] Removed `IWebScraper` parameter from `BulkImportWorkerServiceImpl` constructor
- [x] Removed `webScrapingService` from `AppModule` DI wiring for `BulkImportWorkerServiceImpl`
- [x] Deprecated `IWebScraper` interface with `@Deprecated` annotation
- [x] Updated `ImportRoutesTest` to remove mock `IWebScraper` usage
- [x] All tests pass, deprecation warnings confirm annotations working

## Phase 4: Cleanup (Future)
- [ ] Remove `IWebScraper`, `WebScrapingServiceImpl`, `DeterministicWebScraper`, and DI binding
- [ ] Migrate `StructuralVotingEngine` from `ScrapedSectionDto` to `CanonicalSectionDto`
- [ ] Remove `ScrapedKrithiMetadata` fallback path from `LyricVariantPersistenceService` once no legacy payloads remain in DB
- [ ] Remove `ScrapedKrithiMetadata` and related DTOs
- [ ] Remove dual-format detection code

# Decision Record

**Decision**: `CanonicalExtractionDto` is the single canonical format for extraction payloads going forward.

**Rationale**:
- It's in the shared domain module (accessible to all modules)
- It cleanly separates structure (sections) from text (lyric variants)
- It includes provenance metadata (source URL, tier, extraction method, checksum)
- It supports pre-normalized matching keys
- It's the output format of the Python extraction worker, which is the active extraction engine

**What NOT to do**:
- Do not create a third format
- Do not store raw HTML/text in `parsed_payload` — always store structured `CanonicalExtractionDto`
- Do not add fields to `ScrapedKrithiMetadata` — add them to `CanonicalExtractionDto` instead

# Related Tracks

- **TRACK-041**: Introduced `CanonicalExtractionDto`
- **TRACK-064**: Migrated extraction to Python (started producing canonical format)
- **TRACK-094**: Fixed `LyricVariantPersistenceService` to read both formats (bridge fix)
- **TRACK-095**: Integration tests to prevent format mismatches from going undetected

Ref: application_documentation/10-implementations/track-096-payload-format-convergence.md
