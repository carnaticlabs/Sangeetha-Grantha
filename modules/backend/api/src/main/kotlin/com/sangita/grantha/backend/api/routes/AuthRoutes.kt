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

            // TRACK-112 (F3): roles come from the user's stored assignments, never from the
            // request. Previously the caller's `roles` list was copied verbatim into the JWT, so
            // the shared ADMIN_TOKEN was enough to mint an admin token for any user.
            val roles = userManagementService.getUserRoles(user.id).map { it.roleCode }
            val token = jwtConfig.generateToken(user.id, roles)
            call.respond(AuthTokenResponse(token = token, expiresInSeconds = jwtConfig.tokenTtlSeconds))
        }
    }
}

fun Route.authRefreshRoutes(jwtConfig: JwtConfig, userManagementService: UserManagementService) {
    route("/v1/auth") {
        post("/refresh") {
            val principal = call.principal<JWTPrincipal>()
                ?: return@post call.respondText("Missing JWT", status = HttpStatusCode.Unauthorized)
            val userId = principal.payload.getClaim("userId")?.asString()
                ?: return@post call.respondText("Missing userId", status = HttpStatusCode.BadRequest)

            // TRACK-112 (F3): re-read roles from storage rather than carrying the old token's
            // claim forward. Refreshing must not let a revoked role survive indefinitely by
            // repeatedly minting a new token from the previous one.
            val resolvedUserId = userId.toKotlinUuidOrThrow("userId")
            val roles = userManagementService.getUserRoles(resolvedUserId).map { it.roleCode }

            val token = jwtConfig.generateToken(resolvedUserId, roles)
            call.respond(AuthTokenResponse(token = token, expiresInSeconds = jwtConfig.tokenTtlSeconds))
        }
    }
}
