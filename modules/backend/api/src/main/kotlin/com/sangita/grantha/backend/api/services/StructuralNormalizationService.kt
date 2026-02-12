package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

/**
 * TRACK-040: Aligns lyric variant section structures to the canonical template.
 *
 * For each Krithi, the canonical section structure (pallavi, anupallavi, charanam, etc.)
 * is defined in `krithi_sections`. Each lyric variant should have exactly one
 * `krithi_lyric_section` per canonical section. This service detects and reports
 * where variants deviate from the canonical structure.
 *
 * Normalization actions:
 * - Identify variants with missing sections (gaps)
 * - Identify variants with extra sections not in canonical
 * - Report section ordering inconsistencies
 */
class StructuralNormalizationService(private val dal: SangitaDal) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class NormalizationReport(
        val krithisAnalyzed: Int,
        val krithisWithGaps: Int,
        val krithisWithExtras: Int,
        val totalGaps: Int,
        val totalExtras: Int,
        val details: List<KrithiNormalizationDetail>,
    )

    @Serializable
    data class KrithiNormalizationDetail(
        val krithiId: String,
        val title: String,
        val composer: String,
        val canonicalSections: List<SectionInfo>,
        val variants: List<VariantAnalysis>,
    )

    @Serializable
    data class SectionInfo(
        val sectionId: String,
        val sectionType: String,
        val orderIndex: Int,
        val label: String?,
    )

    @Serializable
    data class VariantAnalysis(
        val variantId: String,
        val language: String,
        val script: String,
        val isPrimary: Boolean,
        val missingSections: List<String>,
        val extraSections: List<String>,
        val sectionCount: Int,
        val isAligned: Boolean,
    )

    /**
     * Analyze structural alignment for Krithis, optionally filtered by composer.
     * Returns a report of misaligned variants.
     */
    suspend fun analyze(
        composerFilter: String? = null,
        onlyMisaligned: Boolean = true,
        limit: Int = 200,
    ): NormalizationReport = DatabaseFactory.dbQuery {
        // Get all Krithis (optionally filtered by composer)
        val krithiQuery = KrithisTable
            .innerJoin(ComposersTable, { KrithisTable.composerId }, { ComposersTable.id })
            .selectAll()

        composerFilter?.let { filter ->
            krithiQuery.andWhere {
                ComposersTable.nameNormalized like "%${filter.lowercase()}%"
            }
        }

        krithiQuery.orderBy(ComposersTable.name to SortOrder.ASC, KrithisTable.title to SortOrder.ASC)

        val details = mutableListOf<KrithiNormalizationDetail>()
        var totalGaps = 0
        var totalExtras = 0
        var krithisWithGaps = 0
        var krithisWithExtras = 0
        var krithisAnalyzed = 0

        for (krithiRow in krithiQuery) {
            if (details.size >= limit) break
            krithisAnalyzed++

            val krithiId = krithiRow[KrithisTable.id].value
            val title = krithiRow[KrithisTable.title]
            val composer = krithiRow[ComposersTable.name]

            // Get canonical sections for this Krithi
            val canonicalSections = KrithiSectionsTable
                .selectAll()
                .where { KrithiSectionsTable.krithiId eq krithiId }
                .orderBy(KrithiSectionsTable.orderIndex)
                .map { row ->
                    SectionInfo(
                        sectionId = row[KrithiSectionsTable.id].value.toString(),
                        sectionType = row[KrithiSectionsTable.sectionType],
                        orderIndex = row[KrithiSectionsTable.orderIndex],
                        label = row[KrithiSectionsTable.label],
                    )
                }

            if (canonicalSections.isEmpty()) continue // Skip Krithis without canonical sections

            val canonicalSectionIds = canonicalSections.map { it.sectionId }.toSet()

            // Get all lyric variants for this Krithi
            val variants = KrithiLyricVariantsTable
                .selectAll()
                .where { KrithiLyricVariantsTable.krithiId eq krithiId }
                .map { row ->
                    val variantId = row[KrithiLyricVariantsTable.id].value

                    // Get lyric sections for this variant
                    val lyricSections = KrithiLyricSectionsTable
                        .selectAll()
                        .where { KrithiLyricSectionsTable.lyricVariantId eq variantId }
                        .map { ls -> ls[KrithiLyricSectionsTable.sectionId].toString() }
                        .toSet()

                    val missingSections = canonicalSections
                        .filter { it.sectionId !in lyricSections }
                        .map { "${it.sectionType} (order ${it.orderIndex})" }

                    val extraSections = lyricSections
                        .filter { it !in canonicalSectionIds }
                        .map { "Unknown section $it" }

                    VariantAnalysis(
                        variantId = variantId.toString(),
                        language = row[KrithiLyricVariantsTable.language].dbValue,
                        script = row[KrithiLyricVariantsTable.script].dbValue,
                        isPrimary = row[KrithiLyricVariantsTable.isPrimary],
                        missingSections = missingSections,
                        extraSections = extraSections,
                        sectionCount = lyricSections.size,
                        isAligned = missingSections.isEmpty() && extraSections.isEmpty(),
                    )
                }

            val hasGaps = variants.any { it.missingSections.isNotEmpty() }
            val hasExtras = variants.any { it.extraSections.isNotEmpty() }

            if (hasGaps) krithisWithGaps++
            if (hasExtras) krithisWithExtras++

            totalGaps += variants.sumOf { it.missingSections.size }
            totalExtras += variants.sumOf { it.extraSections.size }

            if (!onlyMisaligned || hasGaps || hasExtras) {
                details.add(
                    KrithiNormalizationDetail(
                        krithiId = krithiId.toString(),
                        title = title,
                        composer = composer,
                        canonicalSections = canonicalSections,
                        variants = variants,
                    )
                )
            }
        }

        NormalizationReport(
            krithisAnalyzed = krithisAnalyzed,
            krithisWithGaps = krithisWithGaps,
            krithisWithExtras = krithisWithExtras,
            totalGaps = totalGaps,
            totalExtras = totalExtras,
            details = details,
        )
    }
}
