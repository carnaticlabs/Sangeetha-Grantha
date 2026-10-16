-- Repeatable seed for TRACK-147: Tyagaraja reviewed cycle memberships.
-- Source of truth: tyagaraja-classification-master.md and
-- track147-tyagaraja-application-manifest.json.
-- Applies only individually approved, witness-verified claims for the Ghana-raga
-- Pancharatnam with provenance-scoped conflict guards and system audit attribution.
-- Aradhana concert order (1..5: Nattai, Gowla, Arabhi, Varali, Sri) is recorded as a
-- performance convention in sequence_order and discriminative_attributes, distinct from
-- any composer-declared sequence.
-- Ref: application_documentation/01-requirements/features/tyagaraja-classification-master.md

-- 1. Ensure canonical cycle tag exists in the tags catalog
INSERT INTO tags (id, category, slug, display_name_en, description_en, created_at)
VALUES (
    '01920000-0000-7000-8000-000000000004'::uuid,
    'STOTRA_STYLE',
    'tyagaraja-pancharatnam',
    'Tyagaraja Ghana Raga Pancharatnam',
    'The five gem compositions by Saint Tyagaraja set in the five major ghana ragas: Nattai, Gaula, Arabhi, Varali, and Sri.',
    NOW()
)
ON CONFLICT (slug) DO UPDATE SET
    display_name_en = EXCLUDED.display_name_en,
    description_en = EXCLUDED.description_en
WHERE tags.display_name_en IS DISTINCT FROM EXCLUDED.display_name_en
   OR tags.description_en IS DISTINCT FROM EXCLUDED.description_en;

-- 2. Apply approved Ghana-raga Pancharatnam memberships with conflict protection and system audit logging
DO $$
DECLARE
    v_tag_id CONSTANT uuid := '01920000-0000-7000-8000-000000000004'::uuid;
    v_composer_id CONSTANT uuid := 'b1bdccd8-8cb1-4a14-a9e6-7ffc843cd7b6'::uuid;
    v_actor_meta CONSTANT jsonb := jsonb_build_object(
        'actor', 'system:TRACK-147',
        'migration', 'R__seed_11_tyagaraja_reviewed_memberships.sql',
        'provenance_owner', 'TRACK-147'
    );
    rec RECORD;
    v_existing_cm RECORD;
    v_existing_tag_source text;
    v_existing_tag_conf int;
    v_cm_id uuid;
