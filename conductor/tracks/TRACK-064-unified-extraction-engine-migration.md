| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.8.0 |
| **Last Updated** | 2026-02-13 |
| **Author** | Sangita Grantha Architect |

# TRACK-064: Unified Extraction Engine (UEE) Migration

## 1. Objective
Consolidate all composition extraction logic (HTML and PDF) into the Python service. This eliminates the "Heuristic Split" between Kotlin and Python, centralizes domain expertise (regex, normalization), and simplifies the Kotlin backend into a "Pure Ingestor."

## 2. Context
- **Motivation**: [Technical Retrospective (2026-02-12)](../../application_documentation/11-retrospective/remediation-technical-retrospective-2026-02-12.md) and [Extraction Consolidation Analysis](../../application_documentation/11-retrospective/extraction-consolidation-analysis-2026-02.md).
- **Core Problem**: Heuristics are duplicated in Kotlin and Python, leading to logic divergence and "dirty data" (e.g., merged meanings, missed Madhyama Kala).
- **Strategy**: Move "Intelligence" (parsing/scraping) to Python; keep "Persistence/Domain" in Kotlin.

## 3. Implementation Plan: The Vertical Slice Pattern
Following the retrospective's key lesson: **Build one working slice first, then expand.**

### Phase 0: E2E Harness Foundation (Dev-Topology Mirror)
- [x] **CLI**: Add `sangita-cli test extraction-e2e` to run backend + DB + Python extractor checks without Playwright.
- [x] **Validation**: Automate queue lifecycle checks (`PENDING -> PROCESSING -> DONE -> INGESTED`) and DB assertions (`extraction_queue`, `krithi_source_evidence`).
- [x] **Ops Ergonomics**: Support `--keep-services`, `--skip-migrations`, and `--skip-extraction-start` for rapid local diagnosis.

### Phase 1: The Vertical Slice (HTML Fetch & Text Extraction)
- [x] **Python**: Implement `src/html_extractor.py` using `BeautifulSoup4`.
    - Port selector priority and boilerplate removal from `HtmlTextExtractor.kt`.
- [x] **Python**: Extend `worker.py` to poll for `source_format = 'HTML'` tasks.
- [x] **Python**: Ensure the worker returns a valid `CanonicalExtraction` object for a single Blogspot URL.
- [x] **Kotlin**: Refactor `ImportService.kt` to allow submitting a `PENDING` HTML task to the `extraction_queue` instead of immediate scraping.
- [x] **Kotlin (Critical Gap)**: Ensure `DONE/INGESTED` HTML extraction results are consumed to create/update Krithi records and link import records.
- [x] **Validation (Phase 1 exit gate)**: Prove full HTML path with one automated test:
  `submit URL -> queue task -> worker extraction -> ingest -> krithi_source_evidence -> import record linkage`.

### Phase 2 Entry Criteria (must be complete before parser migration)
- [x] **Critical**: Close the Phase 1 ingest-consumption gap identified in [TRACK-064 Code Review (2026-02-13)](../../application_documentation/11-retrospective/track-064-code-review-2026-02-13.md).
- [x] **Critical**: Fix key-collision metadata SQL to read raga from the canonical array shape (`raw_extraction::jsonb->'ragas'->0->>'name'`), then rerun `--max-rows 200` and confirm `metadataMissingRows` drops.
  - Validation snapshot (2026-02-13): `total_rows=200`, `keyed_rows=200`, `metadata_missing_rows=0`.
- [x] **High**: Add one end-to-end integration test for HTML import ingestion.
- [x] **High**: Add migration for composite lookup index `krithi_source_evidence (krithi_id, source_url)`.
  - Implemented as `database/migrations/35__add_krithi_source_evidence_krithi_source_url_index.sql`.
- [x] **High**: Replace scaffold-only Python tests with real assertions in `test_html_extractor.py`, `test_metadata_parser.py`, and `test_worker.py`.
  - Validation snapshot (2026-02-13): `uv run python -m pytest tests/test_html_extractor.py tests/test_metadata_parser.py tests/test_worker.py` -> `11 passed`.

### Phase 2: Heuristic Consolidation
- [x] **Step 2.1 (Design Freeze)**: Define parser contract in `structure_parser.py` to output sectioned lyrics + metadata boundaries in `CanonicalExtraction`.
  - Implemented via `StructureParseResult` (`sections` + `metadata_boundaries`) and canonical payload field `metadataBoundaries`.
  - Validation snapshot (2026-02-13): `uv run python -m pytest tests/test_structure_parser.py tests/test_schema.py tests/test_worker.py` -> `21 passed`.
