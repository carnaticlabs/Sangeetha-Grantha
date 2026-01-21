package com.sangita.grantha.backend.api

import com.sangita.grantha.backend.api.config.ApiEnvironmentLoader
import com.sangita.grantha.backend.api.config.LogbackConfig
import com.sangita.grantha.backend.api.plugins.configureCors
import com.sangita.grantha.backend.api.plugins.configureRequestLogging
import com.sangita.grantha.backend.api.plugins.configureRouting
import com.sangita.grantha.backend.api.plugins.configureSecurity
import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.AuditLogService
import com.sangita.grantha.backend.api.services.BulkImportOrchestrationService
import com.sangita.grantha.backend.api.services.BulkImportWorkerService
import com.sangita.grantha.backend.api.services.ImportService
import com.sangita.grantha.backend.api.services.KrithiNotationService
import com.sangita.grantha.backend.api.services.KrithiService
import com.sangita.grantha.backend.api.services.ReferenceDataService
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.api.clients.GeminiApiClient
import com.sangita.grantha.backend.api.services.TransliterationService
import com.sangita.grantha.backend.api.services.WebScrapingService

import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("SangitaAPI")

fun main() {
    val env = ApiEnvironmentLoader.load()
    LogbackConfig.configure(env)
    logger.info("Starting Sangita Grantha API (env: {})", env.environment)
    logger.info("Loaded Gemini API Key: ${env.geminiApiKey?.take(4)}... (Length: ${env.geminiApiKey?.length ?: 0})")
    
    if (env.database == null) {
        throw IllegalStateException("Database configuration is missing")
    }
    DatabaseFactory.connect(env.database)
    val dal = SangitaDal()
    val krithiService = KrithiService(dal)
    val notationService = KrithiNotationService(dal)
    val referenceDataService = ReferenceDataService(dal)
    val importService = ImportService(dal)
    val auditLogService = AuditLogService(dal)
    val dashboardService = com.sangita.grantha.backend.api.services.AdminDashboardService(dal)
    val userManagementService = com.sangita.grantha.backend.api.services.UserManagementService(dal)
    val bulkImportService = BulkImportOrchestrationService(dal)

    // AI Services
    val geminiApiClient = GeminiApiClient(env.geminiApiKey ?: "")
    val transliterationService = TransliterationService(geminiApiClient)
    val webScrapingService = WebScrapingService(geminiApiClient)
    val entityResolutionService = com.sangita.grantha.backend.api.services.EntityResolutionService(dal)
    val bulkImportWorkerService = BulkImportWorkerService(
        dal = dal,
        importService = importService,
        webScrapingService = webScrapingService,
        entityResolutionService = entityResolutionService
    )

    embeddedServer(Netty, host = env.host, port = env.port) {
        configureSerialization()
        configureRequestLogging()
        configureCors(env)
        configureSecurity(env)
        configureStatusPages()
        configureRouting(
            krithiService,
            notationService,
            referenceDataService,
            importService,
            auditLogService,
            dashboardService,
            transliterationService,
            webScrapingService,
            userManagementService,
            bulkImportService
        )

        // Start background workers (bulk import orchestration)
        bulkImportWorkerService.start()

        monitor.subscribe(ApplicationStopping) {
            logger.info("Shutting down, closing database pool")
            bulkImportWorkerService.stop()
            DatabaseFactory.close()
        }
    }.start(wait = true)
}
