| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-02-28 |
| **Author** | Sangeetha Grantha Team |

# Sangita Grantha – Schema Overview


**Status:** Draft  
**Version:** 0.4  
**Owners:** Sangita Grantha Data & Backend Team  

---

## 1. Purpose

This document describes the **authoritative PostgreSQL schema** for Sangita Grantha.

The schema is designed to:

- Accurately model **Carnatic musical compositions** across forms
- Preserve **musicological structure** (sections, ragas, tala)
- Support **notation-centric forms** such as Varnams and Swarajathis
- Enable **fast search**, **editorial workflows**, and **data provenance**
- Align with **Kotlin Multiplatform shared DTOs**

This document is the **single source of truth** for how musical concepts are persisted.

---

## 2. Core Design Principles

1. **Musical Form Awareness**  
   Every composition is classified by a `musical_form`, which determines:
   - Required sections
   - Presence of notation
   - Validation rules

2. **Lyrics and Notation Are Distinct**  
   - Lyrics are language/script specific
   - Notation is performance- and tala-specific  
   **Swara notation MUST NOT be stored in lyric tables**

3. **Normalized, Editorially Safe Schema**
   - No denormalized blobs for core musical data
   - All mutations audited
   - Imported data never auto-published

4. **Read-Optimized for Mobile**
   - Ordered sections and notation rows
   - Deterministic rendering
   - Trigram indexes for lyric search

---

## 3. Enum Types

Defined in `01__baseline-schema-and-types.sql`.

### 3.1 `workflow_state_enum`
```text
draft
in_review
published
archived
```
Controls editorial lifecycle.

---

### 3.2 `language_code_enum`
```text
sa  – Sanskrit
ta  – Tamil
te  – Telugu
kn  – Kannada
ml  – Malayalam
hi  – Hindi
en  – English
```
---

### 3.3 `script_code_enum`
```text
devanagari
tamil
telugu
kannada
malayalam
latin
```
---

### 3.4 `musical_form_enum`
```text
KRITHI
VARNAM
SWARAJATHI
```
Defines structural and notation requirements.

---

### 3.5 `import_status_enum`
```text
pending
in_review
mapped
rejected
discarded
```
---

## 4. Core Reference Entities

### 4.1 `composers`

Canonical list of composers.

Key fields:
- `name`, `name_normalized`
- `birth_year`, `death_year`
- `place`, `notes`

---

### 4.2 `ragas`

Supports janya/melakarta hierarchy.

Key fields:
- `name`, `name_normalized`
- `melakarta_number`
- `parent_raga_id`
- `arohanam`, `avarohanam`

---

### 4.3 `talas`

Key fields:
- `name`, `name_normalized`
- `anga_structure`
- `beat_count`

---

### 4.4 `deities`

Canonical deity registry.

---

### 4.5 `temples`

Supports kṣetram attribution.

Includes:
- Location (city/state/country)
- Primary deity
- Geo-coordinates
- Multilingual names via `temple_names`

---

## 5. Core Composition Model

### 5.1 `krithis`

Represents **all compositions**, regardless of musical form.

Key fields:
- `title`, `title_normalized`, `incipit`, `incipit_normalized`
- `composer_id`
- `musical_form`
- `primary_language`
- `primary_raga_id`
- `tala_id`
- `deity_id`
- `temple_id`
- `is_ragamalika`
- `workflow_state`

**Rules**
- Public APIs expose only `published`
- Musical form drives validation and UI behavior

---

### 5.2 `krithi_ragas`

Supports **ragamalika**.

- Ordered list of ragas
- Optional section-level association

---

## 6. Section Modeling

### 6.1 `krithi_sections`

Defines the **structural grammar** of a composition.

Supported `section_type` values:
```text
PALLAVI
ANUPALLAVI
CHARANAM
MUKTAAYI_SWARAM
CHITTASWARAM
JATHI
SAMASHTI_CHARANAM
SWARA_SAHITYA
MADHYAMA_KALA
OTHER
```
Each section:
- Belongs to a single composition
- Has an explicit `order_index`

---

### 6.2 Section Constraints (Enforced at Service Layer)

**VARNAM**
- Pallavi + Anupallavi mandatory
- Exactly one Muktaayi Swaram
- At least one Chittaswaram

**SWARAJATHI**
- Alternating Jathi and Sahitya sections
- Tala alignment mandatory

---

## 7. Lyric Modeling (Sahitya)

### 7.1 `krithi_lyric_variants`

Represents **language/script-specific** lyric versions.

