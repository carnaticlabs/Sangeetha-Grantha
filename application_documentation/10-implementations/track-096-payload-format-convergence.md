| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-17 |
| **Author** | Sangeetha Grantha Team |

# Payload Format Convergence

## Purpose

Converge on `CanonicalExtractionDto` as the single payload format for `imported_krithis.parsed_payload` and deprecate `ScrapedKrithiMetadata`. Eliminate the format schism that caused TRACK-094's silent lyric persistence failure.

## Background — The Format Schism

Two competing payload formats evolved independently:
- **`ScrapedKrithiMetadata`** (TRACK-001): Produced by Kotlin `WebScrapingService`
- **`CanonicalExtractionDto`** (TRACK-041): Produced by Python extraction worker, defined in shared domain module

The gap: `LyricVariantPersistenceService` was never updated to read `CanonicalExtractionDto`, causing silent failures.

## Implementation Details

### Phase 1: Audit & Document
- Inventoried all 8 code paths that read/write `imported_krithis.parsed_payload`
- Documented which paths produce which format

### Phase 2: Deprecate ScrapedKrithiMetadata
- Marked `ScrapedKrithiMetadata`, `ScrapedSectionDto`, `ScrapedLyricVariantDto`, `ScrapedTempleDetails` as `@Deprecated` with `ReplaceWith` annotations
- Added KDoc explaining migration path
- Dual-format bridge (TRACK-094) handles both formats correctly

### Phase 3: Remove Dead Kotlin Scraper from Active Code Paths
- Removed `IWebScraper` parameter from `ImportRoutes`, `ScrapeWorker`, and `BulkImportWorkerServiceImpl`
- Removed `webScrapingService` from `AppModule` DI wiring
- Deprecated `IWebScraper` interface
- Updated `ImportRoutesTest` to remove mock `IWebScraper` usage

### Phase 4: Cleanup (Future)
- Remove `IWebScraper`, `WebScrapingServiceImpl`, `DeterministicWebScraper`
- Migrate `StructuralVotingEngine` from `ScrapedSectionDto` to `CanonicalSectionDto`
- Remove legacy fallback paths once no legacy payloads remain in DB

## Decision Record

**Decision**: `CanonicalExtractionDto` is the single canonical format going forward.

**Rationale**: Lives in shared domain module, cleanly separates structure from text, includes provenance metadata, supports pre-normalized matching keys, and is the output format of the active Python extraction engine.

## Code Changes

| File | Change |
|------|--------|
| `modules/backend/api/.../services/WebScrapingService.kt` | `@Deprecated` annotations |
| `modules/backend/api/.../routes/ImportRoutes.kt` | Removed `IWebScraper` parameter |
| `modules/backend/api/.../bulkimport/workers/ScrapeWorker.kt` | Removed `IWebScraper` parameter |
| `modules/backend/api/.../bulkimport/BulkImportWorkerServiceImpl.kt` | Removed `webScrapingService` from constructor |
| `modules/backend/api/.../di/AppModule.kt` | Removed DI wiring for `IWebScraper` in worker |
| `modules/backend/api/src/test/.../routes/ImportRoutesTest.kt` | Removed mock `IWebScraper` |

Ref: application_documentation/10-implementations/track-096-payload-format-convergence.md
