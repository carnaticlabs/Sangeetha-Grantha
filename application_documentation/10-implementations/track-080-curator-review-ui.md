| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

# Curator Review UI & Section Issue Tracking

## Purpose

Provide a curator review interface for managing unmatched PDF extractions and tracking lyric section inconsistencies. Also fix the bulk import idempotency key scoping bug.

## Implementation Details

### Backend
- `CuratorRoutes.kt`: Two endpoints — stats and section issues (paginated)
- `CuratorService.kt`: Stats aggregation across `imported_krithis`, section issue detection comparing `krithi_sections` vs `krithi_lyric_sections` counts per variant
- `BulkImportTaskRepository.kt`: Fixed idempotency key to include jobType (`batchId::jobType::sourceUrl`)

### Frontend
- `CuratorReviewPage.tsx`: Two-tab UI (Pending Matches, Section Issues)
- Pending Matches panel: list, detail editor, resolution candidates with confidence scores
- Section Issues panel: paginated table with issue type categorization
- `ConfirmationModal.tsx`: Reusable modal with configurable confirm color and optional notes
- Keyboard shortcuts: j/k navigation, a=approve, r=reject

## Code Changes

| File | Change |
|------|--------|
| `CuratorRoutes.kt` | New — curator API endpoints |
| `CuratorService.kt` | New — stats and section issue logic |
| `AppModule.kt` | DI registration for CuratorService |
| `Routing.kt` | Route registration for curator endpoints |
| `BulkImportTaskRepository.kt` | Idempotency key includes jobType |
| `CuratorReviewPage.tsx` | New — curator review page |
| `ConfirmationModal.tsx` | New — reusable modal component |
| `App.tsx` | Route for `/curator-review` |
| `Sidebar.tsx` | Navigation entry |
| `client.ts` | API client functions |
| `types.ts` | CURATOR_REVIEW enum value |

Ref: application_documentation/10-implementations/track-080-curator-review-ui.md
