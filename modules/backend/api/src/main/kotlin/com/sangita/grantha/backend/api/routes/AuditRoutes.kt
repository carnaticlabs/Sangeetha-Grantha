package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.AuditLogService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlin.uuid.Uuid

fun Route.auditRoutes(auditLogService: AuditLogService) {
    route("/v1/audit") {
        get("/logs") {
            val entityTable = call.request.queryParameters["entityTable"]
            val entityId = call.request.queryParameters["entityId"]

            val logs = if (entityTable != null && entityId != null) {
                auditLogService.listByEntity(entityTable, Uuid.parse(entityId))
            } else {
                auditLogService.listRecent()
            }

            call.respond(logs)
        }
    }
}
