| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-09 |
| **Author** | Sangeetha Grantha Team |

# TRACK-050: Structural Voting UI

## 1. Objective
Implement the Structural Voting Viewer and Detail screens — providing a full audit trail of cross-source structural voting decisions, with the ability for Admin/Reviewer users to manually override low-confidence outcomes by editing the section structure.

## 2. Context
- **Reference**: [Sourcing UI/UX Plan](../../application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md) — §4.7 (Structural Voting), §4.8 (Voting Detail)
- **Parent Track**: [TRACK-041](./TRACK-041-enhanced-sourcing-logic.md)
- **Phase**: Phase 3 (Sprint 5–6) — Provenance & Voting
- **Backend Dependency**: TRACK-045 §3.4 (Structural Voting API)

## 3. Implementation Plan

### 3.1 Structural Voting List View (§4.7)
- [ ] Create `StructuralVotingPage.tsx` at route `/admin/sourcing/voting`.
- [ ] Implement header: Title "Structural Voting", Button "Pending Manual Reviews" (with count badge).
- [ ] **Summary Cards** (top row, 6 cards):
  - Total decisions (count).
  - UNANIMOUS count (%).
  - MAJORITY count (%).
  - AUTHORITY_OVERRIDE count (%).
  - SINGLE_SOURCE count (%).
  - MANUAL count (%).
  - Connect to `GET /v1/admin/sourcing/voting/stats`.
- [ ] Implement filter bar:
  - Consensus Type (multi-select: UNANIMOUS, MAJORITY, AUTHORITY_OVERRIDE, SINGLE_SOURCE, MANUAL).
  - Confidence (HIGH / MEDIUM / LOW badges).
  - Date Range (voted_at).
  - Has Dissenting Sources (toggle).
  - Pending Manual Review (toggle).
- [ ] Implement data table:

  | Column | Description | Sort |
  |:---|:---|:---|
  | Krithi | Krithi title (linked to Krithi editor) | Yes |
  | Voted At | Timestamp | Yes |
  | Sources | Count of participating sources | Yes |
  | Consensus | Consensus type badge (colour-coded: UNANIMOUS=green, MAJORITY=blue, AUTHORITY_OVERRIDE=amber, SINGLE_SOURCE=grey, MANUAL=purple) | Yes |
  | Structure | Winning structure summary (StructureVisualiser compact mode) | No |
  | Confidence | Confidence badge (HIGH=green, MEDIUM=amber, LOW=red) | Yes |
  | Dissents | Count of dissenting sources | Yes |
  | Reviewer | Reviewer name (if MANUAL) or "—" | No |
  | Actions | "View Detail", "Override" (if not MANUAL) | No |

- [ ] Connect to `GET /v1/admin/sourcing/voting` with TanStack Query.
- [ ] "Pending Manual Reviews" button filters to `consensus_type = 'MANUAL'` and `reviewer_id IS NULL`.
- [ ] Table sorting and pagination.
- [ ] Row click navigates to Voting Detail.

### 3.2 Voting Detail View (§4.8)
- [ ] Create `VotingDetailPage.tsx` at route `/admin/sourcing/voting/:id`.
- [ ] Implement header: Krithi title, voting timestamp, consensus type badge (large), confidence badge.
- [ ] **Section A — Winning Structure** (Card, green border):
  - StructureVisualiser showing coloured blocks (Pallavi=blue, Anupallavi=green, Charanam=amber, Chittaswaram=purple).
  - Structure detail table: Section Type, Order, Label.
  - Source(s) that proposed this structure.
- [ ] **Section B — Participating Sources** (Accordion):
  - One panel per source.
  - Panel header: Source name, TierBadge, "Agrees" (green check) or "Dissents" (red X).
  - Panel content:
    - Proposed section structure (StructureVisualiser).
    - Side-by-side diff against winning structure (added=green, removed=red, modified=amber).
    - Source URL and extraction metadata.
