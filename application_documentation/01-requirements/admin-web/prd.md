| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Sangita Grantha Admin Web Application PRD

- [Api Contract](../../03-api/api-contract.md)
- [Ui To Api Mapping](../../03-api/ui-to-api-mapping.md)
- [Schema](../../04-database/schema.md)
- [UI Specs](../../05-frontend/admin-web/ui-specs.md)

# 1. Executive Summary

The **Sangita Grantha Admin Web Application** is the primary console for
curating and governing the canonical catalog of Carnatic Krithis. Public
users (rasikas, students, performers) access read-only data via the mobile
apps and public APIs, but editors and reviewers need a secure, structured
interface to:

- View a real-time dashboard of catalog health and recent editorial activity.
- Maintain reference data (composers, ragas, talas, deities, temples).
- Create and edit Krithis and their metadata (including musical form classification).
- Manage multiple lyric variants, languages, scripts, and sampradayas.
- Manage notation variants and rows for Varnams and Swarajathis.
- Manage thematic tags and apply them to compositions.
- Ingest data from external web sources via AI-assisted scraping (Gemini).
- Orchestrate bulk import campaigns from CSV manifests with full pipeline monitoring.
- Review and promote imported data from legacy sources into canonical form.
- Enforce editorial workflow states and provenance via audit logging.

This PRD defines the scope for the Admin Web, aligned with the
Sangita Grantha PRD and database schema.

---

# 2. Product Overview

## 2.1 Problem

- Carnatic Krithi data is scattered across websites, PDFs, book scans,
  and personal compilations.
- There is no single, normalized system of record linking composers, ragas,
  talas, deities, temples, and sahitya variants.
- Existing sources lack:
  - Consistent metadata.
  - Robust search (especially lyrics substring search).
  - Clear editorial workflow and provenance.

## 2.2 Solution

Provide a secure, browser-based admin console built with **React 19.2 +
TypeScript 5.9** that:

- Talks to the **Sangita Grantha backend** (Ktor + Exposed) via
  REST APIs.
- Surfaces CRUD workflows for core entities and imported data.
- Provides a dashboard for catalog health and editorial activity.
- Offers AI-assisted web scraping and bulk import orchestration.
- Enforces **workflow_state** (`draft`, `in_review`, `published`, `archived`).
- Ensures every mutation is captured in `audit_log`.

## 2.3 Target Users

- **Editors** – enter, correct, and enrich Krithi metadata and lyrics.
- **Reviewers** – review editor work, approve/reject changes, publish.
- **System Administrators** – manage users, roles, and configuration.
- **Ingestion / Curation Team** – map imported Krithis to canonical entities, operate bulk import campaigns.

---

# 3. Core Features

## 3.1 Dashboard

Home page providing at-a-glance visibility into catalog health and editorial activity.

Capabilities:
- **Stat Cards**: Total Krithis, Composers, Ragas, and Pending Review counts
  with clickable navigation to relevant pages.
- **Recent Edits Feed**: Chronological list of recent audit log entries showing
  entity type, action, actor, and timestamp.
- **Curator Tasks Panel**: Actionable alerts for missing metadata (e.g., Krithis
  without Tala) and pending validation batches.
- **Quick Actions**: "New Composition" button, "Manage Taxonomy" link to
  Reference Data.

**User story:** As an admin, I see key catalog metrics and recent activity on
login so I can prioritise my editorial work.

**Status:** ✅ Implemented

---

## 3.2 Reference Data Management

Manage core lookup tables that other workflows depend on.

Scope:
- **Composers** – name, normalized name, life dates, place, notes.
- **Ragas** – name, normalized name, melakarta number, parent raga,
  arohanam/avarohanam, notes.
- **Talas** – name, normalized name, anga structure, beat count.
- **Deities** – name, normalized name, description.
- **Temples** – name, normalized name, location, primary deity,
  geo-coordinates, notes.

