-- V48: Fix the "Gauri" melakarta mis-tag introduced by V40
--
-- PROBLEM
-- V40__seed_missing_trinity_ragas.sql seeded a raga under the assertion:
--     "8. Gauri (= Gourimanohari, melakarta #23)"
--     "Gauri is the common abbreviated name for melakarta #23 Gourimanohari."
-- That identification is incorrect. Gauri (Gowri) is a *janya* of melakarta #15
-- Māyāmāḻavagouḻai, not an abbreviation of melakarta #23 Gaurimanohari. They are
-- different ragas: Gaurimanohari uses R2/G2/D2, Gauri uses R1/G3/D1.
--
-- V40 therefore created a row named 'Gauri' carrying Gaurimanohari's melakarta
-- number (23) AND Gaurimanohari's scale. The import subsequently attached 5
-- krithis to it — those krithis are tagged 'Gauri' at source and do mean the
-- janya, so the krithi links are correct; only the raga's metadata is wrong.
--
-- Corroboration from the reference data itself: R__seed_04_raga_reference.sql
-- (sourced from Wikipedia "List of Janya ragas") already contains the correct
-- raga as 'Gowri' with arohanam 'S R1 M1 P N3 S' — the mela-15 scale. The scale
-- is self-verifying: R1 G3 M1 P D1 N3 is exactly Māyāmāḻavagouḻai.
--
-- FIX
-- Correct the 'Gauri' row's metadata in place. The 5 krithi links are left
-- untouched — they were always pointing at the right raga.
--
-- NOT IN SCOPE (deliberately deferred to TRACK-132, which adjudicates merges):
--   - 'Gauri' [5] and 'Gowri' [0] are the same raga under two spellings, and
--     will be duplicates once this migration aligns their metadata. Merging them
--     is a dedup decision, not a mis-tag fix.
--   - Melakarta #23 is itself split across 'Gourimanohari' [0] and
--     'Gauri Manohari' [2]; the latter has no melakarta number or scale.
--   - Melakarta #65 is split across 'Kalyāni' [40] and 'Mechakalyāni' [0].
--
-- Ref: conductor/tracks/TRACK-132-raga-deduplication-normalizer-fix.md

-- ORDERING NOTE — why this migration skips instead of failing on a fresh database.
-- The raga reference set, including melakarta #15 Māyāmāḻavagouḻai, is seeded by the
-- repeatable R__seed_04_raga_reference.sql, which Flyway runs AFTER every versioned
-- migration. On a fresh database melakarta #15 therefore does not exist when this
-- migration runs, so the reparent below cannot resolve. In that case the correction
-- is carried by R__seed_04 itself (which runs later and is authoritative), so this
-- migration skips cleanly. It only has real work to do on an already-seeded existing
-- database that predates the fix. A hard failure here aborted the whole migrate and
-- broke the nightly compose stack.
DO $$
DECLARE
    mela15_count INT;
    gauri_count  INT;
    v_parent_id  UUID;
    v_gauri_id   UUID;
BEGIN
    SELECT COUNT(*) INTO mela15_count FROM ragas WHERE melakarta_number = 15;
    SELECT COUNT(*) INTO gauri_count  FROM ragas WHERE name_normalized = 'gauri';

    -- Fresh database: reference data not seeded yet — defer to R__seed_04.
    IF mela15_count = 0 OR gauri_count = 0 THEN
        RAISE NOTICE 'V48: reference data not present yet (mela15=%, gauri=%); correction deferred to R__seed_04, skipping.', mela15_count, gauri_count;
        RETURN;
    END IF;

    -- Present but ambiguous: fail loudly rather than write a silently-wrong parent.
    IF mela15_count <> 1 THEN
        RAISE EXCEPTION 'V48: expected exactly 1 raga with melakarta_number=15, found %', mela15_count;
    END IF;
    IF gauri_count <> 1 THEN
        RAISE EXCEPTION 'V48: expected exactly 1 raga with name_normalized=''gauri'', found %', gauri_count;
    END IF;

    SELECT id INTO v_parent_id FROM ragas WHERE melakarta_number = 15;

    -- Re-parent 'Gauri' as a janya of melakarta #15 and correct its scale.
    -- Scale values match the Wikipedia-sourced 'Gowri' row in R__seed_04.
    UPDATE ragas
    SET melakarta_number = NULL,
        parent_raga_id   = v_parent_id,
        arohanam         = 'S R1 M1 P N3 S',
        avarohanam       = 'S N3 D1 P M1 G3 R1 S',
        updated_at       = NOW()
    WHERE name_normalized = 'gauri'
    RETURNING id INTO v_gauri_id;

    -- Audit log (every mutation must be recorded — see CLAUDE.md).
    INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
    VALUES ('ragas', v_gauri_id, 'CORRECT_MELAKARTA_MISTAG',
        jsonb_build_object(
            'before', jsonb_build_object('melakarta_number', 23, 'parent_raga_id', NULL,
                                         'arohanam', 'S R2 G2 M1 P D2 N3 S', 'avarohanam', 'S N3 D2 P M1 G2 R2 S'),
            'after',  jsonb_build_object('melakarta_number', NULL, 'parent_raga_id', v_parent_id,
                                         'arohanam', 'S R1 M1 P N3 S', 'avarohanam', 'S N3 D1 P M1 G3 R1 S')
        ),
        jsonb_build_object(
            'reason',    'V40 wrongly identified Gauri as melakarta #23 Gourimanohari; Gauri is a janya of melakarta #15 Mayamalavagowla',
            'migration', 'V48',
            'krithi_links_affected', 0
        )
    );
END $$;
