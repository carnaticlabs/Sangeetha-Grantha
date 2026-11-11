-- Repeatable seed for TRACK-149: Tyagaraja Kshetra Pancharatnam reviewed memberships.
-- Successor to R__seed_11 (same membership mechanism: krithi_cycle_memberships + krithi_tags,
-- provenance-scoped conflict guards, system audit attribution).
-- Source of truth: track147-tyagaraja-application-manifest.json (10 owner-approved kshetra group claims,
-- recorded in track149-lead-decisions.json on 2026-11-03: Srirangam 3, Kovur 3, Lalgudi 4).
-- The Tiruvottiyur claim is a geography claim, not a group membership, and is NOT applied here.
-- No source gives a declared order inside a kshetra set, so sequence_order is the placeholder 0 and each row
-- records order_declared = false (and carries no order_convention, so the API reports the order basis as unspecified).
-- These sets are partial (3 of 5 Srirangam, 3 of 4 labelled Kovur, 4 of 5 Lalgudi).
-- Ref: application_documentation/01-requirements/features/tyagaraja-classification-master.md

-- 1. Ensure the three kshetra cycle tags exist
INSERT INTO tags (id, category, slug, display_name_en, description_en, created_at)
VALUES ('01920000-0000-7000-8000-00000000000d'::uuid, 'STOTRA_STYLE', 'tyagaraja-srirangam-pancharatnam', 'Tyagaraja Srirangam Pancharatnam', 'Kshetra Pancharatnam of Saint Tyagaraja associated with Srirangam (Ranganathaswamy Temple). Recorded from a public Music Academy journal listing that names five kritis; only three are linked here (partial set).', NOW())
ON CONFLICT (slug) DO UPDATE SET
    display_name_en = EXCLUDED.display_name_en,
    description_en = EXCLUDED.description_en
WHERE tags.display_name_en IS DISTINCT FROM EXCLUDED.display_name_en
   OR tags.description_en IS DISTINCT FROM EXCLUDED.description_en;

INSERT INTO tags (id, category, slug, display_name_en, description_en, created_at)
VALUES ('01920000-0000-7000-8000-00000000000e'::uuid, 'STOTRA_STYLE', 'tyagaraja-kovur-pancharatnam', 'Tyagaraja Kovur Pancharatnam', 'Kshetra Pancharatnam of Saint Tyagaraja associated with Kovur (Sundareswara). Recorded from a secondary translation compilation that labels four items; three are linked here (partial set; a Pancharatnam normally has five).', NOW())
ON CONFLICT (slug) DO UPDATE SET
    display_name_en = EXCLUDED.display_name_en,
    description_en = EXCLUDED.description_en
WHERE tags.display_name_en IS DISTINCT FROM EXCLUDED.display_name_en
   OR tags.description_en IS DISTINCT FROM EXCLUDED.description_en;

INSERT INTO tags (id, category, slug, display_name_en, description_en, created_at)
VALUES ('01920000-0000-7000-8000-00000000000f'::uuid, 'STOTRA_STYLE', 'tyagaraja-lalgudi-pancharatnam', 'Tyagaraja Lalgudi Pancharatnam', 'Kshetra Pancharatnam of Saint Tyagaraja associated with Lalgudi (Saptarishisvara). Recorded from a public Music Academy journal listing that names five kritis; only four are linked here (partial set).', NOW())
ON CONFLICT (slug) DO UPDATE SET
    display_name_en = EXCLUDED.display_name_en,
    description_en = EXCLUDED.description_en
WHERE tags.display_name_en IS DISTINCT FROM EXCLUDED.display_name_en
   OR tags.description_en IS DISTINCT FROM EXCLUDED.description_en;

-- 2. Apply approved memberships with conflict protection and system audit logging
DO $$
DECLARE
    v_composer_id CONSTANT uuid := 'b1bdccd8-8cb1-4a14-a9e6-7ffc843cd7b6'::uuid;
    v_actor_meta CONSTANT jsonb := jsonb_build_object(
        'actor', 'system:TRACK-149',
        'migration', 'R__seed_13_tyagaraja_kshetra_pancharatnam_memberships.sql',
        'provenance_owner', 'TRACK-149'
    );
    rec RECORD;
    v_existing_cm RECORD;
    v_existing_tag_source text;
    v_existing_tag_conf int;
    v_cm_id uuid;
    v_present int;
