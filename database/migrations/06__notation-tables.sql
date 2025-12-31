-- 06__notation-tables.sql
-- Purpose: Notation variants and rows for Varnam and Swarajathi compositions

-- migrate:up
SET search_path TO public;

-- Notation Variants
-- Multiple notation variants per krithi (e.g., different pathantharams, sources)
CREATE TABLE IF NOT EXISTS krithi_notation_variants (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    krithi_id           UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    
    notation_type       TEXT NOT NULL CHECK (notation_type IN ('SWARA', 'JATHI')),
    tala_id             UUID REFERENCES talas(id) ON DELETE SET NULL,
    kalai                INT NOT NULL DEFAULT 1,              -- kalai (subdivision)
    eduppu_offset_beats INT,                                  -- eduppu (starting offset in beats)
    
    variant_label       TEXT,                                  -- e.g. "Walajapet", "Veedhi-bhaga"
    source_reference    TEXT,                                  -- which book/site/manuscript
    is_primary          BOOLEAN NOT NULL DEFAULT FALSE,
    
    created_by_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

-- Notation Rows
-- Individual rows of notation, organized by section and order
CREATE TABLE IF NOT EXISTS krithi_notation_rows (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notation_variant_id  UUID NOT NULL REFERENCES krithi_notation_variants(id) ON DELETE CASCADE,
    section_id           UUID NOT NULL REFERENCES krithi_sections(id) ON DELETE CASCADE,
    
    order_index         INT NOT NULL DEFAULT 0,               -- ordering within section
    swara_text           TEXT NOT NULL,                         -- swara notation (e.g., "S R G M P D N S")
    sahitya_text         TEXT,                                  -- sahitya (lyrics) for this row
    tala_markers         TEXT,                                  -- tala markers/beats (e.g., "| | | |")
    
    created_at          TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    
    CONSTRAINT krithi_notation_rows_variant_section_order_uq
        UNIQUE (notation_variant_id, section_id, order_index)
);

-- Indexes (already referenced conditionally in 03__constraints-and-indexes.sql)
CREATE INDEX IF NOT EXISTS idx_krithi_notation_variants_krithi_type
    ON krithi_notation_variants (krithi_id, notation_type);

CREATE INDEX IF NOT EXISTS idx_krithi_notation_rows_variant_section_order
    ON krithi_notation_rows (notation_variant_id, section_id, order_index);

-- Trigram index for swara text search (if extension exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        CREATE INDEX IF NOT EXISTS idx_krithi_notation_rows_swara_trgm
            ON krithi_notation_rows USING gin (swara_text gin_trgm_ops);
    END IF;
END$$;

-- migrate:down
-- DROP INDEX IF EXISTS idx_krithi_notation_rows_swara_trgm;
-- DROP INDEX IF EXISTS idx_krithi_notation_rows_variant_section_order;
-- DROP INDEX IF EXISTS idx_krithi_notation_variants_krithi_type;
-- DROP TABLE IF EXISTS krithi_notation_rows;
-- DROP TABLE IF EXISTS krithi_notation_variants;


