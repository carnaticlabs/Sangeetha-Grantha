# Track: Bulk Import - Review & Resolution UI
**ID:** TRACK-004
**Status:** In Progress
**Owner:** Sangita Frontend Lead
**Created:** 2026-01-21
**Updated:** 2026-01-21

## Goal
Implement the advanced "Review" and "Entity Resolution" interfaces for the Bulk Import system. This allows human operators to resolve conflicts and approve data for production.

## Context
- **Reference UI Plan:** `/application_documentation/01-requirements/features/bulk-import/ui-ux-plan.md`
- **Tech Stack:** React, TypeScript.

## Implementation Plan (Phased)

### Phase 1: Review Queue & Diff Viewer
- [x] Implement `ReviewQueue` page (List of items ready for review).
- [x] Build `DiffViewer` component (Side-by-side inputs for Scraped vs Overrides).
- [x] Implement "Approve" and "Reject" actions.

### Phase 2: Entity Resolution Interface
- [x] Build `EntityResolution` widget (for unresolved Ragas/Composers).
- [x] Display confidence scores and suggestions from backend.
- [x] Implement "Search & Select Existing" workflow (via AI suggestion buttons).

### Phase 3: Bulk Actions & Finalize
- [ ] Add checkboxes for bulk approval/rejection.
- [ ] Implement "Finalize Batch" workflow.
- [ ] Add export functionality for QA reports.

## Progress Log
### 2026-01-21
- Created `ImportReviewPage.tsx` with sidebar for pending imports.
- Implemented resolution panel showing AI candidates for Composers and Ragas with confidence scores.
- Implemented override form for Title, Composer, Raga, Tala, Language, and Lyrics.
- Integrated `Approve & Create` and `Reject` actions with backend.
- Wired route `/bulk-import/review` and added Sidebar link.

## Dependencies
- TRACK-001 (Entity Resolution & Review APIs) must be ready.
- TRACK-003 (Dashboard Foundation) should be in progress or done.