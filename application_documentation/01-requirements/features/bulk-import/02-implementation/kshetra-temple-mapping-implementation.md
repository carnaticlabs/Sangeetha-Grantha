| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Kshetra & Temple Mapping – Implementation Summary

**Conductor:** [TRACK-029](../../../../../conductor/tracks/TRACK-029-bulk-import-kshetra-mapping.md) – Bulk Import - Kshetra & Deity Mapping

## 1. Purpose

Enhance Bulk Import to scrape and map Kshetra (Temple) and Deity details from Krithi source URLs (e.g. TempleNet links in Guru Guha posts). Cache scraped temple data, geocode locations, and use confidence thresholds when auto-creating Temple/Deity on import approval.

## 2. Categorization of Changes

| Category | Description |
|:---|:---|
| **Database** | `temple_source_cache` table and migration 21 |
| **DAL** | TempleSourceCacheTable, TempleSourceCacheRepository; TempleRepository updates; SangitaDal wiring (already committed with TRACK-031) |
| **API / Config** | ApiEnvironment (geo, Gemini rate-limit, temple confidence); GeocodingService, TempleScrapingService; AppModule, DalModule; NameNormalizationService; BulkImportWorkerConfig, RateLimiter; ImportService.reviewImport (temple/deity auto-create) |
| **Tests** | WebScrapingServiceTest, TestDatabaseFactory; TempleScrapingServiceTest (new) |
| **Frontend** | ImportReview.tsx (temple/deity candidates, overrides); BulkImport.tsx (UI polish, task breakdown); types.ts; index.tsx |
| **API models** | ImportRequests (overrides/exposure for temple/deity) |

## 3. Code Changes Summary (Retrospective)

### 3.1 Database

| File | Change |
|:---|:---|
| `database/migrations/21__create_temple_source_cache.sql` | **New.** Creates `temple_source_cache` (source_url, temple_name, deity_name, city, lat/long, geo fields, error, timestamps). Indexes on source_url, temple_name_normalized, deity_name. Trigger for updated_at. |

### 3.2 DAL

| File | Change |
|:---|:---|
| `modules/backend/dal/.../tables/TempleSourceCacheTable.kt` | **New.** Exposed table for temple_source_cache. |
| `modules/backend/dal/.../repositories/TempleSourceCacheRepository.kt` | **New.** findByUrl, save, and cache DTO. |
| `modules/backend/dal/.../repositories/TempleRepository.kt` | **Updated.** Additional lookup/helpers for temple resolution. |
| `modules/backend/dal/.../support/UuidSerializer.kt` | **New.** Serializer for UUID in DTOs if needed. |
| SangitaDal interface/impl | **Updated.** (Already committed in TRACK-031.) Exposes templeSourceCache, composerAliases; composers(composerAliases). |

### 3.3 API – Config & DI

| File | Change |
|:---|:---|
| `modules/backend/api/.../config/ApiEnvironment.kt` | **Updated.** Added geminiModelUrl, geminiMinIntervalMs, geoProvider, geoApiKey, templeAutoCreateConfidence; loader reads SG_GEMINI_*, SG_GEO_*, SG_TEMPLE_AUTO_CREATE_CONFIDENCE. |
| `modules/backend/api/.../di/AppModule.kt` | **Updated.** GeminiApiClient(geminiApiKey, geminiModelUrl, geminiMinIntervalMs); GeocodingService, TempleScrapingService; WebScrapingServiceImpl(get(), get()); ImportServiceImpl(..., get(), get()). |
| `modules/backend/api/.../di/DalModule.kt` | **Updated.** SangitaDalImpl() binding (or equivalent). |

### 3.4 API – Services

| File | Change |
|:---|:---|
| `modules/backend/api/.../services/GeocodingService.kt` | **New.** Geocoding for temple locations (e.g. OSM). |
| `modules/backend/api/.../services/TempleScrapingService.kt` | **New.** Cache-first scrape of temple URLs; uses GeocodingService. |
| `modules/backend/api/.../services/NameNormalizationService.kt` | **Updated.** Temple/deity normalization improvements. |
| `modules/backend/api/.../services/bulkimport/BulkImportWorkerConfig.kt` | **Updated.** Config for worker/temple domains. |
| `modules/backend/api/.../services/bulkimport/RateLimiter.kt` | **Updated.** Rate limiting for temple domains. |
| `modules/backend/api/.../models/ImportRequests.kt` | **Updated.** ImportReviewRequest/response exposure for temple/deity overrides/candidates. |

### 3.5 Tests

| File | Change |
|:---|:---|
| `modules/backend/api/.../services/WebScrapingServiceTest.kt` | **Updated.** Adjustments for WebScrapingService/TempleScrapingService. |
| `modules/backend/api/.../services/TempleScrapingServiceTest.kt` | **New.** Tests for TempleScrapingService (mocking). |
| `modules/backend/api/.../support/TestDatabaseFactory.kt` | **Updated.** Test DB setup / schema for new tables. |

### 3.6 Frontend

| File | Change |
|:---|:---|
| `modules/frontend/sangita-admin-web/src/pages/ImportReview.tsx` | **Updated.** Display temple/deity resolution candidates; override Temple/Deity in review UI. |
| `modules/frontend/sangita-admin-web/src/pages/BulkImport.tsx` | **Updated.** UI polish (manifest upload layout); task breakdown; basename for manifest display. |
| `modules/frontend/sangita-admin-web/src/types.ts` | **Updated.** Types for temple/deity candidates and overrides. |
| `modules/frontend/sangita-admin-web/src/index.tsx` | **Updated.** Minor (e.g. basename or routing). |

### 3.7 Conductor

| File | Change |
|:---|:---|
| `conductor/tracks/TRACK-029-bulk-import-kshetra-mapping.md` | **Existing.** Track file; progress log. |

## 4. Commit Reference

Use this file as the single documentation reference for the **Kshetra & Temple Mapping** commit:

```text
Ref: application_documentation/01-requirements/features/bulk-import/02-implementation/kshetra-temple-mapping-implementation.md
```

**Suggested commit scope (atomic):** All files listed in §3 (database migration 21, DAL temple cache + TempleRepository, API config/DI/services/models, tests, frontend pages/types/index). Exclude `config/development.env`.