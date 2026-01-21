# Track: Bulk Import - Admin Dashboard
**ID:** TRACK-003
**Status:** In Progress
**Owner:** Sangita Frontend Lead
**Created:** 2026-01-21
**Updated:** 2026-01-21

## Goal
Implement the core Admin Dashboard pages for the Bulk Import system. This allows admins to create, monitor, and control import batches.
(Relies on Backend APIs from TRACK-001).

## Context
- **Reference UI Plan:** `/application_documentation/01-requirements/features/bulk-import/ui-ux-plan.md`
- **Tech Stack:** React, TypeScript, Material UI (or existing design system).

## Implementation Plan (Phased)

### Phase 1: Foundation & Listing
- [x] Create `BulkImportLayout` and navigation entry.
- [x] Implement `BatchList` page (Table with ID, Source, Progress, Status).
- [ ] Implement `NewBatchWizard` (File upload -> Preview -> Start).
- [x] Integrate with `GET /batches` and `POST /batches`.

### Phase 2: Batch Detail & Monitoring
- [x] Implement `BatchDetail` page header (Stats, Status Banner).
- [ ] Implement `JobPipeline` visualizer (Stepper).
- [x] Implement `TaskExplorer` (Data Grid with filters for Tasks).
- [x] Connect to `GET /batches/{id}` and `GET /batches/{id}/tasks`.

### Phase 3: Operational Controls
- [x] Add "Pause", "Resume", "Cancel" buttons to Batch Detail.
- [x] Add "Retry Failed" functionality.
- [ ] Implement `LogViewer` drawer for inspecting task errors/events.

## Progress Log
### 2026-01-21
- Built `/bulk-import` admin page with navigation entry and routing.
- Batch list table with progress bars, status chips, and create-batch form (manifest path input).
- Batch detail panel with tasks filter, events feed, job statuses, and pause/resume/cancel/retry controls.
- Connected to backend orchestration endpoints with toasts and error parsing.

## Dependencies
- TRACK-001 (Backend APIs) must be partially ready (Foundation Phase) to start Phase 1.
