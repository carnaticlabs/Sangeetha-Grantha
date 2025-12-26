package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.AuditLogDto

class AuditLogService(private val dal: SangitaDal) {
    suspend fun listRecent(limit: Int = 100): List<AuditLogDto> =
        dal.auditLogs.listRecent(limit)
}
