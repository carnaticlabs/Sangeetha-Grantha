| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Sangita Grantha Admin Web Application PRD


- Data Mapping (Deleted)
- [Api Contract](../../03-api/api-contract.md)
- [Ui To Api Mapping](../../03-api/ui-to-api-mapping.md)
- [Schema](../../04-database/schema.md)

# 1. Executive Summary

The **Sangita Grantha Admin Web Application** is the primary console for
curating and governing the canonical catalog of Carnatic Krithis. Public
users (rasikas, students, performers) access read-only data via the mobile
apps and public APIs, but editors and reviewers need a secure, structured
interface to:

- Maintain reference data (composers, ragas, talas, deities, temples).
- Create and edit Krithis and their metadata (including musical form classification).
- Manage multiple lyric variants, languages, scripts, and sampradayas.
- Manage notation variants and rows for Varnams and Swarajathis.
- Review and promote imported data from legacy sources into canonical form.
- Enforce editorial workflow states and provenance via audit logging.

This PRD defines the scope for **v1** of the Admin Web, aligned with the
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
TypeScript 5.8** that:

- Talks to the **Sangita Grantha backend** (Ktor 3.3.x + Exposed) via
  REST APIs.
- Surfaces CRUD workflows for core entities and imported data.
- Enforces **workflow_state** (`draft`, `in_review`, `published`, `archived`).
- Ensures every mutation is captured in `audit_log`.

## 2.3 Target Users

- **Editors** – enter, correct, and enrich Krithi metadata and lyrics.
- **Reviewers** – review editor work, approve/reject changes, publish.
- **System Administrators** – manage users, roles, and configuration.
- **Ingestion / Curation Team** – map imported Krithis to canonical entities.

---

# 3. Core Features

## 3.1 Reference Data Management

Manage core lookup tables that other workflows depend on.

Scope:
- **Composers** – name, normalized name, life dates, place, notes.
- **Ragas** – name, normalized name, melakarta number, parent raga,
  arohanam/avarohanam, notes.
- **Talas** – name, normalized name, anga structure, beat count.
- **Deities** – name, normalized name, description.
- **Temples** – name, normalized name, location, primary deity,
  geo-coordinates, notes.
- **Temple Names** – multilingual and alias names per temple.

Capabilities:
- List, filter, and sort each reference entity.
- Create/edit/delete entries with validation.
- Prevent destructive changes when entities are in use (soft-delete or
  archival strategy to be defined in backend spec).

**User story:** As an editor, I add a new raga with arohanam/avarohanam
so that it can be assigned to Krithis and discovered via search.

---

## 3.2 Krithi Management

CRUD for **Krithis**, including all core metadata.

Fields (aligned with DB `krithis` table):
- title, incipit, title_normalized, incipit_normalized.
- composer, primary raga, tala, deity, temple.
- primary language of composition.
- `musical_form` (`KRITHI`, `VARNAM`, `SWARAJATHI`).
- `is_ragamalika` flag.
- workflow_state (`draft`, `in_review`, `published`, `archived`).
- sahitya summary, notes.

Capabilities:
- Create new Krithis by selecting existing reference data or creating
  new reference entries inline (composer/raga/tala/etc.).
- Edit existing Krithis with clear display of current workflow state.
- Change workflow state via explicit actions: **Submit for Review**,
  **Publish**, **Archive**, **Send Back to Draft**.
- View audit history for each Krithi (who changed what, when).

**User story:** As an editor, I create a new Krithi record with composer,
raga, tala, and deity so that it becomes searchable across all clients.

---

## 3.3 Notation Management (Varnams & Swarajathis)

Manage **KrithiNotationVariant** and **KrithiNotationRow** for compositions
with `musical_form` of `VARNAM` or `SWARAJATHI`.

Scope:
- Multiple notation variants per Krithi (e.g., different pathantharams, sources).
- Notation types: `SWARA` (swara notation) or `JATHI` (jathi notation).
- Tala, kalai, and eduppu metadata per variant.
- Line-by-line notation rows organized by section.

