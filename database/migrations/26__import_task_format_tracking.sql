-- TRACK-041: Import Task Format Tracking
-- Purpose: Track source format (HTML, PDF, DOCX) and page range per import task run.
-- Enables format-specific analytics and extraction method attribution.

SET search_path TO public;

ALTER TABLE import_task_run
    ADD COLUMN IF NOT EXISTS source_format TEXT DEFAULT 'HTML'
        CHECK (source_format IN ('HTML', 'PDF', 'DOCX', 'IMAGE', 'API', 'MANUAL'));

ALTER TABLE import_task_run
    ADD COLUMN IF NOT EXISTS page_range TEXT;

COMMENT ON COLUMN import_task_run.source_format IS 'Document format of the source: HTML, PDF, DOCX, IMAGE, API, or MANUAL';
COMMENT ON COLUMN import_task_run.page_range IS 'For PDF sources: page range extracted, e.g. 42-43';

-- migrate:down
-- ALTER TABLE import_task_run DROP COLUMN IF EXISTS page_range;
-- ALTER TABLE import_task_run DROP COLUMN IF EXISTS source_format;