- [x] **Step 2.2 (Regex Port)**: Port 100+ Indic script regex rules from `KrithiStructureParser.kt` into Python with fixture-backed parity checks.
  - Implemented Kotlin-parity header detection (Latin + Indic abbreviations and full-script section headers) plus ragamalika sub-section splitting in `structure_parser.py`.
  - Fixture-backed parity checks added:
    - `tests/fixtures/structure_parser/kotlin_parity_multiscript.txt`
    - `tests/fixtures/structure_parser/kotlin_parity_tamil_headers.txt`
- [x] **Step 2.3 (Metadata Isolation)**: Implement deterministic hard-stops for `MEANING`, `NOTES`, and known synonyms/variants before lyric segmentation.
  - Parser now truncates lyric parsing at first metadata boundary and emits structured `metadataBoundaries` offsets for downstream validation.
- [x] **Step 2.4 (Variant Splitting)**: Split multi-script HTML blocks at node level into independent lyric variants with language/script tags.
  - Added deterministic language-header variant extraction and script-run fallback splitting for mixed-script blocks (`latin`, `devanagari`, `tamil`, `telugu`, `kannada`, `malayalam`).
- [x] **Step 2.5 (Worker Integration)**: Route both HTML and PDF flows through the new Python structure parser and emit normalized extraction payloads only.
  - `worker.py` and `cli.py` now consume parser-emitted `lyric_variants` + `metadata_boundaries` directly for HTML/PDF/PDF-OCR flows.
- [x] **Step 2.6 (Regression Harness)**: Extend CLI `extraction-e2e` scenarios with assertions for:
  - section counts,
  - variant count per source,
  - absence of meaning text in lyric payloads.
  - Implemented in `sangita-cli test extraction-e2e` validation gate (`validate_extraction_outcome`) as hard failures on payload regressions.

#### Phase 2 Exit Criteria
- [x] `KrithiStructureParser.kt` is no longer required for runtime extraction behavior.
  - Runtime extraction path (`extraction_queue` -> Python worker -> ingestion) now uses Python parser contract exclusively.
- [x] Multi-script variant splitting is deterministic for both Blogspot HTML and PDF payloads.
- [x] Meaning/notes text never appears in persisted lyric variants for validated fixtures.
- [x] Regression suite includes at least one HTML and one PDF source proving parity.

### Phase 3: Identity & Enrichment
- [x] **Python**: Integrate `google-generativeai` SDK for Gemini metadata enrichment (replacing `GeminiApiClient.kt`).
- [x] **Python**: Implement "Identity Candidate Discovery" (proposing Raga/Composer matches via RapidFuzz).

### Phase 4: Orchestration & Cleanup
- [ ] **Kotlin**: Update `BulkImportRepository.kt` to atomically increment `total_tasks` for all job transitions (Fixing the 50% stall).
- [ ] **Kotlin**: Decommission `KrithiStructureParser.kt`, `DeterministicWebScraper.kt`, and `GeminiApiClient.kt`.
- [ ] **DevOps**: Update `compose.yaml` to mount `tools/pdf-extractor/src` as a volume for instant logic updates.

## 4. Acceptance Criteria
- [ ] All extraction (PDF & HTML) is performed by the Python service.
- [ ] Kotlin backend contains ZERO heuristic regexes for composition structure.
- [ ] Batch progress bars accurately reflect all stages (Scrape -> Resolution -> Ingest).
- [ ] Multi-language variants are correctly split into separate database records.
- [ ] Meanings and Notes are isolated from lyric sections.
- [ ] Collision-scan metadata extraction is schema-aligned with canonical payloads (`ragas` array), with no unresolved accessor mismatch.

