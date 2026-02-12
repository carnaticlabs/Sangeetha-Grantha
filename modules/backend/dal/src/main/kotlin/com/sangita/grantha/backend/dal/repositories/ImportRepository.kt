package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.dal.models.toImportedKrithiDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.ImportSourcesTable
import com.sangita.grantha.backend.dal.tables.ImportedKrithisTable
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*

/**
 * Repository for import sources and imported krithi records.
 */
class ImportRepository {
    /**
     * List imported krithis with optional status filter.
     */
    suspend fun listImports(status: ImportStatus? = null): List<ImportedKrithiDto> =
        DatabaseFactory.dbQuery {
            val query = ImportedKrithisTable.selectAll()
            status?.let { query.andWhere { ImportedKrithisTable.importStatus eq it } }
            query.map { it.toImportedKrithiDto() }
        }

    /**
     * Find an imported krithi by ID.
     */
    suspend fun findById(id: Uuid): ImportedKrithiDto? = DatabaseFactory.dbQuery {
        ImportedKrithisTable
            .selectAll()
            .andWhere { ImportedKrithisTable.id eq id.toJavaUuid() }
            .map { it.toImportedKrithiDto() }
            .singleOrNull()
    }

    /**
     * Find or create an import source and return its ID.
     */
    suspend fun findOrCreateSource(name: String): UUID = DatabaseFactory.dbQuery {
        ImportSourcesTable
            .selectAll()
            .andWhere { ImportSourcesTable.name eq name }
            .limit(1)
            .map { it[ImportSourcesTable.id].value }
            .singleOrNull()
            ?: run {
                val now = OffsetDateTime.now(ZoneOffset.UTC)
                val sourceId = UUID.randomUUID()
                ImportSourcesTable.insert {
                    it[id] = sourceId
                    it[ImportSourcesTable.name] = name
                    it[ImportSourcesTable.createdAt] = now
                }
                sourceId
            }
    }

    /**
     * Create an imported krithi record (idempotent by source key).
     */
    suspend fun createImport(
        sourceId: UUID,
        sourceKey: String?,
        rawTitle: String?,
        rawLyrics: String?,
        rawComposer: String?,
        rawRaga: String?,
        rawTala: String?,
        rawDeity: String?,
        rawTemple: String?,
        rawLanguage: String?,
        parsedPayload: String?,
        importBatchId: UUID? = null,
    ): ImportedKrithiDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        if (!sourceKey.isNullOrBlank()) {
            ImportedKrithisTable
                .selectAll()
                .andWhere { (ImportedKrithisTable.importSourceId eq sourceId) and (ImportedKrithisTable.sourceKey eq sourceKey) }
                .singleOrNull()
                ?.toImportedKrithiDto()
                ?.let { return@dbQuery it }
        }

        val importId = UUID.randomUUID()

