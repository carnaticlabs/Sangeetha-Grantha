package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.VariantMatchTable
import com.sangita.grantha.shared.domain.model.VariantMatchDto
import com.sangita.grantha.shared.domain.model.VariantMatchReportDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 * TRACK-056: Repository for variant match operations.
 */
class VariantMatchRepository {

    private val VM = VariantMatchTable
    private val K = KrithisTable

    private fun toInstant(odt: OffsetDateTime): kotlin.time.Instant =
        odt.toInstant().let { kotlin.time.Instant.fromEpochSeconds(it.epochSecond, it.nano) }

    private fun ResultRow.toDto(krithiTitle: String): VariantMatchDto {
        return VariantMatchDto(
            id = this[VM.id].value.toKotlinUuid(),
            extractionId = this[VM.extractionId].toKotlinUuid(),
            krithiId = this[VM.krithiId].toKotlinUuid(),
            krithiTitle = krithiTitle,
            confidence = this[VM.confidence].toDouble(),
            confidenceTier = this[VM.confidenceTier],
            matchSignals = this[VM.matchSignals],
            matchStatus = this[VM.matchStatus],
            isAnomaly = this[VM.isAnomaly],
            structureMismatch = this[VM.structureMismatch],
            reviewerNotes = this[VM.reviewerNotes],
            createdAt = toInstant(this[VM.createdAt]),
        )
    }

    /** Resolve krithi titles for a list of rows in a single batch query. */
    private fun batchResolveTitles(rows: List<ResultRow>): Map<java.util.UUID, String> {
        val krithiIds = rows.map { it[VM.krithiId] }.distinct()
        if (krithiIds.isEmpty()) return emptyMap()
        return K.select(K.id, K.title)
            .where { K.id inList krithiIds }
            .associate { it[K.id].value to it[K.title] }
    }

    /**
     * Find a variant match by ID.
     */
    suspend fun findById(id: Uuid): Pair<VariantMatchDto, String?>? = DatabaseFactory.dbQuery {
        val row = VM.selectAll()
            .andWhere { VM.id eq id.toJavaUuid() }
            .singleOrNull() ?: return@dbQuery null
        val title = K.selectAll()
            .andWhere { K.id eq row[VM.krithiId] }
            .singleOrNull()?.get(K.title) ?: "Unknown"
        row.toDto(title) to row[VM.extractionPayload]
    }

    /**
     * List variant matches for a given extraction, with optional status filter.
     */
    suspend fun listByExtraction(
        extractionId: Uuid,
        status: List<String>? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): Pair<List<VariantMatchDto>, Int> = DatabaseFactory.dbQuery {
        var query = VM.selectAll()
            .andWhere { VM.extractionId eq extractionId.toJavaUuid() }

        status?.takeIf { it.isNotEmpty() }?.let { statuses ->
            query = query.andWhere { VM.matchStatus inList statuses }
        }

        val total = query.count().toInt()
        val rows = query
            .orderBy(VM.confidence to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .toList()
        val titleMap = batchResolveTitles(rows)
        val items = rows.map { it.toDto(titleMap[it[VM.krithiId]] ?: "Unknown") }

        Pair(items, total)
    }

    /**
     * List all pending variant matches across all extractions.
     */
    suspend fun listPending(
        limit: Int = 50,
        offset: Int = 0,
    ): Pair<List<VariantMatchDto>, Int> = DatabaseFactory.dbQuery {
        val query = VM.selectAll()
            .andWhere { VM.matchStatus eq "PENDING" }

        val total = query.count().toInt()
        val rows = query
            .orderBy(VM.createdAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .toList()
        val titleMap = batchResolveTitles(rows)
        val items = rows.map { it.toDto(titleMap[it[VM.krithiId]] ?: "Unknown") }

        Pair(items, total)
    }

    /**
     * Create a new variant match record.
     */
    suspend fun create(
        extractionId: Uuid,
        krithiId: Uuid,
        confidence: Double,
        confidenceTier: String,
        matchSignals: String,
        matchStatus: String = "PENDING",
        extractionPayload: String? = null,
        isAnomaly: Boolean = false,
        structureMismatch: Boolean = false,
    ): VariantMatchDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val matchId = UUID.randomUUID()

        VM.insert {
            it[VM.id] = matchId
            it[VM.extractionId] = extractionId.toJavaUuid()
            it[VM.krithiId] = krithiId.toJavaUuid()
            it[VM.confidence] = confidence.toBigDecimal()
            it[VM.confidenceTier] = confidenceTier
            it[VM.matchSignals] = matchSignals
            it[VM.matchStatus] = matchStatus
            it[VM.extractionPayload] = extractionPayload
            it[VM.isAnomaly] = isAnomaly
            it[VM.structureMismatch] = structureMismatch
            it[VM.createdAt] = now
            it[VM.updatedAt] = now
        }

        val row = VM.selectAll()
            .andWhere { VM.id eq matchId }
            .single()
        val title = K.selectAll()
            .andWhere { K.id eq krithiId.toJavaUuid() }
            .singleOrNull()?.get(K.title) ?: "Unknown"
        row.toDto(title)
    }

    /**
     * Update match status (approve, reject).
     */
    suspend fun updateStatus(
        id: Uuid,
        matchStatus: String,
        reviewerId: Uuid?,
        reviewerNotes: String? = null,
    ): Boolean = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        VM.update({ VM.id eq id.toJavaUuid() }) {
            it[VM.matchStatus] = matchStatus
            it[VM.reviewedBy] = reviewerId?.toJavaUuid()
            it[VM.reviewedAt] = now
            it[VM.reviewerNotes] = reviewerNotes
            it[VM.updatedAt] = now
        } > 0
    }

    /**
     * Get a report summary for all matches associated with an extraction.
     */
    suspend fun getReport(extractionId: Uuid): VariantMatchReportDto = DatabaseFactory.dbQuery {
        val matches = VM.selectAll()
            .andWhere { VM.extractionId eq extractionId.toJavaUuid() }
            .toList()

        VariantMatchReportDto(
            extractionId = extractionId,
            totalMatches = matches.size,
            highConfidence = matches.count { it[VM.confidenceTier] == "HIGH" },
            mediumConfidence = matches.count { it[VM.confidenceTier] == "MEDIUM" },
            lowConfidence = matches.count { it[VM.confidenceTier] == "LOW" },
            anomalies = matches.count { it[VM.isAnomaly] },
            autoApproved = matches.count { it[VM.matchStatus] == "AUTO_APPROVED" },
        )
    }
}
