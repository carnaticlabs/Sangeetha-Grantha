-- Repeatable migration to set canonical musical forms (ADR-013 / TRACK-108).
-- Updates Syama Sastri's Swarajathi Ratnatrayam compositions to musical_form = 'SWARAJATHI'
-- and logs audit entries for any modified compositions.

DO $$
DECLARE
    r RECORD;
    v_actor_id UUID;
BEGIN
    -- Resolve system / admin user if exists for actor_user_id
    SELECT id INTO v_actor_id FROM users ORDER BY created_at ASC LIMIT 1;

    -- Update Syama Sastri's Swarajathis to musical_form = 'SWARAJATHI'
    FOR r IN
        SELECT k.id, k.title, k.musical_form, c.name AS composer_name
        FROM krithis k
        JOIN composers c ON k.composer_id = c.id
        WHERE (lower(c.name) LIKE '%syama%' OR lower(c.name) LIKE '%sastri%')
          AND lower(k.title) SIMILAR TO '%(kamakshi anudinamu|rave hima giri|kamakshi ni pada)%'
          AND k.musical_form <> 'SWARAJATHI'
    LOOP
        UPDATE krithis
        SET musical_form = 'SWARAJATHI',
            updated_at = NOW()
        WHERE id = r.id;

        INSERT INTO audit_log (entity_table, entity_id, actor_user_id, action, diff, metadata)
        VALUES (
            'krithis',
            r.id,
            v_actor_id,
            'UPDATE_MUSICAL_FORM',
            jsonb_build_object(
                'before', jsonb_build_object('musical_form', r.musical_form),
                'after',  jsonb_build_object('musical_form', 'SWARAJATHI')
            ),
            jsonb_build_object(
                'reason', 'Taxonomy correction: Syama Sastri Swarajathi Ratnatrayam classification',
                'title', r.title,
                'composer', r.composer_name,
                'migration', 'R__seed_08'
            )
        );
    END LOOP;
END $$;
