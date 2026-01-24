| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Bulk Import Orchestration & Ops Plan

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---


## Goal
Provide a resilient, observable orchestration for Krithi bulk import starting from CSV manifests, with full progress tracking, failure/retry controls, and an admin-facing dashboard—built using existing stack (Kotlin Ktor backend, React admin web, Postgres).

## Recommendation (Best-Fit Option)
- **Use Postgres-backed workflow orchestration with Ktor backend coroutines for async processing.**
  - **Kotlin (Ktor) backend**: Orchestrator service with coroutine-based background workers, state machine, APIs, and audit logging.
  - **React admin web**: Dashboard + controls (pause/resume, retry, drill-down).
  - **Postgres**: State persistence, task queue, and event log.
- **Avoid external engines** (Temporal/Airflow/Prefect) and **avoid mixing dev tooling** (`sangita-cli`) with production operations—keeps architecture clean and maintainable.

## Architecture Rationale

### Why NOT `sangita-cli` for Production Operations?

**`sangita-cli` is developer tooling, not production infrastructure:**

1. **Purpose**: Designed for local development workflows:
   - Database management (`db reset`, `db migrate`)
   - Development server orchestration (`dev --start-db`)
   - Testing (`test steel-thread`)
   - Git guardrails (`commit check`)
   - Network/mobile setup utilities

2. **Architectural Mismatch**:
   - CLI tools are meant for one-off operations, not long-running services
   - Would require subprocess spawning from backend (complex error handling, resource management)
   - No native integration with Ktor's coroutine ecosystem
   - Would mix dev tooling concerns with production business logic

3. **Operational Concerns**:
   - CLI processes are harder to monitor, scale, and restart
   - No built-in health checks or graceful shutdown
   - Would require separate deployment/runtime management
   - Breaks the service-oriented architecture pattern

### Why Ktor Backend with Coroutines?

**Kotlin coroutines provide native async processing within the existing backend:**

1. **Architectural Alignment**:
   - All existing services already use `suspend` functions
   - `WebScrapingService` and `ImportService` are already in the backend
   - Consistent error handling, logging, and audit patterns
   - Single codebase, single deployment unit

2. **Technical Advantages**:
   - **CoroutineScope**: Built-in structured concurrency for background jobs
   - **Channels/Flow**: Natural task queue patterns
   - **SupervisorJob**: Isolated failure handling per batch
   - **CoroutineContext**: Easy cancellation, timeouts, and resource management
   - **Integration**: Direct access to `DatabaseFactory`, `SangitaDal`, and existing services

3. **Operational Benefits**:
   - Single process to monitor and scale
   - Unified logging and metrics
   - Graceful shutdown via Ktor lifecycle hooks
   - Health checks via existing `/health` endpoint
   - No subprocess management overhead

### Architecture Overview

- **State model:** `import_batch` → `import_job` → `import_task_run` with immutable event log.
- **Execution paths:**
  - **Synchronous API kickoff:** Admin starts a batch via backend API; backend records batch + jobs, enqueues tasks.
  - **Async coroutine workers:** Background coroutine jobs within Ktor process pick up queued tasks from Postgres.
  - **Idempotent tasks:** Each task keyed by `batch_id + source_url`; retries safe via upsert semantics.
- **Storage:** Postgres tables (new) + existing `AUDIT_LOG` for mutations.
- **Observability:** Task/Batch statuses, retry counts, durations, error payloads, source URL lineage.

## Components

### Backend (Ktor) - Core Orchestration

**Service Layer:**
- `BulkImportOrchestrationService`: Main orchestrator managing batch lifecycle
- `BulkImportWorkerService`: Background coroutine workers for task processing
- Extends existing `ImportService` and `WebScrapingService` for actual work

**API Routes (`/v1/admin/bulk-import`):**
- `POST /batches` - Create and start a new import batch
- `GET /batches` - List all batches with filters (status, date range)
- `GET /batches/{id}` - Get batch details with tasks
- `POST /batches/{id}/pause` - Pause a running batch
- `POST /batches/{id}/resume` - Resume a paused batch
- `POST /batches/{id}/cancel` - Cancel a batch
- `POST /batches/{id}/retry` - Retry failed tasks in a batch
- `GET /batches/{id}/tasks` - List tasks with filters (status, source, error)
- `GET /batches/{id}/metrics` - Get batch metrics (success rate, durations, etc.)

