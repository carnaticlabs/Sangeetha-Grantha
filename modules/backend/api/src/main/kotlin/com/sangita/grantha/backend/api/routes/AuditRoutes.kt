package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.AuditLogService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.auditRoutes(auditLogService: AuditLogService) {
    route("/v1/audit") {
        get("/logs") {
            call.respond(auditLogService.listRecent())
        }
    }
}
