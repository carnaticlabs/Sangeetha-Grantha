package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.AuditLogDto
import kotlin.uuid.Uuid

/**
 * Service wrapper around audit log data access.
 */
class AuditLogService(private val dal: SangitaDal) {
    /**
     * List recent audit log entries.
     */
    suspend fun listRecent(limit: Int = 100): List<AuditLogDto> =
        dal.auditLogs.listRecent(limit)

    /**
     * List audit log entries filtered by entity table and entity ID.
     */
    suspend fun listByEntity(entityTable: String, entityId: Uuid, limit: Int = 100): List<AuditLogDto> =
        dal.auditLogs.listByEntity(entityTable, entityId, limit)
}
