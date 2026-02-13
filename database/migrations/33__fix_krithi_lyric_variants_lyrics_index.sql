-- 33__fix_krithi_lyric_variants_lyrics_index.sql
-- Purpose: replace unsafe btree index on unbounded lyrics text

-- migrate:up
SET search_path TO public;

-- Plain btree index on large text rows can fail inserts with:
-- "index row size ... exceeds btree version 4 maximum 2704"
DROP INDEX IF EXISTS idx_krithi_lyric_variants_lyrics;

-- Keep a bounded exact-match index if needed for dedupe/debug workflows.
CREATE INDEX IF NOT EXISTS idx_krithi_lyric_variants_lyrics_md5
  ON krithi_lyric_variants (md5(lyrics));

-- migrate:down
DROP INDEX IF EXISTS idx_krithi_lyric_variants_lyrics_md5;
CREATE INDEX IF NOT EXISTS idx_krithi_lyric_variants_lyrics
  ON krithi_lyric_variants (lyrics);
