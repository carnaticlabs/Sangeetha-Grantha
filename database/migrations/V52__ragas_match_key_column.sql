-- V52: TRACK-136 / ADR-017 — generated STORED match_key on ragas.
-- No UNIQUE yet: that lands in V53 after mela_disambiguator is backfilled
-- (a name-only unique would collapse the Kalāvathi/Kalāvati homonym pair — D1).
--
-- Ref: application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md

ALTER TABLE ragas
    ADD COLUMN match_key text GENERATED ALWAYS AS (raga_match_key(name)) STORED NOT NULL;

COMMENT ON COLUMN ragas.match_key IS
    'TRACK-136: name-fold via raga_match_key(name). Identity is (match_key, mela_disambiguator).';
