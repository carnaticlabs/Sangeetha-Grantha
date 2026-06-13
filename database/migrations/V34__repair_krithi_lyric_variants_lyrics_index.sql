-- 34__repair_krithi_lyric_variants_lyrics_index.sql
-- Purpose: enforce safe lyrics indexing shape for already-migrated databases

SET search_path TO public;

DROP INDEX IF EXISTS idx_krithi_lyric_variants_lyrics;
CREATE INDEX IF NOT EXISTS idx_krithi_lyric_variants_lyrics_md5
  ON krithi_lyric_variants (md5(lyrics));
