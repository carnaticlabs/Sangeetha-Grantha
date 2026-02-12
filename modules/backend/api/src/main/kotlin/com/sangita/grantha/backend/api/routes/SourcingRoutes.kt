package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.SourcingService
import com.sangita.grantha.backend.api.services.VariantMatchingService
import com.sangita.grantha.backend.api.support.currentUserId
import com.sangita.grantha.shared.domain.model.CreateExtractionRequestDto
import com.sangita.grantha.shared.domain.model.CreateSourceRequestDto
import com.sangita.grantha.shared.domain.model.ManualOverrideRequestDto
import com.sangita.grantha.shared.domain.model.UpdateSourceRequestDto
import com.sangita.grantha.shared.domain.model.VariantMatchReviewRequestDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlin.uuid.Uuid
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * TRACK-045: All sourcing & extraction monitoring routes.
 * Mounted under /v1/admin/sourcing within the authenticated block.
 */
fun Route.sourcingRoutes(service: SourcingService, variantService: VariantMatchingService) {
    route("/v1/admin/sourcing") {

        // =====================================================================
        // §5.1 Source Registry
        // =====================================================================
        route("/sources") {
            get {
                val tier = call.parameters.getAll("tier")?.mapNotNull { it.toIntOrNull() }
                val format = call.parameters.getAll("format")
                val search = call.parameters["search"]
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20

                call.respond(service.listSources(tier, format, search, page, pageSize))
            }

            get("/{id}") {
                val id = Uuid.parse(call.parameters["id"]!!)
                val detail = service.getSourceDetail(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Source not found"))
                call.respond(detail)
            }

            post {
                val request = call.receive<CreateSourceRequestDto>()
                val userId = call.currentUserId()
                val result = service.createSource(request, userId)
                call.respond(HttpStatusCode.Created, result)
            }

            put("/{id}") {
                val id = Uuid.parse(call.parameters["id"]!!)
                val request = call.receive<UpdateSourceRequestDto>()
                val userId = call.currentUserId()
                val result = service.updateSource(id, request, userId)
                    ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Source not found"))
                call.respond(result)
            }

            delete("/{id}") {
                val id = Uuid.parse(call.parameters["id"]!!)
                val userId = call.currentUserId()
                service.deactivateSource(id, userId)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        // =====================================================================
        // §5.2 Extraction Queue
        // =====================================================================
        route("/extractions") {
            get {
                val status = call.parameters.getAll("status")
                val format = call.parameters.getAll("format")
                val sourceId = call.parameters["sourceId"]
                val batchId = call.parameters["batchId"]
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20

                call.respond(service.listExtractions(status, format, sourceId, batchId, page, pageSize))
            }

            get("/stats") {
                call.respond(service.getExtractionStats())
            }

            get("/{id}") {
                val id = Uuid.parse(call.parameters["id"]!!)
                val detail = service.getExtractionDetail(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Extraction not found"))
                call.respond(detail)
            }

            post {
                val request = call.receive<CreateExtractionRequestDto>()
                val userId = call.currentUserId()
                val result = service.createExtraction(request, userId)
                call.respond(HttpStatusCode.Created, result)
            }

            post("/{id}/retry") {
                val id = Uuid.parse(call.parameters["id"]!!)
                val userId = call.currentUserId()
                val success = service.retryExtraction(id, userId)
                if (success) call.respond(HttpStatusCode.OK, mapOf("status" to "retried"))
                else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Extraction not found"))
            }

            post("/{id}/cancel") {
                val id = Uuid.parse(call.parameters["id"]!!)
                val userId = call.currentUserId()
                val success = service.cancelExtraction(id, userId)
                if (success) call.respond(HttpStatusCode.OK, mapOf("status" to "cancelled"))
                else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Extraction not found"))
            }

            post("/retry-all-failed") {
                val userId = call.currentUserId()
                val count = service.retryAllFailedExtractions(userId)
                call.respond(mapOf("retriedCount" to count))
            }
        }

        // =====================================================================
        // §5.3 Source Evidence
        // =====================================================================
        route("/evidence") {
            get {
                val minSourceCount = call.parameters["minSourceCount"]?.toIntOrNull()
                val search = call.parameters["search"]
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20

                call.respond(service.listEvidence(minSourceCount, search, page, pageSize))
            }

            get("/krithi/{id}") {
                val krithiId = Uuid.parse(call.parameters["id"]!!)
                val evidence = service.getKrithiEvidence(krithiId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Evidence not found"))
                call.respond(evidence)
            }
        }

        // =====================================================================
        // §5.4 Structural Voting
        // =====================================================================
        route("/voting") {
            get {
                val consensusType = call.parameters.getAll("consensusType")
                val confidence = call.parameters.getAll("confidence")
                val hasDissents = call.parameters["hasDissents"]?.toBooleanStrictOrNull()
                val pendingReview = call.parameters["pendingReview"]?.toBooleanStrictOrNull()
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20

                call.respond(service.listVotingDecisions(consensusType, confidence, hasDissents, pendingReview, page, pageSize))
            }

            get("/stats") {
                call.respond(service.getVotingStats())
            }

            get("/{id}") {
                val id = Uuid.parse(call.parameters["id"]!!)
                val detail = service.getVotingDetail(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Voting decision not found"))
                call.respond(detail)
            }

            post("/{id}/override") {
                val id = Uuid.parse(call.parameters["id"]!!)
                val request = call.receive<ManualOverrideRequestDto>()
                val userId = call.currentUserId()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
                val result = service.submitOverride(id, request.structure, request.notes, userId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Voting decision not found"))
                call.respond(result)
            }
        }

        // =====================================================================
        // §5.6 Variant Matching (TRACK-056)
        // =====================================================================
        route("/variants") {
            // List pending variant matches across all extractions
            get("/pending") {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 50
                call.respond(variantService.listPendingMatches(page, pageSize))
            }

            // List variant matches for a specific extraction
            get("/extraction/{extractionId}") {
                val extractionId = Uuid.parse(call.parameters["extractionId"]!!)
                val status = call.parameters.getAll("status")
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 50
                call.respond(variantService.listMatches(extractionId, status, page, pageSize))
            }

            // Get match report for an extraction
            get("/extraction/{extractionId}/report") {
                val extractionId = Uuid.parse(call.parameters["extractionId"]!!)
                call.respond(variantService.getMatchReport(extractionId))
            }

            // Review a variant match (approve/reject)
            post("/{id}/review") {
                val id = Uuid.parse(call.parameters["id"]!!)
                val request = call.receive<VariantMatchReviewRequestDto>()
                val userId = call.currentUserId()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
                val success = variantService.reviewMatch(id, request.action, userId, request.notes)
                if (success) call.respond(HttpStatusCode.OK, mapOf("status" to "reviewed"))
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid action or match not found"))
            }
        }

        // =====================================================================
        // §5.5 Quality Dashboard
        // =====================================================================
        route("/quality") {
            get("/summary") {
                call.respond(service.getQualitySummary())
            }

            // Placeholder endpoints — will be fully implemented in TRACK-051
            get("/distribution") {
                call.respond(mapOf("buckets" to emptyList<Any>()))
            }

            get("/coverage") {
                call.respond(buildJsonObject {
                    put("tierCoverage", buildJsonArray { })
                    put("composerFieldMatrix", buildJsonObject {
                        put("composers", buildJsonArray { })
                        put("fields", buildJsonArray { })
                        put("data", buildJsonArray { })
                    })
                    put("phaseProgress", buildJsonArray { })
                })
            }

            get("/gaps") {
                call.respond(mapOf("gaps" to emptyList<Any>()))
            }

            get("/audit") {
                call.respond(buildJsonObject {
                    put("results", buildJsonArray { })
                    put("lastRunAt", null as String?)
                })
            }

            post("/audit/run") {
                val userId = call.currentUserId()
                dal@ run {
                    // Placeholder — will trigger actual audit in TRACK-051
                }
                call.respond(buildJsonObject {
                    put("results", buildJsonArray { })
                    put("lastRunAt", null as String?)
                })
            }
        }
    }
}