Key fields:
- `language`
- `script`
- `transliteration_scheme`
- `sampradaya_id` (optional)
- `variant_label`
- `source_reference`

---

### 7.2 `krithi_lyric_sections`

Stores **section-wise lyric text**.

Key points:
- One row per (variant × section)
- Supports normalized text for search
- Indexed for substring queries

---

## 8. Notation Modeling (Varnams & Swarajathis)

Notation is **first-class and independent of lyrics**.

---

### 8.1 `krithi_notation_variants`

Represents a **notation interpretation** of a composition.

Key fields:
- `notation_type` (`SWARA` | `JATHI`)
- `tala_id`
- `kalai`
- `eduppu_offset_beats`
- `variant_label`
- `source_reference`
- `is_primary`

Examples:
- Lalgudi bani
- SSP notation
- Shivkumar.org transcription

---

### 8.2 `krithi_notation_rows`

Stores **line-by-line notation**, aligned to sections.

Each row:
- Belongs to a notation variant
- Belongs to a section
- Has an `order_index`

Fields:
- `swara_text`
- `sahitya_text` (optional)
- `tala_markers`

This models:
- Avartanam boundaries
- Swara–sahitya alignment
- Multiple cycles per section

Notation variants are stored independently of lyric variants to keep lyrics and swara notation separate.

---

## 9. Tags & Sampradayas

### 9.1 `tags` and `krithi_tags`

Controlled taxonomy:
- Bhava
- Festival
- Philosophy
- Kṣetra
- Style

---

### 9.2 `sampradayas`

Supports:
- Pathantaram
- Bani
- School

Used by:
- Lyric variants
- Notation variants

---

## 10. Import Pipeline

### 10.1 `import_sources`

Tracks provenance of scraped data.

