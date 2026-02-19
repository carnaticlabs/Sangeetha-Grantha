| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.1 |
| **Last Updated** | 2026-02-19 |
| **Author** | Sangita Grantha Architect |

# TRACK-041: Enhanced Sourcing Logic & Structural Voting

## 1. Objective
Harden the Krithi import pipeline by implementing multi-source structural voting and authoritative source prioritization.

## 2. Context
- **Goal**: Ensure the "System of Record" always uses the most musicologically accurate structure.
- **Reference**: [Proposed Sourcing Hierarchy](../application_documentation/07-quality/results/krithi-structural-audit-2026-02.md#5-proposed-sourcing-hierarchy--logic)
- **Strategy**: [Krithi Data Sourcing & Quality Strategy](../application_documentation/01-requirements/krithi-data-sourcing/quality-strategy.md) — Sections 3, 5.3, 8

## 3. Implementation Plan
- [x] Define source authority hierarchy (Tier 1–5) with field-level authority model.
- [x] Design canonical extraction schema for multi-format, multi-source data.
- [x] Create database schema for source evidence tracking and structural vote logging.
- [x] Create extraction_queue integration table (Kotlin ↔ Python).
- [x] Build Python PDF extraction service foundation.
- [x] Update `ImportService` with `ComposerSourcePriority` map.
- [x] Implement `StructuralVotingEngine` to compare multi-source extractions.
- [x] Enhance `KrithiStructureParser` for technical headers (Madhyama Kala, Chittaswaram).
- [x] Add support for script-specific subscript normalization (Tamil subscripts 1-4).
- [x] Create Docker infrastructure (Dockerfile, compose.yaml extension).
- [x] Add `extraction` CLI command to sangita-cli.
- [x] Integrate "Authority Source" validation into the `ImportReview` UI (TierBadge + AuthorityWarning — TRACK-044/052).
- [x] Build Kotlin `ExtractionQueueRepository` and `ExtractionQueueService` (TRACK-045).
- [x] Build `ExtractionResultProcessor` for reading Python extraction results.
- [ ] End-to-end integration test: Kotlin → extraction_queue → Python → results.

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Strategy document | `application_documentation/01-requirements/krithi-data-sourcing/quality-strategy.md` | Comprehensive strategy covering all sourcing & quality work |
| Implementation checklist | `application_documentation/01-requirements/krithi-data-sourcing/implementation-checklist.md` | Phase-by-phase task breakdown with acceptance criteria |
| StructuralVotingEngine | `modules/backend/api/.../services/scraping/StructuralVotingEngine.kt` | Multi-source section structure voting |
| ImportService (enhanced) | `modules/backend/api/.../services/ImportService.kt` | ComposerSourcePriority, structural voting integration |
| KrithiStructureParser (enhanced) | `modules/backend/api/.../services/scraping/KrithiStructureParser.kt` | Multi-script detection, Madhyama Kala, Tamil subscripts |
| CanonicalExtractionDto | `modules/shared/domain/.../import/CanonicalExtractionDto.kt` | Universal extraction format (Kotlin) |
| Canonical schema (Python) | `tools/krithi-extract-enrich-worker/src/schema.py` | Pydantic model matching Kotlin DTO |
| Canonical JSON schema | `shared/domain/model/import/canonical-extraction-schema.json` | JSON Schema for validation |
| Migration 23 | `database/migrations/23__source_authority_enhancement.sql` | Source authority model |
| Migration 24 | `database/migrations/24__krithi_source_evidence.sql` | Source evidence tracking |
| Migration 25 | `database/migrations/25__structural_vote_log.sql` | Structural vote audit trail |
| Migration 26 | `database/migrations/26__import_task_format_tracking.sql` | Format tracking |
| Migration 27 | `database/migrations/27__extraction_queue.sql` | Kotlin ↔ Python extraction queue |
| Source registry seed | `database/seed_data/04_import_sources_authority.sql` | Import sources with tier rankings |
| PDF extraction service | `tools/krithi-extract-enrich-worker/` | Python service (PyMuPDF, OCR, transliteration) |
| Dockerfile | `tools/krithi-extract-enrich-worker/Dockerfile` | Container image with Tesseract + Indic packs |
| Docker Compose | `compose.yaml` | Extended with `krithi-extract-enrich-worker` service |
| CLI extraction command | `tools/sangita-cli/src/commands/extraction.rs` | sangita-cli extraction start/stop/status/logs |
| Sourcing UI/UX Plan | `application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md` | UI/UX requirements for sourcing & extraction monitoring screens |
| ExtractionResultProcessor | `modules/backend/api/.../services/ExtractionResultProcessor.kt` | Processes DONE extraction queue items → source evidence + structural voting |
| Migration 28 | `database/migrations/28__extraction_ingested_status.sql` | Adds INGESTED status to extraction_status enum |

## 5. Progress Log
- **2026-02-07**: Track created to operationalize sourcing findings from TRACK-039.
- **2026-02-08**: Major implementation work completed across backend code and infrastructure:
  - **Backend code**: Implemented authority source mapping and structural voting scaffolding in `ImportService` (`ComposerSourcePriority` map), plus parser upgrades for Madhyama Kala shorthand and Tamil subscript normalization in `KrithiStructureParser`. Created initial `StructuralVotingEngine` service.
  - **Source Authority Model**: 5-tier hierarchy defined with field-level authority (Section 3.3 of strategy). Source registry seed data created for guruguha.org (T1), swathithirunalfestival.org (T2), shivkumar.org (T3), karnatik.com (T4), and existing blogspot sources (T5). See `database/seed_data/04_import_sources_authority.sql`.
  - **Database Migrations** (5 new):
    - Migration 23: `source_tier`, `supported_formats`, `composer_affinity` columns on `import_sources`.
    - Migration 24: `krithi_source_evidence` table — links each Krithi to all contributing sources with per-field provenance.
    - Migration 25: `structural_vote_log` table — audit trail for cross-source structural voting decisions.
    - Migration 26: `source_format` and `page_range` columns on `import_task_run`.
    - Migration 27: `extraction_queue` table — database-backed work queue for Kotlin ↔ Python integration using `SELECT ... FOR UPDATE SKIP LOCKED`.
  - **Canonical Extraction Schema**: Defined in both Kotlin (`CanonicalExtractionDto.kt`) and Python (`schema.py`). This is the universal contract between all source adapters and the resolution pipeline.
  - **Python PDF Extraction Service**: Full project structure created at `tools/krithi-extract-enrich-worker/` with:
    - `extractor.py` — PyMuPDF text extraction with font-size and position data.
    - `page_segmenter.py` — Krithi boundary detection in anthology PDFs.
    - `structure_parser.py` — Section label detection (P/A/C/SC/Chittaswaram).
    - `metadata_parser.py` — Header field extraction (title, raga, tala, deity).
    - `ocr_fallback.py` — Tesseract integration with Indic language packs.
    - `transliterator.py` — indic-transliteration wrapper for script conversion.
    - `worker.py` — Database-queue polling worker (production entry point).
    - `db.py` — PostgreSQL operations for extraction_queue.
    - `cli.py` — CLI for local development and testing.
    - Dockerfile with Tesseract OCR + Indic language packs.
    - Unit tests for schema validation and structure parsing.
  - **Docker Infrastructure**: `compose.yaml` extended with `krithi-extract-enrich-worker` service (profile-gated, depends on postgres health).
  - **CLI Tooling**: Added `extraction` command to `sangita-cli` with `build`, `start`, `stop`, `logs`, `status`, and `restart` subcommands.
  - **Documentation Updates**: Updated `application_documentation/` across 12+ files:
    - `04-database/schema.md` — New tables §10.3–10.6, migration summary §14
    - `02-architecture/backend-system-design.md` — Enhanced §5.4, new §5.8 (containerised extraction)
    - `06-backend/README.md` — New services and extraction architecture
    - `08-operations/README.md` — PDF extraction service operations
    - `01-requirements/README.md`, `01-requirements/features/README.md` — Links to krithi-data-sourcing
    - `07-quality/README.md`, `07-quality/reports/README.md` — Sourcing strategy and audit results
    - Root `README.md` — Comprehensive link updates across all sections
    - All README files audited for missing file links
- **2026-02-09**: Completed ExtractionResultProcessor and closed remaining integration gaps:
  - **ExtractionResultProcessor**: New service that polls DONE extraction queue items, parses `result_payload` as `List<CanonicalExtractionDto>`, matches each to existing Krithis (via normalised title), creates `krithi_source_evidence` records, and runs structural voting for Krithis with multiple sources. Marks processed items as INGESTED.
  - **Migration 28**: Added `INGESTED` status to `extraction_status` DB enum to track Kotlin-processed items.
  - **DAL updates**: Added `markIngested()` to `ExtractionQueueRepository`, `createEvidence()` to `SourceEvidenceRepository`, `createVotingRecord()` to `StructuralVotingRepository`.
  - **DbEnums**: Added `INGESTED` to `ExtractionStatus` enum.
  - **API route**: `POST /v1/admin/quality/extraction/process` triggers batch processing.
  - Wired into DI and routing. Authority Source validation in ImportReview UI and ExtractionQueueRepository+Service confirmed as already completed in TRACK-044/045/052.
  - Remaining: end-to-end integration test (Kotlin → extraction_queue → Python → results → INGESTED).