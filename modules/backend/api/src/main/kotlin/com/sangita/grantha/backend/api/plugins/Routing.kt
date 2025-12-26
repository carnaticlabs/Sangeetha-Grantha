package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.backend.api.routes.adminKrithiRoutes
import com.sangita.grantha.backend.api.routes.auditRoutes
import com.sangita.grantha.backend.api.routes.healthRoutes
import com.sangita.grantha.backend.api.routes.importRoutes
import com.sangita.grantha.backend.api.routes.publicKrithiRoutes
import com.sangita.grantha.backend.api.services.AuditLogService
import com.sangita.grantha.backend.api.services.ImportService
import com.sangita.grantha.backend.api.services.KrithiService
import com.sangita.grantha.backend.api.services.ReferenceDataService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing

fun Application.configureRouting(
    krithiService: KrithiService,
    referenceDataService: ReferenceDataService,
    importService: ImportService,
    auditLogService: AuditLogService,
) {
    install(io.ktor.server.plugins.defaultheaders.DefaultHeaders)
    routing {
        healthRoutes()
        publicKrithiRoutes(krithiService, referenceDataService)

        authenticate("admin-auth") {
            adminKrithiRoutes(krithiService)
            importRoutes(importService)
            auditRoutes(auditLogService)
        }
    }
}
