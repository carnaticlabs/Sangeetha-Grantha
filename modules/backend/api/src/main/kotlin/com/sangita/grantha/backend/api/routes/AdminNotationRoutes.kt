package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.NotationRowCreateRequest
import com.sangita.grantha.backend.api.models.NotationRowUpdateRequest
import com.sangita.grantha.backend.api.models.NotationVariantCreateRequest
import com.sangita.grantha.backend.api.models.NotationVariantUpdateRequest
import com.sangita.grantha.backend.api.services.KrithiNotationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.adminNotationRoutes(notationService: KrithiNotationService) {
    route("/v1/admin") {
        post("/krithis/{id}/notation/variants") {
            val id = parseUuidParam(call.parameters["id"], "krithiId")
                ?: return@post call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<NotationVariantCreateRequest>()
            val created = notationService.createVariant(id, request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/notation/variants/{variantId}") {
            val variantId = parseUuidParam(call.parameters["variantId"], "variantId")
                ?: return@put call.respondText("Missing variant ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<NotationVariantUpdateRequest>()
            val updated = notationService.updateVariant(variantId, request)
            call.respond(updated)
        }

        delete("/notation/variants/{variantId}") {
            val variantId = parseUuidParam(call.parameters["variantId"], "variantId")
                ?: return@delete call.respondText("Missing variant ID", status = HttpStatusCode.BadRequest)
            val deleted = notationService.deleteVariant(variantId)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("Not found", status = HttpStatusCode.NotFound)
            }
        }

        post("/notation/variants/{variantId}/rows") {
            val variantId = parseUuidParam(call.parameters["variantId"], "variantId")
                ?: return@post call.respondText("Missing variant ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<NotationRowCreateRequest>()
            val created = notationService.createRow(variantId, request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/notation/rows/{rowId}") {
            val rowId = parseUuidParam(call.parameters["rowId"], "rowId")
                ?: return@put call.respondText("Missing row ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<NotationRowUpdateRequest>()
            val updated = notationService.updateRow(rowId, request)
            call.respond(updated)
        }

        delete("/notation/rows/{rowId}") {
            val rowId = parseUuidParam(call.parameters["rowId"], "rowId")
                ?: return@delete call.respondText("Missing row ID", status = HttpStatusCode.BadRequest)
            val deleted = notationService.deleteRow(rowId)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("Not found", status = HttpStatusCode.NotFound)
            }
        }
    }
}
