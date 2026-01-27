package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.AuditLogDto

/**
 * Service wrapper around audit log data access.
 */
class AuditLogService(private val dal: SangitaDal) {
    /**
     * List recent audit log entries.
     */
    suspend fun listRecent(limit: Int = 100): List<AuditLogDto> =
        dal.auditLogs.listRecent(limit)
}
