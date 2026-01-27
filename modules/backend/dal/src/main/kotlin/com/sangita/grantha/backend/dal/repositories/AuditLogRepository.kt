package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toAuditLogDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.AuditLogTable
import com.sangita.grantha.shared.domain.model.AuditLogDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Repository for reading and writing audit log entries.
 */
class AuditLogRepository {
    /**
     * Returns recent audit entries ordered by timestamp descending.
     */
    suspend fun listRecent(limit: Int = 100): List<AuditLogDto> = DatabaseFactory.dbQuery {
        AuditLogTable
            .selectAll()
            .orderBy(AuditLogTable.changedAt to SortOrder.DESC)
            .limit(limit)
            .map { row: ResultRow -> row.toAuditLogDto() }
    }

    /**
     * Appends a single audit log entry.
     */
    suspend fun append(
        action: String,
        entityTable: String,
        entityId: Uuid? = null,
        actorUserId: Uuid? = null,
        actorIp: String? = null,
        diff: String? = null,
        metadata: String? = null,
    ) = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        AuditLogTable.insert {
            it[AuditLogTable.action] = action
            it[AuditLogTable.entityTable] = entityTable
            it[AuditLogTable.entityId] = entityId?.toJavaUuid()
            it[AuditLogTable.actorUserId] = actorUserId?.toJavaUuid()
            it[AuditLogTable.actorIp] = actorIp
            it[AuditLogTable.changedAt] = now
            it[AuditLogTable.diff] = diff
            it[AuditLogTable.metadata] = metadata
        }
    }
}