Capabilities:
- Card-based home view with entity counts and colour-coded category indicators.
- List, filter, and sort each reference entity with search.
- Create/edit/delete entries with dedicated forms per entity type.
- Breadcrumb navigation: Reference Data → Entity Type → Create/Edit.
- Prevent destructive changes when entities are in use (confirmation dialog).
- Stats refresh after mutations.

**User story:** As an editor, I add a new raga with arohanam/avarohanam
so that it can be assigned to Krithis and discovered via search.

**Status:** ✅ Implemented (Composers, Ragas, Talas, Deities, Temples)

---

## 3.3 Krithi Management

CRUD for **Krithis**, including all core metadata via a tabbed editor.

Fields (aligned with DB `krithis` table):
- title, incipit, title_normalized, incipit_normalized.
- composer, primary raga, tala, deity, temple.
- primary language of composition.
- `musical_form` (`KRITHI`, `VARNAM`, `SWARAJATHI`).
- `is_ragamalika` flag.
- workflow_state (`draft`, `in_review`, `published`, `archived`).
- sahitya summary, notes.

Editor Tabs:
1. **Metadata** – Core fields with searchable reference data selectors.
2. **Structure** – Section editor (pallavi, anupallavi, charanams, etc.).
3. **Lyrics** – Multi-language/script lyric variant management.
4. **Notation** – Notation variant editor (shown only for VARNAM/SWARAJATHI/KRITHI musical forms).
5. **Tags** – Tag assignment with category display.
6. **Audit** – Audit log history for the Krithi.

Capabilities:
- Create new Krithis by selecting existing reference data or creating
  new reference entries inline.
- Edit existing Krithis with clear display of current workflow state.
- Workflow state pill displayed in header (colour-coded: grey=draft, yellow=in_review, green=published, red=archived).
- Header shows title, composer, raga, tala, and primary language.
- Save changes with optimistic UI feedback.
- Lazy loading of sections, lyric variants, and tags per tab.
- Error boundary wrapping for resilience.
- Breadcrumb navigation: Kritis → Edit Composition.

**User story:** As an editor, I create a new Krithi record with composer,
raga, tala, and deity so that it becomes searchable across all clients.

**Status:** ✅ Implemented

---

## 3.4 Notation Management (Varnams & Swarajathis)

Manage **KrithiNotationVariant** and **KrithiNotationRow** for compositions
with applicable `musical_form`.

Scope:
- Multiple notation variants per Krithi (e.g., different pathantharams, sources).
- Notation types: `SWARA` (swara notation) or `JATHI` (jathi notation).
- Tala, kalai, and eduppu metadata per variant.
- Line-by-line notation rows organized by section.

Capabilities:
- Add/edit/delete notation variants via modal:
  - notation type (SWARA/JATHI), tala, kalai, eduppu offset.
  - variant label (e.g., "Lalgudi bani", "SSP notation").
  - source reference.
  - primary flag.
- Notation variant list with metadata display.
- Notation rows editor per variant:
  - section assignment, order index.
  - swara text (e.g., "S R G M P D N S").
  - optional sahitya text per row.
  - tala markers.
- UI conditionally shows Notation tab based on `musical_form`.
- Display notation in structured, section-aligned format.

**User story:** As an editor, I add swara notation for a Varnam with
multiple pathantharam variants so that students can learn different
interpretations.

**Status:** ✅ Implemented

---

## 3.5 Lyric Variants & Sections

Manage **KrithiLyricVariant** and **KrithiSection** / **KrithiLyricSection**.

Scope:
- Multiple variants per Krithi, across languages and scripts.
- Per-variant sections like pallavi, anupallavi, charanams, etc.

Capabilities:
- Add/edit/delete lyric variants for a Krithi:
  - language, script, transliteration scheme.
  - sampradaya (optional), variant label, source reference.
  - full lyrics text.
- Mark one variant per language/script as **primary**.
- Create structural sections for a Krithi (pallavi, anupallavi,
  charanam 1/2/3, etc.).
- For each lyric variant, enter text **by section** using
  `krithi_lyric_sections`.

