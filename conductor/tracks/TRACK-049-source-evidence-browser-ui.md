| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-09 |
| **Author** | Sangeetha Grantha Team |

# TRACK-049: Source Evidence Browser UI

## 1. Objective
Implement the Source Evidence Browser — the per-Krithi provenance view showing which sources contributed which fields, with confidence scores and the ability to compare field values side-by-side across sources. This is the core "Provenance Transparency" feature.

## 2. Context
- **Reference**: [Sourcing UI/UX Plan](../../application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md) — §4.6 (Source Evidence Browser), §4.6.1 (Krithi Evidence Detail)
- **Parent Track**: [TRACK-041](./TRACK-041-enhanced-sourcing-logic.md)
- **Phase**: Phase 3 (Sprint 5–6) — Provenance & Voting
- **Backend Dependency**: TRACK-045 §3.3 (Source Evidence API)

## 3. Implementation Plan

### 3.1 Source Evidence List View (§4.6)
- [ ] Create `SourceEvidencePage.tsx` at route `/admin/sourcing/evidence`.
- [ ] Implement header: Title "Source Evidence", search bar (Krithi title typeahead).
- [ ] Implement filter bar:
  - Minimum source count (number input, e.g., "show only Krithis with 2+ sources").
  - Source tier filter (multi-select TierBadge checkboxes).
  - Extraction method filter (multi-select).
  - Contributed field filter (multi-select: title, raga, tala, sections, lyrics, deity, temple, notation).
- [ ] Implement Krithi-centric data table:

  | Column | Description | Sort |
  |:---|:---|:---|
  | Krithi | Title + Raga + Tala summary | Yes |
  | Sources | Count with TierBadge indicators | Yes |
  | Top Source | Highest-tier contributing source name | Yes |
  | Contributed Fields | Aggregate field pill badges across all sources | No |
  | Avg Confidence | Mean confidence (ConfidenceBar) | Yes |
  | Voting Status | Latest voting consensus type badge or "No Vote" | Yes |
  | Actions | "View Evidence" | No |

- [ ] Connect to `GET /v1/admin/sourcing/evidence` with TanStack Query.
- [ ] Table sorting and pagination.
- [ ] Row click or "View Evidence" navigates to Krithi Evidence Detail.

### 3.2 Krithi Evidence Detail (§4.6.1)
- [ ] Create `KrithiEvidenceDetail.tsx` — implement as either inline expansion or dedicated modal (evaluate UX during development; default to modal for more space).
- [ ] Implement header: Krithi title, Raga, Tala, Composer, current workflow state badge.
- [ ] **Evidence Cards** (one per source, ordered by tier — highest first):
  - Source Header: Source name, TierBadge, FormatPill, extraction method.
  - Source URL (clickable external link).
  - Confidence (ConfidenceBar).
  - Contributed Fields (checklist with green checkmarks for contributed fields).
  - Field Values (two-column layout: Field Name → Extracted Value).
  - Extraction Date.
  - Raw Extraction (collapsible JsonViewer).
- [ ] Connect to `GET /v1/admin/sourcing/evidence/krithi/:id`.

### 3.3 Field Comparison View (§4.6.1 — Compare mode)
- [ ] Implement "Compare" toggle button on Krithi Evidence Detail.
- [ ] When active, render the **FieldComparisonTable** component:
  - Multi-column diff table: each column is a source, each row is a field.
  - Cell colour-coding:
    - Green: all sources agree.
    - Amber: variation (different wording, same meaning).
    - Red: conflict (different values).
  - Fields with conflicts show a link to the relevant Structural Voting decision.
- [ ] Connect to `GET /v1/admin/sourcing/evidence/compare/:id`.
- [ ] Ensure the comparison table scrolls horizontally when >3 sources.

### 3.4 Loading & Error States
- [ ] Skeleton loaders for evidence list and detail sections.
- [ ] Empty state: "No source evidence found — evidence is created during extraction and import."
- [ ] Error boundary with retry.

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Evidence Browser page | `modules/frontend/sangita-admin-web/src/pages/sourcing/SourceEvidencePage.tsx` | List view |
| Krithi Evidence Detail | `modules/frontend/sangita-admin-web/src/components/sourcing/KrithiEvidenceDetail.tsx` | Detail modal/expansion |
| Evidence Card | `modules/frontend/sangita-admin-web/src/components/sourcing/EvidenceCard.tsx` | Per-source evidence card |
| Evidence filter bar | `modules/frontend/sangita-admin-web/src/components/sourcing/EvidenceFilterBar.tsx` | Filter controls |

## 5. Acceptance Criteria
- Evidence list shows all Krithis with source counts, top source, and field summaries.
- Filters correctly narrow results (min source count, tier, fields).
- Krithi Evidence Detail displays all contributing sources ordered by tier.
- Each evidence card shows correct field values, confidence, and extraction metadata.
- Comparison view renders FieldComparisonTable with correct colour-coding.
- Conflict fields link to the corresponding Structural Voting decision.
- Search typeahead filters Krithis by title.

## 6. Dependencies
- TRACK-044 (TierBadge, FormatPill, ConfidenceBar, FieldComparisonTable, JsonViewer, DataGrid).
- TRACK-045 §3.3 (Source Evidence API endpoints).

## 7. Progress Log
- **2026-02-09**: Track created based on Sourcing UI/UX Plan §4.6, §4.6.1.
- **2026-02-09**: Implementation complete:
  - Implemented `SourceEvidencePage.tsx` with search filter, paginated evidence summary table, source count, confidence bar, and field conflict indicators.
