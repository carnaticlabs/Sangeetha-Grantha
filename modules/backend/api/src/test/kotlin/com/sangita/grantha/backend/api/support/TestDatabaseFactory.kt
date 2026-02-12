package com.sangita.grantha.backend.api.support

import com.sangita.grantha.backend.dal.DatabaseFactory
import kotlinx.coroutines.runBlocking
import java.sql.DriverManager

/**
 * Migration-based test database factory.
 *
 * Uses the actual SQL migration files from `database/migrations/` to set up
 * the test database, guaranteeing schema parity with production.
 *
 * Prefer extending [IntegrationTestBase] for new test classes. This object
 * is kept for backward compatibility with tests that call it directly.
 */
object TestDatabaseFactory {
    private const val TEST_DB_NAME = "sangita_grantha_test"
    private const val ADMIN_URL = "jdbc:postgresql://localhost:5432/postgres"
    private const val TEST_DB_URL = "jdbc:postgresql://localhost:5432/$TEST_DB_NAME"
    private const val DB_USER = "postgres"
    private const val DB_PASSWORD = "postgres"

    /**
     * Ensures the test database exists, runs migrations, and connects Exposed.
     */
    fun connectTestDb() {
        ensureTestDatabaseExists()
        runMigrations()

        DatabaseFactory.connect(
            DatabaseFactory.ConnectionConfig(
                databaseUrl = TEST_DB_URL,
                username = DB_USER,
                password = DB_PASSWORD,
                driverClassName = "org.postgresql.Driver",
                enableQueryLogging = false,
            )
        )
    }

    /**
     * Truncates all user tables (fast) and closes the connection pool.
     */
    fun reset() {
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
            }
        }
    }

    private fun runMigrations() {
        DriverManager.getConnection(TEST_DB_URL, DB_USER, DB_PASSWORD).use { conn ->
            val migrations = MigrationRunner.loadMigrations()
            MigrationRunner.runMigrations(conn, migrations)
        }
    }
}