**User story:** As a reviewer, I can see and compare two different
patantharam variants for a Krithi side by side and mark one as primary.

**Status:** ✅ Implemented

---

## 3.6 Tags & Themes

Manage the `tags` catalog and assign tags to Krithis.

Tag categories include:
- BHAVA, FESTIVAL, PHILOSOPHY, KSHETRA, STOTRA_STYLE, NAYIKA_BHAVA, OTHER.

Capabilities:
- Dedicated Tags page with full CRUD:
  - Create tags with category, slug, display name, and description.
  - Edit existing tags via inline form.
  - Delete tags with confirmation.
  - Search/filter tags by name, category, or slug.
  - Category shown as colour-coded pill badges.
- Assign/unassign tags to Krithis via the **Tags tab** in the Krithi Editor.
- Optionally record source and confidence for tags imported from
  external sources.

**User story:** As a curator, I tag all Navaratri-related Krithis so that
rasikas can easily filter for festival-specific repertoire.

**Status:** ✅ Implemented

---

## 3.7 Import & Web Scraping

Manage individual data imports from external web sources with AI-assisted extraction.

Capabilities:
- **Tabbed interface** with two modes:
  - **Scrape New**: Enter a URL to fetch HTML and extract Krithi metadata
    using Google Gemini AI. Shows success result with extracted title and composer.
  - **Import History**: Table listing all imported Krithis with status badges
    (PENDING, IN_REVIEW, APPROVED, MAPPED, REJECTED, DISCARDED).
- **Import Review Modal**: Review individual imports with:
  - Raw field inspection (title, composer, raga, tala, lyrics, deity, temple).
  - Override fields before approval.
  - Approve or reject with status update.
- Status colour-coding: green (APPROVED/MAPPED), yellow (IN_REVIEW),
  blue (PENDING), red (REJECTED), grey (DISCARDED).

**User story:** As an ingestion operator, I scrape a Krithi from shivkumar.org,
review the AI-extracted fields, correct any errors, and approve it into the
canonical catalog.

**Status:** ✅ Implemented

---

## 3.8 Bulk Import Orchestration

Orchestrate large-scale CSV-driven bulk imports with full pipeline monitoring.

Capabilities:
- **CSV Upload**: File input with drag-and-drop for CSV manifests
  (columns: Krithi, Raga, Hyperlink).
- **Batch List**: Table of all import batches with:
  - Manifest filename, status chip (PENDING/RUNNING/PAUSED/SUCCEEDED/FAILED/CANCELLED).
  - Progress bar with processed/total counts.
  - Created timestamp.
  - Quick actions: Retry, Pause/Resume, Delete.
- **Batch Detail Panel** (side panel):
  - Batch-level controls: Approve All, Reject All, Retry Failed, Cancel, Delete.
  - Finalize and Export Report buttons (for SUCCEEDED batches).
  - Task breakdown: Succeeded/Failed/Retryable/Pending/Running counts.
  - Status and progress metrics with percentage bar.
  - Started/completed timestamps.
  - **Pipeline Stepper**: Visual 3-stage pipeline (Ingest → Scrape → Resolve)
    with animated active state, colour-coded completion, and task summary.
  - **Job Details**: Expandable list of pipeline jobs with status.
  - **Tasks**: Filterable task list (by status: ALL/PENDING/RUNNING/SUCCEEDED/FAILED/RETRYABLE/BLOCKED/CANCELLED).
    Each task shows source URL, status, attempt count, duration, and truncated error.
  - **Task Log Viewer Drawer**: Slide-in panel with:
    - Task overview (ID, status, attempt, duration, source URL).
    - Error details with formatted stack trace.
    - Task data with krithi key.
    - Copy Task JSON action.
  - **Events**: Chronological event log for the batch.
- **Auto-Refresh**: Automatic 5-second polling for active (RUNNING/PENDING) batches.

**User story:** As a system admin, I upload a CSV manifest of 500 Thyagaraja
Krithis, monitor the scrape/resolve pipeline in real time, retry failed tasks,
and approve the batch for publication.

