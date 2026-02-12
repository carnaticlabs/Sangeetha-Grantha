| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-09 |
| **Author** | Sangeetha Grantha Team |

# TRACK-047: Extraction Monitor & Operations UI

## 1. Objective
Implement the Extraction Queue Monitor — the real-time operational view of the Kotlin ↔ Python extraction pipeline — including the queue list with auto-refresh, the extraction detail with results/errors, the new extraction request wizard, and queue operation controls (retry, cancel).

## 2. Context
- **Reference**: [Sourcing UI/UX Plan](../../application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md) — §4.4 (Extraction Monitor), §4.4.1 (New Extraction Wizard), §4.5 (Extraction Detail)
- **Parent Track**: [TRACK-041](./TRACK-041-enhanced-sourcing-logic.md)
- **Phase**: Phase 1–2 (Sprint 1–4) — basic list in Phase 1, detail and operations in Phase 2
- **Backend Dependency**: TRACK-045 §3.2 (Extraction Queue API)

## 3. Implementation Plan

### 3.1 Extraction Monitor List View (§4.4)
- [ ] Create `ExtractionMonitorPage.tsx` at route `/admin/sourcing/extractions`.
- [ ] Implement header: Title "Extraction Queue", Buttons: "New Extraction Request", "Retry All Failed".
- [ ] **Status Summary Bar** (horizontal segments):
  - Five status segments: PENDING (grey), PROCESSING (blue pulse), DONE (green), FAILED (red), CANCELLED (dark grey).
  - Total count and throughput metric (extractions/hour over last 24h).
  - Connect to `GET /v1/admin/sourcing/extractions/stats`.
- [ ] Implement filter bar:
  - Status multi-select (PENDING, PROCESSING, DONE, FAILED, CANCELLED).
  - Source Format (PDF, DOCX, IMAGE).
  - Source Name (typeahead from source registry).
  - Date Range picker (created_at).
  - Batch link (linked import batch, if any).
- [ ] Implement data table with columns:
  - ID (truncated UUID with copy-to-clipboard button).
  - Source (name + TierBadge).
  - Format (FormatPill).
  - URL (truncated, external link icon).
  - Status (StatusChip with pulse animation for PROCESSING).
  - Krithis (result count).
  - Confidence (ConfidenceBar).
  - Duration (human-readable, e.g., "4.2s").
  - Attempts (current / max).
  - Worker (claimed-by hostname for PROCESSING).
  - Created (relative timestamp).
  - Actions: "View", "Retry" (if FAILED), "Cancel" (if PENDING/PROCESSING).
- [ ] Connect to `GET /v1/admin/sourcing/extractions` with TanStack Query.
- [ ] **Auto-Refresh**: Poll every 10 seconds using `refetchInterval`. PROCESSING rows have subtle pulse animation.
- [ ] Failed rows highlighted with red left-border accent.
- [ ] Row click navigates to Extraction Detail.
- [ ] Table sorting on Source, Format, Status, Krithis, Confidence, Duration, Created.
- [ ] Pagination with configurable page size.

### 3.2 New Extraction Request Wizard (§4.4.1)
- [ ] Create `ExtractionRequestWizard.tsx` — multi-step modal.
- [ ] **Step 1 — Source Selection**:
  - Dropdown to select existing source from registry (with TierBadges).
  - Or toggle to "One-off URL" mode: URL input + Format select.