**Background Processing:**
- **CoroutineScope**: Application-level scope for long-running jobs
- **Worker Pool**: Configurable number of concurrent workers per job type
- **Task Queue**: Postgres-backed queue with `status = 'pending'` as queue
- **Scheduler**: Lightweight coroutine-based ticker for stuck task detection
- **Rate Limiting**: Per-domain rate limiting for web scraping

**Lifecycle Management:**
- Startup: Initialize worker coroutines on application start
- Shutdown: Graceful cancellation via `ApplicationStopping` hook
- Health: Worker health checks via `/health` endpoint

### Frontend (React Admin Web)

**Dashboard Components:**
- Batch list: status chips, counts, progress bar (`processed/total`), age, owner
- Batch detail: timelines, job stages, task table with filters, error drawer, evidence links
- Controls: pause/resume, retry failed, cancel batch, export errors, trigger requeue of stuck
- Metrics: success rate, median/95p durations per stage, retry rate, top failure reasons

## Proposed Data Model (Postgres)
- `import_batch` — id, source_manifest, created_by, status (`pending/running/paused/succeeded/failed/cancelled`), stats (total/processed/failed), started_at, completed_at.
- `import_job` — id, batch_id, job_type (`manifest_ingest/scrape/enrich/entity_resolution/review_prep`), status, retry_count, payload (JSONB), result (JSONB), started_at, completed_at.
- `import_task_run` — id, job_id, krithi_key (slug/uuid), status, attempt, error (JSONB), duration_ms, source_url, checksum, evidence_path.
- `import_event` — id, ref_type (`batch/job/task`), ref_id, event_type, data (JSONB), created_at (append-only audit for orchestration).
- Indexes on `status`, `batch_id`, `job_type`, `krithi_key`, `created_at` for fast dashboards.

## Orchestration Flow (Happy Path)

1. **Batch Create** (Admin API):
   - `POST /v1/admin/bulk-import/batches` with CSV file or manifest path
   - Backend creates `import_batch` row with status `pending`
   - Returns batch ID immediately (async processing)

2. **Manifest Ingest Job** (Background Worker):
   - Worker coroutine picks up batch, changes status to `running`
   - Parses CSV file, validates entries
   - Creates `import_job` (type: `manifest_ingest`) and `import_task_run` rows (one per CSV entry)
   - Updates batch totals (`total_tasks`, `processed_tasks`)
   - Marks job as `succeeded`, enqueues scrape tasks

3. **Scrape/Enrich Jobs** (Background Workers):
   - Multiple worker coroutines poll for `pending` tasks with `job_type = 'scrape'`
   - Each worker:
     - Fetches URL using `WebScrapingService.scrapeKrithi()`
     - Parses and normalizes metadata
     - Creates staging record via `ImportService.submitImports()`
     - Updates task status to `succeeded` or `failed`
     - Updates batch progress counters

4. **Entity Resolution Job** (Background Worker):
   - Worker processes tasks with `job_type = 'entity_resolution'`
   - Calls existing resolution logic to map composer/raga/temple/etc.
   - Stores confidence scores in task `result` JSONB
   - Marks low-confidence tasks as `blocked` for manual review

5. **Review Prep Job** (Background Worker):
   - Worker processes tasks ready for review
   - Attaches evidence links, aggregates metadata
   - Marks tasks as `ready_for_review`
   - Updates batch status when all tasks complete

6. **Batch Completion**:
   - When all tasks reach terminal status (`succeeded`, `failed`, `blocked`, `cancelled`)
   - Batch status updated to `succeeded` (if any succeeded) or `failed` (if all failed)
   - Final metrics calculated and stored

