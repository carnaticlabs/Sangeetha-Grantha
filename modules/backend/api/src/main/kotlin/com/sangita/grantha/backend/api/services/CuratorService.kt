package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.dal.tables.ImportedKrithisTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

@Serializable
data class CuratorStats(
    val totalPending: Long,
    val totalApproved: Long,
    val totalRejected: Long,
    val totalKrithis: Long,
    val sectionIssuesCount: Long,
)

@Serializable
data class SectionIssue(
    val krithiId: String,
    val title: String,
    val language: String,
    val expectedSections: Long,
    val actualSections: Long,
    val issueType: String,
)

@Serializable
data class SectionIssuesPage(
    val items: List<SectionIssue>,
    val total: Long,
    val page: Int,
    val size: Int,
)

class CuratorService(private val dal: SangitaDal) {

    suspend fun getStats(): CuratorStats = DatabaseFactory.dbQuery {
        val pending = ImportedKrithisTable
            .selectAll()
            .andWhere { ImportedKrithisTable.importStatus eq ImportStatus.PENDING }
            .count()

        val approved = ImportedKrithisTable
            .selectAll()
            .andWhere { ImportedKrithisTable.importStatus eq ImportStatus.APPROVED }
            .count()

        val rejected = ImportedKrithisTable
            .selectAll()
            .andWhere { ImportedKrithisTable.importStatus eq ImportStatus.REJECTED }
            .count()

        val totalKrithis = KrithisTable
            .selectAll()
            .count()

        // Count section issues inline
        val sectionCountCol = KrithiSectionsTable.id.count()
        val krithiSectionCounts = KrithiSectionsTable
            .select(KrithiSectionsTable.krithiId, sectionCountCol)
            .groupBy(KrithiSectionsTable.krithiId)
            .associate { it[KrithiSectionsTable.krithiId] to it[sectionCountCol] }

        val lyricSectionCountCol = KrithiLyricSectionsTable.id.count()
        val variantSectionCounts = KrithiLyricSectionsTable
            .innerJoin(KrithiLyricVariantsTable, { KrithiLyricSectionsTable.lyricVariantId }, { KrithiLyricVariantsTable.id })
            .select(KrithiLyricVariantsTable.krithiId, KrithiLyricVariantsTable.language, lyricSectionCountCol)
            .groupBy(KrithiLyricVariantsTable.krithiId, KrithiLyricVariantsTable.language)
            .map { it[KrithiLyricVariantsTable.krithiId] to it[lyricSectionCountCol] }

        var sectionIssuesCount = 0L
        for ((krithiId, actualCount) in variantSectionCounts) {
            val expectedCount = krithiSectionCounts[krithiId] ?: 0L
            if (actualCount != expectedCount) sectionIssuesCount++
        }

        CuratorStats(
            totalPending = pending,
            totalApproved = approved,
            totalRejected = rejected,
            totalKrithis = totalKrithis,
            sectionIssuesCount = sectionIssuesCount,
        )
    }

    suspend fun getSectionIssues(page: Int, size: Int): SectionIssuesPage = DatabaseFactory.dbQuery {
        val sectionCountCol = KrithiSectionsTable.id.count()
        val krithiSectionCounts = KrithiSectionsTable
            .select(KrithiSectionsTable.krithiId, sectionCountCol)
            .groupBy(KrithiSectionsTable.krithiId)
            .associate { it[KrithiSectionsTable.krithiId] to it[sectionCountCol] }

        val lyricSectionCountCol = KrithiLyricSectionsTable.id.count()
        val variantSectionCounts = KrithiLyricSectionsTable
            .innerJoin(KrithiLyricVariantsTable, { KrithiLyricSectionsTable.lyricVariantId }, { KrithiLyricVariantsTable.id })
            .select(KrithiLyricVariantsTable.krithiId, KrithiLyricVariantsTable.language, lyricSectionCountCol)
            .groupBy(KrithiLyricVariantsTable.krithiId, KrithiLyricVariantsTable.language)
            .map {
                Triple(
                    it[KrithiLyricVariantsTable.krithiId],
                    it[KrithiLyricVariantsTable.language].dbValue,
                    it[lyricSectionCountCol]
                )
            }

        val issues = mutableListOf<SectionIssue>()
        val krithiTitles = mutableMapOf<java.util.UUID, String>()

        for ((krithiId, language, actualCount) in variantSectionCounts) {
            val expectedCount = krithiSectionCounts[krithiId] ?: 0L
            if (actualCount != expectedCount) {
                val title = krithiTitles.getOrPut(krithiId) {
                    KrithisTable
                        .selectAll()
                        .andWhere { KrithisTable.id eq krithiId }
                        .singleOrNull()
                        ?.get(KrithisTable.title) ?: "Unknown"
                }

                val issueType = when {
                    actualCount == 0L -> "missing sections"
                    actualCount < expectedCount -> "missing sections"
                    actualCount > expectedCount -> "extra sections (dual-format)"
                    else -> "unknown"
                }

                issues.add(
                    SectionIssue(
                        krithiId = krithiId.toString(),
                        title = title,
                        language = language,
                        expectedSections = expectedCount,
                        actualSections = actualCount,
                        issueType = issueType,
                    )
                )
            }
        }

        // Find variants with zero sections
        val variantsWithSections = variantSectionCounts.map { (krithiId, lang, _) -> krithiId to lang }.toSet()
        val allVariants = KrithiLyricVariantsTable
            .select(KrithiLyricVariantsTable.krithiId, KrithiLyricVariantsTable.language)
            .map { it[KrithiLyricVariantsTable.krithiId] to it[KrithiLyricVariantsTable.language].dbValue }

        for ((krithiId, language) in allVariants) {
            if ((krithiId to language) !in variantsWithSections) {
                val expectedCount = krithiSectionCounts[krithiId] ?: 0L
                if (expectedCount > 0) {
                    val title = krithiTitles.getOrPut(krithiId) {
                        KrithisTable
                            .selectAll()
                            .andWhere { KrithisTable.id eq krithiId }
                            .singleOrNull()
                            ?.get(KrithisTable.title) ?: "Unknown"
                    }
                    issues.add(
                        SectionIssue(
                            krithiId = krithiId.toString(),
                            title = title,
                            language = language,
                            expectedSections = expectedCount,
                            actualSections = 0,
                            issueType = "missing sections",
                        )
                    )
                }
            }
        }

        issues.sortBy { it.title }

        val total = issues.size.toLong()
        val paged = issues.drop(page * size).take(size)

        SectionIssuesPage(
            items = paged,
            total = total,
            page = page,
            size = size,
        )
    }
}