- [ ] **Step 2 — Extraction Parameters**:
  - Source URL (pre-filled if source selected, editable).
  - Format (select: PDF, DOCX, IMAGE — pre-filled from source's supported formats).
  - Page Range (text input, for PDFs, e.g., "1-10" or "42-43").
  - Composer Hint (typeahead from existing composers).
  - Expected Krithi Count (number input, optional).
  - Link to Import Batch (select from existing batches, optional).
  - Max Attempts (number input, default: 3).
- [ ] **Step 3 — Confirmation**:
  - Read-only summary of all parameters.
  - "Submit to Queue" button.
  - On success: navigate to Extraction Detail for the new task.
- [ ] Client-side validation per step (URL format, required fields).
- [ ] Connect to `POST /v1/admin/sourcing/extractions`.

### 3.3 Extraction Detail View (§4.5)
- [ ] Create `ExtractionDetailPage.tsx` at route `/admin/sourcing/extractions/:id`.
- [ ] Implement header: Task ID, large StatusChip, FormatPill, Source name with TierBadge.
- [ ] **Section A — Request Parameters** (Card):
  - Source URL (clickable external link).
  - Source format, tier, name.
  - Page range (if applicable).
  - Composer hint.
  - Request payload (collapsible JsonViewer).
  - Linked import batch (clickable link to Batch Detail, if any).
- [ ] **Section B — Processing Status** (TimelineCard):
  - Visual timeline: Created → Claimed → Completed/Failed.
  - Each state shows timestamp, duration, and actor (worker hostname for PROCESSING).
  - Attempt counter with per-attempt details if retried.
- [ ] **Section C — Extraction Results** (shown only when DONE):
  - Summary row: Krithi count, overall confidence, extraction method, extractor version, duration.
  - Results table (one row per extracted Krithi):
    - Title, Raga, Tala, Composer, Sections (summary, e.g., "P + A + 2C"), Languages, Confidence (per-Krithi ConfidenceBar).
    - Actions: "Preview" (opens JsonViewer modal), "Import" (sends to import pipeline).
  - Raw Payload: Collapsible JsonViewer showing full `result_payload`.
- [ ] **Section D — Error Details** (shown only when FAILED):
  - Error summary with categorisation badge (network, parse, OCR, timeout).
  - Structured error detail (collapsible JsonViewer from `error_detail`).
  - Last error timestamp.
  - "Retry" button (if attempts < max_attempts).
  - "Cancel" button.
- [ ] **Section E — Audit Trail** (Collapsible):
  - Source checksum (SHA-256).
  - Cached artifact path.
  - Created/updated timestamps.
- [ ] Connect to `GET /v1/admin/sourcing/extractions/:id` with TanStack Query.
- [ ] Auto-refresh while status is PENDING or PROCESSING (poll every 5 seconds).

### 3.4 Queue Operations
- [ ] "Retry" action on individual failed tasks: confirmation dialog → `POST .../retry`.
- [ ] "Cancel" action on individual pending/processing tasks: confirmation dialog → `POST .../cancel`.
- [ ] "Retry All Failed" button: confirmation dialog showing count of failed tasks → `POST .../retry-all-failed`.
- [ ] All operations show success/error toasts and invalidate query cache.

### 3.5 Loading & Error States
- [ ] Skeleton loaders for list table, status summary bar, and detail sections.
- [ ] Empty state: "No extractions yet — submit an extraction request to get started."
- [ ] Error boundary with retry for failed API calls.
- [ ] Loading indicator during auto-refresh (subtle, non-blocking).

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Extraction Monitor page | `modules/frontend/sangita-admin-web/src/pages/sourcing/ExtractionMonitorPage.tsx` | Queue list view |
| Extraction Detail page | `modules/frontend/sangita-admin-web/src/pages/sourcing/ExtractionDetailPage.tsx` | Task detail |
| Extraction Wizard | `modules/frontend/sangita-admin-web/src/components/sourcing/ExtractionRequestWizard.tsx` | Multi-step form |
| Status Summary Bar | `modules/frontend/sangita-admin-web/src/components/sourcing/StatusSummaryBar.tsx` | Queue status visualization |

## 5. Acceptance Criteria
- Extraction queue list displays all tasks with correct status chips and formatting.
- Auto-refresh updates the table every 10 seconds without losing scroll position or filter state.
- PROCESSING rows show pulse animation; FAILED rows show red accent.
- New extraction request wizard validates inputs and creates queue entries.
- Extraction Detail shows correct section (results or errors) based on status.
- TimelineCard renders state transitions chronologically.
- Retry and cancel operations work with confirmation dialogs.
- "Retry All Failed" bulk operation works correctly.
- Detail page auto-refreshes while task is in-progress.

## 6. Dependencies
- TRACK-044 (StatusChip extension, FormatPill, TierBadge, ConfidenceBar, TimelineCard, JsonViewer, MetricCard, DataGrid).
- TRACK-045 §3.2 (Extraction Queue API endpoints).
- TRACK-046 (Source Registry — for source selection in wizard).

## 7. Progress Log
- **2026-02-09**: Track created based on Sourcing UI/UX Plan §4.4, §4.4.1, §4.5.
- **2026-02-09**: Implementation complete:
  - Created `StatusSummaryBar.tsx` with visual status summary and throughput display.
  - Implemented `ExtractionMonitorPage.tsx` with auto-refreshing queue table (10s), StatusChip with pulse animation, retry/cancel actions, "Retry All Failed" batch operation, and extraction stats using MetricCard.
