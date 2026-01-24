# Track: Bulk Import - Admin Dashboard
**ID:** TRACK-003
**Status:** Completed
**Owner:** Sangita Frontend Lead
**Created:** 2026-01-21
**Updated:** 2026-01-23

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
- [x] Implement `NewBatchWizard` (File upload -> Preview -> Start).
- [x] Integrate with `GET /batches` and `POST /batches`.

### Phase 2: Batch Detail & Monitoring
- [x] Implement `BatchDetail` page header (Stats, Status Banner).
- [x] Implement `JobPipeline` visualizer (Stepper).
- [x] Implement `TaskExplorer` (Data Grid with filters for Tasks).
- [x] Connect to `GET /batches/{id}` and `GET /batches/{id}/tasks`.

### Phase 3: Operational Controls
- [x] Add "Pause", "Resume", "Cancel" buttons to Batch Detail.
- [x] Add "Retry Failed" functionality.
- [x] **Cleanup:** Add "Delete" button for removing test/stuck batches.
- [x] Implement `LogViewer` drawer for inspecting task errors/events.

## Progress Log
### 2026-01-23: Phase 3 Completion
- Implemented JobPipeline visualizer with Stepper component showing MANIFEST_INGEST -> SCRAPE -> ENTITY_RESOLUTION stages
- Implemented LogViewer drawer for inspecting task errors/events with detailed task information and copy-to-clipboard functionality
- All three phases completed

### 2026-01-22: Cleanup & Polish
- Added "Delete" button to Batch Detail header for cleaning up batches.
- Connected to new `DELETE` API endpoint.

### 2026-01-21
- Built `/bulk-import` admin page with navigation entry and routing.
- Batch list table with progress bars, status chips, and create-batch form (manifest path input).
- Batch detail panel with tasks filter, events feed, job statuses, and pause/resume/cancel/retry controls.
- Connected to backend orchestration endpoints with toasts and error parsing.
- Integrated Review Queue navigation and updated types/client for manual intervention support.

## Dependencies
- TRACK-001 (Backend APIs) must be partially ready (Foundation Phase) to start Phase 1.
