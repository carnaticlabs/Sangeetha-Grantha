-- TRACK-117 / ADR-014: Versioned Canon & Provenance Graph (N5)
-- Append-only revision history + normalised per-section provenance.
-- Pure DDL — no backfill: per decision D1 the corpus is re-imported fresh,
-- so revision #1 + provenance are written by the import path at creation.

SET search_path TO public;

-- ─── 1. Physical source artifact (the "source_document" node) ───────────────
-- Sits between the registry (import_sources) and the extraction run
-- (extraction_queue): one registry lists many artifacts; one artifact may be
-- extracted many times.
CREATE TABLE IF NOT EXISTS source_documents (
    id                UUID PRIMARY KEY DEFAULT uuidv7(),
    import_source_id  UUID NOT NULL REFERENCES import_sources(id),
    source_url        TEXT NOT NULL,
    source_format     TEXT NOT NULL CHECK (source_format IN ('HTML', 'PDF', 'DOCX', 'API', 'MANUAL')),
    page_range        TEXT,
    checksum          TEXT,                                            -- SHA-256 of artifact bytes
    retrieved_at      TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    CONSTRAINT source_documents_dedup_uq UNIQUE (import_source_id, source_url, checksum)
);

COMMENT ON TABLE source_documents IS
    'Provenance node: the physical artifact (page/PDF) between import_sources (registry) and extraction_queue (run). ADR-014.';

-- ─── 2. Link the existing extraction run to the document it consumed ────────
ALTER TABLE extraction_queue
    ADD COLUMN IF NOT EXISTS source_document_id UUID REFERENCES source_documents(id);

COMMENT ON COLUMN extraction_queue.source_document_id IS
    'The physical artifact this run extracted (ADR-014 provenance graph).';

-- ─── 3. Append-only revision envelope (one per accepted change-set) ─────────
CREATE TABLE IF NOT EXISTS krithi_revisions (
    id                 UUID PRIMARY KEY DEFAULT uuidv7(),
    krithi_id          UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    revision_no        INT  NOT NULL,                                  -- 1..N per krithi
    change_kind        TEXT NOT NULL CHECK (change_kind IN ('IMPORT', 'CURATOR_EDIT', 'MERGE', 'CORRECTION')),
    change_reason      TEXT,
    extraction_id      UUID REFERENCES extraction_queue(id),           -- NULL for manual edits
    created_by_user_id UUID REFERENCES users(id),
    valid_from         TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    recorded_at        TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    CONSTRAINT krithi_revisions_no_uq     UNIQUE (krithi_id, revision_no),
    -- Never anonymous: a revision carries an extraction or a user (or both).
    CONSTRAINT krithi_revisions_attrib_ck CHECK (extraction_id IS NOT NULL OR created_by_user_id IS NOT NULL)
);

COMMENT ON TABLE krithi_revisions IS
    'Append-only revision envelope; "current" is the latest revision. Source of truth for canon history (ADR-014).';

-- ─── 4. Append-only per-section content + per-section provenance ────────────
CREATE TABLE IF NOT EXISTS krithi_section_revisions (
    id                 UUID PRIMARY KEY DEFAULT uuidv7(),
    revision_id        UUID NOT NULL REFERENCES krithi_revisions(id) ON DELETE CASCADE,
    krithi_id          UUID NOT NULL REFERENCES krithis(id),           -- denormalized for fast as-of
    section_type       TEXT NOT NULL,                                  -- PALLAVI / ANUPALLAVI / CHARANAM / …
    order_index        INT  NOT NULL,
    label              TEXT,
    language           language_code_enum,
    script             script_code_enum,
    text               TEXT NOT NULL,
    normalized_text    TEXT,
    extraction_id      UUID REFERENCES extraction_queue(id),           -- per-section source attribution
    source_document_id UUID REFERENCES source_documents(id),
    valid_from         TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    -- Manual text may lack a document, but a document without a run is invalid.
    CONSTRAINT ksr_doc_requires_extraction_ck CHECK (source_document_id IS NULL OR extraction_id IS NOT NULL)
);

COMMENT ON TABLE krithi_section_revisions IS
    'Per-section content under a krithi_revisions envelope; each section independently attributable (ADR-014).';

-- ─── 5. Indexes for point-in-time and provenance joins ──────────────────────
CREATE INDEX IF NOT EXISTS krithi_revisions_asof_idx         ON krithi_revisions (krithi_id, valid_from DESC, revision_no DESC);
CREATE INDEX IF NOT EXISTS krithi_section_revisions_asof_idx ON krithi_section_revisions (krithi_id, valid_from DESC);
CREATE INDEX IF NOT EXISTS krithi_section_revisions_rev_idx  ON krithi_section_revisions (revision_id);
CREATE INDEX IF NOT EXISTS source_documents_registry_idx     ON source_documents (import_source_id, checksum);

-- ─── 6. Current-state projection (latest revision per krithi) ───────────────
CREATE OR REPLACE VIEW v_krithi_current_revision AS
    SELECT DISTINCT ON (krithi_id) *
    FROM krithi_revisions
    ORDER BY krithi_id, valid_from DESC, revision_no DESC;

-- ─── 7. As-of read: sections of the latest revision at or before :p_at ──────
CREATE OR REPLACE FUNCTION krithi_sections_asof(p_krithi UUID, p_at TIMESTAMPTZ)
RETURNS SETOF krithi_section_revisions
LANGUAGE sql STABLE AS $$
    SELECT sr.*
    FROM krithi_section_revisions sr
    WHERE sr.revision_id = (
        SELECT id FROM krithi_revisions
        WHERE krithi_id = p_krithi AND valid_from <= p_at
        ORDER BY valid_from DESC, revision_no DESC
        LIMIT 1
    );
$$;
