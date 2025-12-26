-- 05__sections-tags-sampradaya-temple-names.sql
-- Purpose: Krithi sections, tags, sampradaya, and temple name aliases

-- migrate:up
SET search_path TO public;

-- 1️⃣ Sections
-- Canonical structural sections (language-agnostic) + per-variant text

CREATE TABLE IF NOT EXISTS krithi_sections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    krithi_id       UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,

    section_type    TEXT NOT NULL CHECK (
        section_type IN (
            'PALLAVI',
            'ANUPALLAVI',
            'CHARANAM',
            'CHITTASWARAM',
            'SWARA_SAHITYA',
            'MADHYAMA_KALA',
            'OTHER'
        )
    ),

    order_index     INT NOT NULL,
    label           TEXT,        -- e.g. "Charanam 2"
    notes           TEXT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),

    CONSTRAINT krithi_sections_krithi_order_uq UNIQUE (krithi_id, order_index)
);

CREATE TABLE IF NOT EXISTS krithi_lyric_sections (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lyric_variant_id    UUID NOT NULL REFERENCES krithi_lyric_variants(id) ON DELETE CASCADE,
    section_id          UUID NOT NULL REFERENCES krithi_sections(id) ON DELETE CASCADE,

    text                TEXT NOT NULL,
    normalized_text     TEXT,   -- optional, for search

    created_at          TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),

    CONSTRAINT krithi_lyric_sections_variant_section_uq
        UNIQUE (lyric_variant_id, section_id)
);

-- Optional index if you plan to search within sections by normalized_text
CREATE INDEX IF NOT EXISTS idx_krithi_lyric_sections_norm_text
    ON krithi_lyric_sections (normalized_text);


-- 2️⃣ Tags (Controlled Taxonomy Only)
-- Controlled vocabulary + Krithi ↔ Tag mapping

CREATE TABLE IF NOT EXISTS tags (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    category        TEXT NOT NULL CHECK (
        category IN (
            'BHAVA',
            'FESTIVAL',
            'PHILOSOPHY',
            'KSHETRA',
            'STOTRA_STYLE',
            'NAYIKA_BHAVA',
            'OTHER'
        )
    ),

    slug            TEXT NOT NULL UNIQUE,   -- e.g. 'bhakti', 'navaratri'
    display_name_en TEXT NOT NULL,
    description_en  TEXT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE TABLE IF NOT EXISTS krithi_tags (
    krithi_id   UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    tag_id      UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,

    source      TEXT NOT NULL DEFAULT 'manual',  -- 'manual' | 'import'
    confidence  INT CHECK (confidence BETWEEN 0 AND 100),

    PRIMARY KEY (krithi_id, tag_id)
);


-- 3️⃣ Sampradaya (Minimal v1)
-- Lineage/patantharam attribution

CREATE TABLE IF NOT EXISTS sampradayas (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL UNIQUE,     -- e.g. 'Walajapet'
    type            TEXT NOT NULL CHECK (
        type IN ('PATHANTARAM', 'BANI', 'SCHOOL')
    ),
    description     TEXT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

ALTER TABLE krithi_lyric_variants
    ADD COLUMN IF NOT EXISTS sampradaya_id UUID REFERENCES sampradayas(id),
    ADD COLUMN IF NOT EXISTS variant_label TEXT;   -- human-readable fallback


-- 4️⃣ Temple Names (Multilingual + Aliases)
-- We already have `temples` for canonical metadata.
-- This table adds multilingual names and aliases.

CREATE TABLE IF NOT EXISTS temple_names (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    temple_id       UUID NOT NULL REFERENCES temples(id) ON DELETE CASCADE,

    language_code   language_code_enum NOT NULL,   -- sa, ta, te, kn, ml, hi, en
    script_code     script_code_enum NOT NULL,     -- devanagari, tamil, telugu, etc.
    name            TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    is_primary      BOOLEAN NOT NULL DEFAULT false,

    source          TEXT NOT NULL DEFAULT 'manual',  -- 'manual' | 'import'

    created_at      TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),

    CONSTRAINT temple_names_unique_name_per_lang
        UNIQUE (temple_id, language_code, name)
);

CREATE INDEX IF NOT EXISTS idx_temple_names_normalized
    ON temple_names (normalized_name);

-- migrate:down
-- NOTE: Carefully ordered to satisfy FKs.

-- DROP INDEX IF EXISTS idx_temple_names_normalized;
-- DROP TABLE IF EXISTS temple_names;
--
-- ALTER TABLE krithi_lyric_variants
--     DROP COLUMN IF EXISTS sampradaya_id,
--     DROP COLUMN IF EXISTS variant_label;
--
-- DROP TABLE IF EXISTS sampradayas;
--
-- DROP TABLE IF EXISTS krithi_tags;
-- DROP TABLE IF EXISTS tags;
--
-- DROP INDEX IF EXISTS idx_krithi_lyric_sections_norm_text;
-- DROP TABLE IF EXISTS krithi_lyric_sections;
-- DROP TABLE IF EXISTS krithi_sections;
