package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.ExtractionIntent
import com.sangita.grantha.backend.dal.enums.ExtractionStatus
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.ExtractionQueueTable
import com.sangita.grantha.shared.domain.model.ExtractionTaskDto
import com.sangita.grantha.shared.domain.model.ExtractionDetailDto
import com.sangita.grantha.shared.domain.model.ExtractionStatsDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Repository for extraction queue operations.
 */
class ExtractionQueueRepository {

    private val T = ExtractionQueueTable

    private fun toInstant(odt: java.time.OffsetDateTime?): kotlin.time.Instant? =
        odt?.toInstant()?.let { kotlin.time.Instant.fromEpochSeconds(it.epochSecond, it.nano) }

    private fun ResultRow.toTaskDto(): ExtractionTaskDto = ExtractionTaskDto(
        id = this[T.id].value.toKotlinUuid(),
        sourceUrl = this[T.sourceUrl],
        sourceFormat = this[T.sourceFormat],
        sourceName = this[T.sourceName],
        sourceTier = this[T.sourceTier],
        importBatchId = this[T.importBatchId]?.toKotlinUuid(),
        status = this[T.status].dbValue,
        resultCount = this[T.resultCount],
        confidence = this[T.confidence]?.toDouble(),
        extractionMethod = this[T.extractionMethod],
        extractorVersion = this[T.extractorVersion],
        durationMs = this[T.durationMs],
        attempts = this[T.attempts],
        maxAttempts = this[T.maxAttempts],
        claimedBy = this[T.claimedBy],
        claimedAt = toInstant(this[T.claimedAt]),
        pageRange = this[T.pageRange],
        lastErrorAt = toInstant(this[T.lastErrorAt]),
        contentLanguage = this[T.contentLanguage],
        extractionIntent = this[T.extractionIntent].dbValue,
        relatedExtractionId = this[T.relatedExtractionId]?.toKotlinUuid(),
        createdAt = toInstant(this[T.createdAt])!!,
        updatedAt = toInstant(this[T.updatedAt])!!,
    )

    private fun ResultRow.toDetailDto(): ExtractionDetailDto = ExtractionDetailDto(
        id = this[T.id].value.toKotlinUuid(),
        sourceUrl = this[T.sourceUrl],
        sourceFormat = this[T.sourceFormat],
        sourceName = this[T.sourceName],
        sourceTier = this[T.sourceTier],
        importBatchId = this[T.importBatchId]?.toKotlinUuid(),
        status = this[T.status].dbValue,
        resultCount = this[T.resultCount],
        confidence = this[T.confidence]?.toDouble(),
        extractionMethod = this[T.extractionMethod],
        extractorVersion = this[T.extractorVersion],
        durationMs = this[T.durationMs],
        attempts = this[T.attempts],
        maxAttempts = this[T.maxAttempts],
        claimedBy = this[T.claimedBy],
        claimedAt = toInstant(this[T.claimedAt]),
        pageRange = this[T.pageRange],
        lastErrorAt = toInstant(this[T.lastErrorAt]),
        requestPayload = this[T.requestPayload],
        resultPayload = this[T.resultPayload],
        errorDetail = this[T.errorDetail],
        sourceChecksum = this[T.sourceChecksum],
        cachedArtifactPath = this[T.cachedArtifactPath],
        contentLanguage = this[T.contentLanguage],
        extractionIntent = this[T.extractionIntent].dbValue,
        relatedExtractionId = this[T.relatedExtractionId]?.toKotlinUuid(),
        createdAt = toInstant(this[T.createdAt])!!,
        updatedAt = toInstant(this[T.updatedAt])!!,
    )

    suspend fun list(
        status: List<String>? = null,
        format: List<String>? = null,
        sourceId: String? = null,
        batchId: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): Pair<List<ExtractionTaskDto>, Int> = DatabaseFactory.dbQuery {
        var query = T.selectAll()

        status?.takeIf { it.isNotEmpty() }?.let { statuses ->
            val enumStatuses = statuses.mapNotNull { s ->
                try { ExtractionStatus.entries.first { it.dbValue == s } } catch (_: Exception) { null }
            }
            if (enumStatuses.isNotEmpty()) {
                query = query.andWhere { T.status inList enumStatuses }
            }
        }

        format?.takeIf { it.isNotEmpty() }?.let { formats ->
            query = query.andWhere { T.sourceFormat inList formats }
        }

        batchId?.let { bid ->
            try {
                val uuid = UUID.fromString(bid)
                query = query.andWhere { T.importBatchId eq uuid }
            } catch (_: Exception) { /* ignore invalid UUID */ }
        }

        val total = query.count().toInt()
        val items = query
            .orderBy(T.createdAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toTaskDto() }

        Pair(items, total)
    }

