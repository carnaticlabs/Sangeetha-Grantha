package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.KrithiCreateRequest
import com.sangita.grantha.backend.api.models.KrithiUpdateRequest
import com.sangita.grantha.backend.api.services.KrithiService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.adminKrithiRoutes(krithiService: KrithiService) {
    route("/v1") {
        post("/krithis") {
            val request = call.receive<KrithiCreateRequest>()
            val created = krithiService.createKrithi(request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/krithis/{id}") {
            val id = parseUuidParam(call.parameters["id"], "krithiId")
                ?: return@put call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<KrithiUpdateRequest>()
            val updated = krithiService.updateKrithi(id, request)
            call.respond(updated)
        }
    }
}
