package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.backend.api.config.ApiEnvironment
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer

fun Application.configureSecurity(env: ApiEnvironment) {
    install(Authentication) {
        bearer("admin-auth") {
            authenticate { credentials ->
                if (credentials.token == env.adminToken) {
                    UserIdPrincipal("admin")
                } else {
                    null
                }
            }
        }
    }
}
