-- corpus-data-fix: allow
-- TRACK-144: Additive repair after applied V61–V64. Does not edit those migrations.
--
-- 1. Rebuild idx_audit_entity_time before reading audits. On the populated database that
--    index returned 77 krithi audit rows against 1,877 on a sequential scan, which would
--    hide Isra Capu / Ad / Ekam / English compositions from the cohort below.
-- 2. Record a retrospective observation for V64's Catusra Ekam UPDATE. That statement
--    hardcoded beat_count and anga_structure as null and inserted an UPDATE even when it
--    changed no row. The prior values were not captured and are not reconstructed.
-- 3. Undo V61's Latin pallavi write on non-Latin Marugelaraa variants. V61 inserts
--    'marug(E)lar(A) O rAghav(A)' into every variant of that title and prepends it to
--    every lyrics column, with no composer or script predicate. New mutations capture
--    the row as read and the row as written.
-- 4. Invalidate document_embeddings for every identifiable V61/V63 composition whose
--    synchronized content_hash can conceal a stale vector: V63 taxonomy merges (from
--    their captured krithi audits) and V61's three title repairs when the title matches
--    one krithi. V64 already marked the 343 backfill compositions.

REINDEX INDEX idx_audit_entity_time;

DO $track144_v65$
DECLARE
    latin_pallavi constant text := 'marug(E)lar(A) O rAghav(A)';
    lyrics_prefix constant text := latin_pallavi || E'\n\n';
    fabricated audit_log%ROWTYPE;
    tala_row talas%ROWTYPE;
    variant_row krithi_lyric_variants%ROWTYPE;
    repaired_variant krithi_lyric_variants%ROWTYPE;
    section_row krithi_lyric_sections%ROWTYPE;
    marugelara_id uuid;
    variant_inventory jsonb;
    repaired_variants integer := 0;
    deleted_sections integer := 0;
    cohort_krithis integer := 0;
    invalidated_embeddings integer := 0;
    concealed integer := 0;
