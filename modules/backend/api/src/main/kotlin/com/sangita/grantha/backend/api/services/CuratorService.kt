package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.dal.tables.ImportedKrithisTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.RagaResolutionQueueTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

@Serializable
data class CuratorStats(
    val totalPending: Long,
    val totalApproved: Long,
    val totalRejected: Long,
    val totalKrithis: Long,
    val sectionIssuesCount: Long,
    val unresolvedRagaCount: Long = 0,
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

    private companion object {
        /**
         * Canonical section-mismatch count as a single aggregate: number of
         * (krithi, language) variant rows whose lyric-section count differs from the
         * krithi's canonical `krithi_sections` count. LEFT JOINs so a variant with zero
         * lyric sections counts as 0 (still a mismatch when the krithi has sections),
         * matching the track's reference query. Returns exactly one row (a bare COUNT).
         */
        val SECTION_ISSUES_COUNT_SQL = """
            WITH canon AS (
                SELECT krithi_id, COUNT(*) AS c FROM krithi_sections GROUP BY krithi_id
            ),
            var AS (
                SELECT v.krithi_id, v.language, COUNT(s.id) AS c
                FROM krithi_lyric_variants v
                LEFT JOIN krithi_lyric_sections s ON s.lyric_variant_id = v.id
                GROUP BY v.krithi_id, v.language
            )
            SELECT COUNT(*) AS mismatch_count
            FROM var
            LEFT JOIN canon ON canon.krithi_id = var.krithi_id
            WHERE var.c <> COALESCE(canon.c, 0)
        """.trimIndent()
    }

    suspend fun getStats(): CuratorStats = DatabaseFactory.dbQuery {
        val pending = ImportedKrithisTable
            .selectAll()
            .andWhere {
                ImportedKrithisTable.importStatus inList listOf(ImportStatus.PENDING, ImportStatus.IN_REVIEW)
            }
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

        // Count section-issue variant rows with a single SQL aggregate rather than
        // scanning krithi_sections + krithi_lyric_sections into memory and diffing in
        // Kotlin (TRACK-133 folded-in cleanup). A "section issue" is a (krithi, language)
        // variant whose lyric-section count differs from its krithi's canonical section
        // count; sectionIssuesCount is the number of such variant rows. Mirrors the
        // track's canonical mismatch query and AuditSqlQueries.SECTION_COUNT_MISMATCH_SQL.
        var sectionIssuesCount = 0L
        exec(SECTION_ISSUES_COUNT_SQL, emptyList(), StatementType.SELECT) { rs ->
            if (rs.next()) sectionIssuesCount = rs.getLong(1)
        }

        val unresolvedRagaCount = RagaResolutionQueueTable
            .selectAll()
            .where { RagaResolutionQueueTable.status eq "pending" }
            .count()

        CuratorStats(
            totalPending = pending,
            totalApproved = approved,
            totalRejected = rejected,
            totalKrithis = totalKrithis,
            sectionIssuesCount = sectionIssuesCount,
            unresolvedRagaCount = unresolvedRagaCount,
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
