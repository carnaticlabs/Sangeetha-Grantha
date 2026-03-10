| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 3.0.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

# Sangeetha Grantha: Complete Engineering Evolution


## From Vision to Production-Ready Platform

> **Audience**: Software Engineers & Technical Leaders
> **Period**: Project Inception – March 2026

---

## Executive Summary

Sangeetha Grantha represents a systematic journey from concept to production-ready platform, combining **musicological rigor** with **best-in-class software engineering**. This document chronicles the complete evolution: from initial vision through architectural decisions, performance optimizations, developer experience improvements, and scaling considerations.

**Key Achievements:**
- **40-50% reduction** in database queries through modern ORM patterns
- **95% reduction** in developer setup time via cross-platform standardization
- **100% documentation coverage** with commit-level traceability
- **Zero security incidents** from automated guardrails
- **Production-ready** testing infrastructure with steel thread and E2E validation
- **38 database migrations** evolving from baseline to comprehensive sourcing schema
- **86 conductor tracks** tracking engineering progress (76 completed)
- **120+ commits** shipped since mid-January 2026
- **PDF extraction pipeline** decoding Velthuis Sanskrit fonts and garbled diacritics
- **9-screen Sourcing & Quality UI** for provenance tracking and structural voting
- **Unified extraction architecture**: Python as single source of truth for all composition parsing
- **PostgreSQL 18 upgrade** with native UUID v7 support across 27 tables
- **Rust CLI archived** — simplified to Python db-migrate + Makefile workflow
- **Major backend refactoring** — 9,000+ lines reorganized into focused, testable modules
- **92% section inconsistency** discovered and fully remediated across 473 krithis

This is the story of building a system that respects both **musical tradition** and **engineering excellence**.

---

## High-Level Timeline of Evolution

While the rest of this document is structured thematically, it is useful to anchor the journey in time. The following phases consolidate the earlier **journey-so-far** summary and the **January 2026** engineering report into a single narrative.

### Phase 1 – Foundation and Baseline Clarity (Dec 2025, Week 1)

- Repository bootstrapped with core Ktor backend, Exposed DAL, and React admin shell.
- Initial README and stack documentation clarified project intent, tech choices, and module layout.
- Baseline PostgreSQL schema and migrations established (baseline enums, roles, audit log).

### Phase 2 – Documentation as Source of Truth (Late Dec 2025)

- Spec-driven documentation architecture (ADR-001) implemented under `application_documentation/`.
- Primary PRD, domain model, and early AI integration specs authored and linked.
- Admin web and mobile PRDs aligned to the shared domain model, ensuring a single mental model across clients.

### Phase 3 – AI Pipeline & Governance (Early Jan 2026)

- Intelligent Content Ingestion requirements and implementation delivered:
  - AI-powered web scraping and multi-script transliteration.
  - Structured JSON extraction and staging into `imported_krithis`.
- Commit Guardrails shipped in `sangita-cli`:
  - Enforced 1:1 mapping between commits and documentation references.
  - Sensitive data scanning added to pre-commit hooks.
- Early API coverage gaps identified and a concrete implementation plan produced (lyric variants, path standardisation, RBAC).

### Phase 4 – Data & Architecture Maturity (Mid Jan 2026)

- Database layer modernization completed:
  - Exposed RC-4 `resultedValues` and `updateReturning` adopted across repositories.
  - DELETE+INSERT anti-patterns replaced with smart diffing.
- Query optimisation and Exposed DAO vs DSL trade-offs documented and tested.
- Scaling evaluation and GCP scaling strategy authored, introducing edge-first and hub-and-spoke models.
- Cross-platform dev environment standardisation delivered with mise + bootstrap scripts, slashing onboarding time.

### Phase 5 – Feature Expansion & Operational Hardening (Late Jan 2026)

- Advanced krithi notation and AI transliteration features expanded multi-script, notation-centric capabilities.
- Generic scraping + domain mapping added to support heterogeneous legacy sources.
- Searchable deity and temple management added, strengthening domain coverage.
- Steel thread test (`make steel-thread`) wired into the dev workflow, acting as a production-grade smoke test.

### Phase 6 – Data Quality, Sourcing Pipeline & PDF Extraction (Late Jan – Feb 2026)

- **Authentication & Login**: JWT-based login page shipped; seed-data admin user standardised for local development.
- **Code Quality Refactoring**: Systematic Kotlin backend and React frontend audits (TRACK-021–025) eliminating `any` types, adding accessibility, and extracting reusable components.
- **Bulk Import Hardening**: Composer deduplication via alias tables, kshetra/temple mapping with geocoding, TextBlocker parsing fixes for Devanagari and complex headers, newline sanitisation, and duplicate section prevention.
- **Scraping Pipeline Evolution**: `TextBlocker` promoted to **`KrithiStructureParser`** — a deterministic, regex-based Carnatic section parser. Gemini LLM usage reduced by ~90% (lyrics no longer sent to LLM when deterministic extraction succeeds). Ragamalika-aware sub-section detection added.
- **Data Quality Audit**: SQL-driven audit of Krithi structural consistency across 30+ authoritative sources (Guruguha PDFs, karnatik.com, shivkumar.org). Remediation plan for duplicates, orphaned sections, and missing metadata.
- **Enhanced Sourcing & Structural Voting**: Source authority tiers (T1–T5), `krithi_source_evidence` provenance table, `structural_vote_log` for multi-source consensus, and `extraction_queue` for database-backed work distribution.
- **PDF Extraction Service**: Python-based PDF extractor with Docker infrastructure. English diacritic normalisation (garbled Utopia fonts), Sanskrit Velthuis Type 1 font decoder (155-glyph map with mātrā reordering), and segment-level bold detection.
- **Language Variant Pipeline**: Backend matching & enrichment API for cross-language variants (e.g. Sanskrit PDF as variant of English PDF). Multi-signal confidence scoring, auto-approval for high-confidence matches (≥0.85), anomaly/structure mismatch flagging.
- **Sourcing UI (9 screens)**: Source Registry, Extraction Monitor, Sourcing Dashboard, Source Evidence Browser, Structural Voting, Quality Dashboard, and integration with existing Krithi Editor. Full design system with 12 shared components (TierBadge, ConfidenceBar, StructureVisualiser, etc.).
- **Transliteration-Aware Normalisation**: Aspirate collapse table (`sh→s`, `th→t`, `ksh→ks`, etc.) ensuring cross-scheme matching (IAST ↔ Harvard-Kyoto ↔ ITRANS).
- **Bulk Import Idempotency**: Deduplication guards on Krithi creation, section insertion, and lyric variant persistence. Source evidence records now written during bulk import. Migration-based integration test infrastructure replacing SchemaUtils.
- **Dependency Updates**: Two upgrade cycles (Q1 2026 + Feb 2026) covering Ktor, Exposed, Kotlin, React, Vite, and Rust toolchains. Environment variable standardisation across monorepo.
- **Frontend E2E Testing**: Playwright scaffolding with headed/debug mode support.
- **Documentation**: Documentation Guardian audit & repair (TRACK-043), 12+ doc files updated, comprehensive project README overhaul.

### Phase 7 – Simplification, Consolidation & Data Integrity (Late Feb – Mar 2026)

- **Unified Extraction Engine (TRACK-064)**: Massive consolidation migrating all extraction logic from Kotlin to Python. Created `html_extractor.py` (BeautifulSoup-based), consolidated 100+ Indic regex rules into Python `structure_parser.py`, and added `gemini_enricher.py` with schema-driven enrichment and exponential backoff. Kotlin backend became a pure ingestion orchestrator. 97 passing Python tests.
- **Python Module Promotion (TRACK-065)**: Renamed `tools/pdf-extractor/` to `tools/krithi-extract-enrich-worker/` reflecting expanded scope (PDF + HTML + OCR + Gemini enrichment). Pinned Python 3.11 in root `.mise.toml`.
- **Rust CLI Archival (TRACK-078)**: Moved `tools/sangita-cli/` to `tools/sangita-cli-archived/`. Replaced all Rust CLI commands with `make` targets. Updated all Claude Code commands, git hooks, and documentation. Eliminated Rust as a required build dependency.
- **PostgreSQL 18 Upgrade (TRACK-072)**: Bumped Docker Compose from PostgreSQL 15 to 18.3-alpine. Created migration `37__pg18_uuidv7_defaults.sql` switching 27 tables to native `uuidv7()` defaults. Better sortability and temporal awareness on all UUID-keyed queries.
- **Major Backend Refactoring (TRACK-073–076)**: Systematic decomposition of monolithic Kotlin files into focused, testable modules:
  - TRACK-073: Split `SourcingDtos.kt` and `DtoMappers.kt` into domain-specific files
  - TRACK-074: Extracted `KrithiLyricRepository`, `KrithiSearchRepository`, `BulkImportEventRepository`, `BulkImportTaskRepository` from monolithic repositories
  - TRACK-075: Extracted `ImportReportGenerator`, `LyricVariantPersistenceService`, `VariantScorer`, `StructuralVotingProcessor`, `KrithiMatcherService`
  - TRACK-076: Extracted `GeminiModels`, `GeminiRetryStrategy`, `ScrapingPromptBuilder`, `SectionHeaderDetector`
