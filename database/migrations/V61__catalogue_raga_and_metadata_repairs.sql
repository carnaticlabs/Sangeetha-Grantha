-- corpus-data-fix: allow
-- TRACK-144: catalogue repairs from the September 2026 Rasika field notes.
-- Lyric orthography for Marugelara's pallavi follows the stored Latin variant.

-- Permanently ensure search_path is set on raga matching functions for clean pg_restore/pg_dump compatibility.
ALTER FUNCTION strip_diacritics(text) SET search_path = public, pg_temp;
ALTER FUNCTION raga_match_key(text) SET search_path = public, pg_temp;

-- Tyagaraja's Marugelara: ensure raga is Jayanthashrī (jayantaSrI)
UPDATE krithi_ragas
SET raga_id = (SELECT id FROM ragas WHERE name = 'Jayanthashrī')
WHERE krithi_id = (SELECT id FROM krithis WHERE title = 'Marugelaraa')
  AND raga_id = (SELECT id FROM ragas WHERE name = 'Jayanthasena');

UPDATE krithis
SET primary_raga_id = (SELECT id FROM ragas WHERE name = 'Jayanthashrī'),
    updated_at = timezone('UTC', now())
WHERE title = 'Marugelaraa'
  AND primary_raga_id = (SELECT id FROM ragas WHERE name = 'Jayanthasena');

-- dIna janAvana was filed under one raga named "bhUpALaM - bhauLi". Bhoopalam and Bowli are different ragas.
UPDATE krithi_ragas
SET raga_id = (SELECT id FROM ragas WHERE name = 'Bhoopālam')
WHERE krithi_id = (SELECT id FROM krithis WHERE title = 'dIna janAvana')
  AND raga_id = (SELECT id FROM ragas WHERE name = 'bhUpALaM - bhauLi');

UPDATE krithis
SET primary_raga_id = (SELECT id FROM ragas WHERE name = 'Bhoopālam'),
    updated_at = timezone('UTC', now())
WHERE title = 'dIna janAvana'
  AND primary_raga_id = (SELECT id FROM ragas WHERE name = 'bhUpALaM - bhauLi');

-- Set Adi tala for Marugelaraa if still Unknown
UPDATE krithis
SET tala_id = (SELECT id FROM talas WHERE name = 'Adi'),
    updated_at = timezone('UTC', now())
WHERE title = 'Marugelaraa'
  AND tala_id = (SELECT id FROM talas WHERE name = 'Unknown');

-- Set Rupaka tala directly for Sri Narada Nada (Kanada) if still Unknown
UPDATE krithis
SET tala_id = (SELECT id FROM talas WHERE name = 'Rupaka'),
    updated_at = timezone('UTC', now())
WHERE title = 'Sri Narada Nada'
  AND tala_id = (SELECT id FROM talas WHERE name = 'Unknown');

-- Marugelara had anupallavi and charanam only. Shift them down and store the pallavi.
UPDATE krithi_sections
SET order_index = 3, updated_at = timezone('UTC', now())
WHERE krithi_id = (SELECT id FROM krithis WHERE title = 'Marugelaraa')
  AND section_type = 'CHARANAM'
  AND order_index = 2;

UPDATE krithi_sections
SET order_index = 2, updated_at = timezone('UTC', now())
WHERE krithi_id = (SELECT id FROM krithis WHERE title = 'Marugelaraa')
  AND section_type = 'ANUPALLAVI'
  AND order_index = 1;

INSERT INTO krithi_sections (krithi_id, section_type, order_index)
SELECT id, 'PALLAVI', 1
FROM krithis
WHERE title = 'Marugelaraa'
  AND NOT EXISTS (
      SELECT 1 FROM krithi_sections s
      WHERE s.krithi_id = krithis.id AND s.section_type = 'PALLAVI'
  );

INSERT INTO krithi_lyric_sections (lyric_variant_id, section_id, text, normalized_text)
SELECT v.id, s.id, 'marug(E)lar(A) O rAghav(A)', 'marugelara o raghava'
FROM krithi_lyric_variants v
JOIN krithi_sections s ON s.krithi_id = v.krithi_id AND s.section_type = 'PALLAVI'
WHERE v.krithi_id = (SELECT id FROM krithis WHERE title = 'Marugelaraa')
  AND NOT EXISTS (
      SELECT 1 FROM krithi_lyric_sections ls
      WHERE ls.lyric_variant_id = v.id AND ls.section_id = s.id
  );

UPDATE krithi_lyric_variants
SET lyrics = 'marug(E)lar(A) O rAghav(A)' || E'\n\n' || lyrics,
    updated_at = timezone('UTC', now())
WHERE krithi_id = (SELECT id FROM krithis WHERE title = 'Marugelaraa')
  AND lyrics NOT LIKE 'marug(E)lar(A)%';

-- Inline source URLs were drawn as links inside the sahitya (Meenakshi Me Mudam anupallavi).
UPDATE krithi_lyric_sections
SET text = regexp_replace(text, '\s*\(https?://[^)]*\)', '', 'g'),
    updated_at = timezone('UTC', now())
WHERE text ~ 'https?://';

UPDATE krithi_lyric_variants
SET lyrics = regexp_replace(lyrics, '\s*\(https?://[^)]*\)', '', 'g'),
    updated_at = timezone('UTC', now())
WHERE lyrics ~ 'https?://';

UPDATE search_documents
SET original_content = regexp_replace(original_content, '\s*\(https?://[^)]*\)', '', 'g'),
    indexed_content = regexp_replace(indexed_content, '\s*\(https?://[^)]*\)', '', 'g'),
    updated_at = clock_timestamp()
WHERE original_content ~ 'https?://' OR indexed_content ~ 'https?://';

-- Sumadyuti is Dikshitar's name for Simhendramadhyamam.
INSERT INTO raga_relations (from_raga_id, to_raga_id, relation, source)
SELECT LEAST(a.id, b.id), GREATEST(a.id, b.id),
       'nomenclature_equivalent', 'https://guruguha.org/wp-content/uploads/2022/03/ssp_7to12.pdf — Sumadyuti, raganga 57'
FROM ragas a
JOIN ragas b ON TRUE
WHERE a.name = 'sumadyuti'
  AND b.name = 'Simhendramadhyamam'
ON CONFLICT (from_raga_id, to_raga_id, relation) DO NOTHING;

INSERT INTO audit_log (entity_table, entity_id, action, diff)
SELECT 'krithis', id, 'UPDATE',
       '{"reason": "TRACK-144: Adi tala, Jayanthashri raga, pallavi for Marugelaraa"}'::jsonb
FROM krithis WHERE title = 'Marugelaraa';

INSERT INTO audit_log (entity_table, entity_id, action, diff)
SELECT 'krithis', id, 'UPDATE',
       '{"reason": "TRACK-144: Rupaka tala for Sri Narada Nada"}'::jsonb
FROM krithis WHERE title = 'Sri Narada Nada';

INSERT INTO audit_log (entity_table, entity_id, action, diff)
SELECT 'krithis', id, 'UPDATE',
       '{"reason": "TRACK-144: raga split from bhUpALaM - bhauLi onto Bhoopalam"}'::jsonb
FROM krithis WHERE title = 'dIna janAvana';

INSERT INTO audit_log (entity_table, entity_id, action, diff)
VALUES (
    'krithi_lyric_sections',
    NULL,
    'UPDATE',
    '{"reason": "TRACK-144: remove parenthetical source URLs from sahitya"}'::jsonb
);
