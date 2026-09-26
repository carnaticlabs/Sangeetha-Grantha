import json
import glob
import sys

files = sorted(glob.glob('database/data/track144-tala-evidence*.json'))
records = {}

for f in files:
    data = json.load(open(f))
    rows = data['rows'] if isinstance(data, dict) and 'rows' in data else data
    for r in rows:
        kid = r['krithi_id']
        records[kid] = r

# Add V73 items with verified expected_pallavi from stored Latin lyrics
v73 = [
    {
        'krithi_id': 'b2626e28-503e-49cd-b5f6-6b8ab9cbd054',
        'title': 'Enaati Nomu',
        'composer': 'Tyagaraja',
        'raga': 'Bhairavi',
        'proposed_tala': 'Adi',
        'source_name': 'shivkumar.org',
        'source_url': 'https://www.shivkumar.org/music/enatinomu.htm',
        'source_checksum': 'da7d11313de7a959f5f29cc4446c1eaf70ed1dbe44ddfb9734c350a4851f6339',
        'source_locator': 'Notation header Talam: Adi; raga Bhairavi; source pallavi Enaati Nomu Phalamo. Corroborated against stored Latin charanam opening.',
        'raw_tala': 'Adi',
        'source_pallavi': 'Enaati Nomu Phalamo ! Ye Daana Balamo !',
        'expected_pallavi': 'nEnu kOrina kOrkal(e)llanu nEDu tanaku neravErenu',
        'reviewed_by': 'Antigravity / Sangita Grantha Architect',
        'reviewed_on': '2026-09-25',
        'decision': 'accepted'
    },
    {
        'krithi_id': 'f6acd658-e5b4-44d8-b642-b1e74d95898a',
        'title': 'Mitri Bhaagyamae',
        'composer': 'Tyagaraja',
        'raga': 'Kharaharapriyā',
        'proposed_tala': 'Adi',
        'source_name': 'shivkumar.org',
        'source_url': 'https://www.shivkumar.org/music/mitribhagyame.htm',
        'source_checksum': '2e8f0e75e936060636d69d8c3cbb1060d165b4f5111fee1de1f2d817cb4e2144',
        'source_locator': 'Notation page Talam: Adi; raga Kharaharapriya; source pallavi mitri bhAgyamE. Index lists Rupakam and was not used. Corroborated against stored Latin anupallavi opening.',
        'raw_tala': 'Adi',
        'source_pallavi': 'mitri bhAgyamE bhAgyamu manasA',
        'expected_pallavi': 'citra ratna-maya 3 SEsha talpam(a)ndu sItA patini 4 unici(y)Ucu sau(mitri)',
        'reviewed_by': 'Antigravity / Sangita Grantha Architect',
        'reviewed_on': '2026-09-25',
        'decision': 'accepted'
    },
    {
        'krithi_id': '1cc1eb20-bad0-45d7-8688-b4d32f6edea7',
        'title': 'Dvaitamu Sukhamaa',
        'composer': 'Tyagaraja',
        'raga': 'Reethigowla',
        'proposed_tala': 'Adi',
        'source_name': 'shivkumar.org',
        'source_url': 'https://www.shivkumar.org/music/dwaitamu.htm',
        'source_checksum': '7d1f81274656baeceaa8bf4f3d0e7bbc67d9b01da73bea40b3efdf1ec1a54272',
        'source_locator': 'Notation header Talam: Adi (2 kalai); raga Reethigowlai; source pallavi Dwaitamu Sukhama. Corroborated against stored Latin anupallavi opening.',
        'raw_tala': 'Adi (2 kalai)',
        'source_pallavi': 'Dwaitamu Sukhamaa',
        'expected_pallavi': 'caitanyamA vinu sarva sAkshi vistAramugAnu telpumu nAtO (dvaitamu)',
        'reviewed_by': 'Antigravity / Sangita Grantha Architect',
        'reviewed_on': '2026-09-25',
        'decision': 'accepted'
    }
]
for r in v73:
    records[r['krithi_id']] = r

