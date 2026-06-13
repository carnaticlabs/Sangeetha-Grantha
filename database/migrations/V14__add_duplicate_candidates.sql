-- 14__add_duplicate_candidates.sql
-- Purpose: Add column to store potential duplicates found during import process

-- migrate:up
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS duplicate_candidates JSONB;
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS import_batch_id UUID REFERENCES import_batch(id) ON DELETE SET NULL;

-- migrate:down
ALTER TABLE imported_krithis DROP COLUMN IF EXISTS import_batch_id;
ALTER TABLE imported_krithis DROP COLUMN IF EXISTS duplicate_candidates;
