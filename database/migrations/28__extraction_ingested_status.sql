-- TRACK-041: Add INGESTED status to extraction_status enum
-- Purpose: Track extraction queue items that have been processed by the Kotlin
--          ExtractionResultProcessor (source evidence records created, voting run).
-- After Python marks an item as DONE, Kotlin processes it and marks it INGESTED.

SET search_path TO public;

ALTER TYPE extraction_status ADD VALUE IF NOT EXISTS 'INGESTED' AFTER 'DONE';

COMMENT ON TYPE extraction_status IS 'PENDING→PROCESSING→DONE→INGESTED (or FAILED/CANCELLED). INGESTED = Kotlin has processed the results.';

-- migrate:down
-- PostgreSQL does not support removing enum values; this is a one-way migration.
