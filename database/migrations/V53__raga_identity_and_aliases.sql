-- V53: TRACK-136 / ADR-017 Phase 1 — mela-qualified identity, aliases, relations.
--
-- (a) ragas.mela_disambiguator (trigger-maintained, NEVER NULL — sentinel 0)
--     + UNIQUE(match_key, mela_disambiguator) = the R__seed_04 ON CONFLICT target.
-- (b) raga_aliases — same-identity surface forms with provenance.
-- (c) raga_relations — distinct-scale nomenclature pairs (from < to).
-- (d) raga_identity_keys — union of raga + differing-alias keys; PK is the guardrail.
-- (e) ragas.name_normalized UNIQUE dropped (display/search only; idx kept).
--
-- Ref: application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md

-- ---------------------------------------------------------------------------
-- (a) mela_disambiguator + BEFORE trigger (N2) so UNIQUE / ON CONFLICT see the
--     parent mela, not the 0 default.
-- ---------------------------------------------------------------------------

ALTER TABLE ragas
    ADD COLUMN mela_disambiguator int NOT NULL DEFAULT 0;

COMMENT ON COLUMN ragas.mela_disambiguator IS
    'TRACK-136: own melakarta_number, else parent''s, else 0 (unresolved). Part of the identity key.';

CREATE OR REPLACE FUNCTION ragas_set_mela_disambiguator()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.mela_disambiguator := COALESCE(
        NEW.melakarta_number,
        (SELECT p.melakarta_number FROM ragas p WHERE p.id = NEW.parent_raga_id),
        0
    );
    RETURN NEW;
END;
$$;

CREATE TRIGGER ragas_mela_disambiguator_tg
    BEFORE INSERT OR UPDATE ON ragas
    FOR EACH ROW
    EXECUTE FUNCTION ragas_set_mela_disambiguator();

-- Backfill existing rows (fires the BEFORE trigger).
UPDATE ragas SET updated_at = updated_at;

-- TRACK-132 residue the identity key cannot catch, cleared before the UNIQUE.
-- 'Sri', 'Shree' and 'SrI rAgaM' are one raga (Śrī, janya of Kharaharapriyā 22)
-- but fold to THREE different match_keys — sri / shree / ragam (the last dropped
-- its name token, keeping only the descriptor), so match_key never merges them.
-- Musicologist ruling 2026-08-30: keeper 'Sri', vakra avarohanam with D2. This
-- must run here: on an existing DB the preflight ASSERT below would otherwise trip
-- on the Shree/Sri (match_key=sri, mela=22) pair. track132_merge_raga (V50)
-- repoints krithi_ragas + primary_raga_id, re-parents children, and audits.
-- No-op on a fresh reset (keeper not seeded until R__seed_04) — the seed there
-- already ships the single correct 'Sri' row.
SELECT track132_merge_raga('Shree', 'Sri');
SELECT track132_merge_raga('SrI rAgaM', 'Sri');
UPDATE ragas
   SET avarohanam = 'S N2 P D2 N2 P M1 R2 G2 R2 S', updated_at = NOW()
 WHERE name = 'Sri'
   AND avarohanam IS DISTINCT FROM 'S N2 P D2 N2 P M1 R2 G2 R2 S';

-- N3: preflight ASSERT — refuse UNIQUE if any (match_key, mela) pair is shared,
-- sentinel 0 included. Forces curation of leftover same-key orphans.
DO $$
DECLARE
    rec record;
BEGIN
    FOR rec IN
        SELECT match_key,
               mela_disambiguator,
               count(*) AS n,
               string_agg(name, ', ' ORDER BY name) AS names
          FROM ragas
         GROUP BY match_key, mela_disambiguator
        HAVING count(*) > 1
    LOOP
        RAISE EXCEPTION
            'TRACK-136 P1.4: duplicate identity (match_key=%, mela_disambiguator=%): % (n=%)',
            rec.match_key, rec.mela_disambiguator, rec.names, rec.n;
    END LOOP;
END;
$$;

ALTER TABLE ragas
    ADD CONSTRAINT ragas_identity_uq UNIQUE (match_key, mela_disambiguator);

