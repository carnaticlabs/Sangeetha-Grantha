-- TRACK-041: Source Registry — Authority Hierarchy Population
-- Purpose: Insert import_sources records for all target sources with tier rankings,
--          supported formats, and composer affinity weights.
-- Run after migration 23 (source_authority_enhancement).

SET search_path TO public;

-- ─── Tier 1: Scholarly/Published Editions ──────────────────────────────────

INSERT INTO import_sources (name, base_url, description, source_tier, supported_formats, composer_affinity)
VALUES (
    'guruguha.org',
    'https://guruguha.org',
    'Dr. P.P. Narayanaswami scholarly compilation of 484 Dikshitar Krithis. Two PDF editions: mdskt.pdf (Sanskrit/Devanagari) and mdeng.pdf (English transliteration). Very high authority for section structure, deity, and Kshetra metadata.',
    1,
    '{PDF}',
    '{"muthuswami_dikshitar": 1.0}'::JSONB
)
ON CONFLICT DO NOTHING;

-- ─── Tier 2: Official Institutional Archives ──────────────────────────────

INSERT INTO import_sources (name, base_url, description, source_tier, supported_formats, composer_affinity)
VALUES (
    'swathithirunalfestival.org',
    'https://swathithirunalfestival.org',
    'Official Swathi Thirunal Sangeetha Sabha (Charlotte, NC). ~400 compositions with Raga (Arohanam/Avarohanam), Tala, Sanskrit/Malayalam lyrics, and meanings. Composition index at /swathi-thirunal/compositions.',
    2,
    '{HTML}',
    '{"swathi_thirunal": 1.0}'::JSONB
)
ON CONFLICT DO NOTHING;

-- ─── Tier 3: Practitioner-Curated Archives ────────────────────────────────

INSERT INTO import_sources (name, base_url, description, source_tier, supported_formats, composer_affinity)
VALUES (
    'shivkumar.org',
    'https://www.shivkumar.org',
    'Curated by trained violinist. ~300 compositions with Swara notation in HTML, Word, and PDF formats. Uniquely valuable for notation data (krithi_notation_variants). Multi-composer coverage.',
    3,
    '{HTML,PDF,DOCX}',
    '{"tyagaraja": 0.6, "muthuswami_dikshitar": 0.3, "syama_sastri": 0.1}'::JSONB
)
ON CONFLICT DO NOTHING;

-- ─── Tier 4: Community Databases ──────────────────────────────────────────

INSERT INTO import_sources (name, base_url, description, source_tier, supported_formats, composer_affinity)
VALUES (
    'karnatik.com',
    'https://www.karnatik.com',
    'Community-maintained database with broad coverage. Composers index at /composers.shtml. Good for cross-referencing entity resolution and metadata gap-filling. Variable quality for lyrics.',
    4,
    '{HTML}',
    '{"tyagaraja": 0.4, "muthuswami_dikshitar": 0.3, "syama_sastri": 0.2, "swathi_thirunal": 0.1}'::JSONB
)
ON CONFLICT DO NOTHING;

-- ─── Tier 5: Individual Blogs (update existing records) ───────────────────
-- Update any existing blogspot sources to Tier 5 with appropriate metadata

UPDATE import_sources
SET source_tier = 5,
    supported_formats = '{HTML}',
    composer_affinity = '{}',
    updated_at = timezone('UTC', now())
WHERE name LIKE '%blogspot%'
   OR base_url LIKE '%blogspot%';

-- Also set default tier for any sources that still have the default
-- (ensures no source is accidentally left at tier 5 without explicit decision)
