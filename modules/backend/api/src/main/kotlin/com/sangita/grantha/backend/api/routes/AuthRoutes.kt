package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.config.JwtConfig
import com.sangita.grantha.backend.api.models.AuthTokenRequest
import com.sangita.grantha.backend.api.models.AuthTokenResponse
import com.sangita.grantha.backend.api.support.toKotlinUuidOrThrow
import com.sangita.grantha.backend.api.services.UserManagementService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(env: ApiEnvironment, jwtConfig: JwtConfig, userManagementService: UserManagementService) {
    route("/v1/auth") {
        post("/token") {
            val request = call.receive<AuthTokenRequest>()
            if (request.adminToken != env.adminToken) {
                return@post call.respondText("Invalid admin token", status = HttpStatusCode.Unauthorized)
            }

            if (request.email.isNullOrBlank() && request.userId.isNullOrBlank()) {
                return@post call.respondText("Provide email or userId", status = HttpStatusCode.BadRequest)
            }
            val user = when {
                !request.email.isNullOrBlank() -> userManagementService.getUserByEmail(request.email.trim())
                else -> userManagementService.getUser(request.userId!!.toKotlinUuidOrThrow("userId"))
            } ?: return@post call.respondText("User not found", status = HttpStatusCode.NotFound)

            val token = jwtConfig.generateToken(user.id, request.roles)
            call.respond(AuthTokenResponse(token = token, expiresInSeconds = jwtConfig.tokenTtlSeconds))
        }
    }
}

fun Route.authRefreshRoutes(jwtConfig: JwtConfig) {
    route("/v1/auth") {
        post("/refresh") {
            val principal = call.principal<JWTPrincipal>()
                ?: return@post call.respondText("Missing JWT", status = HttpStatusCode.Unauthorized)
            val userId = principal.payload.getClaim("userId")?.asString()
                ?: return@post call.respondText("Missing userId", status = HttpStatusCode.BadRequest)
            val roles = principal.payload.getClaim("roles")?.asList(String::class.java) ?: emptyList()

            val token = jwtConfig.generateToken(userId.toKotlinUuidOrThrow("userId"), roles)
            call.respond(AuthTokenResponse(token = token, expiresInSeconds = jwtConfig.tokenTtlSeconds))
        }
    }
}