-- ---------------------------------------------------------------------------
-- (b) raga_aliases
-- ---------------------------------------------------------------------------

CREATE TABLE raga_aliases (
    id          uuid PRIMARY KEY DEFAULT uuidv7(),
    raga_id     uuid NOT NULL REFERENCES ragas(id) ON DELETE CASCADE,
    alias       text NOT NULL,
    match_key   text GENERATED ALWAYS AS (raga_match_key(alias)) STORED NOT NULL,
    alias_type  text NOT NULL CHECK (alias_type IN
                    ('transliteration', 'nomenclature', 'common', 'historical')),
    tradition   text,
    source      text NOT NULL,
    confidence  text NOT NULL DEFAULT 'high' CHECK (confidence IN ('high', 'medium', 'low')),
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT raga_aliases_raga_alias_uq UNIQUE (raga_id, alias)
);

CREATE INDEX raga_aliases_raga_id_idx ON raga_aliases (raga_id);
CREATE INDEX raga_aliases_match_key_idx ON raga_aliases (match_key);

COMMENT ON TABLE raga_aliases IS
    'TRACK-136: same-identity surface forms of a raga (spellings; same-scale tradition names).';

-- ---------------------------------------------------------------------------
-- (c) raga_relations — distinct-scale nomenclature equivalence (D2)
-- ---------------------------------------------------------------------------

CREATE TABLE raga_relations (
    from_raga_id uuid NOT NULL REFERENCES ragas(id) ON DELETE CASCADE,
    to_raga_id   uuid NOT NULL REFERENCES ragas(id) ON DELETE CASCADE,
    relation     text NOT NULL CHECK (relation IN ('nomenclature_equivalent')),
    source       text NOT NULL,
    CHECK (from_raga_id < to_raga_id),
    PRIMARY KEY (from_raga_id, to_raga_id, relation)
);

COMMENT ON TABLE raga_relations IS
    'TRACK-136: links between distinct identities (e.g. sampurna mela ↔ asampurna raganga).';

-- ---------------------------------------------------------------------------
-- (d) raga_identity_keys — union UNIQUE (D3)
-- ---------------------------------------------------------------------------

CREATE TABLE raga_identity_keys (
    match_key          text NOT NULL,
    mela_disambiguator int  NOT NULL,
    raga_id            uuid NOT NULL REFERENCES ragas(id) ON DELETE CASCADE,
    PRIMARY KEY (match_key, mela_disambiguator)
);

CREATE INDEX raga_identity_keys_raga_id_idx ON raga_identity_keys (raga_id);

COMMENT ON TABLE raga_identity_keys IS
    'TRACK-136: transactional union of ragas identity keys and differing alias keys.';

-- Own-key rows for every existing raga, before triggers attach (avoids double-insert).
INSERT INTO raga_identity_keys (match_key, mela_disambiguator, raga_id)
SELECT match_key, mela_disambiguator, id FROM ragas;

CREATE OR REPLACE FUNCTION raga_identity_keys_from_raga()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM raga_identity_keys WHERE raga_id = OLD.id;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE'
       AND (OLD.match_key IS DISTINCT FROM NEW.match_key
            OR OLD.mela_disambiguator IS DISTINCT FROM NEW.mela_disambiguator) THEN
        DELETE FROM raga_identity_keys
         WHERE raga_id = NEW.id
           AND match_key = OLD.match_key
           AND mela_disambiguator = OLD.mela_disambiguator;

        IF OLD.mela_disambiguator IS DISTINCT FROM NEW.mela_disambiguator THEN
            UPDATE raga_identity_keys
               SET mela_disambiguator = NEW.mela_disambiguator
             WHERE raga_id = NEW.id
               AND match_key IS DISTINCT FROM NEW.match_key;
        END IF;
    END IF;

    INSERT INTO raga_identity_keys (match_key, mela_disambiguator, raga_id)
    VALUES (NEW.match_key, NEW.mela_disambiguator, NEW.id)
    ON CONFLICT (match_key, mela_disambiguator) DO UPDATE
        SET raga_id = EXCLUDED.raga_id
        WHERE raga_identity_keys.raga_id = EXCLUDED.raga_id;

    IF NOT EXISTS (
        SELECT 1 FROM raga_identity_keys
         WHERE match_key = NEW.match_key
           AND mela_disambiguator = NEW.mela_disambiguator
           AND raga_id = NEW.id
    ) THEN
        RAISE EXCEPTION
            'TRACK-136: raga % identity (match_key=%, mela=%) collides with another raga',
            NEW.name, NEW.match_key, NEW.mela_disambiguator;
    END IF;

    -- Parent mela change: recompute children (BEFORE trigger reads the new parent mela).
    IF TG_OP = 'UPDATE' AND OLD.melakarta_number IS DISTINCT FROM NEW.melakarta_number THEN
        UPDATE ragas
           SET parent_raga_id = parent_raga_id
         WHERE parent_raga_id = NEW.id
           AND id <> NEW.id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER ragas_identity_keys_tg
    AFTER INSERT OR UPDATE ON ragas
    FOR EACH ROW
    EXECUTE FUNCTION raga_identity_keys_from_raga();

