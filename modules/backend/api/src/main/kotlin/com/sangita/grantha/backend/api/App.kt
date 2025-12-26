package com.sangita.grantha.backend.api

import com.sangita.grantha.backend.api.config.ApiEnvironmentLoader
import com.sangita.grantha.backend.api.plugins.configureCors
import com.sangita.grantha.backend.api.plugins.configureRequestLogging
import com.sangita.grantha.backend.api.plugins.configureRouting
import com.sangita.grantha.backend.api.plugins.configureSecurity
import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.AuditLogService
import com.sangita.grantha.backend.api.services.ImportService
import com.sangita.grantha.backend.api.services.KrithiService
import com.sangita.grantha.backend.api.services.ReferenceDataService
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("SangitaAPI")

fun main() {
    val env = ApiEnvironmentLoader.load()
    logger.info("Starting Sangita Grantha API (env: {})", env.environment)

    DatabaseFactory.connectFromExternal(env.databaseConfigPath)
    val dal = SangitaDal()
    val krithiService = KrithiService(dal)
    val referenceDataService = ReferenceDataService(dal)
    val importService = ImportService(dal)
    val auditLogService = AuditLogService(dal)

    embeddedServer(Netty, host = env.host, port = env.port) {
        configureSerialization()
        configureRequestLogging()
        configureCors(env)
        configureSecurity(env)
        configureStatusPages()
        configureRouting(krithiService, referenceDataService, importService, auditLogService)

        monitor.subscribe(ApplicationStopping) {
            logger.info("Shutting down, closing database pool")
            DatabaseFactory.close()
        }
    }.start(wait = true)
}
