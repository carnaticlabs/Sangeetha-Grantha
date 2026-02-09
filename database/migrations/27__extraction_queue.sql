-- TRACK-041: Extraction Queue (Kotlin ↔ Python Integration)
-- Purpose: Database-backed work queue for multi-format extraction tasks.
-- The Kotlin backend writes extraction requests (PENDING status) and the Python
-- extraction service polls, claims (PROCESSING), and completes (DONE/FAILED) them.
-- Uses SELECT ... FOR UPDATE SKIP LOCKED for exactly-once processing semantics.

SET search_path TO public;

-- Extraction task status enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'extraction_status') THEN
        CREATE TYPE extraction_status AS ENUM (
            'PENDING',      -- Queued by Kotlin, awaiting Python pickup
            'PROCESSING',   -- Claimed by Python worker, extraction in progress
            'DONE',         -- Extraction completed successfully
            'FAILED',       -- Extraction failed (may be retried)
            'CANCELLED'     -- Cancelled by admin
        );
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS extraction_queue (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Relationship to import pipeline (optional — extractions can be standalone)
    import_batch_id         UUID REFERENCES import_batch(id),
    import_task_run_id      UUID REFERENCES import_task_run(id),

    -- ─── Request (written by Kotlin backend) ───────────────────────────────
    source_url              TEXT NOT NULL,                   -- URL or file reference
    source_format           TEXT NOT NULL CHECK (source_format IN ('PDF', 'DOCX', 'IMAGE')),
    source_name             TEXT,                            -- e.g. 'guruguha.org'
    source_tier             INTEGER CHECK (source_tier BETWEEN 1 AND 5),
    request_payload         JSONB NOT NULL DEFAULT '{}',     -- extraction parameters (page_range, composer hint, etc.)
    page_range              TEXT,                            -- e.g. '42-43' for specific pages

    -- ─── Processing state ──────────────────────────────────────────────────
    status                  extraction_status NOT NULL DEFAULT 'PENDING',
    claimed_at              TIMESTAMPTZ,                     -- when a worker claimed this task
    claimed_by              TEXT,                            -- container hostname for debugging
    attempts                INTEGER NOT NULL DEFAULT 0,
    max_attempts            INTEGER NOT NULL DEFAULT 3,

    -- ─── Result (written by Python extraction service) ─────────────────────
    result_payload          JSONB,                           -- array of CanonicalExtractionDto
    result_count            INTEGER,                         -- number of Krithis extracted
    extraction_method       TEXT,                            -- PDF_PYMUPDF, PDF_OCR, DOCX_PYTHON
    extractor_version       TEXT,                            -- e.g. 'pdf-extractor:1.0.0'
    confidence              DECIMAL(5,4),                    -- overall extraction confidence
    duration_ms             INTEGER,                         -- processing duration

    -- ─── Error handling ────────────────────────────────────────────────────
    error_detail            JSONB,                           -- structured error info
    last_error_at           TIMESTAMPTZ,

    -- ─── Artifact tracking ─────────────────────────────────────────────────
    source_checksum         TEXT,                            -- SHA-256 of source document
    cached_artifact_path    TEXT,                            -- path in extraction_cache volume

    -- ─── Audit ─────────────────────────────────────────────────────────────
    created_at              TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

-- Index for Python worker polling: find PENDING tasks efficiently
CREATE INDEX IF NOT EXISTS idx_eq_status_pending
    ON extraction_queue(status, created_at)
    WHERE status = 'PENDING';

-- Index for Kotlin reading completed results
CREATE INDEX IF NOT EXISTS idx_eq_status_done
    ON extraction_queue(status, updated_at)
    WHERE status = 'DONE';

-- Index for batch-level queries
CREATE INDEX IF NOT EXISTS idx_eq_batch
    ON extraction_queue(import_batch_id)
    WHERE import_batch_id IS NOT NULL;

-- Index for failed task monitoring
CREATE INDEX IF NOT EXISTS idx_eq_status_failed
    ON extraction_queue(status, last_error_at)
    WHERE status = 'FAILED';

COMMENT ON TABLE extraction_queue IS 'Database-backed work queue: Kotlin writes PENDING tasks, Python claims and completes them. Uses FOR UPDATE SKIP LOCKED for concurrency.';
COMMENT ON COLUMN extraction_queue.request_payload IS 'JSON extraction parameters: {composerHint, pageRange, expectedKrithiCount, options}';
COMMENT ON COLUMN extraction_queue.result_payload IS 'JSON array of CanonicalExtractionDto objects, one per extracted Krithi';

-- migrate:down
-- DROP INDEX IF EXISTS idx_eq_status_failed;
-- DROP INDEX IF EXISTS idx_eq_batch;
-- DROP INDEX IF EXISTS idx_eq_status_done;
-- DROP INDEX IF EXISTS idx_eq_status_pending;
-- DROP TABLE IF EXISTS extraction_queue;
-- DROP TYPE IF EXISTS extraction_status;