CREATE OR REPLACE FUNCTION raga_identity_keys_from_alias()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_own_key text;
    v_mela    int;
    v_raga    uuid;
BEGIN
    IF TG_OP = 'DELETE' THEN
        SELECT match_key, mela_disambiguator INTO v_own_key, v_mela
          FROM ragas WHERE id = OLD.raga_id;
        IF FOUND AND OLD.match_key IS DISTINCT FROM v_own_key THEN
            DELETE FROM raga_identity_keys
             WHERE match_key = OLD.match_key
               AND mela_disambiguator = v_mela
               AND raga_id = OLD.raga_id;
        END IF;
        RETURN OLD;
    END IF;

    v_raga := NEW.raga_id;
    SELECT match_key, mela_disambiguator INTO v_own_key, v_mela
      FROM ragas WHERE id = v_raga;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'TRACK-136: alias % references missing raga %', NEW.alias, v_raga;
    END IF;

    IF TG_OP = 'UPDATE'
       AND (OLD.match_key IS DISTINCT FROM NEW.match_key
            OR OLD.raga_id IS DISTINCT FROM NEW.raga_id) THEN
        DECLARE
            v_old_own text;
            v_old_mela int;
        BEGIN
            SELECT match_key, mela_disambiguator INTO v_old_own, v_old_mela
              FROM ragas WHERE id = OLD.raga_id;
            IF FOUND AND OLD.match_key IS DISTINCT FROM v_old_own THEN
                DELETE FROM raga_identity_keys
                 WHERE match_key = OLD.match_key
                   AND mela_disambiguator = v_old_mela
                   AND raga_id = OLD.raga_id;
            END IF;
        END;
    END IF;

    -- Alias whose (match_key, mela) equals its own raga's identity: no extra row.
    IF NEW.match_key IS NOT DISTINCT FROM v_own_key THEN
        RETURN NEW;
    END IF;

    INSERT INTO raga_identity_keys (match_key, mela_disambiguator, raga_id)
    VALUES (NEW.match_key, v_mela, v_raga)
    ON CONFLICT (match_key, mela_disambiguator) DO UPDATE
        SET raga_id = EXCLUDED.raga_id
        WHERE raga_identity_keys.raga_id = EXCLUDED.raga_id;

    IF NOT EXISTS (
        SELECT 1 FROM raga_identity_keys
         WHERE match_key = NEW.match_key
           AND mela_disambiguator = v_mela
           AND raga_id = v_raga
    ) THEN
        RAISE EXCEPTION
            'TRACK-136: alias % (match_key=%, mela=%) collides with a different raga identity',
            NEW.alias, NEW.match_key, v_mela;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER raga_aliases_identity_keys_tg
    AFTER INSERT OR UPDATE OR DELETE ON raga_aliases
    FOR EACH ROW
    EXECUTE FUNCTION raga_identity_keys_from_alias();

-- ---------------------------------------------------------------------------
-- (e) name_normalized is display/search only; drop the competing UNIQUE (§1.5).
--     idx_ragas_name_normalized is retained for lookup.
-- ---------------------------------------------------------------------------

ALTER TABLE ragas DROP CONSTRAINT ragas_name_normalized_uq;
