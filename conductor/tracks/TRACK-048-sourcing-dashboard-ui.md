| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-09 |
| **Author** | Sangeetha Grantha Team |

# TRACK-048: Sourcing Dashboard UI

## 1. Objective
Implement the Sourcing Dashboard — the high-level index page at `/admin/sourcing` providing at-a-glance pipeline health, recent activity, and quick-action shortcuts. This screen serves as the landing page for the entire Sourcing & Quality module.

## 2. Context
- **Reference**: [Sourcing UI/UX Plan](../../application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md) — §4.1 (Sourcing Dashboard)
- **Parent Track**: [TRACK-041](./TRACK-041-enhanced-sourcing-logic.md)
- **Phase**: Phase 2 (Sprint 3–4) — Monitoring & Operations
- **Backend Dependencies**: TRACK-045 §3.2 (Extraction stats), §3.4 (Voting stats), §3.5 (Quality summary)

## 3. Implementation Plan

### 3.1 Pipeline Health Cards (Row 1)
- [ ] Create `SourcingDashboardPage.tsx` at route `/admin/sourcing`.
- [ ] Implement header: Title "Sourcing & Quality", subtitle with last-updated timestamp.
- [ ] Implement 4 MetricCards across the top row:

  | Card | Metric | Visual | API Source |
  |:---|:---|:---|:---|
  | Sources | Total / Active sources | Count with tier breakdown sparkline | `GET /sources` (count) |
  | Extraction Queue | Pending / Processing / Done / Failed | Stacked bar with status colours | `GET /extractions/stats` |
  | Voting Decisions | Total decisions, % requiring manual review | Donut chart (UNANIMOUS / MAJORITY / AUTHORITY_OVERRIDE / MANUAL) | `GET /voting/stats` |
  | Corpus Quality | Mean quality score, coverage % | Gauge chart with target indicator | `GET /quality/summary` |

- [ ] Each MetricCard is clickable, navigating to its respective detail screen.
- [ ] Implement sparkline, stacked bar, donut, and gauge micro-charts (consider lightweight charting library or inline SVG).

### 3.2 Recent Activity Feed + Top Sources (Row 2)
- [ ] **Activity Feed** (left 60%):
  - Chronological list of recent events: extraction completions, voting decisions, source registry changes.
  - Each entry shows: timestamp, event type icon, description, link to detail screen.
  - Limit to most recent 20 entries.
  - Consider a dedicated `GET /v1/admin/sourcing/activity` endpoint or aggregate from existing audit_log.
- [ ] **Top Sources by Volume** (right 40%):
  - Horizontal bar chart of sources ranked by total contributed Krithis.
  - Bars colour-coded by tier (using TierBadge colours).
  - Clicking a bar navigates to Source Detail.

### 3.3 Quick Actions (Row 3)
- [ ] "Start New Extraction" button → opens Extraction Request Wizard (from TRACK-047).
- [ ] "Run Quality Audit" button → triggers `POST /quality/audit/run` and navigates to Quality Dashboard.
- [ ] "View Pending Reviews" button → navigates to Structural Voting filtered to `MANUAL` decisions awaiting review.
- [ ] Button styling: primary, secondary, secondary — with descriptive icons.

### 3.4 Data Fetching & Refresh
- [ ] Use TanStack Query with parallel queries for all 4 metric data sources.
- [ ] Auto-refresh dashboard every 30 seconds.
- [ ] Stale-while-revalidate strategy for smooth UX.

### 3.5 Loading & Error States
- [ ] Skeleton loaders for all MetricCards, activity feed, and chart areas.
- [ ] Individual error handling per section (one failed query doesn't block others).
- [ ] Empty states with helpful onboarding messages for first-time users.

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Dashboard page | `modules/frontend/sangita-admin-web/src/pages/sourcing/SourcingDashboardPage.tsx` | Index page |
| Activity Feed | `modules/frontend/sangita-admin-web/src/components/sourcing/ActivityFeed.tsx` | Recent events list |
| TopSourcesChart | `modules/frontend/sangita-admin-web/src/components/sourcing/TopSourcesChart.tsx` | Bar chart component |
| Dashboard MetricCards | `modules/frontend/sangita-admin-web/src/components/sourcing/DashboardMetrics.tsx` | Metric card row with micro-charts |

## 5. Acceptance Criteria
- Dashboard loads and displays all 4 metric cards with live data.
- Each metric card navigates to its respective detail screen on click.
- Activity feed shows recent events chronologically with correct links.
- Top Sources chart renders source volumes ranked and colour-coded by tier.
- Quick action buttons work correctly (open wizard, trigger audit, navigate to voting).
- Auto-refresh works without visual flicker.
- Dashboard is responsive: 4 cards → 2×2 on tablet → stacked on mobile.

## 6. Dependencies
- TRACK-044 (MetricCard, TierBadge, StatusChip components).
- TRACK-045 §3.2, §3.4, §3.5 (stats and summary API endpoints).
- TRACK-047 (Extraction Request Wizard — for "Start New Extraction" quick action).

## 7. Progress Log
- **2026-02-09**: Track created based on Sourcing UI/UX Plan §4.1.
- **2026-02-09**: Implementation complete:
  - Implemented `SourcingDashboardPage.tsx` with KPI MetricCards from live data (source count, extraction stats, voting stats, quality summary).
  - Added recent activity and quick stats sections.
  - Added quick action buttons navigating to Source Registry, Extraction Monitor, and Quality Dashboard.
