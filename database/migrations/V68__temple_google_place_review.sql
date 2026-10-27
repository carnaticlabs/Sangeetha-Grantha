-- V68__temple_google_place_review.sql
-- TRACK-154: reviewed Google place_id per temple, so the kshetra detail can open
-- Google Maps without the database storing Google-derived content.
--
-- Schema only. The reviewed placements are applied by R__seed_12_temple_place_review.sql.
-- Only the place_id is stored: no Google name, address, rating, photo or coordinate.
-- temples.latitude / temples.longitude stay for hand-entered or curator-measured
-- values and are never written from Google results.
-- Ref: application_documentation/04-database/schema.md

SET search_path TO public;

CREATE TYPE place_review_state_enum AS ENUM ('CONFIRMED', 'LOCALITY_ONLY', 'UNRESOLVED');

ALTER TABLE temples
    ADD COLUMN google_place_id TEXT,
    ADD COLUMN place_review_state place_review_state_enum,
    ADD COLUMN place_reviewed_by TEXT,
    ADD COLUMN place_reviewed_at TIMESTAMPTZ;

-- NULL review state reads as "not looked at": there is no fourth state.
ALTER TABLE temples
    ADD CONSTRAINT chk_temples_place_confirmed_has_id
        CHECK (place_review_state IS DISTINCT FROM 'CONFIRMED' OR google_place_id IS NOT NULL),
    ADD CONSTRAINT chk_temples_place_unresolved_has_no_id
        CHECK (place_review_state IS DISTINCT FROM 'UNRESOLVED' OR google_place_id IS NULL),
    ADD CONSTRAINT chk_temples_place_id_needs_state
        CHECK (google_place_id IS NULL OR place_review_state IS NOT NULL),
    ADD CONSTRAINT chk_temples_place_review_audit_fields
        CHECK (
            (place_review_state IS NULL
                AND place_reviewed_by IS NULL AND place_reviewed_at IS NULL)
            OR (place_review_state IS NOT NULL
                AND place_reviewed_by IS NOT NULL AND place_reviewed_at IS NOT NULL)
        ),
    -- Sub-shrines inherit their parent's place; they never hold one of their own.
    ADD CONSTRAINT chk_temples_subshrine_no_own_place
        CHECK (
            place_kind NOT IN ('SANNIDHI', 'MANDAPAM')
            OR (google_place_id IS NULL AND place_review_state IS NULL)
        );

-- No two CONFIRMED temples may point at the same Google place.
CREATE UNIQUE INDEX uq_temples_confirmed_google_place_id
    ON temples (google_place_id)
    WHERE place_review_state = 'CONFIRMED';

COMMENT ON COLUMN temples.google_place_id IS
    'Reviewed Google Places place_id (TRACK-154). The only Google-derived value we store; may go stale.';
COMMENT ON COLUMN temples.place_review_state IS
    'CONFIRMED = the place is this temple; LOCALITY_ONLY = Google only has the town; UNRESOLVED = no acceptable candidate; NULL = not reviewed. Only CONFIRMED is ever exposed.';
COMMENT ON COLUMN temples.place_reviewed_by IS
    'Approver of the place review, or system:TRACK-154:auto for automatic confirmations (curator sign-off is in the reviewed file).';
