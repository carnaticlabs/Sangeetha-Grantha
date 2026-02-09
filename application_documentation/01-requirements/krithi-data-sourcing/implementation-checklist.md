| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |
| **Parent Document** | [quality-strategy.md](./quality-strategy.md) |
| **Related Tracks** | TRACK-039, TRACK-040, TRACK-041 |

# Implementation Checklist — Krithi Data Sourcing & Quality Strategy

This checklist provides a detailed, actionable breakdown of every task required to implement the strategy defined in the parent document. Tasks are organised by phase and sprint, with dependencies, acceptance criteria, and the responsible codebase area clearly identified.

---

## Legend

- `[ ]` — Not started
- `[~]` — In progress
- `[x]` — Completed
- `[!]` — Blocked
- **Bold items** are milestone deliverables
- `(TRACK-nnn)` indicates direct contribution to an active track

---

## Phase 0: Foundation & Quality Baseline

> **Goal**: Establish quality baseline, formalise schemas, and set up infrastructure for multi-format ingestion.
> **Timeline**: Sprints 1–2 (~2 weeks)

### 0.1 TRACK-039: Structural Quality Audit (TRACK-039)

- [x] Write SQL audit query: section count mismatch across lyric variants per Krithi
  - **File**: `database/audits/audit_section_count_mismatch.sql`
  - **Acceptance**: Query returns all Krithis where variant section counts differ from `krithi_sections` count
- [x] Write SQL audit query: section label sequence mismatch across variants
  - **File**: `database/audits/audit_label_sequence_mismatch.sql`
  - **Acceptance**: Query detects ordering differences (e.g., Pallavi-Charanam vs. Pallavi-Anupallavi-Charanam)
- [x] Write SQL audit query: orphaned lyric blobs (lyric sections without parent section mapping)
  - **File**: `database/audits/audit_orphaned_lyric_blobs.sql`
- [ ] Run all three audits against production database
- [ ] Analyse failure patterns by source and language
- [ ] **Document audit results in `application_documentation/07-quality/results/krithi-structural-audit-2026-02.md`**
- [ ] Create quality baseline metrics snapshot (total Krithis, % with issues, % by composer)

### 0.2 Canonical Extraction Schema

- [ ] Define JSON Schema for the Canonical Extraction Format
  - **File**: `shared/domain/model/import/canonical-extraction-schema.json`
  - **Fields**: title, alternateTitle, composer, musicalForm, ragas[], tala, sections[], lyricVariants[], deity, temple, templeLocation, sourceUrl, sourceName, sourceTier, extractionMethod, extractionTimestamp, pageRange, checksum
  - **Acceptance**: Schema validates all existing `ScrapeWorker` output when retrofitted
- [x] Create Kotlin data class `CanonicalExtractionDto` matching the schema
  - **File**: `modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/import/CanonicalExtractionDto.kt`
- [x] Create Python dataclass/Pydantic model matching the schema
  - **File**: `tools/pdf-extractor/src/schema.py`
- [x] Write unit tests validating schema compliance for sample Dikshitar and Thyagaraja Krithis

### 0.3 Database Schema Extensions

- [x] Write migration 23: Add `source_tier`, `supported_formats`, `composer_affinity`, `last_harvested_at` to `import_sources`
  - **File**: `database/migrations/23__source_authority_enhancement.sql`
  - **Acceptance**: Existing import_sources rows receive default tier 5
- [x] Write migration 24: Create `krithi_source_evidence` table
  - **File**: `database/migrations/24__krithi_source_evidence.sql`
  - **Acceptance**: Table created with FK to krithis and import_sources, indexes on krithi_id and import_source_id
- [x] Write migration 25: Create `structural_vote_log` table
  - **File**: `database/migrations/25__structural_vote_log.sql`
- [x] Write migration 26: Add `source_format` and `page_range` to `import_task_run`
  - **File**: `database/migrations/26__import_task_format_tracking.sql`
- [x] Write migration 27: Create `extraction_queue` table (Kotlin ↔ Python integration)
  - **File**: `database/migrations/27__extraction_queue.sql`
  - **Schema**: See strategy document Section 8.3.2
  - **Acceptance**: Table created with `extraction_status` enum, partial indexes for polling, FK to import_batch and import_task_run
- [ ] Write migration 28: Create `source_documents`, `extraction_runs`, and `field_assertions` tables
  - **File**: `database/migrations/28__source_documents_extraction_runs.sql`
  - **Acceptance**: Tables created with checksumming, extractor versioning, and per-field provenance tracking
  - **Note**: Can be deferred to medium-term; short-term uses `parsed_payload` JSONB in `imported_krithis`
- [ ] Add indexes for artifact lookup by source domain/checksum
- [ ] Add indexes for extraction run lookup by batch/status
- [ ] Run all migrations against dev database and verify with `sangita-cli db status`
- [ ] Update `application_documentation/04-database/schema.md` with new tables/columns

### 0.4 Source Registry Population

