| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-09 |
| **Author** | Sangeetha Grantha Team |

# TRACK-046: Source Registry UI

## 1. Objective
Implement the Source Registry management screens — the list view for browsing and filtering import sources, the detail view for deep-diving into a single source's contribution history, and the register/edit modal for CRUD operations.

## 2. Context
- **Reference**: [Sourcing UI/UX Plan](../../application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md) — §4.2 (Source Registry), §4.3 (Source Detail), §4.2.1 (Register/Edit Form)
- **Parent Track**: [TRACK-041](./TRACK-041-enhanced-sourcing-logic.md)
- **Phase**: Phase 1 (Sprint 1–2) — Foundation
- **Backend Dependency**: TRACK-045 §3.1 (Source Registry API)

## 3. Implementation Plan

### 3.1 Source Registry List View (§4.2)
- [ ] Create `SourceRegistryPage.tsx` at route `/admin/sourcing/sources`.
- [ ] Implement header with title "Source Registry" and "Register New Source" button.
- [ ] Implement filter bar:
  - Tier multi-select checkboxes (1–5).
  - Supported Formats multi-select (HTML, PDF, DOCX, API, MANUAL).
  - Status toggle (Active / Inactive).
  - Search input (by name or URL).
- [ ] Implement data table with columns:
  - Name (source display name).
  - URL (base URL, truncated).
  - Tier (TierBadge component with tooltip).
  - Formats (FormatPill badges).
  - Composers (top affinities with weights).
  - Krithis (count of sourced Krithis).
  - Last Harvested (relative timestamp, e.g., "3 days ago").
  - Actions (View, Edit, Deactivate).
- [ ] Connect to `GET /v1/admin/sourcing/sources` with TanStack Query.
- [ ] Implement table sorting (Name, Tier, Krithis, Last Harvested).
- [ ] Implement pagination.
- [ ] Row click navigates to Source Detail.

### 3.2 Register / Edit Source Modal (§4.2.1)
- [ ] Create `SourceFormModal.tsx` — reusable for both create and edit modes.
- [ ] Implement form fields:
  - Name (text, required, unique validation).
  - Base URL (URL input, required, format validation).
  - Tier (select 1–5, required).
  - Supported Formats (multi-select, at least one required).
  - Composer Affinities (repeatable key-value: Composer typeahead → Weight 0.0–1.0 slider).
  - Description (textarea, max 500 chars).
  - Active (toggle, default: On).
- [ ] Client-side validation with error messages.
- [ ] Connect to `POST /v1/admin/sourcing/sources` (create) and `PUT /v1/admin/sourcing/sources/:id` (edit).
- [ ] Success: close modal, invalidate query cache, show toast.
- [ ] Error: display server-side validation errors in form.

### 3.3 Source Detail View (§4.3)
- [ ] Create `SourceDetailPage.tsx` at route `/admin/sourcing/sources/:id`.
- [ ] Implement header: Source name, TierBadge, FormatPills, "Edit" and "Deactivate" buttons.
- [ ] **Section A — Source Profile** (Card):
  - Base URL (clickable external link with icon).
  - Description text.
  - Composer affinities (visual weight bars, e.g., horizontal bars with labels).
  - Registered date and last harvested timestamp.
- [ ] **Section B — Contribution Summary** (MetricCards row):
  - Total Krithis contributed.
  - Fields contributed breakdown (pie chart: title, raga, tala, sections, lyrics, deity, temple, notation).
  - Average confidence score (ConfidenceBar).
  - Extraction success rate (percentage with trend indicator).
- [ ] **Section C — Extraction History** (DataGrid):
  - Filterable/sortable table of extraction queue entries for this source.
  - Columns: Task ID, Format, Status, Krithi Count, Confidence, Duration, Created At.
  - Row click navigates to Extraction Detail (TRACK-047).
- [ ] **Section D — Contributed Krithis** (DataGrid):
  - List of Krithis with evidence from this source.
  - Columns: Krithi Title, Raga, Tala, Contributed Fields (pill badges), Confidence, Extracted At.
  - Row click navigates to Krithi editor with Source Evidence tab active.
- [ ] Connect to `GET /v1/admin/sourcing/sources/:id` with TanStack Query.
- [ ] Implement deactivate confirmation dialog with `DELETE` API call.

### 3.4 Loading & Error States
- [ ] Skeleton loaders for list table rows and detail card sections.
- [ ] Empty state: "No sources registered yet — click 'Register New Source' to get started."
- [ ] Error boundary with retry action for failed API calls.

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Source Registry page | `modules/frontend/sangita-admin-web/src/pages/sourcing/SourceRegistryPage.tsx` | List view |
| Source Detail page | `modules/frontend/sangita-admin-web/src/pages/sourcing/SourceDetailPage.tsx` | Detail view |
| Source Form modal | `modules/frontend/sangita-admin-web/src/components/sourcing/SourceFormModal.tsx` | Create/edit form |
| Source filter bar | `modules/frontend/sangita-admin-web/src/components/sourcing/SourceFilterBar.tsx` | Filter controls |

## 5. Acceptance Criteria
- Sources can be listed, filtered by tier/format/status/search, and sorted.
- New sources can be registered via modal with validation.
- Existing sources can be edited — form pre-fills with current values.
- Sources can be deactivated with confirmation dialog.
- Source Detail shows profile, contribution metrics, extraction history, and contributed Krithis.
- Navigation between list → detail → edit flows smoothly.
- TierBadge tooltips explain each tier level.
- All states handled: loading (skeleton), empty, error (retry).

## 6. Dependencies
- TRACK-044 (route scaffolding, TierBadge, FormatPill, MetricCard, ConfidenceBar, DataGrid, StatusChip).
- TRACK-045 §3.1 (Source Registry API endpoints).

## 7. Progress Log
- **2026-02-09**: Track created based on Sourcing UI/UX Plan §4.2, §4.2.1, §4.3.
- **2026-02-09**: Implementation complete:
  - Created `SourceFilterBar.tsx` with tier, format, and search filters.
  - Created `SourceFormModal.tsx` for create/edit forms with client-side validation.
  - Implemented `SourceRegistryPage.tsx` with filterable table view, TierBadge, FormatPill, deactivation support.
  - Implemented `SourceDetailPage.tsx` with source profile, MetricCards for contribution stats, composer affinity display.
