-- TRACK-056: Language Variant Support for Extraction Queue
-- Purpose: Enable "enrich" extraction intent where a second PDF (e.g. Sanskrit)
--          is submitted as a language variant of an existing extraction (e.g. English).
--          Adds content_language, extraction_intent, and related_extraction_id columns.

SET search_path TO public;

-- ─── Extraction intent enum ──────────────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'extraction_intent') THEN
        CREATE TYPE extraction_intent AS ENUM (
            'PRIMARY',   -- Standard extraction: create new Krithis
            'ENRICH'     -- Variant enrichment: add lyric variants to existing Krithis
        );
    END IF;
END$$;

-- ─── Add columns to extraction_queue ─────────────────────────────────────────

-- Content language of the source document (ISO 639-1)
ALTER TABLE extraction_queue
    ADD COLUMN IF NOT EXISTS content_language TEXT;

-- Extraction intent: PRIMARY (default) or ENRICH
ALTER TABLE extraction_queue
    ADD COLUMN IF NOT EXISTS extraction_intent extraction_intent NOT NULL DEFAULT 'PRIMARY';

-- For ENRICH intent: link to the primary extraction this enriches
ALTER TABLE extraction_queue
    ADD COLUMN IF NOT EXISTS related_extraction_id UUID REFERENCES extraction_queue(id);

-- ─── Variant match results table ─────────────────────────────────────────────
-- Stores matching results between enrichment and primary extractions,
-- including confidence scores and match signals.

CREATE TABLE IF NOT EXISTS variant_match (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- The enrichment extraction that produced this match
    extraction_id           UUID NOT NULL REFERENCES extraction_queue(id),

    -- The Krithi this variant was matched to
    krithi_id               UUID NOT NULL REFERENCES krithis(id),

    -- Match quality
    confidence              DECIMAL(5,4) NOT NULL,  -- 0.0000–1.0000
    confidence_tier         TEXT NOT NULL CHECK (confidence_tier IN ('HIGH', 'MEDIUM', 'LOW')),

    -- Matching signals (what contributed to the confidence score)
    match_signals           JSONB NOT NULL DEFAULT '{}',
    -- e.g. {"titleMatch": 0.95, "ragaTalaMatch": 1.0, "pagePositionMatch": 0.8}

    -- Status
    match_status            TEXT NOT NULL DEFAULT 'PENDING'
                            CHECK (match_status IN ('PENDING', 'APPROVED', 'REJECTED', 'AUTO_APPROVED')),

    -- The raw extraction data for this variant
    extraction_payload      JSONB,

    -- Flags
    is_anomaly              BOOLEAN NOT NULL DEFAULT FALSE,  -- Krithi not in related extraction scope
    structure_mismatch      BOOLEAN NOT NULL DEFAULT FALSE,  -- Section count/order differs

    -- Review
    reviewed_by             UUID,
    reviewed_at             TIMESTAMPTZ,
    reviewer_notes          TEXT,

    -- Audit
    created_at              TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),

    -- Prevent duplicate matches for same extraction + Krithi
    UNIQUE (extraction_id, krithi_id)
);

-- Index for listing matches by extraction
CREATE INDEX IF NOT EXISTS idx_vm_extraction
    ON variant_match(extraction_id);

-- Index for finding matches by Krithi
CREATE INDEX IF NOT EXISTS idx_vm_krithi
    ON variant_match(krithi_id);

-- Index for pending review
CREATE INDEX IF NOT EXISTS idx_vm_pending
    ON variant_match(match_status)
    WHERE match_status = 'PENDING';

COMMENT ON TABLE variant_match IS 'TRACK-056: Stores variant matching results between enrichment extractions and existing Krithis.';
COMMENT ON COLUMN variant_match.match_signals IS 'JSON object with individual signal scores: titleMatch, ragaTalaMatch, pagePositionMatch, etc.';
COMMENT ON COLUMN variant_match.is_anomaly IS 'TRUE when the matched Krithi was NOT in the related (primary) extraction scope.';
COMMENT ON COLUMN variant_match.structure_mismatch IS 'TRUE when the variant has a different section structure than the primary.';

-- migrate:down
-- DROP INDEX IF EXISTS idx_vm_pending;
-- DROP INDEX IF EXISTS idx_vm_krithi;
-- DROP INDEX IF EXISTS idx_vm_extraction;
-- DROP TABLE IF EXISTS variant_match;
-- ALTER TABLE extraction_queue DROP COLUMN IF EXISTS related_extraction_id;
-- ALTER TABLE extraction_queue DROP COLUMN IF EXISTS extraction_intent;
-- ALTER TABLE extraction_queue DROP COLUMN IF EXISTS content_language;
-- DROP TYPE IF EXISTS extraction_intent;
