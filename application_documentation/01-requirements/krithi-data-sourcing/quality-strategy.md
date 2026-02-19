| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.1 |
| **Last Updated** | 2026-02-19 |
| **Author** | Sangeetha Grantha Team |
| **Related Tracks** | TRACK-039, TRACK-040, TRACK-041 |
| **Scope** | Multi-format sourcing, quality framework, enrichment pipeline |
| **Consolidates** | [Original combined strategy](../../archive/krithi-data-sourcing/krithi-data-sourcing-strategy-and-implementation-checklist.md) (archived) |

# Krithi Data Sourcing & Quality Strategy

## 1. Executive Summary

Sangeetha Grantha aspires to be the definitive digital system of record for Carnatic music compositions (Krithis). Today the application successfully ingests data from HTML web sources through a three-phase pipeline of manifest parsing, web scraping with LLM-assisted extraction, and entity resolution. However, the richest and most authoritative Krithi data resides in curated PDF collections and specialised archives that the current pipeline cannot reach.

This document presents a comprehensive, multi-phase strategy to:

1. Expand data sourcing beyond HTML into PDF documents and structured web archives.
2. Harden data quality through structural voting, deduplication, and editorial governance.
3. Implement layered enrichment where initial phases establish the canonical Krithi skeleton and subsequent phases add lyric variants, notation, deity/temple metadata, and cross-source validation.

The approach builds on the foundations laid by three active tracks:

- **TRACK-039** — Structural consistency auditing
- **TRACK-040** — Remediation and deduplication
- **TRACK-041** — Enhanced sourcing with structural voting

> **Strategic Objective**: Transform Sangeetha Grantha from a single-source HTML scraper into a multi-format, multi-source harmonisation platform that produces and maintains Krithi data of the highest musicological quality.

---

## 2. Current State Analysis

### 2.1 Existing Pipeline Architecture

The current bulk import system operates as a three-phase worker pipeline orchestrated by the `BulkImportOrchestrationService`:

**Phase 1 — Manifest Ingest** (`ManifestWorker`): Parses CSV manifest files containing Krithi name, Raga, and a hyperlink to a blogspot or web page. Currently supports three manifests for the Trinity composers (~690 Thyagaraja, ~480 Dikshitar, ~70 Syama Sastri). Creates `SCRAPE` tasks for each entry.

**Phase 2 — Web Scraping** (`ScrapeWorker`): Fetches HTML from the hyperlink using Ktor Client (CIO engine, 60s timeout), extracts text via Jsoup (max 120k chars), applies `KrithiStructureParser` for section detection, then sends extracted text to Google Gemini for structured JSON extraction (title, composer, ragas, tala, deity, temple, sections, lyric variants). Results are cached with 24h TTL and in-flight deduplication.

**Phase 3 — Resolution** (`ResolutionWorker`): Performs entity resolution (mapping raw names to canonical Composer, Raga, Tala, Deity, Temple records via `EntityResolutionServiceImpl`), deduplication detection (normalised title + Levenshtein distance), and multi-dimensional quality scoring. Results land in the `imported_krithis` staging table for human review.

### 2.2 Strengths

- Robust staging-to-publication workflow with `DRAFT → IN_REVIEW → PUBLISHED → ARCHIVED` lifecycle.
- Quality scoring across five dimensions: completeness, resolution confidence, source quality, validation score, and quality tier.
- Entity resolution cache with normalised name matching and fuzzy fallback (15-min TTL).
- Full audit trail with field-level mutation tracking via `AuditLogService`.
- Musical form awareness: different validation rules for `KRITHI`, `VARNAM`, and `SWARAJATHI`.
- Separation of lyrics and notation as independent first-class entities.
- Multilingual/multiscript support (7 languages, 6 scripts) with primary variant designation.
- Idempotency keys, retry logic, and batch pause/resume capabilities.

### 2.3 Identified Gaps

| Gap Area | Description | Impact |
|:---|:---|:---|
| **Format Lock-in** | Pipeline only handles HTML. No capability to ingest PDFs, which contain the most authoritative Dikshitar and Swathi Thirunal collections. | Cannot access Tier 1 sources |
| **Single-Source Trust** | Each Krithi is sourced from a single URL. No cross-referencing or structural voting across multiple sources. | No validation of section structure accuracy |
| **Script Coverage** | Most imports arrive in a single script. Systematic sourcing of variants in Devanagari, Tamil, Telugu, Kannada, Malayalam is not automated. | Incomplete multilingual coverage |
| **Metadata Enrichment** | Deity and Temple associations are opportunistic (extracted only if present in HTML). No dedicated enrichment pipeline. | Sparse Kshetra metadata |
| **Section Drift** | TRACK-039 identified that language variants sometimes have mismatching section counts, violating the musicological invariant that structure is script-independent. | Structural inconsistency in DB |
| **Notation Gap** | Notation tables (`krithi_notation_variants`, `krithi_notation_rows`) exist but are unused. shivkumar.org has ~300 compositions with notation but no ingestion path exists. | Notation feature unusable |

---

## 3. Source Landscape Analysis

### 3.1 Source Registry

| Source | Format | Composers | Scripts | Key Metadata | Authority Level |
|:---|:---|:---|:---|:---|:---|
| **guruguha.org** (PDFs) | PDF | Dikshitar (~484) | Sanskrit (Devanagari), English (Latin) | Raga, Tala, Deity, Kshetra, Mudra | Very High — Dr. P.P. Narayanaswami scholarly compilation |
| **swathithirunalfestival.org** | HTML | Swathi Thirunal (~400) | Sanskrit, Malayalam | Raga (with Arohanam/Avarohanam), Tala, Meaning | High — Official Swathi Thirunal Sangeetha Sabha |
| **shivkumar.org** | HTML, PDF, DOCX | Multi-composer (~300) | English transliteration | Swara Notation, Audio, Raga, Tala | High — Curated by trained violinist |
| **karnatik.com** | HTML | All major composers | English | Raga, Tala, Composer index, Lyrics | Medium — Community curated, broad coverage |
| **Blogspot sources** (current) | HTML | Trinity + others | Mixed | Variable | Low–Medium — Individual blogs, variable quality |

### 3.2 Source Authority Hierarchy

The system must implement a trust hierarchy that prioritises authoritative, editorially reviewed sources over community or individual contributions. This hierarchy directly informs the Structural Voting Engine (TRACK-041) and quality scoring:

