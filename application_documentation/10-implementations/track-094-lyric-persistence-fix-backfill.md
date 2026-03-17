| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-17 |
| **Author** | Sangeetha Grantha Team |

# Lyric Persistence Fix & Backfill

## Purpose

Fix the lyric variant persistence pipeline so that krithis imported via the Python extraction worker (`CanonicalExtractionDto` format) have their lyrics, sections, and lyric sections saved to the database. Backfill the 67 already-approved krithis from TRACK-093 that were approved with zero lyrics persisted.

## Root Cause

A classic integration seam failure: `ExtractionResultProcessor` stores `CanonicalExtractionDto` in `parsed_payload`, but `LyricVariantPersistenceService` only knew `ScrapedKrithiMetadata`. The deserialization mismatch was swallowed by a `catch(e) { println(...) }` block — no logger, no API error, no status change.

### Why It Was Invisible
1. Silent catch with `println` instead of logger
2. No assertion at approval time that lyrics were actually persisted
3. No integration test covering the full pipeline
4. UI showed "Approved" with no indication lyrics were missing

## Implementation Details

- Dual-format detection: try `CanonicalExtractionDto` first, fall back to `ScrapedKrithiMetadata`
- New `persistFromCanonical()` method mapping canonical sections/variants to DB
- Replaced silent `println` error handler with `logger.error`
- Lenient JSON (`ignoreUnknownKeys = true`) for forward compatibility
- Admin backfill endpoint: `POST /v1/admin/imports/backfill-lyrics`

## Code Changes

| File | Change |
|------|--------|
| `modules/backend/api/.../services/LyricVariantPersistenceService.kt` | Dual-format detection, `persistFromCanonical()`, proper logging |
| `modules/backend/api/.../routes/ImportRoutes.kt` | Backfill endpoint |

## Results

- 66/67 krithis backfilled successfully (194 total sections)
- 1 krithi (`agastISvaraM`) had empty sections in extraction payload — needs re-extraction, not backfill
- 2 krithis (`agastISvaraM`, `allakallOlamu`) had no section markers — assigned single OTHER section with full text
- 23 Dikshitar krithis from guru-guha.blogspot.com have only OTHER sections due to non-standard HTML (tracked in TRACK-097)

Ref: application_documentation/10-implementations/track-094-lyric-persistence-fix-backfill.md
