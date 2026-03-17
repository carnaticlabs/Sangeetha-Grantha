-- migrate:up
-- Prevent duplicate import_sources rows created by concurrent findOrCreateSource calls.
-- This was the root cause of duplicate imported_krithis: two ScrapeWorker tasks racing to
-- create the "BulkImportCSV" source would each get a different source_id, bypassing the
-- existing unique constraint on (import_source_id, source_key).

-- First deduplicate any existing rows (keep the oldest source per name)
DELETE FROM import_sources
WHERE id NOT IN (
    SELECT DISTINCT ON (name) id
    FROM import_sources
    ORDER BY name, created_at ASC
);

ALTER TABLE import_sources
    ADD CONSTRAINT ux_import_sources_name UNIQUE (name);

-- migrate:down
-- ALTER TABLE import_sources DROP CONSTRAINT IF EXISTS ux_import_sources_name;
