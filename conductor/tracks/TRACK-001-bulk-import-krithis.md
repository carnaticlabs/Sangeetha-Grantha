# Track: Bulk Import - Backend Orchestration
**ID:** TRACK-001
**Status:** In Progress
**Owner:** Sangita Grantha Architect
**Created:** 2026-01-20
**Updated:** 2026-01-21

## Goal
Build the backend orchestration engine for bulk importing Krithis. This includes the database schema, Ktor services, coroutine workers, and management APIs.
(UI/UX is handled in TRACK-003 and TRACK-004).

## Context
- **CSV Files:** Located in `database/for_import/`
- **Architecture:** Ktor backend with coroutine-based background workers, Postgres-backed orchestration.
- **Reference:** `/application_documentation/01-requirements/features/bulk-import/bulk-import-orchestration-ops-plan-goose.md`

## Implementation Plan (Phased)

### Phase A: Foundation (Current)
- [x] Update TRACK-001 with orchestration plan
- [x] Create database migration for orchestration tables
- [x] Create DAL layer (tables, repositories)
- [x] Implement `BulkImportOrchestrationService` (batch/job/task lifecycle + admin controls)
- [x] Implement `BulkImportWorkerService` (background coroutine workers + polling)
- [x] Create API routes (`/v1/admin/bulk-import/...`) for batch management
- [x] Implement CSV manifest ingestion worker
- [x] Integrate workers into `App.kt` lifecycle
- [x] Hardening: idempotency keys, rate limiting, stuck-task watchdog, richer error taxonomy

### Phase B: Scrape & Enrich Workers
- [x] Implement scrape worker coroutines with rate limiting
- [x] Integrate with `WebScrapingService` and `ImportService`
- [x] Implement enrichment logic (Scraping is the enrichment)

### Phase C: Entity Resolution Workers
- [x] Implement entity resolution worker (Levenshtein based)
- [x] Add confidence scoring and `blocked` state logic (Basic confidence implemented)
- [x] Implement APIs for retrieving resolution candidates (Saved to `resolution_data`)

### Phase D: Review Workflow APIs
- [x] Implement APIs for fetching tasks ready for review (`GET /imports?status=PENDING`)
- [x] Implement APIs for approving/rejecting tasks (`POST /imports/{id}/review`)
- [ ] Implement APIs for manual entity resolution (overrides) (Currently partial support)

### Phase E: Hardening & Performance
- [ ] Stuck task detector
- [ ] SLO monitoring and alerts
- [ ] Performance tuning
- [ ] Load test on ~1,240 entries

## Progress Log
*(Previous logs preserved)*

### 2026-01-21: Entity Resolution & Workers
- ✅ Implemented `EntityResolutionService` with fuzzy matching (Levenshtein) for Composers, Ragas, Talas.
- ✅ Added `resolution_data` JSONB column to `imported_krithis`.
- ✅ Updated `BulkImportWorkerService` to include `EntityResolution` worker loop.
- ✅ Implemented orchestration transition: Scrape -> Entity Resolution.
- ✅ Wired up services in `App.kt`.

### 2026-01-21: Track Restructuring
- 🔄 Moved UI/UX scope to `TRACK-003` and `TRACK-004`.
- 🔄 Renamed track to "Bulk Import - Backend Orchestration".

### 2026-01-21: Hardening Complete
- ✅ Added `idempotency_key` column + unique index via `11__bulk-import-hardening.sql`
- ✅ Worker improvements: dedupe manifest rows, reuse scrape jobs per batch, task idempotency on insert
- ✅ Scrape loop: rate limiting (per-domain/global), retryable errors until max attempts, terminal failures increment counters
- ✅ Watchdog loop: marks stuck RUNNING tasks as `RETRYABLE` with audit events