**Status:** ✅ Implemented

---

## 3.9 Import Review Queue

Dedicated review interface for processing pending imports at scale.

Capabilities:
- **Split-pane layout**:
  - **Left sidebar**: Scrollable list of pending imports with:
    - Select-all checkbox and per-item checkboxes for bulk operations.
    - Title, composer, raga, and source key preview.
    - Active item highlighted with accent border.
    - Refresh button.
  - **Right detail panel**: Full review form for selected import:
    - Source URL header (clickable external link).
    - Approve & Create / Reject action buttons.
    - **AI Resolution Candidates**: Colour-coded confidence panel showing
      entity resolution matches for Composer, Raga, Deity, and Temple with
      click-to-apply scores.
    - **Primary Metadata form**: Editable fields for title, composer, raga,
      tala, deity, temple, language with pre-filled values from raw import.
    - **Lyrics Preview**: Monospace textarea with full lyrics content.
- **Bulk Operations**: Select multiple imports, then "Approve Selected" or
  "Reject Selected" with confirmation.

**User story:** As a reviewer, I process a batch of imported Krithis,
use AI-suggested entity matches to map them to canonical records, correct
any errors, and approve them in bulk.

**Status:** ✅ Implemented

---

## 3.10 Search & Filters (Admin)

Admin-facing search across Krithis and reference entities.

Capabilities:
- **Krithi List**: Search by title, raga, or composer with debounced input.
  Results shown in a table with title, composer, raga pills, and primary
  language. Row click navigates to Krithi Editor.
- **Reference Data**: Search within each entity type list.
- **Tags**: Search by name, category, or slug.
- **Imports**: Filter by status (PENDING, APPROVED, etc.).
- **Filter button** placeholder for advanced filtering (workflow state,
  musical form, language — scaffolded, not yet fully implemented).
- **Top Bar Search**: Global search bar in the top navigation (desktop only).

Non-goals (current):
- End-user optimized faceting or recommendation logic; admin search
  is utility-focused.

**Status:** ✅ Partially Implemented (basic search active; advanced filters planned)

---

## 3.11 User & Role Management

Placeholder pages for future RBAC management.

Capabilities (planned):
- **Users Page**: List, create, edit, delete users. Role assignment.
- **Roles Page**: Define roles and their capabilities for access control.

**Status:** 🔲 Placeholder (UI scaffolded with "coming soon" message)

---

# 4. User Roles & Permissions

Focused on admin personas rather than public users.

| Role          | Capabilities                                                        |
|---------------|---------------------------------------------------------------------|
| Admin         | Full CRUD on all entities; manage users and roles.                 |
| Editor        | Create/edit Krithis, lyrics, and reference data; cannot publish.   |
| Reviewer      | Review changes, publish/archived Krithis; limited CRUD on refs.    |
| IngestionOps  | Import mapping workflows; limited edit rights on Krithis/refs.     |

Authorization is implemented via `users`, `roles`, and `role_assignments`
plus JWT claims in the backend.

---

# 5. Database Roles & Privileges (Reference)

These are illustrative Postgres roles for ops; actual values live in
infrastructure configuration, not hard-coded.

```sql
-- Admin
CREATE ROLE sg_admin LOGIN PASSWORD 'strong_admin_pwd';
GRANT CONNECT ON DATABASE sangita_grantha TO sg_admin;
GRANT USAGE, CREATE ON SCHEMA public TO sg_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO sg_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO sg_admin;

-- Editor / Reviewer / IngestionOps can be modeled as separate roles
-- with more restricted SELECT/INSERT/UPDATE on specific tables.
```

---

# 6. Technical & API Requirements (Admin Surface)

## 6.1 Implemented API Endpoints

### Authentication
- `POST /v1/admin/login` – Token-based admin login with email or userId.

### Dashboard
- `GET /v1/admin/dashboard/stats` – Catalog statistics (totals, pending review).
- `GET /v1/admin/audit-logs` – Recent audit log entries.

