-- Repeatable seed for TRACK-146: Syama Sastri reviewed cycle memberships.
-- Source of truth: syama-sastri-classification-master.md and
-- track146-syama-sastri-application-manifest.json.
-- Applies only individually approved, witness-verified claims for the Swarajathi
-- Ratnatrayam with provenance-scoped conflict guards and system audit attribution.
-- Ref: application_documentation/01-requirements/features/musicological-classification-contract.md

-- 1. Ensure canonical cycle tag exists in the tags catalog
INSERT INTO tags (id, category, slug, display_name_en, description_en, created_at)
VALUES (
    '01920000-0000-7000-8000-000000000007'::uuid,
    'STOTRA_STYLE',
    'syama-sastri-swarajathi-ratnatrayam',
    'Syama Sastri Swarajathi Ratnatrayam',
    'The three swarajathis of Syama Sastri in Bhairavi, Todi, and Yerukala Kambhoji (Yadukulakambhoji).',
    NOW()
)
ON CONFLICT (slug) DO UPDATE SET
    display_name_en = EXCLUDED.display_name_en,
    description_en = EXCLUDED.description_en
WHERE tags.display_name_en IS DISTINCT FROM EXCLUDED.display_name_en
   OR tags.description_en IS DISTINCT FROM EXCLUDED.description_en;

-- 2. Apply approved Swarajathi Ratnatrayam memberships with conflict protection and system audit logging
DO $$
DECLARE
    v_tag_id CONSTANT uuid := '01920000-0000-7000-8000-000000000007'::uuid;
    v_composer_id CONSTANT uuid := 'c259e3ea-a85c-41b8-951a-19bb3d0d2392'::uuid;
    v_actor_meta CONSTANT jsonb := jsonb_build_object(
        'actor', 'system:TRACK-146',
        'migration', 'R__seed_10_syama_sastri_reviewed_memberships.sql',
        'provenance_owner', 'TRACK-146'
    );
    rec RECORD;
    v_existing_composer uuid;
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
            'f5a415fd-b4ed-4394-ae0b-6f89cacb4fca'::uuid,
            '2974996a-7cee-433f-b523-dbecbf691f68'::uuid,
            '7c15e1c9-5c30-4f68-8ab4-a414847e06be'::uuid
        )
          AND k.composer_id <> v_composer_id
    LOOP
        RAISE EXCEPTION 'TRACK-146: canonical krithi % belongs to unexpected composer % (expected %)',
            rec.id, rec.composer_id, v_composer_id;
    END LOOP;

    -- Remove any stale TRACK-146 memberships on this tag that are not in the approved 3-member manifest
    FOR rec IN
        DELETE FROM krithi_cycle_memberships
        WHERE tag_id = v_tag_id
          AND discriminative_attributes->>'provenance_owner' = 'TRACK-146'
          AND krithi_id NOT IN (
              'f5a415fd-b4ed-4394-ae0b-6f89cacb4fca'::uuid,
              '2974996a-7cee-433f-b523-dbecbf691f68'::uuid,
              '7c15e1c9-5c30-4f68-8ab4-a414847e06be'::uuid
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

    -- Iterate over the 3 approved Ratnatrayam memberships present in the catalogue
    FOR rec IN
        WITH approved_members (krithi_id, sequence_order, role, axis_value, attrs) AS (
            VALUES
                (
                    'f5a415fd-b4ed-4394-ae0b-6f89cacb4fca'::uuid,
                    1,
                    'CORE'::cycle_member_role_enum,
                    'Bhairavi',
                    jsonb_build_object(
                        'provenance_owner', 'TRACK-146',
                        'review_state', 'reviewed',
                        'claim_id', 'SS-0001:group:1',
                        'supporting_claim_ids', jsonb_build_array(
                            'SS-0001:form:1',
                            'SS-0001:laya:1',
                            'SS-0001:structure:1',
                            'SS-0001:mudra:1'
                        ),
                        'laya', 'Misra Chapu',
                        'structure', '8 numbered swara-sahitya units',
                        'mudra', 'Syama Krishna',
                        'approval', jsonb_build_object(
                            'decision', 'approve',
                            'reviewer', 'Seshadri',
                            'accepted_at', '2026-10-06',
                            'note', 'Approved canonical Swarajathi Ratnatrayam membership corroborated by V. Raghavan (1977, PDF p.2).'
                        )
                    )
                ),
                (
                    '2974996a-7cee-433f-b523-dbecbf691f68'::uuid,
                    2,
                    'CORE'::cycle_member_role_enum,
                    'Hanumatodi',
                    jsonb_build_object(
                        'provenance_owner', 'TRACK-146',
                        'review_state', 'reviewed',
                        'claim_id', 'SS-0057:group:1',
                        'supporting_claim_ids', jsonb_build_array(
                            'SS-0057:form:1',
                            'SS-0057:structure:1',
                            'SS-0057:mudra:1'
                        ),
                        'structure', '6 numbered swara-sahitya units',
                        'mudra', 'Syama Krishna',
                        'approval', jsonb_build_object(
                            'decision', 'approve',
                            'reviewer', 'Seshadri',
                            'accepted_at', '2026-10-06',
                            'note', 'Approved canonical Swarajathi Ratnatrayam membership corroborated by V. Raghavan (1977, PDF p.2).'
                        )
                    )
                ),
                (
                    '7c15e1c9-5c30-4f68-8ab4-a414847e06be'::uuid,
                    3,
                    'CORE'::cycle_member_role_enum,
                    'Yadukula Kāmbhoji',
                    jsonb_build_object(
                        'provenance_owner', 'TRACK-146',
                        'review_state', 'reviewed',
                        'claim_id', 'SS-0026:group:1',
                        'supporting_claim_ids', jsonb_build_array(
                            'SS-0026:form:1',
                            'SS-0026:laya:1',
                            'SS-0026:structure:1',
                            'SS-0026:mudra:1'
                        ),
                        'laya', 'Misra Chapu',
                        'structure', '11 numbered swara-sahitya units',
                        'mudra', 'Syama Krishna',
                        'approval', jsonb_build_object(
                            'decision', 'approve',
                            'reviewer', 'Seshadri',
                            'accepted_at', '2026-10-06',
                            'note', 'Approved canonical Swarajathi Ratnatrayam membership corroborated by V. Raghavan (1977, PDF p.2).'
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
            IF COALESCE(v_existing_cm.discriminative_attributes->>'provenance_owner', '') <> 'TRACK-146' THEN
                RAISE EXCEPTION 'TRACK-146: conflicting independently reviewed krithi_cycle_memberships row for krithi %',
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
            -- Abort if an independent non-cycle tag provenance exists (unless migrating prior TRACK-146 state)
            IF v_existing_tag_source NOT IN ('canonical_cycle', 'reviewed_cycle')
               AND NOT (v_existing_tag_source = 'curator_reviewed' AND v_existing_cm.id IS NOT NULL)
            THEN
                RAISE EXCEPTION 'TRACK-146: conflicting independently reviewed krithi_tags row (source=%) for krithi %',
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
