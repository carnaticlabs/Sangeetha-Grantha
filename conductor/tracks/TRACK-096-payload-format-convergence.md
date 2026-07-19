| Metadata | Value |
|:---|:---|
| **Status** | Ready — unblocked, cleanup pending |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |

> **2026-07-19 — unblocked.** This track was paused waiting for the legacy payloads to drain from
> the corpus. The TRACK-093 re-import has done that: **all 1,238 `imported_krithis.parsed_payload`
> rows are canonical (`CanonicalExtractionDto`), with zero legacy `ScrapedKrithiMetadata`.**
>
> ```sql
> SELECT CASE WHEN parsed_payload ? 'sections' THEN 'canonical'
>             WHEN parsed_payload ? 'rawLyrics' OR parsed_payload ? 'scrapedAt' THEN 'legacy'
>             ELSE 'other' END AS shape, COUNT(*)
> FROM imported_krithis GROUP BY 1;
> -- canonical | 1238
> ```
>
> The remaining work is the deletion cleanup: remove `ScrapedKrithiMetadata` and the dual-format
> fallback branches now that nothing produces or reads the old shape. Re-run the query above
> immediately before deleting — the guarantee is "no legacy rows *right now*", not a schema
> constraint, so a stray import between now and then would reintroduce one.

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

## Phase 4: Cleanup
- [x] Remove `IWebScraper`, `WebScrapingServiceImpl`, `DeterministicWebScraper`, and DI binding —
      already absent from the codebase (Phase 3's removal went all the way; verified 2026-07-11).
- [x] Migrate `StructuralVotingEngine` off `ScrapedSectionDto` (2026-07-11) — it now carries its own
      `VotedSection(type: RagaSectionDto, label)`. Landed on the domain `RagaSectionDto` enum rather
      than `CanonicalSectionType` deliberately: voting scores on the richer technical-section set
      (MUKTAYI/ETTUGADA/VILOMA/…) that `CanonicalSectionType` collapses to `OTHER`.
      `StructuralVotingProcessor` builds `VotedSection` straight from canonical extractions; the
      legacy `LyricVariantPersistenceService` fallback maps its `ScrapedSectionDto` at the call site.
      No active code path outside the deprecated fallback references the scraper section DTO now.
- [ ] Remove `ScrapedKrithiMetadata` fallback path from `LyricVariantPersistenceService` once no
      legacy payloads remain in DB — **blocked on the TRACK-093 re-import** (the cutover that leaves
      only canonical payloads). Until then the fallback + `ImportService` deity/temple parsing of
      legacy payloads must stay.
- [ ] Remove `ScrapedKrithiMetadata` and related DTOs (after the re-import).
- [ ] Remove dual-format detection code (after the re-import).

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
