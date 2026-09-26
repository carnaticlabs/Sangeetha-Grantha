-- TRACK-144: read-only JSON inventory for scripts/audit_missing_talas.py.
-- Run with psql -Atf; no database mutation and no source fetching.
SELECT jsonb_build_object('rows', coalesce(jsonb_agg(to_jsonb(inventory) ORDER BY composer,title,id), '[]'::jsonb))
FROM (
    SELECT k.id, k.title, c.name AS composer, r.name AS raga,
        (SELECT ls.text FROM krithi_lyric_variants v
         JOIN krithi_lyric_sections ls ON ls.lyric_variant_id=v.id
         JOIN krithi_sections s ON s.id=ls.section_id AND s.krithi_id=v.krithi_id
         WHERE v.krithi_id=k.id AND v.script='latin' AND s.section_type='PALLAVI'
         ORDER BY v.is_primary DESC,v.created_at,v.id LIMIT 1) AS pallavi,
        coalesce((SELECT jsonb_agg(jsonb_build_object('source_url', e.source_url))
                  FROM krithi_source_evidence e WHERE e.krithi_id=k.id), '[]'::jsonb) AS evidence,
        coalesce((SELECT jsonb_agg(jsonb_build_object('import_id',i.id,'title',i.raw_title,
                     'raga',i.raw_raga,'tala',i.raw_tala,'source_url',i.source_key))
                  FROM imported_krithis i WHERE i.mapped_krithi_id=k.id), '[]'::jsonb) AS imports
    FROM krithis k
    JOIN talas t ON t.id=k.tala_id
    JOIN composers c ON c.id=k.composer_id
    LEFT JOIN ragas r ON r.id=k.primary_raga_id
    WHERE t.name='Unknown'
) inventory;
