-- TRACK-041: Structural Vote Log
-- Purpose: Record outcomes of cross-source structural voting for audit transparency.
-- When multiple sources contribute section structures for the same Krithi, the
-- StructuralVotingEngine determines the canonical structure and logs the decision.

SET search_path TO public;

CREATE TABLE IF NOT EXISTS structural_vote_log (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    krithi_id               UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,

    -- Voting timestamp
    voted_at                TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),

    -- Participating sources: [{sourceId, sourceName, tier, sectionStructure: [{type, order, label}]}]
    participating_sources   JSONB NOT NULL,

    -- The winning section structure: [{type, order, label}]
    consensus_structure     JSONB NOT NULL,

    -- How consensus was reached
    consensus_type          TEXT NOT NULL CHECK (consensus_type IN (
        'UNANIMOUS',            -- All sources agree
        'MAJORITY',             -- Most sources agree, minority dissents
        'AUTHORITY_OVERRIDE',   -- Tier 1 source overrides lower-tier disagreement
        'SINGLE_SOURCE',        -- Only one source available (no voting needed)
        'MANUAL'                -- Human reviewer made the decision
    )),

    -- Confidence in the consensus
    confidence              TEXT NOT NULL CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),

    -- Sources that disagreed: [{sourceId, sourceName, tier, sectionStructure}]
    dissenting_sources      JSONB NOT NULL DEFAULT '[]',

    -- Optional human review
    reviewer_id             UUID REFERENCES users(id),
    notes                   TEXT,

    -- Audit
    created_at              TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE INDEX IF NOT EXISTS idx_svl_krithi
    ON structural_vote_log(krithi_id);

CREATE INDEX IF NOT EXISTS idx_svl_voted_at
    ON structural_vote_log(voted_at DESC);

CREATE INDEX IF NOT EXISTS idx_svl_consensus_type
    ON structural_vote_log(consensus_type);

COMMENT ON TABLE structural_vote_log IS 'Audit trail for cross-source structural voting decisions per Krithi';
COMMENT ON COLUMN structural_vote_log.consensus_type IS 'How the canonical structure was determined: UNANIMOUS, MAJORITY, AUTHORITY_OVERRIDE, SINGLE_SOURCE, or MANUAL';

-- migrate:down
-- DROP INDEX IF EXISTS idx_svl_consensus_type;
-- DROP INDEX IF EXISTS idx_svl_voted_at;
-- DROP INDEX IF EXISTS idx_svl_krithi;
-- DROP TABLE IF EXISTS structural_vote_log;
