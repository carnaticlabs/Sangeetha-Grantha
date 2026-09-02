-- V61: TRACK-133 Bucket C (Group 3) — `mAdhavO mAM pAtu` Dashavatara Ragamalika metadata.
-- Muthuswami Dikshitar's Dashavatara Ragamalika (10 avatara stanzas, 10 ragas) is
-- currently mis-modelled: is_ragamalika=false with a single bogus raga "Alika"
-- (mis-parsed from the title word "mAlika") — a §6.2 violation. Set the flag and
-- replace the single mapping with the 10 ordered ragas.
--
-- Musicologist adjudication (TRACK-133 Round 3, 2026-09-02): sequence confirmed
-- nATa, gauLa, SrI, Arabhi, varALi, kEdAra, vasanta, suraTi, saurAshTra,
-- madhyamAvati (the earlier "SrI gauLa" was an honorific-prefix artifact; source
-- shows plain gauLa). Two ragas need an alias first (same raga, spelling variant):
--   * kEdAra ≡ Kedaram   (janya of 29; NOT Kedaragaula — distinct raga)
--   * saurAshTra ≡ saurAshTraM (janya of 17; -m-final spelling)
--
-- Ragas are resolved through the TRACK-136/137 identity fold (ragas.match_key ∪
-- raga_aliases.match_key); the block RAISES on any unresolved/ambiguous name so no
-- duplicate raga rows are ever created. order_index 1-based (V57 convention).
--
-- SCOPE = metadata only (is_ragamalika + krithi_ragas). Section text is populated
-- separately by the extraction pipeline.
--
-- Ref: application_documentation/01-requirements/domain-model.md (§6.2 ragamalika)

-- 1. Aliases so kEdAra / saurAshTra resolve to their canonical ragas (idempotent).
-- match_key is a GENERATED column (raga_match_key(alias)) — do not insert it.
INSERT INTO raga_aliases (raga_id, alias, alias_type, source, confidence)
SELECT r.id, v.alias, 'transliteration',
       'TRACK-133: mAdhavO Dashavatara ragamalika spelling variant', 'high'
FROM (VALUES
    ('kEdAra',     'kedaram'),
    ('saurAshTra', 'saurastram')
) AS v(alias, target_key)
JOIN ragas r ON r.match_key = v.target_key
WHERE NOT EXISTS (
    SELECT 1 FROM raga_aliases a WHERE a.match_key = raga_match_key(v.alias)
);

-- 2. Flag + 10 ordered krithi_ragas (replacing the bogus single mapping).
DO $$
DECLARE
    v_krithi uuid;
    d        RECORD;
    v_ids    uuid[];
BEGIN
    SELECT id INTO v_krithi FROM krithis WHERE title = 'mAdhavO mAM pAtu';
    IF v_krithi IS NULL THEN
        RAISE NOTICE 'V61: "mAdhavO mAM pAtu" not present — skipping (no-op)';
        RETURN;
    END IF;

    CREATE TEMP TABLE _dm(seq int PRIMARY KEY, name text NOT NULL, raga_id uuid) ON COMMIT DROP;
    INSERT INTO _dm(seq, name) VALUES
        (1,'nATa'), (2,'gauLa'),  (3,'SrI'),    (4,'Arabhi'),    (5,'varALi'),
        (6,'kEdAra'),(7,'vasanta'),(8,'suraTi'),(9,'saurAshTra'),(10,'madhyamAvati');

    FOR d IN SELECT seq, name FROM _dm ORDER BY seq LOOP
        SELECT array_agg(DISTINCT rid) INTO v_ids FROM (
            SELECT id      AS rid FROM ragas        WHERE match_key = raga_match_key(d.name)
            UNION
            SELECT raga_id AS rid FROM raga_aliases WHERE match_key = raga_match_key(d.name)
        ) c;
        IF v_ids IS NULL OR array_length(v_ids,1) = 0 THEN
            RAISE EXCEPTION 'V61: raga "%" (seq %) unresolved via match_key "%" — add the ragas row / alias first',
                d.name, d.seq, raga_match_key(d.name);
        ELSIF array_length(v_ids,1) > 1 THEN
            RAISE EXCEPTION 'V61: raga "%" (seq %) ambiguous -> % ids (%). Disambiguate before seeding.',
                d.name, d.seq, array_length(v_ids,1), v_ids;
        END IF;
        UPDATE _dm SET raga_id = v_ids[1] WHERE seq = d.seq;
    END LOOP;

    UPDATE krithis
       SET is_ragamalika  = true,
           primary_raga_id = (SELECT raga_id FROM _dm WHERE seq = 1),  -- headline = pallavi raga (nATa)
           updated_at      = now()
     WHERE id = v_krithi;

    DELETE FROM krithi_ragas WHERE krithi_id = v_krithi;

    INSERT INTO krithi_ragas (krithi_id, raga_id, order_index, section, notes)
    SELECT v_krithi, raga_id, seq, NULL,
           'TRACK-133: Dashavatara ragamalika stanza ' || seq || ' (' || name || ')'
    FROM _dm ORDER BY seq;

    INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
    VALUES ('krithis', v_krithi, 'UPDATE',
        jsonb_build_object('reason', 'TRACK-133 §6.2: mAdhavO mAM pAtu is the Dashavatara ragamalika; is_ragamalika=true, replaced bogus single-raga (Alika) mapping with 10 ordered ragas',
                           'is_ragamalika', true),
        jsonb_build_object('migration','V61','track','TRACK-133'));

    INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
    SELECT 'krithi_ragas', v_krithi, 'INSERT',
           jsonb_build_object('order_index', seq, 'raga', name, 'raga_id', raga_id),
           jsonb_build_object('migration','V61','track','TRACK-133')
    FROM _dm ORDER BY seq;

    IF (SELECT count(*) FROM krithi_ragas WHERE krithi_id = v_krithi) <> 10 THEN
        RAISE EXCEPTION 'V61: expected 10 krithi_ragas rows after seeding';
    END IF;

    RAISE NOTICE 'V61: "mAdhavO mAM pAtu" — is_ragamalika=true, 10 ordered ragas seeded';
END $$;
