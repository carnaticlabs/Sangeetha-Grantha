-- Indexes to improve krithi search performance
CREATE INDEX IF NOT EXISTS idx_krithis_title_normalized ON krithis (title_normalized);
CREATE INDEX IF NOT EXISTS idx_krithis_incipit_normalized ON krithis (incipit_normalized);
CREATE INDEX IF NOT EXISTS idx_composers_name_normalized ON composers (name_normalized);
CREATE INDEX IF NOT EXISTS idx_ragas_name_normalized ON ragas (name_normalized);
CREATE INDEX IF NOT EXISTS idx_krithi_lyric_variants_lyrics ON krithi_lyric_variants (lyrics);
CREATE INDEX IF NOT EXISTS idx_krithi_ragas_krithi_id ON krithi_ragas (krithi_id);
CREATE INDEX IF NOT EXISTS idx_krithi_ragas_raga_id ON krithi_ragas (raga_id);
