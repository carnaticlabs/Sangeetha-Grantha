package com.sangita.grantha.backend.api.tools

import com.sangita.grantha.backend.api.config.ApiEnvironmentLoader
import com.sangita.grantha.backend.api.support.PasswordHasher
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.tables.RoleAssignmentsTable
import com.sangita.grantha.backend.dal.tables.RolesTable
import com.sangita.grantha.backend.dal.tables.UsersTable
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.slf4j.LoggerFactory

/**
 * First-run admin provisioning (TRACK-110, Sub-part A).
 *
 * Reads ADMIN_EMAIL / ADMIN_PASSWORD from the environment and idempotently upserts the admin
 * `users` row with an **argon2id** password hash (via [PasswordHasher], the TRACK-114 helper),
 * then ensures the `grp_sangita_admin` role assignment exists.
 *
 * This is *environment data*, deliberately carved out of the `R__seed_01_reference.sql`
 * repeatable migration (ADR-013 seed tiering, D15): credentials never live in migrations or
 * seed SQL. DB connection params come from the same env the API uses (DB_HOST/DB_PORT/...),
 * resolved by [ApiEnvironmentLoader].
 *
 * Invoke via `make bootstrap-admin` (host) or the `bootstrap-admin` compose service.
 */
object BootstrapAdmin {
    private val logger = LoggerFactory.getLogger("BootstrapAdmin")

    private val ADMIN_ROLE = com.sangita.grantha.backend.api.support.Roles.ADMIN
    private const val DEFAULT_FULL_NAME = "System Admin"

    fun run(env: Map<String, String> = System.getenv()) {
        val email = env["ADMIN_EMAIL"]?.takeIf { it.isNotBlank() }
            ?: error("ADMIN_EMAIL is required (the admin login email).")
        val password = env["ADMIN_PASSWORD"]?.takeIf { it.isNotBlank() }
            ?: error("ADMIN_PASSWORD is required (will be argon2id-hashed; never stored in plaintext).")

        val dbConfig = ApiEnvironmentLoader.load(env).database
            ?: error("Database configuration is missing (DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD).")

        val passwordHash = PasswordHasher.hash(password)
        DatabaseFactory.connect(dbConfig)
        try {
            val outcome = runBlocking {
                DatabaseFactory.dbQuery {
                    val now = OffsetDateTime.now(ZoneOffset.UTC)

                    // The role DEFINITION is reference data (seeded by R__seed_01_reference.sql),
                    // but guard against bootstrap running before migrate so the FK is satisfiable.
                    val roleExists = RolesTable.selectAll()
                        .where { RolesTable.code eq ADMIN_ROLE }
                        .any()
                    require(roleExists) {
                        "Role '$ADMIN_ROLE' is absent — run migrations (make migrate) before bootstrap-admin."
                    }

                    val existing = UsersTable.selectAll()
                        .where { UsersTable.email eq email }
                        .singleOrNull()

                    val userId: UUID
                    val created: Boolean
                    if (existing == null) {
                        userId = UsersTable.insertAndGetId {
                            it[UsersTable.email] = email
                            it[fullName] = DEFAULT_FULL_NAME
                            it[UsersTable.passwordHash] = passwordHash
                            it[isActive] = true
                            it[createdAt] = now
                            it[updatedAt] = now
                        }.value
                        created = true
                    } else {
                        userId = existing[UsersTable.id].value
                        UsersTable.update({ UsersTable.email eq email }) {
                            it[UsersTable.passwordHash] = passwordHash
                            it[isActive] = true
                            it[updatedAt] = now
                        }
                        created = false
                    }

                    val hasRole = RoleAssignmentsTable.selectAll()
                        .where { (RoleAssignmentsTable.userId eq userId) and (RoleAssignmentsTable.roleCode eq ADMIN_ROLE) }
                        .any()
                    if (!hasRole) {
                        RoleAssignmentsTable.insert {
                            it[RoleAssignmentsTable.userId] = userId
                            it[roleCode] = ADMIN_ROLE
                            it[assignedAt] = now
                        }
                    }
                    if (created) "created" else "updated"
                }
            }
            // Never log the password or the hash.
            logger.info("Admin user {} ({}) with role {}.", email, outcome, ADMIN_ROLE)
        } finally {
            DatabaseFactory.close()
        }
    }
}

fun main() {
    BootstrapAdmin.run()
}
