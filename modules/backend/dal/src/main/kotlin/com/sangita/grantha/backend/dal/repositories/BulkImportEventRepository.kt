package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toImportEventDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.ImportBatchTable
import com.sangita.grantha.backend.dal.tables.ImportEventTable
import com.sangita.grantha.shared.domain.model.ImportEventDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*

/**
 * Repository for import events and batch deletion.
 * Extracted from BulkImportRepository as part of TRACK-074.
 */
class BulkImportEventRepository {

    /**
     * Create a batch/job/task event.
     */
    suspend fun createEvent(
        refType: String,
        refId: Uuid,
        eventType: String,
        data: String? = null
    ): ImportEventDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val eventId = UUID.randomUUID()

        ImportEventTable.insert {
            it[id] = eventId
            it[ImportEventTable.refType] = refType
            it[ImportEventTable.refId] = refId.toJavaUuid()
            it[ImportEventTable.eventType] = eventType
            it[ImportEventTable.data] = data
            it[ImportEventTable.createdAt] = now
        }
            .resultedValues
            ?.single()
            ?.toImportEventDto()
            ?: error("Failed to create import event")
    }

    /**
     * List events for a reference type/id ordered by most recent.
     */
    suspend fun listEventsByRef(
        refType: String,
        refId: Uuid,
        limit: Int = 100
    ): List<ImportEventDto> = DatabaseFactory.dbQuery {
        ImportEventTable
            .selectAll()
            .andWhere { ImportEventTable.refType eq refType }
            .andWhere { ImportEventTable.refId eq refId.toJavaUuid() }
            .orderBy(ImportEventTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toImportEventDto() }
    }

    /**
     * Delete a batch and its related records.
     */
    suspend fun deleteBatch(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        ImportEventTable.deleteWhere {
            (ImportEventTable.refType eq "batch") and (ImportEventTable.refId eq id.toJavaUuid())
        }
        val deleted = ImportBatchTable.deleteWhere { ImportBatchTable.id eq id.toJavaUuid() }
        deleted > 0
    }
}
