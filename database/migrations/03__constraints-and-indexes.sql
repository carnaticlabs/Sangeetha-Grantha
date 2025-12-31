-- 03__constraints-and-indexes.sql
-- Purpose: Additional constraints and indexes for Sangita Grantha

-- migrate:up
SET search_path TO public;

-- Ensure audit_log jsonb defaults & index
ALTER TABLE audit_log
  ALTER COLUMN diff TYPE jsonb USING diff::jsonb,
  ALTER COLUMN metadata SET DEFAULT '{}'::jsonb;

CREATE INDEX IF NOT EXISTS idx_audit_entity_time
  ON audit_log(entity_table, entity_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_actor
  ON audit_log(actor_user_id, changed_at DESC);

-- Composers / Ragas / Talas / Deities / Temples search indexes
CREATE INDEX IF NOT EXISTS idx_composers_name_normalized
  ON composers (name_normalized);

CREATE INDEX IF NOT EXISTS idx_ragas_name_normalized
  ON ragas (name_normalized);

CREATE INDEX IF NOT EXISTS idx_talas_name_normalized
  ON talas (name_normalized);

CREATE INDEX IF NOT EXISTS idx_deities_name_normalized
  ON deities (name_normalized);

CREATE INDEX IF NOT EXISTS idx_temples_name_city
  ON temples (name_normalized, city, state, country);

-- Krithis: search by title, incipit, composer, raga, tala, deity, temple
CREATE INDEX IF NOT EXISTS idx_krithis_title_norm
  ON krithis (title_normalized);

CREATE INDEX IF NOT EXISTS idx_krithis_incipit_norm
  ON krithis (incipit_normalized);

CREATE INDEX IF NOT EXISTS idx_krithis_composer
  ON krithis (composer_id);

CREATE INDEX IF NOT EXISTS idx_krithis_primary_raga
  ON krithis (primary_raga_id);

CREATE INDEX IF NOT EXISTS idx_krithis_tala
  ON krithis (tala_id);

CREATE INDEX IF NOT EXISTS idx_krithis_deity
  ON krithis (deity_id);

CREATE INDEX IF NOT EXISTS idx_krithis_temple
  ON krithis (temple_id);

CREATE INDEX IF NOT EXISTS idx_krithis_workflow_state
  ON krithis (workflow_state);

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'krithis'
      AND column_name = 'musical_form'
  ) THEN
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_krithis_musical_form ON krithis (musical_form)';
  END IF;
END$$;

-- Ragamalika lookup
CREATE INDEX IF NOT EXISTS idx_krithi_ragas_krithi
  ON krithi_ragas (krithi_id, order_index);

CREATE INDEX IF NOT EXISTS idx_krithi_ragas_raga
  ON krithi_ragas (raga_id);

-- Lyrics search: trigram index for ILIKE '%term%' queries
CREATE INDEX IF NOT EXISTS idx_krithi_lyrics_trgm
  ON krithi_lyric_variants
  USING gin (lyrics gin_trgm_ops);

-- Optional narrower index for language/script filtered searches
CREATE INDEX IF NOT EXISTS idx_krithi_lyrics_lang_script
  ON krithi_lyric_variants (language, script, is_primary);

-- Notation lookup
DO $$
BEGIN
  IF to_regclass('public.krithi_notation_variants') IS NOT NULL THEN
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_krithi_notation_variants_krithi_type ON krithi_notation_variants (krithi_id, notation_type)';
  END IF;
END$$;

DO $$
BEGIN
  IF to_regclass('public.krithi_notation_rows') IS NOT NULL THEN
    EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS idx_krithi_notation_rows_variant_section_order ON krithi_notation_rows (notation_variant_id, section_id, order_index)';
  END IF;
END$$;

-- Notation search: trigram index for ILIKE '%term%' queries
DO $$
BEGIN
  IF to_regclass('public.krithi_notation_rows') IS NOT NULL
     AND EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_krithi_notation_rows_swara_trgm ON krithi_notation_rows USING gin (swara_text gin_trgm_ops)';
  END IF;
END$$;

-- Users / roles
CREATE INDEX IF NOT EXISTS idx_users_email
  ON users (email);

CREATE INDEX IF NOT EXISTS idx_role_assignments_user
  ON role_assignments (user_id);

-- migrate:down
-- DROP INDEX IF EXISTS idx_role_assignments_user;
-- DROP INDEX IF EXISTS idx_users_email;
-- DROP INDEX IF EXISTS idx_krithi_lyrics_lang_script;
-- DROP INDEX IF EXISTS idx_krithi_lyrics_trgm;
-- DROP INDEX IF EXISTS idx_krithi_notation_rows_swara_trgm;
-- DROP INDEX IF EXISTS idx_krithi_notation_rows_variant_section_order;
-- DROP INDEX IF EXISTS idx_krithi_notation_variants_krithi_type;
-- DROP INDEX IF EXISTS idx_krithi_ragas_raga;
-- DROP INDEX IF EXISTS idx_krithi_ragas_krithi;
-- DROP INDEX IF EXISTS idx_krithis_musical_form;
-- DROP INDEX IF EXISTS idx_krithis_temple;
-- DROP INDEX IF EXISTS idx_krithis_deity;
-- DROP INDEX IF EXISTS idx_krithis_tala;
-- DROP INDEX IF EXISTS idx_krithis_primary_raga;
-- DROP INDEX IF EXISTS idx_krithis_composer;
-- DROP INDEX IF EXISTS idx_krithis_incipit_norm;
-- DROP INDEX IF EXISTS idx_krithis_title_norm;
-- DROP INDEX IF EXISTS idx_temples_name_city;
-- DROP INDEX IF EXISTS idx_deities_name_normalized;
-- DROP INDEX IF EXISTS idx_talas_name_normalized;
-- DROP INDEX IF EXISTS idx_ragas_name_normalized;
-- DROP INDEX IF EXISTS idx_composers_name_normalized;
-- DROP INDEX IF EXISTS idx_audit_actor;
-- DROP INDEX IF EXISTS idx_audit_entity_time;