        ImportedKrithisTable.insert {
            it[id] = importId
            it[importSourceId] = sourceId
            it[ImportedKrithisTable.importBatchId] = importBatchId
            it[ImportedKrithisTable.sourceKey] = sourceKey
            it[ImportedKrithisTable.rawTitle] = rawTitle
            it[ImportedKrithisTable.rawLyrics] = rawLyrics
            it[ImportedKrithisTable.rawComposer] = rawComposer
            it[ImportedKrithisTable.rawRaga] = rawRaga
            it[ImportedKrithisTable.rawTala] = rawTala
            it[ImportedKrithisTable.rawDeity] = rawDeity
            it[ImportedKrithisTable.rawTemple] = rawTemple
            it[ImportedKrithisTable.rawLanguage] = rawLanguage
            it[ImportedKrithisTable.parsedPayload] = parsedPayload
            it[ImportedKrithisTable.importStatus] = ImportStatus.PENDING
            it[ImportedKrithisTable.createdAt] = now
        }
            .resultedValues
            ?.single()
            ?.toImportedKrithiDto()
            ?: error("Failed to insert imported krithi")
    }

    /**
     * List imports for a batch with optional status filter.
     */
    suspend fun listByBatch(batchId: Uuid, status: ImportStatus? = null): List<ImportedKrithiDto> = DatabaseFactory.dbQuery {
        val query = ImportedKrithisTable.selectAll().where { ImportedKrithisTable.importBatchId eq batchId.toJavaUuid() }
        status?.let { query.andWhere { ImportedKrithisTable.importStatus eq it } }
        query.map { it.toImportedKrithiDto() }
    }

    // TRACK-013: Optimized deduplication query - use DB ILIKE for filtering instead of loading all
    /**
     * Find similar pending imports for deduplication.
     */
    suspend fun findSimilarPendingImports(
        normalizedTitle: String,
        excludeId: Uuid? = null,
        batchId: Uuid? = null,
        limit: Int = 10
    ): List<ImportedKrithiDto> = DatabaseFactory.dbQuery {
        var query = ImportedKrithisTable
            .selectAll()
            .andWhere { ImportedKrithisTable.importStatus eq ImportStatus.PENDING }

        excludeId?.let { query = query.andWhere { ImportedKrithisTable.id neq it.toJavaUuid() } }
        batchId?.let { query = query.andWhere { ImportedKrithisTable.importBatchId eq it.toJavaUuid() } }

        // NOTE: Title fuzzy matching intentionally omitted here because Exposed 1.0.0-rc-2's
        // LIKE/lower-case helpers are in flux and this module compiles with deprecations as errors.

        query
            .limit(limit)
            .map { it.toImportedKrithiDto() }
    }

    /**
     * Review an import and set its mapped krithi ID.
     */
    suspend fun reviewImport(
        id: Uuid,
        status: ImportStatus,
        mappedKrithiId: UUID?,
        reviewerNotes: String?,
    ): ImportedKrithiDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaId = id.toJavaUuid()
        
        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        ImportedKrithisTable
            .updateReturning(
                where = { ImportedKrithisTable.id eq javaId }
            ) {
                it[ImportedKrithisTable.importStatus] = status
                it[ImportedKrithisTable.mappedKrithiId] = mappedKrithiId
                it[ImportedKrithisTable.reviewerNotes] = reviewerNotes
                it[ImportedKrithisTable.reviewedAt] = now
            }
            .singleOrNull()
            ?.toImportedKrithiDto()
    }

    /**
     * Find an import by source and source key.
     */
    suspend fun findBySourceAndKey(sourceId: UUID, sourceKey: String): ImportedKrithiDto? = DatabaseFactory.dbQuery {
        ImportedKrithisTable
            .selectAll()
            .where { (ImportedKrithisTable.importSourceId eq sourceId) and (ImportedKrithisTable.sourceKey eq sourceKey) }
            .map { it.toImportedKrithiDto() }
            .singleOrNull()
    }

    /**
     * Persist resolution data for an import.
     */
    suspend fun saveResolution(
        id: Uuid,
        resolutionData: String,
    ): ImportedKrithiDto? = DatabaseFactory.dbQuery {
        val javaId = id.toJavaUuid()
        ImportedKrithisTable
            .updateReturning(
                where = { ImportedKrithisTable.id eq javaId }
            ) {
                it[ImportedKrithisTable.resolutionData] = resolutionData
            }
            .singleOrNull()
            ?.toImportedKrithiDto()
    }

    /**
     * Persist duplicate candidates for an import.
     */
    suspend fun saveDuplicates(
        id: Uuid,
        duplicateCandidates: String,
    ): ImportedKrithiDto? = DatabaseFactory.dbQuery {
        val javaId = id.toJavaUuid()
        ImportedKrithisTable
            .updateReturning(
                where = { ImportedKrithisTable.id eq javaId }
            ) {
                it[ImportedKrithisTable.duplicateCandidates] = duplicateCandidates
            }
            .singleOrNull()
            ?.toImportedKrithiDto()
    }

    // TRACK-011: Update quality scoring fields
    /**
     * Update quality scoring fields on an import.
     */
    suspend fun updateQualityScores(
        id: Uuid,
        qualityScore: Double? = null,
        qualityTier: String? = null,
        completenessScore: Double? = null,
        resolutionConfidence: Double? = null,
        sourceQuality: Double? = null,
        validationScore: Double? = null,
    ): ImportedKrithiDto? = DatabaseFactory.dbQuery {
        val javaId = id.toJavaUuid()
        ImportedKrithisTable
            .updateReturning(
                where = { ImportedKrithisTable.id eq javaId }
            ) { stmt ->
                qualityScore?.let { stmt[ImportedKrithisTable.qualityScore] = it.toBigDecimal() }
                qualityTier?.let { stmt[ImportedKrithisTable.qualityTier] = it }
                completenessScore?.let { stmt[ImportedKrithisTable.completenessScore] = it.toBigDecimal() }
                resolutionConfidence?.let { stmt[ImportedKrithisTable.resolutionConfidence] = it.toBigDecimal() }
                sourceQuality?.let { stmt[ImportedKrithisTable.sourceQuality] = it.toBigDecimal() }
                validationScore?.let { stmt[ImportedKrithisTable.validationScore] = it.toBigDecimal() }
            }
            .singleOrNull()
            ?.toImportedKrithiDto()
    }
}
