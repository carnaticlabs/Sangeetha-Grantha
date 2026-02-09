-- TRACK-041: Krithi Source Evidence Tracking
-- Purpose: Link each Krithi to all sources that contributed data, with per-source
--          extraction metadata and confidence scores. This table powers the
--          StructuralVotingEngine and the provenance display in the admin UI.

SET search_path TO public;

CREATE TABLE IF NOT EXISTS krithi_source_evidence (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    krithi_id           UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    import_source_id    UUID NOT NULL REFERENCES import_sources(id),

    -- Source artifact reference
    source_url          TEXT NOT NULL,
    source_format       TEXT NOT NULL CHECK (source_format IN ('HTML', 'PDF', 'DOCX', 'API', 'MANUAL')),
    extraction_method   TEXT NOT NULL CHECK (extraction_method IN (
        'PDF_PYMUPDF', 'PDF_OCR', 'HTML_JSOUP', 'HTML_JSOUP_GEMINI',
        'DOCX_PYTHON', 'MANUAL', 'TRANSLITERATION'
    )),

    -- PDF-specific
    page_range          TEXT,                           -- e.g. '42-43'

    -- Extraction metadata
    extracted_at        TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    checksum            TEXT,                           -- SHA-256 of source content
    confidence          DECIMAL(5,4),                   -- 0.0000 to 1.0000

    -- What this source contributed
    contributed_fields  TEXT[] NOT NULL DEFAULT '{}',   -- e.g. '{title,raga,tala,sections,lyrics_sa}'

    -- Raw extraction payload for audit/replay
    raw_extraction      JSONB,

    -- Audit
    created_at          TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

-- Indexes for common access patterns
CREATE INDEX IF NOT EXISTS idx_kse_krithi
    ON krithi_source_evidence(krithi_id);

CREATE INDEX IF NOT EXISTS idx_kse_source
    ON krithi_source_evidence(import_source_id);

CREATE INDEX IF NOT EXISTS idx_kse_krithi_source
    ON krithi_source_evidence(krithi_id, import_source_id);

COMMENT ON TABLE krithi_source_evidence IS 'Links each Krithi to all sources that contributed data, enabling multi-source validation and provenance tracking';
COMMENT ON COLUMN krithi_source_evidence.contributed_fields IS 'Array of field names this source contributed: title, raga, tala, sections, lyrics_sa, lyrics_en, deity, temple, notation';

-- migrate:down
-- DROP INDEX IF EXISTS idx_kse_krithi_source;
-- DROP INDEX IF EXISTS idx_kse_source;
-- DROP INDEX IF EXISTS idx_kse_krithi;
-- DROP TABLE IF EXISTS krithi_source_evidence;
