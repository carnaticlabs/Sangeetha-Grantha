| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-080 |
| **Title** | Curator Review UI & Section Issue Tracking |
| **Status** | Completed |
| **Created** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-079 (E2E Pipeline & Section Fix) |

# TRACK-080: Curator Review UI & Section Issue Tracking

## Objective

Build a curator review interface for approving/rejecting unmatched PDF extractions and tracking lyric section inconsistencies.

## Problem Statement

172 unmatched PDF extractions sit in `imported_krithis` with no review UI. Curators need a page to approve, reject, or reassign matches. Additionally, krithis with inconsistent lyric sections need a tracking view.

## Scope

### Backend
- `CuratorRoutes.kt`: GET `/v1/admin/curator/stats`, GET `/v1/admin/curator/section-issues`
- `CuratorService.kt`: Stats aggregation, section issue detection (missing sections, extra sections from dual-format, MKS as top-level)
- DI registration in `AppModule.kt`, route registration in `Routing.kt`
- Fix idempotency key scoping in `BulkImportTaskRepository.kt` (include jobType in dedup key)

### Frontend
- `CuratorReviewPage.tsx`: Two-tab interface (Pending Matches, Section Issues)
- Pending Matches: list + detail + resolution panels, bulk selection, approve/reject/merge
- Section Issues: paginated table with issue type categorization
- `ConfirmationModal.tsx`: Reusable modal with configurable actions and optional notes
- Keyboard shortcuts: j/k navigation, a=approve, r=reject
- Sidebar navigation entry with "Curator Review" link

## Verification

- Navigate to `/curator-review` and see 172 pending matches
- Section issues tab shows krithis with inconsistent section counts
- Approve/reject actions update state correctly
