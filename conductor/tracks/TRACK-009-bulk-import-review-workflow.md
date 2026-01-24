# TRACK-009: Bulk Import Review Workflow & Auto-Approval

| Metadata | Value |
|:---|:---|
| **Status** | **Proposed** |
| **Owner** | Frontend/Backend Team |
| **Priority** | Medium |
| **Related Tracks** | TRACK-001, TRACK-004 |

## 1. Goal
Streamline the review process for bulk-imported Krithis (Phase 4). Given the volume (1,200+), manual review of every single item is inefficient. We need batch-level review tools and auto-approval capabilities for high-confidence matches.

## 2. Problem Statement
- **Volume**: Reviewing 1,200 items one-by-one takes too long.
- **Context**: Reviewers need to see items in the context of their batch (e.g., "All Thyagaraja imports").
- **Efficiency**: Many imports will be perfect (high confidence). These should be auto-approved or approved in bulk.

## 3. Implementation Plan

### 3.1 Backend Enhancements
- **Auto-Approval Logic**:
    - Implement `AutoApprovalService` that runs after Entity Resolution.
    - Rules: If `composer_confidence` > 0.95 AND `raga_confidence` > 0.95 AND `title_match` > 0.90 -> Set status `APPROVED` automatically (or `AUTO_APPROVED`).
- **Bulk API**:
    - `POST /v1/admin/bulk-import/batches/{id}/approve-all`: Approve all pending items in a batch.
    - `POST /v1/admin/bulk-import/batches/{id}/reject-all`: Reject all pending items.

### 3.2 Frontend Enhancements (Batch Dashboard)
- **Batch Filter**: Add "Filter by Batch" dropdown to the main Review Queue.
- **Batch Stats**: Show "Ready for Review", "Auto-Approved", "Rejected" counts per batch.
- **Bulk Actions**: Add "Approve All" / "Reject All" buttons to the Batch Detail view.

## 4. Progress Log
- [x] Backend: Implement `AutoApprovalService`.
- [x] Backend: Create Bulk Approval/Rejection APIs.
- [x] Frontend: Add Batch Filter to Review Queue. (Logic added via ListByBatch API, UI filter already handles by selected batch context)
- [x] Frontend: Implement Bulk Action buttons in Batch Detail.