- [x] Insert `import_sources` records for each target source:
  - [x] guruguha.org — tier 1, formats: ["PDF"], composer_affinity: {"dikshitar": 1.0}
  - [x] swathithirunalfestival.org — tier 2, formats: ["HTML"], composer_affinity: {"swathi_thirunal": 1.0}
  - [x] shivkumar.org — tier 3, formats: ["HTML", "PDF", "DOCX"]
  - [x] karnatik.com — tier 4, formats: ["HTML"]
  - [x] Existing blogspot sources — tier 5, formats: ["HTML"]
- [x] Create seed SQL script: `database/seed_data/04_import_sources_authority.sql`

---

## Phase 1: PDF Ingestion — Skeleton Extraction

> **Goal**: Build the Python PDF extraction service and integrate it into the Kotlin orchestration pipeline.
> **Timeline**: Sprints 1–4 (~4 weeks)

### 1.1 Python PDF Extraction Service — Setup

- [x] Initialise Python project structure:
  ```
  tools/pdf-extractor/
    pyproject.toml
    src/
      __init__.py
      cli.py              # CLI entry point (for local dev / testing)
      worker.py            # DB-queue polling worker (production entry point)
      db.py                # PostgreSQL connection and extraction_queue operations
      extractor.py         # PyMuPDF text extraction
      page_segmenter.py    # Krithi boundary detection in anthology PDFs
      structure_parser.py  # Section label detection (P/A/C/SC/Chittaswaram)
      metadata_parser.py   # Header field extraction (title, raga, tala, deity)
      ocr_fallback.py      # Tesseract integration with Indic language packs
      transliterator.py    # indic-transliteration wrapper
      schema.py            # Canonical JSON schema validation (Pydantic)
      config.py            # Environment variable configuration
    tests/
      test_guruguha_mdskt.py
      test_guruguha_mdeng.py
      test_worker.py
      fixtures/            # Sample PDF pages for regression testing
    Dockerfile
    docker-compose.override.yml  # For standalone testing
  ```
- [x] Configure `pyproject.toml` with dependencies:
  - Core: pymupdf (fitz), pdfplumber, pydantic, click
  - OCR: pytesseract
  - Indic: indic-transliteration, aksharamukha
  - Database: psycopg[binary] (async-capable)
  - LLM: google-generativeai (Gemini client)
  - Utilities: httpx (for PDF downloads), structlog (logging)
- [x] Create Dockerfile with Tesseract OCR + Indic language packs (see strategy Section 8.2.2)
  - **Base**: `python:3.11-slim`
  - **System deps**: tesseract-ocr, tesseract-ocr-san, tesseract-ocr-tam, tesseract-ocr-tel, tesseract-ocr-kan, tesseract-ocr-mal, libpq-dev
  - **Entry point**: `python -m src.worker` (DB-queue polling mode)
- [x] Implement `db.py`: PostgreSQL connection pooling + extraction_queue CRUD
  - `claim_pending_task()` — `SELECT ... FOR UPDATE SKIP LOCKED`
  - `mark_processing(task_id, hostname)`
  - `mark_done(task_id, result_payload, metadata)`
  - `mark_failed(task_id, error_detail)`
- [x] Implement `worker.py`: Polling loop (see strategy Section 8.3.3)
  - Configurable poll interval (`EXTRACTION_POLL_INTERVAL_S`)
  - Configurable batch size and max concurrency
  - Graceful shutdown on SIGTERM (for K8s pod termination)
  - Structured logging with task IDs
- [x] Implement `config.py`: Read from environment variables
  - `DATABASE_URL`, `SG_GEMINI_API_KEY`, `EXTRACTION_POLL_INTERVAL_S`, `EXTRACTION_BATCH_SIZE`, `EXTRACTION_MAX_CONCURRENT`, `LOG_LEVEL`
- [x] **Verify setup**: `sangita-cli extraction start --with-db` starts and polls extraction_queue
  - Added `extraction` command to sangita-cli with build/start/stop/logs/status subcommands
- [ ] **Verify setup**: `python -m pytest tools/pdf-extractor/tests/` runs clean locally

### 1.2 PDF Extraction — Core (guruguha.org mdskt.pdf)

- [x] Implement `extractor.py`: PyMuPDF text extraction with font-size and position data
  - **Acceptance**: Extracts full Unicode text from mdskt.pdf including Devanagari
- [x] Implement `page_segmenter.py`: Detect Krithi boundaries in anthology PDFs
  - **Approach**: Font-size change detection + title pattern matching (bold text followed by Raga/Tala header)
  - **Acceptance**: Correctly segments mdskt.pdf into ~484 individual Krithi blocks
- [x] Implement `metadata_parser.py`: Extract title, raga, tala, deity, temple from Krithi headers
  - **Pattern**: Title line → "Raga: X — Tala: Y" → optional "Deity: Z at Temple" → lyrics
  - **Acceptance**: >95% header fields extracted for 10 sample Krithis