- **E2E Pipeline Validation (TRACK-079)**: Discovered 435/473 krithis (92%) had inconsistent `krithi_lyric_sections` counts across 6 language variants. Rewrote `structure_parser.py` for MKS demotion, dual-format merge, and Indic anusvara handling. Migration 38 repaired all inconsistencies. Fixed `KrithiMatcherService` to route unmatched extractions to pending review instead of auto-creating krithis.
- **Curator Review UI (TRACK-080)**: Built `CuratorReviewPage.tsx` with two-tab interface (Pending Matches, Section Issues). Implemented approve/reject/merge workflows with keyboard shortcuts (j/k navigation, a=approve, r=reject). Backend `CuratorRoutes.kt` with stats and section-issue detection endpoints.
- **Local File Path Support (TRACK-081)**: Added `file://` URI and bare path support to extraction worker. Docker Compose volume mount `./data/pdfs:/app/pdfs:ro` for direct filesystem access.
- **Agent Skills Modernization (TRACK-083)**: Updated all workflows from Rust CLI to Makefile. Added `data-quality-audit` skill with 7 diagnostic SQL queries and `extraction-debugger` skill. Added protective hooks: config file guard, migration naming check. Fixed Pallavi detection for Nottusvara Sahityams.
- **UI Humanization (TRACK-084–086)**: Renamed engineering terminology to musicologist-friendly language ("Bulk Import" to "Add Compositions", "Sourcing & Quality" to "Collection Review"). Consolidated sourcing tab navigation. Polished collection review UX.
- **Documentation Overhaul**: Comprehensive audit and sync of all documentation files. Updated 25+ files with correct versions, tool paths, and commands. Synced `current-versions.md` with actual source files.

The remaining sections of this document dive deeper into each of these themes, connecting them back to the underlying architecture, schema, and tooling decisions.

---

## Part I: Foundation & Vision (Early Days)

### 1.1 The Problem Statement

**The Challenge:**
Carnatic classical music knowledge exists in fragmented, semi-structured sources:
- Static websites (karnatik.com, shivkumar.org)
- Blogspot composer lists
- PDFs and scanned books
- No unified schema or normalization
- Poor searchability (especially by lyrics)
- Inconsistent multilingual representation
- No structured handling of notation-centric forms
- No editorial workflow or provenance tracking

**The Vision:**
Create an authoritative, multi-platform digital compendium that:
- Unifies scattered sources into a single, normalized system
- Preserves musicological correctness (Pallavi/Anupallavi/Charanams, Ragamalika, Sampradaya)
- Supports multiple musical forms (Krithis, Varnams, Swarajathis)
- Enables fast, multi-field search
- Provides strong editorial governance
- Delivers production-grade engineering

### 1.2 Initial Architecture Decisions

**Technology Stack Selection (Locked Early):**

The foundational technology choices were made with clear rationale:

| Layer | Technology | Rationale |
|-------|-----------|-----------|
| **Mobile** | Kotlin Multiplatform + Compose | Shared codebase for Android & iOS, type-safe, modern UI |
| **Backend** | Kotlin + Ktor | Lightweight, async-first, excellent PostgreSQL support |
| **Database** | PostgreSQL 18+ | Rich type system (enums, JSONB), full-text search, proven reliability |
| **Admin Web** | React + TypeScript + Tailwind | Rapid development, large ecosystem, type safety |
| **Migrations** | Python db-migrate (originally Rust CLI, archived Feb 2026) | Lightweight, no compilation step, shared Python runtime |
| **Cloud** | AWS or GCP | Scalable, managed services, global distribution |

**Key Principle:**
> "Sangita Grantha models Carnatic compositions **as musicians learn and perform them**, not merely as blocks of text."

This principle guided all data modeling decisions, ensuring the system respects musical structure rather than forcing it into generic content management patterns.

### 1.3 Spec-Driven Documentation Architecture (ADR-001)

**Decision Date:** September 2025

**The Problem:**
Documentation was fragmented across module-level folders, duplicated PRDs, and ad-hoc notes that became stale. Engineers struggled to find canonical sources of truth.

**The Solution:**
Adopted a spec-driven documentation architecture with:
- Canonical docs grouped by domain (requirements, API, database, backend, frontend, ops, QA, decisions, AI)
- Front matter metadata (Status, Version, Last Updated, Owners, Related Docs)
- Central index (`application_documentation/README.md`)
- Archived legacy docs with tombstones

**Structure:**
```text
application_documentation/
├── 00-meta/              # Standards, retention plans
├── 01-requirements/      # PRDs, domain models, features
├── 02-architecture/      # System design, tech stack, ADRs
├── 03-api/              # API contracts, integration specs
├── 04-database/         # Schema, migrations, audit logs
├── 05-frontend/         # UI specs for admin web and mobile
├── 06-backend/          # Backend patterns, security
├── 07-quality/          # Test plans, coverage reports
├── 08-operations/       # Config, runbooks
├── 09-ai/               # AI integration docs
└── archive/             # Legacy documentation
```

**Impact:**
- ✅ Single source of truth for all documentation
- ✅ Version-controlled alongside code
- ✅ Clear ownership and discoverability
- ✅ Foundation for future tooling automation

---

## Part II: Database Schema Evolution

### 2.1 Baseline Schema (Migration 01)

**Date:** Early 2025

**Foundation:**
- PostgreSQL extensions: `pgcrypto` (UUID generation), `pg_trgm` (trigram search)
- Core enum types:
  - `workflow_state_enum`: `draft`, `in_review`, `published`, `archived`
  - `language_code_enum`: `sa`, `ta`, `te`, `kn`, `ml`, `hi`, `en`
  - `script_code_enum`: `devanagari`, `tamil`, `telugu`, `kannada`, `malayalam`, `latin`
  - `musical_form_enum`: `KRITHI`, `VARNAM`, `SWARAJATHI`
- Foundational tables: `roles`, `audit_log`

**Design Principles Established:**
1. **Musical Form Awareness**: Every composition classified by `musical_form`
2. **Lyrics and Notation Are Distinct**: Swara notation never stored in lyric tables
3. **Normalized, Editorially Safe**: No denormalized blobs for core musical data
4. **Read-Optimized for Mobile**: Ordered sections, deterministic rendering, trigram indexes

### 2.2 Domain Tables (Migration 02)

**Core Entities:**
- `users`, `composers`, `ragas`, `talas`, `deities`, `temples`
- `krithis` (central entity with `musical_form`, `workflow_state`)
- `krithi_ragas` (supporting ragamalika with `order_index`)
- `krithi_sections` (Pallavi, Anupallavi, Charanams)
- `krithi_lyric_variants` (language/script/sampradaya variants)
- `krithi_lyric_sections` (section-wise lyrics per variant)

**Key Relationships:**
- `Krithi` → `Composer` (many-to-one)
- `Krithi` → `Raga` (one primary, many via `krithi_ragas` for ragamalika)
- `Krithi` → `Tala`, `Deity`, `Temple`
- `KrithiLyricVariant` → `KrithiSection` → `KrithiLyricSection`

### 2.3 Constraints and Indexes (Migration 03)

**Performance Optimizations:**
- Trigram indexes on `title_normalized`, `incipit_normalized` for fast search
- Unique constraints on canonical entities (composers, ragas, talas)
- Foreign key constraints ensuring referential integrity
- Composite indexes for common query patterns

### 2.4 Import Pipeline (Migration 04)

**Staging Architecture:**
- `import_sources`: Origin tracking (name, baseUrl, description)
- `imported_krithis`: Staging records with:
  - Raw data fields (`rawTitle`, `rawLyrics`, `rawComposer`, etc.)
  - `parsedPayload` (JSONB) for structured extraction
  - `importStatus` enum: `pending`, `in_review`, `mapped`, `rejected`, `discarded`
  - `mappedKrithiId` linking to canonical `krithis` after review

**Editorial Safety:**
- Imported data **never auto-published**
- Human review required before canonicalization
- Full audit trail of import → review → mapping workflow

### 2.5 Sections, Tags, Sampradaya, Temple Names (Migration 05)

**Extended Features:**
- `tags`: Controlled vocabulary (festival, bhava, philosophy, etc.)
- `krithi_tags`: Many-to-many with `source` and `confidence` metadata
- `sampradayas`: Lineage/school tracking (PATHANTARAM, BANI, SCHOOL)
- `temple_names`: Multilingual temple names with aliases

### 2.6 Notation Tables (Migration 06)

**Notation-Centric Forms:**
- `krithi_notation_variants`: Notation variants for Varnams/Swarajathis
  - `notationType`: `SWARA` or `JATHI`
  - `talaId`, `kalai`, `eduppuOffsetBeats`
  - `variantLabel`, `sourceReference`
- `krithi_notation_rows`: Individual notation rows
  - `sectionId`, `orderIndex`
  - `swaraText`, `sahityaText` (optional alignment)
  - `talaMarkers` for avartanam boundaries

**Critical Design:**
Notation is **independent of lyrics**, allowing multiple notation variants per composition while preserving lyric variants separately.

### 2.7-2.9 Import Workflow & Section Type Enhancements

**Migration 07:** Added `approved` status to import workflow
**Migration 08:** Added `samashti_charanam` section type enum
**Migration 09:** Added advanced section types for complex compositions

### 2.10-2.20 Bulk Import Orchestration & Entity Resolution (Phase 5-6)

