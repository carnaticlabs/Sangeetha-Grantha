# Track: Bulk Import - Architectural Refactoring
**ID:** TRACK-007
**Status:** Completed
**Owner:** Sangita Grantha Architect
**Created:** 2026-01-22
**Updated:** 2026-01-22

## Problem Statement
While `TRACK-006` mitigated the immediate database load issues via adaptive polling, the architecture remains "Pull-based" with multiple independent polling loops. This has limitations:
1.  **fragmented Control:** Hard to coordinate global rate limits or priority between job types.
2.  **Redundant Checks:** Multiple loops might wake up simultaneously to find nothing.
3.  **Scalability:** Adding new job types requires adding new polling loops, increasing DB load linearly.

## Goal
Transition to a **"Push-based" architecture** (Unified Polling) where a single **Dispatcher** queries the database and distributes tasks to worker pools via Channels.

## Implementation Plan

### Phase 1: The Dispatcher Pattern
- [x] **Channels:** Define Kotlin `Channel<ImportTaskRunDto>` for `Manifest`, `Scrape`, and `Resolution` work streams.
- [x] **Dispatcher Loop:** Implement a single coroutine that:
    -   Iterates through job types.
    -   Checks if the corresponding channel has capacity.
    -   Claims tasks from DB if capacity exists.
    -   Sends tasks to channels.
    -   Applies global adaptive backoff if *no* tasks are found across all types.
- [x] **Worker Refactor:** Convert worker loops to simply consume from their respective channels (`for (task in channel)`).

### Phase 2: Tuning & Safety
- [ ] **Graceful Shutdown:** Ensure channels are closed and drained properly on stop.
- [ ] **Capacity Management:** Tune channel buffer sizes (e.g., 20) to prevent memory bloat while keeping workers fed.

## Architecture Change
**Before:**
```
[Manifest Worker] -> Poll DB
[Scrape Worker 1] -> Poll DB
[Scrape Worker 2] -> Poll DB
[Resolution Worker] -> Poll DB
```

**After:**
```
[Dispatcher] -> Poll DB (Unified)
     |
     +-> [Manifest Channel] -> [Manifest Worker]
     +-> [Scrape Channel]   -> [Scrape Worker 1, 2]
     +-> [Resolution Ch.]   -> [Resolution Worker]
```