- [x] Implement `structure_parser.py`: Detect section labels (Pallavi, Anupallavi, Charanam, Samashti Charanam, Chittaswaram)
  - **Acceptance**: Correctly identifies sections for 10 sample Krithis including Samashti Charanam
- [x] Implement `schema.py`: Validate output against canonical extraction JSON schema
- [x] Implement `cli.py`: CLI entry point
  - **Interface**: `python -m src.cli extract --input <pdf_path_or_url> --output <json_path> [--pages 1-10]`
  - **Output**: JSON array of canonical extraction records
- [ ] **Proof of Concept**: Extract 10 Dikshitar Krithis from mdskt.pdf, validate against existing DB entries
  - [ ] Compare extracted raga/tala with existing `krithis` table records
  - [ ] Verify section structure matches known compositions
  - [ ] Document discrepancies

### 1.3 PDF Extraction — English Edition (mdeng.pdf)

- [ ] Adapt `page_segmenter.py` for English/Latin script PDF layout
- [ ] Adapt `metadata_parser.py` for English header format (may differ from Sanskrit edition)
- [ ] Run extraction on mdeng.pdf
- [ ] **Cross-validate**: Compare section structures between mdskt.pdf and mdeng.pdf extractions for same Krithis
  - **Acceptance**: Section structures match for >98% of compositions

### 1.4 PDF Extraction — OCR Fallback

- [ ] Implement `ocr_fallback.py`: Tesseract integration with Indic language packs
  - **Trigger**: When PyMuPDF text extraction returns empty or >50% garbled characters
  - **Languages**: Sanskrit (Devanagari), Tamil, Telugu, English
- [ ] Test with intentionally degraded PDF samples
- [ ] **Acceptance**: OCR produces usable text for >80% of scanned pages

### 1.5 PDF Extraction — LLM Refinement

- [ ] Create Gemini prompt template for PDF-extracted text (distinct from HTML prompt)
  - **Differences from HTML prompt**: Handle page headers/footers, footnotes, index entries, scholarly annotations
  - **File**: `tools/pdf-extractor/src/prompts/pdf_extraction_prompt.txt`
- [ ] Implement LLM refinement step in extraction pipeline
  - **Approach**: Pattern-matched extraction first, LLM validates and corrects
- [ ] **Acceptance**: LLM catches and corrects >90% of pattern-matching errors in 20 sample Krithis

### 1.6 Kotlin Integration — DB Queue Producer

The Kotlin backend writes extraction requests to the `extraction_queue` table. The Python container (running independently) picks up and processes these tasks. The Kotlin backend then reads completed results.

- [ ] Create `ExtractionQueueRepository`
  - **File**: `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/ExtractionQueueRepository.kt`
  - **Operations**:
    - `submitExtractionTask(batchId, sourceUrl, format, requestPayload)` — INSERT with PENDING status
    - `findCompletedTasks(batchId)` — SELECT WHERE status='DONE'
    - `findFailedTasks(batchId)` — SELECT WHERE status='FAILED'
    - `getQueueStats()` — COUNT by status (for dashboard/observability)
- [ ] Create `ExtractionQueueService`
  - **File**: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/import/ExtractionQueueService.kt`
  - **Responsibilities**:
    1. Submit PDF/DOCX extraction requests to `extraction_queue`
    2. Poll for completed results and feed into Resolution pipeline
    3. Handle retries for failed tasks (reset status to PENDING if attempts < max_attempts)
    4. Report queue health metrics to dashboard
- [ ] Update `ManifestWorker` to detect PDF URLs in manifests
  - When manifest entry has a `.pdf` URL → INSERT into `extraction_queue` (instead of creating SCRAPE task)
  - When manifest entry has `.html` URL → existing ScrapeWorker flow (unchanged)
- [ ] Create `ExtractionResultProcessor` (runs on Kotlin side)
  - **File**: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/import/ExtractionResultProcessor.kt`
  - **Behaviour**:
    1. Polls `extraction_queue` for status='DONE' tasks
    2. Reads `result_payload` JSON (array of CanonicalExtractionDto)
    3. For each extraction: creates `ImportedKrithi` in staging
    4. Queues `RESOLUTION` tasks (same as existing HTML flow)
    5. Updates extraction_queue status to acknowledge processing
- [ ] Add `PDF`, `DOCX`, `IMAGE` to source format handling in `BulkImportRepository`
- [ ] Create `PdfManifestWorker` for PDF-based manifests
  - **Input**: JSON manifest with PDF URL, page ranges per Krithi (or "full" for auto-segmentation)
  - **Output**: `extraction_queue` entries with `source_format: PDF`
- [ ] **Integration test**: Submit batch → extraction_queue populated → Python container picks up → results appear → ResolutionWorker processes
- [ ] **End-to-end test**: Full pipeline for mdskt.pdf (484 Krithis) through staging review → 5 Krithis published

### 1.7 Source Adapter Interface

