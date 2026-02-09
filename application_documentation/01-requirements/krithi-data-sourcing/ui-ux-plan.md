| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |
| **Related Tracks** | TRACK-041 |
| **Parent** | [Krithi Data Sourcing & Quality](./README.md) |
| **Companion** | [Bulk Import UI/UX Plan](../features/bulk-import/ui-ux-plan.md) |

# Sourcing & Extraction Monitoring — UI/UX Plan

## 1. Overview

This document specifies the User Interface and User Experience requirements for the **Sourcing & Extraction Monitoring** feature set. These screens give Admin and IngestionOps users visibility into the multi-source, multi-format data pipeline introduced by TRACK-041 — covering source authority management, extraction queue monitoring, per-Krithi provenance tracking, structural voting audit, and corpus-wide quality metrics.

The Bulk Import module handles *batch orchestration* (CSV manifests → scrape → resolve → review). This module complements it by handling *source-level governance* — which sources exist, how authoritative they are, what has been extracted, how structural conflicts were resolved, and what the overall data quality posture looks like.

### 1.1 Goals

1. **Operational Visibility**: Real-time monitoring of the extraction queue (Kotlin ↔ Python pipeline) so operators can spot failures, retries, and bottlenecks.
2. **Provenance Transparency**: For any Krithi, show exactly which sources contributed which fields, enabling editorial confidence.
3. **Governance & Control**: Manage the source registry (tiers, formats, composer affinities) and intervene in structural voting decisions when automated consensus is insufficient.
4. **Quality Insight**: Corpus-wide dashboards showing quality score distributions, coverage gaps, and enrichment progress across the 6-phase strategy.

---

## 2. User Personas

| Persona | Description | Primary Screens |
|:---|:---|:---|
| **System Admin** | Manages source registry, monitors extraction queue health, intervenes on stuck/failed tasks, reviews infrastructure metrics. | Source Registry, Extraction Monitor, Quality Dashboard |
| **IngestionOps** | Operates extraction campaigns, tracks per-source harvest progress, reviews extraction results before they enter the import pipeline. | Extraction Monitor, Source Evidence Browser |
| **Content Editor / Data Reviewer** | Reviews structural voting outcomes, resolves low-confidence consensus decisions, inspects per-Krithi provenance before publication. | Source Evidence Browser, Structural Voting Viewer |
| **Quality Auditor** | Monitors corpus-wide quality metrics, identifies coverage gaps, prioritises enrichment targets. | Quality Dashboard, Source Evidence Browser |

---

## 3. Sitemap & Navigation

The Sourcing module will be located within the existing Admin Console, peer to the Bulk Import module.

- **Route Prefix**: `/admin/sourcing`
- **Navigation Menu**: "Sourcing & Quality" (under "Data Management" section, below "Bulk Import")

**Hierarchy:**

| # | Screen | Route | Description |
|:---|:---|:---|:---|
| 1 | Sourcing Dashboard | `/admin/sourcing` | High-level overview with key metrics |
| 2 | Source Registry | `/admin/sourcing/sources` | Manage import sources and authority tiers |
| 3 | Source Detail | `/admin/sourcing/sources/:id` | Deep dive into a single source |
| 4 | Extraction Monitor | `/admin/sourcing/extractions` | Real-time extraction queue monitoring |
| 5 | Extraction Detail | `/admin/sourcing/extractions/:id` | Single extraction task detail |
| 6 | Source Evidence Browser | `/admin/sourcing/evidence` | Per-Krithi provenance view |
| 7 | Structural Voting | `/admin/sourcing/voting` | Voting decision audit trail |
| 8 | Voting Detail | `/admin/sourcing/voting/:id` | Single voting decision detail |
| 9 | Quality Dashboard | `/admin/sourcing/quality` | Corpus-wide quality metrics |

---

## 4. Key Screens & Detailed UX

### 4.1. Sourcing Dashboard (Index)

**Goal**: At-a-glance health of the entire sourcing pipeline.

**Route**: `/admin/sourcing`

**Layout:**

- **Header**: Title "Sourcing & Quality", subtitle with last-updated timestamp.

