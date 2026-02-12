package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.DatabaseFactory
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/**
 * TRACK-039: Runs data quality audit queries against the database and returns structured results.
 *
 * Implements the three audit queries defined in database/audits/:
 * 1. Section count mismatch across lyric variants per Krithi
 * 2. Section label sequence mismatch across variants
 * 3. Orphaned lyric blobs (lyric variants without proper section mapping)
 */
class AuditRunnerService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class AuditReport(
        val sectionCountMismatch: SectionCountAudit,
        val labelSequenceMismatch: LabelSequenceAudit,
        val orphanedLyricBlobs: OrphanedBlobsAudit,
        val summary: AuditSummary,
    )

    @Serializable
    data class SectionCountAudit(
        val mismatches: List<SectionCountMismatch>,
        val composerSummary: List<ComposerAuditSummary>,
        val krithisWithoutSections: List<KrithiWithoutSections>,
    )

    @Serializable
    data class SectionCountMismatch(
        val krithiId: String,
        val title: String,
        val composer: String,
        val canonicalSectionCount: Int,
        val language: String,
        val script: String,
        val isPrimary: Boolean,
        val variantSectionCount: Int,
        val sectionDrift: Int,
    )

    @Serializable
    data class ComposerAuditSummary(
        val composer: String,
        val mismatchedKrithis: Int,
        val totalKrithis: Int,
        val mismatchPct: Double,
    )

    @Serializable
    data class KrithiWithoutSections(
        val krithiId: String,
        val title: String,
        val composer: String,
        val variantCount: Int,
    )

    @Serializable
    data class LabelSequenceAudit(
        val mismatches: List<LabelSequenceMismatch>,
        val composerSummary: List<ComposerAuditSummary>,
    )

    @Serializable
    data class LabelSequenceMismatch(
        val krithiId: String,
        val title: String,
        val composer: String,
        val distinctSequences: Int,
        val language: String,
        val script: String,
        val isPrimary: Boolean,
        val sectionSequence: String,
    )

    @Serializable
    data class OrphanedBlobsAudit(
        val orphanedVariants: List<OrphanedVariant>,
        val composerSummary: List<ComposerOrphanSummary>,
        val crossKrithiReferences: List<CrossKrithiReference>,
        val emptyShells: List<EmptyKrithiShell>,
    )

    @Serializable
    data class OrphanedVariant(
        val krithiId: String,
        val title: String,
        val composer: String,
        val language: String,
        val script: String,
        val isPrimary: Boolean,
        val lyricsLength: Int,
    )

    @Serializable
    data class ComposerOrphanSummary(
        val composer: String,
        val orphanedVariants: Int,
        val affectedKrithis: Int,
        val totalKrithis: Int,
        val orphanPct: Double,
    )

    @Serializable
    data class CrossKrithiReference(
        val lyricSectionId: String,
        val variantKrithiTitle: String,
        val sectionKrithiTitle: String,
    )

    @Serializable
    data class EmptyKrithiShell(
        val krithiId: String,
        val title: String,
        val composer: String,
        val workflowState: String,
    )

    @Serializable
    data class AuditSummary(
        val totalKrithis: Int,
        val sectionCountMismatchCount: Int,
        val labelSequenceMismatchCount: Int,
        val orphanedVariantCount: Int,
        val emptyShellCount: Int,
        val crossKrithiReferenceCount: Int,
    )

    /**
     * Run all three audit queries and return a comprehensive report.
     */
    suspend fun runFullAudit(): AuditReport {
        logger.info("Starting full data quality audit")

        val sectionCount = runSectionCountAudit()
        val labelSequence = runLabelSequenceAudit()
        val orphaned = runOrphanedBlobsAudit()

        val totalKrithis = DatabaseFactory.dbQuery {
            var count = 0
            exec("SELECT COUNT(*) FROM krithis") { rs ->
                if (rs.next()) count = rs.getInt(1)
            }
            count
        }

        val summary = AuditSummary(
            totalKrithis = totalKrithis,
            sectionCountMismatchCount = sectionCount.mismatches.map { it.krithiId }.distinct().size,
            labelSequenceMismatchCount = labelSequence.mismatches.map { it.krithiId }.distinct().size,
            orphanedVariantCount = orphaned.orphanedVariants.size,
            emptyShellCount = orphaned.emptyShells.size,
            crossKrithiReferenceCount = orphaned.crossKrithiReferences.size,
        )

        logger.info("Audit complete: $summary")
        return AuditReport(sectionCount, labelSequence, orphaned, summary)
    }

    /**
     * Audit 1: Section count mismatch — Krithis where variant section counts differ from canonical.
     */
    suspend fun runSectionCountAudit(): SectionCountAudit = DatabaseFactory.dbQuery {
        val mismatches = mutableListOf<SectionCountMismatch>()
        exec(SECTION_COUNT_MISMATCH_SQL) { rs ->
            while (rs.next()) {
                mismatches.add(
                    SectionCountMismatch(
                        krithiId = rs.getString("krithi_id"),
                        title = rs.getString("title"),
                        composer = rs.getString("composer"),
                        canonicalSectionCount = rs.getInt("canonical_section_count"),
                        language = rs.getString("language"),
                        script = rs.getString("script"),
                        isPrimary = rs.getBoolean("is_primary"),
                        variantSectionCount = rs.getInt("variant_section_count"),
                        sectionDrift = rs.getInt("section_drift"),
                    )
                )
            }
        }

        val composerSummary = mutableListOf<ComposerAuditSummary>()
        exec(SECTION_COUNT_COMPOSER_SUMMARY_SQL) { rs ->
            while (rs.next()) {
                composerSummary.add(
                    ComposerAuditSummary(
                        composer = rs.getString("composer"),
                        mismatchedKrithis = rs.getInt("mismatched_krithis"),
                        totalKrithis = rs.getInt("total_krithis"),
                        mismatchPct = rs.getDouble("mismatch_pct"),
                    )
                )
            }
        }

        val withoutSections = mutableListOf<KrithiWithoutSections>()
        exec(KRITHIS_WITHOUT_SECTIONS_SQL) { rs ->
            while (rs.next()) {
                withoutSections.add(
                    KrithiWithoutSections(
                        krithiId = rs.getString("krithi_id"),
                        title = rs.getString("title"),
                        composer = rs.getString("composer"),
                        variantCount = rs.getInt("variant_count"),
                    )
                )
            }
        }

        SectionCountAudit(mismatches, composerSummary, withoutSections)
    }

    /**
     * Audit 2: Section label sequence mismatch across lyric variants.
     */
    suspend fun runLabelSequenceAudit(): LabelSequenceAudit = DatabaseFactory.dbQuery {
        val mismatches = mutableListOf<LabelSequenceMismatch>()
        exec(LABEL_SEQUENCE_MISMATCH_SQL) { rs ->
            while (rs.next()) {
                mismatches.add(
                    LabelSequenceMismatch(
                        krithiId = rs.getString("krithi_id"),
                        title = rs.getString("title"),
                        composer = rs.getString("composer"),
                        distinctSequences = rs.getInt("distinct_sequences"),
                        language = rs.getString("language"),
                        script = rs.getString("script"),
                        isPrimary = rs.getBoolean("is_primary"),
                        sectionSequence = rs.getString("section_sequence"),
                    )
                )
            }
        }

        val composerSummary = mutableListOf<ComposerAuditSummary>()
        exec(LABEL_SEQUENCE_COMPOSER_SUMMARY_SQL) { rs ->
            while (rs.next()) {
                composerSummary.add(
                    ComposerAuditSummary(
                        composer = rs.getString("composer"),
                        mismatchedKrithis = rs.getInt("mismatched_krithis"),
                        totalKrithis = rs.getInt("total_krithis_with_variants"),
                        mismatchPct = rs.getDouble("mismatch_pct"),
                    )
                )
            }
        }

        LabelSequenceAudit(mismatches, composerSummary)
    }

    /**
     * Audit 3: Orphaned lyric blobs — variants with text but no section mappings.
     */
    suspend fun runOrphanedBlobsAudit(): OrphanedBlobsAudit = DatabaseFactory.dbQuery {
        val orphaned = mutableListOf<OrphanedVariant>()
        exec(ORPHANED_BLOBS_SQL) { rs ->
            while (rs.next()) {
                orphaned.add(
                    OrphanedVariant(
                        krithiId = rs.getString("krithi_id"),
                        title = rs.getString("title"),
                        composer = rs.getString("composer"),
                        language = rs.getString("language"),
                        script = rs.getString("script"),
                        isPrimary = rs.getBoolean("is_primary"),
                        lyricsLength = rs.getInt("lyrics_length"),
                    )
                )
            }
        }

        val composerSummary = mutableListOf<ComposerOrphanSummary>()
        exec(ORPHANED_COMPOSER_SUMMARY_SQL) { rs ->
            while (rs.next()) {
                composerSummary.add(
                    ComposerOrphanSummary(
                        composer = rs.getString("composer"),
                        orphanedVariants = rs.getInt("orphaned_variants"),
                        affectedKrithis = rs.getInt("affected_krithis"),
                        totalKrithis = rs.getInt("total_krithis"),
                        orphanPct = rs.getDouble("orphan_pct"),
                    )
                )
            }
        }

        val crossRefs = mutableListOf<CrossKrithiReference>()
        exec(CROSS_KRITHI_REFERENCES_SQL) { rs ->
            while (rs.next()) {
                crossRefs.add(
                    CrossKrithiReference(
                        lyricSectionId = rs.getString("lyric_section_id"),
                        variantKrithiTitle = rs.getString("variant_krithi_title"),
                        sectionKrithiTitle = rs.getString("section_krithi_title"),
                    )
                )
            }
        }

        val emptyShells = mutableListOf<EmptyKrithiShell>()
        exec(EMPTY_SHELLS_SQL) { rs ->
            while (rs.next()) {
                emptyShells.add(
                    EmptyKrithiShell(
                        krithiId = rs.getString("krithi_id"),
                        title = rs.getString("title"),
                        composer = rs.getString("composer"),
                        workflowState = rs.getString("workflow_state"),
                    )
                )
            }
        }

        OrphanedBlobsAudit(orphaned, composerSummary, crossRefs, emptyShells)
    }

    companion object {
        // --- Audit 1: Section Count Mismatch ---
        private val SECTION_COUNT_MISMATCH_SQL = """
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

        private val SECTION_COUNT_COMPOSER_SUMMARY_SQL = """
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

        private val KRITHIS_WITHOUT_SECTIONS_SQL = """
            SELECT k.id AS krithi_id, k.title, c.name AS composer, COUNT(klv.id) AS variant_count
            FROM krithis k JOIN composers c ON c.id = k.composer_id
            JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
            LEFT JOIN krithi_sections ks ON ks.krithi_id = k.id
            WHERE ks.id IS NULL
            GROUP BY k.id, k.title, c.name ORDER BY c.name, k.title
        """.trimIndent()

        // --- Audit 2: Label Sequence Mismatch ---
        private val LABEL_SEQUENCE_MISMATCH_SQL = """
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

        private val LABEL_SEQUENCE_COMPOSER_SUMMARY_SQL = """
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
        private val ORPHANED_BLOBS_SQL = """
            SELECT k.id AS krithi_id, k.title, c.name AS composer,
                   klv.language, klv.script, klv.is_primary, LENGTH(klv.lyrics) AS lyrics_length
            FROM krithi_lyric_variants klv
            JOIN krithis k ON k.id = klv.krithi_id
            JOIN composers c ON c.id = k.composer_id
            WHERE LENGTH(TRIM(klv.lyrics)) > 0
              AND NOT EXISTS (SELECT 1 FROM krithi_lyric_sections kls WHERE kls.lyric_variant_id = klv.id)
            ORDER BY c.name, k.title, klv.language
        """.trimIndent()

        private val ORPHANED_COMPOSER_SUMMARY_SQL = """
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

        private val CROSS_KRITHI_REFERENCES_SQL = """
            SELECT kls.id AS lyric_section_id,
                   k1.title AS variant_krithi_title, k2.title AS section_krithi_title
            FROM krithi_lyric_sections kls
            JOIN krithi_lyric_variants klv ON klv.id = kls.lyric_variant_id
            JOIN krithi_sections ks ON ks.id = kls.section_id
            LEFT JOIN krithis k1 ON k1.id = klv.krithi_id
            LEFT JOIN krithis k2 ON k2.id = ks.krithi_id
            WHERE klv.krithi_id != ks.krithi_id
        """.trimIndent()

        private val EMPTY_SHELLS_SQL = """
            SELECT k.id AS krithi_id, k.title, c.name AS composer, k.workflow_state
            FROM krithis k JOIN composers c ON c.id = k.composer_id
            LEFT JOIN krithi_sections ks ON ks.krithi_id = k.id
            LEFT JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
            WHERE ks.id IS NULL AND klv.id IS NULL
            ORDER BY c.name, k.title
        """.trimIndent()
    }
}
