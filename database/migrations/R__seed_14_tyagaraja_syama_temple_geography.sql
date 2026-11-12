-- Repeatable seed for TRACK-149: temple geography for the owner-approved Tyagaraja and Syama Sastri claims.
-- Source of truth: track147-tyagaraja-application-manifest.json (11 named-temple geography claims) and
-- track146-syama-sastri-application-manifest.json (7 Meenakshi (Madurai) geography claims), both approved by
-- Seshadri on 2026-11-03 and lifted from deferred on 2026-11-12 (track149-lead-decisions.json; Syama master).
-- A geography claim becomes krithis.temple_id. NON_SPECIFIC_ADDRESS claims are not temples and are not applied.
-- The four Tyagaraja temples did not exist, so they are created here with fixed ids (…62 to …65) and sourced
-- coordinates and deity. place_review_state stays NULL (no Google place id confirmed), so they are unreviewed
-- places. Guards abort rather than overwrite a krithi that already points at a different temple.
-- Ref: application_documentation/01-requirements/features/tyagaraja-classification-master.md

-- 1. Deities
INSERT INTO deities (id, name, name_normalized, created_at, updated_at)
VALUES
(gen_random_uuid(), 'Ranganatha', 'ranganatha', NOW(), NOW()),
(gen_random_uuid(), 'Shiva', 'shiva', NOW(), NOW()),
(gen_random_uuid(), 'Tripurasundari', 'tripurasundari', NOW(), NOW())
ON CONFLICT (name_normalized) DO NOTHING;

