| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-29 |
| **Author** | Sangeetha Grantha Team |

# TRACK-029: Bulk Import - Kshetra & Deity Mapping

## 1. Goal
Enhance the Bulk Import capability to scrape and map Kshetra (Temple) and Deity details from Krithi URLs, specifically targeting TempleNet links embedded in Guru Guha blog posts.

## 2. Context
- **Problem:** Currently, bulk import captures basic Krithi details but misses rich context about Kshetras and Deities available in linked pages.
- **Source:** Guru Guha blog posts often link to TempleNet pages (e.g., `http://templenet.com/Tamilnadu/s207.html`).
- **Target:** Extract Temple name, Deity, Location (Lat/Long), and brief description to populate `Temple` and `Deity` entities.

## 3. Implementation Plan

### Phase 1: Data Model & Config (Foundational)
- [x] **Database Schema**
    - [x] Create migration for `temple_source_cache` table.
    - [x] Add `TempleSourceCacheTable` definition.
    - [x] Create `TempleSourceCacheRepository`.
- [x] **Configuration**
    - [x] Update `ApiEnvironment` with Geocoding keys and thresholds (`SG_GEO_PROVIDER`, `SG_TEMPLE_AUTO_CREATE_CONFIDENCE`).

### Phase 2: Scrape & Cache Enrichment (Backend)
- [x] **Refactor Scraping**
    - [x] Extract `TempleScrapingService` from `WebScrapingService`.
    - [x] Implement Cache-First strategy (Check DB -> Scrape -> Geocode -> Save).
    - [x] Update `RateLimiter` for temple domains.
- [x] **Geocoding Fallback**
    - [x] Implement `GeocodingService`.
    - [x] Integrate into `TempleScrapingService` fallback flow.

### Phase 3: Resolution & Workflow (Backend)
- [x] **Enhanced Resolution**
    - [x] Update `NameNormalizationService` for temples/deities.
    - [x] Hardening `EntityResolutionService` with cache support.
- [x] **Approval Logic**
    - [x] Update `ImportService.reviewImport` to use `confidence >= THRESHOLD` logic for auto-creation.
    - [x] Ensure audit logging for all creations.

### Phase 4: Frontend & API (Admin UI)
- [x] **API Updates**
    - [x] Expose resolution candidates in `ImportReviewRequest/Response`.
- [x] **UI Enhancements**
    - [x] Update `ImportReview.tsx` to display Temple/Deity candidates.
    - [x] Add ability to override Temple/Deity in the UI.

### Phase 5: Verification & Cleanup
- [ ] **Testing**
    - [ ] Integration tests for `TempleScrapingService` (mocking external calls).
    - [ ] E2E test for the full flow.
- [ ] **Documentation**
    - [ ] Update architectural diagrams (`erd.md`, `flows.md`).

## 4. Progress Log
- **2026-01-29**: Track created. Initial research in progress.
- **2026-01-29**: Implementation Plan approved. Starting Execution.
- **2026-01-29**: Initial nested scraping prototyped in `WebScrapingService` (Proof of Concept).
- **2026-01-29**: Received architectural direction to implement dedicated Cache + Geocoding. Refactoring plan.
- **2026-01-29**: Completed Phases 1, 2, 3, and 4.
    - Implemented `TempleScrapingService`, `GeocodingService`, `TempleSourceCache`.
    - Updated `NameNormalizationService`, `EntityResolutionService`, `ImportService` (Auto-Creation).
    - Updated `ImportReview.tsx` and `types.ts` for Admin UI.
    - Verified compilation (Build Success).
- **2026-01-30**: Addressed Gemini API 429 "Resource Exhausted" errors.
    - Implemented exponential backoff retry mechanism in `GeminiApiClient` (5 retries, 2s initial delay).
    - Refactored Gemini API configuration: moved model URL to environment variables (`SG_GEMINI_MODEL_URL`).
    - Verified compilation.

