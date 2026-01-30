-- TRACK-031: Seed known composer aliases (alias_normalized -> canonical composer)
-- Run after 01_reference_data.sql so composers exist.

INSERT INTO composer_aliases (alias_normalized, composer_id)
SELECT 'dikshitar', id FROM composers WHERE name_normalized = 'muthuswami dikshitar' LIMIT 1
ON CONFLICT (alias_normalized) DO NOTHING;

INSERT INTO composer_aliases (alias_normalized, composer_id)
SELECT 'thyagaraja', id FROM composers WHERE name_normalized = 'tyagaraja' LIMIT 1
ON CONFLICT (alias_normalized) DO NOTHING;

INSERT INTO composer_aliases (alias_normalized, composer_id)
SELECT 'saint tyagaraja', id FROM composers WHERE name_normalized = 'tyagaraja' LIMIT 1
ON CONFLICT (alias_normalized) DO NOTHING;

INSERT INTO composer_aliases (alias_normalized, composer_id)
SELECT 'muthuswamy dikshitar', id FROM composers WHERE name_normalized = 'muthuswami dikshitar' LIMIT 1
ON CONFLICT (alias_normalized) DO NOTHING;

INSERT INTO composer_aliases (alias_normalized, composer_id)
SELECT 'syama sastry', id FROM composers WHERE name_normalized = 'syama sastri' LIMIT 1
ON CONFLICT (alias_normalized) DO NOTHING;

INSERT INTO composer_aliases (alias_normalized, composer_id)
SELECT 'shyama sastri', id FROM composers WHERE name_normalized = 'syama sastri' LIMIT 1
ON CONFLICT (alias_normalized) DO NOTHING;

INSERT INTO composer_aliases (alias_normalized, composer_id)
SELECT 'shyama shastri', id FROM composers WHERE name_normalized = 'syama sastri' LIMIT 1
ON CONFLICT (alias_normalized) DO NOTHING;

INSERT INTO composer_aliases (alias_normalized, composer_id)
SELECT 'papanasam shivan', id FROM composers WHERE name_normalized = 'papanasam sivan' LIMIT 1
ON CONFLICT (alias_normalized) DO NOTHING;
