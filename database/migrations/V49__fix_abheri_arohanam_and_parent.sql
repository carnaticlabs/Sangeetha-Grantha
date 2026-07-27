-- V49: Correct Abheri's ārohaṇam and its parent melakarta
--
-- PROBLEM 1 — corrupted ārohaṇam
-- Stored as 'S M1 G2 M1 P P S': a doubled P, and no nishadam at all. Abheri is an
-- audava-sampurna janya whose ascent is 'S G2 M1 P N2 S'.
--
-- PROBLEM 2 — wrong parent, and the stored data proves it
-- Abheri is recorded as a janya of Natabhairavi (melakarta 20). That is impossible
-- given its own avarohanam:
--
--   Abheri avarohanam  : S N2 D2 P M1 G2 R2 S      <- D2
--   Natabhairavi (20)  : S R2 G2 M1 P D1 N2 S      <- D1
--   Kharaharapriya (22): S R2 G2 M1 P D2 N2 S      <- D2
--
-- A janya's swaras must be a subset of its parent's (domain-model §6.4). Abheri
-- carries D2, which melakarta 20 does not contain — so Natabhairavi cannot be its
-- parent. Melakarta 22 Kharaharapriyā contains every swara Abheri uses.
--
-- This is corroborated externally: Wikipedia's "List of Janya ragas" — the canonical
-- janya authority under ADR-016 — lists Abheri under melakarta 22 Kharaharapriya with
-- arohanam 'S G2 M1 P N2 S' and avarohanam 'S N2 D2 P M1 G2 R2 S'. The stored
-- avarohanam already matches that source exactly and is left untouched.
--
-- FIX
-- Correct the ārohaṇam and re-parent to melakarta 22. The 2 krithi links are left
-- untouched — they always pointed at the right raga; only its metadata was wrong.
--
-- Ref: application_documentation/02-architecture/decisions/ADR-016-raga-naming-authority.md

-- ORDERING NOTE — why this migration skips instead of failing on a fresh database.
-- Abheri and melakarta #22 Kharaharapriyā are both seeded by the repeatable
-- R__seed_04_raga_reference.sql, which Flyway runs AFTER every versioned migration.
-- On a fresh database neither row exists when this migration runs, so R__seed_04 —
-- which now seeds Abheri correctly at source — is authoritative and this migration
-- skips. It only has real work to do on an already-seeded existing database that
-- predates the fix. A hard failure here aborted the whole migrate and broke the
-- nightly compose stack.
DO $$
DECLARE
    abheri_count INT;
    mela22_count INT;
    v_parent_id  UUID;
    v_abheri_id  UUID;
BEGIN
    SELECT COUNT(*) INTO abheri_count FROM ragas WHERE name_normalized = 'abheri';
    SELECT COUNT(*) INTO mela22_count FROM ragas WHERE melakarta_number = 22;

    -- Fresh database: reference data not seeded yet — defer to R__seed_04.
    IF abheri_count = 0 OR mela22_count = 0 THEN
        RAISE NOTICE 'V49: reference data not present yet (abheri=%, mela22=%); correction deferred to R__seed_04, skipping.', abheri_count, mela22_count;
        RETURN;
    END IF;

    -- Present but ambiguous: fail loudly rather than write a silently-wrong parent.
    IF abheri_count <> 1 THEN
        RAISE EXCEPTION 'V49: expected exactly 1 raga with name_normalized=''abheri'', found %', abheri_count;
    END IF;
    IF mela22_count <> 1 THEN
        RAISE EXCEPTION 'V49: expected exactly 1 raga with melakarta_number=22, found %', mela22_count;
    END IF;

    SELECT id INTO v_parent_id FROM ragas WHERE melakarta_number = 22;

    UPDATE ragas
    SET arohanam       = 'S G2 M1 P N2 S',
        parent_raga_id = v_parent_id,
        updated_at     = NOW()
    WHERE name_normalized = 'abheri'
    RETURNING id INTO v_abheri_id;

    -- Audit log (every mutation must be recorded — see CLAUDE.md).
    INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
    VALUES ('ragas', v_abheri_id, 'CORRECT_RAGA_LAKSHANA',
        jsonb_build_object(
            'before', jsonb_build_object('arohanam', 'S M1 G2 M1 P P S',
                                         'parent', 'Natabhairavi (melakarta 20)'),
            'after',  jsonb_build_object('arohanam', 'S G2 M1 P N2 S',
                                         'parent', 'Kharaharapriya (melakarta 22)')
        ),
        jsonb_build_object(
            'reason',    'Arohanam was corrupted (doubled P, no nishadam); parent violated the janya subset rule (Abheri carries D2, melakarta 20 has D1)',
            'source',    'Wikipedia List of Janya ragas (ADR-016 canonical janya authority)',
            'migration', 'V49',
            'krithi_links_affected', 0
        )
    );
END $$;
