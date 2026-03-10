package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.kotlinInstant
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.AuditLogTable
import com.sangita.grantha.backend.dal.tables.UsersTable
import com.sangita.grantha.shared.domain.model.AuditLogDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Repository for reading and writing audit log entries.
 */
class AuditLogRepository {
    /**
     * Returns recent audit entries ordered by timestamp descending,
     * with actor names resolved from the users table.
     */
    suspend fun listRecent(limit: Int = 100): List<AuditLogDto> = DatabaseFactory.dbQuery {
        AuditLogTable
            .join(UsersTable, JoinType.LEFT, AuditLogTable.actorUserId, UsersTable.id)
            .selectAll()
            .orderBy(AuditLogTable.changedAt to SortOrder.DESC)
            .limit(limit)
            .map { row: ResultRow -> row.toAuditLogDtoWithActor() }
    }

    /**
     * Returns audit entries filtered by entity table and entity ID,
     * with actor names resolved from the users table.
     */
    suspend fun listByEntity(entityTable: String, entityId: Uuid, limit: Int = 100): List<AuditLogDto> =
        DatabaseFactory.dbQuery {
            AuditLogTable
                .join(UsersTable, JoinType.LEFT, AuditLogTable.actorUserId, UsersTable.id)
                .selectAll()
                .where { (AuditLogTable.entityTable eq entityTable) and (AuditLogTable.entityId eq entityId.toJavaUuid()) }
                .orderBy(AuditLogTable.changedAt to SortOrder.DESC)
                .limit(limit)
                .map { row: ResultRow -> row.toAuditLogDtoWithActor() }
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

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private fun ResultRow.toAuditLogDtoWithActor(): AuditLogDto {
    val actorName = this.getOrNull(UsersTable.displayName)
        ?: this.getOrNull(UsersTable.fullName)
        ?: this.getOrNull(UsersTable.email)

    return AuditLogDto(
        id = this[AuditLogTable.id].value.toKotlinUuid(),
        actorUserId = this[AuditLogTable.actorUserId]?.toKotlinUuid(),
        actorName = actorName,
        actorIp = this[AuditLogTable.actorIp],
        action = this[AuditLogTable.action],
        entityTable = this[AuditLogTable.entityTable],
        entityId = this[AuditLogTable.entityId]?.toKotlinUuid(),
        changedAt = this.kotlinInstant(AuditLogTable.changedAt),
        diff = this[AuditLogTable.diff],
        metadata = this[AuditLogTable.metadata],
    )
}