- [ ] Define `SourceAdapter` interface in Kotlin
  - **File**: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/import/SourceAdapter.kt`
  - **Contract**: `suspend fun extract(sourceUrl: String, format: SourceFormat): List<CanonicalExtractionDto>`
- [ ] Refactor existing `WebScrapingService` to implement `SourceAdapter`
- [ ] Implement `PdfSourceAdapter` wrapping the Python service invocation
- [ ] **Acceptance**: Both HTML and PDF paths produce identical `CanonicalExtractionDto` output

---

## Phase 2: Structural Validation & Voting

> **Goal**: Implement cross-source structural voting to establish verified canonical structures.
> **Timeline**: Sprints 5–6 (~2 weeks)
> **Dependency**: Phase 1 complete (multiple sources ingested for overlapping Krithis)

### 2.1 Structural Voting Engine (TRACK-041)

- [ ] Create `StructuralVotingEngine` service
  - **File**: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/quality/StructuralVotingEngine.kt`
  - **Input**: Krithi ID + list of `krithi_source_evidence` records with extracted section structures
  - **Output**: Consensus section structure + confidence level + vote log entry
- [ ] Implement voting rules:
  - [ ] Unanimous agreement → HIGH confidence
  - [ ] Majority agreement → MEDIUM confidence (dissenting source flagged)
  - [ ] No consensus → LOW confidence (flagged for expert review)
  - [ ] Authority override: Tier 1 always prevails over Tier 4–5
- [ ] Create `KrithiSourceEvidenceRepository`
  - **File**: `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiSourceEvidenceRepository.kt`
- [ ] Create `StructuralVoteLogRepository`
  - **File**: `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/StructuralVoteLogRepository.kt`
- [ ] Wire voting engine into the `ResolutionWorker` post-processing step
- [ ] **Test**: For 50 Dikshitar Krithis sourced from both mdskt.pdf and blogspot, run voting and verify consensus

### 2.2 ComposerSourcePriority Map (TRACK-041)

- [ ] Create `ComposerSourcePriority` configuration
  - **File**: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/config/ComposerSourcePriority.kt`
  - **Content**: Map of composer ID → ordered list of preferred source IDs
  - **Example**: Dikshitar → [guruguha.org, karnatik.com, blogspot]
- [ ] Integrate priority map into `ImportService` for source ranking
- [ ] Update admin UI `ImportReview` to display source tier and authority ranking

### 2.3 Source Evidence Tracking

- [ ] Update `ResolutionWorker` to create `krithi_source_evidence` records after resolution
  - **Fields populated**: krithi_id, import_source_id, source_url, source_format, extraction_method, confidence, contributed_fields
- [ ] Update `KrithiService` to expose source evidence via API
  - **Endpoint**: `GET /v1/admin/krithis/{id}/source-evidence`
- [ ] Update admin UI Krithi detail page to show provenance table

---

## Phase 3: Lyric Enrichment

> **Goal**: Systematically source lyric variants in multiple scripts and align to canonical section structure.
> **Timeline**: Sprints 7–8 (~2 weeks)

### 3.1 Multi-Script Source Ingestion

- [ ] Build `SwathiThirunalManifestWorker` for swathithirunalfestival.org
  - **Approach**: Crawl `/swathi-thirunal/compositions` index, extract slug URLs
  - **Output**: SCRAPE tasks for each composition page
- [ ] Build site-specific Jsoup parser for swathithirunalfestival.org composition pages
  - **Extract**: Title, Raga (with Arohanam/Avarohanam), Tala, lyrics (Sanskrit + Malayalam), meaning
- [ ] Run full ingestion: ~400 Swathi Thirunal compositions into staging
- [ ] Ingest mdeng.pdf (English Dikshitar collection) as lyric variants for existing Dikshitar Krithis
  - **Key**: Match by title/composer to existing entries from mdskt.pdf ingestion

### 3.2 Lyric Variant Alignment

- [ ] Implement `LyricVariantAligner` service
  - **File**: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/quality/LyricVariantAligner.kt`
  - **Responsibility**: Ensure new lyric variant section count and labels match canonical section structure
  - **Handling**:
    - Exact match → link sections 1:1
    - Variant has fewer sections → flag for manual review (possible combined sections)
    - Variant has more sections → flag for manual review (possible split or extra section)
- [ ] Integrate aligner into lyric variant import workflow
- [ ] **Acceptance**: All new lyric variants pass section alignment check before entering staging

### 3.3 Transliteration Service

- [ ] Implement Python transliteration wrapper around `indic-transliteration` library
  - **File**: `tools/pdf-extractor/src/transliterator.py`
  - **Interface**: `transliterate(text, from_script, to_script) -> str`
  - **Supported conversions**: Devanagari ↔ Tamil, Telugu, Kannada, Malayalam, Latin (IAST)
- [ ] Create Kotlin integration (subprocess or HTTP call)
- [ ] Implement `TransliterationEnrichmentJob`:
  - For each Krithi with verified canonical structure but only 1 lyric variant:
    - Generate transliterated variants in missing scripts
    - Tag as `extraction_method: TRANSLITERATION` with `is_primary: false`
    - Stage for review (never auto-publish transliterated text)
