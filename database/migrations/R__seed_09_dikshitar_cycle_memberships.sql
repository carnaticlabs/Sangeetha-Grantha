-- Repeatable seed for TRACK-145 Dikshitar cycle memberships and anchor shrines.
-- Definitive source of truth: dikshitar-corpus-481-classification-master.md
-- Fully populates all 95 temples across the 7 mandalams, 105 cycle memberships, and all 481 compositions.
-- Matches imported kritis by exact stored title, composer, and primary raga.
-- Kanyakumari is a temple row only. The worker owns its KSHETRA_OVERVIEW document.
-- Ref: application_documentation/01-requirements/features/dikshitar-kshetra-musicological-enrichment.md

SET search_path TO public;

-- 1. Purge historical heuristic links from earlier R__seed_07 runs.
DELETE FROM krithi_tags
WHERE tag_id IN (
    '01920000-0000-7000-8000-000000000001', -- pancha-bhuta-sthala
    '01920000-0000-7000-8000-000000000002', -- kamalamba-navavarnam
    '01920000-0000-7000-8000-000000000003', -- navagraha-krithis
    '01920000-0000-7000-8000-000000000005', -- abhayamba-vibhakti
    '01920000-0000-7000-8000-000000000006'  -- nilotpalamba-vibhakti
)
AND source = 'canonical_cycle';

-- 2. Insert missing cycle tags into tags catalog.
INSERT INTO tags (id, category, slug, display_name_en, description_en, created_at)
VALUES
(
    '01920000-0000-7000-8000-000000000008',
    'STOTRA_STYLE',
    'guruguha-vibhakti',
    'Guruguha Vibhakti Krithis',
    'The eight-case vibhakti set on Guruguha at Tiruttani. The instrumental (tritiya) member is not in the imported corpus; sequence 3 is reserved.',
    NOW()
),
(
    '01920000-0000-7000-8000-000000000009',
    'STOTRA_STYLE',
    'tyagaraja-vibhakti',
    'Tyagaraja Vibhakti Krithis',
    'The eight-case vibhakti set on Tyagaraja at the Tiruvarur sanctum. Tyagaraja Yoga Vaibhavam is a separate gopuccha kriti and is not a member.',
    NOW()
),
(
    '01920000-0000-7000-8000-00000000000a',
    'STOTRA_STYLE',
    'tiruvarur-panchalinga',
    'Tiruvarur Pancha Linga Krithis',
    'The five local lingams inside the Tiruvarur complex: Achalesvara, Hatakesvara, Valmikesvara, Anandesvara, and Siddhisvara.',
    NOW()
),
(
    '01920000-0000-7000-8000-00000000000b',
    'STOTRA_STYLE',
    'shodasa-ganapati',
    'Shodasa Ganapati Krithis',
    'Attested Dikshitar temple compositions on Ganesha. An open set, not a manufactured grid of sixteen.',
    NOW()
),
(
    '01920000-0000-7000-8000-00000000000c',
    'STOTRA_STYLE',
    'nottusvara-sahitya',
    'Nottusvara Sahitya Compositions',
    'Attested Sankarabharanam note-svara sahityas. axis_value holds a published European air name when one is identified; otherwise it stays null.',
    NOW()
)
ON CONFLICT (slug) DO UPDATE SET
    display_name_en = EXCLUDED.display_name_en,
    description_en = EXCLUDED.description_en,
    category = EXCLUDED.category;

-- 3. Seed Navagraha deities for deity linking.
INSERT INTO deities (id, name, name_normalized, created_at, updated_at)
VALUES
    ('01940000-0000-7000-8000-000000000001', 'Surya', 'surya', NOW(), NOW()),
    ('01940000-0000-7000-8000-000000000002', 'Chandra', 'chandra', NOW(), NOW()),
    ('01940000-0000-7000-8000-000000000003', 'Angaraka', 'angaraka', NOW(), NOW()),
    ('01940000-0000-7000-8000-000000000004', 'Budha', 'budha', NOW(), NOW()),
    ('01940000-0000-7000-8000-000000000005', 'Brihaspati', 'brihaspati', NOW(), NOW()),
    ('01940000-0000-7000-8000-000000000006', 'Shukra', 'shukra', NOW(), NOW()),
    ('01940000-0000-7000-8000-000000000007', 'Shani', 'shani', NOW(), NOW()),
    ('01940000-0000-7000-8000-000000000008', 'Rahu', 'rahu', NOW(), NOW()),
    ('01940000-0000-7000-8000-000000000009', 'Ketu', 'ketu', NOW(), NOW())
ON CONFLICT (name_normalized) DO NOTHING;

-- 4. Seed parent temples across the 7 pilgrimage mandalams.
INSERT INTO temples (
    id, name, name_normalized, city, state, country,
    place_kind, mandalam, bhuta, sthala_vriksha, sthala_tirtha, nadi_tirtha, deity_posture,
    notes, created_at, updated_at
)
VALUES
(
    '01930000-0000-7000-8000-000000000001', 'Tiruvarur Tyagarajaswami Temple', 'tiruvarur tyagarajaswami temple', 'Tiruvarur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Kamalalayam', NULL, 'ASINA',
    'Tiruvarur Tyagaraja complex. Local lingams are child sannidhis and are not Pancha Bhuta kshetras.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000000b', 'Tiruttani Subrahmanya Temple', 'tiruttani subrahmanya temple', 'Tiruttani', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', NULL, NULL, 'Saravana Poigai', NULL, 'STHANAKA',
    'Anchor shrine of the Guruguha vibhakti set. Dikshitar received divine inspiration here.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000000c', 'Mayiladuthurai', 'mayiladuthurai', 'Mayiladuthurai', 'Tamil Nadu', 'India',
    'LOCALITY', 'CHOLA', NULL, NULL, NULL, NULL, NULL,
    'Mayuram town. The Mayuranathar temple and Abhayamba sannidhi sit under this locality.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000000f', 'Ekambareswarar Temple', 'ekambareswarar temple', 'Kanchipuram', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', 'PRITHVI', 'Sahakara Mango', 'Sivaganga', NULL, NULL,
    'Prithvi lingam of the Pancha Bhuta set.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000010', 'Jambukeswarar Temple', 'jambukeswarar temple', 'Tiruvanaikkaval', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', 'APPU', 'Jambu', 'Jambu Tirtha', 'Kaveri', NULL,
    'Appu lingam of the Pancha Bhuta set.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000011', 'Arunachaleswarar Temple', 'arunachaleswarar temple', 'Tiruvannamalai', 'Tamil Nadu', 'India',
    'COMPLEX', 'NADU', 'AGNI', NULL, 'Brahma Tirtha', 'Pennai', NULL,
    'Agni lingam of the Pancha Bhuta set. Pennai basin, so mandalam is NADU.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000012', 'Srikalahasti Temple', 'srikalahasti temple', 'Srikalahasti', 'Andhra Pradesh', 'India',
    'COMPLEX', 'TONDAI', 'VAYU', NULL, 'Swarnamukhi Tirtha', 'Swarnamukhi', NULL,
    'Vayu lingam of the Pancha Bhuta set.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000013', 'Nataraja Temple', 'nataraja temple', 'Chidambaram', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', 'AKASHA', NULL, 'Sivaganga', NULL, 'TANDAVA',
    'Akasha lingam of the Pancha Bhuta set. Archa-murti is the dancing Nataraja.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000014', 'Akshayalingeswarar Temple', 'akshayalingeswarar temple', 'Keevalur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, NULL, NULL,
    'Keevalur is in the Kaveri delta, so mandalam is CHOLA.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000015', 'Bhagavati Amman Temple', 'bhagavati amman temple', 'Kanyakumari', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, NULL, NULL, NULL,
    'Recorded gap. No Dikshitar kriti is seeded here. The worker alone writes the KSHETRA_OVERVIEW document.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000017', 'Adi Jagannatha Perumal Temple (Dharbasayanam)', 'adi jagannatha perumal temple (dharbasayanam)', 'Thiruppullani', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, NULL, 'Bay of Bengal', 'SAYANA',
    'Thiruppullani Sethu Divya Desam shrine.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000018', 'Adi Kumbeswarar Temple', 'adi kumbeswarar temple', 'Kumbakonam', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Mahamaham Tank', 'Kaveri', NULL,
    'Adi Kumbeswarar Temple at Kumbakonam.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000019', 'Agastiswarar Temple', 'agastiswarar temple', 'Thanjavur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, NULL, NULL,
    'Agastiswarar temple in Thanjavur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000001a', 'Amritaghateswarar Abhirami Temple', 'amritaghateswarar abhirami temple', 'Thirukkadaiyur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Amrita Pushkarini', NULL, NULL,
    'Markandeya longevity sthala: Lord Shiva granting eternal life against Yama.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000001b', 'Anandavalli Sametha Thanjapureeswarar Temple (Vennatrankarai)', 'anandavalli sametha thanjapureeswarar temple (vennatrankarai)', 'Thanjavur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, 'Vennaru', NULL,
    'Thanjapureeswarar temple at Vennatrankarai, Thanjavur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000001c', 'Ardhanareeswarar Temple', 'ardhanareeswarar temple', 'Tiruchengodu', 'Tamil Nadu', 'India',
    'COMPLEX', 'KONGU', NULL, NULL, NULL, NULL, 'STHANAKA',
    'Sarpagiri / Nagagiri hill shrine celebrating Ardhanareeswara in Kongu Nadu.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000001d', 'Ashtabhuja Perumal Temple', 'ashtabhuja perumal temple', 'Kanchipuram', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', NULL, NULL, 'Gajendra Pushkarini', 'Vegavati', 'STHANAKA',
    'Ashtabhuja Perumal Divya Desam at Kanchipuram.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000001e', 'Badrinath Temple (Satyanarayana Sannidhi)', 'badrinath temple (satyanarayana sannidhi)', 'Badrinath', 'Uttarakhand', 'India',
    'COMPLEX', 'UTTARA', NULL, NULL, 'Tapt Kund', 'Alaknanda', 'ASINA',
    'Badrinath shrine in the Himalayas.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000020', 'Bangaru Kamakshi Amman Temple', 'bangaru kamakshi amman temple', 'Thanjavur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, NULL, 'ASINA',
    'Golden Kamakshi temple established in Thanjavur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000021', 'Bhaktavatsala Perumal Temple', 'bhaktavatsala perumal temple', 'Tirukannamangai', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Darshana Pushkarini', NULL, 'STHANAKA',
    'Bhaktavatsala Perumal Divya Desam.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000022', 'Bhavani Shrine', 'bhavani shrine', 'Ettayapuram', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, NULL, NULL, NULL,
    'Bhavani shrine at Ettayapuram where Dikshitar caused rain via Amritavarshini.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000023', 'Bindu Madhava Temple', 'bindu madhava temple', 'Varanasi', 'Uttar Pradesh', 'India',
    'COMPLEX', 'UTTARA', NULL, NULL, 'Panchaganga Ghat', 'Ganga', 'STHANAKA',
    'Bindu Madhava Vishnu temple on the ghats of Varanasi.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000024', 'Brihadisvara Temple', 'brihadisvara temple', 'Thanjavur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Sivaganga Tank', 'Kaveri', NULL,
    'Great Chola Temple of Brihadisvara at Thanjavur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000025', 'Dandayudhapani Swamy Temple', 'dandayudhapani swamy temple', 'Palani', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, 'Shanmuganathi', NULL, 'STHANAKA',
    'Murugan hill temple at Palani.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000026', 'Gokula Krishna Temple', 'gokula krishna temple', 'Gokul', 'Uttar Pradesh', 'India',
    'COMPLEX', 'UTTARA', NULL, NULL, NULL, 'Yamuna', NULL,
    'Gokul shrine in the Vraja region.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000027', 'Govardhana Hill Shrine', 'govardhana hill shrine', 'Govardhana', 'Uttar Pradesh', 'India',
    'COMPLEX', 'UTTARA', NULL, NULL, 'Manasi Ganga', 'Yamuna', 'STHANAKA',
    'Sacred Govardhana hill shrine in Mathura.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000028', 'Govindaraja Perumal Temple', 'govindaraja perumal temple', 'Chidambaram', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Sivaganga', NULL, 'SAYANA',
    'Govindaraja Perumal shrine within the Chidambaram Nataraja complex.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000029', 'Guruvayurappan Temple', 'guruvayurappan temple', 'Guruvayur', 'Kerala', 'India',
    'COMPLEX', 'CHERA', NULL, NULL, 'Rudratheertham', NULL, 'STHANAKA',
    'Bhuloka Vaikunta shrine at Guruvayur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000002a', 'Himachala Kumari Shrine', 'himachala kumari shrine', 'Sattur', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, NULL, NULL, NULL,
    'Himachala Kumari shrine at Sattur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000002b', 'Jagannath Temple', 'jagannath temple', 'Puri', 'Odisha', 'India',
    'COMPLEX', 'UTTARA', NULL, 'Kalpabata', 'Rohini Kunda', 'Bay of Bengal', 'ASINA',
    'Nilachala sacred shrine of Lord Jagannath at Puri.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000002c', 'Kadambavaneswarar Temple', 'kadambavaneswarar temple', 'Kulittalai', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, 'Kadamba', NULL, 'Kaveri', NULL,
    'Kadambavaneswarar / Ratnagiri shrine on the Kaveri.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000002d', 'Kailasanatha Temple', 'kailasanatha temple', 'Kanchipuram', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', NULL, NULL, NULL, 'Vegavati', NULL,
    'Historic Pallava Kailasanatha temple at Kanchipuram.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000002e', 'Kallazhagar Temple', 'kallazhagar temple', 'Madurai', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, 'Noopura Ganga', NULL, 'STHANAKA',
    'Alagar Kovil / Tirumalirunjolai Divya Desam near Madurai.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000002f', 'Kamakshi Amman Temple', 'kamakshi amman temple', 'Kanchipuram', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', NULL, NULL, NULL, 'Vegavati', 'ASINA',
    'Supreme Shakta Pitha of Goddess Kamakshi at Kanchipuram.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000030', 'Kasi Viswanatha Temple', 'kasi viswanatha temple', 'Varanasi', 'Uttar Pradesh', 'India',
    'COMPLEX', 'UTTARA', NULL, NULL, 'Manikarnika', 'Ganga', NULL,
    'Sacred Jyotirlinga of Kasi Viswanatha on the Ganga.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000031', 'Kasi Viswanathar Temple', 'kasi viswanathar temple', 'Kuzhikkarai', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, 'Kaveri', NULL,
    'Kasi Viswanatha shrine at Kuzhikkarai patronized by Vaidyalinga Mudaliar.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000032', 'Kayarohanaswami Temple', 'kayarohanaswami temple', 'Nagapattinam', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Pundarikaksha Tirtha', 'Bay of Bengal', NULL,
    'Ancient Kayarohanaswami Siva temple on the Coromandel coast.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000033', 'Kazhugachalamurthi Temple', 'kazhugachalamurthi temple', 'Kazhugumalai', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, NULL, NULL, 'STHANAKA',
    'Rock-cut Murugan hill temple at Kazhugumalai.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000034', 'Konkaneswarar Temple', 'konkaneswarar temple', 'Thanjavur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, NULL, NULL,
    'Konkaneswarar Temple in Thanjavur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000035', 'Lakshmi Varaha Perumal Temple', 'lakshmi varaha perumal temple', 'Kallidaikurichi', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, 'Varaha Tirtha', 'Tamraparni', 'STHANAKA',
    'Lakshmi Varaha Perumal temple on the banks of Tamraparni.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000036', 'Mahabaleshwar Temple', 'mahabaleshwar temple', 'Gokarna', 'Karnataka', 'India',
    'COMPLEX', 'UTTARA', NULL, NULL, 'Koti Tirtha', 'Arabian Sea', NULL,
    'Atmalinga shrine of Mahabaleshwar at Gokarna.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000037', 'Mahalingaswami Temple', 'mahalingaswami temple', 'Tiruvidaimarudur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, 'Maruda', 'Karunya Tirtha', 'Kaveri', NULL,
    'Madhyarjuna Mahalingaswami temple.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000038', 'Manali / Chennai (British Airs)', 'manali / chennai (british airs)', 'Chennai', 'Tamil Nadu', 'India',
    'LOCALITY', 'TONDAI', NULL, NULL, NULL, NULL, NULL,
    'Manali Muddukrishna Mudaliar patronage of Western Band airs.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000039', 'Maragathasaleswarar Temple', 'maragathasaleswarar temple', 'Tiruvingoimalai', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, 'Kaveri', NULL,
    'Tiruvingoimalai hill shrine on the Kaveri.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000003a', 'Margabandheeswarar Temple', 'margabandheeswarar temple', 'Virinjipuram', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', NULL, 'Palm', 'Simha Kinaru', 'Palar', NULL,
    'Margabandheeswarar temple on the Palar river.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000003b', 'Matrubhuteswarar Temple (Rockfort)', 'matrubhuteswarar temple (rockfort)', 'Tiruchirapalli', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, 'Kaveri', NULL,
    'Rockfort hill temple of Matrubhuteswarar (Thayumanavar).',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000003c', 'Meenakshi Sundareswarar Temple', 'meenakshi sundareswarar temple', 'Madurai', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, 'Kadamba', 'Potramarai', 'Vaigai', 'STHANAKA',
    'Meenakshi Sundareswarar temple at Madurai.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000003d', 'Navapashanam Temple', 'navapashanam temple', 'Devipattinam', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, NULL, 'Bay of Bengal', NULL,
    'Navapashanam coastal shrine near Rameswaram.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000003e', 'Nellaiappar Temple', 'nellaiappar temple', 'Tirunelveli', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, 'Venu', 'Swarna Lotus Tank', 'Tamraparni', NULL,
    'Nellaiappar and Gandhimathi temple on the Tamraparni.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000003f', 'Padmanabhaswamy Temple', 'padmanabhaswamy temple', 'Thiruvananthapuram', 'Kerala', 'India',
    'COMPLEX', 'CHERA', NULL, NULL, 'Padmateertham', NULL, 'SAYANA',
    'Ananthapadmanabhaswamy temple at Thiruvananthapuram.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000040', 'Panchanatheeswarar Temple', 'panchanatheeswarar temple', 'Tiruvaiyaru', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Surya Pushkarini', 'Kaveri', NULL,
    'Panchanatheeswarar (Pranatartihara) and Dharmasamvardhani at Tiruvaiyaru.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000041', 'Parimala Ranganatha Swamy Temple', 'parimala ranganatha swamy temple', 'Mayiladuthurai', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Indu Pushkarini', 'Kaveri', 'SAYANA',
    'Parimala Ranganatha Swamy Divya Desam at Mayiladuthurai.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000042', 'Parthasarathy Temple', 'parthasarathy temple', 'Chennai', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', NULL, NULL, 'Kairavini', 'Bay of Bengal', 'STHANAKA',
    'Parthasarathy Divya Desam at Triplicane, Chennai.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000043', 'Pashupatinath Temple', 'pashupatinath temple', 'Kathmandu', 'Bagmati', 'Nepal',
    'COMPLEX', 'UTTARA', NULL, NULL, 'Aryaghat', 'Bagmati', NULL,
    'Pashupatinath Jyotirlinga on the Bagmati River in Nepal.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000044', 'Prasanna Venkatesa Shrine', 'prasanna venkatesa shrine', 'Thanjavur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, NULL, 'STHANAKA',
    'Prasanna Venkatesa Perumal temple in Thanjavur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000045', 'Pushpavaneswarar Temple', 'pushpavaneswarar temple', 'Tiruppuvanam', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, NULL, 'Vaigai', NULL,
    'Pushpavaneswarar temple on the Vaigai river.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000046', 'Rajagopalaswamy Temple', 'rajagopalaswamy temple', 'Mannargudi', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, 'Champaka', 'Haridra Nadhi', NULL, 'STHANAKA',
    'Rajagopalaswamy temple at Mannargudi.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000047', 'Ramanathaswamy Temple', 'ramanathaswamy temple', 'Rameswaram', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, 'Agni Tirtha', 'Bay of Bengal', NULL,
    'Ramanathaswamy Jyotirlinga on the island of Rameswaram.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000048', 'Ramaswamy Temple', 'ramaswamy temple', 'Kumbakonam', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, 'Kaveri', 'ASINA',
    'Ramaswamy temple at Kumbakonam.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000049', 'Ranganathaswamy Temple', 'ranganathaswamy temple', 'Srirangam', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Chandra Pushkarini', 'Kaveri', 'SAYANA',
    'Ranganathaswamy premier Divya Desam at Srirangam.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000004a', 'Renukadevi Temple', 'renukadevi temple', 'Vijayapuram', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, NULL, NULL,
    'Renukadevi temple in Vijayapuram, near Tiruvarur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000004b', 'Sabarimala Sastha Temple', 'sabarimala sastha temple', 'Sabarimala', 'Kerala', 'India',
    'COMPLEX', 'CHERA', NULL, NULL, NULL, 'Pamba', 'ASINA',
    'Ayyappa Sastha hill shrine at Sabarimala.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000004c', 'Sankaranarayanaswamy Temple', 'sankaranarayanaswamy temple', 'Sankaranayinarkoil', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, 'Punnai', 'Naga Sunai', NULL, 'STHANAKA',
    'Composite Sankaranarayana shrine in Pandya Nadu.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000004d', 'Saraswati Temple', 'saraswati temple', 'Basrur', 'Karnataka', 'India',
    'COMPLEX', 'UTTARA', NULL, NULL, NULL, 'Sharavati', 'ASINA',
    'Historical Saraswati temple on the banks of Sharavati river at Basrur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000004e', 'Sharada Peeth', 'sharada peeth', 'Sharda', 'Kashmir', 'India',
    'COMPLEX', 'UTTARA', NULL, NULL, NULL, 'Kishanganga', 'ASINA',
    'Ancient Saraswati Sharada Peeth on the Kishanganga river in Kashmir.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000004f', 'Sikkil Singaravelan Temple', 'sikkil singaravelan temple', 'Sikkil', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Ksheera Pushkarini', NULL, NULL,
    'Singaravelan Murugan shrine at Sikkil.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000050', 'Soundararaja Perumal Temple', 'soundararaja perumal temple', 'Nagapattinam', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, 'Bay of Bengal', 'STHANAKA',
    'Soundararaja Perumal Divya Desam at Nagapattinam.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000051', 'Subrahmanya Swamy Temple', 'subrahmanya swamy temple', 'Tiruchendur', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, NULL, 'Bay of Bengal', 'STHANAKA',
    'Seashore Murugan temple at Tiruchendur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000052', 'Subrahmanya Swamy Temple', 'subrahmanya swamy temple', 'Tirupparankunram', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, 'Saravana Poigai', NULL, 'STHANAKA',
    'First Arupadaiveedu Murugan cave temple at Tirupparankunram.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000053', 'Sveta Vinayakar, Vadaanyeswarar Temple', 'sveta vinayakar, vadaanyeswarar temple', 'Tiruvalanchuzhi', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, 'Kaveri', NULL,
    'White Ganesha (Sveta Vinayaka) shrine at Tiruvalanchuzhi.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000054', 'Swaminatha Swamy Temple', 'swaminatha swamy temple', 'Swamimalai', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Netra Pushkarini', 'Kaveri', 'STHANAKA',
    'Swaminatha Swamy Arupadaiveedu at Swamimalai.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000055', 'Swetharanyeswarar Temple', 'swetharanyeswarar temple', 'Tiruvengadu', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, 'Agni/Surya/Chandra', 'Kaveri', NULL,
    'Swetharanyeswarar (Budha sthala) at Tiruvengadu.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000056', 'Thyagarajaswamy Temple', 'thyagarajaswamy temple', 'Tiruvottiyur', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', NULL, 'Athi', 'Brahma Tirtha', 'Bay of Bengal', NULL,
    'Adipurisvara and Vadivudaiamman temple at Tiruvottiyur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000057', 'Tiruvavinankudi Temple', 'tiruvavinankudi temple', 'Palani', 'Tamil Nadu', 'India',
    'COMPLEX', 'PANDYA', NULL, NULL, 'Shanmuganathi', NULL, 'STHANAKA',
    'Foot-hill Arupadaiveedu Murugan temple at Palani.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000058', 'Vadanyeswarar Temple (Vallalar Koil)', 'vadanyeswarar temple (vallalar koil)', 'Mayiladuthurai', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, 'Kaveri', NULL,
    'Vallalar Koil Vadanyeswarar temple at Mayiladuthurai.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000059', 'Vaitheeswaran Koil', 'vaitheeswaran koil', 'Vaitheeswarankoil', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, 'Neem', 'Siddhamrita Tirtha', NULL, NULL,
    'Vaidyanatha Swamy and Thaiyalnayaki healing shrine.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000005a', 'Vanchinathar Temple', 'vanchinathar temple', 'Srivanchiyam', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, 'Sandal', 'Gupta Ganga', 'Kaveri', NULL,
    'Vanchinathar and Mangalambika shrine at Srivanchiyam.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000005b', 'Varadaraja Perumal Temple', 'varadaraja perumal temple', 'Kanchipuram', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', NULL, 'Athi', 'Ananta Sarovar', 'Vegavati', 'STHANAKA',
    'Hastigiri Varadaraja Perumal Divya Desam at Kanchipuram.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000005c', 'Vedagiriswarar Temple', 'vedagiriswarar temple', 'Tirukazhukundram', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', NULL, NULL, 'Sangu Tirtha', NULL, NULL,
    'Sacred eagle hill shrine of Vedagiriswarar.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000005d', 'Vedaranyeswarar Temple', 'vedaranyeswarar temple', 'Vedaranyam', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, 'Vanni', 'Manikarnika', 'Bay of Bengal', NULL,
    'Vedaranyeswarar coastal shrine where Vedas worshipped.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000005e', 'Veeratteswarar Temple (Gajasamhara Murti)', 'veeratteswarar temple (gajasamhara murti)', 'Vazhuvur', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, NULL, NULL,
    'Veerattam sthala of Gajasamhara Murti at Vazhuvur.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000005f', 'Venkateswara Temple', 'venkateswara temple', 'Tirupati', 'Andhra Pradesh', 'India',
    'COMPLEX', 'TONDAI', NULL, NULL, 'Swami Pushkarini', NULL, 'STHANAKA',
    'Seven Hills Venkateswara shrine at Tirumala Tirupati.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000060', 'Yamunambal Sametha Kasi Viswanatha Temple', 'yamunambal sametha kasi viswanatha temple', 'Needamangalam', 'Tamil Nadu', 'India',
    'COMPLEX', 'CHOLA', NULL, NULL, NULL, NULL, NULL,
    'Yamunambal Sametha Kasi Viswanatha temple at Needamangalam.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000061', 'Yoga Narasimha Swamy Temple', 'yoga narasimha swamy temple', 'Sholinghur', 'Tamil Nadu', 'India',
    'COMPLEX', 'TONDAI', NULL, NULL, 'Thakkan Kulam', NULL, 'ASINA',
    'Ghatikachala Yoga Narasimha hill temple at Sholinghur.',
    NOW(), NOW()
)
ON CONFLICT ON CONSTRAINT temples_name_city_uq DO UPDATE SET
    place_kind = EXCLUDED.place_kind,
    mandalam = EXCLUDED.mandalam,
    bhuta = EXCLUDED.bhuta,
    sthala_vriksha = EXCLUDED.sthala_vriksha,
    sthala_tirtha = EXCLUDED.sthala_tirtha,
    nadi_tirtha = EXCLUDED.nadi_tirtha,
    deity_posture = EXCLUDED.deity_posture,
    notes = EXCLUDED.notes,
    updated_at = NOW();

