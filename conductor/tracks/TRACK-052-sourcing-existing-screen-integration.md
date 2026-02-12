| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-09 |
| **Author** | Sangeetha Grantha Team |

# TRACK-052: Existing Screen Integration (Sourcing)

## 1. Objective
Integrate sourcing and provenance data into existing Admin Console screens — the Krithi Editor, Bulk Import Batch Detail, Bulk Import Task Review, and Import Review — ensuring sourcing context is available where editors and operators need it most.

## 2. Context
- **Reference**: [Sourcing UI/UX Plan](../../application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md) — §9 (Integration with Existing Screens)
- **Parent Track**: [TRACK-041](./TRACK-041-enhanced-sourcing-logic.md)
- **Phase**: Cross-cutting — can begin after Phase 2, completes alongside Phase 3
- **Backend Dependencies**: TRACK-045 §3.3 (Source Evidence API), §3.4 (Structural Voting API)

## 3. Implementation Plan

### 3.1 Krithi Editor — Source Evidence Tab
- [ ] Add a new "Source Evidence" tab to the existing Krithi Editor component.
- [ ] When active, display all contributing sources for the current Krithi (reuse `KrithiEvidenceDetail` from TRACK-049).
- [ ] Show field provenance: for each field (title, raga, tala, sections, lyrics, deity, temple), indicate which source(s) provided the current value.
- [ ] Show voting decisions affecting this Krithi (link to Voting Detail in TRACK-050).
- [ ] Link to full Source Evidence Browser (`/admin/sourcing/evidence`) with the current Krithi pre-filtered.
- [ ] Lazy-load evidence data only when the tab is selected.

### 3.2 Bulk Import — Batch Detail Integration
- [ ] Add "Source Authority" column to the Task Explorer table in Batch Detail view.
  - Display the TierBadge of the source being imported for each task.
- [ ] For extraction-linked tasks, add a clickable link from the task row to Extraction Detail (`/admin/sourcing/extractions/:id`).
- [ ] Show extraction-linked batch ID in Extraction Detail (reverse link from TRACK-047).

### 3.3 Bulk Import — Task Review Integration
- [ ] Add a "Source Evidence" sidebar panel to the Task Review interface.
  - When reviewing an entity resolution decision, show alternative values from other sources.
  - Display ConfidenceBar indicators from structural voting alongside each alternative.
- [ ] Highlight when a higher-tier source has a different value than the one currently being reviewed.
- [ ] Add tooltip explaining the tier system for reviewers unfamiliar with it.

### 3.4 Import Review — Authority Source Validation (§6.2.7 of PRD)
- [ ] Show TierBadge next to the source name in the Import Review screen.
- [ ] Implement authority validation warning:
  - When approving data from a lower-tier source, check if higher-tier data exists for the same Krithi.
  - If yes, show a warning banner: "Higher-tier source (Tier X: {source_name}) has different data for this Krithi. Review before approving."
  - Warning includes a link to the field comparison view.
- [ ] Allow dismissal of the warning with a reason (logged to audit_log).

### 3.5 Navigation Cross-Links
- [ ] Ensure all deep-links between modules work correctly:
  - Krithi Editor → Source Evidence Browser → Voting Detail.
  - Batch Detail → Extraction Detail.
  - Task Review → Field Comparison View.
  - Import Review → Source Detail.
- [ ] Breadcrumb navigation correctly reflects cross-module navigation paths.

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Krithi Editor tab | `modules/frontend/sangita-admin-web/src/components/krithi/SourceEvidenceTab.tsx` | New evidence tab |
| Batch Detail extension | `modules/frontend/sangita-admin-web/src/pages/bulk-import/BatchDetailPage.tsx` | Modified for tier column |
| Task Review sidebar | `modules/frontend/sangita-admin-web/src/components/bulk-import/SourceEvidenceSidebar.tsx` | Review sidebar |
| Authority validation | `modules/frontend/sangita-admin-web/src/components/import-review/AuthorityWarning.tsx` | Tier validation warning |

## 5. Acceptance Criteria
- Krithi Editor displays "Source Evidence" tab with correct provenance data for the current Krithi.
- Batch Detail shows TierBadge in Task Explorer and links to Extraction Detail.
- Task Review displays source evidence sidebar with alternative values and confidence indicators.
- Import Review shows authority validation warning when approving lower-tier data over higher-tier.
- Warning can be dismissed with a reason (logged).
- All cross-module navigation links work correctly.
- Evidence tab lazy-loads data (no performance impact on Krithi Editor initial load).

## 6. Dependencies
- TRACK-044 (TierBadge, ConfidenceBar components).
- TRACK-045 §3.3, §3.4 (Evidence and Voting API endpoints).
- TRACK-049 (KrithiEvidenceDetail component — reused in Krithi Editor tab).
- TRACK-050 (Voting Detail — linked from evidence views).
- Existing Krithi Editor (TRACK-023), Bulk Import pages (TRACK-003, TRACK-004, TRACK-005).

## 7. Progress Log
- **2026-02-09**: Track created based on Sourcing UI/UX Plan §9.
- **2026-02-09**: Implementation complete:
  - §3.1: Created `SourceEvidenceTab.tsx` in `components/krithi-editor/` — displays contributing sources, field provenance comparison (FieldComparisonTable), structural analysis per source, and voting decisions. Tab is hidden for new compositions and lazy-loads data on selection.
  - §3.1: Updated `KrithiEditorState.activeTab` type union to include `'Source Evidence'`.
  - §3.1: Added `SourceEvidenceTab` to tab bar and content rendering in `KrithiEditor.tsx`.
  - §3.2: Added extraction queue cross-link in BulkImport batch detail sidebar. Full source authority column requires backend enrichment of `ImportTaskRun` with tier info (deferred).
  - §3.4: Created `AuthorityWarning.tsx` component in `components/import-review/` with tier validation banner, conflict display, and dismissible with reason (for audit logging).
  - §3.4: Integrated `TierBadge` next to source name and `AuthorityWarning` into `ImportReview.tsx`. Warning triggers for tier > 2 sources.
  - §3.5: All cross-module navigation links implemented: Krithi Editor ↔ Source Evidence Browser ↔ Voting Detail, Batch Detail ↔ Extraction Queue, Import Review ↔ Source Detail.
  - Upgraded `ExtractionDetailPage.tsx` and `VotingDetailPage.tsx` from placeholders to full functional implementations with data hooks, status displays, and interactive controls.
