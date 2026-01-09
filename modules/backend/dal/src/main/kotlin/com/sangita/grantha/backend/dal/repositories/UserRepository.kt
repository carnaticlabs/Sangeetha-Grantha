package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toUserDto
import com.sangita.grantha.backend.dal.models.toRoleAssignmentDto
import com.sangita.grantha.backend.dal.tables.UsersTable
import com.sangita.grantha.backend.dal.tables.RoleAssignmentsTable
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.shared.domain.model.UserDto
import com.sangita.grantha.shared.domain.model.RoleAssignmentDto
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid

class UserRepository {
    suspend fun findByEmail(email: String): UUID? = DatabaseFactory.dbQuery {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.get(UsersTable.id)?.value
    }

    suspend fun findById(id: Uuid): UserDto? = DatabaseFactory.dbQuery {
        UsersTable
            .selectAll()
            .where { UsersTable.id eq id.toJavaUuid() }
            .map { it.toUserDto() }
            .singleOrNull()
    }

    suspend fun listAll(): List<UserDto> = DatabaseFactory.dbQuery {
        UsersTable
            .selectAll()
            .orderBy(UsersTable.fullName to SortOrder.ASC)
            .map { it.toUserDto() }
    }

    suspend fun create(
        email: String? = null,
        fullName: String,
        displayName: String? = null,
        passwordHash: String? = null,
        isActive: Boolean = true
    ): UserDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val newId = UUID.randomUUID()

        UsersTable
            .insert {
                it[id] = newId
                it[UsersTable.email] = email
                it[UsersTable.fullName] = fullName
                it[UsersTable.displayName] = displayName
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.isActive] = isActive
                it[UsersTable.createdAt] = now
                it[UsersTable.updatedAt] = now
            }
            .resultedValues
            ?.single()
            ?.toUserDto()
            ?: error("Failed to insert user")
    }

    suspend fun update(
        id: Uuid,
        email: String? = null,
        fullName: String? = null,
        displayName: String? = null,
        passwordHash: String? = null,
        isActive: Boolean? = null
    ): UserDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaId = id.toJavaUuid()

        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        UsersTable
            .updateReturning(
                where = { UsersTable.id eq javaId }
            ) {
                email?.let { value -> it[UsersTable.email] = value }
                fullName?.let { value -> it[UsersTable.fullName] = value }
                displayName?.let { value -> it[UsersTable.displayName] = value }
                passwordHash?.let { value -> it[UsersTable.passwordHash] = value }
                isActive?.let { value -> it[UsersTable.isActive] = value }
                it[UsersTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toUserDto()
    }

    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = UsersTable.deleteWhere { UsersTable.id eq id.toJavaUuid() }
        deleted > 0
    }

    suspend fun getUserRoles(userId: Uuid): List<RoleAssignmentDto> = DatabaseFactory.dbQuery {
        RoleAssignmentsTable
            .selectAll()
            .where { RoleAssignmentsTable.userId eq userId.toJavaUuid() }
            .orderBy(RoleAssignmentsTable.assignedAt to SortOrder.ASC)
            .map { it.toRoleAssignmentDto() }
    }

    suspend fun assignRole(userId: Uuid, roleCode: String): Boolean = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaUserId = userId.toJavaUuid()
        
        // Check if assignment already exists
        val exists = RoleAssignmentsTable
            .selectAll()
            .where { 
                (RoleAssignmentsTable.userId eq javaUserId) and 
                (RoleAssignmentsTable.roleCode eq roleCode)
            }
            .singleOrNull()
        
        if (exists != null) {
            return@dbQuery false // Already assigned
        }
        
        RoleAssignmentsTable.insert {
            it[RoleAssignmentsTable.userId] = javaUserId
            it[RoleAssignmentsTable.roleCode] = roleCode
            it[RoleAssignmentsTable.assignedAt] = now
        }
        
        true
    }

    suspend fun removeRole(userId: Uuid, roleCode: String): Boolean = DatabaseFactory.dbQuery {
        val deleted = RoleAssignmentsTable.deleteWhere {
            (RoleAssignmentsTable.userId eq userId.toJavaUuid()) and
            (RoleAssignmentsTable.roleCode eq roleCode)
        }
        deleted > 0
    }
}
