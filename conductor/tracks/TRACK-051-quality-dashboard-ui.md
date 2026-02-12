| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-09 |
| **Author** | Sangeetha Grantha Team |

# TRACK-051: Quality Dashboard UI

## 1. Objective
Implement the Corpus Quality Dashboard — the comprehensive quality metrics view showing KPIs, quality score distributions, enrichment phase progress, source tier coverage, composer coverage matrix, data gap analysis, and audit query results. This is the primary screen for Quality Auditors to monitor the overall data health posture.

## 2. Context
- **Reference**: [Sourcing UI/UX Plan](../../application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md) — §4.9 (Quality Dashboard)
- **Parent Track**: [TRACK-041](./TRACK-041-enhanced-sourcing-logic.md)
- **Phase**: Phase 4 (Sprint 7–8) — Quality & Insights
- **Backend Dependencies**: TRACK-045 §3.5 (Quality Dashboard API), TRACK-039 (audit queries)

## 3. Implementation Plan

### 3.1 KPI Cards (Row 1)
- [ ] Create `QualityDashboardPage.tsx` at route `/admin/sourcing/quality`.
- [ ] Implement header: Title "Corpus Quality Dashboard", last audit run timestamp, "Run Audit Now" button.
- [ ] Implement 5 MetricCards:

  | Card | Metric | Description | API |
  |:---|:---|:---|:---|
  | Total Krithis | Count | Total canonical Krithis in the system | `GET /quality/summary` |
  | Multi-Source | Count (%) | Krithis with 2+ contributing sources | `GET /quality/summary` |
  | Structural Consensus | Count (%) | Krithis with HIGH confidence voting | `GET /quality/summary` |
  | Avg Quality Score | Number (0–1) | Mean quality score across all Krithis | `GET /quality/summary` |
  | Enrichment Coverage | % | Krithis with deity + temple + 2+ language variants | `GET /quality/summary` |

### 3.2 Quality Score Distribution + Source Tier Coverage (Row 2)
- [ ] **Quality Distribution** (left 50%):
  - Histogram of quality scores in buckets (0–0.2, 0.2–0.4, 0.4–0.6, 0.6–0.8, 0.8–1.0).
  - Bars colour-coded: red (0–0.2), orange (0.2–0.4), amber (0.4–0.6), yellow-green (0.6–0.8), green (0.8–1.0).
  - Connect to `GET /v1/admin/sourcing/quality/distribution`.
  - Consider using a lightweight charting library (e.g., recharts, visx, or Chart.js).
- [ ] **Source Tier Coverage** (right 50%):
  - Stacked bar chart showing how many Krithis have at least one source from each tier.
  - Bars colour-coded by tier (using TierBadge colours).
  - Connect to `GET /v1/admin/sourcing/quality/coverage`.

### 3.3 Enrichment Phase Progress (Row 3)
- [ ] Full-width section with 6 **ProgressBarRow** components:
  - Phase 0: Foundation & Quality Baseline.
  - Phase 1: PDF Ingestion — Skeleton Extraction.
  - Phase 2: Structural Validation & Voting.
  - Phase 3: Lyric Enrichment.
  - Phase 4: Metadata Enrichment.
  - Phase 5: Notation Ingestion.
- [ ] Each bar shows: target count, completed count, in-progress count, percentage.
- [ ] Connect to `GET /v1/admin/sourcing/quality/coverage` (phase progress data).

### 3.4 Data Gaps & Priorities + Composer Coverage Matrix (Row 4)
- [ ] **Data Gaps** (left 60%):
  - Table of top enrichment priorities:
    - Krithis with no lyrics (count).
    - Krithis with single-language lyrics only (count).
    - Krithis with no deity/temple (count).
    - Krithis with structural conflicts (LOW confidence voting) (count).
    - Krithis with no notation (Varnams/Swarajathis only) (count).
  - Each row is clickable, linking to a filtered view of the relevant Krithis (e.g., Evidence Browser filtered by field).
  - Connect to `GET /v1/admin/sourcing/quality/gaps`.
