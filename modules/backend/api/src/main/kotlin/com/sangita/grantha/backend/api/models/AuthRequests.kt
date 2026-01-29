package com.sangita.grantha.backend.api.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenRequest(
    val adminToken: String,
    /** User UUID; use when known (e.g. programmatic). */
    val userId: String? = null,
    /** User email; preferred for login so UUID need not be looked up after seed. */
    val email: String? = null,
    val roles: List<String> = emptyList()
)

@Serializable
data class AuthTokenResponse(
    val token: String,
    val expiresInSeconds: Long
)