**Migration 10:** Bulk import orchestration tables (`import_batches`, `import_task_runs`, stage/status enums, polling indexes)
**Migration 11:** Import hardening (header validation, URL deduplication, operational safety columns)
**Migration 12:** Resolution data columns on `imported_krithis` for entity linking
**Migration 13:** Optimised polling indexes for `SELECT ... FOR UPDATE SKIP LOCKED` pattern
**Migration 14:** Duplicate candidate tracking table
**Migration 15:** Missing import metadata columns backfill
**Migration 16:** Quality scoring columns (`qualityScore`, `confidenceScore`, scoring breakdown)
**Migration 17:** Entity resolution cache table for performance
**Migration 18:** Full-text search indexes on `krithis` table
**Migration 19-20:** Schema fixes and alignment (`imported_krithis` nullable columns, foreign key corrections)
**Migration 21:** Temple source cache table for geocoding results
**Migration 22:** `composer_aliases` table for deduplication across transliteration schemes

### 2.11 Source Authority & Evidence Tracking (Phase 6)

**Migration 23:** Source authority enhancement — `authority_tier` (T1-T5), `reliability_score`, `coverage_metadata` on `import_sources`
**Migration 24:** `krithi_source_evidence` table — provenance trail linking each Krithi to its authoritative sources with `contributed_fields`, `confidence_score`, and `extraction_method`
**Migration 25:** `structural_vote_log` table — multi-source consensus tracking with field-level voting
**Migration 26:** `source_format` and `page_range` tracking on import task runs
**Migration 27:** `extraction_queue` table — database-backed work queue for Kotlin ↔ Python integration using `SELECT ... FOR UPDATE SKIP LOCKED`
**Migration 28:** Added `INGESTED` status to extraction_status enum
**Migration 29:** Extraction variant support — `content_language`, `extraction_intent`, `related_extraction_id` columns; `variant_match` table
**Migration 30:** Entity resolution cache schema fix (missing `updated_at` column, `confidence` type)

**Migrations 31–38 (Phase 7):**

**Migration 31:** HTML extraction queue support and index improvements
**Migration 32:** Normalize entity resolution cache confidence type
**Migration 33-34:** Repair and optimize `krithi_lyric_variants` lyrics index
**Migration 35:** Add `krithi_source_evidence(krithi_id, source_url)` composite index
**Migration 36:** Add `strip_diacritics()` PostgreSQL function for search normalization
**Migration 37:** PostgreSQL 18 UUID v7 defaults across 27 tables
**Migration 38:** Fix inconsistent lyric sections (MKS demotion, deduplication, re-indexing)

**Total Evolution:** 38 migrations from baseline to comprehensive sourcing and extraction schema supporting:
- Multiple musical forms (Krithi, Varnam, Swarajathi)
- Multilingual lyrics with sampradaya variants
- Notation variants with tala alignment
- Import pipeline with review workflow and quality scoring
- Source authority tiers and evidence provenance
- Structural voting for multi-source consensus
- PDF extraction queue with variant matching
- Tags, sampradayas, temple names
- Full audit trail

---

## Part III: Frontend Architecture Decision (ADR-002)

### 3.1 The Choice: React vs Kotlin/JS

**Decision Date:** January 2025

**Context:**
Admin web console needed for content management. Choice between:
1. **React with TypeScript**: Industry-standard, large ecosystem
2. **Kotlin/JS (Compose for Web)**: Shared codebase potential with KMP mobile

**Decision:**
Chose **React 19 with TypeScript** for admin web console.

**Rationale:**
1. **Mature Ecosystem**: Extensive UI libraries, documentation, community
2. **Developer Productivity**: TypeScript + React provides excellent DX
3. **Performance**: React 19 + Vite offers fast development builds
4. **Hiring & Maintenance**: React skills widely available
5. **Separation of Concerns**: Admin web distinct from mobile; shared UI code not primary requirement

**Stack:**
- React 19.2.4 (functional components, hooks)
- TypeScript 5.9.x (strict type safety)
- Vite 7.3.1 (modern build tool, fast HMR)
- Tailwind CSS 4.1.18 (utility-first styling)
- React Router 7.13.0 (client-side routing)
- TanStack Query 5.90.20 (data fetching & caching)

**Impact:**
- ✅ Rapid development and iteration
- ✅ Rich UI component ecosystem
- ✅ Type-safe client code
- ✅ Modern developer experience

---

## Part IV: Database Migration Strategy (ADR-003)

### 4.1 The Decision: Rust Over Flyway

**Decision Date:** January 2025

**Context:**
PostgreSQL migration tool needed. Options:
1. **Flyway**: Industry-standard Java-based tool
2. **Custom Rust CLI**: Part of unified `sangita-cli` toolchain

**Decision:**
Chose **Rust-based migration tool** using **sqlx** for database migrations.

**Rationale:**
1. **Unified Tooling**: Migration tool part of Sangita CLI, single interface for all DB operations
2. **Rust Performance**: Fast, reliable database operations without JVM overhead
3. **CLI Integration**: Seamlessly integrated with database reset, seed, and health check commands
4. **Sqlx Reliability**: Compile-time SQL verification and excellent PostgreSQL support
5. **Cross-Platform**: Works consistently across macOS, Linux, and Windows
6. **Simplified Workflow**: `cargo run -- db migrate` simpler than Gradle + Flyway setup

**Implementation:**
- Migration files in `database/migrations/` with naming: `NN__description.sql`
- CLI commands: `db migrate`, `db reset`, `db init`, `db health`
- Migration tracking and application via sqlx

**Trade-offs:**
- ⚠️ Rust dependency (in addition to Java/Kotlin)
- ⚠️ Custom implementation (less standard than Flyway, mitigated by documentation)

**Impact:**
- ✅ Unified workflow for all database operations
- ✅ Fast, reliable migrations
- ✅ Excellent developer experience
- ✅ Cross-platform consistency

### 4.2 Course Correction: Rust CLI to Python db-migrate + Makefile (TRACK-078)

**Date:** February 2026

**The Problem:**
The Rust CLI (`sangita-cli`) had grown beyond its original scope — from a simple migration runner into an orchestration tool for dev workflows, steel thread tests, and extraction management. This introduced Rust as a mandatory build dependency for all developers, even those working solely on the Kotlin backend or React frontend.

**The Decision:**
Archive the Rust CLI and replace it with:
1. **Python `db-migrate`** (`tools/db-migrate/`) — lightweight migration runner using `psycopg`
2. **Makefile** — single entry point for all developer workflows (`make dev`, `make migrate`, `make db-reset`, etc.)
3. **Docker Compose** (`compose.yaml`) — full-stack orchestration

**Rationale:**
1. **Reduced toolchain complexity**: Eliminated Rust as a required dependency
2. **Faster onboarding**: New developers no longer need to compile a Rust binary
3. **Better composability**: Makefile targets are transparent and easy to extend
4. **Python alignment**: Extraction worker already required Python; migration tool shares the runtime

**Impact:**
- ✅ One fewer language in the mandatory toolchain
- ✅ Migration tool installable via `pip` (no compilation step)
- ✅ All workflows accessible via `make <target>`
- ✅ Rust CLI preserved in `tools/sangita-cli-archived/` for historical reference

**Lesson Learned:**
Custom tooling in a specialized language is justified when the team uses that language broadly. When the Rust CLI became the only Rust artifact in active use, the maintenance burden outweighed the benefits. Simplification to well-understood tools (Python + Make) was the right call.

---

## Part V: Authentication Strategy (ADR-004)

### 5.1 JWT with Role-Based Access Control

**Decision:**
JWT-based authentication with role-based access control (RBAC).

**Implementation:**
- JWT tokens with role claims (`editor`, `reviewer`, `admin`)
- `roles` table with `capabilities` (JSONB) for fine-grained permissions
- `role_assignments` table linking users to roles
- Public endpoints: Read-only, no authentication
- Admin endpoints: Require JWT with appropriate roles

**Security:**
- HTTPS only
- Bearer token authentication
- Role-based route protection
- Audit logging for all mutations

---

## Part VI: Database Layer Optimization Revolution

### 6.1 The Problem: DELETE+INSERT Anti-Pattern

**Discovery:** Early 2025

**The Issue:**
Updating child collections (e.g., `KrithiSections`, `LyricVariantSections`, `Tags`, `Ragas`) involved:
- Deleting ALL existing records
- Re-inserting the entire set

**Impact:**
- Loss of metadata (`created_at` timestamps, `notes` fields)
- Unnecessary transaction log expansion
- Index churn and performance degradation
- Potential foreign key relationship breaks
- 2x database round-trips for every create/update operation

### 6.2 The Solution: Smart Diffing with Delta Updates

**Implementation Date:** January 2025

**Algorithm:**
1. **Fetch** existing records from database
2. **Compare** with incoming data to identify:
   - **Updates**: Records that exist in both but have changed values
   - **Inserts**: New records not present in the database
   - **Deletes**: Records in the database not present in the new set
3. **Execute** targeted `batchInsert`, `update`, and `deleteWhere` operations

**Methods Optimized:**
- ✅ `KrithiRepository.saveSections()` - Krithi sections
- ✅ `KrithiRepository.saveLyricVariantSections()` - Lyric variant sections
- ✅ `KrithiRepository.update()` - Raga associations
- ✅ `KrithiRepository.updateTags()` - Tag associations

**Performance Impact:**
- **No changes scenario**: 0 operations (was 6+)
- **One item changed**: 1 operation (was 6+)
- **One item added**: 1 operation (was 7+)
- **83-100% reduction** in database operations for typical updates

### 6.3 Single Round-Trip Persistence: Exposed RC-4