- [ ] **Acceptance**: Transliteration preserves sahitya syllable boundaries (manually verified for 20 samples)

---

## Phase 4: Metadata Enrichment

> **Goal**: Populate deity, temple/Kshetra, tags, and Sampradaya metadata.
> **Timeline**: Sprints 9–10 (~2 weeks)

### 4.1 Deity Enrichment

- [ ] Extract deity fields from guruguha.org PDF extractions (Phase 1 output)
- [ ] Implement LLM-based deity inference from lyric text
  - **Prompt**: "Given this Carnatic composition lyric, identify the primary deity being addressed..."
  - **Composer-specific patterns**: Padmanabha → Vishnu (Swathi Thirunal), Rama → Rama (Thyagaraja), Guruguha → Subrahmanya (Dikshitar)
- [ ] Map inferred deities to canonical `deities` table records
- [ ] **Acceptance**: >80% of Dikshitar Krithis have resolved deity after enrichment

### 4.2 Temple/Kshetra Enrichment

- [ ] Extract temple fields from guruguha.org PDF extractions
- [ ] Cross-reference extracted temple names with `temples` table (multilingual matching via `temple_names`)
- [ ] For unmatched temples: create new temple records with available data
- [ ] Enrich temple records with geo-coordinates (from `temple_source_cache` or LLM inference)
- [ ] **Acceptance**: >70% of Dikshitar Krithis have resolved temple after enrichment

### 4.3 Tag Classification

- [ ] Define tag taxonomy expansion if needed (current: Bhava, Festival, Philosophy, Kshetra, Style)
- [ ] Implement LLM-based tag suggestion from lyric content
- [ ] Create batch tagging job for existing PUBLISHED Krithis
- [ ] **Acceptance**: >50% of PUBLISHED Krithis have at least 2 tags

### 4.4 TRACK-040: Remediation Pipeline

- [ ] Implement `MetadataCleanupTask`
  - **File**: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/quality/MetadataCleanupTask.kt`
  - **Action**: Strip boilerplate text from `krithi_lyric_sections` (blog headers, copyright notices, navigation text)
  - **Approach**: Pattern matching + LLM classification of non-lyric content
- [ ] Implement `StructuralNormalisationLogic`
  - **File**: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/quality/StructuralNormalisationLogic.kt`
  - **Action**: Align all lyric variants to canonical section template
- [ ] Run remediation on Dikshitar compositions (TRACK-040 priority 1)
- [ ] Run remediation on Thyagaraja compositions (TRACK-040 priority 2)
- [ ] Re-run `QualityScoringService` on all remediated Krithis
- [ ] **Acceptance**: Zero section drift for all remediated Krithis; quality scores improved

---

## Phase 5: Notation Ingestion

> **Goal**: Ingest Swara notation from practitioner sources into the notation tables.
> **Timeline**: Sprints 11–12 (~2 weeks)

### 5.1 Notation Parser

- [ ] Implement `NotationParser` for HTML notation (shivkumar.org format)
  - **File**: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/notation/NotationParser.kt`
  - **Parse**: Swara text (Sa Ri Ga Ma Pa Da Ni), tala markers, avartanam boundaries
  - **Output**: `KrithiNotationVariantDto` + list of `KrithiNotationRowDto`
- [ ] Detect kalai (speed) and eduppu (starting beat offset) from notation headers
- [ ] Align notation rows with corresponding lyric sections
- [ ] **Acceptance**: Parser correctly structures 10 sample compositions from shivkumar.org

### 5.2 Shivkumar.org Ingestion

- [ ] Build manifest for shivkumar.org compositions (~300 entries)
  - **Format**: JSON manifest with URL, format (HTML/PDF/DOCX), composer, title
- [ ] Build site-specific Jsoup parser for shivkumar.org notation pages
- [ ] Run ingestion: ~300 compositions with notation into staging
- [ ] Link notation variants to existing Krithis via title + composer + raga matching
- [ ] **Acceptance**: 300+ Krithis have at least one notation variant in the database

### 5.3 Notation Quality Checks

- [ ] Validate tala alignment: notation row count per avartanam matches tala beat count
- [ ] Validate section alignment: notation rows map to correct lyric sections
- [ ] Flag inconsistencies for manual review
- [ ] **Acceptance**: >90% of notation variants pass tala alignment check

---

## Phase 6: Continuous Curation & Operations

> **Goal**: Establish ongoing quality monitoring, automated audits, and editorial governance.
> **Timeline**: Ongoing from Sprint 9

### 6.1 Automated Quality Audits

- [ ] Create `QualityAuditScheduler` service
  - **File**: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/quality/QualityAuditScheduler.kt`
  - **Schedule**: Nightly
  - **Audits**: Section count mismatch, label sequence mismatch, orphaned lyric blobs, entity resolution gaps
