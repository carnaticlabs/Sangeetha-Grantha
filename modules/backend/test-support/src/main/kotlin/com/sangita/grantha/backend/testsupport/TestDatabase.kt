package com.sangita.grantha.backend.testsupport

import java.nio.file.Paths
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

/**
 * Resolves the integration-test database connection and migrates its schema with Flyway
 * (TRACK-110 Sub-part B; shared in TRACK-111) — replacing the 156-line hand-rolled `MigrationRunner`.
 *
 * Default: a Testcontainers Postgres ([SangitaPostgres]).
 * Escape hatch: if `TEST_DATABASE_URL` is set, that external database is used instead (e.g. a CI
 * service container), with credentials from `TEST_DATABASE_USER` / `TEST_DATABASE_PASSWORD`
 * (default postgres/postgres). The container is then never started.
 *
 * Migration is **schema-only**: Flyway applies the `V__` versioned migrations but not the `R__`
 * repeatable reference data. Tests build their own fixtures ([TestFixtures]); seeding reference
 * data here would collide with those fixtures (e.g. composer "Tyagaraja"). Schema-only exactly
 * preserves the prior `MigrationRunner` behaviour.
 */
object TestDatabase {
    private val logger = LoggerFactory.getLogger(TestDatabase::class.java)

    data class Connection(val jdbcUrl: String, val username: String, val password: String)

    /** Connection to a schema-migrated test database. Container start + migration happen once. */
    val connection: Connection by lazy {
        val external = System.getenv("TEST_DATABASE_URL")?.takeIf { it.isNotBlank() }
        val conn = if (external != null) {
            logger.info("Using external test database from TEST_DATABASE_URL")
            Connection(
                jdbcUrl = external,
                username = System.getenv("TEST_DATABASE_USER")?.takeIf { it.isNotBlank() } ?: "postgres",
                password = System.getenv("TEST_DATABASE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "postgres",
            )
        } else {
            val c = SangitaPostgres.container
            Connection(jdbcUrl = c.jdbcUrl, username = c.username, password = c.password)
        }
        migrateSchema(conn)
        conn
    }

    private fun migrateSchema(conn: Connection) {
        val migrationsDir = Paths.get(System.getProperty("user.dir")).resolve("database/migrations")
        Flyway.configure()
            .dataSource(conn.jdbcUrl, conn.username, conn.password)
            .locations("filesystem:$migrationsDir")
            // Schema-only: disable the repeatable prefix so the R__ reference-data files are not
            // recognised as migrations and are skipped (tests seed their own reference data).
            .repeatableSqlMigrationPrefix("RDISABLED")
            .load()
            .migrate()
    }
}