- **Row 1: Pipeline Health Cards** (4 metric cards across the top):

  | Card | Metric | Visual |
  |:---|:---|:---|
  | Sources | Total sources / Active sources | Count with tier breakdown sparkline |
  | Extraction Queue | Pending / Processing / Done / Failed | Stacked bar with status colours |
  | Voting Decisions | Total decisions, % requiring manual review | Donut chart (UNANIMOUS / MAJORITY / AUTHORITY_OVERRIDE / MANUAL) |
  | Corpus Quality | Mean quality score, coverage % | Gauge chart with target indicator |

- **Row 2: Recent Activity Feed** (left 60%) + **Top Sources by Volume** (right 40%):
  - **Activity Feed**: Chronological list of recent extraction completions, voting decisions, and source registry changes. Each entry shows: timestamp, event type icon, description, and a link to the relevant detail screen.
  - **Top Sources**: Horizontal bar chart of sources ranked by total contributed Krithis, colour-coded by tier.

- **Row 3: Quick Actions**:
  - Button: "Start New Extraction" → opens Extraction Request form.
  - Button: "Run Quality Audit" → triggers audit queries and navigates to Quality Dashboard.
  - Button: "View Pending Reviews" → navigates to Structural Voting filtered to `MANUAL` decisions awaiting review.

**Interactions:**
- Each metric card is clickable, navigating to the corresponding detail screen.
- Activity feed items link to their respective detail pages.

---

### 4.2. Source Registry

**Goal**: Manage the authoritative source catalogue with tier rankings, format capabilities, and composer affinities.

**Route**: `/admin/sourcing/sources`

**Layout:**

- **Header**: Title "Source Registry", Button "Register New Source".
- **Filters**:
  - Tier (1–5, multi-select checkboxes)
  - Supported Formats (HTML, PDF, DOCX, API, MANUAL — multi-select)
  - Status (Active / Inactive toggle)
  - Search (by name or URL)

- **Table**:

  | Column | Description | Sort |
  |:---|:---|:---|
  | Name | Source display name (e.g., "guruguha.org") | Yes |
  | URL | Base URL | No |
  | Tier | Authority tier badge (colour-coded: T1 = gold, T2 = silver, T3 = bronze, T4 = blue, T5 = grey) | Yes |
  | Formats | Pill badges for each supported format | No |
  | Composers | Top composer affinities (e.g., "Dikshitar 1.0, Swathi Thirunal 0.8") | No |
  | Krithis | Count of Krithis sourced from this source | Yes |
  | Last Harvested | Relative timestamp (e.g., "3 days ago") | Yes |
  | Actions | "View", "Edit", "Deactivate" | No |

**Interactions:**
- Clicking a row navigates to **Source Detail** (`/admin/sourcing/sources/:id`).
- "Register New Source" opens a modal form (see §4.2.1).
- Tier badges have tooltips explaining each tier level.

#### 4.2.1. Register / Edit Source Form (Modal)

**Fields:**

| Field | Type | Required | Validation |
|:---|:---|:---|:---|
| Name | Text | Yes | Unique, non-empty |
| Base URL | URL | Yes | Valid URL format |
| Tier | Select (1–5) | Yes | — |
| Supported Formats | Multi-select | Yes | At least one |
| Composer Affinities | Repeatable key-value (Composer → Weight 0.0–1.0) | No | Weight between 0 and 1 |
| Description | Textarea | No | Max 500 chars |
| Active | Toggle | Yes | Default: On |

---

### 4.3. Source Detail

**Goal**: Deep dive into a single source — its harvest history, contributed Krithis, and extraction activity.

**Route**: `/admin/sourcing/sources/:id`

**Layout:**

- **Header**: Source name, Tier badge, Format pills, "Edit" and "Deactivate" buttons.

- **Section A: Source Profile** (Card)
  - Base URL (clickable external link)
  - Description
  - Composer affinities (visual weight bars)
  - Registered date
  - Last harvested timestamp

- **Section B: Contribution Summary** (Metrics Row)
  - Total Krithis contributed
  - Fields contributed breakdown (pie chart: title, raga, tala, sections, lyrics, deity, temple, notation)
  - Average confidence score
  - Extraction success rate

- **Section C: Extraction History** (Table)
  - Filterable/sortable list of all extraction queue entries for this source.
  - Columns: Task ID, Format, Status, Krithi Count, Confidence, Duration, Created At.
  - Clicking a row navigates to Extraction Detail.