**Upgrade:** Exposed ORM 1.0.0-rc-4

**Innovation:**
Leveraged PostgreSQL's `RETURNING` clause to eliminate redundant SELECT queries.

```sql
**Before (Two Round-Trips):**
// Query 1: INSERT
KrithisTable.insert { ... }
// Query 2: SELECT (to return the created entity)
KrithisTable.selectAll().where { id eq newId }.single()
```

```text
**After (Single Round-Trip):**
// Single query: INSERT with RETURNING
KrithisTable.insert { ... }
    .resultedValues
    ?.single()
    ?.toKrithiDto()
```

**Update Pattern:**
// Before: UPDATE + SELECT
KrithisTable.update { ... }
KrithisTable.selectAll().where { ... }.singleOrNull()

```text
// After: UPDATE with RETURNING
KrithisTable.updateReturning(where = { id eq javaId }) { ... }
    .singleOrNull()
    ?.toKrithiDto()
```

**Repositories Modernized:**
All 9 repository classes now use `resultedValues` for creates and `updateReturning` for updates.

**Performance Gains:**
- **40-50% reduction** in queries per create/update operation
- **Eliminated network round-trip** latency
- **Atomic operations** reduce lock contention
- **Better transaction efficiency**

---

## Part VII: Developer Experience Transformation

### 7.1 The Challenge: Multi-Platform Complexity

**Problem:**
Complex monorepo requiring:
- Java 25 (Kotlin/JVM backend, Android)
- Python 3.11 (Migration tool, extraction worker)
- Bun 1.3.6 (Frontend package management)
- PostgreSQL 18 (Database)
- Docker Compose (Container orchestration)

**Pain Points:**
- 2-4 hours initial setup time per developer
- Version mismatches causing "works on my machine" issues
- Inconsistent configuration files across platforms
- Different database setup procedures (macOS vs Linux vs Windows)
- Manual PATH configuration and toolchain management

### 7.2 The Solution: mise + Unified Bootstrap

**Implementation Date:** January 2026

**Architecture Decision:**
Chose **mise** (formerly rtx) as unified toolchain version manager:
- ✅ Cross-platform (macOS, Linux, Windows)
- ✅ Fast (Rust-based)
- ✅ Simple configuration (single `.mise.toml` file)
- ✅ Automatic PATH management

**Configuration:**
```toml
# .mise.toml
[tools]
java = "temurin-25"      # Matches Gradle toolchain requirement
python = "3.11"          # Migration tool & extraction worker
bun = "1.3.6"            # Frontend package manager
```

**Bootstrap Scripts:**
Created unified bootstrap scripts (`tools/bootstrap` for Unix, `tools/bootstrap.ps1` for Windows) that:
1. Install toolchain via mise (Java, Python, Bun)
2. Verify Docker + Docker Compose availability
3. Create canonical config files from templates
4. Start PostgreSQL 18 via Docker Compose
5. Run database reset (drop → create → migrate → seed)
6. Install frontend dependencies

**Result:**
- **Single command setup**: `./tools/bootstrap`
- **5-10 minutes** total setup time (down from 2-4 hours)
- **100% consistency** across all platforms
- **Idempotent operations** (safe to run multiple times)

**Impact:**
- ✅ **Onboarding time**: < 30 minutes (down from 2.5-4.5 hours)
- ✅ **Environment consistency**: 100% across all developers
- ✅ **Setup success rate**: > 95%
- ✅ **Reduced support burden**: Minimal environment troubleshooting

---

## Part VIII: Code Quality & Workflow Enforcement

### 8.1 Commit Guardrails System

**Implementation Date:** January 2025

**Philosophy:**
Every code change must be traceable to documented requirements, ensuring:
- Code changes are properly documented
- Commits are logically grouped
- Features are traceable to requirements
- No accidental secret commits

### 8.2 Git Hooks

**Technology Choice:**
Originally implemented in Rust (`tools/sangita-cli`, now archived), later maintained via Claude Code hooks and Makefile targets:
- Fast execution (< 500ms target)
- Cross-platform compatibility
- Seamless IDE integration

**Commit Message Format:**
<subject line>

Ref: application_documentation/01-requirements/features/my-feature.md

```text
<optional body>
```

**Git Hooks:**
1. **`commit-msg` Hook**: Validates commit message format and documentation reference
2. **`pre-commit` Hook**: Scans staged files for sensitive data (API keys, secrets)

**Validation Logic:**
- Extracts reference using regex: `(?i)ref:\s*(.+?)(?:\n|$)`
- Ensures single reference per commit (1:1 mapping)
- Verifies file exists in `application_documentation/`
- Normalizes paths (handles relative/absolute paths)

**Sensitive Data Detection:**
- API keys: `api[_-]?key`, `SG_GEMINI_API_KEY`
- Secrets: `secret`, `password`, `token`
- AWS credentials: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
- Provides file location and line number for detected issues

**Impact:**
- ✅ **100% adoption** of documentation references in commits
- ✅ **0 incidents** of committed API keys or secrets
- ✅ **< 500ms** hook execution time (p95)
- ✅ **< 1%** false positive rate

---

## Part IX: AI Integration (ADR-006)

### 9.1 Google Gemini Integration

**Decision Date:** January 2026

**Decision:**
Integrated Google Gemini models (Gemini 2.0 Flash and Gemini 1.5 Pro) for intelligent content ingestion.

**Use Cases:**
1. **Multi-Script Transliteration**: Automating conversion between Indian scripts and Latin
2. **Intelligent Web Scraping**: Extracting structured JSON data from unstructured HTML sources
3. **Metadata Normalization**: Fuzzy matching and canonicalizing entities (Composers, Ragas)
4. **Musicological Validation**: Verifying structural integrity (Pallavi/Charanam detection) and Raga alignment

**Rationale:**
1. **Superior Indic Language Support**: High proficiency in Indian languages
2. **Structured Output (JSON Mode)**: Native support for enforcing JSON schemas
3. **Cost-Performance Ratio**: ~$3-5 per 1,000 kritis
4. **Long Context Window**: Process entire web pages in single pass

**Compliance & Governance:**
- ✅ **Audit Logging**: All AI-generated changes logged in `AUDIT_LOG` table with model version
- ✅ **Human-in-the-Loop**: AI outputs stored in staging area (`imported_krithis`) requiring human approval
- ✅ **Security**: API keys managed via environment variables, never committed

**Impact:**
- **Scalability**: Ingestion throughput expected to increase by order of magnitude
- **Accessibility**: Automatic multi-script generation
- **Data Quality**: Automated validation reduces expert reviewer burden

---

## Part X: Testing Infrastructure

### 10.1 Steel Thread Testing

**Purpose:**
End-to-end smoke test that verifies core functionality of the entire stack.

**Scope:**
1. ✅ Database connectivity and migrations
2. ✅ Backend health endpoint
3. ✅ Public API endpoints (Krithi search)
4. ✅ Admin API endpoints (with authentication)
5. ✅ Frontend dev server startup

**Execution:**
```bash
make steel-thread
```

**Phases:**
1. **Database & Migrations**: Ensures PostgreSQL is running, applies migrations
2. **Backend Verification**: Starts Ktor server, tests `/health` and search endpoints
3. **Frontend Launch**: Starts React admin web frontend
4. **Manual Verification**: Services remain running for manual QA

**Deliverables:**
- ✅ Automated verification of core system components
- ✅ Running services for manual QA
- ✅ Clear success/failure indicators
- ✅ Troubleshooting information on failure

### 10.2 Integration Testing

**Infrastructure:**
- Ktor `testApplication` with test database
- Deterministic fixtures with fixed UUIDs
- Ktor Client for API testing
- Coverage spans health routes, OTP auth, admin sangita lifecycle, pagination, participant rosters/payments

**Test Execution:**
# Seed test data
./gradlew :modules:backend:api:seedTestData

# Run integration tests
```text
./gradlew :modules:backend:api:test
```

### 10.3 API Coverage Analysis

**Comprehensive Analysis:**
- Identified 3 missing lyric variant management endpoints (blocking Lyrics tab functionality)
- Path standardization plan: All admin routes to `/v1/admin/` prefix
- RBAC implementation plan for fine-grained access control
- User management routes design

**Implementation Plan:**
Detailed implementation plan created with:
- Phase-by-phase rollout strategy
- Code patterns and reference implementations
- Testing requirements
- Success criteria

---

## Part XI: Scraping Pipeline Evolution

### 11.1 From TextBlocker to KrithiStructureParser

**Evolution Date:** January – February 2026 (TRACK-034, TRACK-036)

**The Problem:**
The original `TextBlocker` was a simple text chunker that structured HTML content into prompt blocks for the Gemini LLM. All lyric section extraction, metadata parsing, and structure detection was delegated to the LLM — consuming significant tokens and occasionally producing formatting hallucinations.

**The Solution:**
`TextBlocker` was progressively enhanced and ultimately renamed to **`KrithiStructureParser`** — a deterministic, regex-based parser that understands Carnatic music section structure natively.

**Key Capabilities:**
- ✅ **Section Detection**: Regex-based identification of Pallavi, Anupallavi, Charanam, Samashti Charanam, Madhyama Kala Sahityam, and Chittaswaram
- ✅ **Ragamalika Awareness**: Detects raga sub-sections within blocks (e.g., "1. Sri Raga", "Viloma - Mohana") and extracts them as distinct labelled sections
- ✅ **Multi-Script Support**: Handles Devanagari headers, transliterated labels (both `ch` and `c` prefixes for Charanam), and abbreviation patterns
- ✅ **Madhyama Kala Detection**: Correctly flushes current block and starts new `MADHYAMA_KALA` sections when encountering sub-headers
- ✅ **Strongly-Typed Output**: Returns `List<ScrapedSectionDto>` instead of raw text blocks