- [ ] **Composer Coverage Matrix** (right 40%):
  - HeatmapGrid component: Composer (rows) × Field (columns).
  - Fields: Title, Raga, Tala, Sections, Lyrics (Sa), Lyrics (En), Deity, Temple, Notation.
  - Cell values: coverage percentages (0–100%).
  - Colour scale: white (0%) → light green → dark green (100%).
  - Connect to `GET /v1/admin/sourcing/quality/coverage` (composer matrix data).

### 3.5 Audit Results (Row 5)
- [ ] Full-width collapsible section.
- [ ] Display results of TRACK-039 audit queries:
  - Section count mismatch across language variants.
  - Label sequence mismatch (non-canonical section ordering).
  - Orphaned lyric blobs (lyrics without section assignment).
- [ ] Each audit shows: query name, count of violations, trend indicator (arrow up/down/flat vs. last run), "View Details" link.
- [ ] Connect to `GET /v1/admin/sourcing/quality/audit`.
- [ ] "Run Audit Now" button triggers `POST /v1/admin/sourcing/quality/audit/run`, shows loading state, refreshes results on completion.

### 3.6 Chart Library Integration
- [ ] Evaluate and select a charting library for:
  - Histogram (quality distribution).
  - Stacked bar chart (tier coverage).
  - Heatmap (composer matrix).
  - Progress bars (enrichment phases).
- [ ] Candidate libraries: `recharts` (React-friendly, declarative), `visx` (low-level, Airbnb), or `chart.js` via `react-chartjs-2`.
- [ ] Ensure charts are responsive and render correctly on tablet/mobile.
- [ ] Add ARIA labels and descriptions to all charts for accessibility.

### 3.7 Loading & Error States
- [ ] Skeleton loaders for all sections (cards, charts, tables).
- [ ] Individual error handling per section.
- [ ] Empty states with helpful context ("No audit results — click 'Run Audit Now' to generate the first analysis").

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Quality Dashboard page | `modules/frontend/sangita-admin-web/src/pages/sourcing/QualityDashboardPage.tsx` | Main page |
| Quality Distribution chart | `modules/frontend/sangita-admin-web/src/components/sourcing/QualityDistributionChart.tsx` | Histogram |
| Tier Coverage chart | `modules/frontend/sangita-admin-web/src/components/sourcing/TierCoverageChart.tsx` | Stacked bar |
| Phase Progress section | `modules/frontend/sangita-admin-web/src/components/sourcing/PhaseProgressSection.tsx` | Progress bars |
| Data Gaps table | `modules/frontend/sangita-admin-web/src/components/sourcing/DataGapsTable.tsx` | Gap priorities |
| Composer Matrix | `modules/frontend/sangita-admin-web/src/components/sourcing/ComposerCoverageMatrix.tsx` | HeatmapGrid usage |
| Audit Results section | `modules/frontend/sangita-admin-web/src/components/sourcing/AuditResultsSection.tsx` | Audit query display |

## 5. Acceptance Criteria
- All 5 KPI cards display correct values from the backend.
- Quality Distribution histogram renders with correct bucket counts and colour coding.
- Tier Coverage stacked bar shows correct source tier distribution.
- Phase Progress bars show target/actual/percentage for all 6 enrichment phases.
- Data Gaps table shows accurate counts with working links to filtered views.
- Composer Coverage Matrix renders HeatmapGrid with correct percentages and colour scale.
- Audit Results display query results with trend indicators.
- "Run Audit Now" triggers an audit and refreshes results.
- All charts are responsive (resize gracefully on smaller viewports).
- Charts have ARIA labels for screen reader accessibility.

## 6. Dependencies
- TRACK-044 (MetricCard, ProgressBarRow, HeatmapGrid components).
- TRACK-045 §3.5 (Quality Dashboard API endpoints).
- TRACK-039 (audit queries must be defined and runnable from the backend).

## 7. Progress Log
- **2026-02-09**: Track created based on Sourcing UI/UX Plan §4.9.
- **2026-02-09**: Implementation complete:
  - Implemented `QualityDashboardPage.tsx` with 5 KPI MetricCards (total krithis, multi-source, consensus, avg quality, enrichment coverage).
  - Quality score distribution bar chart.
  - Source tier coverage bar chart.
  - Enrichment phase progress with ProgressBarRow components.
  - Data gaps & priorities section.
  - Audit results section with trend indicators.
  - "Run Audit Now" button with mutation hook.
