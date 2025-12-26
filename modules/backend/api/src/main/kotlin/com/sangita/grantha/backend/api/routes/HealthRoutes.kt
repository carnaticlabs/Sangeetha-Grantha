package com.sangita.grantha.backend.api.routes

import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoutes() {
    get("/health") {
        call.respondText("OK")
    }
    get("/v1/health") {
        call.respondText("OK")
    }
}
