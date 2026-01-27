package com.sangita.grantha.backend.dal

import com.sangita.grantha.backend.dal.support.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.MeterRegistry
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.Slf4jSqlDebugLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

/**
 * Central Exposed + Hikari configuration.
 *
 * All database access in the backend should go through [dbQuery].
 */
object DatabaseFactory {
    data class ConnectionConfig(
        val databaseUrl: String,
        val username: String,
        val password: String,
        val maxPoolSize: Int = 10,
        val minIdle: Int = maxOf(2, maxPoolSize / 2),
        val schema: String? = null,
        val connectionTimeoutMillis: Long = 10_000,
        val idleTimeoutMillis: Long = 600_000,
        val maxLifetimeMillis: Long = 1_800_000,
        val driverClassName: String? = null,
        val meterRegistry: MeterRegistry? = null,
        val enableQueryLogging: Boolean = false,
        val slowQueryThresholdMs: Long = 100
    )

    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val dataSourceRef = AtomicReference<HikariDataSource?>()
    private var queryLoggingEnabled: Boolean = false
    private var slowQueryThresholdMs: Long = 100

    /**
     * Connects using the provided DatabaseConfig.
     */
    fun connect(config: DatabaseConfig) = connect(
        ConnectionConfig(
            databaseUrl = config.jdbcUrl,
            username = config.username,
            password = config.password,
            schema = config.schema
        )
    )

    fun connect(config: ConnectionConfig) {
        queryLoggingEnabled = config.enableQueryLogging
        slowQueryThresholdMs = config.slowQueryThresholdMs
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.databaseUrl
            this.username = config.username
            this.password = config.password
            this.driverClassName = config.driverClassName ?: when {
                jdbcUrl.startsWith("jdbc:h2:") -> "org.h2.Driver"
                else -> "org.postgresql.Driver"
            }
            maximumPoolSize = config.maxPoolSize
            minimumIdle = minOf(config.minIdle, config.maxPoolSize)
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            connectionTimeout = config.connectionTimeoutMillis
            idleTimeout = config.idleTimeoutMillis
            maxLifetime = config.maxLifetimeMillis
            validationTimeout = config.connectionTimeoutMillis / 2L
            config.schema?.let { setSchema(it) }
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("stringtype", "unspecified")
            config.meterRegistry?.let { registry ->
                metricsTrackerFactory = com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory(registry)
            }
            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)
        val previous = dataSourceRef.getAndSet(dataSource)
        previous?.close()
        Database.connect(dataSource)
    }

    fun setDispatcher(customDispatcher: CoroutineDispatcher) {
        dispatcher = customDispatcher
    }

    // Exposed currently deprecates this helper in favour of the R2DBC-only suspendTransaction API.
    // Until an equivalent becomes available for JDBC, keep using it and suppress the warning.
    @Suppress("DEPRECATION")
    suspend fun <T> dbQuery(block: suspend JdbcTransaction.() -> T): T =
        newSuspendedTransaction(context = dispatcher) {
            if (com.sangita.grantha.backend.dal.support.QueryCounter.isActive()) {
                addLogger(com.sangita.grantha.backend.dal.support.QueryCounterLogger)
            }
            if (queryLoggingEnabled) {
                org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.current().warnLongQueriesDuration = slowQueryThresholdMs
                addLogger(Slf4jSqlDebugLogger)
            }
            block()
        }

    fun close() {
        dataSourceRef.getAndSet(null)?.close()
    }
}
