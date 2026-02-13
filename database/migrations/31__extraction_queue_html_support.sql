-- TRACK-064 Phase 1: Add HTML support to extraction_queue source_format.
-- Purpose: allow Kotlin to enqueue HTML extraction tasks for Python worker.

SET search_path TO public;

ALTER TABLE extraction_queue
    DROP CONSTRAINT IF EXISTS extraction_queue_source_format_check;

ALTER TABLE extraction_queue
    ADD CONSTRAINT extraction_queue_source_format_check
    CHECK (source_format IN ('PDF', 'DOCX', 'IMAGE', 'HTML'));

COMMENT ON CONSTRAINT extraction_queue_source_format_check ON extraction_queue
    IS 'Allowed formats for Python extraction worker queue.';

-- migrate:down
-- ALTER TABLE extraction_queue DROP CONSTRAINT IF EXISTS extraction_queue_source_format_check;
-- ALTER TABLE extraction_queue
--     ADD CONSTRAINT extraction_queue_source_format_check
--     CHECK (source_format IN ('PDF', 'DOCX', 'IMAGE'));
