-- V70__merge_poornalalita_into_poornalalitha.sql
-- TRACK-149: the catalogue held two raga rows for Tyagaraja's "kalugunA pada nIraja sEva",
-- Poornalalita (a janya of Mayamalavagowla) and Poornalalitha (a janya of Pavani). The raga
-- resolver could not tell them apart, which dropped Kalugunaa's raga link during a re-extract.
--
-- Owner decision (2026-11-11): the kriti is in Poornalalitha, a janya of Pavani. Sources the
-- owner supplied: https://thyagaraja-vaibhavam.blogspot.com/2007/05/thyagaraja-kriti-kalugunaa-raga-poorna.html
-- (names the raga "pUrNalalita" and gives no parent) and the shrutipad.in raga page
-- (Poornalalitha, janya of Pavani, S R1 M2 G1 R1 M2 P N3 D2 N3 S / S N3 D2 P M2 G1 R1 S).
-- karnatik.com lists the same kriti under a janya of Jhankaradhwani; that conflict is recorded
-- in the audit row below and in TRACK-149, not resolved here.
--
-- Moves every reference from Poornalalita to Poornalalitha and removes the Poornalalita row.
-- R__seed_04 no longer inserts it. Safe to run on a database where either row is absent.
-- Ref: application_documentation/04-database/schema.md

SET search_path TO public;

DO $$
DECLARE
    v_loser   uuid := (SELECT id FROM ragas WHERE name_normalized = 'poornalalita');
    v_keeper  uuid := (SELECT id FROM ragas WHERE name_normalized = 'poornalalitha');
    v_pavani  uuid := (SELECT id FROM ragas WHERE name_normalized = 'pavani');
BEGIN
    IF v_keeper IS NULL THEN
        RAISE NOTICE 'TRACK-149: Poornalalitha is absent, nothing to merge';
        RETURN;
    END IF;

    UPDATE ragas
    SET parent_raga_id = COALESCE(v_pavani, parent_raga_id), updated_at = NOW()
    WHERE id = v_keeper AND parent_raga_id IS DISTINCT FROM v_pavani AND v_pavani IS NOT NULL;

    IF v_loser IS NULL THEN
        RETURN;
    END IF;

    -- The queue FK has no delete action, so repoint it before the helper deletes the row.
    UPDATE raga_resolution_queue SET resolved_raga_id = v_keeper WHERE resolved_raga_id = v_loser;
    DELETE FROM raga_identity_keys WHERE raga_id = v_loser;

    PERFORM track132_merge_raga('Poornalalita', 'Poornalalitha');

    INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
    VALUES (
        'ragas', v_keeper, 'MERGE_DUPLICATE_RAGA_OWNER_DECISION',
        jsonb_build_object('loser_id', v_loser, 'loser_name', 'Poornalalita', 'keeper_name', 'Poornalalitha',
                           'keeper_parent', 'Pavani'),
        jsonb_build_object(
            'migration', 'V70',
            'owner_decision', '2026-11-11',
            'supporting_sources', jsonb_build_array(
                'https://thyagaraja-vaibhavam.blogspot.com/2007/05/thyagaraja-kriti-kalugunaa-raga-poorna.html',
                'https://shrutipad.in/ragas/carnatic/poornalalitha'),
            'conflicting_source', 'https://www.karnatik.com/c2384.shtml lists a janya of Jhankaradhwani'
        )
    );
END
$$;