- **Section D: Contributed Krithis** (Table)
  - List of all Krithis with evidence from this source (joined from `krithi_source_evidence`).
  - Columns: Krithi Title, Raga, Tala, Contributed Fields (pills), Confidence, Extracted At.
  - Clicking a row navigates to the Krithi editor with the Source Evidence tab active.

---

### 4.4. Extraction Monitor

**Goal**: Real-time operational view of the extraction queue — the Kotlin ↔ Python integration pipeline.

**Route**: `/admin/sourcing/extractions`

**Layout:**

- **Header**: Title "Extraction Queue", Buttons: "New Extraction Request", "Retry All Failed".

- **Status Summary Bar** (horizontal):
  - Five status segments showing counts: `PENDING` (grey), `PROCESSING` (blue pulse), `DONE` (green), `FAILED` (red), `CANCELLED` (dark grey).
  - Total count and throughput metric (extractions/hour over last 24h).

- **Filters**:
  - Status (multi-select: PENDING, PROCESSING, DONE, FAILED, CANCELLED)
  - Source Format (PDF, DOCX, IMAGE)
  - Source Name (typeahead)
  - Date Range (created_at)
  - Batch (linked import batch, if any)

- **Table**:

  | Column | Description | Sort |
  |:---|:---|:---|
  | ID | Extraction task ID (truncated UUID with copy button) | No |
  | Source | Source name + tier badge | Yes |
  | Format | Format pill (PDF / DOCX / IMAGE) | Yes |
  | URL | Source URL (truncated, external link icon) | No |
  | Status | Status chip with pulse animation for PROCESSING | Yes |
  | Krithis | Result count (number of Krithis extracted) | Yes |
  | Confidence | Confidence score bar (0–1) | Yes |
  | Duration | Processing time (human-readable, e.g., "4.2s") | Yes |
  | Attempts | Current attempt / max attempts | No |
  | Worker | Claimed-by hostname (for PROCESSING tasks) | No |
  | Created | Relative timestamp | Yes |
  | Actions | "View", "Retry" (if FAILED), "Cancel" (if PENDING/PROCESSING) | No |

- **Auto-Refresh**: Table polls every 10 seconds for status updates. Active PROCESSING rows have a subtle pulse animation.

**Interactions:**
- Clicking a row navigates to **Extraction Detail**.
- "New Extraction Request" opens the extraction request wizard (see §4.4.1).
- "Retry All Failed" opens a confirmation dialog showing the count of failed tasks.
- Failed rows are highlighted with a red left-border accent.

#### 4.4.1. New Extraction Request Wizard (Modal)

**Goal**: Submit a new extraction task to the queue.

**Step 1 — Source Selection:**
- Select an existing source from the registry (dropdown with tier badges).
- Or enter a one-off URL with format selection.

**Step 2 — Extraction Parameters:**

| Field | Type | Required | Notes |
|:---|:---|:---|:---|
| Source URL | URL | Yes | Pre-filled if source selected |
| Format | Select (PDF, DOCX, IMAGE) | Yes | Pre-filled from source supported formats |
| Page Range | Text | No | For PDFs, e.g., "1-10" or "42-43" |
| Composer Hint | Typeahead (Composer) | No | Helps parser with attribution |
| Expected Krithi Count | Number | No | For validation of extraction results |
| Link to Import Batch | Select (Batch) | No | Associates extraction with existing batch |
| Max Attempts | Number | No | Default: 3 |

**Step 3 — Confirmation:**
- Summary of parameters.
- "Submit to Queue" button.
- System creates `extraction_queue` row with `PENDING` status and navigates to the Extraction Detail.

---

### 4.5. Extraction Detail

**Goal**: Full detail of a single extraction task — parameters, progress, results, and errors.

**Route**: `/admin/sourcing/extractions/:id`

**Layout:**

- **Header**: Task ID, Status chip (large), Format badge, Source name with tier badge.

- **Section A: Request Parameters** (Card)
  - Source URL (clickable)
  - Source format, tier, and name
  - Page range (if applicable)
  - Composer hint
  - Request payload (collapsible JSON viewer)
  - Linked import batch (if any, clickable link to Batch Detail)

