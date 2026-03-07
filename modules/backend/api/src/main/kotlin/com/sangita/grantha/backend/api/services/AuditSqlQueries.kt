package com.sangita.grantha.backend.api.services

/**
 * SQL queries for data quality audits.
 * Extracted from AuditRunnerService companion object.
 */
object AuditSqlQueries {
    // --- Audit 1: Section Count Mismatch ---
    val SECTION_COUNT_MISMATCH_SQL = """
        WITH canonical_counts AS (
            SELECT ks.krithi_id, COUNT(ks.id) AS canonical_section_count
            FROM krithi_sections ks GROUP BY ks.krithi_id
        ),
        variant_counts AS (
            SELECT klv.krithi_id, klv.id AS lyric_variant_id, klv.language, klv.script, klv.is_primary,
                   COUNT(kls.id) AS variant_section_count
            FROM krithi_lyric_variants klv
            LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
            GROUP BY klv.krithi_id, klv.id, klv.language, klv.script, klv.is_primary
        )
        SELECT k.id AS krithi_id, k.title, c.name AS composer,
               cc.canonical_section_count, vc.language, vc.script, vc.is_primary,
               vc.variant_section_count,
               (cc.canonical_section_count - vc.variant_section_count) AS section_drift
        FROM krithis k
        JOIN composers c ON c.id = k.composer_id
        JOIN canonical_counts cc ON cc.krithi_id = k.id
        JOIN variant_counts vc ON vc.krithi_id = k.id
        WHERE cc.canonical_section_count != vc.variant_section_count
        ORDER BY c.name, k.title, vc.language
    """.trimIndent()

    val SECTION_COUNT_COMPOSER_SUMMARY_SQL = """
        WITH canonical_counts AS (
            SELECT ks.krithi_id, COUNT(ks.id) AS cnt FROM krithi_sections ks GROUP BY ks.krithi_id
        ),
        variant_counts AS (
            SELECT klv.krithi_id, klv.id AS vid, COUNT(kls.id) AS cnt
            FROM krithi_lyric_variants klv
            LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
            GROUP BY klv.krithi_id, klv.id
        ),
        mismatched AS (
            SELECT DISTINCT vc.krithi_id FROM variant_counts vc
            JOIN canonical_counts cc ON cc.krithi_id = vc.krithi_id WHERE cc.cnt != vc.cnt
        )
        SELECT c.name AS composer, COUNT(DISTINCT m.krithi_id) AS mismatched_krithis,
               COUNT(DISTINCT k.id) AS total_krithis,
               ROUND(100.0 * COUNT(DISTINCT m.krithi_id) / NULLIF(COUNT(DISTINCT k.id), 0), 1) AS mismatch_pct
        FROM krithis k JOIN composers c ON c.id = k.composer_id
        LEFT JOIN mismatched m ON m.krithi_id = k.id
        GROUP BY c.name ORDER BY mismatched_krithis DESC
    """.trimIndent()

    val KRITHIS_WITHOUT_SECTIONS_SQL = """
        SELECT k.id AS krithi_id, k.title, c.name AS composer, COUNT(klv.id) AS variant_count
        FROM krithis k JOIN composers c ON c.id = k.composer_id
        JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
        LEFT JOIN krithi_sections ks ON ks.krithi_id = k.id
        WHERE ks.id IS NULL
        GROUP BY k.id, k.title, c.name ORDER BY c.name, k.title
    """.trimIndent()

    // --- Audit 2: Label Sequence Mismatch ---
    val LABEL_SEQUENCE_MISMATCH_SQL = """
        WITH variant_section_sequences AS (
            SELECT klv.krithi_id, klv.id AS lyric_variant_id, klv.language, klv.script, klv.is_primary,
                   STRING_AGG(ks.section_type || ':' || ks.order_index, '→' ORDER BY ks.order_index) AS section_sequence
            FROM krithi_lyric_variants klv
            JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
            JOIN krithi_sections ks ON ks.id = kls.section_id
            GROUP BY klv.krithi_id, klv.id, klv.language, klv.script, klv.is_primary
        ),
        krithi_sequences AS (
            SELECT krithi_id, COUNT(DISTINCT section_sequence) AS distinct_sequences
            FROM variant_section_sequences GROUP BY krithi_id
            HAVING COUNT(DISTINCT section_sequence) > 1
        )
        SELECT k.id AS krithi_id, k.title, c.name AS composer, ks.distinct_sequences,
               vss.language, vss.script, vss.is_primary, vss.section_sequence
        FROM krithi_sequences ks
        JOIN krithis k ON k.id = ks.krithi_id
        JOIN composers c ON c.id = k.composer_id
        JOIN variant_section_sequences vss ON vss.krithi_id = ks.krithi_id
        ORDER BY c.name, k.title, vss.language
    """.trimIndent()

