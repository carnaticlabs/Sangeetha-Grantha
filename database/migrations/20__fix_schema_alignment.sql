-- 20__fix_schema_alignment.sql
-- Purpose: Align database schema with CoreTables.kt definitions by adding missing columns
-- This covers missing migrations from 14-17 (duplicate_candidates, quality scores, entity cache)

-- migrate:up
-- 1. Ensure imported_krithis has all expected columns
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS import_batch_id UUID REFERENCES import_batch(id) ON DELETE SET NULL;
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS duplicate_candidates JSONB;
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS resolution_data JSONB;
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS quality_score DECIMAL(3,2);
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS quality_tier VARCHAR(20);
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS completeness_score DECIMAL(3,2);
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS resolution_confidence DECIMAL(3,2);
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS source_quality DECIMAL(3,2);
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS validation_score DECIMAL(3,2);

-- 2. Ensure entity_resolution_cache exists as per CoreTables (Migration 17 version)
-- Using IF NOT EXISTS to avoid errors if partially applied
CREATE TABLE IF NOT EXISTS entity_resolution_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    raw_name TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    resolved_entity_id UUID NOT NULL,
    confidence INTEGER NOT NULL, -- CoreTables uses Int
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    UNIQUE(entity_type, normalized_name)
);

-- migrate:down
-- No-op as this is a fix-forward migration
