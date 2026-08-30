package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.AttachAliasRequest
import com.sangita.grantha.backend.api.services.ConfirmNewRagaRequest
import com.sangita.grantha.backend.api.services.CuratorService
import com.sangita.grantha.backend.api.services.DisambiguateRagaRequest
import com.sangita.grantha.backend.api.services.RagaQueueActionResult
import com.sangita.grantha.backend.api.services.RagaResolutionService
import com.sangita.grantha.backend.api.support.currentUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.curatorRoutes(
    curatorService: CuratorService,
    ragaResolutionService: RagaResolutionService? = null,
) {
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

        ragaResolutionService?.let { ragas ->
            route("/raga-queue") {
                get {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50
                    call.respond(ragas.listPending(page, size))
                }

                post("/scan-scale-collisions") {
                    call.respond(ragas.scanScaleCollisions(call.currentUserId()))
                }

                post("/{id}/attach") {
                    val id = parseUuidParam(call.parameters["id"], "queueId")
                        ?: return@post call.respondText("Missing queue ID", status = HttpStatusCode.BadRequest)
                    val request = call.receive<AttachAliasRequest>()
                    call.respondQueueAction(ragas.attachAlias(id, request, call.currentUserId()))
                }

                post("/{id}/confirm-new") {
                    val id = parseUuidParam(call.parameters["id"], "queueId")
                        ?: return@post call.respondText("Missing queue ID", status = HttpStatusCode.BadRequest)
                    val request = call.receive<ConfirmNewRagaRequest>()
                    call.respondQueueAction(ragas.confirmNew(id, request, call.currentUserId()))
                }

                post("/{id}/disambiguate") {
                    val id = parseUuidParam(call.parameters["id"], "queueId")
                        ?: return@post call.respondText("Missing queue ID", status = HttpStatusCode.BadRequest)
                    val request = call.receive<DisambiguateRagaRequest>()
                    call.respondQueueAction(ragas.disambiguate(id, request, call.currentUserId()))
                }
            }
        }
    }
}

private suspend fun ApplicationCall.respondQueueAction(result: RagaQueueActionResult) {
    when (result) {
        is RagaQueueActionResult.Ok -> respond(result.raga)
        is RagaQueueActionResult.NotFound -> respondText(result.message, status = HttpStatusCode.NotFound)
        is RagaQueueActionResult.Invalid -> respondText(result.message, status = HttpStatusCode.BadRequest)
    }
}