    val LABEL_SEQUENCE_COMPOSER_SUMMARY_SQL = """
        WITH variant_section_sequences AS (
            SELECT klv.krithi_id, klv.id AS lyric_variant_id,
                   STRING_AGG(ks.section_type || ':' || ks.order_index, '→' ORDER BY ks.order_index) AS section_sequence
            FROM krithi_lyric_variants klv
            JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
            JOIN krithi_sections ks ON ks.id = kls.section_id
            GROUP BY klv.krithi_id, klv.id
        ),
        mismatched AS (
            SELECT krithi_id FROM variant_section_sequences
            GROUP BY krithi_id HAVING COUNT(DISTINCT section_sequence) > 1
        )
        SELECT c.name AS composer, COUNT(DISTINCT m.krithi_id) AS mismatched_krithis,
               COUNT(DISTINCT k.id) AS total_krithis_with_variants,
               ROUND(100.0 * COUNT(DISTINCT m.krithi_id) / NULLIF(COUNT(DISTINCT k.id), 0), 1) AS mismatch_pct
        FROM krithis k JOIN composers c ON c.id = k.composer_id
        JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
        LEFT JOIN mismatched m ON m.krithi_id = k.id
        GROUP BY c.name ORDER BY mismatched_krithis DESC
    """.trimIndent()

    // --- Audit 3: Orphaned Lyric Blobs ---
    val ORPHANED_BLOBS_SQL = """
        SELECT k.id AS krithi_id, k.title, c.name AS composer,
               klv.language, klv.script, klv.is_primary, LENGTH(klv.lyrics) AS lyrics_length
        FROM krithi_lyric_variants klv
        JOIN krithis k ON k.id = klv.krithi_id
        JOIN composers c ON c.id = k.composer_id
        WHERE LENGTH(TRIM(klv.lyrics)) > 0
          AND NOT EXISTS (SELECT 1 FROM krithi_lyric_sections kls WHERE kls.lyric_variant_id = klv.id)
        ORDER BY c.name, k.title, klv.language
    """.trimIndent()

    val ORPHANED_COMPOSER_SUMMARY_SQL = """
        SELECT c.name AS composer,
               COUNT(DISTINCT klv.id) AS orphaned_variants,
               COUNT(DISTINCT klv.krithi_id) AS affected_krithis,
               COUNT(DISTINCT k_all.id) AS total_krithis,
               ROUND(100.0 * COUNT(DISTINCT klv.krithi_id) / NULLIF(COUNT(DISTINCT k_all.id), 0), 1) AS orphan_pct
        FROM composers c
        JOIN krithis k_all ON k_all.composer_id = c.id
        LEFT JOIN (
            SELECT klv_inner.* FROM krithi_lyric_variants klv_inner
            WHERE LENGTH(TRIM(klv_inner.lyrics)) > 0
              AND NOT EXISTS (SELECT 1 FROM krithi_lyric_sections kls WHERE kls.lyric_variant_id = klv_inner.id)
        ) klv ON klv.krithi_id = k_all.id
        GROUP BY c.name ORDER BY orphaned_variants DESC
    """.trimIndent()

    val CROSS_KRITHI_REFERENCES_SQL = """
        SELECT kls.id AS lyric_section_id,
               k1.title AS variant_krithi_title, k2.title AS section_krithi_title
        FROM krithi_lyric_sections kls
        JOIN krithi_lyric_variants klv ON klv.id = kls.lyric_variant_id
        JOIN krithi_sections ks ON ks.id = kls.section_id
        LEFT JOIN krithis k1 ON k1.id = klv.krithi_id
        LEFT JOIN krithis k2 ON k2.id = ks.krithi_id
        WHERE klv.krithi_id != ks.krithi_id
    """.trimIndent()

    val EMPTY_SHELLS_SQL = """
        SELECT k.id AS krithi_id, k.title, c.name AS composer, k.workflow_state
        FROM krithis k JOIN composers c ON c.id = k.composer_id
        LEFT JOIN krithi_sections ks ON ks.krithi_id = k.id
        LEFT JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
        WHERE ks.id IS NULL AND klv.id IS NULL
        ORDER BY c.name, k.title
    """.trimIndent()
}
