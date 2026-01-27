package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.config.JwtConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity(env: ApiEnvironment) {
    val jwtConfig = JwtConfig.fromEnvironment(env)
    install(Authentication) {
        jwt("admin-auth") {
            realm = jwtConfig.realm
            verifier(jwtConfig.verifier())
            validate { credential ->
                val audienceValid = credential.payload.audience.contains(jwtConfig.audience)
                val userId = credential.payload.getClaim("userId")?.asString()
                if (audienceValid && !userId.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
