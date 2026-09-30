-- Repeatable reference seed for canonical Carnatic cycles and thematic groupings (ADR-013 / TRACK-108).
-- Tag catalog for the Dikshitar cycles, Tyagaraja Pancharatnam, and Syama Sastri Swarajathis.
-- TRACK-145: Dikshitar krithi_tags links are no longer heuristic title patterns.
-- Reviewed memberships and the purge of historical canonical_cycle rows live in R__seed_09.

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

-- Tyagaraja Pancharatna Krithis (non-Dikshitar; retained)
INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
SELECT k.id, '01920000-0000-7000-8000-000000000004'::uuid, 'canonical_cycle', 100
FROM krithis k
WHERE lower(k.title) SIMILAR TO '%(jagadananda karaka|duduku gala|sadhincene|kana kana rucira|endaro mahaanubhaavulu)%'
  AND k.composer_id = (SELECT id FROM composers WHERE lower(name) LIKE '%tyagaraja%' LIMIT 1)
ON CONFLICT (krithi_id, tag_id) DO NOTHING;

-- Syama Sastri Swarajathi Ratnatrayam (non-Dikshitar; retained)
DELETE FROM krithi_tags
WHERE tag_id = '01920000-0000-7000-8000-000000000007'::uuid;

INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
SELECT k.id, '01920000-0000-7000-8000-000000000007'::uuid, 'canonical_cycle', 100
FROM krithis k
JOIN composers c ON k.composer_id = c.id
WHERE (lower(c.name) LIKE '%syama%' OR lower(c.name) LIKE '%sastri%')
  AND lower(k.title) SIMILAR TO '%(kamakshi anudinamu|rave hima giri|kamakshi ni pada)%'
ON CONFLICT (krithi_id, tag_id) DO NOTHING;
