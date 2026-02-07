package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.backend.api.routes.adminDashboardRoutes
import com.sangita.grantha.backend.api.routes.adminKrithiRoutes
import com.sangita.grantha.backend.api.routes.adminNotationRoutes
import com.sangita.grantha.backend.api.routes.authRefreshRoutes
import com.sangita.grantha.backend.api.routes.authRoutes
import com.sangita.grantha.backend.api.routes.auditRoutes
import com.sangita.grantha.backend.api.routes.bulkImportRoutes
import com.sangita.grantha.backend.api.routes.healthRoutes
import com.sangita.grantha.backend.api.routes.importRoutes
import com.sangita.grantha.backend.api.routes.metricsRoutes
import com.sangita.grantha.backend.api.routes.publicKrithiRoutes
import com.sangita.grantha.backend.api.routes.referenceDataRoutes
import com.sangita.grantha.backend.api.routes.userManagementRoutes
import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.config.JwtConfig
import com.sangita.grantha.backend.api.services.AdminDashboardService
import com.sangita.grantha.backend.api.services.AuditLogService
import com.sangita.grantha.backend.api.services.BulkImportOrchestrationService
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.backend.api.services.IKrithiService
import com.sangita.grantha.backend.api.services.IReferenceDataService
import com.sangita.grantha.backend.api.services.ITransliterator
import com.sangita.grantha.backend.api.services.IWebScraper
import com.sangita.grantha.backend.api.services.KrithiNotationService
import com.sangita.grantha.backend.api.services.UserManagementService
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val krithiService by inject<IKrithiService>()
    val notationService by inject<KrithiNotationService>()
    val referenceDataService by inject<IReferenceDataService>()
    val importService by inject<IImportService>()
    val auditLogService by inject<AuditLogService>()
    val dashboardService by inject<AdminDashboardService>()
    val transliterationService by inject<ITransliterator>()
    val webScrapingService by inject<IWebScraper>()
    val userManagementService by inject<UserManagementService>()
    val bulkImportService by inject<BulkImportOrchestrationService>()
    val metricsRegistry by inject<PrometheusMeterRegistry>()
    val env by inject<ApiEnvironment>()
    val jwtConfig by inject<JwtConfig>()

    install(io.ktor.server.plugins.defaultheaders.DefaultHeaders)
    routing {
        healthRoutes()
        authRoutes(env, jwtConfig, userManagementService)
        publicKrithiRoutes(krithiService, referenceDataService, notationService)

        authenticate("admin-auth") {
            authRefreshRoutes(jwtConfig)
            adminKrithiRoutes(krithiService, transliterationService)
            adminNotationRoutes(notationService)
            importRoutes(importService, webScrapingService)
            bulkImportRoutes(bulkImportService, importService)
            auditRoutes(auditLogService)
            referenceDataRoutes(referenceDataService)
            userManagementRoutes(userManagementService)
            metricsRoutes(metricsRegistry)
        }
        
        // Dashboard stats accessible with optional auth (for admin console)
        authenticate("admin-auth", optional = true) {
            adminDashboardRoutes(dashboardService)
        }
    }
}