- [ ] Store audit results in `application_documentation/07-quality/results/` (auto-generated)
- [ ] Create API endpoint for audit results: `GET /v1/admin/quality/audits`

### 6.2 Quality Dashboard

- [ ] Extend `DashboardRoutes` with quality metrics:
  - [ ] Total Krithis by workflow state
  - [ ] Quality tier distribution (Gold/Silver/Bronze/Needs Review)
  - [ ] Source evidence coverage (% with 2+ sources)
  - [ ] Script variant coverage (% with 3+ scripts)
  - [ ] Deity/temple metadata coverage by composer
  - [ ] Notation coverage
- [ ] Update admin web dashboard component to display quality metrics
- [ ] **Acceptance**: Dashboard loads in <2s with all quality metrics

### 6.3 Re-Harvesting Pipeline

- [ ] Implement `SourceRefreshJob`:
  - Periodically check sources for updates (based on `last_harvested_at`)
  - Re-extract and compare with existing data
  - Flag differences for review
- [ ] Configure refresh schedule per source tier:
  - Tier 1–2: Monthly
  - Tier 3–4: Quarterly
  - Tier 5: On-demand only

### 6.4 Auto-Approval Enhancement

- [ ] Update `AutoApprovalService` thresholds:
  - Tier 1 source + quality_score > 85 + HIGH entity resolution → auto-approve
  - Tier 2 source + quality_score > 90 + HIGH entity resolution → auto-approve
  - All other combinations → require manual review
- [ ] Log all auto-approval decisions in audit trail
- [ ] **Acceptance**: Auto-approval correctly handles 20 test cases without false positives

### 6.5 Admin UI Enhancements

- [ ] Import Review page:
  - [ ] Add source tier badge and source format indicator (HTML/PDF/DOCX/OCR)
  - [ ] Show per-field source lineage (which source asserted each field)
  - [ ] Show all structure candidates and selected winner (voting rationale)
  - [ ] Show artifact links (PDF pages, HTML snapshots) and extraction logs
  - [ ] Add filters for low-confidence structural imports
  - [ ] Add bulk actions by confidence tier with safeguards
  - [ ] Add clear manual override audit notes template
- [ ] Krithi Detail page:
  - [ ] Add source evidence panel (all contributing sources with tier and confidence)
  - [ ] Add structural vote history (if multiple sources contributed)
  - [ ] Add quality score breakdown by dimension
  - [ ] Add field-level provenance panel (from `field_assertions` table)
- [ ] Import Dashboard:
  - [ ] Add format-based filtering (HTML/PDF/DOCX)
  - [ ] Add extraction queue status widget (PENDING/PROCESSING/DONE/FAILED counts)
- [ ] Quality tab: Display audit results, quality trends over time, and confidence drift alerts

---

## Cross-Cutting Concerns

### Docker & Container Infrastructure

- [x] Extend `compose.yaml` with `pdf-extractor` service (see strategy Section 8.2.1)
  - **Depends on**: postgres (service_healthy)
  - **Environment**: DATABASE_URL, SG_GEMINI_API_KEY, poll/batch/concurrency config
  - **Volume**: `extraction_cache` for downloaded PDFs
  - **Profile**: `extraction` (opt-in: `docker compose --profile extraction up`)
- [x] Create `tools/pdf-extractor/Dockerfile` with Tesseract OCR + Indic language packs
  - **Base**: python:3.11-slim
  - **System deps**: tesseract-ocr-san, tesseract-ocr-tam, tesseract-ocr-tel, tesseract-ocr-kan, tesseract-ocr-mal, libpq-dev
  - **Acceptance**: `docker build -t sangita-pdf-extractor tools/pdf-extractor/` succeeds; container starts and polls extraction_queue
- [ ] Create `tools/pdf-extractor/docker-compose.override.yml` for standalone testing (pdf-extractor + postgres only)
- [ ] Verify three-container local stack: `sangita-cli extraction start --with-db` + Kotlin backend via Gradle
  - **Acceptance**: Kotlin submits extraction task → Python processes → result appears in extraction_queue
- [ ] Add `pdf-extractor` to `.mise.toml` tool configuration if applicable
- [ ] Configure `extraction_cache` volume retention policy (delete after 30 days, configurable)

### Kubernetes / GCP Production Deployment

- [ ] Create `k8s/pdf-extractor-deployment.yaml` (see strategy Section 8.2.3)
  - Replicas: 2 (baseline), with Cloud SQL Proxy sidecar
  - Resource limits: 2 CPU, 4Gi memory (OCR + PDF parsing is memory-intensive)
  - Volume: emptyDir with 10Gi sizeLimit for extraction cache
- [ ] Create `k8s/pdf-extractor-hpa.yaml` — HorizontalPodAutoscaler
  - Scale on `extraction_queue_pending_count` custom metric
  - Min: 1, Max: 5 replicas
- [ ] Create `k8s/pdf-extractor-configmap.yaml` for non-secret configuration
- [ ] Add pdf-extractor secrets to Google Secret Manager:
  - `sangita-db-credentials/connection-string`
  - `sangita-gemini/api-key`
