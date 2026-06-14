-- Repeatable reference data (Flyway R__): idempotent ON CONFLICT upserts, re-applied on
-- checksum change. Ref: ADR-013 seed tiering (D15). The admin USER and its role assignment
-- are environment data, not reference data — they are provisioned by the BootstrapAdmin
-- entrypoint (argon2id password via PasswordHasher / TRACK-114), never seeded here. Only the
-- role DEFINITION (grp_sangita_admin) stays as reference data below.

-- Roles
INSERT INTO roles (code, name)
VALUES ('grp_sangita_admin', 'Sangita Admin')
ON CONFLICT (code) DO NOTHING;

-- Composers
INSERT INTO composers (id, name, name_normalized, birth_year, death_year, created_at, updated_at)
VALUES 
(gen_random_uuid(), 'Tyagaraja', 'tyagaraja', 1767, 1847, NOW(), NOW()),
(gen_random_uuid(), 'Muthuswami Dikshitar', 'muttuswami diksitar', 1775, 1835, NOW(), NOW()),
(gen_random_uuid(), 'Syama Sastri', 'syama sastri', 1762, 1827, NOW(), NOW())
ON CONFLICT (name_normalized) DO NOTHING;

-- Ragas: owned entirely by R__seed_04_raga_reference.sql (72 melakarta + janyas, with
-- parent_raga_id linkage). Not seeded here — the former "basic samples" (Sri, Hamsadhwani,
-- Mohanam) were redundant duplicates of those authoritative rows.

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

-- Import Source for unmatched PDF extractions (used by KrithiMatcherService)
INSERT INTO import_sources (name, description, source_tier, supported_formats)
VALUES ('PDF Extraction (Unmatched)', 'Auto-created for unmatched PDF extraction results requiring manual review', 3, '{PDF}')
ON CONFLICT DO NOTHING;
