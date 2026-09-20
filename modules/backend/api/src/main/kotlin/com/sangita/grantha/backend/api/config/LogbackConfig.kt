package com.sangita.grantha.backend.api.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import java.nio.file.Files
import java.nio.file.Path
import net.logstash.logback.encoder.LogstashEncoder
import org.slf4j.LoggerFactory

object LogbackConfig {

    fun configure(env: ApiEnvironment) {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        
        // Reset context to remove default configuration
        context.reset()

        val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
        
        // Configure logging level based on environment
        val rootLevel = when (env.environment) {
            Environment.DEV -> Level.DEBUG
            Environment.TEST, Environment.PROD -> Level.INFO
        }
        rootLogger.level = rootLevel

        val encoder = when (env.environment) {
            Environment.PROD -> LogstashEncoder().apply {
                this.context = context
                start()
            }
            else -> PatternLayoutEncoder().apply {
                this.context = context
                // %d{ISO8601} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg%n
                pattern = "%d{ISO8601} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg%n"
                start()
            }
        }

        // Create and configure the console appender
        val consoleAppender = ConsoleAppender<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
            this.context = context
            name = "CONSOLE"
            this.encoder = encoder
            start()
        }

        // Attach appender to root logger
        rootLogger.addAppender(consoleAppender)
        
        // Create a dedicated encoder for the file appender.
        // Each Logback appender MUST have its own encoder instance — sharing an
        // encoder between appenders causes the OutputStream to be stolen by the
        // last appender that starts, silently breaking the other one.
        val fileEncoder = when (env.environment) {
            Environment.PROD -> LogstashEncoder().apply {
                this.context = context
                start()
            }
            else -> PatternLayoutEncoder().apply {
                this.context = context
                pattern = "%d{ISO8601} %-5level [%thread] %logger{36} - %msg%n"
                start()
            }
        }

        // Create and configure the rolling file appender for Exposed
        val fileAppender = ch.qos.logback.core.rolling.RollingFileAppender<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
            this.context = context
            name = "EXPOSED_FILE"
            file = "exposed_queries.log"
            // Use setter explicitly to avoid ambiguity with protected field
            setAppend(true)
            
            val parentAppender = this
            
            val rollingPolicy = ch.qos.logback.core.rolling.TimeBasedRollingPolicy<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
                this.context = context
                setParent(parentAppender)
                fileNamePattern = "exposed_queries.%d{yyyy-MM-dd}.log"
                maxHistory = 30 // Keep 30 days of history
                // Use setter explicitly
                setTotalSizeCap(ch.qos.logback.core.util.FileSize.valueOf("1GB")) 
                start()
            }
            this.rollingPolicy = rollingPolicy
            
            this.encoder = fileEncoder
            start()
        }

        // Configure Exposed logger to write to file and not console
        // Note: Exposed framework often uses the logger name "Exposed" directly
        for (loggerName in listOf("org.jetbrains.exposed", "Exposed")) {
            val logger = context.getLogger(loggerName)
            logger.level = Level.INFO
            logger.isAdditive = false
            logger.addAppender(fileAppender)
        }
        
        // Example: Reduce noise from external libraries if running in DEBUG
        if (rootLevel == Level.DEBUG) {
            // Less chatty Netty/Exposed if needed, or keep them debug
            context.getLogger("io.netty").level = Level.INFO
            context.getLogger("com.zaxxer.hikari").level = Level.INFO 
            context.getLogger("io.ktor.server.Application").level = Level.INFO 
        }
        
        attachCatalogueUsageFileAppender(context, env)

        // Internal logger for this config
        val selfLogger = context.getLogger(LogbackConfig::class.java)
        selfLogger.info("Logback configured programmatically for environment: ${env.environment}, Level: $rootLevel")
    }

    /**
     * TRACK-138: catalogue-usage is a dedicated non-additive JSONL logger.
     * Rotation is 10 MiB / 7 days / 100 MiB total. This is operational retention,
     * not a cryptographic deletion guarantee. Startup or rollover failures must
     * not prevent the API from serving catalogue reads.
     */
    internal fun attachCatalogueUsageFileAppender(context: LoggerContext, env: ApiEnvironment) {
        val usageLogger = context.getLogger("catalogue-usage")
        usageLogger.level = Level.INFO
        try {
            val directory = Path.of(env.catalogueUsageDirectory)
            Files.createDirectories(directory)
            val encoder = PatternLayoutEncoder().apply {
                this.context = context
                pattern = "%msg%n"
                start()
            }
            val fileAppender = RollingFileAppender<ILoggingEvent>().apply {
                this.context = context
                name = "CATALOGUE_USAGE_FILE"
                file = directory.resolve("catalogue-usage.jsonl").toString()
                setAppend(true)
                val parentAppender = this
                rollingPolicy = SizeAndTimeBasedRollingPolicy<ILoggingEvent>().apply {
                    this.context = context
                    setParent(parentAppender)
                    fileNamePattern = directory.resolve("catalogue-usage.%d{yyyy-MM-dd}.%i.jsonl").toString()
                    setMaxFileSize(FileSize.valueOf("10MB"))
                    maxHistory = 7
                    setTotalSizeCap(FileSize.valueOf("100MB"))
                    start()
                }
                this.encoder = encoder
                start()
            }
            usageLogger.isAdditive = false
            usageLogger.addAppender(fileAppender)
        } catch (ex: Exception) {
            usageLogger.isAdditive = true
            usageLogger.warn(
                "Catalogue usage JSONL file appender unavailable; events stay on the console: {}",
                ex.message,
            )
        }
    }
}