BEGIN
    DROP TABLE IF EXISTS _t149_approved;
    CREATE TEMP TABLE _t149_approved (
        tag_id uuid, krithi_id uuid, claim_id text, witness_id text, title text, kshetra text, note text
    ) ON COMMIT DROP;

    INSERT INTO _t149_approved (tag_id, krithi_id, claim_id, witness_id, title, kshetra, note) VALUES
            ('01920000-0000-7000-8000-00000000000e'::uuid, '1d3c167b-d5ae-4f4b-8e55-8f11ab9c39e4'::uuid, 'tyagaraja:krithi:1d3c167b-d5ae-4f4b-8e55-8f11ab9c39e4:group', 'hindupedia-thyagaraja-compilation', 'Ivasudha Neevanti', 'Kovur', 'Labelled as a Kovur Pancharatna item in a secondary translation compilation.'),
            ('01920000-0000-7000-8000-00000000000e'::uuid, 'e630e8f7-f8da-45b3-8e8b-fce0f91a74e9'::uuid, 'tyagaraja:krithi:e630e8f7-f8da-45b3-8e8b-fce0f91a74e9:group', 'hindupedia-thyagaraja-compilation', 'Kori Sevimpa', 'Kovur', 'Labelled as a Kovur Pancharatna item in a secondary translation compilation.'),
            ('01920000-0000-7000-8000-00000000000e'::uuid, '8300ca9e-eca0-4d3a-8a93-0c54c3715baf'::uuid, 'tyagaraja:krithi:8300ca9e-eca0-4d3a-8a93-0c54c3715baf:group', 'hindupedia-thyagaraja-compilation', 'nammi vaccina', 'Kovur', 'Labelled as a Kovur Pancharatna item in a secondary translation compilation.'),
            ('01920000-0000-7000-8000-00000000000f'::uuid, '271a1479-c84d-4c22-af76-040f659fce2b'::uuid, 'tyagaraja:krithi:271a1479-c84d-4c22-af76-040f659fce2b:group', 'ma:jma-vol69-1998', 'gati nIvani', 'Lalgudi', 'Named in the Lalgudi Pancharatnam list.'),
            ('01920000-0000-7000-8000-00000000000f'::uuid, '550a03c3-991c-435f-b1b5-439c16e98183'::uuid, 'tyagaraja:krithi:550a03c3-991c-435f-b1b5-439c16e98183:group', 'ma:jma-vol69-1998', 'ISa pAhi', 'Lalgudi', 'Named in the Lalgudi Pancharatnam list.'),
            ('01920000-0000-7000-8000-00000000000f'::uuid, '4215e9b5-17dd-4730-a140-55d48014b034'::uuid, 'tyagaraja:krithi:4215e9b5-17dd-4730-a140-55d48014b034:group', 'ma:jma-vol69-1998', 'Lalitae Sri Pravriddhae', 'Lalgudi', 'Named in the Lalgudi Pancharatnam list.'),
            ('01920000-0000-7000-8000-00000000000f'::uuid, '7914b699-b204-42b1-9057-72f4ff5db6d4'::uuid, 'tyagaraja:krithi:7914b699-b204-42b1-9057-72f4ff5db6d4:group', 'ma:jma-vol69-1998', 'Mahita Pravrddha', 'Lalgudi', 'Named in the Lalgudi Pancharatnam list.'),
            ('01920000-0000-7000-8000-00000000000d'::uuid, '0931f98a-64fa-403f-967c-941677395832'::uuid, 'tyagaraja:krithi:0931f98a-64fa-403f-967c-941677395832:group', 'ma:jma-vol54-1983', 'karuNa jUDavayya', 'Srirangam', 'Named in the Srirangam Pancharatnam list.'),
            ('01920000-0000-7000-8000-00000000000d'::uuid, 'c262739c-e7cc-492d-b248-3775fec24bfe'::uuid, 'tyagaraja:krithi:c262739c-e7cc-492d-b248-3775fec24bfe:group', 'ma:jma-vol54-1983', 'O Ranga Saayi', 'Srirangam', 'Named in the Srirangam Pancharatnam list.'),
            ('01920000-0000-7000-8000-00000000000d'::uuid, 'c5aee074-7a6b-4fd3-83ea-19fd6d9083eb'::uuid, 'tyagaraja:krithi:c5aee074-7a6b-4fd3-83ea-19fd6d9083eb:group', 'ma:jma-vol54-1983', 'rAju veDale', 'Srirangam', 'Named in the Srirangam Pancharatnam list.');

    SELECT count(*) INTO v_present FROM _t149_approved a JOIN krithis k ON k.id = a.krithi_id;
    IF v_present < 10 THEN
        RAISE NOTICE 'TRACK-149: only % of 10 approved kshetra krithis exist in this catalogue; the rest are skipped', v_present;
    END IF;

    -- Abort if any target canonical krithi is bound to a different composer
    FOR rec IN
        SELECT k.id, k.composer_id
        FROM krithis k
        WHERE k.id IN (SELECT krithi_id FROM _t149_approved)
          AND k.composer_id <> v_composer_id
    LOOP
        RAISE EXCEPTION 'TRACK-149: canonical krithi % belongs to unexpected composer % (expected %)',
            rec.id, rec.composer_id, v_composer_id;
    END LOOP;

    -- Remove stale TRACK-149 memberships on these tags that are no longer in the approved set
    FOR rec IN
        DELETE FROM krithi_cycle_memberships m
        WHERE m.tag_id IN (SELECT DISTINCT tag_id FROM _t149_approved)
          AND m.discriminative_attributes->>'provenance_owner' = 'TRACK-149'
          AND NOT EXISTS (SELECT 1 FROM _t149_approved a WHERE a.krithi_id = m.krithi_id AND a.tag_id = m.tag_id)
        RETURNING m.id, m.krithi_id, m.tag_id, m.discriminative_attributes
    LOOP
        INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
        VALUES (gen_random_uuid(), 'krithi_cycle_memberships', rec.id, NULL, 'DELETE_STALE_CYCLE_MEMBERSHIP',
            jsonb_build_object('krithi_id', rec.krithi_id, 'tag_id', rec.tag_id,
                               'previous_attributes', rec.discriminative_attributes),
            v_actor_meta);
    END LOOP;

    FOR rec IN
        SELECT a.tag_id, a.krithi_id, a.kshetra,
               jsonb_build_object(
                   'provenance_owner', 'TRACK-149',
                   'review_state', 'reviewed',
                   'claim_id', a.claim_id,
                   'witness_id', a.witness_id,
                   'kshetra', a.kshetra,
                   'order_declared', false,
                   'set_completeness', 'partial',
                   'mudra', 'Tyagaraja',
                   'approval', jsonb_build_object(
                       'decision', 'approve',
                       'reviewer', 'Seshadri',
                       'accepted_at', '2026-11-03',
                       'note', a.note
                   )
               ) AS attrs
        FROM _t149_approved a
        JOIN krithis k ON k.id = a.krithi_id AND k.composer_id = v_composer_id
        ORDER BY a.kshetra, a.title
    LOOP
        SELECT id, sequence_order, role, axis_value, discriminative_attributes
          INTO v_existing_cm
        FROM krithi_cycle_memberships
        WHERE krithi_id = rec.krithi_id AND tag_id = rec.tag_id;

        IF FOUND THEN
            -- Abort on conflict with an independently owned membership row
            IF COALESCE(v_existing_cm.discriminative_attributes->>'provenance_owner', '') <> 'TRACK-149' THEN
                RAISE EXCEPTION 'TRACK-149: conflicting independently reviewed krithi_cycle_memberships row for krithi %',
                    rec.krithi_id;
            END IF;

            IF v_existing_cm.sequence_order IS DISTINCT FROM 0
               OR v_existing_cm.role IS DISTINCT FROM 'CORE'::cycle_member_role_enum
               OR v_existing_cm.axis_value IS DISTINCT FROM rec.kshetra
               OR v_existing_cm.discriminative_attributes IS DISTINCT FROM rec.attrs
            THEN
                UPDATE krithi_cycle_memberships
                SET sequence_order = 0,
                    role = 'CORE'::cycle_member_role_enum,
                    axis_value = rec.kshetra,
                    discriminative_attributes = rec.attrs
                WHERE id = v_existing_cm.id;

                INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
                VALUES (gen_random_uuid(), 'krithi_cycle_memberships', v_existing_cm.id, NULL,
                    'UPDATE_REVIEWED_CYCLE_MEMBERSHIP',
                    jsonb_build_object(
                        'krithi_id', rec.krithi_id, 'tag_id', rec.tag_id,
                        'before', jsonb_build_object(
                            'sequence_order', v_existing_cm.sequence_order, 'role', v_existing_cm.role,
                            'axis_value', v_existing_cm.axis_value,
                            'discriminative_attributes', v_existing_cm.discriminative_attributes),
                        'after', jsonb_build_object(
                            'sequence_order', 0, 'role', 'CORE', 'axis_value', rec.kshetra,
                            'discriminative_attributes', rec.attrs)),
                    v_actor_meta);
            END IF;
        ELSE
            v_cm_id := gen_random_uuid();
            INSERT INTO krithi_cycle_memberships (
                id, krithi_id, tag_id, sequence_order, role, axis_value, discriminative_attributes
            ) VALUES (v_cm_id, rec.krithi_id, rec.tag_id, 0, 'CORE', rec.kshetra, rec.attrs);

            INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
            VALUES (gen_random_uuid(), 'krithi_cycle_memberships', v_cm_id, NULL,
                'INSERT_REVIEWED_CYCLE_MEMBERSHIP',
                jsonb_build_object('krithi_id', rec.krithi_id, 'tag_id', rec.tag_id, 'sequence_order', 0,
                                   'role', 'CORE', 'axis_value', rec.kshetra,
                                   'discriminative_attributes', rec.attrs),
                v_actor_meta);
        END IF;

        -- Reconcile backward-compatible krithi_tags link
        SELECT source, confidence INTO v_existing_tag_source, v_existing_tag_conf
        FROM krithi_tags WHERE krithi_id = rec.krithi_id AND tag_id = rec.tag_id;

        IF FOUND THEN
            IF v_existing_tag_source NOT IN ('canonical_cycle', 'reviewed_cycle')
               AND NOT (v_existing_tag_source = 'curator_reviewed' AND v_existing_cm.id IS NOT NULL)
            THEN
                RAISE EXCEPTION 'TRACK-149: conflicting independently reviewed krithi_tags row (source=%) for krithi %',
                    v_existing_tag_source, rec.krithi_id;
            END IF;

            IF v_existing_tag_source IS DISTINCT FROM 'reviewed_cycle' OR v_existing_tag_conf IS DISTINCT FROM 100 THEN
                UPDATE krithi_tags SET source = 'reviewed_cycle', confidence = 100
                WHERE krithi_id = rec.krithi_id AND tag_id = rec.tag_id;

                INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
                VALUES (gen_random_uuid(), 'krithi_tags', rec.krithi_id, NULL, 'UPDATE_REVIEWED_CYCLE_TAG',
                    jsonb_build_object('krithi_id', rec.krithi_id, 'tag_id', rec.tag_id,
                        'before', jsonb_build_object('source', v_existing_tag_source, 'confidence', v_existing_tag_conf),
                        'after', jsonb_build_object('source', 'reviewed_cycle', 'confidence', 100)),
                    v_actor_meta);
            END IF;
        ELSE
            INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
            VALUES (rec.krithi_id, rec.tag_id, 'reviewed_cycle', 100);

            INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
            VALUES (gen_random_uuid(), 'krithi_tags', rec.krithi_id, NULL, 'INSERT_REVIEWED_CYCLE_TAG',
                jsonb_build_object('krithi_id', rec.krithi_id, 'tag_id', rec.tag_id,
                                   'source', 'reviewed_cycle', 'confidence', 100),
                v_actor_meta);
        END IF;
    END LOOP;
END;
$$;