-- 5. Seed child sannidhis (with parent_temple_id reference).
INSERT INTO temples (
    id, name, name_normalized, city, state, country,
    parent_temple_id, place_kind, mandalam, bhuta, sthala_vriksha, sthala_tirtha, nadi_tirtha, deity_posture,
    notes, created_at, updated_at
)
VALUES
(
    '01930000-0000-7000-8000-000000000002', 'Kamalamba Sannidhi', 'kamalamba sannidhi', 'Tiruvarur', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-000000000001'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, 'Kamalalayam', NULL, 'ASINA',
    'Kamalamba shrine inside the Tiruvarur complex.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000003', 'Nilotpalamba Sannidhi', 'nilotpalamba sannidhi', 'Tiruvarur', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-000000000001'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, 'Kamalalayam', NULL, 'ASINA',
    'Nilotpalamba shrine inside the Tiruvarur complex.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000004', 'Tyagaraja Sanctum', 'tyagaraja sanctum', 'Tiruvarur', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-000000000001'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, NULL, NULL, 'ASINA',
    'Central Tyagaraja sanctum of the Tiruvarur complex.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000005', 'Navagraha Mandapa', 'navagraha mandapa', 'Tiruvarur', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-000000000001'::uuid, 'MANDAPAM', 'CHOLA', NULL, NULL, NULL, NULL, NULL,
    'Navagraha mandapa of the Tiruvarur complex.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000006', 'Achalesvara Sannidhi', 'achalesvara sannidhi', 'Tiruvarur', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-000000000001'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, 'Kamalalayam', NULL, NULL,
    'Local Tiruvarur lingam. Not a Pancha Bhuta kshetra.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000007', 'Hatakesvara Sannidhi', 'hatakesvara sannidhi', 'Tiruvarur', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-000000000001'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, 'Kamalalayam', NULL, NULL,
    'Local Tiruvarur lingam. Not a Pancha Bhuta kshetra.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000008', 'Valmikesvara Sannidhi', 'valmikesvara sannidhi', 'Tiruvarur', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-000000000001'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, 'Kamalalayam', NULL, NULL,
    'Local Tiruvarur lingam. Not a Pancha Bhuta kshetra.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000009', 'Anandesvara Sannidhi', 'anandesvara sannidhi', 'Tiruvarur', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-000000000001'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, 'Kamalalayam', NULL, NULL,
    'Local Tiruvarur lingam. Not a Pancha Bhuta kshetra.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000000a', 'Siddhisvara Sannidhi', 'siddhisvara sannidhi', 'Tiruvarur', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-000000000001'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, 'Kamalalayam', NULL, NULL,
    'Local Tiruvarur lingam sung in Nilambari. Not a Pancha Bhuta kshetra.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000000d', 'Mayuranathar Temple', 'mayuranathar temple', 'Mayiladuthurai', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-00000000000c'::uuid, 'COMPLEX', 'CHOLA', NULL, NULL, 'Amrita Vapi', 'Kaveri', NULL,
    'Mayuranathar temple at Mayiladuthurai.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000000e', 'Abhayamba Sannidhi', 'abhayamba sannidhi', 'Mayiladuthurai', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-00000000000d'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, 'Amrita Vapi', 'Kaveri', NULL,
    'Abhayamba shrine. Tritiya and sambodhana core members are not in the imported corpus.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-000000000016', 'Tiruvarur Saraswati Sannidhi', 'tiruvarur saraswati sannidhi', 'Tiruvarur', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-000000000001'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, 'Kamalalayam', NULL, 'ASINA',
    'Saraswati shrine within the Tiruvarur Tyagaraja temple complex.',
    NOW(), NOW()
),
(
    '01930000-0000-7000-8000-00000000001f', 'Balakuchambika Shrine, Kadambavaneswarar Temple', 'balakuchambika shrine, kadambavaneswarar temple', 'Kulittalai', 'Tamil Nadu', 'India',
    '01930000-0000-7000-8000-00000000002c'::uuid, 'SANNIDHI', 'CHOLA', NULL, NULL, NULL, 'Kaveri', NULL,
    'Balakuchambika consort shrine at Kulittalai (Ratnagiri).',
    NOW(), NOW()
)
ON CONFLICT ON CONSTRAINT temples_name_city_uq DO UPDATE SET
    parent_temple_id = EXCLUDED.parent_temple_id,
    place_kind = EXCLUDED.place_kind,
    mandalam = EXCLUDED.mandalam,
    bhuta = EXCLUDED.bhuta,
    sthala_vriksha = EXCLUDED.sthala_vriksha,
    sthala_tirtha = EXCLUDED.sthala_tirtha,
    nadi_tirtha = EXCLUDED.nadi_tirtha,
    deity_posture = EXCLUDED.deity_posture,
    notes = EXCLUDED.notes,
    updated_at = NOW();

