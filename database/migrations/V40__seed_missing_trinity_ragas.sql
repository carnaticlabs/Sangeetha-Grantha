-- =============================================================================
-- Migration 40: Seed 9 Missing Base Ragas for Trinity Krithi Import
-- =============================================================================
-- These ragas are frequently referenced in Trinity compositions but were
-- missing from the database, causing identity resolution failures during
-- bulk import (TRACK-093).
--
-- Ragas seeded:
--   1. Kalyāni (= Mechakalyāni, melakarta #65)
--   2. Todi (janya of Hanumatodi, melakarta #8)
--   3. Jujāvanti (= Dwijāvanthi, janya of Harikāmbhōji #28)
--   4. Gaula (= Gowla, janya of Māyāmāḻavagouḻai #15)
--   5. Nāta (ghana raga, janya of Dhīraśankarābharaṇam #29)
--   6. Bauli (= Bowli, janya of Māyāmāḻavagouḻai #15)
--   7. Pūrvi (Hindustani/Carnatic fusion raga)
--   8. Gauri (= Gourimanohari, melakarta #23)
--   9. Brindāvana Sāranga (Carnatic variant)
--
-- References:
--   - Sangita Sampradaya Pradarshini (SSP)
--   - Wikipedia: Carnatic ragas
--   - Ragas in Carnatic Music by Prof. S. R. Janakiraman
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Kalyāni (Mechakalyāni #65)
-- -----------------------------------------------------------------------------
-- Kalyāni is the standard Carnatic name for melakarta #65 (Mechakalyāni).
-- It is one of the four "major" ragas (Ghana Raga Malika) along with
-- Sankarabharanam, Todi, and Kharaharapriya.
--
-- Arohanam:  S R2 G3 M2 P D2 N3 S
-- Avarohanam: S N3 D2 P M2 G3 R2 S
-- -----------------------------------------------------------------------------
INSERT INTO ragas (id, name, name_normalized, melakarta_number, arohanam, avarohanam, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Kalyāni',
    'kalyani',
    65,
    'S R2 G3 M2 P D2 N3 S',
    'S N3 D2 P M2 G3 R2 S',
    NOW(),
    NOW()
)
ON CONFLICT (name_normalized) DO UPDATE SET
    name = EXCLUDED.name,
    melakarta_number = COALESCE(EXCLUDED.melakarta_number, ragas.melakarta_number),
    arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam),
    avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 2. Todi (janya of Hanumatodi #8)
-- -----------------------------------------------------------------------------
-- Todi is a janya (derived) raga from melakarta #8 Hanumatodi.
-- It is one of the most ancient and popular ragas in Carnatic music.
--
-- Arohanam:  S R1 G2 M1 P D1 N2 S
-- Avarohanam: S N2 D1 P M1 G2 R1 S
-- -----------------------------------------------------------------------------
INSERT INTO ragas (id, name, name_normalized, parent_raga_id, arohanam, avarohanam, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Todi',
    'todi',
    (SELECT id FROM ragas WHERE melakarta_number = 8),
    'S R1 G2 M1 P D1 N2 S',
    'S N2 D1 P M1 G2 R1 S',
    NOW(),
    NOW()
)
ON CONFLICT (name_normalized) DO UPDATE SET
    name = EXCLUDED.name,
    parent_raga_id = COALESCE(EXCLUDED.parent_raga_id, ragas.parent_raga_id),
    arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam),
    avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 3. Jujāvanti (= Dwijāvanthi, janya of Harikāmbhōji #28)
-- -----------------------------------------------------------------------------
-- Jujāvanti (also Dwijāvanthi) is a janya raga of melakarta #28 Harikāmbhōji.
-- It is an ancient raga with North Indian origins (Jaijaivanti).
--
-- Arohanam:  S R2 M1 G3 M1 P D2 S
-- Avarohanam: S N2 D2 P M1 G3 M1 R2 G2 R2 S N2 D2 N2 S
-- -----------------------------------------------------------------------------
INSERT INTO ragas (id, name, name_normalized, parent_raga_id, arohanam, avarohanam, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Jujāvanti',
    'jujavanti',
    (SELECT id FROM ragas WHERE melakarta_number = 28),
    'S R2 M1 G3 M1 P D2 S',
    'S N2 D2 P M1 G3 M1 R2 G2 R2 S N2 D2 N2 S',
    NOW(),
    NOW()
)
ON CONFLICT (name_normalized) DO UPDATE SET
    name = EXCLUDED.name,
    parent_raga_id = COALESCE(EXCLUDED.parent_raga_id, ragas.parent_raga_id),
    arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam),
    avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 4. Gaula (= Gowla, janya of Māyāmāḻavagouḻai #15)
-- -----------------------------------------------------------------------------
-- Gaula (Gowla) is a janya raga of melakarta #15 Māyāmāḻavagouḻai.
-- It is one of the traditional five ghana ragas (Prathama Ghana Panchakam).
-- Audava-vakra-shadava rāgam (pentatonic ascending, 6-note descending).
--
-- Arohanam:  S R1 M1 P N3 S
-- Avarohanam: S N3 P M1 R1 G3 M1 R1 S
-- -----------------------------------------------------------------------------
INSERT INTO ragas (id, name, name_normalized, parent_raga_id, arohanam, avarohanam, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Gaula',
    'gaula',
    (SELECT id FROM ragas WHERE melakarta_number = 15),
    'S R1 M1 P N3 S',
    'S N3 P M1 R1 G3 M1 R1 S',
    NOW(),
    NOW()
)
ON CONFLICT (name_normalized) DO UPDATE SET
    name = EXCLUDED.name,
    parent_raga_id = COALESCE(EXCLUDED.parent_raga_id, ragas.parent_raga_id),
    arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam),
    avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 5. Nāta (ghana raga, janya of Dhīraśankarābharaṇam #29)
-- -----------------------------------------------------------------------------
-- Nāta is one of the traditional five ghana ragas (Prathama Ghana Panchakam).
-- It is a janya of melakarta #29 Dhīraśankarābharaṇam (Shankarabharanam).
-- Audava-shadava rāgam (5-note ascending, 6-note descending).
--
-- Arohanam:  S G3 M1 P N3 S
-- Avarohanam: S N3 D2 P M1 G3 R2 S
-- -----------------------------------------------------------------------------
INSERT INTO ragas (id, name, name_normalized, parent_raga_id, arohanam, avarohanam, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Nāta',
    'nata',
    (SELECT id FROM ragas WHERE melakarta_number = 29),
    'S G3 M1 P N3 S',
    'S N3 D2 P M1 G3 R2 S',
    NOW(),
    NOW()
)
ON CONFLICT (name_normalized) DO UPDATE SET
    name = EXCLUDED.name,
    parent_raga_id = COALESCE(EXCLUDED.parent_raga_id, ragas.parent_raga_id),
    arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam),
    avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 6. Bauli (= Bowli, janya of Māyāmāḻavagouḻai #15)
-- -----------------------------------------------------------------------------
-- Bauli (Bowli) is a janya raga of melakarta #15 Māyāmāḻavagouḻai.
-- It is part of the Dvitiya Ghana Panchakam (second set of five ghana ragas).
-- Audava-shadava rāgam (5-note ascending, 6-note descending).
--
-- Arohanam:  S R1 G3 P D1 S
-- Avarohanam: S N3 D1 P G3 R1 S
-- -----------------------------------------------------------------------------
INSERT INTO ragas (id, name, name_normalized, parent_raga_id, arohanam, avarohanam, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Bauli',
    'bauli',
    (SELECT id FROM ragas WHERE melakarta_number = 15),
    'S R1 G3 P D1 S',
    'S N3 D1 P G3 R1 S',
    NOW(),
    NOW()
)
ON CONFLICT (name_normalized) DO UPDATE SET
    name = EXCLUDED.name,
    parent_raga_id = COALESCE(EXCLUDED.parent_raga_id, ragas.parent_raga_id),
    arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam),
    avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 7. Pūrvi (Hindustani-origin raga adapted to Carnatic)
-- -----------------------------------------------------------------------------
-- Pūrvi is a Hindustani raga that has been adapted into Carnatic music.
-- In Carnatic system, it can be treated as a janya of melakarta #51 Kāmavardhani
-- or as a vivadi (controversial) raga using both M1 and M2.
--
-- Arohanam:  S R1 G3 M2 P D1 N3 S
-- Avarohanam: S N3 D1 P M2 G3 R1 S
-- -----------------------------------------------------------------------------
INSERT INTO ragas (id, name, name_normalized, parent_raga_id, arohanam, avarohanam, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Pūrvi',
    'purvi',
    (SELECT id FROM ragas WHERE melakarta_number = 51),
    'S R1 G3 M2 P D1 N3 S',
    'S N3 D1 P M2 G3 R1 S',
    NOW(),
    NOW()
)
ON CONFLICT (name_normalized) DO UPDATE SET
    name = EXCLUDED.name,
    parent_raga_id = COALESCE(EXCLUDED.parent_raga_id, ragas.parent_raga_id),
    arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam),
    avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 8. Gauri (= Gourimanohari, melakarta #23)
-- -----------------------------------------------------------------------------
-- Gauri is the common abbreviated name for melakarta #23 Gourimanohari.
-- It is a sampurna (complete 7-note) melakarta raga.
--
-- Arohanam:  S R2 G2 M1 P D2 N3 S
-- Avarohanam: S N3 D2 P M1 G2 R2 S
-- -----------------------------------------------------------------------------
INSERT INTO ragas (id, name, name_normalized, melakarta_number, arohanam, avarohanam, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Gauri',
    'gauri',
    23,
    'S R2 G2 M1 P D2 N3 S',
    'S N3 D2 P M1 G2 R2 S',
    NOW(),
    NOW()
)
ON CONFLICT (name_normalized) DO UPDATE SET
    name = EXCLUDED.name,
    melakarta_number = COALESCE(EXCLUDED.melakarta_number, ragas.melakarta_number),
    arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam),
    avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 9. Brindāvana Sāranga (Carnatic variant)
-- -----------------------------------------------------------------------------
-- Brindāvana Sāranga is a janya raga popular in both Carnatic and Hindustani
-- music. In Carnatic, it is considered a janya of Kharaharapriyā (#22).
-- This is the Carnatic variant (distinct from Hindustani Brindāvani).
-- Shadava-audava rāgam (6-note ascending, 5-note descending).
--
-- Arohanam:  S R2 G2 M1 P D2 S
-- Avarohanam: S D2 P M1 G2 S
-- -----------------------------------------------------------------------------
INSERT INTO ragas (id, name, name_normalized, parent_raga_id, arohanam, avarohanam, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Brindāvana Sāranga',
    'brindavanasaranga',
    (SELECT id FROM ragas WHERE melakarta_number = 22),
    'S R2 G2 M1 P D2 S',
    'S D2 P M1 G2 S',
    NOW(),
    NOW()
)
ON CONFLICT (name_normalized) DO UPDATE SET
    name = EXCLUDED.name,
    parent_raga_id = COALESCE(EXCLUDED.parent_raga_id, ragas.parent_raga_id),
    arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam),
    avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam),
    updated_at = NOW();
