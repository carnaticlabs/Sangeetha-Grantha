| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangita Grantha Architect |

# Bulk Import Query Optimization Plan

## 1. Analysis of Current State
A review of the application logs (`sangita_logs.txt`) reveals a high frequency of database queries originating from the background worker service (`BulkImportWorkerService`).

### Key Findings
- **High Idle Load:** Even when no batches are running, the system executes approximately **8 SELECT ... FOR UPDATE queries per second**.
- **Redundant Polling:** Multiple worker threads (6 total) poll independently.
- **Lock Contention Risk:** The usage of `FOR UPDATE` on frequent polls can lead to lock contention, although `SKIP LOCKED` (if used/implied) mitigates blocking.

### The "Chatty" Queries
The following queries appear repeatedly (every ~750ms per worker):

1.  **Entity Resolution Claim:**
    ```sql
    SELECT ... FROM import_task_run 
    WHERE status IN ('pending', 'retryable') 
    AND job_type = 'entity_resolution' 
    AND batch_status = 'running' 
    LIMIT 1 FOR UPDATE
    ```
2.  **Scrape Claim:**
    ```sql
    SELECT ... FROM import_task_run 
    WHERE status IN ('pending', 'retryable') 
    AND job_type = 'scrape' 
    LIMIT 1 FOR UPDATE
    ```
3.  **Manifest Claim:**
    ```sql
    SELECT ... FROM import_task_run 
    WHERE job_type = 'manifest_ingest' 
    LIMIT 1 FOR UPDATE
    ```

## 2. Optimization Strategy

To address this, we will implement a multi-layered optimization strategy, tracked under **TRACK-006**.

### Strategy A: Adaptive Polling (Exponential Backoff)
**Impact:** Drastically reduces idle queries.
**Logic:**
- Start with `pollInterval = 750ms`.
- If a query returns **no tasks**, double the interval: `750ms -> 1.5s -> 3s -> ... -> Max (e.g., 15s)`.
- If a query **returns a task**, reset interval to `750ms`.
- **Result:** Idle load drops from ~480 queries/min to ~24 queries/min (95% reduction).

### Strategy B: Batch Claiming
**Impact:** Increases throughput during active loads.
**Logic:**
- Instead of `LIMIT 1`, workers should claim `LIMIT N` (e.g., 5) tasks in a single transaction.
- This reduces the overhead of transaction management and network round-trips by 5x for active workloads.

### Strategy C: Index Optimization
**Impact:** Makes the "Check for work" query faster.
**Recommendation:**
Ensure the following composite index exists on `import_task_run`:
```sql
CREATE INDEX CONCURRENTLY idx_import_task_run_polling 
ON import_task_run (job_id, status, created_at);
-- Note: job_type is on import_job, not task_run, requiring a JOIN.
-- Optimizing the JOIN or denormalizing job_type to task_run might be considered if JOIN performance degrades.
```

### Strategy D: Watchdog Tuning
**Impact:** Reduces background noise.
**Logic:**
- The Watchdog loop (checking for stuck tasks) currently runs every 1 minute.
- **Adjustment:** Increase interval to 5 minutes. Stuck tasks are rare edge cases; immediate detection is not critical.

## 3. Implementation Plan (TRACK-006)

| Phase | Action | Est. Effort |
|:---|:---|:---|
| **Phase 1** | Implement `AdaptivePolling` in `BulkImportWorkerService`. | Low |
| **Phase 2** | Update `BulkImportRepository.claimNextPendingTask` to support batch size (e.g., `limit: Int = 1`). | Medium |
| **Phase 3** | Execute database migration for indices (if analysis confirms missing index). | Low |

## 4. Verification
After implementation, we will monitor `sangita_logs.txt` to verify:
1.  **Idle Silence:** Logs should show very few queries when no batch is active.
2.  **Burst Performance:** Throughput during imports should remain high or improve due to batch claiming.