## Failure, Retry, and Idempotency
- Task status set includes `pending/running/succeeded/failed/retryable/blocked/cancelled`.
- Automatic retry policy: exponential backoff with max attempts per job type; errors captured in `error` JSONB (code, message, http_status, url, stack excerpt).
- Idempotency keys: `batch_id + source_url` and `krithi_key`; writes use upsert semantics in staging to avoid dupes.
- Stuck detection: watchdog marks `running` tasks older than threshold as `retryable` with reason.
- Partial batch retry: API to retry only failed/blocked tasks or a filtered subset.

## Dashboard & Ops UX
- Batch list: status chips, counts, progress bar (`processed/total`), age, owner.
- Batch detail: timelines, job stages, task table with filters, error drawer, evidence links.
- Controls: pause/resume, retry failed, cancel batch, export errors, trigger requeue of stuck.
- Metrics: success rate, median/95p durations per stage, retry rate, top failure reasons.

## Security & Governance
- AuthZ: restrict orchestration APIs to admin role; log all actions to `AUDIT_LOG`.
- PII: avoid scraping/storing sensitive data; redact in error payloads.
- Backpressure: configurable concurrency per job type; rate-limit scraping by domain.

## Implementation Details

### Coroutine-Based Worker Pattern

```kotlin
class BulkImportWorkerService(
    private val dal: SangitaDal,
    private val importService: ImportService,
    private val webScrapingService: WebScrapingService
) {
    private val workerScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + 
        CoroutineName("BulkImportWorkers")
    )
    
    fun startWorkers(config: WorkerConfig) {
        repeat(config.scrapeWorkerCount) {
            workerScope.launch {
                processScrapeTasks()
            }
        }
        // ... other worker types
    }
    
    private suspend fun processScrapeTasks() {
        while (isActive) {
            val task = dal.bulkImport.getNextPendingTask("scrape")
            if (task != null) {
                try {
                    processScrapeTask(task)
                } catch (e: Exception) {
                    handleTaskFailure(task, e)
                }
            } else {
                delay(1000) // Poll interval
            }
        }
    }
}
```

### Integration with Existing Services

- **Reuses `WebScrapingService`**: No duplication of scraping logic
- **Reuses `ImportService`**: Standard import workflow for staging
- **Reuses `DatabaseFactory`**: Consistent database access patterns
- **Reuses `AuditLogService`**: All mutations logged automatically

### Configuration

```kotlin
data class BulkImportConfig(
    val scrapeWorkerCount: Int = 3,
    val maxConcurrentScrapes: Int = 5,
    val scrapeRateLimitPerDomain: Int = 10, // per minute
    val taskPollInterval: Duration = Duration.ofSeconds(1),
    val stuckTaskThreshold: Duration = Duration.ofMinutes(30),
    val maxRetries: Int = 3,
    val retryBackoffBase: Duration = Duration.ofSeconds(5)
)
```

## Rollout Plan (Incremental)

1. **Phase A (Foundation)**:
   - Create Postgres tables (`import_batch`, `import_job`, `import_task_run`, `import_event`)
   - Implement `BulkImportOrchestrationService` with batch lifecycle APIs
   - Implement CSV parsing and manifest ingestion worker
   - Basic dashboard: batch list + batch detail with task table

2. **Phase B (Scrape & Enrich)**:
   - Implement scrape worker coroutines with rate limiting
   - Integrate with existing `WebScrapingService` and `ImportService`
   - Dashboard: error drill-down, retry controls, progress indicators

3. **Phase C (Entity Resolution)**:
   - Implement entity resolution worker
   - Add confidence scoring and `blocked` state for low-confidence matches
   - Dashboard: confidence filters, resolution suggestions

4. **Phase D (Review Workflow)**:
   - Frontend review queue integration
   - Approvals push to main import pipeline via existing `ImportService.reviewImport()`
   - Export functionality for QA (CSV error reports)

5. **Phase E (Hardening)**:
   - Stuck task detector (coroutine-based scheduler)
   - SLO monitoring and alerts
   - Performance tuning (worker pool sizing, batch size optimization)
   - Load test on ~1,240 entries from CSV files

## Alternatives Considered

### 1. External Orchestrators (Temporal/Airflow/Prefect)
- **Pros**: Battle-tested, rich features (retries, scheduling, monitoring)
- **Cons**: 
  - Adds new infrastructure and operational overhead
  - Requires learning new tooling and patterns
  - Overkill for current scale (~1,240 entries)
  - Doesn't integrate with existing Ktor/Postgres stack
