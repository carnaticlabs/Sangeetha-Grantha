-- 04__import-pipeline.sql
-- Purpose: Data ingestion and normalization pipeline tables

-- migrate:up
SET search_path TO public;

CREATE TABLE IF NOT EXISTS import_sources (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,                -- e.g. "karnatik.com"
  base_url TEXT,
  description TEXT,
  contact_info TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE TABLE IF NOT EXISTS imported_krithis (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  import_source_id UUID NOT NULL REFERENCES import_sources (id) ON DELETE CASCADE,
  source_key TEXT,                       -- source-specific identifier (URL/path/id)
  raw_title TEXT,
  raw_lyrics TEXT,
  raw_composer TEXT,
  raw_raga TEXT,
  raw_tala TEXT,
  raw_deity TEXT,
  raw_temple TEXT,
  raw_language TEXT,                     -- free-form from source

  parsed_payload JSONB,                  -- parsed/normalized intermediate structure
  import_status import_status_enum NOT NULL DEFAULT 'pending',

  mapped_krithi_id UUID REFERENCES krithis (id) ON DELETE SET NULL,
  reviewer_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
  reviewer_notes TEXT,
  reviewed_at TIMESTAMPTZ,

  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE INDEX IF NOT EXISTS idx_imported_krithis_source_status
  ON imported_krithis (import_source_id, import_status);

CREATE INDEX IF NOT EXISTS idx_imported_krithis_mapped_krithi
  ON imported_krithis (mapped_krithi_id);

-- migrate:down
-- DROP INDEX IF EXISTS idx_imported_krithis_mapped_krithi;
-- DROP INDEX IF EXISTS idx_imported_krithis_source_status;
-- DROP TABLE IF EXISTS imported_krithis;
-- DROP TABLE IF EXISTS import_sources;
