-- =============================================================================
-- Migration 39: Seed Mālāvashree raga
-- =============================================================================
-- Mālāvashree (Malavasri) is a janya of Melakarta #22 Kharaharapriyā.
-- Part of the dvitīya ghana pañcakam (second set of five ghana ragas):
--   Rītigowla, Nārāyanagowla, Bowli, Mālāvashree, Sāranganātta
--
-- Arohanam/Avarohanam from Sangita Sampradaya Pradarshini (SSP):
--   Arohanam:  S G2 G2 M1 P N2 D2 N2 S
--   Avarohanam: S N2 N2 D2 P M1 P N2 D2 M1 M1 G2 S
--
-- Emphasis on gandhara, madhyama, and nishadha notes.
-- Incorporates 18th century raga architectural attributes now largely lost.
-- =============================================================================

INSERT INTO ragas (id, name, name_normalized, parent_raga_id, arohanam, avarohanam, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Mālāvashree',
    'malavasri',
    (SELECT id FROM ragas WHERE name_normalized = 'kharaharapriya'),
    'S G2 G2 M1 P N2 D2 N2 S',
    'S N2 N2 D2 P M1 P N2 D2 M1 M1 G2 S',
    NOW(),
    NOW()
)
ON CONFLICT (name_normalized) DO UPDATE SET
    name = EXCLUDED.name,
    parent_raga_id = COALESCE(EXCLUDED.parent_raga_id, ragas.parent_raga_id),
    arohanam = COALESCE(EXCLUDED.arohanam, ragas.arohanam),
    avarohanam = COALESCE(EXCLUDED.avarohanam, ragas.avarohanam),
    updated_at = NOW();