- **Section B: Processing Status** (Timeline Card)
  - Visual timeline showing state transitions:
    - Created → Claimed → Completed/Failed
  - Each state shows timestamp, duration in state, and actor (worker hostname for PROCESSING).
  - Attempt counter with per-attempt details if retried.

- **Section C: Extraction Results** (shown only when `DONE`)
  - **Summary Row**: Krithi count, overall confidence, extraction method, extractor version, duration.
  - **Results Table**: One row per extracted Krithi:

    | Column | Description |
    |:---|:---|
    | Title | Extracted Krithi title |
    | Raga | Extracted Raga name |
    | Tala | Extracted Tala name |
    | Composer | Extracted composer |
    | Sections | Section count with structure summary (e.g., "P + A + 2C") |
    | Languages | Detected languages/scripts |
    | Confidence | Per-Krithi confidence bar |
    | Actions | "Preview" (opens JSON), "Import" (sends to import pipeline) |

  - **Raw Payload**: Collapsible JSON viewer showing the full `result_payload` (array of `CanonicalExtractionDto`).

- **Section D: Error Details** (shown only when `FAILED`)
  - Error summary with categorisation (network, parse, OCR, timeout).
  - Structured error detail (collapsible JSON from `error_detail`).
  - Last error timestamp.
  - "Retry" button (if attempts < max_attempts).
  - "Cancel" button.

- **Section E: Audit Trail** (Collapsible)
  - Source checksum (SHA-256).
  - Cached artifact path.
  - Created/updated timestamps.

---

### 4.6. Source Evidence Browser

**Goal**: For any Krithi, show all sources that contributed data, what each source provided, and how confident the system is in each contribution.

**Route**: `/admin/sourcing/evidence`

**Layout:**

- **Header**: Title "Source Evidence", search bar (Krithi title typeahead).
- **Filters**:
  - Minimum source count (e.g., "show only Krithis with 2+ sources")
  - Source tier filter
  - Extraction method filter
  - Contributed field filter (e.g., "has lyrics contributions")

- **Table** (Krithi-centric):

  | Column | Description | Sort |
  |:---|:---|:---|
  | Krithi | Title + Raga + Tala summary | Yes |
  | Sources | Count of contributing sources with tier badges | Yes |
  | Top Source | Highest-tier contributing source name | Yes |
  | Contributed Fields | Aggregate field pills across all sources | No |
  | Avg Confidence | Mean confidence across all source evidence | Yes |
  | Voting Status | Latest voting consensus type badge (or "No Vote") | Yes |
  | Actions | "View Evidence" | No |

**Interactions:**
- Clicking "View Evidence" or a row navigates to a **Krithi Evidence Detail** view (inline expansion or dedicated route).

#### 4.6.1. Krithi Evidence Detail (Inline Expandable or Modal)

**Goal**: Side-by-side comparison of what each source contributed for a single Krithi.

**Layout:**

- **Header**: Krithi title, Raga, Tala, Composer, current workflow state badge.

- **Evidence Cards** (one per source, ordered by tier — highest first):

  Each card contains:

  | Element | Description |
  |:---|:---|
  | Source Header | Source name, tier badge, format pill, extraction method |
  | Source URL | Clickable link to original source |
  | Confidence | Confidence bar (0–1) |
  | Contributed Fields | Checked list of fields contributed |
  | Field Values | Two-column view: Field Name → Extracted Value |
  | Extraction Date | When this source was extracted |
  | Raw Extraction | Collapsible JSON viewer |

- **Comparison View** (toggled via "Compare" button):
  - Multi-column diff table where each column is a source and each row is a field.
  - Cells are colour-coded: green (agreement), amber (variation), red (conflict).
  - Fields with conflicts show a link to the relevant structural voting decision.

---

### 4.7. Structural Voting Viewer

**Goal**: Audit trail of all cross-source structural voting decisions, with the ability to manually intervene on low-confidence outcomes.

**Route**: `/admin/sourcing/voting`

**Layout:**

- **Header**: Title "Structural Voting", Button "Pending Manual Reviews" (badge with count).

- **Summary Cards** (top row):
  - Total decisions
  - UNANIMOUS count (%)
  - MAJORITY count (%)
  - AUTHORITY_OVERRIDE count (%)
  - SINGLE_SOURCE count (%)
  - MANUAL count (%)

