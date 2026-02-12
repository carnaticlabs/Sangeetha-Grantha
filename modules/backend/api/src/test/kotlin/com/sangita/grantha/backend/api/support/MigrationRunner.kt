package com.sangita.grantha.backend.api.support

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * Reads and executes SQL migration files from database/migrations/ for integration tests.
 *
 * This ensures the test database schema exactly matches production — no manual enum
 * creation or SchemaUtils.create() needed. Migration files use dbmate-style markers
 * (`-- migrate:up` / `-- migrate:down`).
 */
object MigrationRunner {
    private val logger = LoggerFactory.getLogger(MigrationRunner::class.java)

    private const val MIGRATIONS_DIR = "database/migrations"
    private const val MIGRATE_UP_MARKER = "-- migrate:up"
    private const val MIGRATE_DOWN_MARKER = "-- migrate:down"

    data class Migration(
        val filename: String,
        val version: Int,
        val upSql: String,
    )

    /**
     * Reads all migration files from the project root, parses the up-section, returns sorted.
     */
    fun loadMigrations(projectRoot: Path = Paths.get(System.getProperty("user.dir"))): List<Migration> {
        val migrationsPath = projectRoot.resolve(MIGRATIONS_DIR)
        require(Files.exists(migrationsPath)) {
            "Migrations directory not found at $migrationsPath (working dir: ${System.getProperty("user.dir")})"
        }

        return Files.list(migrationsPath)
            .filter { it.name.endsWith(".sql") }
            .map { path ->
                val filename = path.name
                val version = extractVersionNumber(filename)
                val content = path.readText()
                val upSql = extractUpSection(content)
                Migration(filename, version, upSql)
            }
            .sorted(compareBy { it.version })
            .toList()
    }

    /**
     * Executes pending migrations against the given JDBC connection.
     * Tracks applied migrations in `_sqlx_migrations` to avoid re-running.
     */
    fun runMigrations(connection: Connection, migrations: List<Migration>) {
        connection.autoCommit = false

        // Create tracking table (compatible with Rust CLI's sqlx tracker)
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS _sqlx_migrations (
                    version BIGINT PRIMARY KEY,
                    description TEXT NOT NULL,
                    installed_on TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    success BOOLEAN NOT NULL,
                    checksum BYTEA NOT NULL DEFAULT E'\\x00',
                    execution_time BIGINT NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
        connection.commit()

        // Find already-applied versions
        val appliedVersions = connection.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT version FROM _sqlx_migrations WHERE success = true")
            buildSet {
                while (rs.next()) add(rs.getLong("version"))
            }
        }

        // Run pending migrations
        var applied = 0
        for (migration in migrations) {
            if (migration.version.toLong() in appliedVersions) continue

            try {
                val startMs = System.currentTimeMillis()
                connection.createStatement().use { stmt -> stmt.execute(migration.upSql) }
                val elapsedMs = System.currentTimeMillis() - startMs

                connection.prepareStatement(
                    """
                    INSERT INTO _sqlx_migrations (version, description, success, execution_time)
                    VALUES (?, ?, true, ?)
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setLong(1, migration.version.toLong())
                    stmt.setString(2, migration.filename)
                    stmt.setLong(3, elapsedMs)
                    stmt.execute()
                }

                connection.commit()
                applied++
            } catch (e: Exception) {
                connection.rollback()
                throw RuntimeException("Migration ${migration.filename} failed: ${e.message}", e)
            }
        }

        if (applied > 0) {
            logger.info("Applied $applied migration(s) to test database")
        }
    }

    /**
     * Extracts version number from filename like "01__baseline.sql" -> 1
     */
    private fun extractVersionNumber(filename: String): Int {
        val prefix = filename.substringBefore("__")
        return prefix.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid migration filename: $filename (expected NN__description.sql)")
    }

    /**
     * Extracts the up-section SQL from a migration file. Handles three formats:
     *  1. Both markers present: content between `-- migrate:up` and `-- migrate:down`
     *  2. Only `-- migrate:down` present: everything before it
     *  3. No markers: entire file content
     */
    private fun extractUpSection(content: String): String {
        val upIndex = content.indexOf(MIGRATE_UP_MARKER)
        val downIndex = content.indexOf(MIGRATE_DOWN_MARKER)

        return when {
            // Both markers: extract between them
            upIndex != -1 && downIndex != -1 && downIndex > upIndex ->
                content.substring(upIndex + MIGRATE_UP_MARKER.length, downIndex).trim()

            // Only up marker (no down): everything after up marker
            upIndex != -1 ->
                content.substring(upIndex + MIGRATE_UP_MARKER.length).trim()

            // Only down marker (no up): everything before down marker
            downIndex != -1 ->
                content.substring(0, downIndex).trim()

            // No markers: use entire file
            else -> content.trim()
        }
    }
}
