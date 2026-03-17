| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-15 |
| **Author** | Sangeetha Grantha Team |

# Goal

Fix the lyric variant persistence pipeline so that krithis imported via the Python extraction worker (CanonicalExtractionDto format) have their lyrics, sections, and lyric sections saved to the database. Backfill the 67 already-approved krithis from TRACK-093 that were approved with zero lyrics persisted.

# Root Cause — Pipeline Format Schism

A classic **integration seam failure** where two pipeline stages each worked correctly in isolation but the handoff between them was never tested end-to-end.

| Format | Producer | Consumer | Status |
|:---|:---|:---|:---|
| `ScrapedKrithiMetadata` | Legacy Kotlin scraper (`WebScrapingService`) | `LyricVariantPersistenceService` | Only format it understood |
| `CanonicalExtractionDto` | Python extraction worker (`structure_parser.py`) | `ExtractionResultProcessor` | What's actually stored in `parsed_payload` |

The Python pipeline was built in TRACK-041+ to produce `CanonicalExtractionDto`, and `ExtractionResultProcessor.enrichExistingImport()` stores it in `imported_krithis.parsed_payload`. But `LyricVariantPersistenceService` was never updated to read it — it only knew `ScrapedKrithiMetadata`. The deserialization mismatch was swallowed by a `catch(e) { println(...) }` block, producing **zero user-visible errors** while silently skipping all lyric/section persistence.

### Why it was invisible

1. **Silent catch**: `catch (e: Exception) { println("Error processing scraped metadata: ${e.message}") }` — no logger, no API error, no status change
2. **No assertion at approval time**: The approval endpoint returns success regardless of whether lyrics were persisted
3. **No integration test**: Each component (extraction, enrichment, approval, persistence) was tested in isolation but never as a connected pipeline
4. **UI showed success**: The Curator Review page showed "Approved" status with no indication that lyrics were missing

# Implementation Plan

- [x] Add dual-format detection: try `CanonicalExtractionDto` first, fall back to `ScrapedKrithiMetadata`
- [x] Implement `persistFromCanonical()` method mapping canonical sections/variants to DB
- [x] Replace silent `println` error handler with `logger.error`
- [x] Use lenient JSON (`ignoreUnknownKeys = true`) for forward compatibility
- [x] Add admin API endpoint `POST /v1/admin/imports/backfill-lyrics` to re-run lyric persistence for approved krithis with empty variants
- [x] Run backfill against the 67 TRACK-093 krithis — 66/67 had variants persisted, 1 (`agastISvaraM`) has empty sections in extraction payload (needs re-extraction, not backfill)
- [x] Verify: 66 krithis with parsed_payload have `krithi_lyric_variants` populated (194 total sections)
- [x] 1 krithi (`agastISvaraM`) has `sections: []` in extraction — no lyric content to persist

# Files Changed

| File | Change |
|:---|:---|
| `modules/backend/api/.../services/LyricVariantPersistenceService.kt` | Dual-format detection, `persistFromCanonical()`, proper logging |
| `modules/backend/api/.../routes/ImportRoutes.kt` | Backfill endpoint (pending) |

# Notes

- The 23 Dikshitar krithis from guru-guha.blogspot.com have only OTHER sections due to non-standard HTML formatting — tracked separately as a source-adapter issue, not a persistence bug.
- The 2 krithis with no section markers at all (agastISvaraM, allakallOlamu) will get a single OTHER section with full text.

Ref: application_documentation/10-implementations/track-094-lyric-persistence-fix-backfill.md