- **Filters**:
  - Consensus Type (multi-select)
  - Confidence (HIGH / MEDIUM / LOW)
  - Date Range (voted_at)
  - Has Dissenting Sources (toggle)
  - Pending Manual Review (toggle)

- **Table**:

  | Column | Description | Sort |
  |:---|:---|:---|
  | Krithi | Krithi title (linked) | Yes |
  | Voted At | Timestamp | Yes |
  | Sources | Count of participating sources | Yes |
  | Consensus | Consensus type badge (colour-coded) | Yes |
  | Structure | Winning structure summary (e.g., "P + A + 3C + CS") | No |
  | Confidence | Confidence badge (HIGH = green, MEDIUM = amber, LOW = red) | Yes |
  | Dissents | Count of dissenting sources | Yes |
  | Reviewer | Reviewer name (if MANUAL) or "—" | No |
  | Actions | "View Detail", "Override" (if not MANUAL) | No |

**Interactions:**
- "Pending Manual Reviews" filters to entries where `consensus_type = 'MANUAL'` and `reviewer_id IS NULL`.
- Clicking a row navigates to **Voting Detail**.

---

### 4.8. Voting Detail

**Goal**: Full transparency into how a structural consensus was reached for a specific Krithi.

**Route**: `/admin/sourcing/voting/:id`

**Layout:**

- **Header**: Krithi title, voting timestamp, consensus type badge, confidence badge.

- **Section A: Winning Structure** (Card, highlighted with green border)
  - Visual section layout: coloured blocks for each section type (Pallavi = blue, Anupallavi = green, Charanam = amber, Chittaswaram = purple).
  - Structure detail table: Section Type, Order, Label.
  - Source(s) that proposed this structure.

- **Section B: Participating Sources** (Accordion, one panel per source)
  - Each panel header: Source name, tier badge, "Agrees" (green check) or "Dissents" (red X).
  - Panel content:
    - Proposed section structure (same visual as above).
    - Side-by-side diff against winning structure (added sections in green, removed in red, modified in amber).
    - Source URL and extraction metadata.

- **Section C: Voting Rationale** (Card)
  - Consensus type explanation:
    - **UNANIMOUS**: "All N sources agree on the section structure."
    - **MAJORITY**: "M of N sources agree. K source(s) dissent."
    - **AUTHORITY_OVERRIDE**: "Tier T source overrides lower-tier disagreement."
    - **SINGLE_SOURCE**: "Only one source available — no voting required."
    - **MANUAL**: "Structure was determined by manual review."
  - Notes field (if present).
  - Reviewer attribution (if MANUAL).

- **Section D: Manual Override** (shown for non-MANUAL decisions, restricted to Admin/Reviewer role)
  - Editable section structure builder (add/remove/reorder sections).
  - Notes field (required for override).
  - "Submit Override" button → creates new `structural_vote_log` entry with `consensus_type = 'MANUAL'`, records `reviewer_id`.

---

### 4.9. Quality Dashboard

**Goal**: Corpus-wide quality metrics to guide enrichment priorities and track progress against the 6-phase strategy.

**Route**: `/admin/sourcing/quality`

**Layout:**

- **Header**: Title "Corpus Quality Dashboard", last audit run timestamp, "Run Audit Now" button.

- **Row 1: KPI Cards** (5 cards):

  | Card | Metric | Description |
  |:---|:---|:---|
  | Total Krithis | Count | Total canonical Krithis in the system |
  | Multi-Source | Count (%) | Krithis with 2+ contributing sources |
  | Structural Consensus | Count (%) | Krithis with voting decisions at HIGH confidence |
  | Avg Quality Score | Number (0–1) | Mean quality score across all Krithis |
  | Enrichment Coverage | % | Krithis with deity + temple + 2+ language variants |

- **Row 2: Quality Score Distribution** (left 50%) + **Source Tier Coverage** (right 50%):
  - **Quality Distribution**: Histogram of quality scores in buckets (0–0.2, 0.2–0.4, 0.4–0.6, 0.6–0.8, 0.8–1.0) with colour coding (red → amber → green).
  - **Tier Coverage**: Stacked bar chart showing how many Krithis have at least one Tier 1, Tier 2, etc. source.

