-- 02__domain-tables.sql
-- Purpose: Primary Sangita Grantha domain tables

-- migrate:up
SET search_path TO public;

-- Users (Admin / Editor / Reviewer)
CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT UNIQUE,
  full_name TEXT NOT NULL,
  display_name TEXT,
  password_hash TEXT, -- if using local auth; can be null if external identity provider is used
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

-- Role assignments (RBAC)
CREATE TABLE IF NOT EXISTS role_assignments (
  user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  role_code TEXT NOT NULL REFERENCES roles (code) ON DELETE CASCADE,
  -- scope: for now, global; can add per-domain scoping later if required
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  PRIMARY KEY (user_id, role_code)
);

-- Composers
CREATE TABLE IF NOT EXISTS composers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,                  -- canonical name, e.g. "Tyagaraja"
  name_normalized TEXT NOT NULL,       -- ASCII-normalized for search
  birth_year INT,
  death_year INT,
  place TEXT,                          -- town/region
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT composers_name_normalized_uq UNIQUE (name_normalized)
);

-- Ragas
CREATE TABLE IF NOT EXISTS ragas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,                   -- e.g. "Bhairavi"
  name_normalized TEXT NOT NULL,        -- ASCII-normalized
  melakarta_number INT,                 -- null for janya ragas
  parent_raga_id UUID REFERENCES ragas (id) ON DELETE SET NULL,
  arohanam TEXT,                        -- structured as text for now; can refine later
  avarohanam TEXT,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT ragas_name_normalized_uq UNIQUE (name_normalized)
);

-- Talas
CREATE TABLE IF NOT EXISTS talas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,                -- e.g. "Adi", "Rupaka"
  name_normalized TEXT NOT NULL,
  anga_structure TEXT,               -- e.g. "Chatusra-jaati Triputa"
  beat_count INT,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT talas_name_normalized_uq UNIQUE (name_normalized)
);

-- Deities
CREATE TABLE IF NOT EXISTS deities (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,                 -- e.g. "Shiva", "Rama"
  name_normalized TEXT NOT NULL,
  description TEXT,
  -- optional finer-grained classification later
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT deities_name_normalized_uq UNIQUE (name_normalized)
);

-- Temples / Kshetrams
CREATE TABLE IF NOT EXISTS temples (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  name_normalized TEXT NOT NULL,
  city TEXT,
  state TEXT,
  country TEXT,
  primary_deity_id UUID REFERENCES deities (id) ON DELETE SET NULL,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  CONSTRAINT temples_name_city_uq UNIQUE (name_normalized, city, state, country)
);

-- Krithis
CREATE TABLE IF NOT EXISTS krithis (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title TEXT NOT NULL,                       -- canonical title
  incipit TEXT,                              -- first line / popular handle
  title_normalized TEXT NOT NULL,
  incipit_normalized TEXT,
  composer_id UUID NOT NULL REFERENCES composers (id) ON DELETE RESTRICT,
  primary_raga_id UUID REFERENCES ragas (id) ON DELETE SET NULL,
  tala_id UUID REFERENCES talas (id) ON DELETE SET NULL,
  deity_id UUID REFERENCES deities (id) ON DELETE SET NULL,
  temple_id UUID REFERENCES temples (id) ON DELETE SET NULL,
  primary_language language_code_enum NOT NULL,
  is_ragamalika BOOLEAN NOT NULL DEFAULT FALSE,

  workflow_state workflow_state_enum NOT NULL DEFAULT 'draft',
  -- Optional metadata
  sahitya_summary TEXT,                     -- short prose summary/meaning
  notes TEXT,

  created_by_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
  updated_by_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

-- Krithi ↔ Raga (supports Ragamalika)
CREATE TABLE IF NOT EXISTS krithi_ragas (
  krithi_id UUID NOT NULL REFERENCES krithis (id) ON DELETE CASCADE,
  raga_id UUID NOT NULL REFERENCES ragas (id) ON DELETE RESTRICT,
  order_index INT NOT NULL DEFAULT 0,            -- 0-based ordering in ragamalika
  section raga_section_enum,                     -- pallavi/anupallavi/charanam/other
  -- Potential for fine-grained mapping later (line offsets, etc.)
  notes TEXT,
  PRIMARY KEY (krithi_id, raga_id, order_index)
);

-- Lyric Variants (multilingual + patantharam)
CREATE TABLE IF NOT EXISTS krithi_lyric_variants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  krithi_id UUID NOT NULL REFERENCES krithis (id) ON DELETE CASCADE,

  language language_code_enum NOT NULL,          -- composition language or translation
  script script_code_enum NOT NULL,              -- script in which this text is stored
  transliteration_scheme TEXT,                   -- e.g. "IAST", "ISO-15919"
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,     -- main canonical lyric in that language/script

  variant_label TEXT,                            -- e.g. "Walajapet", "Veedhi-bhaga", etc.
  source_reference TEXT,                         -- which book/site/manuscript

  lyrics TEXT NOT NULL,                          -- full sahitya

  created_by_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
  updated_by_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

-- migrate:down
-- Drop in reverse dependency order
-- DROP TABLE IF EXISTS krithi_lyric_variants;
-- DROP TABLE IF EXISTS krithi_ragas;
-- DROP TABLE IF EXISTS krithis;
-- DROP TABLE IF EXISTS temples;
-- DROP TABLE IF EXISTS deities;
-- DROP TABLE IF EXISTS talas;
-- DROP TABLE IF EXISTS ragas;
-- DROP TABLE IF EXISTS composers;
-- DROP TABLE IF EXISTS role_assignments;
-- DROP TABLE IF EXISTS users;