- [ ] **Section C — Voting Rationale** (Card):
  - Consensus type explanation text:
    - UNANIMOUS: "All N sources agree on the section structure."
    - MAJORITY: "M of N sources agree. K source(s) dissent."
    - AUTHORITY_OVERRIDE: "Tier T source overrides lower-tier disagreement."
    - SINGLE_SOURCE: "Only one source available — no voting required."
    - MANUAL: "Structure was determined by manual review."
  - Notes field (if present).
  - Reviewer attribution (if MANUAL).
- [ ] **Section D — Manual Override** (restricted to Admin/Reviewer role):
  - Only shown for non-MANUAL decisions (or for MANUAL decisions requiring re-review).
  - Editable section structure builder:
    - Add section: select type (Pallavi, Anupallavi, Charanam, Chittaswaram), set order.
    - Remove section: click X on existing section.
    - Reorder sections: drag-and-drop.
    - Live preview of resulting structure (StructureVisualiser).
  - Notes field (required for override — explain rationale).
  - "Submit Override" button → `POST /v1/admin/sourcing/voting/:id/override`.
  - Creates new `structural_vote_log` entry with `consensus_type = 'MANUAL'`, records `reviewer_id`.
  - Confirmation dialog before submission.
- [ ] Connect to `GET /v1/admin/sourcing/voting/:id`.

### 3.3 Section Structure Editor Component
- [ ] Create `SectionStructureEditor.tsx` — reusable component for building/editing section structures.
- [ ] Support section types: Pallavi, Anupallavi, Charanam (with index), Madhyama Kala Sahitya, Chittaswaram.
- [ ] Drag-and-drop reordering (consider @dnd-kit or similar).
- [ ] Validation: at minimum one Pallavi, valid ordering constraints.
- [ ] Diff mode: highlight differences against a reference structure.

### 3.4 Loading & Error States
- [ ] Skeleton loaders for summary cards, voting list, and detail sections.
- [ ] Empty state: "No structural voting decisions yet — voting occurs automatically during multi-source extraction."
- [ ] Error boundary with retry.

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Voting List page | `modules/frontend/sangita-admin-web/src/pages/sourcing/StructuralVotingPage.tsx` | Voting decisions list |
| Voting Detail page | `modules/frontend/sangita-admin-web/src/pages/sourcing/VotingDetailPage.tsx` | Decision detail with sources |
| Section Structure Editor | `modules/frontend/sangita-admin-web/src/components/sourcing/SectionStructureEditor.tsx` | Drag-and-drop section builder |
| Voting Summary Cards | `modules/frontend/sangita-admin-web/src/components/sourcing/VotingSummaryCards.tsx` | Consensus type summary |

## 5. Acceptance Criteria
- Voting list displays all decisions with correct consensus badges and structure summaries.
- Summary cards show accurate counts and percentages by consensus type.
- "Pending Manual Reviews" correctly filters to unreviewed MANUAL decisions.
- Voting Detail shows winning structure, all participating sources, and rationale.
- Source accordion shows agree/dissent indicators and structure diffs.
- Manual Override section allows building a new section structure with drag-and-drop.
- Override submission creates a new vote log entry and updates the UI.
- Section Structure Editor validates constraints (requires Pallavi, valid ordering).
- Role-based visibility: Override section only visible to Admin/Reviewer.

## 6. Dependencies
- TRACK-044 (StructureVisualiser, TierBadge, MetricCard, DataGrid).
- TRACK-045 §3.4 (Structural Voting API endpoints).
- TRACK-049 (Source Evidence Browser — for cross-linking from conflict fields).

## 7. Progress Log
- **2026-02-09**: Track created based on Sourcing UI/UX Plan §4.7, §4.8.
- **2026-02-09**: Implementation complete:
  - Implemented `StructuralVotingPage.tsx` with voting stats MetricCards (total decisions, auto-consensus, manual overrides, avg confidence), paginated decisions table with consensus type badges, StructureVisualiser (compact), confidence bars, and navigation to detail pages.
