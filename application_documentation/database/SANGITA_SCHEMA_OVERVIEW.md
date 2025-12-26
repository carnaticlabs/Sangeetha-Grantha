---
title: Sangita Grantha Schema Overview
status: Draft
version: 0.2
last_updated: 2025-12-21
owners:
  - Sangita Grantha Data/Backend Team
related_docs:
  - ./schema.md
  - ./migrations.md
  - ../requirements/domain-model.md
  - ../api/api-contract.md
---

# Sangita Grantha Schema Overview

This document summarizes the **core PostgreSQL schema** for Sangita
Grantha, focusing on Krithis, their metadata, lyric variants, tags,
imports, and audit logging.

Detailed DDL is defined in SQL migrations under `database/migrations/`.

- `01__baseline-schema-and-types.sql`
- `02__domain-tables.sql`
- `03__constraints-and-indexes.sql`
- `04__import-pipeline.sql`
- `05__sections-tags-sampradaya-temple-names.sql`

The schema is designed for:

- Correctness and referential integrity.
- Fast search (including lyrics substring search).
- Clean editorial workflows and provenance.
- Alignment with KMM shared DTOs in `modules/shared/domain`.

---

## 1. Enum Types

Defined in `01__baseline-schema-and-types.sql`:

### 1.1 `workflow_state_enum`

Editorial lifecycle of a Krithi:

- `draft`
- `in_review`
- `published`
- `archived`

### 1.2 `language_code_enum`

Language of composition or translation:

- `sa` – Sanskrit
- `ta` – Tamil
- `te` – Telugu
- `kn` – Kannada
- `ml` – Malayalam
- `hi` – Hindi
- `en` – English

### 1.3 `script_code_enum`

Script used to store the text:

- `devanagari`
- `tamil`
- `telugu`
- `kannada`
- `malayalam`
- `latin`

### 1.4 `raga_section_enum`

Optional mapping of ragas to Krithi sections (used by `krithi_ragas`):

- `pallavi`
- `anupallavi`
- `charanam`
- `other`

### 1.5 `import_status_enum`

Lifecycle of imported rows:

- `pending`
- `in_review`
- `mapped`
- `rejected`
- `discarded`

These enums are mirrored in KMM as:

- `WorkflowStateDto`
- `LanguageCodeDto`
- `ScriptCodeDto`
- `RagaSectionDto`
- `ImportStatusDto`

---

## 2. Foundational Tables

### 2.1 `roles`

RBAC roles for the admin system.

```sql
CREATE TABLE roles (
  code TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  capabilities JSONB NOT NULL DEFAULT '{}'::jsonb
);
```

- `code`: role identifier, e.g. `admin`, `editor`, `reviewer`.
- `capabilities`: JSONB of permissions/scopes.

### 2.2 `audit_log`

Immutable audit trail for all mutations.

```sql
CREATE TABLE audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_user_id UUID,
  actor_ip INET,
  action TEXT NOT NULL,
  entity_table TEXT NOT NULL,
  entity_id UUID,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  diff JSONB,
  metadata JSONB DEFAULT '{}'::jsonb
);
```

Indexes:

- `idx_audit_entity_time (entity_table, entity_id, changed_at DESC)`
- `idx_audit_actor (actor_user_id, changed_at DESC)`

All admin mutations **must** write an `audit_log` entry.

---

## 3. Identity & Roles

### 3.1 `users`

Admin/editor identities (v1; no public user accounts yet).

Columns (excerpt):

- `id` UUID PK
- `email` (UNIQUE, nullable)
- `full_name`
- `display_name`
- `password_hash` (nullable, for local auth)
- `is_active`
- `created_at`, `updated_at`

Index: `idx_users_email (email)`.

### 3.2 `role_assignments`

Assigns roles to users.

```sql
CREATE TABLE role_assignments (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_code TEXT NOT NULL REFERENCES roles(code) ON DELETE CASCADE,
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  PRIMARY KEY (user_id, role_code)
);
```

Index: `idx_role_assignments_user (user_id)`.

---

## 4. Core Musical Entities

These tables model the **canonical catalog** of composers, ragas, talas,
deities, temples, and Krithis.

### 4.1 `composers`

```sql
CREATE TABLE composers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  name_normalized TEXT NOT NULL,
  birth_year INT,
  death_year INT,
  place TEXT,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT composers_name_normalized_uq UNIQUE (name_normalized)
);
```

Index: `idx_composers_name_normalized (name_normalized)`.

### 4.2 `ragas`

```sql
CREATE TABLE ragas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  name_normalized TEXT NOT NULL,
  melakarta_number INT,
  parent_raga_id UUID REFERENCES ragas(id) ON DELETE SET NULL,
  arohanam TEXT,
  avarohanam TEXT,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT ragas_name_normalized_uq UNIQUE (name_normalized)
);
```

