package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.uuid.Uuid

/**
 * TRACK-040: Orchestrates Krithi data remediation workflows.
 *
 * Coordinates:
 * - MetadataCleanupService: strips boilerplate from lyric sections
 * - StructuralNormalizationService: aligns variants to canonical structure
 * - Variant-level deduplication: merges near-identical lyric variants
 * - Quality re-scoring: refreshes quality scores after remediation
 */
class RemediationService(
    private val dal: SangitaDal,
    private val metadataCleanup: MetadataCleanupService,
    private val structuralNormalization: StructuralNormalizationService,
    private val qualityScorer: IQualityScorer,
    private val normalizer: NameNormalizationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class RemediationReport(
        val cleanup: MetadataCleanupService.CleanupReport,
        val normalization: StructuralNormalizationService.NormalizationReport,
        val deduplication: VariantDeduplicationReport,
    )

    @Serializable
    data class VariantDeduplicationReport(
        val krithisScanned: Int,
        val duplicatePairsFound: Int,
        val variantsMerged: Int,
        val details: List<DuplicateVariantDetail>,
    )

    @Serializable
    data class DuplicateVariantDetail(
        val krithiId: String,
        val krithiTitle: String,
        val composer: String,
        val keepVariantId: String,
        val removeVariantId: String,
        val language: String,
        val similarity: Int,
    )

    /**
     * Preview all remediation actions without making changes.
     */
    suspend fun preview(composerFilter: String? = null): RemediationReport {
        logger.info("Starting remediation preview (composerFilter=$composerFilter)")

        val cleanupPreview = metadataCleanup.preview(composerFilter)
        val normPreview = structuralNormalization.analyze(composerFilter)
        val dedupPreview = findDuplicateVariants(composerFilter)

        return RemediationReport(
            cleanup = MetadataCleanupService.CleanupReport(
                sectionsScanned = cleanupPreview.totalSectionsScanned,
                sectionsCleaned = cleanupPreview.dirtyItems.size,
                variantsCleaned = cleanupPreview.dirtyItems.map { it.variantId }.distinct().size,
                patternsRemoved = cleanupPreview.dirtyItems
                    .flatMap { it.patterns }
                    .groupBy { it }
                    .mapValues { it.value.size },
            ),
            normalization = normPreview,
            deduplication = dedupPreview,
        )
    }

    /**
     * Execute full remediation pipeline.
     */
    suspend fun execute(
        composerFilter: String? = null,
        enableCleanup: Boolean = true,
        enableDeduplication: Boolean = true,
        actorUserId: Uuid? = null,
    ): RemediationReport {
        logger.info("Starting remediation execution (composerFilter=$composerFilter)")

        // Phase 1: Metadata Cleanup
        val cleanupReport = if (enableCleanup) {
            metadataCleanup.execute(composerFilter)
        } else {
            MetadataCleanupService.CleanupReport(0, 0, 0, emptyMap())
        }

        // Phase 2: Structural Normalization (analysis only — structural changes require manual review)
        val normReport = structuralNormalization.analyze(composerFilter)

        // Phase 3: Variant Deduplication
        val dedupReport = if (enableDeduplication) {
            executeDuplicateVariantMerge(composerFilter, actorUserId)
        } else {
            VariantDeduplicationReport(0, 0, 0, emptyList())
        }

        // Log audit
        dal.auditLogs.append(
            action = "REMEDIATION_EXECUTED",
            entityTable = "krithis",
            actorUserId = actorUserId,
            metadata = buildString {
                append("{")
                append("\"composerFilter\":\"${composerFilter ?: "all"}\",")
                append("\"sectionsCleaned\":${cleanupReport.sectionsCleaned},")
                append("\"variantsMerged\":${dedupReport.variantsMerged},")
                append("\"krithisWithGaps\":${normReport.krithisWithGaps}")
                append("}")
            },
        )

        return RemediationReport(cleanupReport, normReport, dedupReport)
    }

    /**
     * Find near-duplicate lyric variants within the same Krithi.
     * Two variants in the same language are duplicates if their section text is >90% similar.
     */
    suspend fun findDuplicateVariants(
        composerFilter: String? = null,
        limit: Int = 100,
    ): VariantDeduplicationReport = DatabaseFactory.dbQuery {
        val details = mutableListOf<DuplicateVariantDetail>()
        var krithisScanned = 0

        // Get Krithis with multiple variants in the same language
        val krithiQuery = KrithisTable
            .innerJoin(ComposersTable, { KrithisTable.composerId }, { ComposersTable.id })
            .selectAll()

        composerFilter?.let { filter ->
            krithiQuery.andWhere {
                ComposersTable.nameNormalized like "%${filter.lowercase()}%"
            }
        }

        for (krithiRow in krithiQuery) {
            if (details.size >= limit) break
            krithisScanned++

            val krithiId = krithiRow[KrithisTable.id].value
            val title = krithiRow[KrithisTable.title]
            val composer = krithiRow[ComposersTable.name]

            // Get all variants for this Krithi
            val variants = KrithiLyricVariantsTable
                .selectAll()
                .where { KrithiLyricVariantsTable.krithiId eq krithiId }
                .map { row ->
                    Triple(
                        row[KrithiLyricVariantsTable.id].value,
                        row[KrithiLyricVariantsTable.language].dbValue,
                        row[KrithiLyricVariantsTable.lyrics],
                    )
                }

            // Group by language and compare within each group
            val byLanguage = variants.groupBy { it.second }

            for ((language, langVariants) in byLanguage) {
                if (langVariants.size < 2) continue

                // Compare all pairs
                for (i in langVariants.indices) {
                    for (j in i + 1 until langVariants.size) {
                        val (idA, _, textA) = langVariants[i]
                        val (idB, _, textB) = langVariants[j]

                        val normalizedA = normalizeForComparison(textA)
                        val normalizedB = normalizeForComparison(textB)

                        if (normalizedA.isBlank() || normalizedB.isBlank()) continue

                        val similarity = levenshteinRatio(normalizedA, normalizedB)
                        if (similarity > 90) {
                            // Keep the longer/more complete variant
                            val (keepId, removeId) = if (textA.length >= textB.length) {
                                idA to idB
                            } else {
                                idB to idA
                            }

                            details.add(
                                DuplicateVariantDetail(
                                    krithiId = krithiId.toString(),
                                    krithiTitle = title,
                                    composer = composer,
                                    keepVariantId = keepId.toString(),
                                    removeVariantId = removeId.toString(),
                                    language = language,
                                    similarity = similarity,
                                )
                            )
                        }
                    }
                }
            }
        }

        VariantDeduplicationReport(
            krithisScanned = krithisScanned,
            duplicatePairsFound = details.size,
            variantsMerged = 0, // Preview only — no merging done
            details = details,
        )
    }

    /**
     * Execute deduplication: merge near-duplicate variants.
     */
    private suspend fun executeDuplicateVariantMerge(
        composerFilter: String?,
        actorUserId: Uuid?,
    ): VariantDeduplicationReport {
        val preview = findDuplicateVariants(composerFilter)
        var merged = 0

        for (detail in preview.details) {
            try {
                DatabaseFactory.dbQuery {
                    val removeId = java.util.UUID.fromString(detail.removeVariantId)

                    // Delete lyric sections of the duplicate variant
                    KrithiLyricSectionsTable.deleteWhere {
                        KrithiLyricSectionsTable.lyricVariantId eq removeId
                    }

                    // Delete the duplicate variant itself
                    KrithiLyricVariantsTable.deleteWhere {
                        KrithiLyricVariantsTable.id eq removeId
                    }
                }

                dal.auditLogs.append(
                    action = "MERGE_DUPLICATE_VARIANT",
                    entityTable = "krithi_lyric_variants",
                    entityId = kotlin.uuid.Uuid.parse(detail.keepVariantId),
                    actorUserId = actorUserId,
                    metadata = """{"removedVariantId":"${detail.removeVariantId}","similarity":${detail.similarity},"language":"${detail.language}"}""",
                )
                merged++
            } catch (e: Exception) {
                logger.warn("Failed to merge variant ${detail.removeVariantId}: ${e.message}")
            }
        }

        return preview.copy(variantsMerged = merged)
    }

    private fun normalizeForComparison(text: String): String =
        text.lowercase()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^\\p{L}\\s]"), "")
            .trim()

    private fun levenshteinRatio(s1: String, s2: String): Int {
        // For very long strings, compare a sample
        val a = if (s1.length > 500) s1.take(500) else s1
        val b = if (s2.length > 500) s2.take(500) else s2

        val rows = a.length + 1
        val cols = b.length + 1
        val distance = Array(rows) { IntArray(cols) }

        for (i in 0 until rows) distance[i][0] = i
        for (j in 0 until cols) distance[0][j] = j

        for (i in 1 until rows) {
            for (j in 1 until cols) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                distance[i][j] = minOf(
                    distance[i - 1][j] + 1,
                    distance[i][j - 1] + 1,
                    distance[i - 1][j - 1] + cost,
                )
            }
        }

        val maxLen = max(a.length, b.length)
        if (maxLen == 0) return 100
        val dist = distance[a.length][b.length]
        return ((1.0 - dist.toDouble() / maxLen) * 100).toInt()
    }
}
