| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-074 |
| **Title** | Refactor DAL Repositories |
| **Status** | Complete |
| **Created** | 2026-02-28 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-073 |

# TRACK-074: Refactor DAL Repositories

## Objective

Split `KrithiRepository.kt` (899 lines) and `BulkImportRepository.kt` (718 lines) into focused, sub-300-line repositories.

## Motivation

- KrithiRepository is the largest file in the codebase (899 lines) mixing CRUD, search, variants, and sections
- BulkImportRepository (718 lines) mixes batch/job/task lifecycle with event logging and watchdog logic
- Both have zero test coverage, making refactoring risky without tests first

## Scope

### Phase 1: Tests (BEFORE any refactoring)

- Write `KrithiRepositoryTest.kt` (~20 integration tests): CRUD, search, variants, sections
- Write `BulkImportRepositoryTest.kt` (~15 integration tests): batch/job/task lifecycle, claiming, watchdog

### Phase 2: Split KrithiRepository.kt

| Extract To | Methods | ~Lines |
|-----------|---------|--------|
| `KrithiLyricVariantRepository.kt` | getLyricVariants, findLyricVariantById, createLyricVariant, updateLyricVariant, saveLyricVariantSections | ~280 |
| `KrithiSearchRepository.kt` | search, findDuplicateCandidates, findNearTitleCandidates, findCandidatesByMetadata, countAll, countByState | ~280 |
| `KrithiRepository.kt` (slimmed) | findById, create, update, getTags, updateTags, getSections, saveSections | ~280 |

### Phase 3: Split BulkImportRepository.kt

| Extract To | Methods | ~Lines |
|-----------|---------|--------|
| `BulkImportTaskRepository.kt` | createTask, createTasks, findTaskById, listTasksByJob, listTasksByBatch, claimNextPendingTasks, markTaskStarted, updateTaskStatus, incrementTaskAttempt, requeueTasksForBatch, markStuckRunningTasks | ~250 |
| `BulkImportEventRepository.kt` | createEvent, listEventsByRef, deleteBatch | ~180 |
| `BulkImportRepository.kt` (slimmed) | createBatch, findBatchById, listBatches, updateBatchStatus, updateBatchStats, setBatchTotals, createJob, findJobById, listJobsByBatch, updateJobStatus, incrementJobRetry, incrementBatchCounters | ~250 |

### Phase 4: Update Wiring

- Update `SangitaDal` interface to expose new repositories
- Update `SangitaDalImpl` to instantiate new repositories
- Update all service-layer callers (ImportService, ExtractionResultProcessor, etc.)

## Verification

1. `./gradlew :modules:backend:api:test` -- 0 failures
2. No file exceeds 300 lines
3. All new repositories registered in SangitaDal