## 5. Progress Log
- **2026-02-12**: Track created. Strategy aligned with "Vertical Slice" pattern.
- **2026-02-12**: Implemented Phase 0 E2E harness via `sangita-cli test extraction-e2e` to validate backend+worker queue integration before HTML migration slice.
- **2026-02-12**: Added `HTML` queue format support (migration 31), implemented Python `html_extractor.py`, wired worker HTML path (`HTML_JSOUP`), and added `test extraction-e2e --scenario blogspot-html` using `database/for_import/Dikshitar-Krithi-Test-20.csv`.
- **2026-02-13**: Incorporated independent review findings into plan: Phase 1 re-baselined with ingest-consumption gap, entry criteria added before Phase 2, and heuristic consolidation expanded into a gated implementation sequence.
- **2026-02-13**: Implemented collision SQL schema alignment in CLI (`ragas[0].name` accessor with fallback), added a metadata coverage regression assertion (`metadataMissingRows` must be `0`), and added backend integration coverage for URL submit -> extraction ingestion -> source evidence -> import linkage (`ExtractionResultProcessorTest`).
- **2026-02-13**: Closed the metadata coverage gate for first 200 Dikshitar rows via direct DB validation query with corrected accessor logic (`metadata_missing_rows=0`).
- **2026-02-13**: Python extractor test suite is now active and validated (`test_html_extractor.py`, `test_metadata_parser.py`, `test_worker.py`: `11 passed`).
- **2026-02-13**: Added migration `35__add_krithi_source_evidence_krithi_source_url_index.sql` for `(krithi_id, source_url)` lookup performance and validated SQL execution shape (`BEGIN`/`ROLLBACK` parse check).
- **2026-02-13**: Started Phase 2 implementation by freezing the parser contract: Python structure parser now emits `sections` plus deterministic `metadata_boundaries`, worker/CLI propagate these as `metadataBoundaries` in `CanonicalExtraction`, and regression tests confirm meaning/notes boundaries do not leak into lyric sections.
- **2026-02-13**: Completed Phase 2 heuristic consolidation: ported Kotlin section/header regex coverage into Python with fixture-backed parity tests, added deterministic metadata hard-stop boundaries, enabled deterministic multi-script variant extraction, routed HTML/PDF/PDF-OCR worker outputs through parser-emitted variants, and upgraded CLI extraction-e2e validation to fail on section/variant/metadata-leak regressions.
- **2026-02-13**: Started/closed Phase 3 kickoff slice in Python extraction worker:
  - Added optional Gemini metadata enrichment service (`google-generativeai`) with fail-open behavior and extraction-method tagging (`HTML_JSOUP_GEMINI`) when metadata updates apply.
  - Added deterministic RapidFuzz identity candidate discovery for composer/raga with DB-backed reference catalog caching and canonical payload emission (`identityCandidates`).
  - Extended canonical extraction contracts (`CanonicalExtraction` Python schema + shared Kotlin DTO + canonical JSON schema) with `identityCandidates` and `metadataEnrichment`.
  - Hardened E2E payload validation in `sangita-cli test extraction-e2e` to validate Phase 3 payload shape when signals are present.
  - Added test coverage: `test_identity_candidates.py`, `test_gemini_enricher.py`, worker/schema assertions for Phase 3 fields.
- **2026-02-13**: Validation snapshot after Phase 2 closure:
  - `uv run python -m pytest` -> `97 passed`
  - `cargo check --manifest-path tools/sangita-cli/Cargo.toml` -> passed
  - `./gradlew :modules:backend:api:test --tests \"com.sangita.grantha.backend.api.services.ExtractionResultProcessorTest\" --tests \"com.sangita.grantha.backend.api.services.ImportServiceTest\"` -> passed
  - `cargo run --manifest-path tools/sangita-cli/Cargo.toml -- test extraction-e2e --scenario blogspot-html --skip-migrations --timeout-seconds 240 --poll-interval-seconds 5` -> passed (`minSections=11`, `minVariants=1`)
  - `cargo run --manifest-path tools/sangita-cli/Cargo.toml -- test extraction-e2e --scenario pdf-smoke --skip-migrations --timeout-seconds 300 --poll-interval-seconds 5` -> passed (`minSections=3`, `minVariants=1`)
  - `cargo run --manifest-path tools/sangita-cli/Cargo.toml -- test extraction-e2e --scenario pdf-smoke --skip-migrations --timeout-seconds 300 --poll-interval-seconds 5` -> passed (`minSections=3`, `minVariants=1`)
- **2026-02-17**: Enhanced Phase 3 (Enrichment Hardening):
  - Replaced Regex-based prompt logic with **Schema-Driven Extraction** (`response_schema`) for structured, robust metadata (including `ragaMudra`).
  - Implemented **Exponential Backoff Retry Logic** for `429 ResourceExhausted` errors in `gemini_enricher.py`.
  - Refined prompt engineering with a "Musicologist" persona for better inference of implicit metadata (e.g., Temple from Deity/Title).
  - Validated via `tests/test_schema_enrichment.py` and E2E `blogspot-html` scenario.
