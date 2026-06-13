-- 35__add_krithi_source_evidence_krithi_source_url_index.sql
-- Purpose: speed up duplicate/source lookups by (krithi_id, source_url)

SET search_path TO public;

CREATE INDEX IF NOT EXISTS idx_kse_krithi_source_url
  ON krithi_source_evidence (krithi_id, source_url);