- **Row 3: Enrichment Phase Progress** (full width):
  - Horizontal progress bars for each phase of the 6-phase strategy:
    - Phase 0: Foundation & Quality Baseline
    - Phase 1: PDF Ingestion — Skeleton Extraction
    - Phase 2: Structural Validation & Voting
    - Phase 3: Lyric Enrichment
    - Phase 4: Metadata Enrichment
    - Phase 5: Notation Ingestion
  - Each bar shows: target count, completed count, in-progress count, percentage.

- **Row 4: Data Gaps & Priorities** (left 60%) + **Composer Coverage Matrix** (right 40%):
  - **Data Gaps**: Table of top enrichment priorities:
    - Krithis with no lyrics (count)
    - Krithis with single-language lyrics only (count)
    - Krithis with no deity/temple (count)
    - Krithis with structural conflicts (LOW confidence voting) (count)
    - Krithis with no notation (Varnams/Swarajathis only) (count)
  - Each row links to a filtered view of the relevant Krithis.
  - **Composer Matrix**: Heatmap grid (Composer × Field) showing coverage percentages. Fields: Title, Raga, Tala, Sections, Lyrics (Sa), Lyrics (En), Deity, Temple, Notation.

- **Row 5: Audit Results** (full width, collapsible):
  - Results of TRACK-039 audit queries:
    - Section count mismatch across language variants.
    - Label sequence mismatch (non-canonical section ordering).
    - Orphaned lyric blobs (lyrics without section assignment).
  - Each audit shows: query name, count of violations, trend (↑↓→ vs. last run), "View Details" link.

---

## 5. API Requirements

The following backend API endpoints are needed to power the UI screens. These extend the existing Ktor route structure.

### 5.1. Source Registry API

| Method | Route | Description |
|:---|:---|:---|
| `GET` | `/v1/admin/sourcing/sources` | List sources (filter: tier, format, active, search) |
| `GET` | `/v1/admin/sourcing/sources/:id` | Get source detail with contribution stats |
| `POST` | `/v1/admin/sourcing/sources` | Register new source |
| `PUT` | `/v1/admin/sourcing/sources/:id` | Update source |
| `DELETE` | `/v1/admin/sourcing/sources/:id` | Deactivate source (soft delete) |

### 5.2. Extraction Queue API

| Method | Route | Description |
|:---|:---|:---|
| `GET` | `/v1/admin/sourcing/extractions` | List extraction tasks (filter: status, format, source, date, batch) |
| `GET` | `/v1/admin/sourcing/extractions/:id` | Get extraction task detail with results |
| `POST` | `/v1/admin/sourcing/extractions` | Submit new extraction request |
| `POST` | `/v1/admin/sourcing/extractions/:id/retry` | Retry failed extraction |
| `POST` | `/v1/admin/sourcing/extractions/:id/cancel` | Cancel pending/processing extraction |
| `POST` | `/v1/admin/sourcing/extractions/retry-all-failed` | Retry all failed extractions |
| `GET` | `/v1/admin/sourcing/extractions/stats` | Queue summary statistics |

### 5.3. Source Evidence API

| Method | Route | Description |
|:---|:---|:---|
| `GET` | `/v1/admin/sourcing/evidence` | List Krithis with evidence summary (filter: source count, tier, field) |
| `GET` | `/v1/admin/sourcing/evidence/krithi/:id` | Get all source evidence for a specific Krithi |
| `GET` | `/v1/admin/sourcing/evidence/compare/:id` | Get field-level comparison across sources for a Krithi |

### 5.4. Structural Voting API

| Method | Route | Description |
|:---|:---|:---|
| `GET` | `/v1/admin/sourcing/voting` | List voting decisions (filter: consensus type, confidence, date, pending review) |
| `GET` | `/v1/admin/sourcing/voting/:id` | Get voting detail with participants and dissents |
| `POST` | `/v1/admin/sourcing/voting/:id/override` | Submit manual structure override |
| `GET` | `/v1/admin/sourcing/voting/stats` | Voting summary statistics |

### 5.5. Quality Dashboard API

