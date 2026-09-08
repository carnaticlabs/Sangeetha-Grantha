-- Repeatable reference seed for canonical Carnatic cycles and thematic groupings (ADR-013 / TRACK-108).
-- Seeds authoritative tags for Pancha Bhuta Sthala, Kamalamba Navavarnam, Navagraha, 
-- Tyagaraja Pancharatnam, Abhayamba Vibhakti, Nilotpalamba Vibhakti, and Syama Sastri Swarajathis.

-- 1. Insert Tags
INSERT INTO tags (id, category, slug, display_name_en, description_en, created_at)
VALUES
(
    '01920000-0000-7000-8000-000000000001',
    'KSHETRA',
    'pancha-bhuta-sthala',
    'Pancha Bhuta Sthala Krithis',
    'Compositions by Muthuswami Dikshitar dedicated to the five elemental Lingams of Shiva: Earth (Prithvi at Kanchipuram - Chintaya Ma Kanda), Water (Appu at Tiruvanaikkaval - Jambu Pate), Fire (Tejas at Tiruvannamalai - Arunachala Natham), Wind (Vayu at Kalahasti - Sri Kalahastisa), and Space (Akasha at Chidambaram - Ananda Natana Prakasam).',
    NOW()
),
(
    '01920000-0000-7000-8000-000000000002',
    'STOTRA_STYLE',
    'kamalamba-navavarnam',
    'Kamalamba Navavarnam',
    'The revered 11-composition cycle by Muthuswami Dikshitar on Goddess Kamalamba of Tiruvarur across the nine vibhaktis of Sri Chakra (including Dhyana and Mangala krithis).',
    NOW()
),
(
    '01920000-0000-7000-8000-000000000003',
    'STOTRA_STYLE',
    'navagraha-krithis',
    'Navagraha Krithis',
    'The nine celestial planetary compositions by Muthuswami Dikshitar in praise of the Navagrahas (Surya, Chandra, Angaraka, Budha, Brihaspati, Sukra, Sani, Rahu, and Ketu).',
    NOW()
),
(
    '01920000-0000-7000-8000-000000000004',
    'STOTRA_STYLE',
    'tyagaraja-pancharatnam',
    'Tyagaraja Ghana Raga Pancharatnam',
    'The five gem compositions by Saint Tyagaraja set in the five major ghana ragas: Nattai, Gaula, Arabhi, Varali, and Sri.',
    NOW()
),
(
    '01920000-0000-7000-8000-000000000005',
    'STOTRA_STYLE',
    'abhayamba-vibhakti',
    'Abhayamba Vibhakti Krithis',
    'The vibhakti compositions by Muthuswami Dikshitar on Goddess Abhayamba of Mayuranatha Temple, Mayiladuthurai.',
    NOW()
),
(
    '01920000-0000-7000-8000-000000000006',
    'STOTRA_STYLE',
    'nilotpalamba-vibhakti',
    'Nilotpalamba Vibhakti Krithis',
    'The vibhakti cycle composed by Muthuswami Dikshitar on Goddess Nilotpalamba of Tiruvarur.',
    NOW()
),
(
    '01920000-0000-7000-8000-000000000007',
    'STOTRA_STYLE',
    'syama-sastri-swarajathi-ratnatrayam',
    'Syama Sastri Swarajathi Ratnatrayam',
    'The three magnificent Swarajathi gems composed by Syama Sastri in Bhairavi, Todi, and Yadukulakambhoji ragas.',
    NOW()
)
ON CONFLICT (slug) DO UPDATE SET
    display_name_en = EXCLUDED.display_name_en,
    description_en = EXCLUDED.description_en;

