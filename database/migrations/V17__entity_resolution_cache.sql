-- TRACK-013: Database-backed Entity Resolution Cache
-- Migration 17: Add entity_resolution_cache table for persistent caching

CREATE TABLE IF NOT EXISTS entity_resolution_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    raw_name TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    resolved_entity_id UUID NOT NULL,
    confidence INTEGER NOT NULL CHECK (confidence >= 0 AND confidence <= 100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    UNIQUE(entity_type, normalized_name)
);

-- Index for fast lookups by entity_type and normalized_name (primary use case)
CREATE INDEX idx_entity_cache_lookup
    ON entity_resolution_cache(entity_type, normalized_name);

-- Index for cache invalidation when entities are updated/deleted
CREATE INDEX idx_entity_cache_entity_id
    ON entity_resolution_cache(resolved_entity_id);

-- Comments for documentation
COMMENT ON TABLE entity_resolution_cache IS 'Persistent cache for entity resolution results to avoid repeated fuzzy matching';
COMMENT ON COLUMN entity_resolution_cache.entity_type IS 'Type of entity: composer, raga, or tala';
COMMENT ON COLUMN entity_resolution_cache.raw_name IS 'Original raw name from import';
COMMENT ON COLUMN entity_resolution_cache.normalized_name IS 'Normalized name used for matching (lowercase, trimmed, etc.)';
COMMENT ON COLUMN entity_resolution_cache.resolved_entity_id IS 'UUID of the resolved entity';
COMMENT ON COLUMN entity_resolution_cache.confidence IS 'Confidence score (0-100) of the resolution';
