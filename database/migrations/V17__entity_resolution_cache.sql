-- TRACK-013: Database-backed Entity Resolution Cache
-- Migration 17: indexes + documentation for entity_resolution_cache.
--
-- The table itself is created earlier by V15; its column shape is reconciled by V30/V32.
-- This migration adds the lookup/invalidation indexes and column documentation.

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
