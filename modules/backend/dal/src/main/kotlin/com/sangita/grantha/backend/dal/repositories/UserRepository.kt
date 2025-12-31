package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory.dbQuery
import com.sangita.grantha.backend.dal.tables.UsersTable
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class UserRepository {
    suspend fun findByEmail(email: String): UUID? = dbQuery {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.get(UsersTable.id)?.value
    }

    suspend fun create(email: String, fullName: String): UUID = dbQuery {
        val newId = UUID.randomUUID()
        UsersTable.insert {
            it[id] = newId
            it[UsersTable.email] = email
            it[UsersTable.fullName] = fullName
            it[isActive] = true
            it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        newId
    }
}
