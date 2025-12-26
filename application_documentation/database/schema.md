---
title: Sangita Grantha Database Schema Overview
status: Draft
version: 0.1
last_updated: 2025-12-21
owners:
  - Sangita Grantha Data/Backend Team
related_docs:
  - ./SANGITA_SCHEMA_OVERVIEW.md
  - ./migrations.md
  - ../requirements/domain-model.md
  - ../diagrams/erd.md
---

# 1. Overview

Sangita Grantha uses PostgreSQL 15+ as the system of record for the
Carnatic Krithi catalog and related metadata:

- Composers, ragas, talas, deities, temples.
- Krithis, ragamalika mappings, lyric variants, and sections.
- Tags and thematic classification.
- Import sources and imported Krithis.
- Audit logging and roles.

The **authoritative description** of the schema is in
`SANGITA_SCHEMA_OVERVIEW.md`. This file provides a short index and
pointers for developers.

---

## 2. Core Migrations

Applied in order (see `database/migrations/`):

1. `01__baseline-schema-and-types.sql`  
   - Extensions (`pgcrypto`, `pg_trgm`).
   - Enums: `workflow_state_enum`, `language_code_enum`,
     `script_code_enum`, `raga_section_enum`, `import_status_enum`.
   - Foundational tables: `roles`, `audit_log`.

2. `02__domain-tables.sql`  
   - Identity: `users`, `role_assignments`.
   - Reference data: `composers`, `ragas`, `talas`, `deities`, `temples`.
   - Core domain: `krithis`, `krithi_ragas`, `krithi_lyric_variants`.

3. `03__constraints-and-indexes.sql`  
   - Audit indexes.
   - Search indexes for names and Krithi fields.
   - Trigram index for lyrics.

4. `04__import-pipeline.sql`  
   - `import_sources`, `imported_krithis`.

5. `05__sections-tags-sampradaya-temple-names.sql`  
   - `krithi_sections`, `krithi_lyric_sections`.
   - `tags`, `krithi_tags`.
   - `sampradayas` + columns on `krithi_lyric_variants`.
   - `temple_names`.

---

## 3. Entity Highlights

See `SANGITA_SCHEMA_OVERVIEW.md` for details on:

- Krithis and ragamalika structure.
- Lyric variants and sections.
- Import pipeline.
- Tags, sampradayas, and temple names.

---

## 4. Alignment with Shared DTOs

`modules/shared/domain` defines DTOs and enums that mirror this schema,
including:

- `ComposerDto`, `RagaDto`, `TalaDto`, `DeityDto`, `TempleDto`.
- `KrithiDto`, `KrithiRagaDto`, `KrithiLyricVariantDto`.
- `KrithiSectionDto`, `KrithiLyricSectionDto`.
- `TagDto`, `KrithiTagDto`, `SampradayaDto`, `TempleNameDto`.

Changes to schema must be reflected in DTOs and this documentation.
