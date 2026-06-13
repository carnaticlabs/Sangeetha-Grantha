package com.sangita.grantha.backend.api.support

import com.sangita.grantha.backend.dal.DatabaseFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.slf4j.LoggerFactory

/**
 * Base class for integration tests that need a real PostgreSQL database.
 *
 * The database is a self-provisioning Testcontainers Postgres ([TestDatabase] / [SangitaPostgres]),
 * schema-migrated once per JVM via the Flyway JVM API — guaranteeing schema parity with production
 * with no `localhost:5432` dependency. Between tests, all tables are truncated (fast) rather than
 * dropped/recreated; `flyway_schema_history` is preserved.
 *
 * Tagged `integration`: run the whole suite with `make test` (or `./gradlew check`), or just the
 * tagged set with `make test-integration` (`./gradlew :modules:backend:api:integrationTest`).
 *
 * Usage:
 * ```kotlin
 * class MyServiceTest : IntegrationTestBase() {
 *     @Test fun `my test`() = runTest { ... }
 * }
 * ```
 */
@Tag("integration")
abstract class IntegrationTestBase {
    companion object {
        private val logger = LoggerFactory.getLogger(IntegrationTestBase::class.java)

        @JvmStatic
        @BeforeAll
        fun initializeDatabase() {
            // Lazily starts the container and migrates the schema once per JVM; cheap on reuse.
            val conn = TestDatabase.connection
            DatabaseFactory.connect(
                DatabaseFactory.ConnectionConfig(
                    databaseUrl = conn.jdbcUrl,
                    username = conn.username,
                    password = conn.password,
                    driverClassName = "org.postgresql.Driver",
                    enableQueryLogging = false,
                    maxPoolSize = 5,
                )
            )
            logger.info("Integration test database connected")
        }

        @JvmStatic
        @AfterAll
        fun shutdownDatabase() {
            DatabaseFactory.close()
        }
    }

    /**
     * Truncate all user tables between tests. Much faster than drop/recreate.
     * Excludes `flyway_schema_history` so the migration record survives.
     */
    @AfterEach
    fun truncateAllTables() {
        runBlocking {
            DatabaseFactory.dbQuery {
                val tables = exec(
                    """
                    SELECT tablename FROM pg_tables
                    WHERE schemaname = 'public'
                      AND tablename NOT IN ('flyway_schema_history')
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
