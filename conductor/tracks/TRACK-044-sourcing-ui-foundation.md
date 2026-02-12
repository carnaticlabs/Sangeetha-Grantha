| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-09 |
| **Author** | Sangeetha Grantha Team |

# TRACK-044: Sourcing UI Foundation & Design System Components

## 1. Objective
Establish the routing scaffold, navigation integration, layout shell, and all shared Design System components required by the Sourcing & Extraction Monitoring module. This track is the prerequisite for all subsequent sourcing UI tracks (TRACK-046 through TRACK-052).

## 2. Context
- **Reference**: [Sourcing UI/UX Plan](../../application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md) — §3 (Sitemap), §6 (Components), §8 (Accessibility)
- **Parent Track**: [TRACK-041](./TRACK-041-enhanced-sourcing-logic.md) — Enhanced Sourcing Logic & Structural Voting
- **Companion**: [Bulk Import UI/UX Plan](../../application_documentation/01-requirements/features/bulk-import/ui-ux-plan.md) — reusable components from existing Bulk Import module
- **Phase**: Phase 1 (Sprint 1–2) — Foundation

## 3. Implementation Plan

### 3.1 Route Scaffolding & Navigation
- [ ] Create route prefix `/admin/sourcing` with nested route definitions for all 9 screens.
- [ ] Add "Sourcing & Quality" entry to the Admin Console navigation menu (under "Data Management", below "Bulk Import").
- [ ] Create `SourcingLayout` wrapper component with shared header, sidebar, and breadcrumb navigation.
- [ ] Implement route-level code splitting (lazy loading) for each sourcing screen.
- [ ] Create placeholder pages for all 9 routes returning skeleton content.

### 3.2 Shared Design System Components (§6)

#### 3.2.1 New Components
- [ ] **TierBadge** — Colour-coded badge for source authority tiers (T1=gold, T2=silver, T3=bronze, T4=blue, T5=grey). Includes tooltip with tier definition. Props: `tier: 1|2|3|4|5`, `size?: 'sm'|'md'|'lg'`.
- [ ] **FormatPill** — Small pill badges for document formats (PDF, HTML, DOCX, API, MANUAL). Props: `format: string`, colour mapping per format.
- [ ] **ConfidenceBar** — Horizontal bar (0–1) with gradient fill (red→amber→green). Props: `value: number`, `showLabel?: boolean`, `size?: 'sm'|'md'`.
- [ ] **StructureVisualiser** — Coloured block diagram showing Krithi section structure (P=blue, A=green, C=amber, SC=secondary, CS=purple). Props: `sections: SectionSummary[]`, `compact?: boolean`.
- [ ] **FieldComparisonTable** — Multi-column diff table for comparing field values across sources. Colour-coded cells: green (agreement), amber (variation), red (conflict). Props: `sources: SourceFieldData[]`, `fields: string[]`.
- [ ] **TimelineCard** — Vertical timeline showing state transitions with timestamps and durations. Props: `events: TimelineEvent[]`.
- [ ] **MetricCard** — Summary card with label, primary value, secondary metric, and optional sparkline/icon. Props: `label: string`, `value: string|number`, `subtitle?: string`, `icon?: ReactNode`, `trend?: 'up'|'down'|'flat'`, `onClick?: () => void`.
- [ ] **HeatmapGrid** — Grid visualisation for Composer × Field coverage matrix. Props: `rows: string[]`, `columns: string[]`, `data: number[][]`, `colorScale?: ColorScale`.
- [ ] **ProgressBarRow** — Labelled horizontal progress bar with target/actual/percentage for phase tracking. Props: `label: string`, `target: number`, `actual: number`, `inProgress?: number`.
- [ ] **JsonViewer** — Collapsible, syntax-highlighted JSON viewer with copy-to-clipboard. Extend from existing LogViewer if applicable. Props: `data: object|string`, `collapsed?: boolean`, `maxHeight?: number`.

