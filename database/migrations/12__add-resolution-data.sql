-- 12__add-resolution-data.sql
-- Purpose: Add resolution_data column to imported_krithis for storing entity resolution candidates

-- migrate:up
ALTER TABLE imported_krithis ADD COLUMN IF NOT EXISTS resolution_data JSONB;

-- migrate:down
-- ALTER TABLE imported_krithis DROP COLUMN IF EXISTS resolution_data;
