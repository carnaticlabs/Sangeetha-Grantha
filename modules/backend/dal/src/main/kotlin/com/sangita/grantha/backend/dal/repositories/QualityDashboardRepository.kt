package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.KrithiSourceEvidenceTable
import com.sangita.grantha.backend.dal.tables.StructuralVoteLogTable
import com.sangita.grantha.shared.domain.model.QualitySummaryDto
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Repository for quality dashboard aggregation queries.
 */
class QualityDashboardRepository {

    suspend fun getSummary(): QualitySummaryDto = DatabaseFactory.dbQuery {
        val totalKrithis = KrithisTable.selectAll().count().toInt()

        // Krithis with 2+ source evidence entries
        val multiSourceKrithis = KrithiSourceEvidenceTable
            .select(KrithiSourceEvidenceTable.krithiId, KrithiSourceEvidenceTable.krithiId.count())
            .groupBy(KrithiSourceEvidenceTable.krithiId)
            .having { KrithiSourceEvidenceTable.krithiId.count() greaterEq 2L }
            .count().toInt()

        // Krithis with HIGH confidence voting
        val consensusKrithis = StructuralVoteLogTable
            .selectAll()
            .andWhere { StructuralVoteLogTable.confidence eq "HIGH" }
            .count().toInt()

        QualitySummaryDto(
            totalKrithis = totalKrithis,
            multiSourceCount = multiSourceKrithis,
            multiSourcePercent = if (totalKrithis > 0) (multiSourceKrithis.toDouble() / totalKrithis * 100) else 0.0,
            consensusCount = consensusKrithis,
            consensusPercent = if (totalKrithis > 0) (consensusKrithis.toDouble() / totalKrithis * 100) else 0.0,
            avgQualityScore = 0.0, // Will be computed once quality scores are populated
            enrichmentCoveragePercent = 0.0,
        )
    }
}
