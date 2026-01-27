package com.sangita.grantha.backend.api.support

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import kotlin.uuid.Uuid

fun ApplicationCall.currentUserId(): Uuid? {
    val principal = principal<JWTPrincipal>() ?: return null
    val userId = principal.payload.getClaim("userId")?.asString()
    return userId?.toKotlinUuidOrNull("userId")
}

fun ApplicationCall.currentUserRoles(): List<String> {
    val principal = principal<JWTPrincipal>() ?: return emptyList()
    val rolesClaim = principal.payload.getClaim("roles")
    return rolesClaim?.asList(String::class.java)?.filter { it.isNotBlank() } ?: emptyList()
}