**Token Savings:**
When deterministic extraction succeeds (sections and variants found), the full lyric text is **excluded from the Gemini prompt**, reducing token usage by approximately **90%**. The LLM is then asked only for metadata (Raga, Tala, Meaning, Temple) — high-value enrichment that is not regex-friendly.

### 11.2 Composer Deduplication & Entity Resolution

**Implementation Date:** January – February 2026 (TRACK-031)

**Problem:** Different transliteration schemes for the same composer created duplicate database records (e.g., "Muthuswami Dikshitar" vs "Muttuswami Diksitar" vs "Muthuswami Dikshithar").

**Solution:**
- Created `composer_aliases` table (Migration 22) linking alternative spellings to canonical composers
- Implemented `ComposerAliasRepository` with query logic for alias resolution
- Added transliteration-aware normalisation in `NameNormalizationService` with aspirate collapse rules

### 11.3 Transliteration-Aware Normalisation

**Implementation Date:** February 2026 (TRACK-061)

**The Problem:**
IAST `ṣ` normalised to `s` after NFD decomposition, but Harvard-Kyoto `sh` stayed as `sh`. These forms never matched despite referring to the same composition.

**The Solution:**
A post-NFD transliteration collapse table applied to all normalised matching keys:

| Input | Output | Example |
|-------|--------|---------|
| `ksh` | `ks` | raksha → raksa |
| `sh` | `s` | shankarabharanam → sankarabaranam |
| `th` | `t` | dikshithar → diksitar |
| `ch` | `c` | charanam → caranam |
| `dh` | `d` | dhyana → dyana |
| `bh` | `b` | bhairavi → bairavi |

Rules are applied longest-first to prevent partial replacement and are used **only for matching keys**, not display titles.

---

## Part XII: PDF Extraction & Data Sourcing Pipeline

### 12.1 Python PDF Extraction Service

**Implementation Date:** February 2026 (TRACK-041, TRACK-053–055)

**Architecture:**
A new Python-based PDF extraction service was created to handle authoritative PDF sources (e.g., guruguha.org compendium with ~486 Dikshitar compositions). The service runs as a Docker container, communicating with the Kotlin backend via a database-backed extraction queue.

**Components:**
- **PdfExtractor**: Core extraction engine using PyMuPDF for text extraction
- **PageSegmenter**: Identifies metadata lines, section headers, and lyric content
- **MetadataParser**: Extracts Raga, Tala, and Composer from body text
- **StructureParser**: Detects Pallavi, Anupallavi, Charanam, and other section types
- **Worker**: Polls `extraction_queue`, processes PDFs, writes `CanonicalExtractionDto` results

**Docker Infrastructure:**
- Dockerfile with Tesseract + Indic language packs for OCR fallback
- Docker Compose extension for local development
- CLI management via `make dev` (starts extraction worker alongside other services)

### 12.2 Diacritic Normalisation (English PDFs)

**Problem:** The guruguha.org English PDF used Utopia fonts that produced garbled diacritics when extracted (e.g., `r¯aga ˙m` instead of `rāgaṁ`). Result: 480 of 481 Krithis had "Unknown" for Raga and Tala.

**Solution (TRACK-054):**
- Created `DiacriticNormalizer` utility with 31 unit tests
- Mapped garbled sequences to Unicode: `¯a→ā`, `˙m→ṁ`, `.n→ṇ`, `.t→ṭ`
- Added garbled-form regex patterns to MetadataParser and StructureParser
- **Result**: ≤5 Krithis with Unknown raga (down from 480); ≥430 Krithis with 3+ sections (up from 0)

### 12.3 Velthuis Sanskrit Font Decoder

**Problem:** Sanskrit PDFs used Velthuis-dvng Type 1 fonts with no `/ToUnicode` CMap. Standard PDF extraction returned garbage characters.

**Solution (TRACK-055):**
- Created `VelthuisDecoder` that parses the Type 1 font program to extract the encoding vector
- Built a 155-entry byte→Unicode glyph mapping table
- Implemented left-side mātrā reordering and vowel merging for correct Devanagari rendering
- Auto-detection: PdfExtractor identifies Velthuis-dvng fonts and applies decoder per span
- Output includes IAST `alternateTitle` for cross-language matching
- **23 unit tests**, 67 total tests passing

### 12.4 Language Variant Pipeline

**Implementation Date:** February 2026 (TRACK-056–058)

**Purpose:** Enable a second PDF (e.g., Sanskrit) to be submitted as a **language variant** of an existing extraction (e.g., English), automatically matching compositions across languages.

**Architecture:**
- Database migration adds `content_language`, `extraction_intent` (NEW/ENRICH), `related_extraction_id` to `extraction_queue`
- `VariantMatchingService`: Multi-signal matching (title, IAST alternate title, section structure, section count)
- **Confidence thresholds**: HIGH (≥0.85) auto-approved; MEDIUM/LOW queued for review
- **Anomaly detection**: Compositions in variant PDF but not in related extraction flagged as ANOMALY
- **Structure mismatch**: Different section counts/types flagged (e.g., 3 vs 4 sections) with section-type alignment
- Approved matches create `krithi_lyric_variants` and `krithi_lyric_sections` with source evidence

### 12.5 Source Authority & Evidence Tracking

**Implementation Date:** February 2026 (TRACK-041)

**Source Authority Tiers:**

| Tier | Description | Example |
|------|-------------|---------|
| T1 | Composer's own manuscripts or direct disciples | Historical manuscripts |
| T2 | Authoritative published compendiums | guruguha.org PDFs |
| T3 | Curated reference websites | karnatik.com |
| T4 | Community-maintained resources | Blogspot collections |
| T5 | User-contributed content | Manual entries |

**Evidence Chain:**
- Every Krithi linked to its sources via `krithi_source_evidence`
- Each evidence record tracks: source URL, format, extraction method, contributed fields, and confidence score
- `structural_vote_log` enables multi-source consensus on field values
- `CanonicalExtractionDto`: Universal contract between all source adapters and the resolution pipeline

### 12.6 Bulk Import Idempotency

**Implementation Date:** February 2026 (TRACK-062)

**Problem:** Bulk import retries created duplicate Krithis (32 duplicates from blogspot import), duplicate sections (9 sections instead of 3), and no source evidence records.

**Solution:**
- Deduplication check via `findDuplicateCandidates(titleNormalized)` before Krithi creation
- Section creation idempotency (skip if `(krithi_id, section_type, order_index)` exists)
- Lyric variant dedup guard on `(krithi_id, language, script, source_reference)`
- Source evidence records written during import task completion
- `idempotency_key` enforcement via `ON CONFLICT DO NOTHING` on `import_task_run`
- **Migration-based integration test infrastructure**: Replaced SchemaUtils with `MigrationRunner` that applies real SQL migration files, ensuring test schema matches production

---

## Part XIII: Sourcing & Quality UI

### 13.1 Design System Components

**Implementation Date:** February 2026 (TRACK-044)

**12 shared components** purpose-built for the sourcing module:

| Component | Purpose |
|-----------|---------|
| TierBadge | Colour-coded authority tier (T1=gold through T5=grey) |
| FormatPill | Document format badges (PDF, HTML, DOCX, API, MANUAL) |
| ConfidenceBar | Gradient-filled 0–1 bar (red→amber→green) |
| StructureVisualiser | Coloured block diagram of Krithi sections (P=blue, A=green, C=amber) |
| FieldComparisonTable | Multi-source diff with colour-coded agreement/conflict cells |
| TimelineCard | Vertical state transition timeline |
| MetricCard | Summary card with trends and sparklines |
| HeatmapGrid | Composer × Field coverage matrix |
| ProgressBarRow | Phase tracking with target/actual/percentage |
| JsonViewer | Collapsible syntax-highlighted JSON |
| StatusChip | Extended with extraction-specific states and pulse animation |
| SourcingErrorBoundary | Module-level error boundary with retry |

### 13.2 Nine Sourcing Screens

**Screens delivered (TRACK-046–052):**

1. **Sourcing Dashboard** — Top-level metrics, phase progress, gap analysis
2. **Source Registry** — CRUD for import sources with authority tier management
3. **Extraction Monitor** — Real-time job tracking with status transitions
4. **Source Evidence Browser** — Provenance explorer linking Krithis to sources
5. **Structural Voting** — Multi-source consensus review and manual override
6. **Quality Dashboard** — Coverage heatmaps, confidence distributions, gap identification
7. **Variant Match Review** — Cross-language match approval/rejection
8. **Extraction Detail** — Per-extraction result viewer with JSON payload
9. **Krithi Evidence Integration** — Sourcing data embedded in the existing Krithi Editor

**Technical Foundation:**
- Route prefix `/admin/sourcing` with lazy-loaded route splitting
- `SourcingLayout` wrapper with sub-navigation tabs and breadcrumbs
- TypeScript interfaces for all 20+ sourcing domain models
- TanStack Query hooks with query key factory for all CRUD operations
- `sourcingApi.ts` typed API client module

---

## Part XIV: Unified Extraction Architecture (TRACK-064)

### 14.1 The Heuristic Split Problem