- **Decision**: ❌ Rejected - unnecessary complexity for current needs

### 2. Koog (Kotlin-based Pipeline Framework)
- **Pros**: Kotlin-native, good for data pipelines
- **Cons**:
  - Requires new deployment/runtime setup
  - Doesn't integrate with existing Ktor application lifecycle
  - Would require separate service management
- **Decision**: ❌ Rejected - doesn't align with monolithic backend architecture

### 3. Rust `sangita-cli` Workers
- **Pros**: Already exists, could reuse CSV parsing logic
- **Cons**:
  - **Architectural mismatch**: CLI is dev tooling, not production infrastructure
  - Would require subprocess spawning from backend (complex error handling)
  - No native integration with Ktor coroutines
  - Harder to monitor, scale, and manage lifecycle
  - Mixes dev tooling concerns with production business logic
- **Decision**: ❌ Rejected - violates separation of concerns

### 4. Pure SQL COPY
- **Pros**: Very fast for bulk data loading
- **Cons**:
  - Lacks orchestration, validation, and enrichment steps
  - No progress tracking or failure handling
  - Insufficient for multi-step workflow (CSV → scrape → enrich → review)
- **Decision**: ❌ Rejected - insufficient for requirements

### 5. Ktor Backend with Coroutines ✅ **SELECTED**
- **Pros**:
  - ✅ Native integration with existing backend architecture
  - ✅ Reuses existing services (`WebScrapingService`, `ImportService`)
  - ✅ Single codebase, single deployment unit
  - ✅ Built-in structured concurrency (CoroutineScope, SupervisorJob)
  - ✅ Easy cancellation, timeouts, and resource management
  - ✅ Unified logging, metrics, and health checks
  - ✅ Graceful shutdown via Ktor lifecycle hooks
- **Cons**:
  - Requires careful design of worker pool and task queue
  - Need to handle long-running jobs within HTTP server process
- **Decision**: ✅ **Selected** - best architectural fit

## Technical Considerations

### Worker Lifecycle Management

**Startup (in `App.kt`):**
```kotlin
val bulkImportService = BulkImportOrchestrationService(dal, importService, webScrapingService)
val workerService = BulkImportWorkerService(dal, importService, webScrapingService)

embeddedServer(Netty, host = env.host, port = env.port) {
    // ... existing configuration
    
    // Start background workers
    workerService.startWorkers(config)
    
    monitor.subscribe(ApplicationStopping) {
        logger.info("Shutting down bulk import workers")
        workerService.stopWorkers() // Graceful cancellation
        DatabaseFactory.close()
    }
}.start(wait = true)
```

### Task Queue Pattern

- **Queue**: Postgres table with `status = 'pending'` and `job_type` filters
- **Polling**: Workers use `SELECT ... FOR UPDATE SKIP LOCKED` to claim tasks atomically
- **Concurrency**: Configurable worker pool per job type
- **Backpressure**: Workers pause when queue is empty (polling interval)

### Error Handling & Retries

- **Automatic Retries**: Exponential backoff with max attempts per job type
- **Error Capture**: Full error context stored in `error` JSONB field
- **Manual Retry**: Admin can retry failed tasks via API
- **Stuck Detection**: Background scheduler marks `running` tasks older than threshold as `retryable`
- **Koog:** assessed earlier; good for pipelines but requires new deployment/runtime and doesn’t integrate with existing CLI/DB patterns.
- **Pure SQL COPY:** fast but lacks orchestration, validation, and enrichment steps; insufficient for this workflow.

## Actionable Next Steps

1. **Approve Architecture**: Review and approve this Ktor-based approach
2. **Database Schema**: Create migration for orchestration tables
3. **Phase A Implementation**:
   - Implement `BulkImportOrchestrationService` and `BulkImportWorkerService`
   - Create batch/job/task APIs
   - Implement CSV manifest ingestion worker
   - Build initial dashboard (batch list + detail)
4. **Testing**: Unit tests for services, integration tests for full workflow
5. **Documentation**: Update API contract and architecture docs
