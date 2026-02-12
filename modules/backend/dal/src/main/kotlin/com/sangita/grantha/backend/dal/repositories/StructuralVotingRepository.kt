package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.StructuralVoteLogTable
import com.sangita.grantha.shared.domain.model.VotingDecisionDto
import com.sangita.grantha.shared.domain.model.VotingDetailDto
import com.sangita.grantha.shared.domain.model.VotingStatsDto
import com.sangita.grantha.shared.domain.model.ConfidenceDistributionDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Repository for structural voting queries.
 */
class StructuralVotingRepository {

    private val V = StructuralVoteLogTable
    private val K = KrithisTable

    private fun toInstant(odt: java.time.OffsetDateTime): kotlin.time.Instant =
        odt.toInstant().let { kotlin.time.Instant.fromEpochSeconds(it.epochSecond, it.nano) }

    private fun ResultRow.toDecisionDto(): VotingDecisionDto {
        val krithiId = this[V.krithiId]
        val krithiTitle = K.selectAll()
            .andWhere { K.id eq krithiId }
            .singleOrNull()?.get(K.title) ?: "Unknown"

        return VotingDecisionDto(
            id = this[V.id].value.toKotlinUuid(),
            krithiId = krithiId.toKotlinUuid(),
            krithiTitle = krithiTitle,
            votedAt = toInstant(this[V.votedAt]),
            consensusType = this[V.consensusType],
            consensusStructure = this[V.consensusStructure],
            confidence = this[V.confidence],
            dissentCount = 0, // Simplified
            reviewerId = this[V.reviewerId]?.toKotlinUuid(),
            notes = this[V.notes],
        )
    }

    suspend fun list(
        consensusType: List<String>? = null,
        confidence: List<String>? = null,
        hasDissents: Boolean? = null,
        pendingReview: Boolean? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): Pair<List<VotingDecisionDto>, Int> = DatabaseFactory.dbQuery {
        var query = V.selectAll()

        consensusType?.takeIf { it.isNotEmpty() }?.let { types ->
            query = query.andWhere { V.consensusType inList types }
        }

        confidence?.takeIf { it.isNotEmpty() }?.let { levels ->
            query = query.andWhere { V.confidence inList levels }
        }

        val total = query.count().toInt()
        val items = query
            .orderBy(V.votedAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toDecisionDto() }

        Pair(items, total)
    }

    suspend fun findById(id: Uuid): VotingDetailDto? = DatabaseFactory.dbQuery {
        val row = V.selectAll()
            .andWhere { V.id eq id.toJavaUuid() }
            .singleOrNull() ?: return@dbQuery null

        val krithiTitle = K.selectAll()
            .andWhere { K.id eq row[V.krithiId] }
            .singleOrNull()?.get(K.title) ?: "Unknown"

        VotingDetailDto(
            id = row[V.id].value.toKotlinUuid(),
            krithiId = row[V.krithiId].toKotlinUuid(),
            krithiTitle = krithiTitle,
            votedAt = toInstant(row[V.votedAt]),
            consensusType = row[V.consensusType],
            confidence = row[V.confidence],
            consensusStructure = row[V.consensusStructure],
            participatingSources = row[V.participatingSources],
            dissentingSources = row[V.dissentingSources],
            notes = row[V.notes],
            reviewerId = row[V.reviewerId]?.toKotlinUuid(),
        )
    }

    /**
     * TRACK-041: Create a voting record from the ExtractionResultProcessor.
     */
    suspend fun createVotingRecord(
        krithiId: Uuid,
        participatingSources: String,
        consensusStructure: String,
        consensusType: String,
        confidence: String,
        dissentingSources: String = "[]",
        notes: String? = null,
    ): Unit = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        V.insert {
            it[V.id] = UUID.randomUUID()
            it[V.krithiId] = krithiId.toJavaUuid()
            it[V.votedAt] = now
            it[V.participatingSources] = participatingSources
            it[V.consensusStructure] = consensusStructure
            it[V.consensusType] = consensusType
            it[V.confidence] = confidence
            it[V.dissentingSources] = dissentingSources
            it[V.notes] = notes
            it[V.createdAt] = now
        }
    }

    suspend fun createOverride(
        votingId: Uuid,
        structure: String,
        notes: String,
        reviewerId: Uuid,
    ): VotingDetailDto? = DatabaseFactory.dbQuery {
        val existing = V.selectAll()
            .andWhere { V.id eq votingId.toJavaUuid() }
            .singleOrNull() ?: return@dbQuery null

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val newId = UUID.randomUUID()

        V.insert {
            it[V.id] = newId
            it[V.krithiId] = existing[V.krithiId]
            it[V.votedAt] = now
            it[V.participatingSources] = existing[V.participatingSources]
            it[V.consensusStructure] = structure
            it[V.consensusType] = "MANUAL"
            it[V.confidence] = "HIGH"
            it[V.dissentingSources] = "[]"
            it[V.reviewerId] = reviewerId.toJavaUuid()
            it[V.notes] = notes
            it[V.createdAt] = now
        }

        findById(newId.toKotlinUuid())
    }

    suspend fun getStats(): VotingStatsDto = DatabaseFactory.dbQuery {
        val counts = mutableMapOf<String, Int>()
        val confidenceCounts = mutableMapOf<String, Int>()

        V.select(V.consensusType, V.consensusType.count())
            .groupBy(V.consensusType)
            .forEach { row ->
                counts[row[V.consensusType]] = row[V.consensusType.count()].toInt()
            }

        V.select(V.confidence, V.confidence.count())
            .groupBy(V.confidence)
            .forEach { row ->
                confidenceCounts[row[V.confidence]] = row[V.confidence.count()].toInt()
            }

        VotingStatsDto(
            total = counts.values.sum(),
            unanimous = counts["UNANIMOUS"] ?: 0,
            majority = counts["MAJORITY"] ?: 0,
            authorityOverride = counts["AUTHORITY_OVERRIDE"] ?: 0,
            singleSource = counts["SINGLE_SOURCE"] ?: 0,
            manual = counts["MANUAL"] ?: 0,
            confidenceDistribution = ConfidenceDistributionDto(
                high = confidenceCounts["HIGH"] ?: 0,
                medium = confidenceCounts["MEDIUM"] ?: 0,
                low = confidenceCounts["LOW"] ?: 0,
            ),
        )
    }
}
