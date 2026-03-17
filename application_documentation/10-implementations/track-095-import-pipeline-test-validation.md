| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-17 |
| **Author** | Sangeetha Grantha Team |

# Import Pipeline Test & Validation

## Purpose

Add end-to-end testing and runtime validation for the import pipeline so that silent failures (like TRACK-094's format mismatch) are caught immediately rather than discovered after manual DB inspection.

## Implementation Details

### Integration Tests
- `LyricVariantPersistenceServiceTest.kt` with 3 test cases:
  - Full pipeline with `CanonicalExtractionDto` → asserts `krithi_sections` (3), `krithi_lyric_variants` (2: SA + TE), `krithi_lyric_sections` (3 per variant)
  - Legacy `ScrapedKrithiMetadata` payload → backward compatibility
  - Malformed payload → `logger.error` logged, approval succeeds without crash

### Validation Endpoints
- `GET /v1/admin/imports/:id/validation` — per-import health check (section/variant/lyric counts, payload format, issue list)
- `GET /v1/admin/imports/validation/summary` — batch-level health check (total, withPayload, withVariants, needsBackfill)
- Response DTOs: `ImportValidationResponse`, `ValidationSummaryResponse` (both `@Serializable`)

### Curator UI Enhancement (Deferred)
- Validation status indicators on Curator Review page — backend APIs ready, UI deferred

## Code Changes

| File | Change |
|------|--------|
| `modules/backend/api/src/test/kotlin/.../services/LyricVariantPersistenceServiceTest.kt` | New: 3 integration tests |
| `modules/backend/api/src/test/kotlin/.../services/ExtractionResultProcessorTest.kt` | Updated tests to reflect existing payload enrichment |
| `modules/backend/api/.../services/ImportValidationService.kt` | New: validation logic (section/variant/lyric counts, payload format detection) |
| `modules/backend/api/.../routes/ImportRoutes.kt` | Validation endpoints |

## Results

- Silent format mismatches now caught by integration tests
- Admin can query validation status per-import and batch-level via API

Ref: application_documentation/10-implementations/track-095-import-pipeline-test-validation.md
