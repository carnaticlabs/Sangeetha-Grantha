package com.sangita.grantha.backend.api.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenRequest(
    val adminToken: String,
    /** User UUID; use when known (e.g. programmatic). */
    val userId: String? = null,
    /** User email; preferred for login so UUID need not be looked up after seed. */
    val email: String? = null,
    // TRACK-112 (F3): the `roles` field is gone deliberately. It used to be copied straight into
    // the issued JWT, so anyone holding the shared ADMIN_TOKEN could mint a token with any roles
    // for any user — privilege escalation by request body. Roles are now read from the user's
    // stored `role_assignments`. `ignoreUnknownKeys` is on, so a client still sending `roles` is
    // ignored rather than rejected; it simply has no effect.
)

@Serializable
data class AuthTokenResponse(
    val token: String,
    val expiresInSeconds: Long
)