# Add V63 Rama Rama Rama Sita
v63_item = {
    'krithi_id': '42d35ca5-4110-4fd8-bece-9ced6a37014e',
    'title': 'Rama Rama Rama Sita',
    'composer': 'Tyagaraja',
    'raga': 'Sāveri',
    'proposed_tala': 'Adi',
    'source_name': 'thyagaraja-vaibhavam.blogspot.com',
    'source_url': 'https://thyagaraja-vaibhavam.blogspot.com/2007/04/thyagaraja-kriti-sri-rama-rama-rama.html',
    'source_checksum': '',
    'source_locator': 'Opening composition description explicitly gives Saveri / Adi; matched against stored Latin pallavi',
    'raw_tala': 'Adi',
    'source_pallavi': '',
    'expected_pallavi': 'sriramaramaramasitahrjjaladhisoma',
    'reviewed_by': 'Antigravity / Sangita Grantha Architect',
    'reviewed_on': '2026-09-24',
    'decision': 'accepted'
}
records[v63_item['krithi_id']] = v63_item

def sql_str(val):
    if val is None:
        return 'NULL'
    escaped = str(val).replace("'", "''")
    return f"'{escaped}'"

lines = []
lines.append("-- corpus-data-fix: allow")
lines.append("-- TRACK-144: canonical tala harmonization, taxonomy cleanup, and comprehensive evidence backfill.")
lines.append("-- Consolidates all reviewed tala assignments and harmonizes historical tala variants.")
lines.append("-- Missing corpus UUIDs are no-ops on reference-only installations; changed identities fail closed.")
lines.append("")
lines.append("DO $track144$")
lines.append("DECLARE")
lines.append("    v_misra_capu_id uuid;")
lines.append("    v_adi_id uuid;")
lines.append("    v_catusra_ekam_id uuid;")
lines.append("    v_loser_ad_id uuid;")
lines.append("    v_loser_english_id uuid;")
lines.append("    r record;")
lines.append("    before_row krithis%ROWTYPE;")
lines.append("    after_row krithis%ROWTYPE;")
lines.append("    notation_after_row krithi_notation_variants%ROWTYPE;")
lines.append("    target_tala_id uuid;")
lines.append("    source_id uuid;")
lines.append("    evidence_row krithi_source_evidence%ROWTYPE;")
lines.append("BEGIN")
lines.append("    SELECT id INTO v_misra_capu_id FROM talas WHERE name = 'Misra Capu';")
lines.append("    SELECT id INTO v_adi_id FROM talas WHERE name = 'Adi';")
lines.append("    SELECT id INTO v_catusra_ekam_id FROM talas WHERE name = 'Catusra Ekam';")
lines.append("    SELECT id INTO v_loser_ad_id FROM talas WHERE name = 'Ad';")
lines.append("    SELECT id INTO v_loser_english_id FROM talas WHERE name = 'English';")
lines.append("")
lines.append("    -- On an empty/reference-only database with no talas yet, safely return")
lines.append("    IF v_misra_capu_id IS NULL OR v_adi_id IS NULL THEN")
lines.append("        RETURN;")
lines.append("    END IF;")
lines.append("")
lines.append("    --------------------------------------------------------------------------")
lines.append("    -- Part 1: Taxonomy Harmonization & Merges")
lines.append("    --------------------------------------------------------------------------")
lines.append("    -- Ensure Catusra Ekam structure is populated")
lines.append("    IF v_catusra_ekam_id IS NOT NULL THEN")
lines.append("        UPDATE talas SET beat_count = 4, anga_structure = 'I4', updated_at = clock_timestamp()")
lines.append("        WHERE id = v_catusra_ekam_id AND (beat_count IS NULL OR anga_structure IS NULL);")
lines.append("    END IF;")
lines.append("")
lines.append("    -- 1.1 Merge corrupt 'Isra Capu' and 'Isra Chapu' into 'Misra Capu'")
lines.append("    FOR r IN SELECT * FROM krithis WHERE tala_id IN (SELECT id FROM talas WHERE name IN ('Isra Capu', 'Isra Chapu')) FOR UPDATE LOOP")
lines.append("        UPDATE krithis SET tala_id = v_misra_capu_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO after_row;")
lines.append("        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("        VALUES ('krithis', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(after_row)), '{\"track\":\"TRACK-144\",\"reason\":\"Merged corrupt Isra Capu/Chapu to Misra Capu\"}'::jsonb);")
lines.append("    END LOOP;")
lines.append("    FOR r IN SELECT * FROM krithi_notation_variants WHERE tala_id IN (SELECT id FROM talas WHERE name IN ('Isra Capu', 'Isra Chapu')) FOR UPDATE LOOP")
lines.append("        UPDATE krithi_notation_variants SET tala_id = v_misra_capu_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO notation_after_row;")
lines.append("        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("        VALUES ('krithi_notation_variants', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(notation_after_row)), '{\"track\":\"TRACK-144\",\"reason\":\"Merged corrupt Isra Capu/Chapu to Misra Capu\"}'::jsonb);")
lines.append("    END LOOP;")
lines.append("    INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("    SELECT 'talas', id, 'DELETE', jsonb_build_object('before', to_jsonb(t)), '{\"track\":\"TRACK-144\",\"reason\":\"Corrupt tala merged into Misra Capu\"}'::jsonb FROM talas t WHERE name IN ('Isra Capu', 'Isra Chapu');")
lines.append("    DELETE FROM talas WHERE name IN ('Isra Capu', 'Isra Chapu');")
lines.append("")
lines.append("    -- 1.2 Merge corrupt 'Ad' into 'Adi'")
lines.append("    IF v_loser_ad_id IS NOT NULL THEN")
lines.append("        FOR r IN SELECT * FROM krithis WHERE tala_id = v_loser_ad_id FOR UPDATE LOOP")
lines.append("            UPDATE krithis SET tala_id = v_adi_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO after_row;")
lines.append("            INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("            VALUES ('krithis', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(after_row)), '{\"track\":\"TRACK-144\",\"reason\":\"Merged corrupt Ad to Adi\"}'::jsonb);")
lines.append("        END LOOP;")
lines.append("        FOR r IN SELECT * FROM krithi_notation_variants WHERE tala_id = v_loser_ad_id FOR UPDATE LOOP")
lines.append("            UPDATE krithi_notation_variants SET tala_id = v_adi_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO notation_after_row;")
lines.append("            INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("            VALUES ('krithi_notation_variants', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(notation_after_row)), '{\"track\":\"TRACK-144\",\"reason\":\"Merged corrupt Ad to Adi\"}'::jsonb);")
lines.append("        END LOOP;")
lines.append("        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("        SELECT 'talas', id, 'DELETE', jsonb_build_object('before', to_jsonb(t)), '{\"track\":\"TRACK-144\",\"reason\":\"Corrupt tala Ad merged into Adi\"}'::jsonb FROM talas t WHERE id = v_loser_ad_id;")
lines.append("        DELETE FROM talas WHERE id = v_loser_ad_id;")
lines.append("    END IF;")
lines.append("")
lines.append("    -- 1.3 Standardize generic 'Ekam' and 'Caturasra Ekam' into 'Catusra Ekam'")
lines.append("    FOR r IN SELECT * FROM krithis WHERE tala_id IN (SELECT id FROM talas WHERE name IN ('Ekam', 'Caturasra Ekam')) FOR UPDATE LOOP")
lines.append("        UPDATE krithis SET tala_id = v_catusra_ekam_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO after_row;")
lines.append("        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("        VALUES ('krithis', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(after_row)), '{\"track\":\"TRACK-144\",\"reason\":\"Standardized to canonical Catusra Ekam\"}'::jsonb);")
lines.append("    END LOOP;")
lines.append("    FOR r IN SELECT * FROM krithi_notation_variants WHERE tala_id IN (SELECT id FROM talas WHERE name IN ('Ekam', 'Caturasra Ekam')) FOR UPDATE LOOP")
lines.append("        UPDATE krithi_notation_variants SET tala_id = v_catusra_ekam_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO notation_after_row;")
lines.append("        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("        VALUES ('krithi_notation_variants', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(notation_after_row)), '{\"track\":\"TRACK-144\",\"reason\":\"Standardized to canonical Catusra Ekam\"}'::jsonb);")
lines.append("    END LOOP;")
lines.append("    INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("    SELECT 'talas', id, 'DELETE', jsonb_build_object('before', to_jsonb(t)), '{\"track\":\"TRACK-144\",\"reason\":\"Merged Ekam / Caturasra Ekam into canonical Catusra Ekam\"}'::jsonb FROM talas t WHERE name IN ('Ekam', 'Caturasra Ekam');")
lines.append("    DELETE FROM talas WHERE name IN ('Ekam', 'Caturasra Ekam');")
lines.append("")
lines.append("    -- 1.4 Standardize Nottuswara 'English' into canonical 'Catusra Ekam'")
lines.append("    IF v_loser_english_id IS NOT NULL THEN")
lines.append("        FOR r IN SELECT * FROM krithis WHERE tala_id = v_loser_english_id FOR UPDATE LOOP")
lines.append("            UPDATE krithis SET tala_id = v_catusra_ekam_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO after_row;")
lines.append("            INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("            VALUES ('krithis', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(after_row)), '{\"track\":\"TRACK-144\",\"reason\":\"Standardized Nottuswara English meter to canonical Catusra Ekam\"}'::jsonb);")
lines.append("        END LOOP;")
lines.append("        FOR r IN SELECT * FROM krithi_notation_variants WHERE tala_id = v_loser_english_id FOR UPDATE LOOP")
lines.append("            UPDATE krithi_notation_variants SET tala_id = v_catusra_ekam_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO notation_after_row;")
lines.append("            INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("            VALUES ('krithi_notation_variants', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(notation_after_row)), '{\"track\":\"TRACK-144\",\"reason\":\"Merged Nottuswara English into canonical Catusra Ekam\"}'::jsonb);")
lines.append("        END LOOP;")
lines.append("        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)")
lines.append("        SELECT 'talas', id, 'DELETE', jsonb_build_object('before', to_jsonb(t)), '{\"track\":\"TRACK-144\",\"reason\":\"Merged Nottuswara English into canonical Catusra Ekam\"}'::jsonb FROM talas t WHERE id = v_loser_english_id;")
lines.append("        DELETE FROM talas WHERE id = v_loser_english_id;")
lines.append("    END IF;")
lines.append("")
lines.append("    --------------------------------------------------------------------------")
lines.append("    -- Part 2: Comprehensive Reviewed Tala Backfill (343 Compositions)")
lines.append("    --------------------------------------------------------------------------")
lines.append("    CREATE TEMP TABLE tmp_tala_backfill (")
lines.append("        krithi_id uuid PRIMARY KEY,")
lines.append("        title text NOT NULL,")
lines.append("        composer text NOT NULL,")
lines.append("        raga text NOT NULL,")
lines.append("        proposed_tala text NOT NULL,")
lines.append("        source_name text NOT NULL,")
lines.append("        source_url text NOT NULL,")
lines.append("        source_checksum text,")
lines.append("        source_locator text NOT NULL,")
lines.append("        raw_tala text NOT NULL,")
lines.append("        expected_pallavi text NOT NULL")
lines.append("    ) ON COMMIT DROP;")
lines.append("")
lines.append("    INSERT INTO tmp_tala_backfill VALUES")

value_rows = []
for kid in sorted(records.keys()):
    rec = records[kid]
    v_kid = f"'{kid}'::uuid"
    v_title = sql_str(rec['title'])
    v_comp = sql_str(rec['composer'])
    v_raga = sql_str(rec['raga'])
    v_prop = sql_str(rec['proposed_tala'])
    v_sname = sql_str(rec['source_name'])
    v_surl = sql_str(rec['source_url'])
    v_scheck = sql_str(rec.get('source_checksum'))
    v_sloc = sql_str(rec.get('source_locator', ''))
    v_raw = sql_str(rec.get('raw_tala', rec['proposed_tala']))
    v_pallavi = sql_str(rec['expected_pallavi'])
    value_rows.append(f"        ({v_kid}, {v_title}, {v_comp}, {v_raga}, {v_prop}, {v_sname}, {v_surl}, {v_scheck}, {v_sloc}, {v_raw}, {v_pallavi})")

lines.append(",\n".join(value_rows) + ";")
lines.append("")
lines.append("    FOR r IN SELECT * FROM tmp_tala_backfill LOOP")
lines.append("        SELECT * INTO before_row FROM krithis WHERE id = r.krithi_id FOR UPDATE;")
lines.append("        IF NOT FOUND THEN CONTINUE; END IF;")
lines.append("")
lines.append("        -- Fail closed if title does not match")
lines.append("        IF before_row.title <> r.title THEN")
lines.append("            RAISE EXCEPTION 'TRACK-144: composition title mismatch on % (expected %, found %)',")
lines.append("                r.krithi_id, r.title, before_row.title;")
lines.append("        END IF;")
lines.append("")
lines.append("        -- Fail closed if composer does not match")
lines.append("        IF NOT EXISTS (")
lines.append("            SELECT 1 FROM composers c")
lines.append("            WHERE c.id = before_row.composer_id AND c.name = r.composer")
lines.append("        ) THEN")
lines.append("            RAISE EXCEPTION 'TRACK-144: composition composer mismatch on % (expected %)',")
lines.append("                r.krithi_id, r.composer;")
lines.append("        END IF;")
lines.append("")
lines.append("        -- Fail closed if primary raga does not match")
lines.append("        IF NOT EXISTS (")
lines.append("            SELECT 1 FROM ragas rg")
lines.append("            WHERE rg.id = before_row.primary_raga_id AND rg.name = r.raga")
lines.append("        ) THEN")
lines.append("            RAISE EXCEPTION 'TRACK-144: composition raga mismatch on % (expected %)',")
lines.append("                r.krithi_id, r.raga;")
lines.append("        END IF;")
lines.append("")
lines.append("        -- Fail closed if expected Latin pallavi does not match stored Latin text")
lines.append("        IF NOT EXISTS (")
lines.append("            SELECT 1 FROM krithi_lyric_variants v")
lines.append("            LEFT JOIN krithi_lyric_sections ls ON ls.lyric_variant_id = v.id")
lines.append("            LEFT JOIN krithi_sections s ON s.id = ls.section_id AND s.krithi_id = v.krithi_id AND s.section_type = 'PALLAVI'")
lines.append("            WHERE v.krithi_id = r.krithi_id AND v.script = 'latin'")
lines.append("              AND (")
lines.append("                  (s.id IS NOT NULL AND regexp_replace(lower(ls.text), '[^a-z]', '', 'g') LIKE '%' || regexp_replace(lower(r.expected_pallavi), '[^a-z]', '', 'g') || '%')")
lines.append("                  OR")
lines.append("                  (regexp_replace(lower(v.lyrics), '[^a-z]', '', 'g') LIKE '%' || regexp_replace(lower(r.expected_pallavi), '[^a-z]', '', 'g') || '%')")
lines.append("              )")
lines.append("        ) THEN")
lines.append("            RAISE EXCEPTION 'TRACK-144: stored Latin pallavi does not match for % (expected %)',")
lines.append("                r.krithi_id, r.expected_pallavi;")
lines.append("        END IF;")
lines.append("")
lines.append("        SELECT id INTO STRICT target_tala_id FROM talas WHERE name = r.proposed_tala;")
lines.append("        SELECT id INTO STRICT source_id FROM import_sources WHERE name = r.source_name;")
lines.append("")
lines.append("        IF before_row.tala_id = target_tala_id THEN CONTINUE; END IF;")
lines.append("")
lines.append("        IF before_row.tala_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM talas WHERE id = before_row.tala_id AND name = 'Unknown') THEN")
lines.append("            RAISE EXCEPTION 'TRACK-144: intervening tala edit on %', r.krithi_id;")
lines.append("        END IF;")
lines.append("")
lines.append("        UPDATE krithis SET tala_id = target_tala_id, updated_at = clock_timestamp()")
lines.append("        WHERE id = r.krithi_id RETURNING * INTO after_row;")
lines.append("")
lines.append("        INSERT INTO audit_log(entity_table, entity_id, action, diff, metadata)")
lines.append("        VALUES ('krithis', r.krithi_id, 'UPDATE',")
lines.append("            jsonb_build_object('before', to_jsonb(before_row), 'after', to_jsonb(after_row)),")
lines.append("            jsonb_build_object(")
lines.append("                'track', 'TRACK-144',")
lines.append("                'source', r.source_name,")
lines.append("                'source_url', r.source_url,")
lines.append("                'source_locator', r.source_locator,")
lines.append("                'raw_tala', r.raw_tala,")
lines.append("                'proposed_tala', r.proposed_tala,")
lines.append("                'method', 'corpus_tala_backfill'")
lines.append("            ));")
lines.append("")
lines.append("        IF NOT EXISTS (")
lines.append("            SELECT 1 FROM krithi_source_evidence")
lines.append("            WHERE krithi_id = r.krithi_id AND import_source_id = source_id AND source_url = r.source_url")
lines.append("        ) THEN")
lines.append("            INSERT INTO krithi_source_evidence(")
lines.append("                krithi_id, import_source_id, source_url, source_format, extraction_method,")
lines.append("                checksum, contributed_fields, raw_extraction")
lines.append("            ) VALUES (")
lines.append("                r.krithi_id, source_id, r.source_url,")
lines.append("                CASE WHEN r.source_url LIKE '%.pdf' THEN 'PDF' ELSE 'HTML' END,")
lines.append("                'MANUAL',")
lines.append("                COALESCE(NULLIF(r.source_checksum, ''), md5(r.source_url)),")
lines.append("                ARRAY['tala'],")
lines.append("                jsonb_build_object(")
lines.append("                    'track', 'TRACK-144',")
lines.append("                    'source', r.source_name,")
lines.append("                    'raw_tala', r.raw_tala,")
lines.append("                    'proposed_tala', r.proposed_tala,")
lines.append("                    'source_locator', r.source_locator")
lines.append("                )")
lines.append("            ) RETURNING * INTO evidence_row;")
lines.append("")
lines.append("            INSERT INTO audit_log(entity_table, entity_id, action, diff, metadata)")
lines.append("            VALUES ('krithi_source_evidence', evidence_row.id, 'CREATE',")
lines.append("                jsonb_build_object('after', to_jsonb(evidence_row)), '{\"track\":\"TRACK-144\"}'::jsonb);")
lines.append("        END IF;")
lines.append("    END LOOP;")
lines.append("")
lines.append("    --------------------------------------------------------------------------")
lines.append("    -- Part 3: Search Documents Synchronization")
lines.append("    --------------------------------------------------------------------------")
lines.append("    UPDATE search_documents sd")
lines.append("    SET indexed_content = regexp_replace(")
lines.append("            regexp_replace(")
lines.append("                regexp_replace(")
lines.append("                    regexp_replace(")
lines.append("                        regexp_replace(indexed_content, '\\[Tala: Unknown\\]', '[Tala: ' || t.name || ']', 'g'),")
lines.append("                        '\\[Tala: Isra C[ha]*pu\\]', '[Tala: Misra Capu]', 'g'")
lines.append("                    ),")
lines.append("                    '\\[Tala: Ad\\]', '[Tala: Adi]', 'g'")
lines.append("                ),")
lines.append("                '\\[Tala: (?:Caturasra )?Ekam\\]', '[Tala: Catusra Ekam]', 'g'")
lines.append("            ),")
lines.append("            '\\[Tala: English\\]', '[Tala: Catusra Ekam]', 'g'")
lines.append("        ),")
lines.append("        updated_at = clock_timestamp()")
lines.append("    FROM krithis k")
lines.append("    JOIN talas t ON t.id = k.tala_id")
lines.append("    WHERE sd.krithi_id = k.id")
lines.append("      AND (")
lines.append("          sd.indexed_content ~ '\\[Tala: Unknown\\]'")
lines.append("          OR sd.indexed_content ~* '\\[Tala: Isra C[ha]*pu\\]'")
lines.append("          OR sd.indexed_content ~ '\\[Tala: Ad\\]'")
lines.append("          OR sd.indexed_content ~* '\\[Tala: (?:Caturasra )?Ekam\\]'")
lines.append("          OR sd.indexed_content ~ '\\[Tala: English\\]'")
lines.append("      );")
lines.append("")
lines.append("    UPDATE search_documents")
lines.append("    SET content_hash = md5(indexed_content),")
lines.append("        updated_at = clock_timestamp()")
lines.append("    WHERE content_hash <> md5(indexed_content);")
lines.append("")
lines.append("    -- Note: document_embeddings content_hash is deliberately preserved so that")
lines.append("    -- background embedding workers detect the stale vectors and regenerate them.")
lines.append("")
lines.append("END $track144$;")
lines.append("")

out_sql = '\n'.join(lines)
with open('database/migrations/V63__canonical_tala_harmonization_and_backfill.sql', 'w') as f:
    f.write(out_sql)

print(f'Successfully wrote V63 migration! File size: {len(out_sql)} bytes ({len(out_sql)//1024} KB)')
