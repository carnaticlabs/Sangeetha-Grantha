-- 09__add-advanced-sections.sql
-- Purpose: Add support for advanced section types (Solkattu Swara, Anubandha, etc.)
-- Updates both the krithi_sections check constraint and the raga_section_enum type.

-- migrate:up
-- 1. Update Check Constraint on krithi_sections (Uppercase values)
ALTER TABLE krithi_sections DROP CONSTRAINT krithi_sections_section_type_check;
ALTER TABLE krithi_sections ADD CONSTRAINT krithi_sections_section_type_check CHECK (
    section_type IN (
        'PALLAVI', 'ANUPALLAVI', 'CHARANAM', 'SAMASHTI_CHARANAM',
        'CHITTASWARAM', 'SWARA_SAHITYA', 'MADHYAMA_KALA',
        'SOLKATTU_SWARA', 'ANUBANDHA', 'MUKTAYI_SWARA',
        'ETTUGADA_SWARA', 'ETTUGADA_SAHITYA', 'VILOMA_CHITTASWARAM',
        'OTHER'
    )
);

-- 2. Update Postgres Enum (used by krithi_ragas, Lowercase values)
-- Using IF NOT EXISTS to be safe, though standard Postgres < 12 doesn't support it for ADD VALUE
-- We assume Postgres 12+ or manual handling if it fails.
ALTER TYPE raga_section_enum ADD VALUE IF NOT EXISTS 'chittaswaram';
ALTER TYPE raga_section_enum ADD VALUE IF NOT EXISTS 'swara_sahitya';
ALTER TYPE raga_section_enum ADD VALUE IF NOT EXISTS 'madhyama_kala';
ALTER TYPE raga_section_enum ADD VALUE IF NOT EXISTS 'solkattu_swara';
ALTER TYPE raga_section_enum ADD VALUE IF NOT EXISTS 'anubandha';
ALTER TYPE raga_section_enum ADD VALUE IF NOT EXISTS 'muktayi_swara';
ALTER TYPE raga_section_enum ADD VALUE IF NOT EXISTS 'ettugada_swara';
ALTER TYPE raga_section_enum ADD VALUE IF NOT EXISTS 'ettugada_sahitya';
ALTER TYPE raga_section_enum ADD VALUE IF NOT EXISTS 'viloma_chittaswaram';