### Krithi Management
- `GET /v1/admin/krithis` – Search Krithis with query parameter.
- `GET /v1/admin/krithis/{id}` – Krithi detail.
- `POST /v1/admin/krithis` – Create Krithi.
- `PUT /v1/admin/krithis/{id}` – Update Krithi.
- `GET /v1/admin/krithis/{id}/sections` – Get structural sections.
- `PUT /v1/admin/krithis/{id}/sections` – Save structural sections.
- `GET /v1/admin/krithis/{id}/lyric-variants` – Get lyric variants.
- `POST /v1/admin/krithis/{id}/lyric-variants` – Create lyric variant.
- `PUT /v1/admin/krithis/{id}/lyric-variants/{variantId}` – Update lyric variant.
- `GET /v1/admin/krithis/{id}/tags` – Get assigned tags.
- `GET /v1/admin/krithis/{id}/audit-logs` – Get krithi audit history.
- `POST /v1/admin/krithis/{id}/transliterate` – AI transliteration.

### Notation
- `GET /v1/admin/krithis/{id}/notation` – Get notation variants and rows.
- `POST /v1/admin/krithis/{id}/notation/variants` – Create notation variant.
- `PUT /v1/admin/notation/variants/{variantId}` – Update notation variant.
- `DELETE /v1/admin/notation/variants/{variantId}` – Delete notation variant.
- `POST /v1/admin/notation/variants/{variantId}/rows` – Create notation row.
- `PUT /v1/admin/notation/rows/{rowId}` – Update notation row.
- `DELETE /v1/admin/notation/rows/{rowId}` – Delete notation row.

### Reference Data (per entity: Composers, Ragas, Talas, Deities, Temples)
- `GET /v1/admin/{entity}` – List entities.
- `GET /v1/admin/{entity}/{id}` – Get entity detail.
- `POST /v1/admin/{entity}` – Create entity.
- `PUT /v1/admin/{entity}/{id}` – Update entity.
- `DELETE /v1/admin/{entity}/{id}` – Delete entity.
- `GET /v1/admin/reference/stats` – Reference data statistics.

### Tags
- `GET /v1/admin/tags` – List tags (with optional query filter).
- `GET /v1/admin/tags/all` – All tags.
- `GET /v1/admin/tags/{id}` – Get tag detail.
- `POST /v1/admin/tags` – Create tag.
- `PUT /v1/admin/tags/{id}` – Update tag.
- `DELETE /v1/admin/tags/{id}` – Delete tag.

### Imports & Scraping
- `GET /v1/admin/imports` – List imported Krithis (filter by status).
- `POST /v1/admin/scrape` – Scrape content from URL.
- `POST /v1/admin/imports/{id}/review` – Review import (approve/reject with overrides).
- `POST /v1/admin/imports/bulk-review` – Bulk review imports.
- `GET /v1/admin/imports/auto-approve-queue` – Get auto-approve eligible imports.

### Bulk Import
- `POST /v1/admin/bulk-import/upload` – Upload CSV manifest file.
- `GET /v1/admin/bulk-import/batches` – List batches (filter by status, pagination).
- `GET /v1/admin/bulk-import/batches/{id}` – Get batch details.
- `DELETE /v1/admin/bulk-import/batches/{id}` – Delete batch.
- `GET /v1/admin/bulk-import/batches/{id}/jobs` – Get batch jobs.
- `GET /v1/admin/bulk-import/batches/{id}/tasks` – Get batch tasks (filter by status, pagination).
- `GET /v1/admin/bulk-import/batches/{id}/events` – Get batch events (pagination).
- `POST /v1/admin/bulk-import/batches/{id}/pause` – Pause batch.
- `POST /v1/admin/bulk-import/batches/{id}/resume` – Resume batch.
- `POST /v1/admin/bulk-import/batches/{id}/cancel` – Cancel batch.
- `POST /v1/admin/bulk-import/batches/{id}/retry` – Retry failed tasks.
- `POST /v1/admin/bulk-import/batches/{id}/approve-all` – Approve all in batch.
- `POST /v1/admin/bulk-import/batches/{id}/reject-all` – Reject all in batch.
- `POST /v1/admin/bulk-import/batches/{id}/finalize` – Finalize batch.
- `GET /v1/admin/bulk-import/batches/{id}/export` – Export QA report.

