package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.CuratorService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.curatorRoutes(curatorService: CuratorService) {
    route("/v1/admin/curator") {
        get("/stats") {
            val stats = curatorService.getStats()
            call.respond(stats)
        }

        get("/section-issues") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50
            val issues = curatorService.getSectionIssues(page, size)
            call.respond(issues)
        }
    }
}
