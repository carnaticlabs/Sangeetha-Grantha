-- TRACK-041: Source Authority Enhancement
-- Purpose: Add authority tier, supported formats, and composer affinity to import_sources
-- Enables field-level authority resolution and multi-format ingestion planning

SET search_path TO public;

-- Source tier: 1 (Scholarly/Published) → 5 (Individual Blogs)
ALTER TABLE import_sources
    ADD COLUMN IF NOT EXISTS source_tier INTEGER NOT NULL DEFAULT 5
        CHECK (source_tier BETWEEN 1 AND 5);

-- Supported formats: which document types this source provides
ALTER TABLE import_sources
    ADD COLUMN IF NOT EXISTS supported_formats TEXT[] NOT NULL DEFAULT '{HTML}';

-- Composer affinity: JSON map of composer_normalized_name → weight (0.0–1.0)
-- e.g. {"dikshitar": 1.0} means this source specialises in Dikshitar compositions
ALTER TABLE import_sources
    ADD COLUMN IF NOT EXISTS composer_affinity JSONB NOT NULL DEFAULT '{}';

-- Last time data was harvested from this source
ALTER TABLE import_sources
    ADD COLUMN IF NOT EXISTS last_harvested_at TIMESTAMPTZ;

-- Updated timestamp (was missing from original table)
ALTER TABLE import_sources
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now());

-- Index for tier-based queries (e.g. "show all Tier 1 sources")
CREATE INDEX IF NOT EXISTS idx_import_sources_tier
    ON import_sources (source_tier);

COMMENT ON COLUMN import_sources.source_tier IS 'Authority hierarchy: 1=Scholarly/Published, 2=Institutional, 3=Practitioner-Curated, 4=Community DB, 5=Individual Blog';
COMMENT ON COLUMN import_sources.supported_formats IS 'Array of supported document formats: HTML, PDF, DOCX, API, MANUAL';
COMMENT ON COLUMN import_sources.composer_affinity IS 'JSON map of normalised composer name to authority weight (0.0-1.0) for this source';

-- migrate:down
-- ALTER TABLE import_sources DROP COLUMN IF EXISTS updated_at;
-- ALTER TABLE import_sources DROP COLUMN IF EXISTS last_harvested_at;
-- ALTER TABLE import_sources DROP COLUMN IF EXISTS composer_affinity;
-- ALTER TABLE import_sources DROP COLUMN IF EXISTS supported_formats;
-- ALTER TABLE import_sources DROP COLUMN IF EXISTS source_tier;
-- DROP INDEX IF EXISTS idx_import_sources_tier;
