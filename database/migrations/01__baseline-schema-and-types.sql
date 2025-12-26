-- 01__baseline-schema-and-types.sql
-- Purpose: Extensions, shared enum types, and foundational tables for Sangita Grantha

-- migrate:up
SET search_path TO public;

-- Extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- trigram indexes for lyrics search

-- Enum types

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'workflow_state_enum') THEN
    CREATE TYPE workflow_state_enum AS ENUM ('draft','in_review','published','archived');
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'language_code_enum') THEN
    CREATE TYPE language_code_enum AS ENUM (
      'sa',  -- Sanskrit
      'ta',  -- Tamil
      'te',  -- Telugu
      'kn',  -- Kannada
      'ml',  -- Malayalam
      'hi',  -- Hindi
      'en'   -- English (for translations/notes)
    );
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'script_code_enum') THEN
    CREATE TYPE script_code_enum AS ENUM (
      'devanagari',
      'tamil',
      'telugu',
      'kannada',
      'malayalam',
      'latin'
    );
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'raga_section_enum') THEN
    CREATE TYPE raga_section_enum AS ENUM ('pallavi','anupallavi','charanam','other');
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'import_status_enum') THEN
    CREATE TYPE import_status_enum AS ENUM ('pending','in_review','mapped','rejected','discarded');
  END IF;
END$$;

-- Foundational tables

CREATE TABLE IF NOT EXISTS roles (
  code TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  -- capabilities as arbitrary JSON: permissions, scopes, etc.
  capabilities JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE IF NOT EXISTS audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_user_id UUID,
  actor_ip INET,
  action TEXT NOT NULL,           -- e.g. "CREATE", "UPDATE", "IMPORT_MAP"
  entity_table TEXT NOT NULL,     -- e.g. "krithis", "composers"
  entity_id UUID,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  diff JSONB,
  metadata JSONB DEFAULT '{}'::jsonb
);

-- migrate:down
-- Baseline objects are not dropped in down migration to avoid breaking dependencies.
