-- 19__fix_imported_krithis_schema.sql
-- Purpose: Add missing import_batch_id column to imported_krithis table

-- migrate:up
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS import_batch_id UUID;

-- migrate:down
-- ALTER TABLE imported_krithis DROP COLUMN IF EXISTS import_batch_id;
