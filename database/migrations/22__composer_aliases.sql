-- TRACK-031: Composer aliases for deduplication (alias -> canonical composer)
-- Purpose: Map short/alternate names (e.g. "Dikshitar") to canonical composer (e.g. "Muthuswami Dikshitar")

SET search_path TO public;

CREATE TABLE IF NOT EXISTS composer_aliases (
    alias_normalized TEXT NOT NULL PRIMARY KEY,
    composer_id UUID NOT NULL REFERENCES composers (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now())
);

CREATE INDEX idx_composer_aliases_composer_id ON composer_aliases (composer_id);
