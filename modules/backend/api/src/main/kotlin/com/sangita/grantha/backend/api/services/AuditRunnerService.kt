package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.DatabaseFactory
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
        exec(AuditSqlQueries.SECTION_COUNT_MISMATCH_SQL) { rs ->
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
        exec(AuditSqlQueries.SECTION_COUNT_COMPOSER_SUMMARY_SQL) { rs ->
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
        exec(AuditSqlQueries.KRITHIS_WITHOUT_SECTIONS_SQL) { rs ->
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
        exec(AuditSqlQueries.LABEL_SEQUENCE_MISMATCH_SQL) { rs ->
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
        exec(AuditSqlQueries.LABEL_SEQUENCE_COMPOSER_SUMMARY_SQL) { rs ->
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
        exec(AuditSqlQueries.ORPHANED_BLOBS_SQL) { rs ->
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
        exec(AuditSqlQueries.ORPHANED_COMPOSER_SUMMARY_SQL) { rs ->
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
        exec(AuditSqlQueries.CROSS_KRITHI_REFERENCES_SQL) { rs ->
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
        exec(AuditSqlQueries.EMPTY_SHELLS_SQL) { rs ->
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
}
