-- Admin User
INSERT INTO users (id, email, full_name, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'admin@sangitagrantha.org', 'System Admin', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Composers
INSERT INTO composers (id, name, name_normalized, birth_year, death_year, created_at, updated_at)
VALUES 
(gen_random_uuid(), 'Tyagaraja', 'tyagaraja', 1767, 1847, NOW(), NOW()),
(gen_random_uuid(), 'Muthuswami Dikshitar', 'muthuswami dikshitar', 1775, 1835, NOW(), NOW()),
(gen_random_uuid(), 'Syama Sastri', 'syama sastri', 1762, 1827, NOW(), NOW())
ON CONFLICT (name_normalized) DO NOTHING;

-- Ragas
INSERT INTO ragas (id, name, name_normalized, melakarta_number, arohanam, avarohanam, created_at, updated_at)
VALUES
(gen_random_uuid(), 'Sri', 'sri', 22, 'S R2 M1 P N2 S', 'S N2 P M1 R2 G2 R2 S', NOW(), NOW()),
(gen_random_uuid(), 'Hamsadhwani', 'hamsadhwani', 29, 'S R2 G3 P N3 S', 'S N3 P G3 R2 S', NOW(), NOW()),
(gen_random_uuid(), 'Mohanam', 'mohanam', 28, 'S R2 G3 P D2 S', 'S D2 P G3 R2 S', NOW(), NOW())
ON CONFLICT (name_normalized) DO NOTHING;

-- Talas
INSERT INTO talas (id, name, name_normalized, beat_count, anga_structure, created_at, updated_at)
VALUES
(gen_random_uuid(), 'Adi', 'adi', 8, 'I4 O O', NOW(), NOW()),
(gen_random_uuid(), 'Rupaka', 'rupaka', 6, 'O I4', NOW(), NOW())
ON CONFLICT (name_normalized) DO NOTHING;

-- Deities
INSERT INTO deities (id, name, name_normalized, created_at, updated_at)
VALUES
(gen_random_uuid(), 'Rama', 'rama', NOW(), NOW()),
(gen_random_uuid(), 'Ganesha', 'ganesha', NOW(), NOW()),
(gen_random_uuid(), 'Krishna', 'krishna', NOW(), NOW())
ON CONFLICT (name_normalized) DO NOTHING;