**Discovery Date:** February 2026

**The Problem:**
Extraction logic was duplicated across two languages: `KrithiStructureParser.kt` (Kotlin) and `structure_parser.py` (Python). Bug fixes in one codebase weren't reflected in the other, causing silent divergence. A `MADHYAMAKALA` splitting fix in Python didn't propagate to Kotlin, leading to section count mismatches.

**Root Cause:**
The original architecture delegated different extraction formats to different runtimes — Kotlin handled HTML scraping inline, Python handled PDFs. Both implemented their own section detection logic independently.

### 14.2 The Consolidation

**Solution (TRACK-064, three phases):**

1. **Phase 1 — HTML Migration**: Created `html_extractor.py` using BeautifulSoup4, replacing Kotlin's inline HTML scraping. All HTML submissions now routed through the extraction queue, processed by Python.
2. **Phase 2 — Heuristic Consolidation**: Migrated 100+ Indic script regex rules from Kotlin to Python. Consolidated section detection, metadata parsing, and multi-script header recognition into a single Python codebase.
3. **Phase 3 — Gemini Enrichment**: Created `gemini_enricher.py` with schema-driven metadata enrichment, exponential backoff retries, and response validation.

**Supporting infrastructure:**
- `identity_candidates.py` with RapidFuzz fuzzy matching for Raga/Composer resolution
- `ExtractionResultProcessor.kt` — Kotlin becomes a pure ingestion consumer
- 5 database migrations for queue support, index improvements, and metadata tracking
- 97 passing Python tests, validated E2E with blogspot-html and pdf-smoke fixtures

### 14.3 Architecture After Consolidation

```
Submission (HTML/PDF/OCR)
    → extraction_queue (PostgreSQL)
        → Python Worker (krithi-extract-enrich-worker)
            ├── html_extractor.py (HTML sources)
            ├── pdf_extractor.py (PDF sources)
            ├── structure_parser.py (section detection — SINGLE SOURCE OF TRUTH)
            ├── metadata_parser.py (raga/tala/composer)
            ├── gemini_enricher.py (AI metadata enrichment)
            └── identity_candidates.py (fuzzy entity matching)
        → CanonicalExtractionDto (result)
    → Kotlin Backend (ExtractionResultProcessor)
        → Krithi creation / evidence persistence / variant matching
```

**Key Principle Established:**
> Intelligence lives in Python. Ingestion lives in Kotlin. Review lives in the Curator UI.

### 14.4 Lessons from the Migration

1. **Heuristic split is a leaky abstraction** — Complex domain logic must be centralized. Duplicating regex patterns across languages guarantees silent divergence.
2. **Schema mismatches cause silent failures** — The `raga` (scalar) vs `ragas[]` (array) mismatch between Kotlin and Python inflated false collision counts until discovered during code review.
3. **Container volume mounting is essential** — COPY-based Docker builds created a rebuild-per-change tax. Switching to volume mounts (`./src:/app/src:ro`) restored dev velocity.
4. **CLI test fixtures beat large-set validation** — Controlled 3-row and 200-row deterministic fixtures catch real issues faster than full-dataset runs.

---

## Part XV: PostgreSQL 18 Upgrade & UUID v7 (TRACK-072)

### 15.1 The Upgrade

**Date:** February 2026

**Changes:**
- Docker Compose image: `postgres:15-alpine` to `postgres:18.3-alpine`
- Volume mount path updated for PG18 layout
- Migration `37__pg18_uuidv7_defaults.sql`: switched 27 tables from `gen_random_uuid()` to `uuidv7()`

### 15.2 UUID v7 Benefits

UUID v7 encodes a millisecond timestamp in the first 48 bits, providing:
- **Temporal ordering**: UUIDs sort by creation time, improving B-tree index locality
- **Reduced page splits**: Sequential inserts cluster on the same index pages
- **Built-in temporal awareness**: Creation timestamp extractable from the UUID itself
- **Native PostgreSQL 18 support**: No extension or custom function required

### 15.3 Impact

- Better query performance on UUID-keyed tables (insertion-order sorting)
- Simplified temporal debugging (UUID encodes its creation time)
- Aligned with modern database best practices

---

## Part XVI: Backend Refactoring Arc (TRACK-073–076)

### 16.1 The Problem: Monolithic Growth

By February 2026, several backend files had grown beyond maintainable size:
- `SourcingDtos.kt`: 300+ lines of unrelated DTOs
- `KrithiRepository`: 500+ lines mixing search, lyric, and CRUD operations
- `BulkImportService`: multiple unrelated responsibilities
- `GeminiApiClient`: retry logic, model config, and API calls in one class

### 16.2 Systematic Decomposition

**TRACK-073 — Shared DTOs & Mappers:**
- Split `SourcingDtos.kt` into `SourcingDtos.kt`, `EvidenceVotingDtos.kt`, and domain-specific files
- Split `DtoMappers.kt` into `CoreEntityMappers.kt`, `ImportDtoMappers.kt`, `KrithiDtoMappers.kt`

**TRACK-074 — DAL Repositories:**
- Extracted `KrithiLyricRepository` (lyric-specific queries)
- Extracted `KrithiSearchRepository` (search operations with different query patterns)
- Extracted `BulkImportEventRepository` and `BulkImportTaskRepository`

**TRACK-075 — Core Services:**
- Extracted `ImportReportGenerator`, `LyricVariantPersistenceService`, `VariantScorer`
- Extracted `StructuralVotingProcessor`, `KrithiMatcherService`
- Added `AuditDataModels` and `AuditSqlQueries`

**TRACK-076 — Infrastructure Services:**
- Extracted `GeminiModels` and `GeminiRetryStrategy` (injectable retry policy)
- Extracted `ScrapingPromptBuilder` (reusable across source types)
- Extracted `SectionHeaderDetector` (independently testable)

### 16.3 Impact

- **9,000+ lines** reorganized into focused, single-responsibility modules
- Reduced file sizes from 500+ to <300 lines per file
- Easier testing in isolation
- Reduced merge conflicts on shared files
- Clearer onboarding path for new developers

---

## Part XVII: Data Integrity Remediation (TRACK-079)

### 17.1 The Discovery

**Date:** February 2026

During E2E pipeline validation, a critical data integrity issue was uncovered:
- **435 out of 473 krithis (92%)** had inconsistent `krithi_lyric_sections` counts across their 6 language variants
- **40 krithis** had zero sections in at least one variant
- Root cause: `structure_parser.py` parsed section labels differently per script (Devanagari vs Roman vs Tamil)

### 17.2 The Root Causes

1. **Dual-format text handling**: Some PDFs contained both continuous and word-division forms of lyrics. The parser treated these as separate sections.
2. **MKS (Madhyama Kala Sahityam) classification**: Top-level MKS sections were created instead of being demoted to sub-sections within their parent Charanam.
3. **Indic anusvara variations**: Different representations of nasal consonants (`ṁ` vs explicit `m`) caused duplicate section detection.
4. **Index-based matching**: `LyricVariantPersistenceService` used order-index matching instead of type+queue matching, causing misalignment when section counts differed between variants.

### 17.3 The Fix

- Rewrote `structure_parser.py` with MKS demotion, dual-format merge, bracket headers, and Indic anusvara normalization
- Updated `LyricVariantPersistenceService.kt` to use type+queue matching
- Fixed `KrithiLyricRepository.kt` ordering via JOIN instead of sub-select
- **Migration 38** (`fix_inconsistent_lyric_sections.sql`): removed MKS top-level sections, deduplicated, re-indexed
- Repair scripts filled 34 zero-section krithis, fixed 3 with inconsistent counts
- **Result: 0 krithis with mismatched section counts** (down from 435)

### 17.4 The Auto-Creation Fix

**Additional discovery:** `KrithiMatcherService` was auto-creating new krithis when no match was found during extraction, bypassing curator review entirely.

**Fix:** Route unmatched extractions to `imported_krithis` as PENDING status, requiring human review before canonicalization.

**Lesson:** Automated systems should never create canonical records without human approval. The curator review gate is a safety boundary, not a convenience feature.

---

## Part XVIII: Curator Review & UI Polish (TRACK-080, 084–086)

### 18.1 Curator Review Interface

**Purpose:** Provide musicologist curators with tools to review unmatched extractions and data quality issues.

**Implementation:**
- `CuratorReviewPage.tsx` with two-tab interface:
  - **Pending Matches**: Unmatched extraction results awaiting curator decision
  - **Section Issues**: Krithis with structural inconsistencies across variants
- `ConfirmationModal.tsx` for approve/reject/merge workflows
- Keyboard shortcuts: `j`/`k` navigation, `a`=approve, `r`=reject
- Backend `CuratorRoutes.kt` with stats and section-issue detection endpoints

### 18.2 UI Terminology Humanization

**The Insight:** The admin UI used engineering terminology that created cognitive friction for musicologist users. "Bulk Import", "Extraction Queue", "Source Evidence" are developer concepts, not music concepts.

**Changes:**
- "Bulk Import" to "Add Compositions"
- "Sourcing & Quality" to "Collection Review"
- Updated 6 sourcing pages with music-domain language
- Added helper functions (`statusLabel`, `formatDuration`) for human-readable display

### 18.3 Tab Consolidation & UX Polish

- Consolidated sourcing sub-navigation tabs for cleaner information architecture
- Improved import detail page layout
- Added collection review UX polish (loading states, empty states, error boundaries)

