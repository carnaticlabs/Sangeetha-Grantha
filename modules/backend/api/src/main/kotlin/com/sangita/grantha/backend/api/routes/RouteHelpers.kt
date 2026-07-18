package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.plugins.ErrorResponse
import com.sangita.grantha.backend.api.support.toKotlinUuidOrNull
import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import kotlin.uuid.Uuid

/** Transparent selector: contributes nothing to path matching, only wraps a pipeline. */
private object AuthorizationRouteSelector : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Transparent

    override fun toString(): String = "(authorize)"
}

/**
 * Require the caller's JWT to carry at least one of [roleCodes], else respond 403.
 *
 * TRACK-112 (F3): `authenticate("admin-auth")` proves only *who* the caller is — it validates the
 * signature, audience and `userId` claim and nothing else. Before this existed no route checked
 * roles at all, so any validly-signed token, including one with no roles, reached every admin
 * route. There was no 403 tier.
 *
 * Roles are read from the token claim, which is trustworthy because `/v1/auth/token` now derives
 * it from stored `role_assignments` rather than the request body. The consequence is that a role
 * revoked mid-session stays effective until the token expires (`tokenTtlSeconds`, 24h by default);
 * `/v1/auth/refresh` re-reads from storage, so refreshing picks up the change. Closing that window
 * entirely means checking storage per request or shortening the TTL — a TRACK-119 decision.
 *
 * Must be used inside an `authenticate` block: with no principal the caller is unauthenticated,
 * which is a 401 the auth plugin should already have produced, so this treats it as 403-worthy
 * rather than silently allowing it.
 */
fun Route.requireRole(vararg roleCodes: String, build: Route.() -> Unit): Route {
    val authorized = createChild(AuthorizationRouteSelector)
    authorized.install(RoleAuthorization) { roles = roleCodes.toSet() }
    authorized.build()
    return authorized
}

class RoleAuthorizationConfig {
    var roles: Set<String> = emptySet()
}

/**
 * Runs on the `AuthenticationChecked` hook — after the auth plugin has resolved (or failed to
 * resolve) a principal, and before the route handler. Responding here ends the call, so an
 * unauthorised request never reaches the handler.
 */
val RoleAuthorization = createRouteScopedPlugin("RoleAuthorization", ::RoleAuthorizationConfig) {
    val required = pluginConfig.roles
    on(AuthenticationChecked) { call ->
        // This hook fires whether or not authentication succeeded. With no principal the caller is
        // unauthenticated, which is the auth plugin's 401 to answer — responding 403 here would
        // both pre-empt that challenge and tell an anonymous caller their credentials were the
        // problem when they never sent any. Only an *authenticated* caller can be forbidden.
        val principal = call.principal<JWTPrincipal>() ?: return@on

        val granted = principal.payload.getClaim("roles")?.asList(String::class.java).orEmpty()
        if (required.none { it in granted }) {
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse("Requires one of: ${required.joinToString()}")
            )
        }
    }
}

fun parseUuidParam(value: String?, label: String): Uuid? {
    return value.toKotlinUuidOrNull(label)
}

fun parseLanguageParam(value: String?, label: String): LanguageCodeDto? {
    if (value.isNullOrBlank()) return null
    require(value.isNotBlank()) { "$label must not be blank" }
    return runCatching { LanguageCodeDto.valueOf(value.uppercase()) }
        .getOrElse { throw IllegalArgumentException("Invalid $label: must be a valid language code") }
}
