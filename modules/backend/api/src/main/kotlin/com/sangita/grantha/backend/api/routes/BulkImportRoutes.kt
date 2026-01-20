package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.BulkImportCreateBatchRequest
import com.sangita.grantha.backend.api.models.BulkImportRetryRequest
import com.sangita.grantha.backend.api.services.BulkImportOrchestrationService
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.TaskStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.bulkImportRoutes(service: BulkImportOrchestrationService) {
    route("/v1/admin/bulk-import") {
        route("/batches") {
            post {
                val request = call.receive<BulkImportCreateBatchRequest>()
                val created = service.createBatch(request.sourceManifestPath)
                call.respond(HttpStatusCode.Accepted, created)
            }

            get {
                val statusParam = call.request.queryParameters["status"]
                val status = statusParam?.let { BatchStatus.valueOf(it.uppercase()) }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                call.respond(service.listBatches(status = status, limit = limit, offset = offset))
            }

            get("/{id}") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@get call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                val batch = service.getBatch(id) ?: return@get call.respondText("Batch not found", status = HttpStatusCode.NotFound)
                call.respond(batch)
            }

            get("/{id}/jobs") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@get call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                call.respond(service.getBatchJobs(id))
            }

            get("/{id}/tasks") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@get call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                val statusParam = call.request.queryParameters["status"]
                val status = statusParam?.let { TaskStatus.valueOf(it.uppercase()) }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 1000
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                call.respond(service.getBatchTasks(id = id, status = status, limit = limit, offset = offset))
            }

            get("/{id}/events") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@get call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                call.respond(service.getBatchEvents(id))
            }

            post("/{id}/pause") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@post call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                call.respond(service.pauseBatch(id))
            }

            post("/{id}/resume") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@post call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                call.respond(service.resumeBatch(id))
            }

            post("/{id}/cancel") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@post call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                call.respond(service.cancelBatch(id))
            }

            post("/{id}/retry") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@post call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                val request = call.receive<BulkImportRetryRequest>()
                val requeued = service.retryBatch(id, includeFailed = request.includeFailed)
                call.respond(mapOf("requeuedTasks" to requeued))
            }
        }
    }
}