- [ ] Build and push container image to GCR: `gcr.io/sangita-grantha/pdf-extractor`
- [ ] Test deployment in staging namespace before production rollout
- [ ] Document pod lifecycle: startup → poll → graceful SIGTERM shutdown
- [ ] Add `pdf-extractor` to Cloud Run / GKE deployment documentation

### Observability & Operations

- [ ] Add extraction queue depth metric: `SELECT status, COUNT(*) FROM extraction_queue GROUP BY status`
  - Expose via `GET /v1/admin/extraction-queue/stats` endpoint
- [ ] Add per-format success/failure dashboards (PDF/DOCX/OCR)
- [ ] Add queue depth and stage latency metrics to admin dashboard
- [ ] Add source-level error-rate monitoring
- [ ] Add structured logging in Python worker with task IDs, source URLs, durations
  - Use `structlog` for JSON-formatted logs compatible with Cloud Logging
- [ ] Add alerting for:
  - Queue depth > 100 PENDING for > 30 minutes
  - Error rate > 20% over 1-hour window
  - Sudden confidence drop by source
  - Python container unhealthy / not polling
- [ ] Create operational runbook: `application_documentation/08-operations/pdf-extractor-runbook.md`
  - Troubleshooting stuck tasks
  - Manual retry procedures
  - Container restart and scaling
  - Queue drain and cleanup
- [ ] Add operational playbook for source parser breakage (site redesign handling)

### Testing

- [ ] Unit tests for `CanonicalExtractionDto` serialisation/deserialisation
- [ ] Unit tests for `StructuralVotingEngine` with all voting scenarios
- [ ] Unit tests for `LyricVariantAligner` with match/mismatch/extra/missing scenarios
- [ ] Unit tests for `ExtractionQueueRepository` and `ExtractionQueueService`
- [ ] Python unit tests: `page_segmenter.py` with fixture PDF pages
- [ ] Python unit tests: `structure_parser.py` for section marker variants
- [ ] Python unit tests: `metadata_parser.py` for header extraction patterns
- [ ] Python unit tests: `worker.py` polling and claim logic
- [ ] Integration tests for extraction_queue flow: Kotlin INSERT → Python process → Kotlin read
- [ ] Integration tests for `SwathiThirunalManifestWorker`
- [ ] Integration tests for `NotationParser`
- [ ] Build fixture corpus for each target source and format
- [ ] Add regression tests for known drift cases from TRACK-039
- [ ] Add non-regression tests for existing CSV + HTML pathway (ensure no breakage)
- [ ] Add remediation dry-run verification tests
- [ ] Performance test: Full batch import of mdskt.pdf (484 Krithis) completes in <30 minutes
- [ ] CI pipeline: Python tests (`pytest tools/pdf-extractor/tests/`) added to build
- [ ] CI pipeline: Docker build for pdf-extractor added to build
- [ ] CI pipeline: Kotlin integration tests for new queue services

### Documentation

- [ ] Update `application_documentation/02-architecture/backend-system-design.md` with containerised architecture and extraction queue pattern
- [ ] Update `application_documentation/04-database/schema.md` with new tables (extraction_queue, source_documents, extraction_runs, field_assertions)
- [ ] Update `application_documentation/06-backend/` with new service documentation
- [ ] Update `application_documentation/08-operations/deployment.md` with pdf-extractor Docker/K8s deployment
- [x] Create `application_documentation/01-requirements/krithi-data-sourcing/README.md` linking all documents
- [~] Update `conductor/tracks/TRACK-039` progress log as audits complete
- [~] Update `conductor/tracks/TRACK-040` progress log as remediation runs
- [~] Update `conductor/tracks/TRACK-041` progress log as voting engine ships

### Governance & Change Control

- [ ] Define approval workflow for source authority changes (who can promote/demote a source tier?)
- [ ] Version and publish source authority matrix as a configuration file
- [ ] Version and publish scoring threshold configuration (not hardcoded)
- [ ] Define extraction confidence taxonomy: HIGH (born-digital PDF) / MEDIUM (OCR, blog HTML) / LOW (degraded scans)
- [ ] Define reviewer decision rubric for structural voting conflict cases
- [ ] Add periodic audit cadence: weekly ops review, monthly quality review
- [ ] Add archival strategy for superseded extraction runs
- [ ] Publish decision log for major ingestion rule changes
- [ ] Define artifact storage and evidence lifecycle policy (retention + archival)

---

## Dependency Graph

