# Sangita Grantha – Schema Overview

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |
| **Related Documents** | - [Sangita Grantha PRD](../01-requirements/product-requirements-document.md)<br>- [Migrations](./migrations.md)<br>- [Domain Model](../01-requirements/domain-model.md)<br>- [API Contract](../03-api/api-contract.md) |

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
```
draft
in_review
published
archived
```
Controls editorial lifecycle.

---

### 3.2 `language_code_enum`
```
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
```
devanagari
tamil
telugu
kannada
malayalam
latin
```
---

### 3.4 `musical_form_enum`
```
KRITHI
VARNAM
SWARAJATHI
```
Defines structural and notation requirements.

---

### 3.5 `import_status_enum`
```
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
- `title`, `incipit`
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
```
PALLAVI
ANUPALLAVI
CHARANAM
MUKTAAYI_SWARAM
CHITTASWARAM
JATHI
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

---

### 10.2 `imported_krithis`

Stores raw imported records.

Rules:
- Never auto-published
- Must be reviewed and mapped
- Retains original raw text and metadata

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

## 12. Indexing & Performance

- Trigram indexes on lyric text
- Normalized name indexes on reference data
- Ordered indexes on sections and notation rows

Target:
- p95 lyric search < 300ms
- Deterministic rendering for mobile clients

---

## 13. DTO Alignment

Shared DTOs in `modules/shared/domain` mirror this schema:

- `ComposerDto`, `RagaDto`, `TalaDto`, `DeityDto`, `TempleDto`.
- `KrithiDto`, `KrithiRagaDto`, `KrithiLyricVariantDto`.
- `KrithiSectionDto`, `KrithiLyricSectionDto`.
- `KrithiNotationVariantDto`, `KrithiNotationRowDto`.
- `TagDto`, `KrithiTagDto`, `SampradayaDto`, `TempleNameDto`.

Any schema change MUST:
1. Add a migration
2. Update DTOs
3. Update this document

---

## 14. Future Extensions

Planned but out of scope for v1:
- Audio / notation synchronization
- Tala animation
- Line-level gamaka annotations
- Public user annotations

---

## 15. Guiding Principle

> Sangita Grantha stores Carnatic music **as it is taught, learned, and performed** — not merely as text.

This document is **authoritative** for schema design.
