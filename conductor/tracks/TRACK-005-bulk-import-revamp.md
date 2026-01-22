# Track: Bulk Import Revamp (Holistic Fix)
**ID:** TRACK-005
**Status:** Completed
**Owner:** Sangita Grantha Architect
**Created:** 2026-01-22
**Updated:** 2026-01-22

## Problem Statement
The initial Bulk Import implementation (TRACK-001, TRACK-003) delivered a "technical" MVP that is operationally brittle and user-unfriendly.
**Key Deficiencies:**
1.  **Manual Path Entry:** Users must manually type server-side file paths.
2.  **Lack of Feedback:** No real-time progress updates (requires manual refresh).
3.  **Opaque Errors:** Failures in early stages (Manifest Ingest) are hard to diagnose.
4.  **Brittle CSV Handling:** Manual parsing is error-prone.

## Goal
Deliver a **production-grade** Bulk Import capability that is intuitive ("Drag & Drop"), transparent (Real-time Progress), and robust.

## Implementation Plan

### Phase 1: Robust Import API (Backend)
- [x] **Endpoint:** Implement `POST /v1/admin/bulk-import/upload` (Multipart).
    -   Accepts CSV file.
    -   Validates headers immediately (fail fast).
    -   Saves to a managed `storage/imports/` directory.
    -   Returns `batchId` immediately.
- [x] **CSV Hardening:** Replace manual string splitting with a robust CSV library (e.g., `kotlin-csv` or `commons-csv`).
- [x] **State Clarity:** meaningful `status_message` on the Batch entity for high-level feedback (Implemented via UI inference from Job states).

### Phase 2: Reactive Admin UI (Frontend)
- [x] **Upload Widget:** Replace "Manifest Path" input with a File Upload Dropzone.
- [x] **Live Polling:** Implement `useInterval` or `tanstack-query` polling (2s interval) for running batches.
- [x] **Stage Visualization:** Show clear visual steps:
    1.  **Uploading** (Client -> Server)
    2.  **Analyzing** (Manifest Ingest)
    3.  **Processing** (Scraping & Resolution)
- [ ] **Error Surfacing:** Display "Manifest Errors" (e.g., missing columns) directly on the Batch Detail summary.

### Phase 3: Operational Safety
- [ ] **Pre-flight Check:** Validate URL accessibility (HEAD request) during Manifest Ingest (optional, or as a "dry run" mode).
- [ ] **Validation:** Ensure all 3 required columns (Krithi, Raga, Hyperlink) exist before creating tasks.

## Technical Approach
1.  **Uploads:** Use Ktor `ContentNegotiation` and `MultiPartData` to handle uploads.
2.  **Polling:** Stick to short-polling (simple, robust) over WebSockets for now.
3.  **CSV:** Use a standard library to handle edge cases (quotes, newlines).

## Dependencies
- Replaces/Extends portions of TRACK-001 and TRACK-003.