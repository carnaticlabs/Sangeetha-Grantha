package com.sangita.grantha.backend.api.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.spi.ContextAwareBase
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
            
            this.encoder = encoder
            start()
        }

        // Configure Exposed logger to write to file and not console
        // Note: Exposed framework often uses the logger name "Exposed" directly
        for (loggerName in listOf("org.jetbrains.exposed", "Exposed")) {
            val logger = context.getLogger(loggerName)
            logger.level = Level.DEBUG
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
        
        // Internal logger for this config
        val selfLogger = context.getLogger(LogbackConfig::class.java)
        selfLogger.info("Logback configured programmatically for environment: ${env.environment}, Level: $rootLevel")
    }
}
