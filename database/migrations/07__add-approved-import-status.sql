-- 07__add-approved-import-status.sql
-- Purpose: Add 'approved' status to import_status_enum for imports ready to be created as krithis

-- migrate:up
SET search_path TO public;

-- Add 'approved' to the import_status_enum
DO $$
BEGIN
  -- Check if 'approved' already exists
  IF NOT EXISTS (
    SELECT 1 FROM pg_enum 
    WHERE enumlabel = 'approved' 
    AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'import_status_enum')
  ) THEN
    ALTER TYPE import_status_enum ADD VALUE 'approved';
  END IF;
END$$;

-- migrate:down
-- Note: PostgreSQL does not support removing enum values directly
-- This would require recreating the enum type, which is complex
-- For now, we'll leave the enum value in place