Index: `idx_ragas_name_normalized (name_normalized)`.

### 4.3 `talas`

```sql
CREATE TABLE talas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  name_normalized TEXT NOT NULL,
  anga_structure TEXT,
  beat_count INT,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT talas_name_normalized_uq UNIQUE (name_normalized)
);
```

Index: `idx_talas_name_normalized (name_normalized)`.

### 4.4 `deities`

```sql
CREATE TABLE deities (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  name_normalized TEXT NOT NULL,
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT deities_name_normalized_uq UNIQUE (name_normalized)
);
```

Index: `idx_deities_name_normalized (name_normalized)`.

### 4.5 `temples`

```sql
CREATE TABLE temples (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  name_normalized TEXT NOT NULL,
  city TEXT,
  state TEXT,
  country TEXT,
  primary_deity_id UUID REFERENCES deities(id) ON DELETE SET NULL,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT temples_name_city_uq UNIQUE (name_normalized, city, state, country)
);
```

Index: `idx_temples_name_city (name_normalized, city, state, country)`.

---

## 5. Krithis & Ragamalika

### 5.1 `krithis`

Central entity.

```sql
CREATE TABLE krithis (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title TEXT NOT NULL,
  incipit TEXT,
  title_normalized TEXT NOT NULL,
  incipit_normalized TEXT,
  composer_id UUID NOT NULL REFERENCES composers(id) ON DELETE RESTRICT,
  primary_raga_id UUID REFERENCES ragas(id) ON DELETE SET NULL,
  tala_id UUID REFERENCES talas(id) ON DELETE SET NULL,
  deity_id UUID REFERENCES deities(id) ON DELETE SET NULL,
  temple_id UUID REFERENCES temples(id) ON DELETE SET NULL,
  primary_language language_code_enum NOT NULL,
  is_ragamalika BOOLEAN NOT NULL DEFAULT FALSE,
  workflow_state workflow_state_enum NOT NULL DEFAULT 'draft',
  sahitya_summary TEXT,
  notes TEXT,
  created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);
```

Indexes:

- `idx_krithis_title_norm (title_normalized)`
- `idx_krithis_incipit_norm (incipit_normalized)`
- `idx_krithis_composer (composer_id)`
- `idx_krithis_primary_raga (primary_raga_id)`
- `idx_krithis_tala (tala_id)`
- `idx_krithis_deity (deity_id)`
- `idx_krithis_temple (temple_id)`
- `idx_krithis_workflow_state (workflow_state)`

Public APIs only expose `workflow_state = 'published'`.

### 5.2 `krithi_ragas`

Supports ragamalika.

```sql
CREATE TABLE krithi_ragas (
  krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
  raga_id UUID NOT NULL REFERENCES ragas(id) ON DELETE RESTRICT,
  order_index INT NOT NULL DEFAULT 0,
  section raga_section_enum,
  notes TEXT,
  PRIMARY KEY (krithi_id, raga_id, order_index)
);
```

Indexes:

- `idx_krithi_ragas_krithi (krithi_id, order_index)`
- `idx_krithi_ragas_raga (raga_id)`

For single-raga Krithis, there is typically one row matching
`primary_raga_id`. For ragamalika, multiple rows ordered by
`order_index`.

---

## 6. Lyric Variants & Sections

### 6.1 `krithi_lyric_variants`

```sql
CREATE TABLE krithi_lyric_variants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
  language language_code_enum NOT NULL,
  script script_code_enum NOT NULL,
  transliteration_scheme TEXT,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  variant_label TEXT,
  source_reference TEXT,
  lyrics TEXT NOT NULL,
  created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);
```

Indexes:

- `idx_krithi_lyrics_trgm` (GIN trigram index on `lyrics`)
- `idx_krithi_lyrics_lang_script (language, script, is_primary)`

This enables fast lyrics substring search and language/script filtered
queries.

### 6.2 Sections: `krithi_sections` & `krithi_lyric_sections`

Defined in `05__sections-tags-sampradaya-temple-names.sql`.

```sql
CREATE TABLE krithi_sections (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
  section_type TEXT NOT NULL CHECK (section_type IN (
    'PALLAVI','ANUPALLAVI','CHARANAM','CHITTASWARAM',
    'SWARA_SAHITYA','MADHYAMA_KALA','OTHER'
  )),
  order_index INT NOT NULL,
  label TEXT,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT krithi_sections_krithi_order_uq UNIQUE (krithi_id, order_index)
);

CREATE TABLE krithi_lyric_sections (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  lyric_variant_id UUID NOT NULL REFERENCES krithi_lyric_variants(id) ON DELETE CASCADE,
  section_id UUID NOT NULL REFERENCES krithi_sections(id) ON DELETE CASCADE,
  text TEXT NOT NULL,
  normalized_text TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT krithi_lyric_sections_variant_section_uq UNIQUE (lyric_variant_id, section_id)
);
```