#### 3.2.2 Extended Components
- [ ] **StatusChip** — Extend existing Bulk Import StatusChip with extraction-specific statuses: `PENDING`, `PROCESSING`, `DONE`, `FAILED`, `CANCELLED`. Add pulse animation for `PROCESSING` state.
- [ ] **DataGrid** — Verify reuse of virtualised table from Bulk Import. Ensure sorting, filtering, and pagination props are compatible with sourcing data shapes.

### 3.3 TypeScript Types & API Client Foundation
- [ ] Define TypeScript interfaces for all sourcing domain models:
  - `ImportSource`, `SourceDetail`, `CreateSourceRequest`, `UpdateSourceRequest`
  - `ExtractionTask`, `ExtractionDetail`, `CreateExtractionRequest`
  - `SourceEvidence`, `KrithiEvidenceDetail`, `FieldComparison`
  - `VotingDecision`, `VotingDetail`, `ManualOverrideRequest`
  - `QualitySummary`, `QualityDistribution`, `CoverageData`, `GapAnalysis`
- [ ] Create API client module `sourcingApi.ts` with typed fetch functions for all §5 endpoints (stubs returning mock data until backend is ready).
- [ ] Set up TanStack Query hooks file `useSourcingQueries.ts` with query keys and basic hooks.

### 3.4 Accessibility & Responsiveness Foundation
- [ ] Ensure all new components meet WCAG AA contrast ratios.
- [ ] Add ARIA labels and roles to MetricCard, TierBadge, StatusChip.
- [ ] Implement responsive breakpoint utilities (metric cards: 4-col → 2×2 → single-col).
- [ ] Create skeleton loader variants for MetricCard, DataGrid rows, and TimelineCard.
- [ ] Implement error boundary component for sourcing module with retry action.

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Route definitions | `modules/frontend/sangita-admin-web/src/routes/sourcing/` | Route config and lazy-loaded page shells |
| SourcingLayout | `modules/frontend/sangita-admin-web/src/components/sourcing/SourcingLayout.tsx` | Shared layout wrapper |
| Design System components | `modules/frontend/sangita-admin-web/src/components/sourcing/shared/` | All 12 components from §6 |
| TypeScript types | `modules/frontend/sangita-admin-web/src/types/sourcing.ts` | Domain model interfaces |
| API client | `modules/frontend/sangita-admin-web/src/api/sourcingApi.ts` | Typed API functions |
| Query hooks | `modules/frontend/sangita-admin-web/src/hooks/useSourcingQueries.ts` | TanStack Query hooks |

## 5. Acceptance Criteria
- All 9 routes are navigable and render placeholder content.
- "Sourcing & Quality" appears in the Admin Console nav menu.
- All 12 shared components render correctly with sample props (visual review).
- TypeScript types compile without errors.
- Skeleton loaders display during loading states.
- Components are keyboard-navigable and have appropriate ARIA attributes.

## 6. Dependencies
- TRACK-041 (backend database migrations and seed data must be applied).
- Existing Bulk Import components (StatusChip, DataGrid, LogViewer) must be importable.

## 7. Progress Log
- **2026-02-09**: Track created based on Sourcing UI/UX Plan §3, §6, §8.
- **2026-02-09**: Implementation complete:
  - Created route prefix `/admin/sourcing` with nested routes for all 9 screens.
  - Added "Sourcing & Quality" entry to Admin Console navigation menu.
  - Created `SourcingLayout` wrapper with sub-navigation tabs and breadcrumbs.
  - Implemented route-level code splitting (lazy loading) for all sourcing pages.
  - Created placeholder pages for all routes, upgraded to functional implementations.
  - Created all 12 shared Design System components: TierBadge, FormatPill, ConfidenceBar, StatusChip, MetricCard, StructureVisualiser, TimelineCard, FieldComparisonTable, HeatmapGrid, ProgressBarRow, JsonViewer, SourcingErrorBoundary.
  - Defined TypeScript interfaces for all sourcing domain models in `types/sourcing.ts`.
  - Created API client module `sourcingApi.ts` with typed functions for all endpoints.
  - Created TanStack Query hooks `useSourcingQueries.ts` with query key factory and all CRUD hooks.
  - Added `ViewState.SOURCING` to enum and Sidebar navigation.