BEGIN
    -- Abort if any target canonical krithi UUID is bound to a different composer
    FOR rec IN
        SELECT k.id, k.composer_id
        FROM krithis k
        WHERE k.id IN (
            '2e115182-dcc2-4a41-962c-8381821a7489'::uuid,
            'b4029df0-a209-470b-ba56-3d64fa8aebc4'::uuid,
            '6fd7860c-0e5d-428f-af9f-cd02c3ad2da8'::uuid,
            'd59984e7-fabc-4536-b5a2-60e550277626'::uuid,
            '2b65609e-9fbf-4232-baae-096cafdf4e79'::uuid
        )
          AND k.composer_id <> v_composer_id
    LOOP
        RAISE EXCEPTION 'TRACK-147: canonical krithi % belongs to unexpected composer % (expected %)',
            rec.id, rec.composer_id, v_composer_id;
    END LOOP;

    -- Remove any stale TRACK-147 memberships on this tag that are not in the approved 5-member manifest
    FOR rec IN
        DELETE FROM krithi_cycle_memberships
        WHERE tag_id = v_tag_id
          AND discriminative_attributes->>'provenance_owner' = 'TRACK-147'
          AND krithi_id NOT IN (
              '2e115182-dcc2-4a41-962c-8381821a7489'::uuid,
              'b4029df0-a209-470b-ba56-3d64fa8aebc4'::uuid,
              '6fd7860c-0e5d-428f-af9f-cd02c3ad2da8'::uuid,
              'd59984e7-fabc-4536-b5a2-60e550277626'::uuid,
              '2b65609e-9fbf-4232-baae-096cafdf4e79'::uuid
          )
        RETURNING id, krithi_id, discriminative_attributes
    LOOP
        INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
        VALUES (
            gen_random_uuid(),
            'krithi_cycle_memberships',
            rec.id,
            NULL,
            'DELETE_STALE_CYCLE_MEMBERSHIP',
            jsonb_build_object(
                'krithi_id', rec.krithi_id,
                'tag_id', v_tag_id,
                'previous_attributes', rec.discriminative_attributes
            ),
            v_actor_meta
        );
    END LOOP;

    -- Iterate over the 5 approved Ghana-raga Pancharatnam memberships present in the catalogue
    FOR rec IN
        WITH approved_members (krithi_id, sequence_order, role, axis_value, attrs) AS (
            VALUES
                (
                    '2e115182-dcc2-4a41-962c-8381821a7489'::uuid,
                    1,
                    'CORE'::cycle_member_role_enum,
                    'Nāṭṭai',
                    jsonb_build_object(
                        'provenance_owner', 'TRACK-147',
                        'review_state', 'reviewed',
                        'claim_id', 'tyagaraja:krithi:2e115182-dcc2-4a41-962c-8381821a7489:group',
                        'supporting_claim_ids', jsonb_build_array(
                            'tyagaraja:krithi:2e115182-dcc2-4a41-962c-8381821a7489:deity',
                            'tyagaraja:krithi:2e115182-dcc2-4a41-962c-8381821a7489:theme',
                            'tyagaraja:krithi:2e115182-dcc2-4a41-962c-8381821a7489:geography'
                        ),
                        'performance_order', 1,
                        'order_convention', 'Aradhana concert tradition (not composer sequence)',
                        'theme', 'Rama Bhakti & Nama Mahima',
                        'mudra', 'Tyagaraja',
                        'approval', jsonb_build_object(
                            'decision', 'approve',
                            'reviewer', 'Seshadri',
                            'accepted_at', '2026-10-15',
                            'note', 'Approved canonical Ghana-raga Pancharatnam membership (Nattai, Aradhana order 1) verified against fetched witness and catalogue snapshot.'
                        )
                    )
                ),
                (
                    'b4029df0-a209-470b-ba56-3d64fa8aebc4'::uuid,
                    2,
                    'CORE'::cycle_member_role_enum,
                    'Gowla',
                    jsonb_build_object(
                        'provenance_owner', 'TRACK-147',
                        'review_state', 'reviewed',
                        'claim_id', 'tyagaraja:krithi:b4029df0-a209-470b-ba56-3d64fa8aebc4:group',
                        'supporting_claim_ids', jsonb_build_array(
                            'tyagaraja:krithi:b4029df0-a209-470b-ba56-3d64fa8aebc4:deity',
                            'tyagaraja:krithi:b4029df0-a209-470b-ba56-3d64fa8aebc4:theme',
                            'tyagaraja:krithi:b4029df0-a209-470b-ba56-3d64fa8aebc4:geography'
                        ),
                        'performance_order', 2,
                        'order_convention', 'Aradhana concert tradition (not composer sequence)',
                        'theme', 'Ethical Reflection & Self-Reproach',
                        'mudra', 'Tyagaraja',
                        'approval', jsonb_build_object(
                            'decision', 'approve',
                            'reviewer', 'Seshadri',
                            'accepted_at', '2026-10-15',
                            'note', 'Approved canonical Ghana-raga Pancharatnam membership (Gowla, Aradhana order 2) verified against fetched witness and catalogue snapshot.'
                        )
                    )
                ),
                (
                    '6fd7860c-0e5d-428f-af9f-cd02c3ad2da8'::uuid,
                    3,
                    'CORE'::cycle_member_role_enum,
                    'Ārabhi',
                    jsonb_build_object(
                        'provenance_owner', 'TRACK-147',
                        'review_state', 'reviewed',
                        'claim_id', 'tyagaraja:krithi:6fd7860c-0e5d-428f-af9f-cd02c3ad2da8:group',
                        'supporting_claim_ids', jsonb_build_array(
                            'tyagaraja:krithi:6fd7860c-0e5d-428f-af9f-cd02c3ad2da8:deity',
                            'tyagaraja:krithi:6fd7860c-0e5d-428f-af9f-cd02c3ad2da8:theme',
                            'tyagaraja:krithi:6fd7860c-0e5d-428f-af9f-cd02c3ad2da8:geography'
                        ),
                        'performance_order', 3,
                        'order_convention', 'Aradhana concert tradition (not composer sequence)',
                        'theme', 'Narrative Allusion & Leela Praise',
                        'structure', '7 Swara-Sahitya sections',
                        'mudra', 'Tyagaraja',
                        'approval', jsonb_build_object(
                            'decision', 'approve',
                            'reviewer', 'Seshadri',
                            'accepted_at', '2026-10-15',
                            'note', 'Approved canonical Ghana-raga Pancharatnam membership (Arabhi, Aradhana order 3) verified against fetched witness and catalogue snapshot.'
                        )
                    )
                ),
                (
                    'd59984e7-fabc-4536-b5a2-60e550277626'::uuid,
                    4,
                    'CORE'::cycle_member_role_enum,
                    'Varāli',
                    jsonb_build_object(
                        'provenance_owner', 'TRACK-147',
                        'review_state', 'reviewed',
                        'claim_id', 'tyagaraja:krithi:d59984e7-fabc-4536-b5a2-60e550277626:group',
                        'supporting_claim_ids', jsonb_build_array(
                            'tyagaraja:krithi:d59984e7-fabc-4536-b5a2-60e550277626:deity',
                            'tyagaraja:krithi:d59984e7-fabc-4536-b5a2-60e550277626:theme',
                            'tyagaraja:krithi:d59984e7-fabc-4536-b5a2-60e550277626:geography'
                        ),
                        'performance_order', 4,
                        'order_convention', 'Aradhana concert tradition (not composer sequence)',
                        'theme', 'Rama Bhakti & Darshana Bliss',
                        'mudra', 'Tyagaraja',
                        'approval', jsonb_build_object(
                            'decision', 'approve',
                            'reviewer', 'Seshadri',
                            'accepted_at', '2026-10-15',
                            'note', 'Approved canonical Ghana-raga Pancharatnam membership (Varali, Aradhana order 4) verified against fetched witness and catalogue snapshot.'
                        )
                    )
                ),
                (
                    '2b65609e-9fbf-4232-baae-096cafdf4e79'::uuid,
                    5,
                    'CORE'::cycle_member_role_enum,
                    'Sri',
                    jsonb_build_object(
                        'provenance_owner', 'TRACK-147',
                        'review_state', 'reviewed',
                        'claim_id', 'tyagaraja:krithi:2b65609e-9fbf-4232-baae-096cafdf4e79:group',
                        'supporting_claim_ids', jsonb_build_array(
                            'tyagaraja:krithi:2b65609e-9fbf-4232-baae-096cafdf4e79:deity',
                            'tyagaraja:krithi:2b65609e-9fbf-4232-baae-096cafdf4e79:theme',
                            'tyagaraja:krithi:2b65609e-9fbf-4232-baae-096cafdf4e79:geography'
                        ),
                        'performance_order', 5,
                        'order_convention', 'Aradhana concert tradition (not composer sequence)',
                        'theme', 'Saints and Teachers & Nadopasana',
                        'mudra', 'Tyagaraja',
                        'approval', jsonb_build_object(
                            'decision', 'approve',
                            'reviewer', 'Seshadri',
                            'accepted_at', '2026-10-15',
                            'note', 'Approved canonical Ghana-raga Pancharatnam membership (Sri, Aradhana order 5) verified against fetched witness and catalogue snapshot.'
                        )
                    )
                )
        )
        SELECT am.krithi_id, am.sequence_order, am.role, am.axis_value, am.attrs
        FROM approved_members am
        JOIN krithis k ON k.id = am.krithi_id AND k.composer_id = v_composer_id
        ORDER BY am.sequence_order
    LOOP
        SELECT id, sequence_order, role, axis_value, discriminative_attributes
          INTO v_existing_cm
        FROM krithi_cycle_memberships
        WHERE krithi_id = rec.krithi_id AND tag_id = v_tag_id;

        IF FOUND THEN
            -- Abort on conflict with an independently owned membership row
            IF COALESCE(v_existing_cm.discriminative_attributes->>'provenance_owner', '') <> 'TRACK-147' THEN
                RAISE EXCEPTION 'TRACK-147: conflicting independently reviewed krithi_cycle_memberships row for krithi %',
                    rec.krithi_id;
            END IF;

            IF v_existing_cm.sequence_order IS DISTINCT FROM rec.sequence_order
               OR v_existing_cm.role IS DISTINCT FROM rec.role
               OR v_existing_cm.axis_value IS DISTINCT FROM rec.axis_value
               OR v_existing_cm.discriminative_attributes IS DISTINCT FROM rec.attrs
            THEN
                UPDATE krithi_cycle_memberships
                SET sequence_order = rec.sequence_order,
                    role = rec.role,
                    axis_value = rec.axis_value,
                    discriminative_attributes = rec.attrs
                WHERE id = v_existing_cm.id;

                INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
                VALUES (
                    gen_random_uuid(),
                    'krithi_cycle_memberships',
                    v_existing_cm.id,
                    NULL,
                    'UPDATE_REVIEWED_CYCLE_MEMBERSHIP',
                    jsonb_build_object(
                        'krithi_id', rec.krithi_id,
                        'tag_id', v_tag_id,
                        'before', jsonb_build_object(
                            'sequence_order', v_existing_cm.sequence_order,
                            'role', v_existing_cm.role,
                            'axis_value', v_existing_cm.axis_value,
                            'discriminative_attributes', v_existing_cm.discriminative_attributes
                        ),
                        'after', jsonb_build_object(
                            'sequence_order', rec.sequence_order,
                            'role', rec.role,
                            'axis_value', rec.axis_value,
                            'discriminative_attributes', rec.attrs
                        )
                    ),
                    v_actor_meta
                );
            END IF;
        ELSE
            v_cm_id := gen_random_uuid();
            INSERT INTO krithi_cycle_memberships (
                id, krithi_id, tag_id, sequence_order, role, axis_value, discriminative_attributes
            ) VALUES (
                v_cm_id, rec.krithi_id, v_tag_id, rec.sequence_order, rec.role, rec.axis_value, rec.attrs
            );

            INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
            VALUES (
                gen_random_uuid(),
                'krithi_cycle_memberships',
                v_cm_id,
                NULL,
                'INSERT_REVIEWED_CYCLE_MEMBERSHIP',
                jsonb_build_object(
                    'krithi_id', rec.krithi_id,
                    'tag_id', v_tag_id,
                    'sequence_order', rec.sequence_order,
                    'role', rec.role,
                    'axis_value', rec.axis_value,
                    'discriminative_attributes', rec.attrs
                ),
                v_actor_meta
            );
        END IF;

        -- Reconcile backward-compatible krithi_tags link
        SELECT source, confidence
          INTO v_existing_tag_source, v_existing_tag_conf
        FROM krithi_tags
        WHERE krithi_id = rec.krithi_id AND tag_id = v_tag_id;

        IF FOUND THEN
            -- Abort if an independent non-cycle tag provenance exists (unless migrating prior TRACK-147 state)
            IF v_existing_tag_source NOT IN ('canonical_cycle', 'reviewed_cycle')
               AND NOT (v_existing_tag_source = 'curator_reviewed' AND v_existing_cm.id IS NOT NULL)
            THEN
                RAISE EXCEPTION 'TRACK-147: conflicting independently reviewed krithi_tags row (source=%) for krithi %',
                    v_existing_tag_source, rec.krithi_id;
            END IF;

            IF v_existing_tag_source IS DISTINCT FROM 'reviewed_cycle'
               OR v_existing_tag_conf IS DISTINCT FROM 100
            THEN
                UPDATE krithi_tags
                SET source = 'reviewed_cycle',
                    confidence = 100
                WHERE krithi_id = rec.krithi_id AND tag_id = v_tag_id;

                INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
                VALUES (
                    gen_random_uuid(),
                    'krithi_tags',
                    rec.krithi_id,
                    NULL,
                    'UPDATE_REVIEWED_CYCLE_TAG',
                    jsonb_build_object(
                        'krithi_id', rec.krithi_id,
                        'tag_id', v_tag_id,
                        'before', jsonb_build_object('source', v_existing_tag_source, 'confidence', v_existing_tag_conf),
                        'after', jsonb_build_object('source', 'reviewed_cycle', 'confidence', 100)
                    ),
                    v_actor_meta
                );
            END IF;
        ELSE
            INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
            VALUES (rec.krithi_id, v_tag_id, 'reviewed_cycle', 100);

            INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
            VALUES (
                gen_random_uuid(),
                'krithi_tags',
                rec.krithi_id,
                NULL,
                'INSERT_REVIEWED_CYCLE_TAG',
                jsonb_build_object(
                    'krithi_id', rec.krithi_id,
                    'tag_id', v_tag_id,
                    'source', 'reviewed_cycle',
                    'confidence', 100
                ),
                v_actor_meta
            );
        END IF;
    END LOOP;
END;
$$;