| Method | Route | Description |
|:---|:---|:---|
| `GET` | `/v1/admin/sourcing/quality/summary` | KPI summary (totals, averages, coverage %) |
| `GET` | `/v1/admin/sourcing/quality/distribution` | Quality score distribution histogram data |
| `GET` | `/v1/admin/sourcing/quality/coverage` | Tier coverage and composer matrix data |
| `GET` | `/v1/admin/sourcing/quality/gaps` | Data gap analysis (missing fields, conflicts) |
| `GET` | `/v1/admin/sourcing/quality/audit` | Latest audit query results (TRACK-039 queries) |
| `POST` | `/v1/admin/sourcing/quality/audit/run` | Trigger fresh audit run |

---

## 6. Component Requirements

The following Design System components are needed (extending what was defined for Bulk Import):

| # | Component | Description | Reuse |
|:---|:---|:---|:---|
| 1 | **TierBadge** | Colour-coded badge for source authority tiers (T1 gold, T2 silver, T3 bronze, T4 blue, T5 grey). Tooltip with tier definition. | New |
| 2 | **FormatPill** | Small pill badges for document formats (PDF, HTML, DOCX, API, MANUAL). | New |
| 3 | **ConfidenceBar** | Horizontal bar (0–1) with gradient fill (red → amber → green). | New |
| 4 | **StatusChip** | Reuse from Bulk Import. Add extraction statuses: PENDING, PROCESSING, DONE, FAILED, CANCELLED. | Extend |
| 5 | **StructureVisualiser** | Coloured block diagram showing Krithi section structure (P/A/C/SC/CS). | New |
| 6 | **FieldComparisonTable** | Multi-column diff table for comparing field values across sources. Colour-coded cells (agreement/variation/conflict). | New |
| 7 | **TimelineCard** | Vertical timeline showing state transitions with timestamps and durations. | New |
| 8 | **MetricCard** | Summary card with label, primary value, secondary metric, and optional sparkline/icon. | New (generic) |
| 9 | **JsonViewer** | Collapsible, syntax-highlighted JSON viewer with copy-to-clipboard. | Extend (from LogViewer) |
| 10 | **HeatmapGrid** | Grid visualisation for Composer × Field coverage matrix. | New |
| 11 | **ProgressBarRow** | Labelled horizontal progress bar with target/actual/percentage for phase tracking. | New |
| 12 | **DataGrid** | Reuse virtualised table from Bulk Import. | Reuse |

---

## 7. Implementation Phases

Implementation is aligned with the backend phases defined in the [Implementation Checklist](./implementation-checklist.md) and the overall 6-phase strategy in [Quality Strategy](./quality-strategy.md).

### Phase 1 — Foundation (Sprint 1–2)
**Backend dependencies**: Source Registry API, Extraction Queue API (basic).

| Deliverable | Description |
|:---|:---|
| Source Registry CRUD | List, create, edit, deactivate sources. TierBadge and FormatPill components. |
| Extraction Monitor (basic) | Queue list view with status filtering, auto-refresh. StatusChip extension. |
| Extraction Request Wizard | Form to submit new extraction tasks. |
| Navigation scaffolding | Route setup, nav menu integration, Sourcing Dashboard skeleton. |

### Phase 2 — Monitoring & Operations (Sprint 3–4)
**Backend dependencies**: Extraction Detail API, queue stats, retry/cancel operations.

| Deliverable | Description |
|:---|:---|
| Extraction Detail | Full task detail with timeline, results table, error display. |
| Source Detail | Source profile, contribution stats, extraction history. |
| Queue Operations | Retry individual/all failed, cancel, confirmation dialogs. |
| Sourcing Dashboard | Metric cards, activity feed, quick actions. |

### Phase 3 — Provenance & Voting (Sprint 5–6)
**Backend dependencies**: Source Evidence API, Structural Voting API.

| Deliverable | Description |
|:---|:---|
| Source Evidence Browser | Krithi-centric provenance list and detail views. |
| Field Comparison View | Multi-source diff table with conflict highlighting. |
| Structural Voting Viewer | Voting decision list and detail views. StructureVisualiser component. |
| Manual Override | Section structure editor for manual voting overrides. |

### Phase 4 — Quality & Insights (Sprint 7–8)
**Backend dependencies**: Quality Dashboard API, audit query integration.

