-- TRACK-039: Audit Query 3 — Orphaned Lyric Blobs
-- Purpose: Find lyric content that exists outside the section mapping structure.
-- This includes:
--   a) Lyric variants with text in the `lyrics` column but no entries in krithi_lyric_sections
--   b) Lyric sections pointing to non-existent canonical sections
--   c) Krithis with lyric data but no section structure at all

SET search_path TO public;

-- 3a. Lyric variants with content in `lyrics` column but zero lyric_sections
-- These represent monolithic lyric blobs that were never decomposed into sections.
SELECT
    k.id AS krithi_id,
    k.title,
    c.name AS composer,
    klv.id AS lyric_variant_id,
    klv.language,
    klv.script,
    klv.is_primary,
    LENGTH(klv.lyrics) AS lyrics_length,
    (SELECT COUNT(*) FROM krithi_lyric_sections kls WHERE kls.lyric_variant_id = klv.id) AS section_count
FROM krithi_lyric_variants klv
JOIN krithis k ON k.id = klv.krithi_id
JOIN composers c ON c.id = k.composer_id
WHERE LENGTH(TRIM(klv.lyrics)) > 0
  AND NOT EXISTS (
    SELECT 1 FROM krithi_lyric_sections kls WHERE kls.lyric_variant_id = klv.id
  )
ORDER BY c.name, k.title, klv.language;

-- 3b. Summary: Orphaned lyric blobs by composer
SELECT
    c.name AS composer,
    COUNT(DISTINCT klv.id) AS orphaned_variants,
    COUNT(DISTINCT klv.krithi_id) AS affected_krithis,
    COUNT(DISTINCT k_all.id) AS total_krithis,
    ROUND(100.0 * COUNT(DISTINCT klv.krithi_id) / NULLIF(COUNT(DISTINCT k_all.id), 0), 1) AS orphan_pct
FROM composers c
JOIN krithis k_all ON k_all.composer_id = c.id
LEFT JOIN (
    SELECT klv_inner.*
    FROM krithi_lyric_variants klv_inner
    WHERE LENGTH(TRIM(klv_inner.lyrics)) > 0
      AND NOT EXISTS (
        SELECT 1 FROM krithi_lyric_sections kls WHERE kls.lyric_variant_id = klv_inner.id
      )
) klv ON klv.krithi_id = k_all.id
GROUP BY c.name
ORDER BY orphaned_variants DESC;

-- 3c. Lyric sections referencing sections that don't belong to the same Krithi
-- (data integrity check — should be prevented by FKs but may exist in legacy data)
SELECT
    kls.id AS lyric_section_id,
    klv.krithi_id AS variant_krithi_id,
    ks.krithi_id AS section_krithi_id,
    k1.title AS variant_krithi_title,
    k2.title AS section_krithi_title
FROM krithi_lyric_sections kls
JOIN krithi_lyric_variants klv ON klv.id = kls.lyric_variant_id
JOIN krithi_sections ks ON ks.id = kls.section_id
LEFT JOIN krithis k1 ON k1.id = klv.krithi_id
LEFT JOIN krithis k2 ON k2.id = ks.krithi_id
WHERE klv.krithi_id != ks.krithi_id;

-- 3d. Krithis with no sections AND no lyric variants (completely empty shells)
SELECT
    k.id AS krithi_id,
    k.title,
    c.name AS composer,
    k.workflow_state,
    k.created_at
FROM krithis k
JOIN composers c ON c.id = k.composer_id
LEFT JOIN krithi_sections ks ON ks.krithi_id = k.id
LEFT JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
WHERE ks.id IS NULL AND klv.id IS NULL
ORDER BY c.name, k.title;
