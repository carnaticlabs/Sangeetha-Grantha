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

class ImportRepository {
    suspend fun listImports(status: ImportStatus? = null): List<ImportedKrithiDto> =
        DatabaseFactory.dbQuery {
            val query = ImportedKrithisTable.selectAll()
            status?.let { query.andWhere { ImportedKrithisTable.importStatus eq it } }
            query.map { it.toImportedKrithiDto() }
        }

    suspend fun findById(id: Uuid): ImportedKrithiDto? = DatabaseFactory.dbQuery {
        ImportedKrithisTable
            .selectAll()
            .andWhere { ImportedKrithisTable.id eq id.toJavaUuid() }
            .map { it.toImportedKrithiDto() }
            .singleOrNull()
    }

    suspend fun findOrCreateSource(name: String): UUID = DatabaseFactory.dbQuery {
        ImportSourcesTable
            .selectAll()
            .andWhere { ImportSourcesTable.name eq name }
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

    suspend fun findBySourceAndKey(sourceId: UUID, sourceKey: String): ImportedKrithiDto? = DatabaseFactory.dbQuery {
        ImportedKrithisTable
            .selectAll()
            .where { (ImportedKrithisTable.importSourceId eq sourceId) and (ImportedKrithisTable.sourceKey eq sourceKey) }
            .map { it.toImportedKrithiDto() }
            .singleOrNull()
    }

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
}