**Lesson:** UI language should match the user's domain vocabulary. Musicians think in "compositions" and "collections", not "imports" and "extractions".

---

## Part XIX: Scalability & Future-Proofing

### 19.1 Architecture Evaluation

**Current State:**
Production-ready for small to medium scale (thousands of concurrent users).

**Key Strengths:**
- ✅ Clean domain model with proper normalization
- ✅ Modern tech stack (Ktor, PostgreSQL, React)
- ✅ Strong audit and governance
- ✅ Optimized DAL with RETURNING clauses and smart diffing
- ✅ Multi-platform support (KMM mobile, React web)

**Critical Gaps for Global Scale:**
- ❌ No caching layer (every request hits database)
- ❌ No CDN for static assets and API responses
- ❌ No rate limiting on public endpoints
- ❌ Single database instance (no read replicas)
- ❌ No horizontal scaling strategy
- ❌ No geographic distribution
- ❌ Search relies solely on PostgreSQL (no dedicated search engine)

### 19.2 Scaling Strategy Documents

**Comprehensive Analysis:**
- **Scaling Evaluation**: Detailed analysis of current architecture and scaling requirements
- **Google Cloud Scaling Strategy**: Cost-effective, global deployment architecture
- **Global Scale Architecture Proposal**: Hub-and-spoke model for millions of users

**Recommendations:**
1. **Edge-First Delivery**: Offload 90% of read traffic to CDNs and Edge Caches
2. **Read-Write Splitting**: Decouple high-volume public reads from transactional admin writes
3. **Dedicated Search Service**: Move search workloads from PostgreSQL to Elasticsearch/OpenSearch
4. **Asynchronous AI Pipeline**: Decouple heavy AI/Scraping tasks using durable message queues

**Target Scale:**
- **Current**: Thousands of concurrent users
- **Target**: 5M+ Monthly Active Users (MAU), distributed globally
- **Latency Goal**: <100ms for 95% of requests globally

---

## Part XIX-B: Engineering Best Practices Established

### 19B.1 Code Patterns

**Database Operations:**
- ✅ Always use `DatabaseFactory.dbQuery { }` for database access
- ✅ Return DTOs, never Exposed entity objects
- ✅ Use transactions for multi-step operations
- ✅ Use `insert().resultedValues` for create operations
- ✅ Use `updateReturning()` for update operations
- ✅ Use smart diffing for collection updates

**Service Layer:**
- ✅ Keep routes thin; delegate to services
- ✅ Services orchestrate repository calls
- ✅ Validate business rules in services, not repositories

**Error Handling:**
- ✅ Use sealed results or nullable returns (avoid exceptions unless necessary)
- ✅ Map database errors to structured API errors
- ✅ Log errors with context, return generic messages to clients

**Mutations:**
- ✅ All mutations must write to `audit_log` table
- ✅ Use transactions for atomic operations
- ✅ Validate `musicalForm` before allowing notation operations

### 19B.2 Documentation Standards

**Spec-Driven Development:**
- All features documented in `application_documentation/`
- Architecture Decision Records (ADRs) for major decisions
- Comprehensive API documentation
- Database schema documentation
- Frontend UI specifications

### 19B.3 Dependency Management

**Version Catalog:**
- Centralized dependency management via `gradle/libs.versions.toml`
- No hardcoded versions in `build.gradle.kts`
- Consistent versions across all modules

**Key Versions (as of March 2026):**

For the full, authoritative version list, see [Current Versions](./current-versions.md).

- Kotlin: 2.3.0, Ktor: 3.4.0, Exposed: 1.0.0, Koin: 4.1.1
- React: 19.2.4, TypeScript: 5.9.x, Vite: 7.3.1, Tailwind CSS: 4.1.18
- PostgreSQL: 18.3 (Docker, with UUID v7)
- Python: 3.11 (extraction worker, migrations)
- Logback: 1.5.27, HikariCP: 7.0.2

---

## Part XX: Metrics & Achievements Summary

### 20.1 Performance Metrics

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Database queries per create/update | 2 | 1 | 50% reduction |
| Database operations for partial updates | 6+ | 0-1 | 83-100% reduction |
| Developer setup time | 2-4 hours | 5-10 minutes | 95% reduction |
| Onboarding time | 2.5-4.5 hours | < 30 minutes | 90% reduction |
| Hook execution time | N/A | < 500ms | Fast validation |

### 20.2 Quality Metrics

| Metric | Target | Achieved |
|--------|--------|----------|
| Documentation coverage | 100% | ✅ 100% |
| Commit reference adoption | 100% | ✅ 100% |
| Secret commit incidents | 0 | ✅ 0 |
| Environment consistency | 100% | ✅ 100% |
| Setup success rate | > 95% | ✅ > 95% |

### 20.3 Code Quality Metrics

| Metric | Status |
|--------|--------|
| DELETE+INSERT anti-patterns | Eliminated |
| Repository optimization | 100% complete |
| Exposed 1.0.0 features | Fully leveraged |
| Smart diffing implementation | All collections |
| Audit logging coverage | 100% mutations |
| Backend refactoring | 9,000+ lines reorganized (TRACK-073–076) |
| Extraction heuristic centralization | Python single source of truth |
| Section consistency | 100% (0 mismatches, down from 435) |

### 20.4 Schema Evolution

| Metric | Count |
|--------|-------|
| Database migrations | 38 |
| Core entities | 30+ |
| Enum types | 10+ |
| Indexes | 30+ |
| Foreign key constraints | 40+ |
| UUID v7 tables | 27 |

### 20.5 Architecture Decisions

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| ADR-001 | Spec-driven documentation architecture | Accepted | 2025-09-15 |
| ADR-002 | Frontend Architecture Decision - React vs Kotlin/JS | Accepted | 2025-01-27 |
| ADR-003 | Database Migration Tool Choice - Rust vs Flyway | Accepted | 2025-01-27 |
| ADR-003.1 | Course Correction: Rust CLI to Python db-migrate + Makefile | Accepted | 2026-02-28 |
| ADR-004 | Authentication Strategy - JWT with Role-Based Access Control | Accepted | 2025-01-27 |
| ADR-005 | Graph Database Evaluation for Music-Aware Graph Explorer | Draft | 2025-12-29 |
| ADR-006 | Integration of Google Gemini for Content Ingestion | Accepted | 2026-01-05 |

---

## Part XXI: Lessons Learned & Engineering Insights

### 17.1 Performance Optimization

