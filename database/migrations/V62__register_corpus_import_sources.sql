-- TRACK-144: register external sources used for reviewed corpus tala backfills.

-- 1. P. P. Narayanaswami Dikshitar compilation (Tier 3)
WITH added_guruguha AS (
    INSERT INTO import_sources (name, base_url, description, source_tier, supported_formats)
    VALUES (
        'ibiblio.org/guruguha',
        'https://www.ibiblio.org/guruguha/',
        'P. P. Narayanaswami, Muttusvami Dikshitar Kirtana Samaharam, August 2007; edition-specific manual review',
        3,
        '{PDF}'
    )
    ON CONFLICT (name) DO NOTHING
    RETURNING *
)
INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
SELECT 'import_sources', id, 'CREATE', jsonb_build_object('after', to_jsonb(added_guruguha)),
       '{"track":"TRACK-144","source_url":"https://www.ibiblio.org/guruguha/mdeng.pdf"}'::jsonb
FROM added_guruguha;

-- 2. Karnatik.com database (Tier 4)
WITH added_karnatik AS (
    INSERT INTO import_sources (name, base_url, description, source_tier, supported_formats)
    VALUES (
        'karnatik.com',
        'https://www.karnatik.com',
        'Community-maintained database with broad coverage. Composers index at /composers.shtml.',
        4,
        '{HTML}'
    )
    ON CONFLICT (name) DO NOTHING
    RETURNING *
)
INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
SELECT 'import_sources', id, 'CREATE', jsonb_build_object('after', to_jsonb(added_karnatik)),
       '{"track":"TRACK-144","source":"karnatik.com"}'::jsonb
FROM added_karnatik;
