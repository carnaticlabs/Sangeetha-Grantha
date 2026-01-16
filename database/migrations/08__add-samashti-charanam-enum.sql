-- 08__update-krithi-sections-check-constraint.sql
-- Purpose: Allow 'SAMASHTI_CHARANAM' in krithi_sections.section_type check constraint.
-- Note: This table uses a TEXT column with a CHECK constraint (UPPERCASE), not the lowercase postgres enum.

-- migrate:up
ALTER TABLE krithi_sections DROP CONSTRAINT krithi_sections_section_type_check;

ALTER TABLE krithi_sections ADD CONSTRAINT krithi_sections_section_type_check CHECK (
    section_type IN (
        'PALLAVI',
        'ANUPALLAVI',
        'CHARANAM',
        'CHITTASWARAM',
        'SWARA_SAHITYA',
        'MADHYAMA_KALA',
        'OTHER',
        'SAMASHTI_CHARANAM'
    )
);

-- Also update the enum type just in case it is used elsewhere (like krithi_ragas)
ALTER TYPE raga_section_enum ADD VALUE IF NOT EXISTS 'samashti_charanam';
