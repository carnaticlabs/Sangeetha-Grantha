-- V54: TRACK-136 / ADR-017 Phase 2 — raga_resolution_queue.
--
-- Unknown spellings and homonyms land here instead of minting a ragas row.
-- Pending-only uniqueness (N1): a plain UNIQUE(match_key, kind) would keep firing
-- after a row is attached/rejected, so a later re-import of a previously-rejected
-- name could never re-enqueue. Scope uniqueness to status = 'pending'.
--
-- Ref: application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md

CREATE TABLE raga_resolution_queue (
    id                 uuid PRIMARY KEY DEFAULT uuidv7(),
    raw_name           text NOT NULL,
    match_key          text NOT NULL,
    kind               text NOT NULL CHECK (kind IN ('unknown', 'ambiguous')),
    context            jsonb,          -- [{krithi_id, title, order_index, is_primary, source_url, extraction_run}]
    proposed_lakshana  jsonb,          -- confirm-new: parent/arohana/avarohana; ambiguous: candidate ids
    status             text NOT NULL DEFAULT 'pending'
                       CHECK (status IN ('pending', 'attached', 'created', 'disambiguated', 'rejected')),
    resolved_raga_id   uuid REFERENCES ragas(id),
    created_at         timestamptz NOT NULL DEFAULT now(),
    resolved_at        timestamptz
);

CREATE UNIQUE INDEX raga_resolution_queue_pending_uq
    ON raga_resolution_queue (match_key, kind) WHERE status = 'pending';

CREATE INDEX raga_resolution_queue_status_idx
    ON raga_resolution_queue (status, created_at DESC);

COMMENT ON TABLE raga_resolution_queue IS
    'TRACK-136: unknown / ambiguous raga names held for curator attach-alias, confirm-new, or disambiguate. No ingestion path inserts ragas.';
