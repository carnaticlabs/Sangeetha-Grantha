package com.sangita.grantha.backend.api.config

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import kotlin.uuid.Uuid

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val tokenTtlSeconds: Long
) {
    fun verifier(): JWTVerifier = JWT.require(Algorithm.HMAC256(secret))
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateToken(userId: Uuid, roles: List<String> = emptyList()): String {
        val now = System.currentTimeMillis()
        val expiresAt = Date(now + tokenTtlSeconds * 1000)
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId.toString())
            .withClaim("roles", roles)
            .withExpiresAt(expiresAt)
            .sign(Algorithm.HMAC256(secret))
    }

    companion object {
        fun fromEnvironment(env: ApiEnvironment): JwtConfig = JwtConfig(
            secret = env.jwtSecret,
            issuer = env.jwtIssuer,
            audience = env.jwtAudience,
            realm = env.jwtRealm,
            tokenTtlSeconds = env.tokenTtlSeconds
        )
    }
}
