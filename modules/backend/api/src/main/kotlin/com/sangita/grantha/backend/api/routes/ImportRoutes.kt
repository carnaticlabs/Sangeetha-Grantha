package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.services.ImportService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.importRoutes(importService: ImportService) {
    route("/v1/imports") {
        post("/krithis") {
            val requests = call.receive<List<ImportKrithiRequest>>()
            val created = importService.submitImports(requests)
            call.respond(HttpStatusCode.Accepted, created)
        }

        post("/{id}/review") {
            val id = parseUuidParam(call.parameters["id"], "importId")
                ?: return@post call.respondText("Missing import ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<ImportReviewRequest>()
            val updated = importService.reviewImport(id, request)
            call.respond(updated)
        }
    }
}