```text
Phase 0 (Foundation)
  ├── 0.1 TRACK-039 Audits
  ├── 0.2 Canonical Schema ──────────────────────────────┐
  ├── 0.3 DB Migrations (incl. extraction_queue) ────────┤
  └── 0.4 Source Registry ───────────────────────────────┤
                                                          │
Cross-Cutting: Docker & Container Infrastructure ─────────┤
  ├── Dockerfile for pdf-extractor                        │
  ├── compose.yaml extension ◄── 0.3 (extraction_queue)  │
  └── K8s manifests (can be deferred to production)       │
                                                          │
Phase 1 (PDF Ingestion) ◄────────────────────────────────┘
  ├── 1.1 Python Setup (incl. worker.py, db.py) ◄── Docker infra, 0.3
  ├── 1.2 Core PDF Extraction (mdskt.pdf) ◄── 1.1
  ├── 1.3 English PDF (mdeng.pdf) ◄── 1.2
  ├── 1.4 OCR Fallback ◄── 1.2
  ├── 1.5 LLM Refinement ◄── 1.2
  ├── 1.6 Kotlin DB Queue Integration ◄── 1.2, 0.2, 0.3
  │        (ExtractionQueueRepo, ExtractionQueueService,
  │         ExtractionResultProcessor)
  └── 1.7 Source Adapter Interface ◄── 1.6
                                          │
Phase 2 (Structural Voting) ◄────────────┘
  ├── 2.1 Voting Engine ◄── 0.3 (new tables)
  ├── 2.2 ComposerSourcePriority + field-level authority ◄── 0.4
  └── 2.3 Source Evidence Tracking ◄── 0.3, 1.6
                                          │
Phase 3 (Lyric Enrichment) ◄─────────────┘
  ├── 3.1 Swathi Thirunal Ingestion ◄── 1.7 (adapter interface)
  ├── 3.2 Lyric Variant Alignment ◄── 2.1 (canonical structure)
  └── 3.3 Transliteration Service ◄── 1.1 (Python container)
                                          │
Phase 4 (Metadata) ◄─────────────────────┘
  ├── 4.1 Deity Enrichment ◄── Phase 1 output
  ├── 4.2 Temple Enrichment ◄── Phase 1 output
  ├── 4.3 Tag Classification
  └── 4.4 TRACK-040 Remediation ◄── 0.1 (audit baseline), 2.1
                                          │
Phase 5 (Notation) ◄─────────────────────┘
  ├── 5.1 Notation Parser
  ├── 5.2 Shivkumar.org Ingestion ◄── 5.1, 1.7
  └── 5.3 Notation Quality Checks ◄── 5.2
                                          │
Phase 6 (Operations) ◄───────────────────┘
  ├── 6.1 Automated Audits ◄── 0.1
  ├── 6.2 Quality Dashboard ◄── all phases
  ├── 6.3 Re-Harvesting ◄── 1.7
  ├── 6.4 Auto-Approval Enhancement ◄── 2.1
  ├── 6.5 Admin UI ◄── 2.3, 6.2
  └── K8s Production Deployment ◄── Docker infra, all phases
```

---

## Sprint Allocation Summary

| Sprint | Phases | Key Deliverables | Estimated Effort |
|:---|:---|:---|:---|
| S1–S2 | Phase 0, Docker infra, Phase 1.1–1.2 | Quality baseline, canonical schema, DB migrations (incl. extraction_queue), Dockerfile, Docker Compose, PDF PoC (10 Krithis), Python worker polling extraction_queue | 2 weeks |
| S3–S4 | Phase 1.3–1.7 | Full PDF pipeline, Kotlin DB queue integration (ExtractionQueueService + ExtractionResultProcessor), 484 Dikshitar Krithis in staging, three-container stack validated | 2 weeks |
| S5–S6 | Phase 2 | Structural Voting Engine, field-level authority, source evidence tracking, multi-source validation | 2 weeks |
| S7–S8 | Phase 3 | Swathi Thirunal ingestion, English PDF variants, transliteration via Python container | 2 weeks |
| S9–S10 | Phase 4 | Deity/temple enrichment, TRACK-040 remediation, metadata cleanup | 2 weeks |
| S11–S12 | Phase 5, K8s prod | Notation parser, shivkumar.org ingestion, K8s deployment manifests, HPA configuration, production rollout | 2 weeks |
| Ongoing | Phase 6 | Automated audits, quality dashboard, re-harvesting, admin UI, operational monitoring | Continuous |

---

## 90-Day Delivery Milestones

Aligned with the sprint allocation above, these milestones map to the 90-day delivery roadmap:

| Window | Days | Milestone | Key Validation |
|:---|:---|:---|:---|
| **Window 1** | 1–20 | Docker container running, extraction_queue operational, 10 Dikshitar Krithis extracted from mdskt.pdf | Kotlin submits → Python extracts → result in DB |
| **Window 2** | 21–45 | Full Dikshitar batch (484) in staging, structural voting MVP, review UI shows source evidence | Cross-source voting for 50 Krithis with 2+ sources |
| **Window 3** | 46–70 | Swathi Thirunal ingestion, enrichment phases gated, quality scoring upgraded | 400 Swathi Thirunal + multi-script Dikshitar variants |
| **Window 4** | 71–90 | Dikshitar remediation pilot complete, KPIs measured, K8s production deployment, runbook published | Quality score improvement, zero section drift, production-ready |