BEGIN
    --------------------------------------------------------------------------
    -- Part 1: Retrospective observation for the fabricated Catusra Ekam audit
    --------------------------------------------------------------------------
    FOR fabricated IN
        SELECT *
        FROM audit_log
        WHERE entity_table = 'talas'
          AND action = 'UPDATE'
          AND metadata->>'track' = 'TRACK-144'
          AND metadata->>'reason' = 'Populated beat_count and anga_structure for Catusra Ekam'
          AND jsonb_typeof(diff->'before'->'beat_count') = 'null'
          AND jsonb_typeof(diff->'before'->'anga_structure') = 'null'
    LOOP
        IF EXISTS (
            SELECT 1
            FROM audit_log existing
            WHERE existing.action = 'OBSERVE'
              AND existing.metadata->>'kind' = 'retrospective_observation'
              AND existing.metadata->>'subject_audit_id' = fabricated.id::text
        ) THEN
            CONTINUE;
        END IF;

        SELECT * INTO tala_row FROM talas WHERE id = fabricated.entity_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'TRACK-144: Catusra Ekam audit % points at a missing tala', fabricated.id;
        END IF;

        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
        VALUES (
            'talas',
            tala_row.id,
            'OBSERVE',
            jsonb_build_object(
                'observation', 'retrospective',
                'subject_audit_id', fabricated.id,
                'current_row', to_jsonb(tala_row),
                'prior_beat_count', 'unknown',
                'prior_anga_structure', 'unknown',
                'note', 'V64 inserted an UPDATE whose before-image hardcoded beat_count and anga_structure as null. That before-image was not read from the row. When the structure was already populated, V64 updated nothing and still wrote the audit. The previous values are unknown and are not reconstructed here.'
            ),
            jsonb_build_object(
                'track', 'TRACK-144',
                'kind', 'retrospective_observation',
                'subject_audit_id', fabricated.id,
                'method', 'catusra_ekam_audit_observation'
            )
        );
    END LOOP;

    --------------------------------------------------------------------------
    -- Part 2: V61 title repairs are unambiguous, or this migration stops
    --------------------------------------------------------------------------
    IF EXISTS (
        SELECT 1
        FROM krithis
        WHERE title IN ('Marugelaraa', 'Sri Narada Nada', 'dIna janAvana')
        GROUP BY title
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'TRACK-144: a V61 title repair matches more than one krithi';
    END IF;

    --------------------------------------------------------------------------
    -- Part 3: Remove Latin pallavi text V61 wrote into non-Latin variants
    --------------------------------------------------------------------------
    FOR variant_row IN
        SELECT v.*
        FROM krithi_lyric_variants v
        JOIN krithis k ON k.id = v.krithi_id
        WHERE k.title = 'Marugelaraa'
          AND v.script <> 'latin'
        FOR UPDATE OF v
    LOOP
        IF left(variant_row.lyrics, char_length(lyrics_prefix)) = lyrics_prefix THEN
            UPDATE krithi_lyric_variants
            SET lyrics = substr(lyrics, char_length(lyrics_prefix) + 1),
                updated_at = clock_timestamp()
            WHERE id = variant_row.id
            RETURNING * INTO repaired_variant;

            INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
            VALUES (
                'krithi_lyric_variants',
                variant_row.id,
                'UPDATE',
                jsonb_build_object('before', to_jsonb(variant_row), 'after', to_jsonb(repaired_variant)),
                jsonb_build_object(
                    'track', 'TRACK-144',
                    'reason', 'Removed the Latin pallavi prefix V61 prepended to a non-Latin Marugelaraa variant',
                    'method', 'v61_multilingual_recovery'
                )
            );
            repaired_variants := repaired_variants + 1;
        END IF;

        FOR section_row IN
            SELECT ls.*
            FROM krithi_lyric_sections ls
            JOIN krithi_sections s ON s.id = ls.section_id
            WHERE ls.lyric_variant_id = variant_row.id
              AND s.section_type = 'PALLAVI'
              AND s.krithi_id = variant_row.krithi_id
              AND ls.text = latin_pallavi
            FOR UPDATE OF ls
        LOOP
            DELETE FROM krithi_lyric_sections WHERE id = section_row.id;

            INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
            VALUES (
                'krithi_lyric_sections',
                section_row.id,
                'DELETE',
                jsonb_build_object('before', to_jsonb(section_row)),
                jsonb_build_object(
                    'track', 'TRACK-144',
                    'reason', 'Removed the Latin pallavi section V61 inserted into a non-Latin Marugelaraa variant',
                    'method', 'v61_multilingual_recovery'
                )
            );
            deleted_sections := deleted_sections + 1;
        END LOOP;
    END LOOP;

    SELECT k.id INTO marugelara_id
    FROM krithis k
    WHERE k.title = 'Marugelaraa';

    IF marugelara_id IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM audit_log
        WHERE action = 'OBSERVE'
          AND entity_table = 'krithis'
          AND entity_id = marugelara_id
          AND metadata->>'kind' = 'retrospective_observation'
          AND metadata->>'method' = 'v61_marugelara_script_scope'
    ) THEN
        SELECT COALESCE(
            jsonb_agg(
                jsonb_build_object(
                    'id', v.id,
                    'script', v.script,
                    'language', v.language,
                    'lyrics_start_with_latin_pallavi', left(v.lyrics, char_length(latin_pallavi)) = latin_pallavi
                )
                ORDER BY v.script, v.id
            ),
            '[]'::jsonb
        )
        INTO variant_inventory
        FROM krithi_lyric_variants v
        WHERE v.krithi_id = marugelara_id;

        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
        VALUES (
            'krithis',
            marugelara_id,
            'OBSERVE',
            jsonb_build_object(
                'observation', 'retrospective',
                'variants', variant_inventory,
                'non_latin_variants_repaired', repaired_variants,
                'non_latin_pallavi_sections_removed', deleted_sections,
                'note', 'V61 inserts the Latin pallavi into every lyric variant of title Marugelaraa and prepends it to every lyrics value. It does not restrict composer or script. Where a non-Latin variant still held that inserted text, this migration removed it and audited the row it read. Where only Latin variants exist, there is no non-Latin row to restore, and the pre-V61 lyric text is not reconstructed.'
            ),
            jsonb_build_object(
                'track', 'TRACK-144',
                'kind', 'retrospective_observation',
                'method', 'v61_marugelara_script_scope'
            )
        );
    END IF;

    --------------------------------------------------------------------------
    -- Part 4: Invalidate embeddings for identifiable V61/V63 compositions
    --------------------------------------------------------------------------
    CREATE TEMP TABLE tmp_track144_v65_cohort (
        krithi_id uuid PRIMARY KEY,
        reason text NOT NULL
    ) ON COMMIT DROP;

    INSERT INTO tmp_track144_v65_cohort (krithi_id, reason)
    SELECT DISTINCT a.entity_id, a.metadata->>'reason'
    FROM audit_log a
    WHERE a.entity_table = 'krithis'
      AND a.entity_id IS NOT NULL
      AND a.metadata->>'reason' IN (
          'Merged corrupt Isra Capu/Chapu to Misra Capu',
          'Merged corrupt Ad to Adi',
          'Standardized to canonical Catusra Ekam',
          'Standardized Nottuswara English meter to canonical Catusra Ekam'
      )
    ON CONFLICT (krithi_id) DO NOTHING;

    INSERT INTO tmp_track144_v65_cohort (krithi_id, reason)
    SELECT k.id, 'V61 catalogue repair for ' || k.title
    FROM krithis k
    WHERE k.title IN ('Marugelaraa', 'Sri Narada Nada', 'dIna janAvana')
    ON CONFLICT (krithi_id) DO NOTHING;

    WITH targets AS (
        SELECT de.id, de.document_id, de.content_hash AS old_hash
        FROM document_embeddings de
        JOIN search_documents sd ON sd.id = de.document_id
        JOIN tmp_track144_v65_cohort c ON c.krithi_id = sd.krithi_id
        WHERE de.content_hash IS DISTINCT FROM 'STALE_TRACK_144_NEEDS_REBUILD'
        FOR UPDATE OF de
    ),
    updated AS (
        UPDATE document_embeddings de
        SET content_hash = 'STALE_TRACK_144_NEEDS_REBUILD'
        FROM targets t
        WHERE de.id = t.id
        RETURNING de.id, de.document_id, de.content_hash AS new_hash
    ),
    audited AS (
        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
        SELECT
            'document_embeddings',
            u.id,
            'UPDATE',
            jsonb_build_object(
                'before', jsonb_build_object(
                    'id', t.id,
                    'document_id', t.document_id,
                    'content_hash', t.old_hash
                ),
                'after', jsonb_build_object(
                    'id', u.id,
                    'document_id', u.document_id,
                    'content_hash', u.new_hash
                )
            ),
            jsonb_build_object(
                'track', 'TRACK-144',
                'reason', 'Marked the embedding stale after a V61 or V63 catalogue change so a synchronized hash cannot conceal the old vector',
                'method', 'track144_embedding_cohort'
            )
        FROM updated u
        JOIN targets t ON t.id = u.id
        RETURNING 1
    )
    SELECT count(*) INTO invalidated_embeddings FROM audited;

    SELECT count(*) INTO cohort_krithis FROM tmp_track144_v65_cohort;

    SELECT count(*) INTO concealed
    FROM tmp_track144_v65_cohort c
    JOIN search_documents sd ON sd.krithi_id = c.krithi_id
    JOIN document_embeddings de ON de.document_id = sd.id
    WHERE de.content_hash IS DISTINCT FROM 'STALE_TRACK_144_NEEDS_REBUILD';

    IF concealed > 0 THEN
        RAISE EXCEPTION 'TRACK-144: % cohort embeddings still have synchronized hashes', concealed;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM krithis k
        JOIN search_documents sd ON sd.krithi_id = k.id
        JOIN document_embeddings de ON de.document_id = sd.id
        WHERE k.title IN ('nidhi cAla sukhamA', 'kana kana rucirA', 'cintaya citta')
          AND de.content_hash IS DISTINCT FROM 'STALE_TRACK_144_NEEDS_REBUILD'
          AND EXISTS (SELECT 1 FROM tmp_track144_v65_cohort c WHERE c.krithi_id = k.id)
    ) THEN
        RAISE EXCEPTION 'TRACK-144: a named taxonomy composition still has a synchronized embedding hash';
    END IF;

    RAISE NOTICE 'TRACK-144 V65: cohort krithis %, embeddings invalidated %, non-Latin variants repaired %, pallavi sections removed %',
        cohort_krithis, invalidated_embeddings, repaired_variants, deleted_sections;
END $track144_v65$;
