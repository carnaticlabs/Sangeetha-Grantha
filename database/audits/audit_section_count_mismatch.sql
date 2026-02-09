-- TRACK-039: Audit Query 1 — Section Count Mismatch
-- Purpose: Find Krithis where lyric variants have different section counts
--          than the canonical section structure in krithi_sections.
-- The musicological invariant: section structure is script-independent.
-- A Tamil lyric variant must have exactly the same sections as the Sanskrit variant.

SET search_path TO public;

-- 1a. Krithis where any lyric variant's section count differs from the canonical section count
WITH canonical_counts AS (
    SELECT
        ks.krithi_id,
        COUNT(ks.id) AS canonical_section_count
    FROM krithi_sections ks
    GROUP BY ks.krithi_id
),
variant_counts AS (
    SELECT
        klv.krithi_id,
        klv.id AS lyric_variant_id,
        klv.language,
        klv.script,
        klv.is_primary,
        COUNT(kls.id) AS variant_section_count
    FROM krithi_lyric_variants klv
    LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
    GROUP BY klv.krithi_id, klv.id, klv.language, klv.script, klv.is_primary
)
SELECT
    k.id AS krithi_id,
    k.title,
    c.name AS composer,
    cc.canonical_section_count,
    vc.language,
    vc.script,
    vc.is_primary,
    vc.variant_section_count,
    (cc.canonical_section_count - vc.variant_section_count) AS section_drift
FROM krithis k
JOIN composers c ON c.id = k.composer_id
JOIN canonical_counts cc ON cc.krithi_id = k.id
JOIN variant_counts vc ON vc.krithi_id = k.id
WHERE cc.canonical_section_count != vc.variant_section_count
ORDER BY c.name, k.title, vc.language;

-- 1b. Summary: Count of mismatched Krithis by composer
WITH canonical_counts AS (
    SELECT ks.krithi_id, COUNT(ks.id) AS cnt
    FROM krithi_sections ks
    GROUP BY ks.krithi_id
),
variant_counts AS (
    SELECT klv.krithi_id, klv.id AS vid, COUNT(kls.id) AS cnt
    FROM krithi_lyric_variants klv
    LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
    GROUP BY klv.krithi_id, klv.id
),
mismatched AS (
    SELECT DISTINCT vc.krithi_id
    FROM variant_counts vc
    JOIN canonical_counts cc ON cc.krithi_id = vc.krithi_id
    WHERE cc.cnt != vc.cnt
)
SELECT
    c.name AS composer,
    COUNT(DISTINCT m.krithi_id) AS mismatched_krithis,
    COUNT(DISTINCT k.id) AS total_krithis,
    ROUND(100.0 * COUNT(DISTINCT m.krithi_id) / NULLIF(COUNT(DISTINCT k.id), 0), 1) AS mismatch_pct
FROM krithis k
JOIN composers c ON c.id = k.composer_id
LEFT JOIN mismatched m ON m.krithi_id = k.id
GROUP BY c.name
ORDER BY mismatched_krithis DESC;

-- 1c. Krithis with lyric variants but NO canonical sections defined
SELECT
    k.id AS krithi_id,
    k.title,
    c.name AS composer,
    COUNT(klv.id) AS variant_count
FROM krithis k
JOIN composers c ON c.id = k.composer_id
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
LEFT JOIN krithi_sections ks ON ks.krithi_id = k.id
WHERE ks.id IS NULL
GROUP BY k.id, k.title, c.name
ORDER BY c.name, k.title;
