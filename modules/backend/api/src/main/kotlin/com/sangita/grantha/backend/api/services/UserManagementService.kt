package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.UserCreateRequest
import com.sangita.grantha.backend.api.models.UserUpdateRequest
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.shared.domain.model.UserDto
import com.sangita.grantha.shared.domain.model.RoleAssignmentDto
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * Service for managing users and role assignments.
 */
class UserManagementService(private val dal: SangitaDal) {
    /**
     * List all users.
     */
    suspend fun listUsers(): List<UserDto> = dal.users.listAll()

    /**
     * Fetch a user by ID.
     */
    suspend fun getUser(id: Uuid): UserDto? = dal.users.findById(id)

    /**
     * Fetch a user by email (e.g. for login when admin UUID is not known).
     */
    suspend fun getUserByEmail(email: String): UserDto? {
        val id = dal.users.findByEmail(email) ?: return null
        return dal.users.findById(id.toKotlinUuid())
    }

    /**
     * Create a user and assign initial roles.
     */
    suspend fun createUser(request: UserCreateRequest): UserDto {
        // TODO: Implement password hashing (e.g., using bcrypt)
        val passwordHash = request.password?.let { hashPassword(it) }
        
        val created = dal.users.create(
            email = request.email,
            fullName = request.fullName,
            displayName = request.displayName,
            passwordHash = passwordHash,
            isActive = request.isActive
        )
        
        // Assign initial roles
        if (request.roleCodes.isNotEmpty()) {
            request.roleCodes.forEach { roleCode ->
                dal.users.assignRole(created.id, roleCode)
            }
        }
        
        dal.auditLogs.append(
            action = "CREATE_USER",
            entityTable = "users",
            entityId = created.id
        )
        
        return created
    }

    /**
     * Update an existing user.
     */
    suspend fun updateUser(id: Uuid, request: UserUpdateRequest): UserDto? {
        // TODO: Implement password hashing (e.g., using bcrypt)
        val passwordHash = request.password?.let { hashPassword(it) }
        
        val updated = dal.users.update(
            id = id,
            email = request.email,
            fullName = request.fullName,
            displayName = request.displayName,
            passwordHash = passwordHash,
            isActive = request.isActive
        )
        
        if (updated != null) {
            dal.auditLogs.append(
                action = "UPDATE_USER",
                entityTable = "users",
                entityId = updated.id
            )
        }
        
        return updated
    }

    /**
     * Delete a user by ID.
     */
    suspend fun deleteUser(id: Uuid): Boolean {
        val deleted = dal.users.delete(id)
        
        if (deleted) {
            dal.auditLogs.append(
                action = "DELETE_USER",
                entityTable = "users",
                entityId = id
            )
        }
        
        return deleted
    }

    /**
     * Assign a role to a user.
     */
    suspend fun assignRole(userId: Uuid, roleCode: String) {
        val assigned = dal.users.assignRole(userId, roleCode)
        if (assigned) {
            dal.auditLogs.append(
                action = "ASSIGN_ROLE",
                entityTable = "role_assignments",
                entityId = userId
            )
        }
    }

    /**
     * Remove a role assignment from a user.
     */
    suspend fun removeRole(userId: Uuid, roleCode: String): Boolean {
        val removed = dal.users.removeRole(userId, roleCode)
        if (removed) {
            dal.auditLogs.append(
                action = "REMOVE_ROLE",
                entityTable = "role_assignments",
                entityId = userId
            )
        }
        return removed
    }

    /**
     * Fetch role assignments for a user.
     */
    suspend fun getUserRoles(userId: Uuid): List<RoleAssignmentDto> = dal.users.getUserRoles(userId)

    private fun hashPassword(password: String): String {
        // TODO: Implement proper password hashing (e.g., bcrypt)
        // For now, return as-is (NOT SECURE - must be implemented before production)
        return password
    }
}
