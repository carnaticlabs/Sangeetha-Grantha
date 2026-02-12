-- Fix missing updated_at in entity_resolution_cache
ALTER TABLE entity_resolution_cache 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();