### Sampradayas
- `GET /v1/admin/sampradayas` – List sampradayas.

## 6.2 Authentication

- JWT-based with admin token + email lookup.
- Token stored in `localStorage` (key: `authToken`).
- Token included in `Authorization: Bearer <token>` header.
- Automatic redirect to login on 401 responses (planned).
- All admin routes require authentication.

## 6.3 Observability

- All mutations emit entries into `audit_log`.
- Request and error logging for admin routes.
- Dashboard displays recent audit log feed.

---

# 7. Success Metrics

| Metric                                      | Target |
|---------------------------------------------|--------|
| Number of Krithis fully curated (metadata + lyrics) | ≥ 2,000 in v1 |
| Imported rows mapped to canonical entities          | ≥ 80% of ingested data |
| Editorial throughput (Krithis/week/editor)          | Measured & improving |
| Zero-downtime schema and data migrations            | 100% of releases |

---

# 8. Risks & Mitigation

- **Data quality variance:** Introduce strong validation, review queues,
  and audit trails for all edits.
- **Editorial backlog:** Provide search, filters, and simple tooling for
  bulk clean-up (e.g. tag assignment, batch review).
- **Access control mistakes:** Keep roles minimal, test with seed users,
  and maintain clear mapping between roles and capabilities.

---

# 9. Implementation Status

| Feature | Status | Notes |
|:---|:---|:---|
| Login / Authentication | ✅ Implemented | Token-based login with email |
| Dashboard | ✅ Implemented | Stats, audit feed, curator tasks, quick actions |
| Krithi List & Search | ✅ Implemented | Debounced search, table view, click-to-edit |
| Krithi Editor (Tabbed) | ✅ Implemented | Metadata, Structure, Lyrics, Notation, Tags, Audit tabs |
| Reference Data CRUD | ✅ Implemented | Composers, Ragas, Talas, Deities, Temples with forms |
| Tags Management | ✅ Implemented | Full CRUD with category filtering and search |
| Imports & Web Scraping | ✅ Implemented | Gemini AI scraping, import history, review modal |
| Bulk Import Orchestration | ✅ Implemented | CSV upload, batch monitoring, pipeline stepper, task drill-down |
| Import Review Queue | ✅ Implemented | Split-pane review, AI resolution candidates, bulk actions |
| Notation Editor | ✅ Implemented | Variant management, row editor, conditional display |
| Lyric Variant Management | ✅ Implemented | Multi-language variants, section-wise entry |
| Transliteration Modal | ✅ Implemented | AI-powered script transliteration |
| User Management | 🔲 Placeholder | UI scaffolded, functionality planned |
| Role Management | 🔲 Placeholder | UI scaffolded, functionality planned |
| Advanced Search Filters | 📋 Planned | Filter button exists, advanced UI pending |
| Auto-Approve Queue | ✅ Implemented | Backend integration complete |

---

# 10. Navigation Structure

The admin console uses a fixed sidebar navigation with the following hierarchy:

| Section | Label | Route | Status |
|:---|:---|:---|:---|
| Main | Dashboard | `/` | ✅ |
| Main | Kritis | `/krithis` | ✅ |
| Main | Reference Data | `/reference` | ✅ |
| Main | Imports | `/imports` | ✅ |
| Main | Bulk Import | `/bulk-import` | ✅ |
| Main | Review Queue | `/bulk-import/review` | ✅ |
| Main | Tags | `/tags` | ✅ |
| System | Settings | — | 🔲 Button only (not routed) |
| System | Users | `/users` | 🔲 Placeholder |
| System | Roles | `/roles` | 🔲 Placeholder |
| — | Login | `/login` | ✅ |
| — | Krithi Editor | `/krithis/new`, `/krithis/:id` | ✅ |
