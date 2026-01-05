package com.sangita.grantha.backend.api.models

import kotlinx.serialization.Serializable

@Serializable
data class UserCreateRequest(
    val email: String? = null,
    val fullName: String,
    val displayName: String? = null,
    val password: String? = null, // Optional if using external auth
    val isActive: Boolean = true,
    val roleCodes: List<String> = emptyList(), // Initial roles
)

@Serializable
data class UserUpdateRequest(
    val email: String? = null,
    val fullName: String? = null,
    val displayName: String? = null,
    val password: String? = null,
    val isActive: Boolean? = null,
)

@Serializable
data class AssignRoleRequest(
    val roleCode: String,
)

