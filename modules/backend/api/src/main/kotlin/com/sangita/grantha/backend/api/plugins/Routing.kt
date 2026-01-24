package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.backend.api.routes.adminKrithiRoutes
import com.sangita.grantha.backend.api.routes.adminNotationRoutes
import com.sangita.grantha.backend.api.routes.auditRoutes
import com.sangita.grantha.backend.api.routes.healthRoutes
import com.sangita.grantha.backend.api.routes.importRoutes
import com.sangita.grantha.backend.api.routes.bulkImportRoutes
import com.sangita.grantha.backend.api.routes.publicKrithiRoutes
import com.sangita.grantha.backend.api.routes.adminDashboardRoutes
import com.sangita.grantha.backend.api.routes.referenceDataRoutes
import com.sangita.grantha.backend.api.routes.userManagementRoutes
import com.sangita.grantha.backend.api.services.AuditLogService
import com.sangita.grantha.backend.api.services.ImportService
import com.sangita.grantha.backend.api.services.KrithiNotationService
import com.sangita.grantha.backend.api.services.KrithiService
import com.sangita.grantha.backend.api.services.ReferenceDataService
import com.sangita.grantha.backend.api.services.AdminDashboardService
import com.sangita.grantha.backend.api.services.TransliterationService
import com.sangita.grantha.backend.api.services.WebScrapingService
import com.sangita.grantha.backend.api.services.UserManagementService
import com.sangita.grantha.backend.api.services.BulkImportOrchestrationService

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing

fun Application.configureRouting(
    krithiService: KrithiService,
    notationService: KrithiNotationService,
    referenceDataService: ReferenceDataService,
    importService: ImportService,
    auditLogService: AuditLogService,
    dashboardService: AdminDashboardService,
    transliterationService: TransliterationService,
    webScrapingService: WebScrapingService,
    userManagementService: UserManagementService,
    bulkImportService: BulkImportOrchestrationService,
) {
    install(io.ktor.server.plugins.defaultheaders.DefaultHeaders)
    routing {
        healthRoutes()
        publicKrithiRoutes(krithiService, referenceDataService, notationService)

        authenticate("admin-auth") {
            adminKrithiRoutes(krithiService, transliterationService)
            adminNotationRoutes(notationService)
            importRoutes(importService, webScrapingService)
            bulkImportRoutes(bulkImportService, importService)
            auditRoutes(auditLogService)
            referenceDataRoutes(referenceDataService)
            userManagementRoutes(userManagementService)
        }
        
        // Dashboard stats accessible with optional auth (for admin console)
        authenticate("admin-auth", optional = true) {
            adminDashboardRoutes(dashboardService)
        }
    }
}
