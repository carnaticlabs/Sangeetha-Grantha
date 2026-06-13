| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P2 |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (W2 Quality) |
| **Decisions** | D13 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | TRACK-111 (CI must exist first) |

# TRACK-118: Frontend Component Tests (Vitest)

## Goal

Component tests on the most business-critical UI, kept off the backend critical path. Estimated ~1 week. Deliberately scheduled **after** CI exists (TRACK-111) so it does not dilute the backend foundation.

## Context

North-star N3: zero frontend tests across 113 TS/TSX files, including the 688-line `CuratorReviewPage.tsx` and 857-line `BulkImport.tsx` — the most business-critical UI in the system.

## Implementation Plan

- [ ] Vitest + Testing Library setup in `modules/frontend/sangita-admin-web`; wire into CI (frontend job).
- [ ] Tests for `CuratorReviewPage.tsx` — the curation workflow.
- [ ] Tests for `BulkImport.tsx` — the highest-volume write path UI.
- [ ] Decompose the 600–850-line page components into testable units as coverage is added.

## Acceptance Criteria

- The two critical pages have meaningful component coverage.
- Runs in CI within the frontend time budget.

## References

- [North-Star Evaluation N3](../../application_documentation/north-star-evaluation.md)
- [Implementation Plan — TRACK-118](../../application_documentation/north-star-production-readiness-implementation-plan.md)
