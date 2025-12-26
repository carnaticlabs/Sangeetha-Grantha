package com.sangita.grantha.backend.dal

import com.sangita.grantha.backend.dal.support.DatabaseConfig
import com.sangita.grantha.backend.dal.support.DatabaseConfigLoader
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

/**
 * Central Exposed + Hikari configuration.
 *
 * All database access in the backend should go through [dbQuery].
 */
object DatabaseFactory {
    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val dataSourceRef = AtomicReference<HikariDataSource?>()

    fun connectFromExternal(configPath: Path? = null, env: Map<String, String> = System.getenv()) {
        val config = DatabaseConfigLoader.load(configPath, env)
        connect(config)
    }

    fun connect(config: DatabaseConfig) = connect(
        databaseUrl = config.jdbcUrl,
        username = config.username,
        password = config.password,
        schema = config.schema
    )

    fun connect(
        databaseUrl: String,
        username: String,
        password: String,
        maxPoolSize: Int = 10,
        minIdle: Int = maxOf(2, maxPoolSize / 2),
        schema: String? = null,
        connectionTimeoutMillis: Long = 10_000,
        idleTimeoutMillis: Long = 600_000,
        maxLifetimeMillis: Long = 1_800_000,
    ) {
        val config = HikariConfig().apply {
            jdbcUrl = databaseUrl
            this.username = username
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = maxPoolSize
            minimumIdle = minOf(minIdle, maxPoolSize)
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            connectionTimeout = connectionTimeoutMillis
            idleTimeout = idleTimeoutMillis
            maxLifetime = maxLifetimeMillis
            validationTimeout = connectionTimeoutMillis / 2L
            schema?.let { setSchema(it) }
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("stringtype", "unspecified")
            validate()
        }

        val dataSource = HikariDataSource(config)
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
        newSuspendedTransaction(context = dispatcher, statement = block)

    fun close() {
        dataSourceRef.getAndSet(null)?.close()
    }
}