**Tier 1 — Scholarly/Published Editions**: guruguha.org PDFs (Dr. P.P. Narayanaswami's compilation of 484 Dikshitar Krithis), printed Sangita Sampradaya Pradarshini. These are editorially curated with high fidelity to original manuscripts. Tier 1 sources receive authority override in structural voting — they prevail regardless of vote count.

**Tier 2 — Official Festival/Institutional Archives**: swathithirunalfestival.org (official Swathi Thirunal Sangeetha Sabha, Charlotte NC), government cultural archives. Institutional backing provides credibility.

**Tier 3 — Practitioner-Curated Archives**: shivkumar.org (curated by a trained violinist with ~300 compositions including notation). High individual effort but single editorial voice.

**Tier 4 — Community Databases**: karnatik.com (broad coverage, community maintained). Good for cross-referencing but may contain errors.

**Tier 5 — Individual Blogs**: Blogspot sources (current primary source). Highly variable quality, often incomplete section structures, metadata pollution in lyric text.

### 3.3 Field-Level Authority

Authority should not be a single global rank per source. Different sources are authoritative for different **field types**. The `ComposerSourcePriority` map (TRACK-041) should encode field-level weights:

| Field Category | Dikshitar Authority | Swathi Thirunal Authority | Thyagaraja Authority |
|:---|:---|:---|:---|
| **Section Structure** | guruguha.org (Tier 1) | swathithirunalfestival.org (Tier 2) | Blogspot/karnatik (Tier 4–5) |
| **Sanskrit Lyrics** | guruguha.org mdskt.pdf | swathithirunalfestival.org | Blogspot sources |
| **English Transliteration** | guruguha.org mdeng.pdf | — | shivkumar.org |
| **Deity / Kshetra** | guruguha.org (explicit in PDF) | LLM inference from lyrics | LLM inference from lyrics |
| **Notation (Swara)** | — | — | shivkumar.org (Tier 3) |
| **Raga / Tala** | guruguha.org | swathithirunalfestival.org | karnatik.com (cross-reference) |

This field-aware authority model ensures that when structural voting encounters a conflict, the system knows which source to trust for which field — rather than applying a blanket tier ranking.

### 3.4 Source-Format Matrix

Understanding what each source provides helps plan the extraction adapters:

| Source | Krithi Title | Composer | Raga | Tala | Sections | Lyrics (SA) | Lyrics (EN) | Lyrics (Other) | Deity | Temple | Notation |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| guruguha.org (mdskt.pdf) | Yes | Yes | Yes | Yes | Yes | Yes | — | — | Yes | Yes | — |
| guruguha.org (mdeng.pdf) | Yes | Yes | Yes | Yes | Yes | — | Yes | — | Yes | Yes | — |
| swathithirunalfestival.org | Yes | Yes | Yes | Yes | Partial | Yes | — | ML | Partial | — | — |
| shivkumar.org | Yes | Yes | Yes | Yes | Yes | — | Yes | — | — | — | Yes |
| karnatik.com | Yes | Yes | Yes | Yes | Partial | — | Yes | — | — | — | — |
| Blogspot (current) | Yes | Yes | Yes | Yes | Variable | Variable | Variable | Variable | Sometimes | Sometimes | — |

---

## 4. Multi-Format Ingestion Architecture

### 4.1 Design Principles

1. **Authority First**: Prefer curated, composer-specific authoritative sources. Source authority is resolved by **field type** (structure vs. metadata vs. meaning), not a single global source rank. A source may be authoritative for Dikshitar section structures but not for Thyagaraja deity metadata.

2. **Structure Before Enrichment**: Lock canonical section structure first, enrich metadata later. The section skeleton is the invariant foundation upon which all other data layers depend. No enrichment proceeds until structural confidence is established.

3. **Format-Agnostic Pipeline**: The ingestion pipeline treats content extraction as a pluggable adapter layer. Whether the source is HTML, PDF, DOCX, or a structured API, the output fed into entity resolution and quality scoring is identical — the Canonical Extraction Format (JSON).

4. **Provenance by Default**: Every asserted field is traceable to source artifact and extractor run. Every piece of data carries provenance metadata linking it to the specific source, page/URL, extraction method, version, and timestamp. This enables quality auditing, structural voting, and dispute resolution.

5. **Confidence-Gated Progression**: Do not auto-approve low-confidence structural imports. Each enrichment phase has explicit entry criteria and quality gates. Phase transitions require minimum confidence thresholds.

6. **Reproducible Pipeline**: Version all extraction and normalisation logic for replayability. Extractor versions are tagged and persisted with each extraction run. Source artifacts are checksummed and cached. Any extraction can be replayed deterministically.

7. **Containerised Separation of Concerns**: The Python extraction service runs as an independent Docker container alongside the Kotlin backend and PostgreSQL, communicating via a shared database table with structured JSON. This keeps the extraction ecosystem (PyMuPDF, Tesseract, indic-transliteration) isolated from the JVM runtime while enabling the same orchestration, scaling, and deployment patterns used for PostgreSQL.

8. **Fix-Forward Operations**: Remediation is auditable, reversible, and measurable. Before/after diffs are captured for every remediation action. Rollback is possible via the audit trail. Quality metrics are re-computed after each remediation batch to measure impact.

9. **Non-Destructive Enrichment**: New data never overwrites existing verified data. It is staged, scored, compared, and promoted through the review workflow.

### 4.2 High-Level Architecture

The architecture follows the same containerised deployment pattern as PostgreSQL: the Python extraction service runs as a Docker container in the same `compose.yaml` / Kubernetes namespace, communicating with the Kotlin backend through the shared PostgreSQL database via a well-structured `extraction_queue` table.

```text
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  PDF Sources  │  │ HTML Sources  │  │  API Sources  │  │ Manual Entry  │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │                 │
       └─────────────────┴─────────────────┴─────────────────┘
                                 │
     ┌───────────────────────────┼───────────────────────────┐
     │   Docker / Kubernetes     │                           │
     │                           │                           │
     │  ┌────────────────────────▼──────────────────────┐    │
     │  │         Kotlin Backend (Cloud Run / K8s)       │    │
     │  │  ┌──────────────────────────────────────────┐  │    │
     │  │  │  BulkImportOrchestrationService          │  │    │
     │  │  │    ├── ManifestWorker (CSV/JSON/PDF URL)  │  │    │
     │  │  │    ├── HtmlScrapeWorker (Jsoup + Gemini)  │  │    │
     │  │  │    └── writes to extraction_queue ────────┼──┼────┤
     │  │  │  ResolutionWorker ◄── reads results ──────┼──┼────┤
     │  │  │  StructuralVotingEngine                   │  │    │
     │  │  │  QualityScoringService                    │  │    │
     │  │  └──────────────────────────────────────────┘  │    │
     │  └────────────────────────────────────────────────┘    │
     │                                                        │
     │  ┌────────────────────────────────────────────────┐    │
     │  │     PostgreSQL 15 (Docker / Cloud SQL)          │    │
     │  │  ┌──────────────────────────────────────────┐   │    │
     │  │  │  extraction_queue (integration table)     │   │    │
     │  │  │    status: PENDING → PROCESSING → DONE    │   │    │
     │  │  │    request_payload: Canonical JSON         │   │    │
     │  │  │    result_payload: Extraction JSON         │   │    │
     │  │  │  + existing schema (krithis, imported_     │   │    │
     │  │  │    krithis, import_batch, etc.)            │   │    │
     │  │  └──────────────────────────────────────────┘   │    │
     │  └──────────────────────┬─────────────────────────┘    │
     │                         │                              │
     │  ┌──────────────────────▼─────────────────────────┐    │
     │  │   Python Extraction Service (Docker container)  │    │
     │  │  ┌──────────────────────────────────────────┐   │    │
     │  │  │  Polls extraction_queue (PENDING tasks)   │   │    │
     │  │  │  ├── PdfExtractor (PyMuPDF / fitz)        │   │    │
     │  │  │  ├── OcrExtractor (Tesseract + Indic)     │   │    │
     │  │  │  ├── DocxExtractor (python-docx)          │   │    │
     │  │  │  ├── Transliterator (indic-transliteration)│   │    │
     │  │  │  ├── LLM Refinement (Gemini API)          │   │    │
     │  │  │  └── Writes result_payload back to table   │   │    │
     │  │  └──────────────────────────────────────────┘   │    │
     │  └────────────────────────────────────────────────┘    │
     └────────────────────────────────────────────────────────┘
```

### 4.2.1 Multi-Layer Pipeline

Aligned with the five-layer pipeline model:

1. **Acquisition Layer**: Accepts URL, PDF URL, local document upload, and mixed manifests. Produces source artifacts with checksum and MIME metadata. The Kotlin backend writes acquisition records to `source_documents`.

2. **Format Extraction Layer**: The Python container handles PDF text extraction, OCR for scanned documents, and DOCX extraction. The Kotlin backend continues to handle HTML extraction (Jsoup + Gemini). Both write to the same canonical extraction format.

3. **Structure Canonicalisation Layer**: Runs structural voting across multiple extractions/sources. Produces canonical section template and confidence. Persists all candidates and winning rationale.

4. **Enrichment Layer**: Adds language variants, deity, temple, notes, and secondary metadata with explicit provenance per field.

5. **Governance Layer**: Applies quality gates, dedup checks, review routing, and audit tracking. Confidence-gated progression ensures no low-quality data is auto-published.

### 4.3 Canonical Extraction Schema

All source adapters must produce output conforming to this schema. This is the contract between the extraction layer and the resolution pipeline:

```json
{
  "title": "string (required)",
  "alternateTitle": "string (optional — for transliterated form)",
  "composer": "string (required — raw name, resolved downstream)",
  "musicalForm": "KRITHI | VARNAM | SWARAJATHI (required)",
  "ragas": [
    {
      "name": "string (required)",
      "order": "integer (1 for primary, 2+ for ragamalika)",
      "section": "string (optional — which section this raga covers)"
    }
  ],
  "tala": "string (required)",
  "sections": [
    {
      "type": "PALLAVI | ANUPALLAVI | CHARANAM | SAMASHTI_CHARANAM | CHITTASWARAM | ...",
      "order": "integer",
      "label": "string (optional — e.g. 'Charanam 1')"
    }
  ],
  "lyricVariants": [
    {
      "language": "sa | ta | te | kn | ml | hi | en",
      "script": "devanagari | tamil | telugu | kannada | malayalam | latin",
      "sections": [
        {
          "sectionOrder": "integer (matches sections[].order)",
          "text": "string"
        }
      ]
    }
  ],
  "deity": "string (optional)",
  "temple": "string (optional)",
  "templeLocation": "string (optional)",
  "sourceUrl": "string (required — URL or file reference)",
  "sourceName": "string (required — e.g. 'guruguha.org')",
  "sourceTier": "integer 1-5 (required)",
  "extractionMethod": "PDF_PYMUPDF | PDF_OCR | HTML_JSOUP | HTML_JSOUP_GEMINI | DOCX_PYTHON | MANUAL",
  "extractionTimestamp": "ISO-8601 datetime",
  "pageRange": "string (optional — for PDFs, e.g. '42-43')",
  "checksum": "string (optional — SHA-256 of source content)"
}
```

### 4.4 PDF Ingestion Pipeline

PDF ingestion is the highest-priority new capability. The guruguha.org PDFs represent the single most authoritative source for Dikshitar compositions.

#### 4.4.1 Challenges Specific to Carnatic Music PDFs

- **Mixed scripts**: Devanagari and Latin (with diacriticals like ṣ, ṇ, ā) on the same page.
- **Domain-specific structural conventions**: Pallavi/Anupallavi/Charanam labels, Raga/Tala headers, Samashti Charanam format (common in Dikshitar).
- **Anthology format**: Multiple Krithis per PDF requiring page segmentation and boundary detection.
- **Footnotes and annotations**: Scholarly PDFs include commentary that should not be confused with lyric text.
- **Unicode complexity**: Combining characters in Devanagari, Tamil subscript numerals, IAST romanisation.

#### 4.4.2 Technical Approach

The recommended approach is a standalone Python extraction service:

| Component | Choice | Rationale |
|:---|:---|:---|
| **Primary Extraction** | PyMuPDF (fitz) | Best text extraction with positional data, Unicode support, Indic script handling. Reading order preservation. |
| **Tabular Fallback** | pdfplumber | For PDFs with structured tables (composition indexes, raga lists). |
| **OCR Fallback** | Tesseract + pytesseract (with Indic packs) | For scanned PDFs or image-embedded text. Invoked only when text extraction yields garbled output. |
| **Script Processing** | indic-transliteration (Python) | Automated conversion between scripts (Devanagari → Tamil, Telugu, etc.). |
| **LLM Post-Processing** | Gemini API (existing integration) | Validates and structures pattern-matched extraction. Same prompt engineering as HTML pipeline. |

#### 4.4.3 PDF Processing Stages

**Stage 1 — Document Classification**: Determine PDF type (anthology vs. single composition, index vs. content, Sanskrit vs. English edition). The guruguha.org collection has two PDFs: `mdskt.pdf` (Sanskrit/Devanagari) and `mdeng.pdf` (English transliteration). Classification guides downstream parsing strategy.

**Stage 2 — Page Segmentation**: Identify page boundaries for individual Krithis. In anthology PDFs, each Krithi typically starts with the title in bold/larger font, followed by Raga/Tala metadata, then the lyric body. Use font-size detection and positional analysis from PyMuPDF to detect boundaries.

**Stage 3 — Metadata Header Extraction**: Parse the Krithi header to extract title, raga, tala, deity, and temple references. The guruguha.org PDFs follow a consistent format that can be pattern-matched before LLM refinement.

**Stage 4 — Section Structure Detection**: Identify Pallavi, Anupallavi, Charanam labels and their associated text blocks. Handle Samashti Charanam (Dikshitar's preferred form). Detect Chittaswaram sections (which contain notation, not lyrics).

**Stage 5 — LLM Refinement**: Pass the extracted structure to Gemini for validation, correction, and enrichment. The LLM acts as a quality check against the pattern-matched extraction, catching edge cases the regex missed.

**Stage 6 — Output**: Emit canonical extraction JSON for each Krithi, with `extractionMethod: "PDF_PYMUPDF"` (or `"PDF_OCR"` if OCR was needed) and `pageRange` populated.

#### 4.4.4 Implementation as Python Service

```text
tools/
  krithi-extract-enrich-worker/
    pyproject.toml
    src/
      cli.py                    # CLI entry point
      extractor.py              # PyMuPDF text extraction
      page_segmenter.py         # Krithi boundary detection
      structure_parser.py       # Section label detection
      metadata_parser.py        # Header field extraction
      ocr_fallback.py           # Tesseract integration
      transliterator.py         # indic-transliteration wrapper
      schema.py                 # Canonical JSON schema validation
    tests/
      test_guruguha_mdskt.py    # Validation against known Dikshitar Krithis
      test_guruguha_mdeng.py
    Dockerfile                  # Container for deployment
```

**Interface**: The service accepts a PDF URL or file path and outputs a JSON array of canonical extraction records (one per Krithi detected in the PDF). The Kotlin `PdfScrapeWorker` invokes this as a subprocess or HTTP call.

### 4.5 Enhanced HTML Ingestion

#### 4.5.1 Swathi Thirunal Festival Site

The swathithirunalfestival.org site hosts individual composition pages at URLs like `/compositions/{slug}`. Each page contains Raga (with Arohanam/Avarohanam), lyrics in Sanskrit/Malayalam, and meaning. A dedicated Jsoup parser can extract from the consistent page template. The site's composition index at `/swathi-thirunal/compositions` provides the manifest.

**Approach**: Build a `SwathiThirunalManifestWorker` that crawls the composition index, extracts slug URLs, and creates `SCRAPE` tasks. The existing `ScrapeWorker` with a site-specific Jsoup parser handles extraction.

#### 4.5.2 Karnatik.com

Karnatik.com serves as a broad cross-referencing source. The composers page at `/composers.shtml` links to per-composer pages, which link to individual composition pages with lyrics and metadata. While individual Krithis may not have the depth of guruguha.org, the site's coverage makes it valuable for:

- Entity resolution validation (does our resolved Raga/Tala match karnatik's?)
- Deduplication matching (confirming a Krithi exists in the canon)
- Metadata gap-filling (Raga/Tala for Krithis where our primary source lacked it)

**Approach**: Lightweight scraper targeting the lyrics section. Results are tagged as Tier 4 and used primarily for cross-validation rather than as a primary source.

#### 4.5.3 Shivkumar.org

Uniquely valuable because it provides **notation** alongside lyrics. The site hosts ~300 compositions with Swara notation in HTML, Word, and PDF formats. A multi-format adapter is needed:

- **HTML notation pages**: Parse directly via Jsoup.
- **PDF notation documents**: Route through the PDF extraction service.
- **Word (.doc/.docx) files**: Extract via python-docx or pandoc.

The notation data maps to `krithi_notation_variants` and `krithi_notation_rows` tables. Each notation row contains `swara_text`, optional `sahitya_text`, `tala_markers`, and section alignment.

---

## 5. Multi-Phase Enrichment Strategy

### 5.1 Phase Overview

| Phase | Name | Objective | Input | Output | Quality Gate |
|:---|:---|:---|:---|:---|:---|
| 1 | Skeleton Ingestion | Establish canonical Krithi identity: title, composer, primary raga, tala, section structure | PDF + HTML sources | Krithi skeleton in staging | Completeness score > 60% |
| 2 | Structural Validation | Cross-reference structures across sources; apply structural voting | Multi-source extractions | Verified canonical structure | 100% section alignment |
| 3 | Lyric Enrichment | Source lyric variants in all available scripts; align to canonical sections | Script-specific sources + transliteration | Multi-script lyric variants | Section count parity |
| 4 | Metadata Enrichment | Source deity, temple/Kshetra, tags, Sampradaya | Source metadata + LLM inference | Complete metadata profile | >80% deity coverage (Dikshitar) |
| 5 | Notation Ingestion | Ingest Swara notation from practitioner sources | shivkumar.org + others | Notation variants linked to Krithis | Tala alignment verified |
| 6 | Continuous Curation | Ongoing monitoring, community corrections, re-harvesting | Quality dashboards + feedback | Sustained data integrity | Quality score trend stable |

### 5.2 Phase 1 — Skeleton Ingestion (Detailed)

The first phase establishes the identity of each Krithi with the minimum viable attributes needed for deduplication and cross-referencing.

**Key Insight**: A Krithi's identity is defined by its **title + composer + primary raga + tala** combination. Its structural skeleton (which sections exist and in what order) is **invariant across languages and scripts** — this is the core musicological principle validated in TRACK-039.

**Process**:

1. Ingest from the highest-authority source first (guruguha.org PDFs for Dikshitar, blogspot manifests for Thyagaraja/Syama Sastri).
2. For each extracted Krithi, create a staging record with identity fields and section structure.
3. Run entity resolution to map raw composer/raga/tala names to canonical records.
4. Run deduplication against existing `krithis` table and other staging records.
5. Compute initial quality score (completeness + resolution confidence).
6. Queue for human review (or auto-approve if quality score exceeds threshold and source is Tier 1–2).

**Acceptance Criteria**: A Krithi skeleton is considered valid when it has a resolved composer, at least one resolved raga, a resolved tala, and at least one section (Pallavi at minimum).

### 5.3 Phase 2 — Structural Validation (TRACK-041 Integration)

Building on TRACK-041's proposed `StructuralVotingEngine`, Phase 2 implements cross-source structural validation:

**Voting Rules**:

- **Unanimous Agreement**: All sources agree on section structure (e.g., P-A-C-C-C). Structure accepted with HIGH confidence.
- **Majority Agreement**: Most sources agree, one dissents. Majority structure accepted with MEDIUM confidence. Dissenting source flagged for review.
- **No Consensus**: Sources disagree significantly. Flagged for expert editorial review. Tier 1 source's structure used as tentative canonical form.
- **Authority Override**: A Tier 1 source always takes precedence over Tier 4–5 sources in case of disagreement, regardless of vote count.

**Data Model**: The `krithi_source_evidence` table (new) links each Krithi to all sources that contributed data, with per-source extraction metadata and confidence scores. The `structural_vote_log` table (new) records voting outcomes for audit transparency.

### 5.4 Phase 3 — Lyric Enrichment

Once the canonical section structure is established, Phase 3 systematically sources lyric variants in multiple scripts.

**Key Principle (from TRACK-039)**: All lyric variants must map one-to-one with the canonical section structure. A Tamil lyric variant must have exactly the same sections as the canonical Devanagari variant.

**Enrichment Process**:

1. For each Krithi with a verified canonical structure, identify which script variants are already present.
2. Source missing variants from script-specific archives (e.g., guruguha.org mdskt.pdf for Sanskrit, mdeng.pdf for English).
3. Where direct sources are unavailable, use the `indic-transliteration` Python library for automated transliteration.
4. Apply LLM-assisted quality checks to verify that transliterated text preserves sahitya syllable boundaries (critical for alignment with swara patterns).
5. Each new variant enters staging with `is_primary: false` until reviewed.

### 5.5 Phase 4 — Metadata Enrichment

Deity and Temple metadata is particularly important for **Dikshitar compositions**, which are organised by Kshetra (temple). The guruguha.org PDF includes deity and temple references for most compositions.

**Enrichment Sources**:

- **PDF extraction** (Phase 1 output): Deity/temple fields from guruguha.org.
- **LLM inference**: Analyse lyric text to identify deity references (e.g., "Padmanabha" → Lord Vishnu for Swathi Thirunal; "Rama" → Lord Rama for Thyagaraja).
- **Temple database cross-reference**: Match extracted temple names against the `temples` table (multilingual names, geo-coordinates via `temple_source_cache`).
- **Tag classification**: Apply controlled taxonomy tags (Bhava, Festival, Philosophy, Kshetra, Style) based on lyric content analysis.

### 5.6 Phase 5 — Notation Ingestion

Notation data from shivkumar.org (~300 compositions) maps to the existing `krithi_notation_variants` and `krithi_notation_rows` tables.

**Notation Parsing Requirements**:

- Parse Swara text (Sa Ri Ga Ma Pa Da Ni) into structured rows.
- Identify tala markers and avartanam boundaries.
- Detect kalai (speed) and eduppu (starting beat offset).
- Align notation rows with corresponding lyric sections.
- Support multiple performance variants (Lalgudi bani, SSP, etc.) via `sampradaya_id`.

**Approach**: The notation parser can be implemented in either Kotlin (extending `KrithiStructureParser`) or Python (alongside the PDF extractor), depending on the source format. HTML notation from shivkumar.org is parsed in Kotlin; PDF/DOCX notation routes through the Python service.

---

## 6. Data Quality Framework

### 6.1 Quality Dimensions

| Dimension | Definition | Measurement | Target |
|:---|:---|:---|:---|
| **Completeness** | All required fields populated (title, composer, raga, tala, sections, at least one lyric variant) | % of required fields present per Krithi | >90% for PUBLISHED |
| **Structural Integrity** | Section structure consistent across all lyric variants; conforms to musical form rules | Section count alignment ratio across variants | 100% alignment |
| **Entity Resolution Confidence** | Raw names correctly mapped to canonical entities | Resolution confidence score (HIGH/MEDIUM/LOW) | >80% HIGH confidence |
| **Source Authority** | Provenance traced to authoritative sources | Weighted source tier score | >60% sourced from Tier 1–3 |
| **Extraction Confidence** | Extraction method reliability: born-digital PDF > OCR > blog HTML. Includes OCR confidence for scanned documents. | Extraction method weight + OCR confidence score | >70% from born-digital sources |
| **Deduplication Cleanliness** | No duplicate Krithis; all variants correctly merged | Duplicate candidate count per Krithi | 0 unresolved duplicates |

### 6.2 Quality Scoring Formula

The existing `QualityScoringService` computes `quality_score`, `quality_tier`, `completeness_score`, `resolution_confidence`, `source_quality`, and `validation_score`. The enhanced formula should be:

```text
quality_score = (
    completeness_score * 0.25 +
    structural_integrity_score * 0.25 +
    resolution_confidence * 0.20 +
    source_authority_score * 0.20 +
    deduplication_score * 0.10
)

quality_tier =
    GOLD   if quality_score >= 85
    SILVER if quality_score >= 70
    BRONZE if quality_score >= 50
    NEEDS_REVIEW otherwise
```

### 6.3 TRACK-039 Integration: Structural Auditing

TRACK-039's structural consistency audit becomes the quality gate for Phase 2. The SQL audits should be automated:

**Audit 1 — Section Count Mismatch**:
```sql
-- Find Krithis where lyric variants have different section counts
SELECT k.id, k.title, klv.language, COUNT(kls.id) as section_count
FROM krithis k
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
GROUP BY k.id, k.title, klv.language
HAVING COUNT(kls.id) != (
    SELECT COUNT(ks.id) FROM krithi_sections ks WHERE ks.krithi_id = k.id
);
```

**Audit 2 — Label Sequence Mismatch**: Compare section label ordering across variants.

**Audit 3 — Orphaned Lyric Blobs**: Find lyric sections without a parent section mapping.

These audits should run nightly as scheduled jobs, feeding results into the quality dashboard.

### 6.4 TRACK-040 Integration: Remediation Pipeline

TRACK-040's remediation runs as post-processing after each enrichment phase:

1. **MetadataCleanupTask**: Strip boilerplate from `krithi_lyric_sections` (blog headers, copyright notices, navigation text ingested from HTML).
2. **StructuralNormalisationLogic**: Align all variants to the canonical section template. Split combined sections or flag gaps.
3. **DeduplicationService**: Merge duplicate Krithis (normalised title + composer matching) under the canonical entry. Archive duplicates. Preserve provenance.
4. **Quality Re-scoring**: Re-run `QualityScoringService` after remediation. Promote qualifying Krithis to `IN_REVIEW` or `PUBLISHED`.

**Priority Order** (per TRACK-040): Dikshitar first (highest authority source now available via guruguha.org PDFs), then Thyagaraja, then others.

---

## 7. Database Schema Extensions

### 7.1 Existing Schema (Leveraged As-Is)

The current schema is well-designed for the proposed strategy. The following tables are used without modification:

- `krithis`, `krithi_sections`, `krithi_ragas` — Core composition model
- `krithi_lyric_variants`, `krithi_lyric_sections` — Lyric storage
- `krithi_notation_variants`, `krithi_notation_rows` — Notation storage
- `imported_krithis` — Staging area with quality scores
- `import_batch`, `import_job`, `import_task_run`, `import_event` — Orchestration
- `entity_resolution_cache` — Resolution memoisation
- `composers`, `ragas`, `talas`, `deities`, `temples` — Reference data

### 7.2 New/Modified Tables

**Migration 23: Source Authority Enhancement**
```sql
ALTER TABLE import_sources ADD COLUMN source_tier INTEGER NOT NULL DEFAULT 5
    CHECK (source_tier BETWEEN 1 AND 5);
ALTER TABLE import_sources ADD COLUMN supported_formats TEXT[] DEFAULT '{"HTML"}';
ALTER TABLE import_sources ADD COLUMN composer_affinity JSONB DEFAULT '{}';
ALTER TABLE import_sources ADD COLUMN last_harvested_at TIMESTAMPTZ;
```

**Migration 24: Source Evidence Tracking**
```sql
CREATE TABLE krithi_source_evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    import_source_id UUID NOT NULL REFERENCES import_sources(id),
    source_url TEXT NOT NULL,
    source_format TEXT NOT NULL,  -- HTML, PDF, DOCX, API, MANUAL
    extraction_method TEXT NOT NULL,
    page_range TEXT,
    extracted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    checksum TEXT,
    confidence DECIMAL(5,4),
    contributed_fields TEXT[] NOT NULL DEFAULT '{}',
    raw_extraction JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kse_krithi ON krithi_source_evidence(krithi_id);
CREATE INDEX idx_kse_source ON krithi_source_evidence(import_source_id);
```

**Migration 25: Structural Vote Log**
```sql
CREATE TABLE structural_vote_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    voted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    participating_sources JSONB NOT NULL,  -- [{sourceId, tier, sectionStructure}]
    consensus_structure JSONB NOT NULL,    -- [{type, order, label}]
    consensus_type TEXT NOT NULL,          -- UNANIMOUS, MAJORITY, AUTHORITY_OVERRIDE, MANUAL
    confidence TEXT NOT NULL,              -- HIGH, MEDIUM, LOW
    dissenting_sources JSONB DEFAULT '[]',
    reviewer_id UUID REFERENCES users(id),
    notes TEXT
);

CREATE INDEX idx_svl_krithi ON structural_vote_log(krithi_id);
```

**Migration 26: Import Task Format Tracking**
```sql
ALTER TABLE import_task_run ADD COLUMN source_format TEXT DEFAULT 'HTML';
ALTER TABLE import_task_run ADD COLUMN page_range TEXT;
```

**Migration 27: Extraction Queue** (see Section 8.3.2 for full schema)

The `extraction_queue` table is the integration contract between Kotlin and Python containers.

**Migration 28: Source Documents & Extraction Runs (Medium-Term)**

These dedicated tables replace the short-term approach of packing everything into `parsed_payload` JSONB:

```sql
-- One row per acquired source artifact (PDF file, HTML snapshot, etc.)
CREATE TABLE source_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_source_id UUID NOT NULL REFERENCES import_sources(id),
    url TEXT NOT NULL,
    mime_type TEXT NOT NULL,           -- application/pdf, text/html, etc.
    checksum TEXT NOT NULL,            -- SHA-256
    file_size_bytes BIGINT,
    cached_path TEXT,                  -- path in artifact storage
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'        -- page count, encoding, etc.
);

CREATE UNIQUE INDEX idx_sd_checksum ON source_documents(checksum);
CREATE INDEX idx_sd_source ON source_documents(import_source_id);

-- One row per extraction attempt against a source document
CREATE TABLE extraction_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_document_id UUID NOT NULL REFERENCES source_documents(id),
    extractor_type TEXT NOT NULL,       -- PDF_PYMUPDF, PDF_OCR, HTML_JSOUP, HTML_GEMINI
    extractor_version TEXT NOT NULL,    -- e.g. 'krithi-extract-enrich-worker:1.2.0'
    status TEXT NOT NULL DEFAULT 'PENDING',
    confidence DECIMAL(5,4),
    result_payload JSONB,              -- canonical extraction JSON
    error_detail JSONB,
    duration_ms INTEGER,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_er_source_doc ON extraction_runs(source_document_id);
CREATE INDEX idx_er_status ON extraction_runs(status);

-- Per-field provenance: which source/extraction asserted each field
CREATE TABLE field_assertions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    field_name TEXT NOT NULL,           -- 'title', 'raga', 'tala', 'section_structure', 'deity', etc.
    asserted_value TEXT NOT NULL,
    extraction_run_id UUID REFERENCES extraction_runs(id),
    source_document_id UUID REFERENCES source_documents(id),
    source_tier INTEGER,
    confidence DECIMAL(5,4),
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fa_krithi_field ON field_assertions(krithi_id, field_name);
CREATE INDEX idx_fa_current ON field_assertions(krithi_id) WHERE is_current = TRUE;
```

These tables enable the Review UI to show per-field source lineage and the quality scoring service to compute field-level confidence.

---

## 8. Technology Decisions & Containerised Deployment

### 8.1 Why Python for PDF Extraction (Not Kotlin)

| Factor | Python | Kotlin/JVM |
|:---|:---|:---|
| **PDF Libraries** | PyMuPDF (fitz), pdfplumber, Camelot — mature, well-documented, Indic script support, positional text API | Apache PDFBox — Java-based, less Unicode handling, no positional text API equivalent |
| **OCR Integration** | pytesseract (Tesseract wrapper) — trivial setup with Indic language packs (Sanskrit, Tamil, Telugu, Kannada, Malayalam) | Tess4J — JNI wrapper, more complex setup, fewer community examples for Indic scripts |
| **Indic Text Processing** | indic-transliteration, aksharamukha — purpose-built libraries for script conversion | No equivalent Kotlin/JVM libraries |
| **NLP Ecosystem** | spaCy, NLTK, regex — mature text processing for diacritical normalisation | Limited NLP libraries in Kotlin ecosystem |
| **Development Speed** | Rapid prototyping for extraction experiments, iterative prompt engineering | Heavier boilerplate for text processing tasks |
| **Community** | Vast PDF/NLP community; StackOverflow, GitHub examples for Indic text extraction | Sparse examples for Indic-script PDF extraction on JVM |

**Decision**: Python service for all document extraction (PDF, DOCX, OCR, transliteration). Kotlin for orchestration, entity resolution, quality scoring, and editorial governance. The two communicate via a shared PostgreSQL database table — no HTTP coupling, no subprocess management, no shared filesystem.

### 8.2 Docker-Containerised Architecture

The Python extraction service is deployed as a **Docker container** alongside the existing PostgreSQL and Kotlin backend containers, following the same pattern already established for the database. This provides process isolation, independent scaling, and reproducible environments.

#### 8.2.1 Docker Compose Configuration (Local Development)

```yaml
# compose.yaml (extended)
services:
  postgres:
    image: postgres:15
    container_name: sangita_postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: sangita_grantha
    volumes:
      - sangita_postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 2s
      timeout: 5s
      retries: 10

  krithi-extract-enrich-worker:
    build:
      context: ./tools/krithi-extract-enrich-worker
      dockerfile: Dockerfile
    container_name: sangita_krithi_extract_enrich_worker
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DATABASE_URL: postgresql://postgres:postgres@postgres:5432/sangita_grantha
      SG_GEMINI_API_KEY: ${SG_GEMINI_API_KEY}
      EXTRACTION_POLL_INTERVAL_S: 5
      EXTRACTION_BATCH_SIZE: 10
      EXTRACTION_MAX_CONCURRENT: 3
      LOG_LEVEL: INFO
    volumes:
      - extraction_cache:/app/cache    # cached PDFs and source artifacts
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "python", "-c", "import psycopg2; psycopg2.connect('${DATABASE_URL}')"]
      interval: 10s
      timeout: 5s
      retries: 3

volumes:
  sangita_postgres_data:
  extraction_cache:
```

#### 8.2.2 Dockerfile (Python Extraction Service)

```dockerfile
# tools/krithi-extract-enrich-worker/Dockerfile
FROM python:3.11-slim

# Install system dependencies for OCR and Indic text
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-san \
    tesseract-ocr-tam \
    tesseract-ocr-tel \
    tesseract-ocr-kan \
    tesseract-ocr-mal \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY pyproject.toml .
RUN pip install --no-cache-dir .
COPY src/ ./src/

# Health check and entry point
HEALTHCHECK --interval=30s --timeout=5s CMD python -c "import sys; sys.exit(0)"
ENTRYPOINT ["python", "-m", "src.worker"]
```

#### 8.2.3 Kubernetes Deployment (Production / GCP)

For production on GCP, the extraction service runs as a Kubernetes Deployment in the same namespace as the backend, connecting to Cloud SQL via the Cloud SQL Proxy sidecar:

```yaml
# k8s/krithi-extract-enrich-worker-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: krithi-extract-enrich-worker
  namespace: sangita-grantha
spec:
  replicas: 2    # scale based on queue depth
  selector:
    matchLabels:
      app: krithi-extract-enrich-worker
  template:
    metadata:
      labels:
        app: krithi-extract-enrich-worker
    spec:
      containers:
        - name: krithi-extract-enrich-worker
          image: gcr.io/sangita-grantha/krithi-extract-enrich-worker:latest
          env:
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: sangita-db-credentials
                  key: connection-string
            - name: SG_GEMINI_API_KEY
              valueFrom:
                secretKeyRef:
                  name: sangita-gemini
                  key: api-key
            - name: EXTRACTION_POLL_INTERVAL_S
              value: "3"
            - name: EXTRACTION_BATCH_SIZE
              value: "20"
            - name: EXTRACTION_MAX_CONCURRENT
              value: "5"
          resources:
            requests:
              cpu: 500m
              memory: 1Gi
            limits:
              cpu: 2000m
              memory: 4Gi    # OCR + PDF parsing can be memory-intensive
          volumeMounts:
            - name: extraction-cache
              mountPath: /app/cache
        - name: cloud-sql-proxy
          image: gcr.io/cloud-sql-connectors/cloud-sql-proxy:2
          args: ["--structured-logs", "INSTANCE_CONNECTION_NAME"]
      volumes:
        - name: extraction-cache
          emptyDir:
            sizeLimit: 10Gi
```

Horizontal Pod Autoscaler can scale replicas based on queue depth:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: krithi-extract-enrich-worker-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: krithi-extract-enrich-worker
  minReplicas: 1
  maxReplicas: 5
  metrics:
    - type: External
      external:
        metric:
          name: extraction_queue_pending_count
        target:
          type: AverageValue
          averageValue: "10"
```

### 8.3 Integration via Database Queue Table

The Kotlin backend and Python extraction service communicate through a shared PostgreSQL table (`extraction_queue`) rather than HTTP calls, message brokers, or subprocess invocation. This approach has several advantages:

- **Zero additional infrastructure**: Reuses the existing PostgreSQL instance. No Redis, RabbitMQ, or Kafka to operate.
- **Transactional guarantees**: Task claiming uses `SELECT ... FOR UPDATE SKIP LOCKED`, providing exactly-once processing semantics natively.
- **Observability**: Standard SQL queries expose queue depth, processing times, error rates, and stuck tasks.
- **Alignment with existing patterns**: The `import_task_run` table already implements a similar polling pattern. The extraction queue follows the same convention.
- **Independent lifecycle**: The Python container can restart, scale, or be upgraded without affecting the Kotlin backend, and vice versa.

#### 8.3.1 Integration Flow

```text
   Kotlin Backend                    PostgreSQL                   Python Extractor
   ──────────────                    ──────────                   ─────────────────
        │                                 │                              │
   1. ManifestWorker                      │                              │
      identifies PDF task                 │                              │
        │                                 │                              │
   2. INSERT INTO extraction_queue ──────►│                              │
      (status=PENDING,                    │                              │
       source_url, source_format,         │                              │
       request_payload as JSON)           │                              │
        │                                 │                              │
        │                                 │◄──── 3. Poll: SELECT ... WHERE
        │                                 │          status='PENDING'
        │                                 │          FOR UPDATE SKIP LOCKED
        │                                 │                              │
        │                                 │      4. UPDATE status='PROCESSING'
        │                                 │         claimed_at=NOW()     │
        │                                 │                              │
        │                                 │      5. Extract PDF/DOCX/OCR │
        │                                 │         Run LLM refinement   │
        │                                 │         Validate output      │
        │                                 │                              │
        │                                 │◄──── 6. UPDATE status='DONE'
        │                                 │         result_payload=JSON
        │                                 │         (or status='FAILED',
        │                                 │          error_detail=JSON)
        │                                 │                              │
   7. ResolutionWorker polls  ───────────►│                              │
      for DONE extraction tasks           │                              │
        │                                 │                              │
   8. Reads result_payload,               │                              │
      creates ImportedKrithi,             │                              │
      queues Resolution tasks             │                              │
```

#### 8.3.2 Queue Table Schema

```sql
-- Migration 27: Extraction Queue (Kotlin ↔ Python integration)
CREATE TYPE extraction_status AS ENUM (
    'PENDING', 'PROCESSING', 'DONE', 'FAILED', 'CANCELLED'
);

CREATE TABLE extraction_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_batch_id UUID REFERENCES import_batch(id),
    import_task_run_id UUID REFERENCES import_task_run(id),

    -- Request (written by Kotlin)
    source_url TEXT NOT NULL,
    source_format TEXT NOT NULL,            -- PDF, DOCX, IMAGE
    source_name TEXT,                       -- e.g. 'guruguha.org'
    source_tier INTEGER CHECK (source_tier BETWEEN 1 AND 5),
    request_payload JSONB NOT NULL,         -- extraction parameters
    page_range TEXT,                        -- e.g. '42-43' for specific pages

    -- Processing state
    status extraction_status NOT NULL DEFAULT 'PENDING',
    claimed_at TIMESTAMPTZ,
    claimed_by TEXT,                        -- container hostname for debugging
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,

    -- Result (written by Python)
    result_payload JSONB,                   -- array of CanonicalExtractionDto
    result_count INTEGER,                   -- number of Krithis extracted
    extraction_method TEXT,                 -- PDF_PYMUPDF, PDF_OCR, DOCX_PYTHON
    extractor_version TEXT,                 -- e.g. 'krithi-extract-enrich-worker:1.2.0'
    confidence DECIMAL(5,4),
    duration_ms INTEGER,

    -- Error handling
    error_detail JSONB,                     -- structured error info
    last_error_at TIMESTAMPTZ,

    -- Artifact tracking
    source_checksum TEXT,                   -- SHA-256 of source document
    cached_artifact_path TEXT,              -- path in extraction_cache volume

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for polling performance (matches existing import_task_run pattern)
CREATE INDEX idx_eq_status_created ON extraction_queue(status, created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_eq_batch ON extraction_queue(import_batch_id);
CREATE INDEX idx_eq_status_done ON extraction_queue(status, updated_at)
    WHERE status = 'DONE';
```

#### 8.3.3 Python Worker Loop

The Python container runs a simple polling loop:

```python
# tools/krithi-extract-enrich-worker/src/worker.py (conceptual)
async def worker_loop():
    while True:
        async with db.transaction():
            task = await db.fetch_one("""
                SELECT * FROM extraction_queue
                WHERE status = 'PENDING' AND attempts < max_attempts
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT 1
            """)
            if task:
                await db.execute("""
                    UPDATE extraction_queue
                    SET status = 'PROCESSING', claimed_at = NOW(),
                        claimed_by = :hostname, attempts = attempts + 1
                    WHERE id = :id
                """, id=task.id, hostname=HOSTNAME)

        if task:
            try:
                result = await extract(task)
                await db.execute("""
                    UPDATE extraction_queue
                    SET status = 'DONE', result_payload = :result,
                        result_count = :count, extraction_method = :method,
                        extractor_version = :version, confidence = :conf,
                        duration_ms = :duration, updated_at = NOW()
                    WHERE id = :id
                """, **result)
            except Exception as e:
                await db.execute("""
                    UPDATE extraction_queue
                    SET status = 'FAILED', error_detail = :error,
                        last_error_at = NOW(), updated_at = NOW()
                    WHERE id = :id
                """, id=task.id, error=json.dumps(serialize_error(e)))
        else:
            await asyncio.sleep(POLL_INTERVAL_S)
```

### 8.4 Why Database Queue Over Alternatives

| Approach | Pros | Cons | Verdict |
|:---|:---|:---|:---|
| **Database table (chosen)** | Zero new infrastructure; transactional; SQL observability; matches existing `import_task_run` pattern; works identically in dev/staging/prod | Polling latency (configurable, 3–5s acceptable for batch work); PostgreSQL connection required | **Best fit** — simplest, most aligned with existing architecture |
| **HTTP microservice** | Synchronous request/response; easy to test with curl | Tight coupling; Kotlin must handle timeouts, retries, circuit breaking; Python service must be always-up; different failure modes from existing pipeline | Adds complexity without proportional benefit |
| **Message queue (Redis/RabbitMQ)** | True async; backpressure; dead-letter queues | New infrastructure to operate; additional failure modes; overkill for batch processing volumes (~500–2000 items per run) | Premature for current scale; consider if >10K items/day |
| **Subprocess (CLI)** | Simplest possible integration | No parallelism; Python process lifecycle tied to Kotlin; no independent scaling; breaks in containerised deployments | Suitable only for PoC, not production |

---

## 9. Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|:---|:---|:---|:---|
| PDF text extraction quality for Indic scripts | Medium | High | Use PyMuPDF (strong Unicode). OCR fallback for image text. Validate against known samples before bulk runs. |
| Source website availability changes | Medium | Medium | Cache all PDFs/HTML locally with checksums (`import_task_run.checksum`). Store `evidence_path`. Design for offline re-processing. |
| LLM extraction hallucination | Low–Medium | High | LLM output always validated against pattern-matched extraction. Structural voting catches single-source errors. Human review mandatory before publication. |
| Deduplication false positives | Medium | Medium | Composite matching (title + composer + raga), not title alone. HIGH confidence for auto-merge; MEDIUM/LOW for manual review. Merge audit trail for rollback. |
| Scale of manual review backlog | High | Medium | Auto-approval thresholds for Tier 1 sources with quality_score > 85. Prioritise editorial effort on Tier 4–5 imports. Batch review UI. |
| Python/Kotlin integration complexity | Low | Low | Simple JSON interface (CLI or HTTP). No shared state. Well-defined canonical schema at boundary. Docker for consistent environment. |
| Transliteration accuracy | Medium | Medium | LLM validation of transliterated text. Flag low-confidence transliterations for human review. Never auto-publish transliterated variants. |
| Notation parsing complexity | Medium | Medium | Start with simplest notation format (shivkumar.org HTML). Build notation parser incrementally. Manual validation for first 50 compositions. |
| Model-driven extraction instability | Medium | Medium | Deterministic parser takes precedence where available. Schema-constrained JSON output from Gemini. Pin extractor versions with regression fixtures. |
| Source structure drift (site redesigns) | Low–Medium | Medium | Versioned extractor profiles per source. Source-specific handlers with regression test fixtures. Alerting on sudden confidence drops by source. |
| Operational load increase | Medium | Low | Staged rollout with capped concurrency. Queue-level observability (depth, latency, error rate). HPA scales Python containers based on queue depth. |

---

## 10. Success Metrics

| Metric | Current State | Target (6 Months) | Target (12 Months) |
|:---|:---|:---|:---|
| Total Krithis in database | ~1,240 (Trinity) | 2,000+ (+ Dikshitar PDF + Swathi Thirunal) | 2,500+ (+ shivkumar.org + karnatik cross-ref) |
| Krithis with 2+ source evidence | ~0% | >40% (Dikshitar from PDF + blogspot) | >60% |
| Krithis with multi-script variants | ~20% (mostly single-script) | >35% with 2+ scripts | >50% with 3+ scripts |
| Section structure consistency | Unknown (pre-audit) | 100% for PUBLISHED Krithis | 100% maintained |
| Dikshitar Krithis with deity/temple | ~30% | >80% (from guruguha.org PDFs) | >90% |
| Krithis with notation data | ~0 | 100 (initial shivkumar.org batch) | 300+ |
| Average quality score (PUBLISHED) | Not yet measured | >75% | >85% |
| Source formats supported | HTML only | HTML, PDF | HTML, PDF, DOCX |
| Automated audit coverage | None | Nightly structural audits | Full quality dimension coverage |

---

## 11. Relationship to Existing Documentation

This strategy document builds on and extends the following existing documents:

| Document | Relationship |
|:---|:---|
| `01-requirements/features/bulk-import/01-strategy/master-analysis.md` | This strategy extends the master analysis with multi-format support and quality framework |
| `01-requirements/features/bulk-import/03-sources/web-source-analysis.md` | Source landscape (Section 3) supersedes with updated source registry and authority hierarchy |
| `01-requirements/features/intelligent-content-ingestion.md` | Phase 1–5 enrichment operationalises the intelligent ingestion vision |
| `01-requirements/features/advanced-krithi-notation-transliteration.md` | Phase 5 notation ingestion implements the notation requirements |
| `04-database/schema.md` | Schema extensions (Section 7) are additive to the existing schema |
| `conductor/tracks/TRACK-039` | Structural auditing becomes automated quality gate (Section 6.3) |
| `conductor/tracks/TRACK-040` | Remediation pipeline integrates as post-processing (Section 6.4) |
| `conductor/tracks/TRACK-041` | Structural voting engine is core component (Section 5.3) |

---

## 12. Recommendations & Next Steps

### 12.1 Immediate (Next 2 Weeks)

1. **Complete TRACK-039 SQL audits** on existing database to establish quality baseline.
2. **Prototype PDF extraction**: Build standalone Python script extracting 10 Dikshitar Krithis from `mdskt.pdf`, output canonical JSON, validate against existing DB entries.
3. **Formalise canonical extraction schema** as a shared JSON Schema file in `shared/domain/model/import/`.

### 12.2 Short-Term (1–2 Months)

4. **Build `PdfScrapeWorker`**: Integrate Python PDF service into Kotlin orchestration. Full batch import of Dikshitar Sanskrit collection.
5. **Implement source tier ranking**: Extend `import_sources` table. Update admin UI to display provenance.
6. **Begin TRACK-040 remediation**: Dikshitar compositions first (Tier 1 source now available as reference).

### 12.3 Medium-Term (3–6 Months)

7. **Multi-source structural voting**: Implement `StructuralVotingEngine` as sources provide overlapping coverage.
8. **Swathi Thirunal ingestion**: Dedicated scraper for swathithirunalfestival.org (~400 compositions).
9. **Notation pipeline**: Notation parser + shivkumar.org ingestion (~300 compositions).
10. **Transliteration automation**: Python transliteration service for automated script variant generation.

### 12.4 Long-Term Vision

Sangeetha Grantha should evolve into a platform where new Krithi data can be contributed through a submission workflow, validated automatically against the quality framework, and integrated with full provenance tracking. The system of record aspiration is realised not through a single heroic import, but through the steady accumulation of verified, cross-referenced, and editorially governed data from the highest-quality sources available.