-- 6. Canonical cycle memberships and tags.
WITH reviewed (
    cycle_slug, title, sequence_order, role, axis_value, attrs,
    vibhakti, stem, temple_id, deity_key, manipravala, occasion_note
) AS (
    VALUES
    -- Kamalamba Navavarana. Dhyana and Mangalam sit outside the nine enclosures.
    ('kamalamba-navavarnam', 'kamalAmbikE ASrita', 0, 'DHYANA', NULL, '{}'::jsonb, 'SAMBODHANA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),
    ('kamalamba-navavarnam', 'kamalAmbA saMrakshatu', 1, 'CORE', 'Trailokyamohana', '{"yogini":"Prakata","avarana":1}'::jsonb, 'PRATHAMA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),
    ('kamalamba-navavarnam', 'kamalAmbAM bhajarE', 2, 'CORE', 'Sarvasiparipuraka', '{"yogini":"Gupta","avarana":2}'::jsonb, 'DVITIYA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),
    ('kamalamba-navavarnam', 'SrI kamalAmbikayA kaTAkshitOhaM', 3, 'CORE', 'Sarvasankshobhana', '{"yogini":"Guptatara","avarana":3}'::jsonb, 'TRITIYA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),
    ('kamalamba-navavarnam', 'kamalAmbikAyai kanaka', 4, 'CORE', 'Sarvasaubhagyadayaka', '{"yogini":"Sampradaya","avarana":4}'::jsonb, 'CHATURTHI', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),
    ('kamalamba-navavarnam', 'SrI kamalAmbikAyAH paraM', 5, 'CORE', 'Sarvarthasadhaka', '{"yogini":"Kulottirna","avarana":5}'::jsonb, 'PANCHAMI', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),
    ('kamalamba-navavarnam', 'kamalAmbikAyAstava', 6, 'CORE', 'Sarvarakshakara', '{"yogini":"Nigarbha","avarana":6}'::jsonb, 'SHASHTHI', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),
    ('kamalamba-navavarnam', 'SrI kamalAmbikAyAM bhaktiM', 7, 'CORE', 'Sarvarogahara', '{"yogini":"Rahasya","avarana":7}'::jsonb, 'SAPTAMI', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),
    ('kamalamba-navavarnam', 'SrI kamalAmbikE avAva', 8, 'CORE', 'Sarvasiddhiprada', '{"yogini":"Atirahasya","avarana":8}'::jsonb, 'SARVA_VIBHAKTI', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),
    ('kamalamba-navavarnam', 'SrI kamalAmbA jayati', 9, 'CORE', 'Sarvanandamaya', '{"yogini":"Parapararahasya","avarana":9}'::jsonb, 'PRATHAMA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),
    ('kamalamba-navavarnam', 'SrI kamalAmbikE SivE', 10, 'MANGALAM', NULL, '{}'::jsonb, 'SAMBODHANA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL),

    -- Guruguha vibhakti. Sequence 3 (tritiya) is reserved.
    ('guruguha-vibhakti', 'SrI guru guha mUrtE', 1, 'CORE', NULL, '{}'::jsonb, 'PRATHAMA', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL),
    ('guruguha-vibhakti', 'mAnasa guru guha', 2, 'CORE', NULL, '{}'::jsonb, 'DVITIYA', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL),
    ('guruguha-vibhakti', 'guru guhAya bhakta', 4, 'CORE', NULL, '{}'::jsonb, 'CHATURTHI', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL),
    ('guruguha-vibhakti', 'guru guhAdanyaM', 5, 'CORE', NULL, '{}'::jsonb, 'PANCHAMI', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL),
    ('guruguha-vibhakti', 'SrI guru guhasya', 6, 'CORE', NULL, '{}'::jsonb, 'SHASHTHI', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL),
    ('guruguha-vibhakti', 'SrI nAthAdi guru guhO', 7, 'CORE', NULL, '{}'::jsonb, 'SAPTAMI', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL),
    ('guruguha-vibhakti', 'SrI guru guha tArayASu', 8, 'CORE', NULL, '{}'::jsonb, 'SAMBODHANA', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL),

    -- Tyagaraja vibhakti.
    ('tyagaraja-vibhakti', 'tyAgarAjO virAjatE', 1, 'CORE', NULL, '{}'::jsonb, 'PRATHAMA', 'Tyagaraja', '01930000-0000-7000-8000-000000000004'::uuid, NULL, false, NULL),
    ('tyagaraja-vibhakti', 'tyAgarAjaM bhajEhaM', 2, 'CORE', NULL, '{}'::jsonb, 'DVITIYA', 'Tyagaraja', '01930000-0000-7000-8000-000000000004'::uuid, NULL, false, NULL),
    ('tyagaraja-vibhakti', 'tyAgarAjEna', 3, 'CORE', NULL, '{}'::jsonb, 'TRITIYA', 'Tyagaraja', '01930000-0000-7000-8000-000000000004'::uuid, NULL, false, NULL),
    ('tyagaraja-vibhakti', 'tyAgarAjAya namastE', 4, 'CORE', NULL, '{}'::jsonb, 'CHATURTHI', 'Tyagaraja', '01930000-0000-7000-8000-000000000004'::uuid, NULL, false, NULL),
    ('tyagaraja-vibhakti', 'tyAgarAjAdanyaM', 5, 'CORE', NULL, '{}'::jsonb, 'PANCHAMI', 'Tyagaraja', '01930000-0000-7000-8000-000000000004'::uuid, NULL, false, NULL),
    ('tyagaraja-vibhakti', 'SrI tyAgarAjasya', 6, 'CORE', NULL, '{}'::jsonb, 'SHASHTHI', 'Tyagaraja', '01930000-0000-7000-8000-000000000004'::uuid, NULL, false, NULL),
    ('tyagaraja-vibhakti', 'tyAgarAjE kRtyAkRtyaM', 7, 'CORE', NULL, '{}'::jsonb, 'SAPTAMI', 'Tyagaraja', '01930000-0000-7000-8000-000000000004'::uuid, NULL, false, NULL),
    ('tyagaraja-vibhakti', 'tyAgarAja pAlayASu', 8, 'CORE', NULL, '{}'::jsonb, 'SAMBODHANA', 'Tyagaraja', '01930000-0000-7000-8000-000000000004'::uuid, NULL, false, NULL),

    -- Nilotpalamba Gaulanta. axis_value stays null; the raga and vibhakti discriminate.
    ('nilotpalamba-vibhakti', 'nIlOtpalAmbA jayati', 1, 'CORE', NULL, '{}'::jsonb, 'PRATHAMA', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL),
    ('nilotpalamba-vibhakti', 'nIlOtpalAmbAM bhajarE', 2, 'CORE', NULL, '{}'::jsonb, 'DVITIYA', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL),
    ('nilotpalamba-vibhakti', 'nIlOtpalAmbikayA nirvANa', 3, 'CORE', NULL, '{}'::jsonb, 'TRITIYA', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL),
    ('nilotpalamba-vibhakti', 'nIlOtpalAmbikAyai namastE', 4, 'CORE', NULL, '{}'::jsonb, 'CHATURTHI', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL),
    ('nilotpalamba-vibhakti', 'nIlOtpalAmbikAyAH paraM', 5, 'CORE', NULL, '{}'::jsonb, 'PANCHAMI', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL),
    ('nilotpalamba-vibhakti', 'nIlOtpalAmbikAyAstava', 6, 'CORE', NULL, '{}'::jsonb, 'SHASHTHI', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL),
    ('nilotpalamba-vibhakti', 'nIlOtpalAmbikAyAM bhaktiM', 7, 'CORE', NULL, '{}'::jsonb, 'SAPTAMI', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL),
    ('nilotpalamba-vibhakti', 'nIlOtpalAmbikE nitya', 8, 'CORE', NULL, '{}'::jsonb, 'SAMBODHANA', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL),

    -- Abhayamba. Sequences 3 and 8 are reserved. Sadashraye and the Sri-raga mangalam are optional.
    ('abhayamba-vibhakti', 'abhayAmbA jagadambA', 1, 'CORE', NULL, '{}'::jsonb, 'PRATHAMA', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL),
    ('abhayamba-vibhakti', 'AryAM abhayAmbAM', 2, 'CORE', NULL, '{}'::jsonb, 'DVITIYA', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL),
    ('abhayamba-vibhakti', 'abhayAmbikAyai aSva', 4, 'CORE', NULL, '{}'::jsonb, 'CHATURTHI', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL),
    ('abhayamba-vibhakti', 'abhayAmbikAyAH anyaM', 5, 'CORE', NULL, '{}'::jsonb, 'PANCHAMI', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL),
    ('abhayamba-vibhakti', 'ambikAyAH abhayAmbikAyAH', 6, 'CORE', NULL, '{}'::jsonb, 'SHASHTHI', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL),
    ('abhayamba-vibhakti', 'abhayAmbAyAM bhaktiM', 7, 'CORE', NULL, '{}'::jsonb, 'SAPTAMI', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL),
    ('abhayamba-vibhakti', 'sadASrayE abhayAmbikE', 0, 'OPTIONAL', NULL, '{}'::jsonb, NULL, 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL),
    ('abhayamba-vibhakti', 'SrI abhayAmbA', 9, 'OPTIONAL', NULL, '{}'::jsonb, NULL, 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, true, NULL),

    -- Navagraha. Rahu and Ketu stay disputed, per SSP.
    ('navagraha-krithis', 'sUrya mUrtE', 1, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000005'::uuid, 'surya', false, NULL),
    ('navagraha-krithis', 'candraM bhaja', 2, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000005'::uuid, 'chandra', false, NULL),
    ('navagraha-krithis', 'angArakaM ASrayAmyahaM', 3, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000005'::uuid, 'angaraka', false, NULL),
    ('navagraha-krithis', 'budhamASrayAmi', 4, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000005'::uuid, 'budha', false, NULL),
    ('navagraha-krithis', 'bRhaspatE tArA patE', 5, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000005'::uuid, 'brihaspati', false, NULL),
    ('navagraha-krithis', 'SrI Sukra bhagavantaM', 6, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000005'::uuid, 'shukra', false, NULL),
    ('navagraha-krithis', 'divAkara tanujaM', 7, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000005'::uuid, 'shani', false, NULL),
    ('navagraha-krithis', 'smarAmyahaM sadA', 8, 'DISPUTED_CONJECTURE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000005'::uuid, 'rahu', false, NULL),
    ('navagraha-krithis', 'mahA suraM kEtuM', 9, 'DISPUTED_CONJECTURE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000005'::uuid, 'ketu', false, NULL),

    -- Pancha Bhuta. The element lives on temples.bhuta, not on axis_value.
    ('pancha-bhuta-sthala', 'cintaya mA kanda', 1, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-00000000000f'::uuid, NULL, false, NULL),
    ('pancha-bhuta-sthala', 'jambU patE', 2, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000010'::uuid, NULL, false, NULL),
    ('pancha-bhuta-sthala', 'aruNAcala nAthaM', 3, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000011'::uuid, NULL, false, NULL),
    ('pancha-bhuta-sthala', 'SrI kALahastISa', 4, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000012'::uuid, NULL, false, NULL),
    ('pancha-bhuta-sthala', 'Ananda naTana prakASaM', 5, 'CORE', NULL, '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000013'::uuid, NULL, false, NULL),

    -- Tiruvarur Pancha Linga. axis_value is the local lingam name.
    ('tiruvarur-panchalinga', 'sadAcalESvaraM', 1, 'CORE', 'Achalesvara', '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000006'::uuid, NULL, false, NULL),
    ('tiruvarur-panchalinga', 'hATakESvara', 2, 'CORE', 'Hatakesvara', '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000007'::uuid, NULL, false, NULL),
    ('tiruvarur-panchalinga', 'SrI valmIka lingaM', 3, 'CORE', 'Valmikesvara', '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000008'::uuid, NULL, false, NULL),
    ('tiruvarur-panchalinga', 'AnandESvarENa', 4, 'CORE', 'Anandesvara', '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-000000000009'::uuid, NULL, false, NULL),
    ('tiruvarur-panchalinga', 'siddhISvarAya', 5, 'CORE', 'Siddhisvara', '{}'::jsonb, NULL, NULL, '01930000-0000-7000-8000-00000000000a'::uuid, NULL, false, NULL),

    -- Shodasa Ganapati, open attested set present in the catalogue.
    ('shodasa-ganapati', 'vAtApi gaNa patiM', 1, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'ucchishTa gaNapatau', 2, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'vallabhA nAyakasya', 3, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'mahA gaNa patiM manasA', 4, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'mahA gaNa patiM vandE', 5, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'mahA gaNa patE', 6, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'SrI mahA gaNa patiravatu', 7, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'gaNa patE mahA matE', 8, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'hErambAya', 9, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'lambOdarAya', 10, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'rakta gaNa patiM', 11, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'SvEta gaNa patiM', 12, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'siddhi vinAyakaM', 13, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'viNAyaka vighna', 14, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'gajAnana yutaM', 15, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'panca mAtanga mukha', 16, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('shodasa-ganapati', 'Sakti sahita', 17, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),

    -- Nottusvara. Tune names only where a published air identification exists.
    ('nottusvara-sahitya', 'santataM pAhi mAM', 1, 'CORE', 'God Save the King', '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'vandE mInAkshi', 2, 'CORE', 'Rakes of Mallow', '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'kamalAsana vandita', 3, 'CORE', 'Galopede', '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'Sakti sahita', 4, 'CORE', 'Voulez-vous danser', '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'SyAmaLE mInAkshi', 5, 'CORE', 'Ah vous dirai-je Maman', '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'jagadISa guru guha', 6, 'CORE', 'Lord MacDonald''s Reel', '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'vara Siva bAlaM', 7, 'CORE', 'Castilian Maid', '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'AnjanEyaM', 8, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'cintaya citta', 9, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'cintayEhaM sadA', 10, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'dASarathE', 11, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'dIna bandhO', 12, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'guru guha pada', 13, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'guru guha sarasija', 14, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'hE mAyE', 15, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'kAncISaM', 16, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'mAyE citkalE', 17, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'mucukunda varada', 18, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'pAhi durgE', 19, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'pAhi mAM janakI vallabha', 20, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'pankaja mukha', 21, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'para dEvatE bhava', 22, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'pArvatI patE', 23, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'pIta varNaM', 24, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'rAjIva lOcanaM', 25, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'rAma candraM rAjIvAkshaM', 26, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'rAma janArdana', 27, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'sadASiva jAyE', 28, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'sakala sura vinuta', 29, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'sAma gAna priyE', 30, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'Sankara vara', 31, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'santAna saubhAgya', 32, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'santataM gOvinda rAjaM', 33, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'Sauri vidhi nutE', 34, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'sOmAskandaM', 35, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'subrahmaNyaM', 36, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'vAgdEvi mAmava', 37, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL),
    ('nottusvara-sahitya', 'varada rAja pAhi', 38, 'CORE', NULL, '{}'::jsonb, NULL, NULL, NULL::uuid, NULL, false, NULL)
)
INSERT INTO krithi_cycle_memberships (
    krithi_id, tag_id, sequence_order, role, axis_value, discriminative_attributes
)
SELECT k.id, t.id, rv.sequence_order, rv.role::cycle_member_role_enum, rv.axis_value, rv.attrs
FROM reviewed rv
JOIN tags t ON t.slug = rv.cycle_slug
JOIN krithis k ON k.title = rv.title
WHERE k.composer_id = (
    SELECT id FROM composers WHERE lower(name) LIKE '%dikshitar%' ORDER BY name LIMIT 1
)
ON CONFLICT ON CONSTRAINT uq_krithi_cycle DO UPDATE SET
    sequence_order = EXCLUDED.sequence_order,
    role = EXCLUDED.role,
    axis_value = EXCLUDED.axis_value,
    discriminative_attributes = EXCLUDED.discriminative_attributes;

INSERT INTO krithi_tags (krithi_id, tag_id, source, confidence)
SELECT m.krithi_id, m.tag_id, 'reviewed_cycle', 100
FROM krithi_cycle_memberships m
JOIN tags t ON t.id = m.tag_id
WHERE t.slug IN (
    'pancha-bhuta-sthala',
    'kamalamba-navavarnam',
    'navagraha-krithis',
    'abhayamba-vibhakti',
    'nilotpalamba-vibhakti',
    'guruguha-vibhakti',
    'tyagaraja-vibhakti',
    'tiruvarur-panchalinga',
    'shodasa-ganapati',
    'nottusvara-sahitya'
)
ON CONFLICT (krithi_id, tag_id) DO UPDATE SET
    source = 'reviewed_cycle',
    confidence = EXCLUDED.confidence;


-- 7. Complete composition lakshana and kshetra update for ALL 481 Dikshitar compositions.
WITH comp_data (
    row_id, title, raga_name, vibhakti, stem, temple_id, deity_key, manipravala, occasion_note, yati_pattern
) AS (
    VALUES
    (1, 'Adi purISvaraM', 'Ārabhi', 'DVITIYA', 'Purisvar', '01930000-0000-7000-8000-000000000056'::uuid, NULL, false, NULL, 'NONE'),
    (2, 'AnandAmRtAkarshiNi', 'amRta varshiNi', 'DVITIYA', 'Anandamrtakarshini', '01930000-0000-7000-8000-000000000022'::uuid, NULL, false, 'Rain miracle: Dikshitar brought heavy rain during a severe drought at Ettayapuram through Amritavarshini raga. The index names Bhavani here. Himagiri kumari in the same raga is the Sattur shrine.', 'NONE'),
    (3, 'AnandESvarENa', 'Anandabhairavi', 'TRITIYA', 'Anandesvara', '01930000-0000-7000-8000-000000000009'::uuid, NULL, false, NULL, 'NONE'),
    (4, 'Ananda naTana prakASaM', 'Kedaram', 'DVITIYA', 'Nataraja', '01930000-0000-7000-8000-000000000013'::uuid, NULL, false, NULL, 'NONE'),
    (5, 'AnjanEyaM', 'SankarAbharaNaM', 'DVITIYA', 'Anjaney', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (6, 'AryAM abhayAmbAM', 'Bhairavi', 'DVITIYA', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (7, 'Ehi annapUrNE', 'Punnagavarali', 'SAPTAMI', 'Annapurn', '01930000-0000-7000-8000-000000000030'::uuid, NULL, false, NULL, 'NONE'),
    (8, 'EkAmrESa nAyakIM', 'Chāmaram', 'DVITIYA', 'Nayakim', '01930000-0000-7000-8000-00000000000f'::uuid, NULL, false, NULL, 'NONE'),
    (9, 'EkAmrESa nAyikE', 'Suddha Sāveri', 'SAPTAMI', 'Nayik', '01930000-0000-7000-8000-00000000000f'::uuid, NULL, false, NULL, 'NONE'),
    (10, 'EkAmra nAthAya', 'Veeravasantham', 'CHATURTHI', 'Nath', '01930000-0000-7000-8000-00000000000f'::uuid, NULL, false, NULL, 'NONE'),
    (11, 'EkAmra nAthAya namastE', 'Mukhāri', 'CHATURTHI', 'Nath', '01930000-0000-7000-8000-00000000000f'::uuid, NULL, false, NULL, 'NONE'),
    (12, 'EkAmra nAthESvarENa', 'Chaturāngini', 'TRITIYA', 'Nathesvar', '01930000-0000-7000-8000-00000000000f'::uuid, NULL, false, NULL, 'NONE'),
    (13, 'EkAmra nAthaM bhajEhaM', 'Gamakakriyā', 'DVITIYA', 'Nath', '01930000-0000-7000-8000-00000000000f'::uuid, NULL, false, NULL, 'NONE'),
    (14, 'Eka dantaM bhajEhaM', 'Bilahari', 'DVITIYA', 'Dant', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (15, 'ISAnAdi SivAkAra', 'Sahāna', 'DVITIYA', 'Isanadi', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (16, 'SAlivATISvaraM', 'Devagāndhāri', 'DVITIYA', 'Salivatisvar', '01930000-0000-7000-8000-00000000003e'::uuid, NULL, false, NULL, 'NONE'),
    (17, 'SEshAcala nAyakaM', 'Varāli', 'DVITIYA', 'Nayak', '01930000-0000-7000-8000-00000000005f'::uuid, NULL, false, NULL, 'NONE'),
    (18, 'SRngArAdi nava', 'Dhavalāngam', 'DVITIYA', 'Srngaradi', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (19, 'SRngAra SaktyAyudha', 'Ramāmanohari', 'DVITIYA', 'Srngara', '01930000-0000-7000-8000-00000000004f'::uuid, NULL, false, NULL, 'NONE'),
    (20, 'SRngAra rasa', 'Rasamanjari', 'DVITIYA', 'Srngara', '01930000-0000-7000-8000-000000000020'::uuid, NULL, false, NULL, 'NONE'),
    (21, 'SailESvaraM', 'sumadyuti', 'DVITIYA', 'Sailesvar', '01930000-0000-7000-8000-00000000002f'::uuid, NULL, false, NULL, 'NONE'),
    (22, 'Saila rAja kumAri', 'Shailadeshākshhi', 'DVITIYA', 'Saila', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (23, 'Sakti sahita', 'SankarAbharaNaM', 'DVITIYA', 'Sakti', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (24, 'Sankara nArAyaNaM', 'Nārāyanadeshākshi', 'DVITIYA', 'Narayan', '01930000-0000-7000-8000-00000000004c'::uuid, NULL, false, NULL, 'NONE'),
    (25, 'Sankara vara', 'SankarAbharaNaM', 'DVITIYA', 'Sankara', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (26, 'SankaraM abhirAmI', 'Manohari', 'DVITIYA', 'Sankar', '01930000-0000-7000-8000-00000000001a'::uuid, NULL, false, 'Markandeya longevity sthala: Lord Shiva (Amritaghateshwara) granting eternal life to sage Markandeya against Yama at Thirukkadaiyur.', 'NONE'),
    (27, 'Sankha cakra gadA', 'Poornachandrika', 'DVITIYA', 'Sankha', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (28, 'SarAvatI taTa', 'Sharāvathi', 'PRATHAMA', 'Sarasvati', '01930000-0000-7000-8000-00000000004d'::uuid, NULL, false, 'Sthala attribution: In praise of Saraswati on the banks of the Sharavati River (traditionally identified with the historical Saraswati shrine at Basrur or regions along the Sharavati in Karnataka), distinct from Sringeri on the Tunga River.', 'NONE'),
    (29, 'SaravaNa bhava', 'Revagupti', 'DVITIYA', 'Saravana', '01930000-0000-7000-8000-000000000057'::uuid, NULL, false, NULL, 'NONE'),
    (30, 'Sauri vidhi nutE', 'SankarAbharaNaM', 'SAPTAMI', 'Nut', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (31, 'Siva kAMI patiM', 'nATa kuranji', 'DVITIYA', 'Patim', '01930000-0000-7000-8000-000000000013'::uuid, NULL, false, NULL, 'NONE'),
    (32, 'Siva kAmESvarIM', 'Mechakalyāni', 'DVITIYA', 'Kamesvarim', '01930000-0000-7000-8000-000000000013'::uuid, NULL, false, NULL, 'NONE'),
    (33, 'Siva kAmESvaraM', 'Ārabhi', 'DVITIYA', 'Kamesvar', '01930000-0000-7000-8000-000000000013'::uuid, NULL, false, NULL, 'NONE'),
    (34, 'Siva kAyArOhaNESAya', 'Rudrapriyā', 'CHATURTHI', 'Kayarohanes', '01930000-0000-7000-8000-000000000032'::uuid, NULL, false, NULL, 'NONE'),
    (35, 'SrI SUlinIM', 'Shailadeshākshhi', 'DVITIYA', 'Sulinim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (36, 'SrI Sukra bhagavantaM', 'paraju', 'DVITIYA', 'Sukra', '01930000-0000-7000-8000-000000000005'::uuid, NULL, false, NULL, 'NONE'),
    (37, 'SrI abhayAmbA', 'Sri', 'SAMBODHANA', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, true, NULL, 'NONE'),
    (38, 'SrI bAlasubrahmaNya', 'Bilahari', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000054'::uuid, NULL, false, NULL, 'NONE'),
    (39, 'SrI bhArgavi', 'Mangalakaishiki', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000049'::uuid, NULL, false, NULL, 'NONE'),
    (40, 'SrI dakshiNA mUrtISaM', 'Phenadhyuti', 'DVITIYA', 'Murtis', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (41, 'SrI dakshiNA mUrtiM sadA', 'Atāna', 'DVITIYA', 'Murtim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (42, 'SrI duM durgE', 'Ranjani', 'DVITIYA', 'Dum', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (43, 'SrI gaNESAtparaM', 'Ardhradesi', 'DVITIYA', 'Ganesatpar', '01930000-0000-7000-8000-000000000059'::uuid, NULL, false, NULL, 'NONE'),
    (44, 'SrI gaNa nAthaM', 'Eeshamanohari', 'DVITIYA', 'Nath', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (45, 'SrI guru guha mUrtE', 'Udayaravichandrika', 'SAMBODHANA', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (46, 'SrI guru guha tArayASu', 'Devakriya', 'SAMBODHANA', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (47, 'SrI guru guhasya', 'Poorvi', 'SHASHTHI', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (48, 'SrI guruNA', 'Pādi', 'TRITIYA', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (49, 'SrI kALahastISa', 'huSani', 'SAMBODHANA', 'Kalahasteesvara', '01930000-0000-7000-8000-000000000012'::uuid, NULL, false, NULL, 'NONE'),
    (50, 'SrI kAntimatIM', 'Deshisimhāravam', 'DVITIYA', 'Kantimatim', '01930000-0000-7000-8000-00000000003e'::uuid, NULL, false, NULL, 'NONE'),
    (51, 'SrI kRshNO mAM', 'Nāsāmani', 'DVITIYA', 'Mam', '01930000-0000-7000-8000-00000000005b'::uuid, NULL, false, NULL, 'NONE'),
    (52, 'SrI kRshNaM bhaja mAnasa', 'Hanumatodi', 'DVITIYA', 'Krshn', '01930000-0000-7000-8000-000000000029'::uuid, NULL, false, 'PPNS places Sri Krishnam bhaja manasa in Todi at Guruvayur.', 'NONE'),
    (53, 'SrI kRshNaM bhajarE', 'Rūpavati', 'DVITIYA', 'Krshn', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (54, 'SrI kamalAmbA jayati', 'Āhiri', 'PRATHAMA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (55, 'SrI kamalAmbikAyAH paraM', 'Bhairavi', 'PANCHAMI', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (56, 'SrI kamalAmbikAyAM bhaktiM', 'Sahāna', 'SAPTAMI', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (57, 'SrI kamalAmbikE SivE', 'Sri', 'SAMBODHANA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (58, 'SrI kamalAmbikE avAva', 'Ghanta', 'SAMBODHANA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (59, 'SrI kamalAmbikayA kaTAkshitOhaM', 'SankarAbharaNaM', 'TRITIYA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (60, 'SrI lakshmI varAhaM', 'Abhogi', 'DVITIYA', 'Varah', '01930000-0000-7000-8000-000000000035'::uuid, NULL, false, NULL, 'NONE'),
    (61, 'SrI mAtR bhUtaM', 'Kannada', 'DVITIYA', 'Bhut', '01930000-0000-7000-8000-00000000003b'::uuid, NULL, false, NULL, 'NONE'),
    (62, 'SrI mAtaH Siva', 'Begada', 'PRATHAMA', 'Mat', '01930000-0000-7000-8000-000000000010'::uuid, NULL, false, NULL, 'NONE'),
    (63, 'SrI mInAkshi gauri', 'Gowri', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (64, 'SrI mInAmbikAyAH', 'Devagāndhāri', 'SHASHTHI', 'Minambik', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (65, 'SrI mUlAdhAra cakra', 'Sri', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (66, 'SrI madhurA puri vihAriNi', 'Bilahari', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (67, 'SrI madhurAmbikE', 'Mechakalyāni', 'SAPTAMI', 'Madhurambik', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (68, 'SrI madhurAmbikayA', 'Atāna', 'CHATURTHI', 'Madhurambik', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (69, 'SrI mahA gaNa patiravatu', 'Gowla', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (70, 'SrI mahArAjnI', 'Karnātaka Kāpi', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (71, 'SrI mangaLAmbikAM', 'Ghanta', 'DVITIYA', 'Mangalambik', '01930000-0000-7000-8000-000000000018'::uuid, NULL, false, NULL, 'NONE'),
    (72, 'SrI mangaLAmbikE', 'Mechakalyāni', 'SAPTAMI', 'Mangalambik', '01930000-0000-7000-8000-00000000005a'::uuid, NULL, false, 'The Kalyani Ata Mangalambike is Srivanchiyam. Sri Mangalambikam in Ghanta stays at Kumbakonam.', 'NONE'),
    (73, 'SrI nAthAdi guru guhO', 'mAyA mALavagauLa', 'PRATHAMA', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, 'First composition: Composed at Tiruttani after Lord Muruga appeared as an ascetic and placed rock sugar on his tongue, initiating his vaggeyakara career.', 'NONE'),
    (74, 'SrI nAtha sOdarIM', 'Nabhomani', 'DVITIYA', 'Sodarim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (75, 'SrI nIlOtpala nAyikE', 'Nārīrītigowla', 'SAMBODHANA', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL, 'NONE'),
    (76, 'SrI pArtha sArathinA', 'Suddha Dhanyāsi', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000042'::uuid, NULL, false, NULL, 'NONE'),
    (77, 'SrI pArvatI paramESvarau', 'Bowli', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000047'::uuid, NULL, false, NULL, 'NONE'),
    (78, 'SrI rAja gOpAla', 'Sāveri', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000046'::uuid, NULL, false, NULL, 'NONE'),
    (79, 'SrI rAja rAjESvarIM', 'Madhyamāvathi', 'DVITIYA', 'Rajesvarim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (80, 'SrI rAja rAjESvari', 'Poornachandrika', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (81, 'SrI rAma candrO', 'Ranjani', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (82, 'SrI rAmaM ravi kula', 'nArAyaNa gauLa', 'DVITIYA', 'Ram', '01930000-0000-7000-8000-000000000017'::uuid, NULL, false, NULL, 'NONE'),
    (83, 'SrI ramA sarasvati', 'Nāsāmani', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (84, 'SrI ranga nAthAya', 'Dhanyāsi', 'CHATURTHI', 'Nath', '01930000-0000-7000-8000-000000000049'::uuid, NULL, false, NULL, 'NONE'),
    (85, 'SrI sAmbaSivaM', 'Bilahari', 'DVITIYA', 'Sambasiv', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (86, 'SrI sarasvati hitE', 'Mānji', 'SAPTAMI', 'Hit', '01930000-0000-7000-8000-000000000020'::uuid, NULL, false, 'Sri Sarasvati hite is the Tanjavur Kamakshi mela kriti.', 'NONE'),
    (87, 'SrI sarasvati namOstu tE', 'Ārabhi', 'SAPTAMI', 'T', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (88, 'SrI satya nArAyaNaM', 'Shivapanthuvarāli', 'DVITIYA', 'Narayan', '01930000-0000-7000-8000-00000000001e'::uuid, NULL, false, NULL, 'NONE'),
    (89, 'SrI subrahmaNyAya', 'Kāmbhoji', 'CHATURTHI', 'Subrahmany', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (90, 'SrI subrahmaNyO mAM', 'Hanumatodi', 'DVITIYA', 'Mam', '01930000-0000-7000-8000-000000000051'::uuid, NULL, false, NULL, 'NONE'),
    (91, 'SrI sugandhi kuntaLAmbikE', 'Kunthalam', 'SAPTAMI', 'Kuntalambik', '01930000-0000-7000-8000-00000000003b'::uuid, NULL, false, NULL, 'NONE'),
    (92, 'SrI sundara rAjaM', 'Kāshirāmakriyā', 'DVITIYA', 'Raj', '01930000-0000-7000-8000-00000000002e'::uuid, NULL, false, NULL, 'NONE'),
    (93, 'SrI svAmi nAthAyA', 'Kamās', 'CHATURTHI', 'Nath', '01930000-0000-7000-8000-000000000054'::uuid, NULL, false, NULL, 'NONE'),
    (94, 'SrI tyAgarAjasya', 'Rudrapriyā', 'SHASHTHI', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (95, 'SrI vAncha nAthaM', 'suraTi', 'DVITIYA', 'Nath', '01930000-0000-7000-8000-00000000005a'::uuid, NULL, false, NULL, 'NONE'),
    (96, 'SrI vENu gOpAla', 'kuranji', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (97, 'SrI vENu gOpAlaM bhaja', 'SankarAbharaNaM', 'DVITIYA', 'Gopal', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (98, 'SrI vEnkaTa girISaM', 'suraTi', 'DVITIYA', 'Giris', '01930000-0000-7000-8000-00000000005f'::uuid, NULL, false, 'PPNS pairs Venkatagirisham with Seshachala at Tirupati. One list says Gokarna. Pulivalam is the rival claim for Venkatacalapate, not this kriti.', 'NONE'),
    (99, 'SrI vaTuka nAtha', 'Devakriya', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000040'::uuid, NULL, false, NULL, 'NONE'),
    (100, 'SrI vaidya nAthaM', 'Atāna', 'DVITIYA', 'Nath', '01930000-0000-7000-8000-000000000059'::uuid, NULL, false, NULL, 'NONE'),
    (101, 'SrI valmIka lingaM', 'Kāmbhoji', 'DVITIYA', 'Valmikesvara', '01930000-0000-7000-8000-000000000008'::uuid, NULL, false, NULL, 'NONE'),
    (102, 'SrI vara lakshmi', 'Sri', 'DVITIYA', 'Sri', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (103, 'SrI vidyA rAja gOpAlaM', 'Jaganmohana', 'DVITIYA', 'Gopal', '01930000-0000-7000-8000-000000000046'::uuid, NULL, false, NULL, 'NONE'),
    (104, 'SvEtAraNyESvaraM', 'Ārabhi', 'DVITIYA', 'Svetaranyesvar', '01930000-0000-7000-8000-000000000055'::uuid, NULL, false, NULL, 'NONE'),
    (105, 'SvEta gaNa patiM', 'Rāgachoodāmani', 'DVITIYA', 'Patim', '01930000-0000-7000-8000-000000000053'::uuid, NULL, false, 'The kshetra index names Tiruvalanchuzhi. A Tanjavur east-gate Sveta Ganapati is also described.', 'NONE'),
    (106, 'SyAmaLAngi', 'Shyāmalam', 'DVITIYA', 'Syamalangi', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (107, 'SyAmaLE mInAkshi', 'SankarAbharaNaM', 'SAPTAMI', 'Syamal', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (108, 'abhayAmbA jagadambA', 'Mechakalyāni', 'PRATHAMA', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (109, 'abhayAmbA nAyaka hari sAyaka', 'Anandabhairavi', 'DVITIYA', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (110, 'abhayAmbA nAyaka vara dAyaka', 'kEdAra gauLa', 'DVITIYA', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (111, 'abhayAmbAyAM bhaktiM', 'Sahāna', 'SAPTAMI', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (112, 'abhayAmbikAyAH anyaM', 'kEdAra gauLa', 'PANCHAMI', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (113, 'abhayAmbikAyai aSva', 'Yadukula Kāmbhoji', 'CHATURTHI', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (114, 'abhirAmIM akhila', 'Bhooshāvathi', 'DVITIYA', 'Abhiramim', '01930000-0000-7000-8000-00000000001a'::uuid, NULL, false, NULL, 'NONE'),
    (115, 'agastISvaraM', 'Lalitā', 'DVITIYA', 'Agastisvar', '01930000-0000-7000-8000-000000000019'::uuid, NULL, false, 'Agastisvara of Tanjavur, not the Tiruvarur bucket.', 'NONE'),
    (116, 'akhilANDESvarO rakshatu', 'Suddha Sāveri', 'DVITIYA', 'Akhilandesvaro', '01930000-0000-7000-8000-000000000010'::uuid, NULL, false, NULL, 'NONE'),
    (117, 'akhilANDESvari raksha mAM', 'Dwijavanthi', 'DVITIYA', 'Mam', '01930000-0000-7000-8000-000000000010'::uuid, NULL, false, NULL, 'NONE'),
    (118, 'akhilANDESvaryai namastE', 'Ārabhi', 'SAPTAMI', 'Namast', '01930000-0000-7000-8000-000000000010'::uuid, NULL, false, NULL, 'NONE'),
    (119, 'akshaya linga vibhO', 'SankarAbharaNaM', 'CHATURTHI', 'Aksh', '01930000-0000-7000-8000-000000000014'::uuid, NULL, false, 'Door-opening at Keevalur: the sanctum opened when this kriti was sung.', 'NONE'),
    (120, 'amba nIlAyatAkshi', 'Neelāmbari', 'DVITIYA', 'Amba', '01930000-0000-7000-8000-000000000032'::uuid, NULL, false, NULL, 'NONE'),
    (121, 'ambikAyAH abhayAmbikAyAH', 'Kedaram', 'SHASHTHI', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (122, 'ananta bAla kRshNa', 'Eeshamanohari', 'DVITIYA', 'Ananta', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (123, 'angArakaM ASrayAmyahaM', 'suraTi', 'DVITIYA', 'Angaraka', '01930000-0000-7000-8000-000000000005'::uuid, 'angaraka', false, NULL, 'NONE'),
    (124, 'annapUrNE viSAlAkshi', 'sAma', 'SAPTAMI', 'Annapurn', '01930000-0000-7000-8000-000000000031'::uuid, NULL, false, 'PPNS places the Sama Annapurne at Kuzhikkarai, apart from the Kasi Visalakshi pair. Some lists still file this kriti at Kasi.', 'NONE'),
    (125, 'ardha nArISvaraM', 'Kumudhakriyā', 'DVITIYA', 'Ardhanarisvara', '01930000-0000-7000-8000-00000000001c'::uuid, NULL, false, NULL, 'NONE'),
    (126, 'aruNAcala nAthaM', 'Sāranga', 'DVITIYA', 'Arunachalesvara', '01930000-0000-7000-8000-000000000011'::uuid, NULL, false, NULL, 'NONE'),
    (127, 'avyAja karuNA', 'Salanganāta', 'DVITIYA', 'Avyaja', '01930000-0000-7000-8000-000000000020'::uuid, NULL, false, NULL, 'NONE'),
    (128, 'bAlAmbikAyAH paraM', 'Kanadā', 'SHASHTHI', 'Balambik', '01930000-0000-7000-8000-000000000059'::uuid, NULL, false, NULL, 'NONE'),
    (129, 'bAlAmbikAyAH tava', 'kEdAra gauLa', 'SHASHTHI', 'Balambik', '01930000-0000-7000-8000-000000000059'::uuid, NULL, false, NULL, 'NONE'),
    (130, 'bAlAmbikAyai namastE', 'nATa kuranji', 'SAPTAMI', 'Namast', '01930000-0000-7000-8000-000000000059'::uuid, NULL, false, NULL, 'NONE'),
    (131, 'bAlAmbikE pAhi', 'Manoranjani', 'SAPTAMI', 'Balambik', '01930000-0000-7000-8000-000000000059'::uuid, NULL, false, NULL, 'NONE'),
    (132, 'bAlAmbikayA kaTAkshitOhaM', 'Ranjani', 'CHATURTHI', 'Balambik', '01930000-0000-7000-8000-000000000059'::uuid, NULL, false, NULL, 'NONE'),
    (133, 'bAla gOpAla', 'Bhairavi', 'DVITIYA', 'Bala', '01930000-0000-7000-8000-000000000046'::uuid, NULL, false, NULL, 'NONE'),
    (134, 'bAla kRshNaM bhAvayAmi', 'Gopikāvasantam', 'DVITIYA', 'Krshn', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (135, 'bAla kucAmbikE', 'suraTi', 'SAPTAMI', 'Kucambik', '01930000-0000-7000-8000-00000000001f'::uuid, NULL, false, 'Balakuchambike in Surati is Kulittalai, with Nilakantham and Ratnacala Nayaka. Not Unnamulai of Tiruvannamalai.', 'NONE'),
    (136, 'bAla subrahmaNyaM', 'suraTi', 'DVITIYA', 'Subrahmany', '01930000-0000-7000-8000-000000000051'::uuid, NULL, false, NULL, 'NONE'),
    (137, 'bRhadISa kaTAkshENa', 'Jeevanthikā', 'TRITIYA', 'Kataksh', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (138, 'bRhadISvarAya namastE', 'SankarAbharaNaM', 'CHATURTHI', 'Brhadisvar', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (139, 'bRhadISvarIM bhaja', 'Lalitapanchamam', 'DVITIYA', 'Brhadisvarim', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (140, 'bRhadISvarO rakshatu', 'Gānasāmavarāli', 'DVITIYA', 'Brhadisvaro', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (141, 'bRhadISvaraM bhaja', 'Nāgadhwani', 'DVITIYA', 'Brhadisvar', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (142, 'bRhadambA madambA', 'Bhānumati', 'DVITIYA', 'Brhadamba', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (143, 'bRhambikAyai', 'Vasanthā', 'DVITIYA', 'Brhambika', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (144, 'bRhannAyaki vara dAyaki', 'Andhali', 'DVITIYA', 'Brhannayaki', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (145, 'bRhaspatE tArA patE', 'Atāna', 'SAMBODHANA', 'Brihaspati', '01930000-0000-7000-8000-000000000005'::uuid, 'brihaspati', false, 'Health healing: Composed at Tiruvarur/Alangudi to cure his disciple Tambiappan of a severe abdominal affliction.', 'NONE'),
    (146, 'bhArati', 'Deva Manohari', 'DVITIYA', 'Bharati', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (147, 'bhOgacchAyA', 'bhOgacchAyAnATa', 'CHATURTHI', 'Bhogacch', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (148, 'bhUshA patiM', 'Bhooshāvathi', 'DVITIYA', 'Patim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (149, 'bhUshAvatiM', 'Bhooshāvathi', 'DVITIYA', 'Bhushavatim', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (150, 'bhajarE rE citta', 'Mechakalyāni', 'SAPTAMI', 'Bhajar', '01930000-0000-7000-8000-000000000059'::uuid, NULL, false, NULL, 'NONE'),
    (151, 'bhakta vatsalaM', 'Vamshavathi', 'DVITIYA', 'Vatsal', '01930000-0000-7000-8000-000000000021'::uuid, NULL, false, NULL, 'NONE'),
    (152, 'brahma vidyAmbikE', 'Mechakalyāni', 'SAPTAMI', 'Vidyambik', '01930000-0000-7000-8000-000000000055'::uuid, NULL, false, NULL, 'NONE'),
    (153, 'budhamASrayAmi', 'nATa kuranji', 'DVITIYA', 'Budha', '01930000-0000-7000-8000-000000000005'::uuid, 'budha', false, NULL, 'NONE'),
    (154, 'cEtaH SrI bAla kRshNaM', 'Dwijavanthi', 'PRATHAMA', 'Cet', '01930000-0000-7000-8000-000000000046'::uuid, NULL, false, NULL, 'NONE'),
    (155, 'candra SEkharaM', 'mArgahindOLaM', 'DVITIYA', 'Sekhar', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (156, 'candraM bhaja', 'Asāveri', 'DVITIYA', 'Chandra', '01930000-0000-7000-8000-000000000005'::uuid, 'chandra', false, NULL, 'NONE'),
    (157, 'chAyAvatIM', 'Chāyāvathi', 'DVITIYA', 'Chayavatim', '01930000-0000-7000-8000-00000000001b'::uuid, NULL, false, NULL, 'NONE'),
    (158, 'cidambarESvaraM', 'Dhunibhinnashadjam', 'DVITIYA', 'Cidambaresvar', '01930000-0000-7000-8000-000000000013'::uuid, NULL, false, NULL, 'NONE'),
    (159, 'cidambara naTarAja mUrtiM', 'Tanukeerti', 'DVITIYA', 'Murtim', '01930000-0000-7000-8000-000000000013'::uuid, NULL, false, NULL, 'NONE'),
    (160, 'cidambara naTarAjaM', 'Kedaram', 'DVITIYA', 'Nataraj', '01930000-0000-7000-8000-000000000013'::uuid, NULL, false, NULL, 'NONE'),
    (161, 'cintayE mahA linga', 'paraju', 'SAPTAMI', 'Cintay', '01930000-0000-7000-8000-000000000037'::uuid, NULL, false, NULL, 'NONE'),
    (162, 'cintayEhaM sadA', 'SankarAbharaNaM', 'DVITIYA', 'Cintayeh', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (163, 'cintaya citta', 'SankarAbharaNaM', 'CHATURTHI', 'Cint', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (164, 'cintaya mA kanda', 'Bhairavi', 'SAMBODHANA', 'Ekambaresvara', '01930000-0000-7000-8000-00000000000f'::uuid, NULL, false, NULL, 'NONE'),
    (165, 'dASarathE', 'SankarAbharaNaM', 'SAPTAMI', 'Dasarath', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (166, 'dAkshAyaNi', 'Hanumatodi', 'SAMBODHANA', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (167, 'dEvi jagadISvari', 'Bhairavi', 'DVITIYA', 'Devi', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (168, 'dIna bandhO', 'SankarAbharaNaM', 'DVITIYA', 'Dina', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (169, 'daNDAyudha pANiM', 'Anandabhairavi', 'DVITIYA', 'Panim', '01930000-0000-7000-8000-000000000025'::uuid, NULL, false, NULL, 'NONE'),
    (170, 'daNDa nAthAya', 'Kamās', 'CHATURTHI', 'Nath', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (171, 'dakshiNA mUrtE', 'SankarAbharaNaM', 'SAPTAMI', 'Murt', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (172, 'dharma samvardhani', 'Madhyamāvathi', 'DVITIYA', 'Dharma', '01930000-0000-7000-8000-000000000040'::uuid, NULL, false, NULL, 'NONE'),
    (173, 'divAkara tanujaM', 'Yadukula Kāmbhoji', 'DVITIYA', 'Sani', '01930000-0000-7000-8000-000000000005'::uuid, 'shani', false, NULL, 'NONE'),
    (174, 'gAna lOlE', 'Nāgavarāli', 'SAPTAMI', 'Lol', '01930000-0000-7000-8000-000000000059'::uuid, NULL, false, NULL, 'NONE'),
    (175, 'gIti cakra ratha', 'Kannada', 'DVITIYA', 'Giti', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (176, 'gOkarNESvara', 'saurAshTraM', 'DVITIYA', 'Gokarnesvara', '01930000-0000-7000-8000-000000000036'::uuid, NULL, false, NULL, 'NONE'),
    (177, 'gOpAla kRshNAya', 'Kāmbhoji', 'CHATURTHI', 'Krshn', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (178, 'gOvardhana girISaM', 'Hindolam', 'DVITIYA', 'Giris', '01930000-0000-7000-8000-000000000027'::uuid, NULL, false, NULL, 'NONE'),
    (179, 'gOvinda rAjAya', 'suraTi', 'CHATURTHI', 'Raj', '01930000-0000-7000-8000-000000000028'::uuid, NULL, false, NULL, 'NONE'),
    (180, 'gOvinda rAjEna', 'mEca bauLi', 'TRITIYA', 'Raj', '01930000-0000-7000-8000-000000000028'::uuid, NULL, false, NULL, 'NONE'),
    (181, 'gOvinda rAjaM', 'Mukhāri', 'DVITIYA', 'Raj', '01930000-0000-7000-8000-000000000028'::uuid, NULL, false, NULL, 'NONE'),
    (182, 'gaNESa kumAra', 'janjUTi', 'DVITIYA', 'Ganesa', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (183, 'gaNa nAyakaM', 'Rudrapriyā', 'DVITIYA', 'Nayak', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (184, 'gaNa patE mahA matE', 'Mechakalyāni', 'SAPTAMI', 'Pat', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (185, 'gaNa rAjEna', 'Ārabhi', 'TRITIYA', 'Raj', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (186, 'gajAdISAdanyaM', 'nATa kuranji', 'DVITIYA', 'Gajadisadany', '01930000-0000-7000-8000-000000000052'::uuid, NULL, false, NULL, 'NONE'),
    (187, 'gajAmbA nAyakO', 'janjUTi', 'DVITIYA', 'Gajamba', '01930000-0000-7000-8000-000000000052'::uuid, NULL, false, NULL, 'NONE'),
    (188, 'gajAnana yutaM', 'Vegavāhini', 'DVITIYA', 'Yut', '01930000-0000-7000-8000-00000000003b'::uuid, NULL, false, NULL, 'NONE'),
    (189, 'gangE mAM pAhi', 'janjUTi', 'SAPTAMI', 'Gang', '01930000-0000-7000-8000-000000000030'::uuid, NULL, false, NULL, 'NONE'),
    (190, 'gaurISAya', 'Ārabhi', 'CHATURTHI', 'Gauris', '01930000-0000-7000-8000-00000000000d'::uuid, NULL, false, NULL, 'NONE'),
    (191, 'gauri giri rAja', 'Gowri', 'DVITIYA', 'Gauri', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (192, 'girijayA ajayA', 'SankarAbharaNaM', 'TRITIYA', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (193, 'guNi janAdi nuta', 'Gurjari', 'DVITIYA', 'Guni', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (194, 'guru guhAdanyaM', 'Bālahamsa', 'PANCHAMI', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (195, 'guru guhAya bhakta', 'sAma', 'CHATURTHI', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (196, 'guru guha bhava', 'Chaturāngini', 'DVITIYA', 'Guru', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (197, 'guru guha pada', 'SankarAbharaNaM', 'DVITIYA', 'Guru', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (198, 'guru guha sarasija', 'SankarAbharaNaM', 'DVITIYA', 'Guru', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (199, 'guru guha svAmini', 'Bhānumati', 'DVITIYA', 'Guru', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (200, 'guru mUrtE bahu', 'SankarAbharaNaM', 'SAPTAMI', 'Murt', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (201, 'hATakESvara', 'Bilahari', 'SAMBODHANA', 'Hatakesvara', '01930000-0000-7000-8000-000000000007'::uuid, NULL, false, NULL, 'NONE'),
    (202, 'hAlAsya nAthaM', 'Darbar', 'SHASHTHI', 'Hal', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (203, 'hE mAyE', 'SankarAbharaNaM', 'SAPTAMI', 'H', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (204, 'hErambAya', 'Atāna', 'CHATURTHI', 'Heramb', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (205, 'hari hara putraM', 'Vasanthā', 'DVITIYA', 'Putr', '01930000-0000-7000-8000-00000000004b'::uuid, NULL, false, NULL, 'NONE'),
    (206, 'hari yuvatIM', 'Deshisimhāravam', 'DVITIYA', 'Yuvatim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (207, 'hasti vadanAya', 'Navaroj', 'CHATURTHI', 'Vadan', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (208, 'himAcala kumArIM', 'Jhankārabhramari', 'DVITIYA', 'Kumarim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (209, 'hima giri kumAri ISa Priya', 'amRta varshiNi', 'DVITIYA', 'Hima', '01930000-0000-7000-8000-00000000002a'::uuid, NULL, false, 'PPNS index places this Amritavarshini kriti at Sattur. Himagiri kumari in Ravikriya stays on Brihannayaki at Thanjavur.', 'NONE'),
    (210, 'hima giri kumAri ISvari', 'Ravi Kriyā', 'DVITIYA', 'Hima', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (211, 'hiraNmayIM lakshmIM', 'Lalitā', 'DVITIYA', 'Hiranmayim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, 'Vairagya: Composed in Lalita raga when his wife suggested singing in praise of wealthy patrons; Dikshitar firmly refused court wealth.', 'NONE'),
    (212, 'jagadISa guru guha', 'SankarAbharaNaM', 'DVITIYA', 'Jagadisa', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (213, 'jagadISa manOhari', 'Eeshamanohari', 'DVITIYA', 'Jagadisa', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (214, 'jambU patE', 'yamunA kalyANi', 'DVITIYA', 'Jambukesvara', '01930000-0000-7000-8000-000000000010'::uuid, NULL, false, NULL, 'NONE'),
    (215, 'jayati SivA', 'Bhavāni', 'DVITIYA', 'Jayati', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (216, 'jnAnAmbikE', 'sAnAgraNi', 'SAPTAMI', 'Jnanambik', '01930000-0000-7000-8000-000000000034'::uuid, NULL, false, 'Jnanambika of Konkanesvara. Anandavalli at Vennatrankarai is the Chayavati kriti.', 'NONE'),
    (217, 'jnAna prasUnAmbikE', 'Mechakalyāni', 'SAPTAMI', 'Prasunambik', '01930000-0000-7000-8000-000000000012'::uuid, NULL, false, NULL, 'NONE'),
    (218, 'kASi viSAlAkshIM', 'Gamakakriyā', 'DVITIYA', 'Visalakshim', '01930000-0000-7000-8000-000000000030'::uuid, NULL, false, NULL, 'NONE'),
    (219, 'kASi viSvESvara', 'Kāmbhoji', 'DVITIYA', 'Kasi', '01930000-0000-7000-8000-000000000030'::uuid, NULL, false, NULL, 'NONE'),
    (220, 'kAdambarI priyAyai', 'Mohanam', 'DVITIYA', 'Kadambari', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (221, 'kAla bhairavaM', 'Bhairavam', 'DVITIYA', 'Bhairav', '01930000-0000-7000-8000-000000000030'::uuid, NULL, false, NULL, 'NONE'),
    (222, 'kAmAkshIM kalyANIM', 'Mechakalyāni', 'DVITIYA', 'Kamakshim', '01930000-0000-7000-8000-00000000002f'::uuid, NULL, false, NULL, 'NONE'),
    (223, 'kAmAkshi kAma kOTi', 'sumadyuti', 'DVITIYA', 'Kamakshi', '01930000-0000-7000-8000-00000000002f'::uuid, NULL, false, NULL, 'NONE'),
    (224, 'kAmAkshi mAM pAhi', 'Shuddha Desi', 'DVITIYA', 'Mam', '01930000-0000-7000-8000-00000000002f'::uuid, NULL, false, NULL, 'NONE'),
    (225, 'kAmAkshi vara lakshmi', 'Bilahari', 'DVITIYA', 'Kamakshi', '01930000-0000-7000-8000-00000000002f'::uuid, NULL, false, NULL, 'NONE'),
    (226, 'kAmESvarENa', 'Sri', 'TRITIYA', 'Kamesvar', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (227, 'kAma kOTi pITha', 'saugandhini', 'DVITIYA', 'Kama', '01930000-0000-7000-8000-000000000020'::uuid, NULL, false, NULL, 'NONE'),
    (228, 'kAncISaM', 'SankarAbharaNaM', 'DVITIYA', 'Kancis', '01930000-0000-7000-8000-00000000000f'::uuid, NULL, false, 'Nottusvara. Kancisa is the lord of Kanchi. Manali is where the European air was heard, not the shrine.', 'NONE'),
    (229, 'kAyArOhaNESaM', 'dEva gAndhAraM', 'DVITIYA', 'Kayarohanes', '01930000-0000-7000-8000-000000000032'::uuid, NULL, false, NULL, 'NONE'),
    (230, 'kOdaNDa rAmaM', 'Kokilāravam', 'DVITIYA', 'Ram', '01930000-0000-7000-8000-00000000003d'::uuid, NULL, false, NULL, 'NONE'),
    (231, 'kRshNAnanda', 'gauLipantu', 'DVITIYA', 'Krshnananda', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (232, 'kailAsa nAthEna', 'Kāmbhoji', 'TRITIYA', 'Nath', '01930000-0000-7000-8000-00000000002d'::uuid, NULL, false, NULL, 'NONE'),
    (233, 'kailAsa nAthaM', 'Vegavāhini', 'DVITIYA', 'Nath', '01930000-0000-7000-8000-00000000002d'::uuid, NULL, false, NULL, 'NONE'),
    (234, 'kalAvati kamalAsana', 'Kalāvathi', 'DVITIYA', 'Kalavati', '01930000-0000-7000-8000-00000000004e'::uuid, NULL, false, NULL, 'NONE'),
    (235, 'kamalAmbA saMrakshatu', 'Anandabhairavi', 'PRATHAMA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (236, 'kamalAmbAM bhajarE', 'Mechakalyāni', 'DVITIYA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (237, 'kamalAmbikAyAstava', 'Punnagavarali', 'SHASHTHI', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (238, 'kamalAmbikAyai kanaka', 'Kāmbhoji', 'CHATURTHI', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (239, 'kamalAmbikE ASrita', 'Hanumatodi', 'SAMBODHANA', 'Kamalamba', '01930000-0000-7000-8000-000000000002'::uuid, NULL, false, NULL, 'NONE'),
    (240, 'kamalAsana vandita', 'SankarAbharaNaM', 'DVITIYA', 'Kamalasana', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (241, 'kanakAmbari kAruNya', 'Kanakāmbari', 'DVITIYA', 'Kanakambari', '01930000-0000-7000-8000-000000000020'::uuid, NULL, false, NULL, 'NONE'),
    (242, 'kanaka sabhA patiM', 'Mālavashree', 'DVITIYA', 'Patim', '01930000-0000-7000-8000-000000000013'::uuid, NULL, false, NULL, 'NONE'),
    (243, 'kanja daLAyatAkshi', 'Manohari', 'DVITIYA', 'Kanja', '01930000-0000-7000-8000-000000000020'::uuid, NULL, false, NULL, 'NONE'),
    (244, 'kari kaLabha', 'Sāveri', 'DVITIYA', 'Kari', '01930000-0000-7000-8000-00000000000d'::uuid, NULL, false, NULL, 'NONE'),
    (245, 'kaumAri gauri', 'gauri vAlAvali', 'DVITIYA', 'Kaumari', '01930000-0000-7000-8000-000000000020'::uuid, NULL, false, NULL, 'NONE'),
    (246, 'kshitijA ramaNaM', 'Devagāndhāri', 'DVITIYA', 'Raman', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (247, 'kumAra svAminaM', 'Asāveri', 'DVITIYA', 'Svamin', '01930000-0000-7000-8000-000000000059'::uuid, NULL, false, NULL, 'NONE'),
    (248, 'kumbhESvarAya namastE', 'Mechakalyāni', 'CHATURTHI', 'Kumbhesvar', '01930000-0000-7000-8000-000000000018'::uuid, NULL, false, NULL, 'NONE'),
    (249, 'kumbhESvarENa', 'Mechakalyāni', 'TRITIYA', 'Kumbhesvar', '01930000-0000-7000-8000-000000000018'::uuid, NULL, false, NULL, 'NONE'),
    (250, 'kusumAkara SObhita', 'Kusumākaram', 'DVITIYA', 'Kusumakara', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (251, 'kusumAkara vimAna', 'Āhiri', 'DVITIYA', 'Kusumakara', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (252, 'lalitA paramESvari', 'suraTi', 'DVITIYA', 'Lalita', '01930000-0000-7000-8000-000000000047'::uuid, NULL, false, NULL, 'NONE'),
    (253, 'lalitAmbikAM', 'Devakriya', 'DVITIYA', 'Lalitambik', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (254, 'lalitAmbikAyai', 'Bhairavi', 'DVITIYA', 'Lalitambika', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (255, 'lambOdarAya', 'Varāli', 'CHATURTHI', 'Lambodar', '01930000-0000-7000-8000-00000000003e'::uuid, NULL, false, NULL, 'NONE'),
    (256, 'mAdhavO mAM pAtu', 'Nāṭṭai', 'DVITIYA', 'Mam', '01930000-0000-7000-8000-000000000023'::uuid, NULL, false, NULL, 'NONE'),
    (257, 'mAmava mInAkshi', 'Varāli', 'SAMBODHANA', 'Minakshi', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (258, 'mAmava paTTAbhirAma', 'Manirangu', 'SAMBODHANA', 'Pattabhirama', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (259, 'mAmava raghuvIra', 'Mahuri', 'SAMBODHANA', 'Raghuvira', '01930000-0000-7000-8000-00000000003d'::uuid, NULL, false, NULL, 'NONE'),
    (260, 'mAnasa guru guha', 'Anandabhairavi', 'DVITIYA', 'Guruguha', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (261, 'mAra kOTi kOTi', 'Ārabhi', 'DVITIYA', 'Mara', '01930000-0000-7000-8000-00000000005e'::uuid, NULL, false, NULL, 'NONE'),
    (262, 'mAra rati priyaM', 'Rathipriyā', 'DVITIYA', 'Priy', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (263, 'mArga hindOLa', 'mArgahindOLaM', 'DVITIYA', 'Marga', '01930000-0000-7000-8000-00000000003a'::uuid, NULL, false, NULL, 'NONE'),
    (264, 'mArga sahAyESvaraM', 'Kāshirāmakriyā', 'DVITIYA', 'Sahayesvar', '01930000-0000-7000-8000-00000000003a'::uuid, NULL, false, NULL, 'NONE'),
    (265, 'mAruvakAdi mAlini', 'Maruva', 'DVITIYA', 'Maruvakadi', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (266, 'mAtangi Sri', 'Ramāmanohari', 'DVITIYA', 'Matangi', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, 'PPNS mela list places Matangi in Ramamanohari on Brihannayaki.', 'NONE'),
    (267, 'mAtangi marakatAngi', 'dhauta pancamaM', 'DVITIYA', 'Matangi', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (268, 'mAyE citkalE', 'SankarAbharaNaM', 'SAPTAMI', 'May', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (269, 'mAyE tvaM yAhi', 'Tarangini', 'SAPTAMI', 'May', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (270, 'mAyUra nAthaM', 'Dhanyāsi', 'DVITIYA', 'Nath', '01930000-0000-7000-8000-00000000000d'::uuid, NULL, false, NULL, 'NONE'),
    (271, 'mInAkshi mE mudaM', 'Gamakakriyā', 'SAPTAMI', 'M', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (272, 'mOhana nATa', 'Mohananāta', 'DVITIYA', 'Mohana', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (273, 'madhurAmbA jayati', 'paraju', 'DVITIYA', 'Madhuramba', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (274, 'madhurAmbA saMrakshatu', 'Devakriya', 'DVITIYA', 'Madhuramba', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (275, 'madhurAmbAM bhaja', 'Sthavarājam', 'DVITIYA', 'Madhuramb', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (276, 'madhurAmbAyAstava', 'Begada', 'DVITIYA', 'Madhurambayastava', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (277, 'madhurAmbikAyAM', 'Deshisimhāravam', 'DVITIYA', 'Madhurambikay', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (278, 'mahA dEvEna', 'Deva Manohari', 'TRITIYA', 'Dev', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (279, 'mahA gaNa patE', 'naTa nArAyaNi', 'SAPTAMI', 'Pat', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (280, 'mahA gaNa patiM manasA', 'Nāṭṭai', 'DVITIYA', 'Patim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (281, 'mahA gaNa patiM vandE', 'Hanumatodi', 'DVITIYA', 'Patim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (282, 'mahA lakshmi karuNa', 'Mādhavamanohari', 'DVITIYA', 'Maha', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (283, 'mahA lingESvarAya', 'Atāna', 'CHATURTHI', 'Lingesvar', '01930000-0000-7000-8000-000000000037'::uuid, NULL, false, NULL, 'NONE'),
    (284, 'mahA lingESvaraM', 'paraju', 'DVITIYA', 'Lingesvar', '01930000-0000-7000-8000-000000000037'::uuid, NULL, false, NULL, 'NONE'),
    (285, 'mahA suraM kEtuM', 'Chāmaram', 'DVITIYA', 'Ketu', '01930000-0000-7000-8000-000000000005'::uuid, 'ketu', false, NULL, 'NONE'),
    (286, 'mahA tripura sundari', 'Madhyamāvathi', 'DVITIYA', 'Maha', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (287, 'mahishAsura mardinIM', 'Nārāyani', 'DVITIYA', 'Mardinim', '01930000-0000-7000-8000-00000000003d'::uuid, NULL, false, NULL, 'NONE'),
    (288, 'mahishAsura mardini', 'Gowla', 'DVITIYA', 'Mahishasura', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (289, 'mangaLAmbAyai', 'Mālavashree', 'DVITIYA', 'Mangalamba', '01930000-0000-7000-8000-00000000005a'::uuid, NULL, false, NULL, 'NONE'),
    (290, 'mangaLa dEvatE', 'Mārgadesi', 'SAPTAMI', 'Devat', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (291, 'mangaLa dEvatayA', 'Dhanyāsi', 'CHATURTHI', 'Devat', '01930000-0000-7000-8000-00000000005b'::uuid, NULL, false, 'Lakshmi in the Varadaraja temple.', 'NONE'),
    (292, 'mangaLaM jaya', 'Vasanthā', 'DVITIYA', 'Mangal', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (293, 'marakata lingaM', 'Vasanthā', 'DVITIYA', 'Ling', '01930000-0000-7000-8000-000000000039'::uuid, NULL, false, NULL, 'NONE'),
    (294, 'marakata vallIM', 'Kāmbhoji', 'DVITIYA', 'Vallim', '01930000-0000-7000-8000-00000000003a'::uuid, NULL, false, NULL, 'NONE'),
    (295, 'matsyAvatAra', 'Bhinnapanchamam', 'DVITIYA', 'Matsyavatara', '01930000-0000-7000-8000-00000000001d'::uuid, NULL, false, NULL, 'NONE'),
    (296, 'mucukunda varada', 'SankarAbharaNaM', 'DVITIYA', 'Mucukunda', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, 'Nottusvara. Mucukunda-varada is an epithet of Tyagaraja. Manali is the tune''s setting, not the shrine.', 'NONE'),
    (297, 'mura harENa', 'Suddha Mukhāri', 'TRITIYA', 'Har', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (298, 'nAgAbharaNaM', 'Nāgabharanam', 'DVITIYA', 'Nagabharan', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (299, 'nAga gAndhAri', 'Nāgagāndhāri', 'DVITIYA', 'Naga', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (300, 'nAga lingaM bhajEhaM', 'SankarAbharaNaM', 'DVITIYA', 'Ling', '01930000-0000-7000-8000-00000000000d'::uuid, NULL, false, NULL, 'NONE'),
    (301, 'nAga lingaM namAmi', 'Mohanam', 'DVITIYA', 'Ling', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (302, 'nI sATi daivamendu', 'Ranjani', 'DVITIYA', 'Ni', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (303, 'nIlAcala nAthaM', 'sumadyuti', 'DVITIYA', 'Nath', '01930000-0000-7000-8000-00000000002b'::uuid, NULL, false, 'The kshetra index names Nilachala. Identified here as Puri.', 'NONE'),
    (304, 'nIlAngaM hariM', 'Neelāmbari', 'DVITIYA', 'Nilang', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (305, 'nIlOtpalAmbA jayati', 'nArAyaNa gauLa', 'PRATHAMA', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL, 'NONE'),
    (306, 'nIlOtpalAmbAM bhajarE', 'Nārīrītigowla', 'DVITIYA', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL, 'NONE'),
    (307, 'nIlOtpalAmbikAyAH paraM', 'Gowla', 'PANCHAMI', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL, 'NONE'),
    (308, 'nIlOtpalAmbikAyAM bhaktiM', 'pUrva gauLa', 'SAPTAMI', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL, 'NONE'),
    (309, 'nIlOtpalAmbikAyAstava', 'mAyA mALavagauLa', 'SHASHTHI', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL, 'NONE'),
    (310, 'nIlOtpalAmbikAyai namastE', 'kEdAra gauLa', 'CHATURTHI', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL, 'NONE'),
    (311, 'nIlOtpalAmbikE nitya', 'chAyA gauLa', 'SAMBODHANA', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL, 'NONE'),
    (312, 'nIlOtpalAmbikayA nirvANa', 'kannaDa gauLa', 'TRITIYA', 'Nilotpalamba', '01930000-0000-7000-8000-000000000003'::uuid, NULL, false, NULL, 'NONE'),
    (313, 'nIla kaNTa mahA dEva', 'Vasanthā', 'DVITIYA', 'Nila', '01930000-0000-7000-8000-000000000031'::uuid, NULL, false, NULL, 'NONE'),
    (314, 'nIla kaNThAya namO', 'nAda rAmakriya', 'CHATURTHI', 'Kanth', '01930000-0000-7000-8000-00000000002c'::uuid, NULL, false, NULL, 'NONE'),
    (315, 'nIla kaNThaM bhajEhaM', 'kEdAra gauLa', 'DVITIYA', 'Kanth', '01930000-0000-7000-8000-00000000002c'::uuid, NULL, false, NULL, 'NONE'),
    (316, 'nIrajAkshi kAmAkshi', 'Hindolam', 'DVITIYA', 'Nirajakshi', '01930000-0000-7000-8000-00000000002f'::uuid, NULL, false, NULL, 'NONE'),
    (317, 'nabhO maNi', 'Nabhomani', 'DVITIYA', 'Nabho', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (318, 'namO namastE gIrvANi', 'Geervāni', 'SAPTAMI', 'Namast', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (319, 'namastE para dEvatE', 'Devaranji', 'SAPTAMI', 'Namast', '01930000-0000-7000-8000-000000000020'::uuid, NULL, false, 'Namaste paradevate addresses Kamakshi, with the other Tanjavur Kamakshi mela kritis.', 'NONE'),
    (320, 'nanda gOpAla', 'yamunA kalyANi', 'DVITIYA', 'Nanda', '01930000-0000-7000-8000-000000000026'::uuid, NULL, false, NULL, 'NONE'),
    (321, 'nara harim', 'Jayashuddhamālavi', 'DVITIYA', 'Harim', '01930000-0000-7000-8000-000000000061'::uuid, NULL, false, NULL, 'NONE'),
    (322, 'narasiMhAgaccha', 'Mohanam', 'DVITIYA', 'Narasimhagaccha', '01930000-0000-7000-8000-000000000061'::uuid, NULL, false, NULL, 'NONE'),
    (323, 'narmadA kAvEri', 'Nāmadeshi', 'DVITIYA', 'Narmada', '01930000-0000-7000-8000-000000000040'::uuid, NULL, false, NULL, 'NONE'),
    (324, 'nava ratna mAlinIM', 'Gamakakriyā', 'DVITIYA', 'Malinim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (325, 'nava ratna vilAsa', 'Navarathna Vilāsam', 'DVITIYA', 'Nava', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (326, 'nishadhAdi', 'nishada', 'DVITIYA', 'Nishadhadi', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (327, 'pAhi durgE', 'SankarAbharaNaM', 'SAMBODHANA', 'Durg', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (328, 'pAhi mAM janakI vallabha', 'SankarAbharaNaM', 'SAMBODHANA', 'Mam', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (329, 'pAhi mAM pArvati paramESvari', 'Mohanam', 'SAMBODHANA', 'Mam', '01930000-0000-7000-8000-000000000047'::uuid, NULL, false, NULL, 'NONE'),
    (330, 'pAhi mAM ratnAcala nAyaka', 'Mukhāri', 'SAMBODHANA', 'Mam', '01930000-0000-7000-8000-00000000002c'::uuid, NULL, false, NULL, 'NONE'),
    (331, 'pAlaya mAM bRhadISvara', 'Nāyaki', 'CHATURTHI', 'Pal', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (332, 'pAlaya mAM bRhadISvari', 'Hanumatodi', 'CHATURTHI', 'Pal', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (333, 'pAlaya mAM pArvatISa', 'Kannada', 'CHATURTHI', 'Pal', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (334, 'pAlaya mAM paramESvari', 'Tarangini', 'CHATURTHI', 'Pal', '01930000-0000-7000-8000-000000000047'::uuid, NULL, false, NULL, 'NONE'),
    (335, 'pAmara jana pAlini', 'sumadyuti', 'DVITIYA', 'Pamara', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (336, 'pArvatI kumAraM', 'nATa kuranji', 'DVITIYA', 'Kumar', '01930000-0000-7000-8000-00000000003a'::uuid, NULL, false, NULL, 'NONE'),
    (337, 'pArvatI patE', 'SankarAbharaNaM', 'SAPTAMI', 'Pat', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (338, 'pArvatI patiM', 'Hamsadhwani', 'DVITIYA', 'Patim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (339, 'pArvatISvarENa', 'Bhooshāvathi', 'TRITIYA', 'Parvatisvar', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (340, 'pIta varNaM', 'SankarAbharaNaM', 'DVITIYA', 'Varn', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (341, 'pUrNa candra bimba', 'rAga mAlikA', 'DVITIYA', 'Purna', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (342, 'paSupatISvaraM', 'Shivapanthuvarāli', 'DVITIYA', 'Pasupatisvar', '01930000-0000-7000-8000-000000000043'::uuid, NULL, false, NULL, 'NONE'),
    (343, 'pancASatpITha rUpiNi', 'dEva gAndhAraM', 'DVITIYA', 'Pancasatpitha', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (344, 'panca bhUta kiraNAvaLIM', 'Keeranāvali', 'DVITIYA', 'Kiranavalim', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (345, 'panca mAtanga mukha', 'Malahari', 'DVITIYA', 'Panca', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (346, 'pankaja mukha', 'SankarAbharaNaM', 'DVITIYA', 'Pankaja', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (347, 'pannaga Sayana', 'Madhyamāvathi', 'DVITIYA', 'Pannaga', '01930000-0000-7000-8000-00000000003f'::uuid, NULL, false, NULL, 'NONE'),
    (348, 'parA Sakti ISvari', 'gauri vElAvali', 'DVITIYA', 'Para', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (349, 'parA SaktiM', 'Rudrapriyā', 'DVITIYA', 'Saktim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (350, 'para dEvatA bRhatkucAmbA', 'Dhanyāsi', 'DVITIYA', 'Para', '01930000-0000-7000-8000-000000000037'::uuid, NULL, false, NULL, 'NONE'),
    (351, 'para dEvatE bhakta pUjitE', 'huSani', 'SAPTAMI', 'Devat', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (352, 'para dEvatE bhava', 'SankarAbharaNaM', 'SAPTAMI', 'Devat', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (353, 'para dEvatE namastE', 'Anandabhairavi', 'SAPTAMI', 'Devat', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (354, 'paramESvarEna', 'Poorvavarāli', 'TRITIYA', 'Paramesvar', '01930000-0000-7000-8000-000000000047'::uuid, NULL, false, NULL, 'NONE'),
    (355, 'paramESvara jagadISvara', 'cala nATa', 'DVITIYA', 'Paramesvara', '01930000-0000-7000-8000-000000000040'::uuid, NULL, false, 'Paramesvara Jagadisvara in Nata is the Tiruvaiyaru group. Ramanatha of Rameswaram is a different kriti.', 'NONE'),
    (356, 'parama SivAtmajaM', 'yamunA kalyANi', 'DVITIYA', 'Sivatmaj', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (357, 'parandhAmavatI', 'Dharmavati', 'DVITIYA', 'Parandhamavati', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (358, 'paranjyOtishmatI', 'Jyothi', 'DVITIYA', 'Paranjyotishmati', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (359, 'parimaLa ranga nAthaM1', 'hamIr kalyANi', 'DVITIYA', 'Parimala', '01930000-0000-7000-8000-000000000041'::uuid, NULL, false, NULL, 'NONE'),
    (360, 'parimaLa ranga nAthaM2', 'hamIr kalyANi', 'DVITIYA', 'Parimala', '01930000-0000-7000-8000-000000000041'::uuid, NULL, false, NULL, 'NONE'),
    (361, 'parvata rAja kumAri', 'Ranjani', 'DVITIYA', 'Parvata', '01930000-0000-7000-8000-00000000003e'::uuid, NULL, false, NULL, 'NONE'),
    (362, 'parvata vardhani', 'sAma', 'DVITIYA', 'Parvata', '01930000-0000-7000-8000-000000000047'::uuid, NULL, false, NULL, 'NONE'),
    (363, 'pavanAtmajAgaccha', 'cala nATa', 'DVITIYA', 'Pavanatmajagaccha', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (364, 'pavanAtmajaM', 'SankarAbharaNaM', 'DVITIYA', 'Pavanatmaj', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (365, 'praNatArti harAya', 'Sāmanta', 'CHATURTHI', 'Har', '01930000-0000-7000-8000-000000000040'::uuid, NULL, false, NULL, 'NONE'),
    (366, 'praNatArti haraM', 'Nāyaki', 'DVITIYA', 'Har', '01930000-0000-7000-8000-000000000040'::uuid, NULL, false, NULL, 'NONE'),
    (367, 'prasanna vEnkaTESvaraM', 'Vātee Vasantabhairavi', 'DVITIYA', 'Venkatesvar', '01930000-0000-7000-8000-000000000044'::uuid, NULL, false, 'The Vativasantabhairavi mela kriti is the Tanjavur Venkatesa, not Tirupati.', 'NONE'),
    (368, 'pratyangirA', 'nAda rAmakriya', 'DVITIYA', 'Pratyangira', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (369, 'pura hara nandana', 'hamIr kalyANi', 'DVITIYA', 'Pura', '01930000-0000-7000-8000-00000000000b'::uuid, NULL, false, NULL, 'NONE'),
    (370, 'rAjIva lOcanaM', 'SankarAbharaNaM', 'DVITIYA', 'Locan', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (371, 'rAja gOpAlaM', 'Mohanam', 'DVITIYA', 'Gopal', '01930000-0000-7000-8000-000000000046'::uuid, NULL, false, NULL, 'NONE'),
    (372, 'rAja rAjEndra', 'Gundakriya', 'DVITIYA', 'Raja', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (373, 'rAmE bharata', 'Jyothi', 'SAPTAMI', 'Ram', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (374, 'rAma candrAdanyaM', 'Dhanyāsi', 'DVITIYA', 'Candradany', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (375, 'rAma candrAya namastE', 'Hanumatodi', 'CHATURTHI', 'Candr', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (376, 'rAma candrEna saMrakshitOhaM', 'Mānji', 'TRITIYA', 'Candr', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (377, 'rAma candra bhaktaM', 'Geya Hejjajji', 'DVITIYA', 'Bhakt', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (378, 'rAma candraM bhAvayAMi', 'Vasanthā', 'DVITIYA', 'Candr', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (379, 'rAma candraM rAjIvAkshaM', 'SankarAbharaNaM', 'DVITIYA', 'Candr', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (380, 'rAma candrasya dAsOhaM', 'Dharmavati', 'SHASHTHI', 'Candr', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (381, 'rAma janArdana', 'SankarAbharaNaM', 'DVITIYA', 'Rama', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (382, 'rAma kRshNEna', 'Sahāna', 'TRITIYA', 'Krshn', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (383, 'rAma nAthaM bhajEhaM', 'Kāshirāmakriyā', 'DVITIYA', 'Nath', '01930000-0000-7000-8000-000000000047'::uuid, NULL, false, NULL, 'NONE'),
    (384, 'rAma rAma kali', 'Rāmakali', 'DVITIYA', 'Rama', '01930000-0000-7000-8000-000000000048'::uuid, NULL, false, NULL, 'NONE'),
    (385, 'rENukA dEvi', 'Kannadabangāla', 'DVITIYA', 'Renuka', '01930000-0000-7000-8000-00000000004a'::uuid, NULL, false, 'Vijayapuram, on the northern edge of Tiruvarur.', 'NONE'),
    (386, 'rakta gaNa patiM', 'Mohanam', 'DVITIYA', 'Patim', '01930000-0000-7000-8000-00000000003f'::uuid, NULL, false, NULL, 'NONE'),
    (387, 'ranga nAyakaM', 'Nāyaki', 'DVITIYA', 'Nayak', '01930000-0000-7000-8000-000000000049'::uuid, NULL, false, NULL, 'NONE'),
    (388, 'ranga pura vihAra', 'bRndAvana sAranga', 'DVITIYA', 'Ranga', '01930000-0000-7000-8000-000000000049'::uuid, NULL, false, NULL, 'NONE'),
    (389, 'rudra kOpa jAta', 'Rudrapriyā', 'DVITIYA', 'Rudra', '01930000-0000-7000-8000-000000000055'::uuid, NULL, false, NULL, 'NONE'),
    (390, 'sAdhu jana citta', 'Poornapanchamam', 'DVITIYA', 'Sadhu', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (391, 'sAdhu jana vinutiM', 'Geethapriyā', 'DVITIYA', 'Vinutim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (392, 'sAma gAna priyE', 'SankarAbharaNaM', 'SAPTAMI', 'Priy', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (393, 'sAmba sadASivAya', 'Kāmbhoji', 'CHATURTHI', 'Sadasiv', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (394, 'sAranga rAga priyE', 'Sāranga', 'SAPTAMI', 'Priy', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (395, 'sArasa daLa nayana', 'Kamās', 'DVITIYA', 'Sarasa', '01930000-0000-7000-8000-000000000046'::uuid, NULL, false, NULL, 'NONE'),
    (396, 'sEnA patE', 'Kāshirāmakriyā', 'TRITIYA', 'S', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (397, 'sOmAskandaM', 'SankarAbharaNaM', 'DVITIYA', 'Somaskand', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (398, 'sOma sundarESvaraM', 'Suddha vasataM', 'DVITIYA', 'Sundaresvar', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (399, 'sUrya mUrtE', 'saurAshTraM', 'SAMBODHANA', 'Surya', '01930000-0000-7000-8000-000000000005'::uuid, 'surya', false, NULL, 'NONE'),
    (400, 'saccidAnanda maya', 'Kumbhini', 'CHATURTHI', 'M', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (401, 'sadA vinata sAdarE', 'Revagupti', 'SAPTAMI', 'Sadar', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (402, 'sadASivEna', 'Sindhu Rāmakriya', 'TRITIYA', 'Sadasiv', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (403, 'sadASiva jAyE', 'SankarAbharaNaM', 'SAPTAMI', 'Jay', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (404, 'sadASivaM upAsmahE', 'SankarAbharaNaM', 'DVITIYA', 'Sadasiv', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (405, 'sadASrayE abhayAmbikE', 'Chāmaram', 'SAMBODHANA', 'Abhayamba', '01930000-0000-7000-8000-00000000000e'::uuid, NULL, false, NULL, 'NONE'),
    (406, 'sadAcalESvaraM', 'Bhoopālam', 'DVITIYA', 'Achalesvara', '01930000-0000-7000-8000-000000000006'::uuid, NULL, false, NULL, 'NONE'),
    (407, 'saindhavi rAga priyE', 'Saindhavi', 'SAPTAMI', 'Priy', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (408, 'sakala sura vinuta', 'SankarAbharaNaM', 'DVITIYA', 'Sakala', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (409, 'sandhyA dEvIM', 'Devakriya', 'DVITIYA', 'Devim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (410, 'santAna gOpAla kRshNaM', 'Kamās', 'DVITIYA', 'Krshn', '01930000-0000-7000-8000-000000000046'::uuid, NULL, false, NULL, 'NONE'),
    (411, 'santAna manjari', 'Santhāna Manjari', 'DVITIYA', 'Santana', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (412, 'santAna rAma svAminaM', 'Hindolavasanta', 'DVITIYA', 'Svamin', '01930000-0000-7000-8000-000000000060'::uuid, NULL, false, NULL, 'NONE'),
    (413, 'santAna saubhAgya', 'SankarAbharaNaM', 'DVITIYA', 'Santana', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (414, 'santataM gOvinda rAjaM', 'SankarAbharaNaM', 'DVITIYA', 'Santat', '01930000-0000-7000-8000-000000000028'::uuid, NULL, false, 'Nottusvara on Govindaraja of Chidambaram. Manali is the tune''s setting.', 'NONE'),
    (415, 'santataM pAhi mAM', 'SankarAbharaNaM', 'DVITIYA', 'Santat', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (416, 'sarasa sauvIra', 'sauvIraM', 'DVITIYA', 'Sarasa', '01930000-0000-7000-8000-000000000045'::uuid, NULL, false, NULL, 'NONE'),
    (417, 'sarasija nAbha sOdari', 'Nāgagāndhāri', 'DVITIYA', 'Sarasija', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (418, 'sarasvati chAyA tarangiNi', 'Chāyatārangini', 'CHATURTHI', 'Ch', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (419, 'sarasvati manOhari', 'Saraswathi Manohari', 'DVITIYA', 'Sarasvati', '01930000-0000-7000-8000-000000000020'::uuid, NULL, false, NULL, 'NONE'),
    (420, 'sarasvati vidhi yuvati', 'Hindolam', 'DVITIYA', 'Sarasvati', '01930000-0000-7000-8000-000000000016'::uuid, NULL, false, NULL, 'NONE'),
    (421, 'sarasvatyA bhagavatyA', 'chAyA gauLa', 'DVITIYA', 'Sarasvatya', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (422, 'saundara rAjaM', 'bRndAvani', 'DVITIYA', 'Raj', '01930000-0000-7000-8000-000000000050'::uuid, NULL, false, 'Vishnu shrine of Nagapattinam. Kayarohanesvara is the Siva of the same town.', 'NONE'),
    (423, 'saura sEnESaM', 'saura sEna', 'DVITIYA', 'Senes', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (424, 'shaDAnanE', 'Kamās', 'SAPTAMI', 'Shadanan', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (425, 'siMhAsana sthitE', 'rAga mAlikA', 'SAPTAMI', 'Sthit', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (426, 'siddhISvarAya', 'Neelāmbari', 'CHATURTHI', 'Siddhisvara', '01930000-0000-7000-8000-00000000000a'::uuid, NULL, false, NULL, 'NONE'),
    (427, 'siddhi vinAyakaM', 'Chāmaram', 'DVITIYA', 'Vinayak', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (428, 'smarAmyahaM sadA', 'Ramāmanohari', 'DVITIYA', 'Rahu', '01930000-0000-7000-8000-000000000005'::uuid, 'rahu', false, NULL, 'NONE'),
    (429, 'stava rAjAdi', 'Sthavarājam', 'DVITIYA', 'Stava', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (430, 'subrahmaNyEna', 'Suddha Dhanyāsi', 'TRITIYA', 'Subrahmany', '01930000-0000-7000-8000-000000000033'::uuid, NULL, false, 'Subrahmanyena is Kazhugumalai. Sri Subrahmanyo mam stays at Tiruchendur.', 'NONE'),
    (431, 'subrahmaNyaM', 'SankarAbharaNaM', 'DVITIYA', 'Subrahmany', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (432, 'sundarESvarAya', 'SankarAbharaNaM', 'CHATURTHI', 'Sundaresvar', '01930000-0000-7000-8000-00000000003c'::uuid, NULL, false, NULL, 'NONE'),
    (433, 'sundara mUrtim', 'Takka', 'DVITIYA', 'Murtim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (434, 'svAmi nAthEna', 'bRndAvani', 'TRITIYA', 'Nath', '01930000-0000-7000-8000-000000000054'::uuid, NULL, false, NULL, 'NONE'),
    (435, 'svAmi nAtha', 'cala nATa', 'DVITIYA', 'Svami', '01930000-0000-7000-8000-000000000054'::uuid, NULL, false, NULL, 'NONE'),
    (436, 'tArakESvara', 'SankarAbharaNaM', 'DVITIYA', 'Tarakesvara', '01930000-0000-7000-8000-00000000000d'::uuid, NULL, false, NULL, 'NONE'),
    (437, 'tiruvaTISvaraM', 'Gamakakriyā', 'DVITIYA', 'Tiruvatisvar', '01930000-0000-7000-8000-000000000056'::uuid, NULL, false, NULL, 'NONE'),
    (438, 'trilOcana mOhinIM', 'Bhairavi', 'DVITIYA', 'Mohinim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (439, 'tripura sundari Sankari', 'sAma', 'DVITIYA', 'Tripura', '01930000-0000-7000-8000-000000000056'::uuid, NULL, false, NULL, 'NONE'),
    (440, 'tripura sundari namOstu tE', 'Deva Manohari', 'SAPTAMI', 'T', '01930000-0000-7000-8000-000000000056'::uuid, NULL, false, NULL, 'NONE'),
    (441, 'tyAgESaM bhajarE', 'Rudrapriyā', 'DVITIYA', 'Tyages', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (442, 'tyAgarAjAdanyaM', 'Darbar', 'PANCHAMI', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (443, 'tyAgarAjAya namastE', 'Begada', 'CHATURTHI', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (444, 'tyAgarAjE kRtyAkRtyaM', 'Sāranga', 'SAPTAMI', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (445, 'tyAgarAjEna', 'Sālagabhairavi', 'TRITIYA', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (446, 'tyAgarAjO virAjatE', 'Atāna', 'PRATHAMA', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (447, 'tyAgarAja mahadhvaja', 'Sri', 'DVITIYA', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (448, 'tyAgarAja pAlayASu', 'Gowla', 'DVITIYA', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (449, 'tyAgarAja yOga', 'Anandabhairavi', 'DVITIYA', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, 'Poetic architecture: Masterpiece in Anandabhairavi featuring double yati structures (gopuccha yati and srotovaha yati).', 'GOPUCCHA'),
    (450, 'tyAgarAjaM bhajEhaM', 'Neelāmbari', 'DVITIYA', 'Tyagaraj', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (451, 'tyAgarAjaM bhajarE', 'Yadukula Kāmbhoji', 'DVITIYA', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (452, 'ucchishTa gaNapatau', 'Kāshirāmakriyā', 'DVITIYA', 'Ucchishta', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (453, 'vAgdEvi mAmava', 'SankarAbharaNaM', 'DVITIYA', 'Vagdevi', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (454, 'vAmAnka sthitayA', 'Atāna', 'CHATURTHI', 'Sthit', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (455, 'vArAhIM', 'Vegavāhini', 'DVITIYA', 'Varahim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (456, 'vAsu dEvamupAsmahE', 'Mālavapanchamam', 'SAPTAMI', 'Devamupasm', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (457, 'vAtApi gaNa patiM', 'Hamsadhwani', 'DVITIYA', 'Patim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (458, 'vEdAraNyESvarAya', 'Hanumatodi', 'CHATURTHI', 'Vedaranyesvar', '01930000-0000-7000-8000-00000000005d'::uuid, NULL, false, NULL, 'NONE'),
    (459, 'vEda purISvaraM', 'Dhanyāsi', 'DVITIYA', 'Purisvar', '01930000-0000-7000-8000-00000000005c'::uuid, NULL, false, NULL, 'NONE'),
    (460, 'vEnkaTAcala patE', 'Karnātaka Kāpi', 'SAPTAMI', 'Pat', '01930000-0000-7000-8000-00000000005f'::uuid, NULL, false, NULL, 'NONE'),
    (461, 'vEnkaTESvara yAdava', 'Megharanjani', 'DVITIYA', 'Venkatesvara', '01930000-0000-7000-8000-00000000005f'::uuid, NULL, false, NULL, 'NONE'),
    (462, 'vINA bhEri', 'Abheri', 'DVITIYA', 'Vina', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (463, 'vINA pustaka', 'Vegavāhini', 'DVITIYA', 'Vina', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (464, 'vIra hanumatE', 'Karnātaka Kāpi', 'SAPTAMI', 'Hanumat', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (465, 'vIra vasanta', 'Veeravasantham', 'SAMBODHANA', 'Tyagaraja', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (466, 'vaMSavati', 'Vamshavathi', 'DVITIYA', 'Vamsavati', '01930000-0000-7000-8000-000000000021'::uuid, NULL, false, NULL, 'NONE'),
    (467, 'vadAnyESvaraM', 'Devagāndhāri', 'DVITIYA', 'Vadanyesvar', '01930000-0000-7000-8000-000000000058'::uuid, NULL, false, NULL, 'NONE'),
    (468, 'vallabhA nAyakasya', 'Begada', 'SHASHTHI', 'Nayak', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (469, 'vandE mInAkshi', 'SankarAbharaNaM', 'SAPTAMI', 'Vand', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (470, 'vara Siva bAlaM', 'SankarAbharaNaM', 'DVITIYA', 'Bal', '01930000-0000-7000-8000-000000000038'::uuid, NULL, false, NULL, 'NONE'),
    (471, 'vara lakshmIM bhaja', 'saurAshTraM', 'DVITIYA', 'Lakshmim', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE'),
    (472, 'varada rAja avAva', 'Gangātarangini', 'SAMBODHANA', 'Raja', '01930000-0000-7000-8000-00000000005b'::uuid, NULL, false, NULL, 'NONE'),
    (473, 'varada rAja pAhi', 'SankarAbharaNaM', 'SAMBODHANA', 'Raja', '01930000-0000-7000-8000-00000000005b'::uuid, NULL, false, 'Nottusvara on Varadaraja of Kanchi. Manali is the tune''s setting.', 'NONE'),
    (474, 'varada rAjamupAsmahE', 'Sāranga', 'SAMBODHANA', 'Rajamupasmah', '01930000-0000-7000-8000-00000000005b'::uuid, NULL, false, NULL, 'NONE'),
    (475, 'viNAyaka vighna', 'Vegavāhini', 'DVITIYA', 'Vinayaka', '01930000-0000-7000-8000-00000000000f'::uuid, NULL, false, NULL, 'NONE'),
    (476, 'viSAlAkshIM', 'Kāshirāmakriyā', 'DVITIYA', 'Visalakshim', '01930000-0000-7000-8000-000000000030'::uuid, NULL, false, NULL, 'NONE'),
    (477, 'viSvESvarO rakshatu', 'Kanadā', 'DVITIYA', 'Visvesvaro', '01930000-0000-7000-8000-000000000030'::uuid, NULL, false, NULL, 'NONE'),
    (478, 'viSva nAthEna', 'Sāmanta', 'TRITIYA', 'Nath', '01930000-0000-7000-8000-000000000030'::uuid, NULL, false, NULL, 'NONE'),
    (479, 'viSva nAthaM bhajEhaM', 'Sri', 'DVITIYA', 'Nath', '01930000-0000-7000-8000-000000000031'::uuid, NULL, false, 'PPNS''s caturdasha ragamalika on Visvanatha at Kuzhikkarai begins in Sri. The Natabharanam Visvanatham stays with the Tanjavur group.', 'NONE'),
    (480, 'viSva nAthaM bhajEhaM', 'Natābharanam', 'DVITIYA', 'Nath', '01930000-0000-7000-8000-000000000024'::uuid, NULL, false, NULL, 'NONE'),
    (481, 'vighnESvaraM', 'Malahari', 'DVITIYA', 'Vighnesvar', '01930000-0000-7000-8000-000000000001'::uuid, NULL, false, NULL, 'NONE')
)
UPDATE krithis k
SET vibhakti_case = cd.vibhakti::vibhakti_enum,
    vibhakti_stem = cd.stem,
    temple_id = cd.temple_id,
    deity_id = COALESCE(d.id, k.deity_id),
    is_manipravala = cd.manipravala,
    occasion_note = COALESCE(cd.occasion_note, k.occasion_note),
    yati_pattern = cd.yati_pattern::yati_pattern_enum,
    updated_at = clock_timestamp()
FROM comp_data cd
LEFT JOIN deities d ON d.name_normalized = cd.deity_key
WHERE k.title = cd.title
  AND k.composer_id = (
      SELECT id FROM composers WHERE lower(name) LIKE '%dikshitar%' ORDER BY name LIMIT 1
  )
  -- Disambiguate duplicate titles by primary raga
  AND (
      cd.title != 'viSva nAthaM bhajEhaM'
      OR (cd.row_id = 479 AND k.primary_raga_id = (SELECT id FROM ragas WHERE lower(name) = 'sri' LIMIT 1))
      OR (cd.row_id = 480 AND k.primary_raga_id = (SELECT id FROM ragas WHERE lower(name) LIKE '%nat%bharan%' LIMIT 1))
  );

-- Special update: Tyagaraja Yoga Vaibhavam carries gopuccha yati.
UPDATE krithis
SET yati_pattern = 'GOPUCCHA',
    occasion_note = 'Famous composition featuring Gopuccha and Srotovaha yati patterns.',
    updated_at = clock_timestamp()
WHERE title = 'tyAgarAja yOga'
  AND composer_id = (
      SELECT id FROM composers WHERE lower(name) LIKE '%dikshitar%' ORDER BY name LIMIT 1
  );
