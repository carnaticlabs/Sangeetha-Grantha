| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-15 |
| **Author** | Sangeetha Grantha Team |

# Goal

Add end-to-end testing and runtime validation for the import pipeline so that silent failures (like TRACK-094's format mismatch) are caught immediately rather than discovered after manual DB inspection.

# Motivation

The TRACK-093 bulk import of 67 krithis appeared successful at every UI touchpoint (upload, extraction, approval) but produced empty shells — zero lyrics, sections, or lyric sections persisted. The failure was invisible because:
1. The deserialization error was swallowed by a `catch { println }` block
2. No integration test covers the full CSV → extract → approve → lyrics-in-DB path
3. No validation endpoint reports whether an approved krithi has complete data

# Implementation Plan

## Integration Test
- [x] Create `LyricVariantPersistenceServiceTest.kt` in `api/src/test/kotlin/.../services/`
- [x] Test: full pipeline (submit → extract → enrich → approve) with `CanonicalExtractionDto` → assert `krithi_sections` (3), `krithi_lyric_variants` (2: SA + TE), `krithi_lyric_sections` (3 per variant) all populated
- [x] Test: legacy `ScrapedKrithiMetadata` payload → assert lyric variant persisted (backward compatibility)
- [x] Test: malformed payload → assert `logger.error` logged, approval succeeds without crash

## Validation Endpoint
- [x] Add `GET /v1/admin/imports/:id/validation` — returns section/variant/lyric counts, payload format, and issue list
- [x] Add `GET /v1/admin/imports/validation/summary` — batch-level health check (total, withPayload, withVariants, needsBackfill)
- [x] Response DTOs: `ImportValidationResponse`, `ValidationSummaryResponse` (both `@Serializable`)

## Curator UI Enhancement
- [ ] Show validation status (green/yellow/red) on Curator Review page per import row (deferred — backend APIs ready)

# Files to Create/Modify

| File | Change |
|:---|:---|
| `modules/backend/api/src/test/kotlin/.../services/LyricVariantPersistenceServiceTest.kt` | New: 3 integration tests (canonical, legacy, malformed) |
| `modules/backend/api/src/test/kotlin/.../services/ExtractionResultProcessorTest.kt` | Updated tests to reflect existing payload enrichment |
| `modules/backend/api/.../routes/ImportRoutes.kt` | Validation endpoints |
| `modules/backend/api/.../services/ImportValidationService.kt` | New: validation logic |
| `modules/frontend/sangita-admin-web/src/pages/CuratorReviewPage.tsx` | Validation indicators |

Ref: application_documentation/10-implementations/track-095-import-pipeline-test-validation.md
