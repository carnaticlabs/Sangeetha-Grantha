# Track: Bulk Import of Krithis
**ID:** TRACK-001
**Status:** In Progress
**Owner:** Sangita Grantha Architect
**Created:** 2026-01-20
**Updated:** 2026-01-20

## Goal
Build a resilient, observable orchestration system for bulk importing Krithis from CSV manifests (~1,240+ entries across 3 CSV files) with full progress tracking, failure/retry controls, and an admin-facing dashboard. The system will scrape URLs from CSV entries, extract metadata, and stage imports for review.

## Context
- **CSV Files:** Located in `database/for_import/`:
  - `Dikshitar-Krithi-For-Import.csv` (~484 entries)
  - `Thyagaraja-Krithi-For-Import.csv` (~700+ entries)
  - `Syama-Sastri-Krithi-For-Import.csv` (~60+ entries)
- **CSV Structure:** `Krithi` (Title), `Raga`, `Hyperlink` (Source URL)
- **Architecture:** Ktor backend with coroutine-based background workers, Postgres-backed orchestration, React admin dashboard
- **Reference:** See `/application_documentation/01-requirements/features/bulk-import/bulk-import-orchestration-ops-plan-goose.md` for full architecture

## Architecture Overview
- **Orchestration:** Postgres-backed workflow with Ktor coroutine workers (NOT `sangita-cli` - it's dev tooling)
- **State Model:** `import_batch` → `import_job` → `import_task_run` with immutable event log
- **Execution:** Admin starts batch via API → Background workers process tasks → Dashboard shows progress
- **Services:** Reuses existing `WebScrapingService` and `ImportService`

## Implementation Plan (Phased)

### Phase A: Foundation (Current)
- [x] Update TRACK-001 with orchestration plan
- [x] Create database migration for orchestration tables
- [x] Create DAL layer (tables, repositories)
- [x] Implement `BulkImportOrchestrationService` (batch/job/task lifecycle + admin controls) — initial
- [x] Implement `BulkImportWorkerService` (background coroutine workers + polling) — initial
- [x] Create API routes (`/v1/admin/bulk-import/...`) for batch management + monitoring — initial
- [x] Implement CSV manifest ingestion worker (parse CSV and create scrape tasks) — initial
- [x] Integrate workers into `App.kt` lifecycle (start/stop workers on app start/stop)
- [ ] Hardening: idempotency keys, rate limiting, stuck-task watchdog, richer error taxonomy

### Phase B: Scrape & Enrich (Next)
- [ ] Implement scrape worker coroutines with rate limiting
- [ ] Integrate with `WebScrapingService` and `ImportService`
- [ ] Dashboard: error drill-down, retry controls, progress indicators

### Phase C: Entity Resolution
- [ ] Implement entity resolution worker
- [ ] Add confidence scoring and `blocked` state
- [ ] Dashboard: confidence filters, resolution suggestions

### Phase D: Review Workflow
- [ ] Frontend review queue integration
- [ ] Approvals push to main import pipeline
- [ ] Export functionality for QA

### Phase E: Hardening
- [ ] Stuck task detector
- [ ] SLO monitoring and alerts
- [ ] Performance tuning
- [ ] Load test on ~1,240 entries

## Progress Log

### 2026-01-20: Phase A - Foundation (In Progress)
- ✅ Updated TRACK-001 to align with orchestration plan
- ✅ Created database migration `10__bulk-import-orchestration.sql` with:
  - `import_batch` table for batch tracking
  - `import_job` table for job-level tracking
  - `import_task_run` table for individual task execution
  - `import_event` table for immutable event log
  - Enum types: `batch_status_enum`, `job_type_enum`, `task_status_enum`
- ✅ Created DAL layer:
  - Added enums: `BatchStatus`, `JobType`, `TaskStatus` to `DbEnums.kt`
  - Added table definitions to `CoreTables.kt`
  - Created `BulkImportRepository` with full CRUD operations
  - Added DTOs to `ImportDtos.kt` (shared domain)
  - Added DTO mappers to `DtoMappers.kt`
  - Integrated repository into `SangitaDal`

### 2026-01-20: Phase A - Orchestration + Workers (Started)
- ✅ Added admin APIs: `modules/backend/api/.../routes/BulkImportRoutes.kt`
  - `POST /v1/admin/bulk-import/batches` (create batch)
  - `GET /v1/admin/bulk-import/batches` (list)
  - `GET /v1/admin/bulk-import/batches/{id}` (detail)
  - `GET /v1/admin/bulk-import/batches/{id}/jobs|tasks|events`
  - `POST /v1/admin/bulk-import/batches/{id}/pause|resume|cancel|retry`
- ✅ Implemented orchestration service: `BulkImportOrchestrationService`
  - Creates batch + initial MANIFEST_INGEST job + task
  - Implements pause/resume/cancel/retry and event/audit logging
- ✅ Implemented worker service: `BulkImportWorkerService`
  - Manifest ingest worker: parses CSV and enqueues SCRAPE tasks
  - Scrape worker: scrapes URL, creates imports via existing `ImportService`
  - Batch counters updated; batch auto-completes when all tasks processed
- ✅ Wired bulk import routing + worker lifecycle into `App.kt` and `configureRouting(...)`
- ✅ Backend compiles: `./gradlew :modules:backend:api:compileKotlin`

- 🔄 Next (In Progress): hardening + operational polish (idempotency, retries/backoff, rate limiting, stuck-task detection) and then frontend dashboard wiring.

## Technical Details
- **Backend:** Kotlin/Ktor with coroutines
- **Database:** PostgreSQL with Exposed ORM
- **Frontend:** React TypeScript (admin dashboard)
- **Migration Tool:** Rust `sangita-cli` (for DB migrations only, NOT for production operations)
- **Reference Docs:** 
  - Architecture: `bulk-import-orchestration-ops-plan-goose.md`
  - Strategy: `bulk-import/01-strategy/csv-import-strategy.md`
