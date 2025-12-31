package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.ReferenceDataService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.referenceDataRoutes(service: ReferenceDataService) {
    route("/v1/reference") {
        get("/stats") {
            call.respond(service.getStats())
        }
    }
}
