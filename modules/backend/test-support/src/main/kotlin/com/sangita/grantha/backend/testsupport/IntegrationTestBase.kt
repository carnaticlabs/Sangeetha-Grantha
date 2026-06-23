package com.sangita.grantha.backend.testsupport

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
 * migrated once per JVM via the Flyway JVM API (full `V__`+`R__`, so the real reference seed is
 * present) — schema parity with production, no `localhost:5432` dependency. Between tests,
 * transactional tables are truncated and the reference tables are reset to their seed snapshot
 * (test-created rows removed, seeded rows untouched) — so the seed is read-only and the suite is
 * idempotent even against a persistent/external DB.
 *
 * Tagged `integration`: run the whole suite with `make test` (or `./gradlew check`), or just the
 * tagged set with `make test-integration` (`./gradlew :modules:backend:<module>:integrationTest`).
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

        /**
         * Tables left untouched by the per-test truncate: Flyway's bookkeeping plus the reference
         * data seeded by the `R__` repeatables (roles, composers, ragas, talas, deities, composer
         * aliases, import sources). Tests read these via `findOrCreate` and must not mutate them,
         * so they stay stable and identical for every test.
         */
        private val PRESERVED_TABLES = listOf(
            "flyway_schema_history",
            TestDatabase.SEED_SNAPSHOT_TABLE,
            // Reference tables never written by tests — kept as-is.
            "roles",
            "composer_aliases",
        )

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
     * Reset state between tests. Transactional tables are truncated; the reference tables are reset
     * to their seed snapshot — any rows a test inserted are deleted, the seeded rows are left
     * untouched. The seed therefore stays read-only and stable, and the suite is idempotent even
     * against a persistent/external database (it never leaves reference rows behind). FK checks are
     * disabled for the reset (`session_replication_role = replica`) so order is irrelevant.
     */
    @AfterEach
    fun resetState() {
        runBlocking {
            DatabaseFactory.dbQuery {
                // Tables left entirely alone (Flyway bookkeeping, the seed snapshot, and the
                // reference tables tests never write) + the reset tables (handled by delete below).
                val untouched = (PRESERVED_TABLES + TestDatabase.RESET_TABLES)
                    .joinToString(", ") { "'$it'" }
                val transactional = exec(
                    """
                    SELECT tablename FROM pg_tables
                    WHERE schemaname = 'public'
                      AND tablename NOT IN ($untouched)
                    """.trimIndent()
                ) { rs ->
                    buildList { while (rs.next()) add(rs.getString(1)) }
                } ?: emptyList()

                exec("SET session_replication_role = replica")
                if (transactional.isNotEmpty()) {
                    exec("TRUNCATE TABLE ${transactional.joinToString(", ")} CASCADE")
                }
                // Restore each reference table to exactly the seed: drop only test-created rows.
                for (table in TestDatabase.RESET_TABLES) {
                    exec(
                        "DELETE FROM $table WHERE id::text NOT IN " +
                            "(SELECT pk FROM ${TestDatabase.SEED_SNAPSHOT_TABLE} WHERE tbl = '$table')"
                    )
                }
                exec("SET session_replication_role = DEFAULT")
            }
        }
    }
}