Index: `idx_krithi_lyric_sections_norm_text (normalized_text)`.

These tables model structure (pallavi, anupallavi, charanams, etc.) and
per-variant section text.

---

## 7. Tags & Sampradayas

### 7.1 `tags` & `krithi_tags`

```sql
CREATE TABLE tags (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  category TEXT NOT NULL CHECK (category IN (
    'BHAVA','FESTIVAL','PHILOSOPHY','KSHETRA',
    'STOTRA_STYLE','NAYIKA_BHAVA','OTHER'
  )),
  slug TEXT NOT NULL UNIQUE,
  display_name_en TEXT NOT NULL,
  description_en TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE TABLE krithi_tags (
  krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
  tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  source TEXT NOT NULL DEFAULT 'manual',
  confidence INT CHECK (confidence BETWEEN 0 AND 100),
  PRIMARY KEY (krithi_id, tag_id)
);
```

Allows thematic classification (festival, bhava, philosophy, etc.).

### 7.2 `sampradayas`

```sql
CREATE TABLE sampradayas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL UNIQUE,
  type TEXT NOT NULL CHECK (type IN ('PATHANTARAM','BANI','SCHOOL')),
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);
```

`krithi_lyric_variants` has optional columns:

- `sampradaya_id UUID REFERENCES sampradayas(id)`
- `variant_label TEXT`

This supports patantharam/school attribution for lyric variants.

---

## 8. Temples & Multilingual Names

### 8.1 `temple_names`

```sql
CREATE TABLE temple_names (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  temple_id UUID NOT NULL REFERENCES temples(id) ON DELETE CASCADE,
  language_code language_code_enum NOT NULL,
  script_code script_code_enum NOT NULL,
  name TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT false,
  source TEXT NOT NULL DEFAULT 'manual',
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT temple_names_unique_name_per_lang
    UNIQUE (temple_id, language_code, name)
);

CREATE INDEX idx_temple_names_normalized
  ON temple_names (normalized_name);
```

This enables multilingual/alias naming and reliable ingestion matching
for temples/kṣetrams.

---

## 9. Import Pipeline

Defined in `04__import-pipeline.sql`.

### 9.1 `import_sources`

```sql
CREATE TABLE import_sources (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  base_url TEXT,
  description TEXT,
  contact_info TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);
```

### 9.2 `imported_krithis`

```sql
CREATE TABLE imported_krithis (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  import_source_id UUID NOT NULL REFERENCES import_sources(id) ON DELETE CASCADE,
  source_key TEXT,
  raw_title TEXT,
  raw_lyrics TEXT,
  raw_composer TEXT,
  raw_raga TEXT,
  raw_tala TEXT,
  raw_deity TEXT,
  raw_temple TEXT,
  raw_language TEXT,
  parsed_payload JSONB,
  import_status import_status_enum NOT NULL DEFAULT 'pending',
  mapped_krithi_id UUID REFERENCES krithis(id) ON DELETE SET NULL,
  reviewer_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  reviewer_notes TEXT,
  reviewed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);
```

Indexes:

- `idx_imported_krithis_source_status (import_source_id, import_status)`
- `idx_imported_krithis_mapped_krithi (mapped_krithi_id)`

Workflow:

1. Scrapers load raw data into `imported_krithis`.
2. Editors/reviewers map records to canonical Krithis and reference
   data.
3. No auto-publish; all promotions go through admin workflows.

---

## 10. DTO Alignment

KMM shared DTOs in `modules/shared/domain` mirror this schema:

- `ComposerDto`, `RagaDto`, `TalaDto`, `DeityDto`, `TempleDto`
- `KrithiDto`, `KrithiRagaDto`, `KrithiLyricVariantDto`
- `KrithiSectionDto`, `KrithiLyricSectionDto`
- `TagDto`, `KrithiTagDto`, `SampradayaDto`, `TempleNameDto`
- `ImportSourceDto`, `ImportedKrithiDto`

Any schema change that surfaces to clients must:

1. Add a SQL migration.
2. Update DTOs and, if needed, enums.
3. Update this document and the domain model (`requirements/domain-model.md`).

---

## 11. Future Extensions

Planned or possible extensions include:

- Richer section modelling (line-level metadata).
- Additional tag categories and relationships.
- Linkage to audio/notation resources.
- Public user accounts and saved lists.

All such changes should be introduced via new migrations and
accompanying documentation updates.
