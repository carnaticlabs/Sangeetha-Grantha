-- V55: TRACK-136 / ADR-017 Phase 3 — provenance on ragas (mirrors raga_aliases).
--
-- Additive: nullable source, backfill, then NOT NULL. Confidence ships with a
-- default so existing inserts (Exposed create, R__seed_04) keep working.
--
-- Ref: application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md

ALTER TABLE ragas ADD COLUMN source text;
ALTER TABLE ragas ADD COLUMN confidence text NOT NULL DEFAULT 'high'
    CHECK (confidence IN ('high', 'medium', 'low'));

UPDATE ragas
   SET source = CASE
         WHEN melakarta_number IS NOT NULL THEN 'Wikipedia / Katapayadi melakarta'
         WHEN parent_raga_id IS NOT NULL THEN 'Wikipedia janya list'
         ELSE 'seed'
       END
 WHERE source IS NULL;

ALTER TABLE ragas ALTER COLUMN source SET DEFAULT 'seed';
ALTER TABLE ragas ALTER COLUMN source SET NOT NULL;

COMMENT ON COLUMN ragas.source IS
    'TRACK-136: authority for this identity row (seed list, curator, expert).';
COMMENT ON COLUMN ragas.confidence IS
    'TRACK-136: high / medium / low — mirrors raga_aliases.confidence.';
