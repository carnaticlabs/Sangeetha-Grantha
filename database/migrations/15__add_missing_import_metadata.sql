-- 15__add_missing_import_metadata.sql
-- Purpose: Add missing metadata columns and entity resolution cache for Phase 3/4 bulk import
-- Alignment: application_documentation/01-requirements/features/bulk-import/01-strategy/csv-import-strategy.md

-- migrate:up
SET search_path TO public;

-- 1. Create Quality Tier Enum
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'quality_tier_enum') THEN
    CREATE TYPE quality_tier_enum AS ENUM ('excellent', 'good', 'fair', 'poor');
  END IF;
END$$;

-- 2. Enhance import_batch with metadata
-- Note: Using 'import_batch' (singular) as per harmonization decision
ALTER TABLE import_batch
    ADD COLUMN IF NOT EXISTS composer_context TEXT,
    ADD COLUMN IF NOT EXISTS error_summary JSONB;

-- 3. Enhance imported_krithis with Phase 3/4 metadata
-- These columns align with Section 6.2 of the strategy document
ALTER TABLE imported_krithis
    ADD COLUMN IF NOT EXISTS csv_row_number INT,
    ADD COLUMN IF NOT EXISTS csv_krithi_name TEXT,
    ADD COLUMN IF NOT EXISTS csv_raga TEXT,
    ADD COLUMN IF NOT EXISTS extraction_confidence DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS entity_mapping_confidence DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS quality_score DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS quality_tier quality_tier_enum,
    ADD COLUMN IF NOT EXISTS processing_errors JSONB;

-- 4. Create Entity Resolution Cache
-- Aligns with Section 6.3 of the strategy document
CREATE TABLE IF NOT EXISTS entity_resolution_cache (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  entity_type VARCHAR(50) NOT NULL, -- composer, raga, deity, temple
  raw_name TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  resolved_entity_id UUID NOT NULL,
  confidence DECIMAL(3,2) NOT NULL,
  resolution_method VARCHAR(50), -- exact, fuzzy, ai_assisted
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  UNIQUE(entity_type, normalized_name)
);

CREATE INDEX IF NOT EXISTS idx_entity_cache_type_name 
    ON entity_resolution_cache(entity_type, normalized_name);

-- migrate:down
DROP INDEX IF EXISTS idx_entity_cache_type_name;
DROP TABLE IF EXISTS entity_resolution_cache;

ALTER TABLE imported_krithis 
    DROP COLUMN IF EXISTS processing_errors,
    DROP COLUMN IF EXISTS quality_tier,
    DROP COLUMN IF EXISTS quality_score,
    DROP COLUMN IF EXISTS entity_mapping_confidence,
    DROP COLUMN IF EXISTS extraction_confidence,
    DROP COLUMN IF EXISTS csv_raga,
    DROP COLUMN IF EXISTS csv_krithi_name,
    DROP COLUMN IF EXISTS csv_row_number;

ALTER TABLE import_batch
    DROP COLUMN IF EXISTS error_summary,
    DROP COLUMN IF EXISTS composer_context;

-- Note: Enum types are generally not dropped in down migrations to avoid breaking dependencies.
