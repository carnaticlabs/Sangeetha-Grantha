# Track: Bulk Import - Performance Optimization
**ID:** TRACK-006
**Status:** Completed
**Owner:** Sangita Grantha Architect
**Created:** 2026-01-22
**Updated:** 2026-01-22

## Problem Statement
The current Bulk Import worker implementation uses aggressive polling (750ms interval) across multiple worker threads (6 total).
This results in excessive "SELECT ... FOR UPDATE" queries even when the system is idle, spamming the database logs and consuming unnecessary resources.

## Goal
Reduce database load by optimizing the worker polling mechanism while maintaining responsiveness during active imports.

## Implementation Plan

### Phase 1: Immediate Relief (Config & Indexing)
- [x] **Adaptive Polling:** Implement exponential backoff for workers when no tasks are found (e.g., 750ms -> 1.5s -> 3s ... -> 15s max).
- [x] **Batch Claiming:** Update `claimNextPendingTask` to claim multiple tasks (e.g., 5 or 10) in a single transaction if available.
- [x] **Index Verification:** Ensure optimal indices exist for the claim query: `(status, job_type, created_at)` on `import_task_run`.

### Phase 2: Architectural Improvements (Refactoring)
- [ ] **Unified Polling:** Replace individual worker polling loops with a single "Task Dispatcher" coroutine that queries the DB and distributes tasks to worker channels.
- [ ] **Event-Driven Wakeup:** (Optional) Use Postgres `LISTEN/NOTIFY` or an in-memory signal to wake up the dispatcher immediately when a batch is created/resumed.

## Technical Approach
1.  **Backoff:** Simple `delay(currentInterval)` where `currentInterval` doubles on empty result, resets on success.
2.  **Batching:** `LIMIT 5` in the claim query, return `List<Task>`.
