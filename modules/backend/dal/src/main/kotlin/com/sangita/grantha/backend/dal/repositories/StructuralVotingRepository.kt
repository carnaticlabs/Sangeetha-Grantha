package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.StructuralVoteLogTable
import com.sangita.grantha.shared.domain.model.SectionSummaryDto
import com.sangita.grantha.shared.domain.model.VotingDecisionDto
import com.sangita.grantha.shared.domain.model.VotingDetailDto
import com.sangita.grantha.shared.domain.model.VotingParticipantDto
import com.sangita.grantha.shared.domain.model.VotingStatsDto
import com.sangita.grantha.shared.domain.model.ConfidenceDistributionDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Repository for structural voting queries.
 *
 * The `consensus_structure` / `participating_sources` JSONB columns are
 * (de)serialized here at the DB boundary so callers and DTOs deal in typed
 * [SectionSummaryDto] / [VotingParticipantDto] values. Dissent is derived from
 * participants (`!agrees`); the legacy `dissenting_sources` column is retained
 * (NOT NULL) but no longer authoritative — written as `[]`.
 */
class StructuralVotingRepository {

    private val V = StructuralVoteLogTable
    private val K = KrithisTable

    private val json = Json { ignoreUnknownKeys = true }
    private val sectionListSerializer = ListSerializer(SectionSummaryDto.serializer())
    private val participantListSerializer = ListSerializer(VotingParticipantDto.serializer())

    private fun decodeSections(raw: String?): List<SectionSummaryDto> =
        if (raw.isNullOrBlank()) emptyList() else json.decodeFromString(sectionListSerializer, raw)

    private fun decodeParticipants(raw: String?): List<VotingParticipantDto> =
        if (raw.isNullOrBlank()) emptyList() else json.decodeFromString(participantListSerializer, raw)

    private fun encodeSections(sections: List<SectionSummaryDto>): String =
        json.encodeToString(sectionListSerializer, sections)

    private fun encodeParticipants(participants: List<VotingParticipantDto>): String =
        json.encodeToString(participantListSerializer, participants)

    private fun toInstant(odt: java.time.OffsetDateTime): kotlin.time.Instant =
        odt.toInstant().let { kotlin.time.Instant.fromEpochSeconds(it.epochSecond, it.nano) }

    private fun ResultRow.toDecisionDto(krithiTitle: String): VotingDecisionDto {
        val participants = decodeParticipants(this[V.participatingSources])
        return VotingDecisionDto(
            id = this[V.id].value.toKotlinUuid(),
            krithiId = this[V.krithiId].toKotlinUuid(),
            krithiTitle = krithiTitle,
            votedAt = toInstant(this[V.votedAt]),
            sourceCount = participants.size,
            consensusType = this[V.consensusType],
            consensusStructure = decodeSections(this[V.consensusStructure]),
            confidence = this[V.confidence],
            dissentCount = participants.count { !it.agrees },
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
        var baseQuery = V.selectAll()

        consensusType?.takeIf { it.isNotEmpty() }?.let { types ->
            baseQuery = baseQuery.andWhere { V.consensusType inList types }
        }

        confidence?.takeIf { it.isNotEmpty() }?.let { levels ->
            baseQuery = baseQuery.andWhere { V.confidence inList levels }
        }

        val total = baseQuery.count().toInt()
        val rows = baseQuery
            .orderBy(V.votedAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .toList()

        // Batch-load krithi titles to avoid N+1 queries
        val krithiIds = rows.map { it[V.krithiId] }.distinct()
        val titleMap = if (krithiIds.isNotEmpty()) {
            K.select(K.id, K.title)
                .where { K.id inList krithiIds }
                .associate { it[K.id].value to it[K.title] }
        } else emptyMap()

        val items = rows.map { row ->
            row.toDecisionDto(titleMap[row[V.krithiId]] ?: "Unknown")
        }

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
            consensusStructure = decodeSections(row[V.consensusStructure]),
            participants = decodeParticipants(row[V.participatingSources]),
            notes = row[V.notes],
            reviewerId = row[V.reviewerId]?.toKotlinUuid(),
        )
    }

    /**
     * TRACK-041: Create a voting record from the ExtractionResultProcessor.
     */
    suspend fun createVotingRecord(
        krithiId: Uuid,
        participants: List<VotingParticipantDto>,
        consensusStructure: List<SectionSummaryDto>,
        consensusType: String,
        confidence: String,
        notes: String? = null,
    ): Unit = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        V.insert {
            it[V.id] = UUID.randomUUID()
            it[V.krithiId] = krithiId.toJavaUuid()
            it[V.votedAt] = now
            it[V.participatingSources] = encodeParticipants(participants)
            it[V.consensusStructure] = encodeSections(consensusStructure)
            it[V.consensusType] = consensusType
            it[V.confidence] = confidence
            it[V.dissentingSources] = "[]" // deprecated; dissent derived from participants — drop via PI-001 (application_documentation/pending_implementation.md)
            it[V.notes] = notes
            it[V.createdAt] = now
        }
    }

    suspend fun createOverride(
        votingId: Uuid,
        structure: List<SectionSummaryDto>,
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
            it[V.consensusStructure] = encodeSections(structure)
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