Capabilities:
- Add/edit/delete notation variants for Varnam/Swarajathi compositions:
  - notation type (SWARA/JATHI), tala, kalai, eduppu offset.
  - variant label (e.g., "Lalgudi bani", "SSP notation").
  - source reference.
  - primary flag.
- Manage notation rows per variant:
  - section assignment, order index.
  - swara text (e.g., "S R G M P D N S").
  - optional sahitya text per row.
  - tala markers.
- UI should validate `musical_form` before allowing notation operations.
- Display notation in structured, section-aligned format.

**User story:** As an editor, I add swara notation for a Varnam with
multiple pathantharam variants so that students can learn different
interpretations.

---

## 3.4 Lyric Variants & Sections

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

---

## 3.5 Tags & Themes

Leverage the `tags` and `krithi_tags` tables to apply thematic or
contextual labels to Krithis.

Tag categories include:
- BHAVA, FESTIVAL, PHILOSOPHY, KSHETRA, STOTRA_STYLE, NAYIKA_BHAVA, OTHER.

Capabilities:
- Maintain the tag catalog (slug, display name, description).
- Assign/unassign tags to Krithis (e.g. **Navaratri**, **Bhakti**,
  **Advaita**, specific kshetrams).
- Optionally record source and confidence for tags imported from
  external sources.

**User story:** As a curator, I tag all Navaratri-related Krithis so that
rasikas can easily filter for festival-specific repertoire.

---

## 3.6 Import Review & Canonicalization

Workflows around `import_sources` and `imported_krithis`.

Capabilities:
- List imported Krithis by status (`pending`, `in_review`, `mapped`,
  `rejected`, `discarded`).
- Inspect raw fields (`raw_title`, `raw_lyrics`, `raw_composer`,
  etc.) and `parsed_payload` JSON.
- Map imported entries to canonical entities:
  - Link to existing `Krithi`, `Composer`, `Raga`, etc.
  - Or create new entities from imported data.
- Update import_status and reviewer notes.

**User story:** As a reviewer, I process a batch of imported Krithis from
karnatik.com, map them to canonical entities, and either mark them as
`mapped` or `rejected` with reasons.

---

## 3.7 Search & Filters (Admin)

Admin-facing search across Krithis and reference entities.

Capabilities:
- Search Krithis by title, incipit, composer, raga, tala, deity,
  temple, tags, and workflow_state.
- Filter by language, sampradaya, and presence of lyric variants.
- Basic full-text search on lyrics in admin view (backed by
  trigram index on `krithi_lyric_variants.lyrics`).

Non-goals (v1):
- End-user optimized faceting or recommendation logic; admin search
  is utility-focused.

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

- Extend the API contract with admin endpoints, e.g.:
  - `GET /v1/admin/krithis` – list Krithis with filters.
  - `GET /v1/admin/krithis/{id}` – Krithi detail + variants + tags + notation.
  - `POST /v1/admin/krithis` – create Krithi (includes `musicalForm`).
  - `PUT /v1/admin/krithis/{id}` – update Krithi.
  - `POST /v1/admin/krithis/{id}/variants` – create lyric variant.
  - `POST /v1/admin/krithis/{id}/notation` – create notation variant (VARNAM/SWARAJATHI only).
  - `PUT /v1/admin/krithis/{id}/notation` – update notation variant.
  - `POST /v1/admin/notation/{variantId}/rows` – update notation rows.
  - `DELETE /v1/admin/notation/{variantId}` – delete notation variant.
  - `POST /v1/admin/krithis/{id}/tags` – assign tags.
  - `GET /v1/admin/imports/krithis` – list imported Krithis.
  - `POST /v1/admin/imports/krithis/{id}/map` – map to canonical Krithi.
- Authentication:
  - JWT-based with role claims (`admin`, `editor`, `reviewer`, etc.).
  - All admin routes require authentication.
- Observability:
  - All mutations emit entries into `audit_log`.
  - Request and error logging for admin routes.

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

# 9. Implementation Status (v1)

This PRD describes **target behaviour**. Implementation status for Sangita
Grantha will be tracked separately in engineering dashboards and project
boards; initial code scaffolding (schema, shared DTOs, backend skeleton)
exists but the admin web UI has not yet been implemented.