-- 2. Link Pancha Bhuta Sthala Krithis
INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
SELECT k.id, '01920000-0000-7000-8000-000000000001'::uuid, 'canonical_cycle', 100
FROM krithis k
WHERE (
    lower(k.title) SIMILAR TO '%(cintaya m[a|A] kanda|jamb[u|U] pat[e|E]|aru[n|N][a|A]cala n[a|A]tha|ananda na[t|T]ana prak[a|A]sa)%'
    OR lower(k.title) LIKE '%kalahasti%'
    OR lower(k.title) LIKE '%kalahastisa%'
    OR lower(k.title) LIKE '%kalahastI%'
)
AND k.composer_id = (SELECT id FROM composers WHERE lower(name) LIKE '%dikshitar%' LIMIT 1)
ON CONFLICT (krithi_id, tag_id) DO NOTHING;

-- 3. Link Kamalamba Navavarnam Krithis
INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
SELECT k.id, '01920000-0000-7000-8000-000000000002'::uuid, 'canonical_cycle', 100
FROM krithis k
WHERE lower(k.title) LIKE '%kamalamb%'
  AND k.composer_id = (SELECT id FROM composers WHERE lower(name) LIKE '%dikshitar%' LIMIT 1)
ON CONFLICT (krithi_id, tag_id) DO NOTHING;

-- 4. Link Navagraha Krithis
INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
SELECT k.id, '01920000-0000-7000-8000-000000000003'::uuid, 'canonical_cycle', 100
FROM krithis k
WHERE lower(k.title) SIMILAR TO '%(surya murte|candram bhaja|angarakam asrayamyaham|budhamasrayami|brhaspate tara pate|sukra bhagavantam|divakara tanujam|smaramyaham sada|maha suram ketum)%'
  AND k.composer_id = (SELECT id FROM composers WHERE lower(name) LIKE '%dikshitar%' LIMIT 1)
ON CONFLICT (krithi_id, tag_id) DO NOTHING;

-- 5. Link Tyagaraja Pancharatna Krithis
INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
SELECT k.id, '01920000-0000-7000-8000-000000000004'::uuid, 'canonical_cycle', 100
FROM krithis k
WHERE lower(k.title) SIMILAR TO '%(jagadananda karaka|duduku gala|sadhincene|kana kana rucira|endaro mahaanubhaavulu)%'
  AND k.composer_id = (SELECT id FROM composers WHERE lower(name) LIKE '%tyagaraja%' LIMIT 1)
ON CONFLICT (krithi_id, tag_id) DO NOTHING;

-- 6. Link Abhayamba Vibhakti Krithis
INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
SELECT k.id, '01920000-0000-7000-8000-000000000005'::uuid, 'canonical_cycle', 100
FROM krithis k
WHERE lower(k.title) LIKE '%abhayamb%'
  AND k.composer_id = (SELECT id FROM composers WHERE lower(name) LIKE '%dikshitar%' LIMIT 1)
ON CONFLICT (krithi_id, tag_id) DO NOTHING;

-- 7. Link Nilotpalamba Vibhakti Krithis
INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
SELECT k.id, '01920000-0000-7000-8000-000000000006'::uuid, 'canonical_cycle', 100
FROM krithis k
WHERE lower(k.title) LIKE '%nilotpal%'
  AND k.composer_id = (SELECT id FROM composers WHERE lower(name) LIKE '%dikshitar%' LIMIT 1)
ON CONFLICT (krithi_id, tag_id) DO NOTHING;

-- 8. Link Syama Sastri Swarajathi Ratnatrayam
DELETE FROM krithi_tags
WHERE tag_id = '01920000-0000-7000-8000-000000000007'::uuid;

INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
SELECT k.id, '01920000-0000-7000-8000-000000000007'::uuid, 'canonical_cycle', 100
FROM krithis k
JOIN composers c ON k.composer_id = c.id
WHERE (lower(c.name) LIKE '%syama%' OR lower(c.name) LIKE '%sastri%')
  AND lower(k.title) SIMILAR TO '%(kamakshi anudinamu|rave hima giri|kamakshi ni pada)%'
ON CONFLICT (krithi_id, tag_id) DO NOTHING;
