-- TRACK-039: Audit Query 2 — Section Label Sequence Mismatch
-- Purpose: Detect Krithis where the section label ordering differs across lyric variants.
-- Example violation: Variant A has [Pallavi, Charanam] but Variant B has [Pallavi, Anupallavi, Charanam]
-- This indicates either a missing section in one variant or a misparse during import.

SET search_path TO public;

-- 2a. Compare section type sequences across lyric variants for each Krithi
-- Build an ordered string of section types per variant, then find disagreements
WITH variant_section_sequences AS (
    SELECT
        klv.krithi_id,
        klv.id AS lyric_variant_id,
        klv.language,
        klv.script,
        klv.is_primary,
        STRING_AGG(
            ks.section_type || ':' || ks.order_index,
            '→' ORDER BY ks.order_index
        ) AS section_sequence
    FROM krithi_lyric_variants klv
    JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
    JOIN krithi_sections ks ON ks.id = kls.section_id
    GROUP BY klv.krithi_id, klv.id, klv.language, klv.script, klv.is_primary
),
krithi_sequences AS (
    SELECT
        krithi_id,
        COUNT(DISTINCT section_sequence) AS distinct_sequences,
        ARRAY_AGG(DISTINCT section_sequence) AS all_sequences
    FROM variant_section_sequences
    GROUP BY krithi_id
    HAVING COUNT(DISTINCT section_sequence) > 1
)
SELECT
    k.id AS krithi_id,
    k.title,
    c.name AS composer,
    ks.distinct_sequences,
    vss.language,
    vss.script,
    vss.is_primary,
    vss.section_sequence
FROM krithi_sequences ks
JOIN krithis k ON k.id = ks.krithi_id
JOIN composers c ON c.id = k.composer_id
JOIN variant_section_sequences vss ON vss.krithi_id = ks.krithi_id
ORDER BY c.name, k.title, vss.language;

-- 2b. Summary: Count of label-sequence-mismatched Krithis by composer
WITH variant_section_sequences AS (
    SELECT
        klv.krithi_id,
        klv.id AS lyric_variant_id,
        STRING_AGG(
            ks.section_type || ':' || ks.order_index,
            '→' ORDER BY ks.order_index
        ) AS section_sequence
    FROM krithi_lyric_variants klv
    JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
    JOIN krithi_sections ks ON ks.id = kls.section_id
    GROUP BY klv.krithi_id, klv.id
),
mismatched AS (
    SELECT krithi_id
    FROM variant_section_sequences
    GROUP BY krithi_id
    HAVING COUNT(DISTINCT section_sequence) > 1
)
SELECT
    c.name AS composer,
    COUNT(DISTINCT m.krithi_id) AS mismatched_krithis,
    COUNT(DISTINCT k.id) AS total_krithis_with_variants,
    ROUND(100.0 * COUNT(DISTINCT m.krithi_id) / NULLIF(COUNT(DISTINCT k.id), 0), 1) AS mismatch_pct
FROM krithis k
JOIN composers c ON c.id = k.composer_id
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
LEFT JOIN mismatched m ON m.krithi_id = k.id
GROUP BY c.name
ORDER BY mismatched_krithis DESC;

-- 2c. Canonical section template vs actual variant sections (detailed)
-- Shows which sections are missing from which variants
WITH canonical_sections AS (
    SELECT
        ks.krithi_id,
        ks.id AS section_id,
        ks.section_type,
        ks.order_index,
        ks.label
    FROM krithi_sections ks
),
variant_coverage AS (
    SELECT
        klv.krithi_id,
        klv.id AS lyric_variant_id,
        klv.language,
        klv.script,
        cs.section_id,
        cs.section_type,
        cs.order_index,
        cs.label AS canonical_label,
        CASE WHEN kls.id IS NOT NULL THEN TRUE ELSE FALSE END AS has_content
    FROM krithi_lyric_variants klv
    CROSS JOIN canonical_sections cs
    LEFT JOIN krithi_lyric_sections kls
        ON kls.lyric_variant_id = klv.id AND kls.section_id = cs.section_id
    WHERE cs.krithi_id = klv.krithi_id
)
SELECT
    k.title,
    c.name AS composer,
    vc.language,
    vc.script,
    vc.section_type,
    vc.order_index,
    vc.canonical_label,
    vc.has_content
FROM variant_coverage vc
JOIN krithis k ON k.id = vc.krithi_id
JOIN composers c ON c.id = k.composer_id
WHERE vc.has_content = FALSE
ORDER BY c.name, k.title, vc.language, vc.order_index;
