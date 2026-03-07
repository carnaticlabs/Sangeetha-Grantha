| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-07 |
| **Author** | Antigravity |

# Refactor DAL Repositories

## Purpose
Split giant `KrithiRepository` and `BulkImportRepository` into focused repositories to improve maintainability and decouple operations.

## Implementation Details
- Extracted `KrithiLyricRepository` and `KrithiSearchRepository` from `KrithiRepository`.
- Extracted `BulkImportEventRepository` and `BulkImportTaskRepository` from `BulkImportRepository`.
- Updated `SangitaDal.kt` and `DatabaseFactory.kt` to load the new repositories.
- Deleted obsolete tests like `UserRepositoryTest`.

## Code Changes
| File | Change |
|------|--------|
| `modules/backend/dal/.../KrithiRepository.kt` | Refactored |
| `modules/backend/dal/.../BulkImportRepository.kt` | Refactored |
| `modules/backend/dal/.../KrithiLyricRepository.kt` | New file |
| `modules/backend/dal/.../KrithiSearchRepository.kt` | New file |
| `modules/backend/dal/.../BulkImportEventRepository.kt` | New file |
| `modules/backend/dal/.../BulkImportTaskRepository.kt` | New file |
| `modules/backend/dal/.../SangitaDal.kt` | Updated |
| `modules/backend/dal/.../DatabaseFactory.kt` | Updated |
| `modules/backend/dal/.../UserRepositoryTest.kt` | Deleted |

Ref: application_documentation/10-implementations/TRACK-074-dal-repositories.md