**Key Insight:**
Modern ORM features (like Exposed's `resultedValues` and `updateReturning`) can provide significant performance improvements with minimal code changes. The upgrade to Exposed RC-4 was a high-leverage decision.

**Takeaway:**
Always evaluate framework capabilities before implementing workarounds. The `RETURNING` clause support in PostgreSQL + Exposed eliminated entire classes of performance issues.

### 17.2 Developer Experience

**Key Insight:**
Investing in developer tooling (mise, bootstrap scripts, commit hooks) pays dividends in team velocity and reduced support burden.

**Takeaway:**
"One command to rule them all" philosophy dramatically improves onboarding and reduces environment-related issues. The upfront investment in tooling standardization prevents ongoing friction.

### 17.3 Code Quality

**Key Insight:**
Enforcing documentation references in commits creates a virtuous cycle: better documentation → better code → better traceability.

**Takeaway:**
Automated guardrails (Git hooks) are more effective than manual processes. Fast validation (< 500ms) ensures developer adoption.

### 17.4 Architecture Decisions

**Key Insight:**
Choosing custom tooling (Rust migration tool) over industry standards (Flyway) can be justified when it provides better integration and developer experience.

**Takeaway:**
Evaluate trade-offs carefully. Custom tooling requires more maintenance but can provide better DX and tighter integration with the rest of the toolchain.

### 17.5 Musicological Modeling

**Key Insight:**
Respecting musical structure (Pallavi/Anupallavi/Charanams, notation variants, sampradaya) in the data model leads to cleaner, more maintainable code.

**Takeaway:**
Domain-driven design principles apply to specialized domains like music. The investment in musicologically correct modeling pays off in query simplicity and data integrity.

### 17.6 Deterministic vs LLM Extraction

**Key Insight:**
Structured, regex-friendly sources (like blogspot posts with consistent headers) should be parsed deterministically rather than delegated to LLMs. The evolution from `TextBlocker` to `KrithiStructureParser` demonstrated that investing in domain-specific regex patterns yields more reliable, faster, and cheaper results.

**Takeaway:**
Reserve LLMs for genuinely unstructured or high-variability content. For structured sources, deterministic parsing with well-tuned regex is superior in reliability, speed, and cost.

### 17.7 Cross-Scheme Normalisation

**Key Insight:**
Indian classical music data arrives in multiple transliteration schemes (IAST, Harvard-Kyoto, ITRANS, simple ASCII). Without a normalisation layer that collapses scheme-specific variations, deduplication is impossible.

**Takeaway:**
Build normalisation as a core service, not an afterthought. The aspirate collapse table is simple (~10 rules) but eliminates an entire class of false negatives in entity matching.

### 17.8 Heuristic Centralisation (Phase 7)

**Key Insight:**
Duplicating domain-specific parsing logic across languages (Kotlin and Python) created a "leaky abstraction" — fixes in one didn't propagate to the other, causing 92% of krithis to have inconsistent section counts.

**Takeaway:**
Complex domain logic must live in exactly one place. The decision to make Python the single source of truth for all composition parsing eliminated an entire class of cross-language divergence bugs.

### 17.9 Toolchain Simplification (Phase 7)

**Key Insight:**
The Rust CLI grew from a focused migration runner into an orchestration tool, making Rust a mandatory dependency for developers who never wrote Rust code. When the CLI became the only Rust artifact in active use, the complexity tax exceeded the benefit.

**Takeaway:**
Periodically audit your toolchain for orphaned dependencies. When a tool's language is no longer used elsewhere in the project, consider rewriting it in a language the team already uses. Python + Makefile replaced Rust with zero loss of functionality.

### 17.10 Data Integrity as First-Class Concern (Phase 7)

**Key Insight:**
The 92% section inconsistency was invisible during normal operation — the UI displayed whichever variant was queried, and no cross-variant consistency check existed. The issue was only discovered during deliberate E2E pipeline validation.

**Takeaway:**
Build data quality assertions into your pipeline, not just your tests. If a critical invariant (e.g., "all language variants of a krithi must have the same section count") isn't continuously validated, it will silently degrade.

### 17.11 Automated Creation Must Have Human Gates (Phase 7)

**Key Insight:**
`KrithiMatcherService` auto-created canonical krithis when no match was found, bypassing curator review. This created orphaned records and undermined the editorial workflow.

**Takeaway:**
Automated systems should never create canonical records without human approval. Route unmatched results to a staging area (PENDING status) and let curators decide. The review gate is a safety boundary, not a convenience feature.

### 17.12 UI Language Must Match User Domain (Phase 7)

**Key Insight:**
Engineering terminology ("Bulk Import", "Extraction Queue", "Source Evidence") created cognitive friction for musicologist users who think in "compositions", "collections", and "sources".

**Takeaway:**
Terminology debt is real. Renaming labels is low-risk and high-impact. Do it early and often as you learn how users actually describe their workflows.

---

## Part XXII: Current State & Future Roadmap

### 22.1 Current Capabilities

**Production-Ready Features:**
- ✅ Complete database schema with 38 migrations (PostgreSQL 18, UUID v7)
- ✅ Optimized data access layer (40-50% query reduction)
- ✅ Cross-platform development environment (95% setup time reduction)
- ✅ Commit guardrails and workflow enforcement (100% documentation coverage)
- ✅ Steel thread and E2E testing infrastructure (Playwright)
- ✅ AI integration for content ingestion (Gemini 2.0 Flash)
- ✅ Comprehensive documentation architecture with guardian audits
- ✅ JWT-based authentication with role-based access control
- ✅ Unified extraction pipeline (Python single source of truth)
- ✅ PDF extraction service (English diacritics + Sanskrit Velthuis decoding)
- ✅ HTML extraction via BeautifulSoup (migrated from Kotlin)
- ✅ Source authority tiers and evidence tracking
- ✅ Language variant matching with confidence scoring
- ✅ 9-screen Collection Review admin UI (humanized terminology)
- ✅ Curator review interface with approve/reject/merge workflows
- ✅ Bulk import with idempotency guards
- ✅ Transliteration-aware entity normalisation
- ✅ Composer deduplication via alias tables
- ✅ 100% section consistency across all krithis and language variants
- ✅ Refactored backend: focused, testable modules (9,000+ lines reorganized)
- ✅ Simplified toolchain: Python db-migrate + Makefile (Rust CLI archived)
- ✅ 86 conductor tracks (76 completed, 4 deferred, 5 deprecated, 1 superseded)

**In Progress / Deferred:**
- 🔄 Frontend E2E test expansion (TRACK-035, deferred)
- 🔄 Mobile app Krithi browsing screens

### 22.2 Planned Enhancements

**High Priority:**
- Mobile app development (Compose Multiplatform)
- Advanced lyric search and ranking
- Additional PDF source integrations (beyond Dikshitar corpus)

**Medium Priority:**
- Caching layer (Redis/Memorystore)
- Rate limiting on public endpoints
- Server-side filtering and pagination improvements
- Public read-only web experience

**Future Considerations:**
- CDN integration for static assets
- Read replicas for database scaling
- Dedicated search service (Elasticsearch/OpenSearch)
- Geographic distribution for global scale
- Media management (audio/notation)
- Automated quality scoring with confidence thresholds

### 22.3 Scaling Roadmap

**Phase 1 (Current):**
- Monolithic Ktor + Single PostgreSQL
- Thousands of concurrent users
- Production-ready for boutique scale

**Phase 2 (Near-term):**
- Caching layer (Redis)
- CDN for static assets
- Rate limiting
- Read replicas

**Phase 3 (Medium-term):**
- Dedicated search service
- Asynchronous AI pipeline
- Horizontal scaling

**Phase 4 (Long-term):**
- Geographic distribution
- Edge-first delivery
- Hub-and-spoke architecture
- 5M+ MAU support

---

## Conclusion

Sangeetha Grantha represents a systematic journey from vision to production-ready platform, combining **musicological rigor** with **best-in-class software engineering**. The evolution demonstrates:

1. **Thoughtful Architecture**: Every decision documented and justified across 7+ ADRs, including course corrections
2. **Performance Excellence**: 40-50% query reduction through modern patterns, UUID v7 for temporal ordering
3. **Developer Experience**: 95% reduction in setup time; further simplified with Makefile + Python toolchain
4. **Code Quality**: 100% documentation coverage with automated enforcement; 9,000+ lines refactored into focused modules
5. **Data Integrity**: 92% section inconsistency discovered and fully remediated; curator review gates prevent automated errors
6. **Architecture Consolidation**: Python as single source of truth for extraction; eliminated heuristic split across languages
7. **Scalability Planning**: Clear roadmap from boutique to global scale
8. **Production-Grade Ingestion**: PDF + HTML extraction, garbled font decoding, language variant matching, and source provenance tracking
9. **Course Corrections**: Willingness to archive tools (Rust CLI), simplify workflows, and humanize terminology when the evidence demanded it

The project stands as a testament to **domain-driven design**, **systematic optimization**, and **engineering excellence**. It respects both **musical tradition** and **modern software practices**, creating a platform that is both **musicologically correct** and **technically sound**.

**Key Principles Established:**
- Spec-driven development
- Documentation as code
- Performance by design
- Developer experience as priority
- Security and audit by default
- Scalability from the start
- Deterministic-first extraction (LLM as enrichment, not parser)
- Source provenance and authority tracking
- Centralise domain logic — never duplicate heuristics across languages
- Data integrity assertions in the pipeline, not just in tests
- Human gates on all canonical record creation
- UI language matches user domain vocabulary

**Next Steps:**
- Mobile app development with Compose Multiplatform
- Advanced lyric search and ranking
- Additional PDF source integrations
- Public read-only web experience
- Caching and rate limiting infrastructure

---

## Appendix: Key Documents & References

### Architecture Decisions
- ADR-001: Spec-driven documentation architecture
- ADR-002: Frontend Architecture Decision - React vs Kotlin/JS
- ADR-003: Database Migration Tool Choice - Rust vs Flyway
- ADR-004: Authentication Strategy - JWT with Role-Based Access Control
- ADR-006: Integration of Google Gemini for Content Ingestion

### Implementation Documents (Phase 1–6)
- Database Layer Optimization & Modernization
- Cross-Platform Development Environment Standardisation
- Commit Guardrails and Workflow Enforcement System
- Exposed RC-4 Features Testing
- API Coverage Implementation Plan
- KrithiStructureParser Deterministic Extraction (TRACK-036)
- Source Authority & Evidence Tracking (TRACK-041)
- PDF Extraction Diacritic Normalisation (TRACK-054)
- Velthuis Sanskrit Font Decoder (TRACK-055)
- Language Variant Matching & Enrichment API (TRACK-056)
- Transliteration-Aware Name Normalisation (TRACK-061)
- Bulk Import Idempotency & Source Evidence (TRACK-062)

### Implementation Documents (Phase 7)
- Unified Extraction Engine Migration (TRACK-064)
- Python Module Promotion (TRACK-065)
- PostgreSQL 18 Upgrade Implementation (TRACK-072)
- Shared DTOs & DAL Mappers Refactoring (TRACK-073)
- DAL Repository Decomposition (TRACK-074)
- Core Services Refactoring (TRACK-075)
- Infrastructure Services Refactoring (TRACK-076)
- Rust CLI Archival (TRACK-078)
- E2E Pipeline Validation & Lyric Section Fix (TRACK-079)
- Curator Review UI (TRACK-080)
- Extraction File Path Support (TRACK-081)
- Agent Skills & Workflows Modernization (TRACK-083)
- UI Terminology Humanization (TRACK-084–086)

### Retrospectives & Course Corrections
- TRACK-064 Extraction Migration Handoff (2026-02-12)
- TRACK-064 Code Review (2026-02-13)
- TRACK-064 Key-Collision Handover (2026-02-13)
- Remediation Technical Retrospective (2026-02-12)

### Scaling & Architecture
- Scaling Evaluation & Strategy
- Google Cloud Scaling Strategy
- Global Scale Architecture Proposal
- Backend System Design

### Core Documentation
- Product Requirements Document
- Domain Model Overview
- Database Schema Overview
- API Contract
- Tech Stack Documentation
- Current Versions (single source of truth)
- Krithi Data Sourcing & Quality Strategy
- PDF Diacritic Extraction Analysis

---

**Document Status**: Current
**Last Updated**: 2026-03-10
**Next Review**: 2026-06-10 (quarterly review)