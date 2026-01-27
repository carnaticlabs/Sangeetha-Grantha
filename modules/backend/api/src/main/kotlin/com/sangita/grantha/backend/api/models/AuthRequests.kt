package com.sangita.grantha.backend.api.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenRequest(
    val adminToken: String,
    val userId: String,
    val roles: List<String> = emptyList()
)

@Serializable
data class AuthTokenResponse(
    val token: String,
    val expiresInSeconds: Long
)
