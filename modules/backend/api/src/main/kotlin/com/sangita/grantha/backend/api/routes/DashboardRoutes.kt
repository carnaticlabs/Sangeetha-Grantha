package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.AdminDashboardService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.adminDashboardRoutes(dashboardService: AdminDashboardService) {
    route("/v1/admin/dashboard") {
        get("/stats") {
            val stats = dashboardService.getStats()
            call.respond(stats)
        }
    }
}