**Source Authority Enhancement** *(Migration 23)*:
- `source_tier` — Authority level 1–5 (1 = scholarly/published, 5 = individual blogs). See [Source Authority Hierarchy](../01-requirements/krithi-data-sourcing/quality-strategy.md#32-source-authority-hierarchy).
- `supported_formats` — Array of supported formats (`HTML`, `PDF`, `DOCX`, etc.)
- `composer_affinity` — JSONB map of composer → weight (e.g., `{"dikshitar": 1.0}` for guruguha.org)
- `last_harvested_at` — Timestamp of most recent harvest from this source

---

### 10.2 `imported_krithis`

Stores raw imported records.

Rules:
- Never auto-published
- Must be reviewed and mapped
- Retains original raw text and metadata

---

### 10.3 `import_task_run` *(Enhanced — Migration 26)*

**Format Tracking**:
- `source_format` — Format of source document (`HTML`, `PDF`, `DOCX`, `IMAGE`). Default: `HTML`.
- `page_range` — For PDF sources, the specific pages extracted (e.g., `42-43`).

---

### 10.4 `krithi_source_evidence` *(New — Migration 24)*

Links each Krithi to all sources that contributed data, with per-source extraction metadata and confidence scores. Supports the multi-source provenance model.

Key columns:
- `krithi_id` — FK to `krithis`
- `import_source_id` — FK to `import_sources`
- `source_url`, `source_format`, `extraction_method`
- `page_range` — For PDFs (e.g., `42-43`)
- `confidence` — Extraction confidence score
- `contributed_fields` — Array of field names this source asserted (e.g., `{title, raga, tala, sections}`)
- `raw_extraction` — Full extraction payload (JSONB) for audit/replay

Indexes: `krithi_id`, `import_source_id`.

---

### 10.5 `structural_vote_log` *(New — Migration 25)*

Audit trail for cross-source structural voting decisions. When multiple sources provide section structures for the same Krithi, the [Structural Voting Engine](../01-requirements/krithi-data-sourcing/quality-strategy.md#53-phase-2--structural-validation-track-041-integration) records the outcome.

Key columns:
- `krithi_id` — FK to `krithis`
- `participating_sources` — JSONB array of `{sourceId, tier, sectionStructure}`
- `consensus_structure` — JSONB array of `{type, order, label}` (the winning structure)
- `consensus_type` — `UNANIMOUS`, `MAJORITY`, `AUTHORITY_OVERRIDE`, or `MANUAL`
- `confidence` — `HIGH`, `MEDIUM`, or `LOW`
- `dissenting_sources` — JSONB array of sources that disagreed
- `reviewer_id` — Optional FK to `users` (for manual overrides)

---

### 10.6 `extraction_queue` *(New — Migration 27)*

Database-backed work queue for Kotlin ↔ Python integration. The Kotlin backend writes extraction requests; the Python PDF extraction service polls, processes, and writes results back. Uses `SELECT ... FOR UPDATE SKIP LOCKED` for exactly-once processing.

Key columns:
- `import_batch_id`, `import_task_run_id` — FKs for orchestration context
- `source_url`, `source_format`, `source_name`, `source_tier` — Request metadata
- `request_payload` — JSONB extraction parameters (written by Kotlin)
- `status` — Enum: `PENDING` → `PROCESSING` → `DONE` / `FAILED` / `CANCELLED`
- `result_payload` — JSONB array of `CanonicalExtractionDto` (written by Python)
- `result_count`, `extraction_method`, `extractor_version`, `confidence`, `duration_ms` — Result metadata
- `attempts`, `max_attempts` — Retry tracking
- `source_checksum`, `cached_artifact_path` — Artifact tracking

Indexes: Partial indexes on `status = 'PENDING'` and `status = 'DONE'` for efficient polling.

See [Extraction Queue Architecture](../01-requirements/krithi-data-sourcing/quality-strategy.md#83-integration-via-database-queue-table) for the integration pattern.

---

## 11. Audit & Governance

### 11.1 `audit_log`

Immutable audit trail.

Captures:
- Actor
- Action
- Entity
- Field-level diff (JSONB)
- Timestamp

All admin mutations MUST write an audit entry.

---

## 12. SQL Functions

### 12.1 `strip_diacritics(text)`

PL/pgSQL function that converts IAST diacritics to ASCII equivalents for search normalisation. Used to populate `title_normalized` and `incipit_normalized` columns, enabling ASCII search queries against diacritics-rich Carnatic music titles (e.g., searching "akhilandesvari" matches "akhilāṇḍeśvaryai").

Mappings include:
- Macron vowels: ā→a, ī→i, ū→u, ē→e, ō→o
- Retroflex/palatal consonants: ṭ→t, ḍ→d, ṇ→n, ṅ→n, ñ→n, ś→s, ṣ→s
- Anusvara/visarga: ṃ→m, ḥ→h
- Chandrabindu and other marks stripped

---

## 13. Indexing & Performance

- Trigram indexes on lyric text
- Normalized name indexes on reference data (`title_normalized`, `incipit_normalized`, `name_normalized`)
- Ordered indexes on sections and notation rows

Target:
- p95 lyric search < 300ms
- Deterministic rendering for mobile clients

---

## 14. DTO Alignment

Shared DTOs in `modules/shared/domain` mirror this schema:

- `ComposerDto`, `RagaDto`, `TalaDto`, `DeityDto`, `TempleDto`.
- `KrithiDto`, `KrithiRagaDto`, `KrithiLyricVariantDto`.
- `KrithiSectionDto`, `KrithiLyricSectionDto`.
- `KrithiNotationVariantDto`, `KrithiNotationRowDto`.
- `TagDto`, `KrithiTagDto`, `SampradayaDto`, `TempleNameDto`.
- `CanonicalExtractionDto` — Universal extraction format for all source adapters (PDF, HTML, DOCX). See [`modules/shared/domain/.../import/CanonicalExtractionDto.kt`](../../modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/import/CanonicalExtractionDto.kt).

Any schema change MUST:
1. Add a migration
2. Update DTOs
3. Update this document

---

## 15. Recent Migrations (Feb 2026)

| Migration | Table/Columns | Purpose |
|:---|:---|:---|
| 23 | `import_sources` + 4 columns | Source authority tiers, supported formats, composer affinity |
| 24 | `krithi_source_evidence` (new) | Per-source provenance tracking for each Krithi |
| 25 | `structural_vote_log` (new) | Cross-source structural voting audit trail |
| 26 | `import_task_run` + 2 columns | Source format and page range tracking |
| 27 | `extraction_queue` (new) | Database-backed work queue for Kotlin ↔ Python extraction |

See [Krithi Data Sourcing Quality Strategy](../01-requirements/krithi-data-sourcing/quality-strategy.md#7-database-schema-extensions) for full schema design rationale.

---

## 16. Future Extensions

Planned but out of scope for v1:
- Audio / notation synchronization
- Tala animation
- Line-level gamaka annotations
- Public user annotations
- `source_documents`, `extraction_runs`, `field_assertions` tables — Per-field provenance tracking (planned for medium-term, see [Strategy §7.2](../01-requirements/krithi-data-sourcing/quality-strategy.md#72-newmodified-tables))

---

## 17. Guiding Principle

> Sangita Grantha stores Carnatic music **as it is taught, learned, and performed** — not merely as text.

This document is **authoritative** for schema design.