| Deliverable | Description |
|:---|:---|
| Quality Dashboard | KPI cards, distribution histogram, tier coverage chart. |
| Enrichment Phase Progress | Phase progress bars with target tracking. |
| Composer Coverage Matrix | HeatmapGrid component for coverage visualisation. |
| Data Gaps & Audit Results | Gap tables with links, audit query result display. |

---

## 8. Accessibility & Responsiveness

| Concern | Requirement |
|:---|:---|
| Keyboard Navigation | All interactive elements must be keyboard-accessible. Tab order follows visual hierarchy. |
| Screen Reader | ARIA labels for metric cards, badges, charts. Data tables have proper headers and captions. |
| Colour Contrast | All tier badge and status chip colours meet WCAG AA contrast ratios against their backgrounds. |
| Responsive Layout | Metric card rows collapse to 2×2 grid on tablet, single column on mobile. Tables switch to card layout below 768px. |
| Loading States | Skeleton loaders for all data-fetching states. Extraction Monitor shows loading indicator during auto-refresh. |
| Error States | Empty states with helpful messages ("No extractions yet — start by submitting an extraction request"). Error boundaries with retry actions. |

---

## 9. Integration with Existing Screens

| Existing Screen | Integration Point |
|:---|:---|
| **Krithi Editor** | New "Source Evidence" tab showing all contributing sources, field provenance, and voting decisions for the current Krithi. Links to Source Evidence Browser and Voting Detail. |
| **Bulk Import — Batch Detail** | Link from extraction-linked tasks to Extraction Detail. "Source Authority" column in task explorer showing the tier of the source being imported. |
| **Bulk Import — Task Review** | Source evidence sidebar showing alternative values from other sources during entity resolution. Confidence indicators from structural voting. |
| **Import Review (Section 6.2.7 of PRD)** | Authority source validation: show tier badge next to source, warn if approving data from a lower-tier source when higher-tier data exists. |

---

## 10. Success Metrics

| Metric | Target | Measurement |
|:---|:---|:---|
| Source Registry Completeness | All 5+ known sources registered with accurate tiers | Manual audit |
| Extraction Queue Visibility | Operators can identify failed extractions within 5 minutes | Time-to-awareness measurement |
| Provenance Coverage | 100% of published Krithis have at least one source evidence record | Quality Dashboard metric |
| Voting Audit Compliance | All structural decisions are logged and viewable | structural_vote_log completeness |
| Manual Review Turnaround | LOW-confidence voting decisions reviewed within 48 hours | Aging metric on pending reviews |
| Quality Dashboard Usage | Dashboard viewed at least weekly by quality auditor | Access logs |

---

## 11. Open Questions & Future Considerations

| # | Question | Context |
|:---|:---|:---|
| 1 | Should the Quality Dashboard support custom date ranges for trend analysis? | Useful for tracking improvement over time, but adds complexity. |
| 2 | Should source evidence be editable (e.g., correcting contributed fields after extraction)? | Current design treats evidence as immutable audit records. |
| 3 | Should notifications (email/Slack) be sent for failed extractions or pending manual reviews? | Depends on team size and operational workflow. |
| 4 | Should the structural voting override support undo/rollback? | Currently each override creates a new vote log entry; previous decisions remain in history. |
| 5 | Should the Extraction Monitor support bulk selection and batch operations? | Useful for large-scale campaigns; adds UI complexity. |

---

## 12. Related Documents

| Document | Path |
|:---|:---|
| Krithi Data Sourcing & Quality Strategy | [quality-strategy.md](./quality-strategy.md) |
| Implementation Checklist | [implementation-checklist.md](./implementation-checklist.md) |
| Bulk Import UI/UX Plan | [../features/bulk-import/ui-ux-plan.md](../features/bulk-import/ui-ux-plan.md) |
| Admin Web PRD | [../admin-web/prd.md](../admin-web/prd.md) |
| Product Requirements Document | [../product-requirements-document.md](../product-requirements-document.md) |
| Database Schema | [../../04-database/schema.md](../../04-database/schema.md) |
| TRACK-041 | [../../../conductor/tracks/TRACK-041-enhanced-sourcing-logic.md](../../../conductor/tracks/TRACK-041-enhanced-sourcing-logic.md) |