-- 2. Temples (coordinates in decimal degrees; sources in notes)
INSERT INTO temples (
    id, name, name_normalized, city, state, country,
    primary_deity_id, latitude, longitude, place_kind, mandalam, notes, created_at, updated_at
)
VALUES
(
    '01930000-0000-7000-8000-000000000062', 'Srirangam Ranganathaswamy Temple', 'srirangam ranganathaswamy temple', 'Srirangam', 'Tamil Nadu', 'India',
    (SELECT id FROM deities WHERE name_normalized = 'ranganatha'), 10.8625, 78.68972, 'COMPLEX', 'CHOLA',
    'Ranganatha (Vishnu) with Ranganayaki, Srirangam island, Tiruchirappalli. Anchor of the Tyagaraja Srirangam Pancharatnam. Coordinates from the Wikipedia infobox (10°51′45″N 78°41′23″E), not field-surveyed.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000063', 'Kovur Sundareswara Temple', 'kovur sundareswara temple', 'Kovur', 'Tamil Nadu', 'India',
    (SELECT id FROM deities WHERE name_normalized = 'shiva'), 13.012324, 80.121257, 'COMPLEX', 'TONDAI',
    'Sundareswarar (Shiva) with Soundarambikai, Kovur, Chennai. Anchor of the Tyagaraja Kovur Pancharatnam (partial set). Coordinates from the Wikipedia infobox (13.012324 N, 80.121257 E), not field-surveyed.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000064', 'Lalgudi Saptarishiswara Temple', 'lalgudi saptarishiswara temple', 'Lalgudi', 'Tamil Nadu', 'India',
    (SELECT id FROM deities WHERE name_normalized = 'shiva'), 10.8703, 78.8206, 'COMPLEX', 'CHOLA',
    'Saptharishishwarar (Shiva), Lalgudi, Tiruchirappalli district. Anchor of the Tyagaraja Lalgudi Pancharatnam (partial set). Coordinates from the Wikipedia infobox (10°52′13″N 78°49′14″E), not field-surveyed. Goddess names vary by source and are not recorded here.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000065', 'Tiruvottiyur Tripurasundari Temple', 'tiruvottiyur tripurasundari temple', 'Tiruvottiyur', 'Tamil Nadu', 'India',
    (SELECT id FROM deities WHERE name_normalized = 'tripurasundari'), 13.1609, 80.2992, 'COMPLEX', 'TONDAI',
    'Vadivudai Amman (Tripurasundari) shrine of the Thyagaraja (Adipureeswarar) temple complex, Tiruvottiyur, north Chennai. Named in the approved claim as the Tiruvottiyur Tripurasundari Temple. Coordinates from the Wikipedia infobox for the complex (13.1609 N, 80.2992 E), not field-surveyed.',
    NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
    primary_deity_id = EXCLUDED.primary_deity_id,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    place_kind = EXCLUDED.place_kind,
    mandalam = EXCLUDED.mandalam,
    notes = EXCLUDED.notes,
    updated_at = NOW()
WHERE temples.place_review_state IS NULL
  AND (temples.primary_deity_id IS DISTINCT FROM EXCLUDED.primary_deity_id
       OR temples.latitude IS DISTINCT FROM EXCLUDED.latitude
       OR temples.longitude IS DISTINCT FROM EXCLUDED.longitude
       OR temples.place_kind IS DISTINCT FROM EXCLUDED.place_kind
       OR temples.mandalam IS DISTINCT FROM EXCLUDED.mandalam
       OR temples.notes IS DISTINCT FROM EXCLUDED.notes);

-- 3. Link krithis to temples with conflict protection and system audit logging
DO $$
DECLARE
    v_tyagaraja CONSTANT uuid := 'b1bdccd8-8cb1-4a14-a9e6-7ffc843cd7b6'::uuid;
    v_syama CONSTANT uuid := 'c259e3ea-a85c-41b8-951a-19bb3d0d2392'::uuid;
    v_meenakshi uuid;
    v_actor_meta CONSTANT jsonb := jsonb_build_object(
        'actor', 'system:TRACK-149',
        'migration', 'R__seed_14_tyagaraja_syama_temple_geography.sql',
        'provenance_owner', 'TRACK-149'
    );
    rec RECORD;
    v_present int;
BEGIN
    SELECT id INTO v_meenakshi FROM temples
    WHERE name_normalized = 'meenakshi sundareswarar temple' AND city = 'Madurai';

    DROP TABLE IF EXISTS _t149_geo;
    CREATE TEMP TABLE _t149_geo (
        temple_id uuid, krithi_id uuid, claim_id text, witness_id text, note text, track text
    ) ON COMMIT DROP;

    INSERT INTO _t149_geo (temple_id, krithi_id, claim_id, witness_id, note, track) VALUES
            ('01930000-0000-7000-8000-000000000062'::uuid, '0931f98a-64fa-403f-967c-941677395832'::uuid, 'tyagaraja:krithi:0931f98a-64fa-403f-967c-941677395832:geography', 'ma:jma-vol54-1983', 'Named in the Srirangam Pancharatnam list.', 'TRACK-147'),
            ('01930000-0000-7000-8000-000000000063'::uuid, '1d3c167b-d5ae-4f4b-8e55-8f11ab9c39e4'::uuid, 'tyagaraja:krithi:1d3c167b-d5ae-4f4b-8e55-8f11ab9c39e4:geography', 'hindupedia-thyagaraja-compilation', 'Labelled as a Kovur Pancharatna item in a secondary translation compilation.', 'TRACK-147'),
            ('01930000-0000-7000-8000-000000000064'::uuid, '271a1479-c84d-4c22-af76-040f659fce2b'::uuid, 'tyagaraja:krithi:271a1479-c84d-4c22-af76-040f659fce2b:geography', 'ma:jma-vol69-1998', 'Named in the Lalgudi Pancharatnam list.', 'TRACK-147'),
            ('01930000-0000-7000-8000-000000000064'::uuid, '4215e9b5-17dd-4730-a140-55d48014b034'::uuid, 'tyagaraja:krithi:4215e9b5-17dd-4730-a140-55d48014b034:geography', 'ma:jma-vol69-1998', 'Named in the Lalgudi Pancharatnam list.', 'TRACK-147'),
            ('01930000-0000-7000-8000-000000000064'::uuid, '550a03c3-991c-435f-b1b5-439c16e98183'::uuid, 'tyagaraja:krithi:550a03c3-991c-435f-b1b5-439c16e98183:geography', 'ma:jma-vol69-1998', 'Named in the Lalgudi Pancharatnam list.', 'TRACK-147'),
            ('01930000-0000-7000-8000-000000000064'::uuid, '7914b699-b204-42b1-9057-72f4ff5db6d4'::uuid, 'tyagaraja:krithi:7914b699-b204-42b1-9057-72f4ff5db6d4:geography', 'ma:jma-vol69-1998', 'Named in the Lalgudi Pancharatnam list.', 'TRACK-147'),
            ('01930000-0000-7000-8000-000000000063'::uuid, '8300ca9e-eca0-4d3a-8a93-0c54c3715baf'::uuid, 'tyagaraja:krithi:8300ca9e-eca0-4d3a-8a93-0c54c3715baf:geography', 'hindupedia-thyagaraja-compilation', 'Labelled as a Kovur Pancharatna item in a secondary translation compilation.', 'TRACK-147'),
            ('01930000-0000-7000-8000-000000000065'::uuid, '9c89d2d5-c211-4790-8577-90bfccbf697e'::uuid, 'tyagaraja:krithi:9c89d2d5-c211-4790-8577-90bfccbf697e:geography', 'ma:jma-vol66-1995', 'Journal names the kriti and the Tiruvottiyur shrine.', 'TRACK-147'),
            ('01930000-0000-7000-8000-000000000062'::uuid, 'c262739c-e7cc-492d-b248-3775fec24bfe'::uuid, 'tyagaraja:krithi:c262739c-e7cc-492d-b248-3775fec24bfe:geography', 'ma:jma-vol54-1983', 'Named in the Srirangam Pancharatnam list.', 'TRACK-147'),
            ('01930000-0000-7000-8000-000000000062'::uuid, 'c5aee074-7a6b-4fd3-83ea-19fd6d9083eb'::uuid, 'tyagaraja:krithi:c5aee074-7a6b-4fd3-83ea-19fd6d9083eb:geography', 'ma:jma-vol54-1983', 'Named in the Srirangam Pancharatnam list.', 'TRACK-147'),
            ('01930000-0000-7000-8000-000000000063'::uuid, 'e630e8f7-f8da-45b3-8e8b-fce0f91a74e9'::uuid, 'tyagaraja:krithi:e630e8f7-f8da-45b3-8e8b-fce0f91a74e9:geography', 'hindupedia-thyagaraja-compilation', 'Labelled as a Kovur Pancharatna item in a secondary translation compilation.', 'TRACK-147'),
            (NULL::uuid, '60aa1036-9063-41b0-bdf6-dd049d2fc620'::uuid, 'SS-0012:geography:2', 'ma:souvenir-1973', 'Both 1954 and 1973 list this kriti among the Navaratnamalika in praise of Meenakshi (Madurai).', 'TRACK-146'),
            (NULL::uuid, 'd292a5d8-8b3b-480b-bd16-d514d970f56c'::uuid, 'SS-0014:geography:2', 'ma:souvenir-1973', 'Both 1954 and 1973 list this kriti among the Navaratnamalika in praise of Meenakshi (Madurai).', 'TRACK-146'),
            (NULL::uuid, 'd14d643b-bfc5-4e39-b2c5-d4433e5be9bf'::uuid, 'SS-0032:geography:2', 'ma:souvenir-1973', 'Both 1954 and 1973 list this kriti among the Navaratnamalika in praise of Meenakshi (Madurai).', 'TRACK-146'),
            (NULL::uuid, '0c251190-3cde-4d05-b584-19930220924c'::uuid, 'SS-0034:geography:2', 'ma:souvenir-1973', 'Both 1954 and 1973 list this kriti among the Navaratnamalika in praise of Meenakshi (Madurai).', 'TRACK-146'),
            (NULL::uuid, '1c7e43c2-e2b7-4b22-bdf3-7bf3e590e4a9'::uuid, 'SS-0035:geography:2', 'ma:souvenir-1973', 'Both 1954 and 1973 list this kriti among the Navaratnamalika in praise of Meenakshi (Madurai).', 'TRACK-146'),
            (NULL::uuid, '63b6c905-8a50-433f-8310-20a60158fd9e'::uuid, 'SS-0038:geography:2', 'ma:souvenir-1973', 'Both 1954 and 1973 list this kriti among the Navaratnamalika in praise of Meenakshi (Madurai).', 'TRACK-146'),
            (NULL::uuid, 'b1f06cf0-96ed-4b99-9f90-9de28a029b7d'::uuid, 'SS-0065:geography:2', 'ma:souvenir-1973', 'Both 1954 and 1973 list this kriti among the Navaratnamalika in praise of Meenakshi (Madurai).', 'TRACK-146');

    UPDATE _t149_geo SET temple_id = v_meenakshi WHERE temple_id IS NULL;

    IF EXISTS (SELECT 1 FROM _t149_geo WHERE temple_id IS NULL) THEN
        RAISE NOTICE 'TRACK-149: Meenakshi Sundareswarar Temple is absent from this catalogue; its geography claims are skipped';
        DELETE FROM _t149_geo WHERE temple_id IS NULL;
    END IF;

    SELECT count(*) INTO v_present FROM _t149_geo g JOIN krithis k ON k.id = g.krithi_id;
    IF v_present < 18 THEN
        RAISE NOTICE 'TRACK-149: only % of 18 approved geography krithis exist in this catalogue; the rest are skipped', v_present;
    END IF;

    FOR rec IN
        SELECT g.*, k.composer_id, k.temple_id AS current_temple_id, k.title
        FROM _t149_geo g
        JOIN krithis k ON k.id = g.krithi_id
        ORDER BY g.track, g.claim_id
    LOOP
        IF rec.composer_id NOT IN (v_tyagaraja, v_syama) THEN
            RAISE EXCEPTION 'TRACK-149: krithi % belongs to unexpected composer %', rec.krithi_id, rec.composer_id;
        END IF;

        IF rec.current_temple_id IS NOT NULL AND rec.current_temple_id <> rec.temple_id THEN
            RAISE EXCEPTION 'TRACK-149: krithi % already points at temple %, not overwriting with %',
                rec.krithi_id, rec.current_temple_id, rec.temple_id;
        END IF;

        IF rec.current_temple_id IS NULL THEN
            UPDATE krithis SET temple_id = rec.temple_id, updated_at = NOW() WHERE id = rec.krithi_id;

            INSERT INTO audit_log (id, entity_table, entity_id, actor_user_id, action, diff, metadata)
            VALUES (gen_random_uuid(), 'krithis', rec.krithi_id, NULL, 'INSERT_REVIEWED_GEOGRAPHY_TEMPLE',
                jsonb_build_object('krithi_id', rec.krithi_id, 'temple_id', rec.temple_id,
                                   'claim_id', rec.claim_id, 'witness_id', rec.witness_id,
                                   'reviewer', 'Seshadri', 'accepted_at', '2026-11-03', 'note', rec.note),
                v_actor_meta || jsonb_build_object('claim_track', rec.track));
        END IF;
    END LOOP;
END;
$$;
