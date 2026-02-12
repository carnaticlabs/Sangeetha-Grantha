package com.sangita.grantha.backend.api

import com.sangita.grantha.backend.api.config.ApiEnvironmentLoader
import com.sangita.grantha.backend.api.config.LogbackConfig
import com.sangita.grantha.backend.api.di.appModule
import com.sangita.grantha.backend.api.di.dalModule
import com.sangita.grantha.backend.api.plugins.configureCaching
import com.sangita.grantha.backend.api.plugins.configureMetrics
import com.sangita.grantha.backend.api.plugins.configureRequestValidation
import com.sangita.grantha.backend.api.plugins.configureCors
import com.sangita.grantha.backend.api.plugins.configureRequestLogging
import com.sangita.grantha.backend.api.plugins.configureRouting
import com.sangita.grantha.backend.api.plugins.configureSecurity
import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.bulkimport.IBulkImportWorker
import com.sangita.grantha.backend.dal.DatabaseFactory

import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("SangitaAPI")

fun main() {
    val env = ApiEnvironmentLoader.load()
    LogbackConfig.configure(env)
    logger.info("Starting Sangita Grantha API (env: {})", env.environment)
    
    if (env.database == null) {
        throw IllegalStateException("Database configuration is missing")
    }
    val metricsRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    embeddedServer(Netty, host = env.host, port = env.port) {
        configureMetrics(metricsRegistry)

        val dbConfig = env.database
        DatabaseFactory.connect(
            DatabaseFactory.ConnectionConfig(
                databaseUrl = dbConfig.jdbcUrl,
                username = dbConfig.username,
                password = dbConfig.password,
                schema = dbConfig.schema,
                meterRegistry = metricsRegistry,
                enableQueryLogging = env.environment != com.sangita.grantha.backend.api.config.Environment.PROD,
                slowQueryThresholdMs = 100
            )
        )

        install(Koin) {
            modules(dalModule, appModule(env, metricsRegistry))
        }

        configureSerialization()
        configureRequestLogging()
        configureCors(env)
        configureSecurity(env)
        configureStatusPages()
        configureRequestValidation()
        configureCaching()
        configureRouting()

        // Start background workers (bulk import orchestration)
        val bulkImportWorkerService by inject<IBulkImportWorker>()
        val extractionWorker by inject<com.sangita.grantha.backend.api.services.IExtractionWorker>()
        
        bulkImportWorkerService.start()
        extractionWorker.start()

        monitor.subscribe(ApplicationStopping) {
            logger.info("Shutting down, closing database pool")
            bulkImportWorkerService.stop()
            extractionWorker.stop()
            DatabaseFactory.close()
        }
    }.start(wait = true)
}