    suspend fun findById(id: Uuid): ExtractionDetailDto? = DatabaseFactory.dbQuery {
        T.selectAll()
            .andWhere { T.id eq id.toJavaUuid() }
            .map { it.toDetailDto() }
            .singleOrNull()
    }

    suspend fun create(
        sourceUrl: String,
        sourceFormat: String,
        importBatchId: UUID? = null,
        pageRange: String? = null,
        composerHint: String? = null,
        maxAttempts: Int = 3,
        requestPayload: String = "{}",
        contentLanguage: String? = null,
        extractionIntent: ExtractionIntent = ExtractionIntent.PRIMARY,
        relatedExtractionId: UUID? = null,
    ): ExtractionDetailDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val taskId = UUID.randomUUID()

        T.insert {
            it[T.id] = taskId
            it[T.sourceUrl] = sourceUrl
            it[T.sourceFormat] = sourceFormat
            it[T.importBatchId] = importBatchId
            it[T.pageRange] = pageRange
            it[T.requestPayload] = requestPayload
            it[T.status] = ExtractionStatus.PENDING
            it[T.attempts] = 0
            it[T.maxAttempts] = maxAttempts
            it[T.contentLanguage] = contentLanguage
            it[T.extractionIntent] = extractionIntent
            it[T.relatedExtractionId] = relatedExtractionId
            it[T.createdAt] = now
            it[T.updatedAt] = now
        }

        T.selectAll()
            .andWhere { T.id eq taskId }
            .map { it.toDetailDto() }
            .single()
    }

    suspend fun retry(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        T.update({ T.id eq id.toJavaUuid() }) {
            it[T.status] = ExtractionStatus.PENDING
            it[T.updatedAt] = now
        } > 0
    }

    suspend fun cancel(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        T.update({ T.id eq id.toJavaUuid() }) {
            it[T.status] = ExtractionStatus.CANCELLED
            it[T.updatedAt] = now
        } > 0
    }

    /**
     * TRACK-041: Mark an extraction task as ingested (results processed by Kotlin).
     */
    suspend fun markIngested(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        T.update({ T.id eq id.toJavaUuid() }) {
            it[T.status] = ExtractionStatus.INGESTED
            it[T.updatedAt] = now
        } > 0
    }

    /**
     * Mark an extraction task as DONE with worker payload metadata.
     */
    suspend fun markDone(
        id: Uuid,
        resultPayload: String,
        resultCount: Int,
        extractionMethod: String,
        extractorVersion: String,
        durationMs: Int? = null,
    ): Boolean = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        T.update({ T.id eq id.toJavaUuid() }) {
            it[T.status] = ExtractionStatus.DONE
            it[T.resultPayload] = resultPayload
            it[T.resultCount] = resultCount
            it[T.extractionMethod] = extractionMethod
            it[T.extractorVersion] = extractorVersion
            it[T.durationMs] = durationMs
            it[T.errorDetail] = null
            it[T.lastErrorAt] = null
            it[T.updatedAt] = now
        } > 0
    }

    suspend fun retryAllFailed(): Int = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        T.update({ T.status eq ExtractionStatus.FAILED }) {
            it[T.status] = ExtractionStatus.PENDING
            it[T.updatedAt] = now
        }
    }

    suspend fun getStats(): ExtractionStatsDto = DatabaseFactory.dbQuery {
        val counts = mutableMapOf<String, Int>()
        T.select(T.status, T.status.count())
            .groupBy(T.status)
            .forEach { row ->
                counts[row[T.status].dbValue] = row[T.status.count()].toInt()
            }

        ExtractionStatsDto(
            pending = counts["PENDING"] ?: 0,
            processing = counts["PROCESSING"] ?: 0,
            done = counts["DONE"] ?: 0,
            failed = counts["FAILED"] ?: 0,
            cancelled = counts["CANCELLED"] ?: 0,
            total = counts.values.sum(),
            throughputPerHour = 0.0, // Simplified for initial implementation
        )
    }
}
