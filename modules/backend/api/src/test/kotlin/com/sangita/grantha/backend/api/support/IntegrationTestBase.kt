package com.sangita.grantha.backend.api.support

import com.sangita.grantha.backend.dal.DatabaseFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base class for integration tests that need a real PostgreSQL database.
 *
 * The database is set up once per test suite using the actual SQL migrations
 * from `database/migrations/`, guaranteeing schema parity with production.
 * Between tests, all tables are truncated (fast) rather than dropped/recreated.
 *
 * Usage:
 * ```kotlin
 * class MyServiceTest : IntegrationTestBase() {
 *     private lateinit var dal: SangitaDal
 *
 *     @BeforeEach
 *     fun setup() {
 *         dal = SangitaDalImpl()
 *         // ... create services
 *     }
 *
 *     @Test
 *     fun `my test`() = runTest { ... }
 * }
 * ```
 */
abstract class IntegrationTestBase {
    companion object {
        private val logger = LoggerFactory.getLogger(IntegrationTestBase::class.java)

        private const val TEST_DB_NAME = "sangita_grantha_test"
        private const val ADMIN_URL = "jdbc:postgresql://localhost:5432/postgres"
        private const val TEST_DB_URL = "jdbc:postgresql://localhost:5432/$TEST_DB_NAME"
        private const val DB_USER = "postgres"
        private const val DB_PASSWORD = "postgres"

        private val initialized = AtomicBoolean(false)

        @JvmStatic
        @BeforeAll
        fun initializeDatabase() {
            if (!initialized.compareAndSet(false, true)) {
                // Already initialized by another test class in this JVM — just reconnect Exposed
                connectExposed()
                return
            }

            ensureTestDatabaseExists()
            runMigrations()
            connectExposed()
            logger.info("Integration test database initialized")
        }

        @JvmStatic
        @AfterAll
        fun shutdownDatabase() {
            DatabaseFactory.close()
        }

        private fun ensureTestDatabaseExists() {
            DriverManager.getConnection(ADMIN_URL, DB_USER, DB_PASSWORD).use { conn ->
                conn.autoCommit = true
                val exists = conn.prepareStatement(
                    "SELECT 1 FROM pg_database WHERE datname = ?"
                ).use { stmt ->
                    stmt.setString(1, TEST_DB_NAME)
                    stmt.executeQuery().next()
                }
                if (!exists) {
                    conn.createStatement().use { it.execute("CREATE DATABASE $TEST_DB_NAME") }
                    logger.info("Created test database: $TEST_DB_NAME")
                }
            }
        }

        private fun runMigrations() {
            DriverManager.getConnection(TEST_DB_URL, DB_USER, DB_PASSWORD).use { conn ->
                val migrations = MigrationRunner.loadMigrations()
                MigrationRunner.runMigrations(conn, migrations)
            }
        }

        private fun connectExposed() {
            DatabaseFactory.connect(
                DatabaseFactory.ConnectionConfig(
                    databaseUrl = TEST_DB_URL,
                    username = DB_USER,
                    password = DB_PASSWORD,
                    driverClassName = "org.postgresql.Driver",
                    enableQueryLogging = false,
                    maxPoolSize = 5,
                )
            )
        }
    }

    /**
     * Truncate all user tables between tests. Much faster than drop/recreate.
     */
    @AfterEach
    fun truncateAllTables() {
        runBlocking {
            DatabaseFactory.dbQuery {
                val tables = exec(
                    """
                    SELECT tablename FROM pg_tables
                    WHERE schemaname = 'public'
                      AND tablename NOT IN ('_sqlx_migrations')
                    """.trimIndent()
                ) { rs ->
                    buildList { while (rs.next()) add(rs.getString(1)) }
                } ?: emptyList()

                if (tables.isNotEmpty()) {
                    exec("SET session_replication_role = replica")
                    exec("TRUNCATE TABLE ${tables.joinToString(", ")} CASCADE")
                    exec("SET session_replication_role = DEFAULT")
                }
            }
        }
    }
}
