package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.models.ImportOverridesDto
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.backend.api.services.IWebScraper
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.shared.domain.model.ScrapeRequest
import com.sangita.grantha.shared.domain.model.ImportStatusDto

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

fun Route.importRoutes(
    importService: IImportService,
    webScrapingService: IWebScraper
) {
    // TRACK-064: Legacy inline scraper endpoint now enqueues HTML extraction via ImportService.
    // Keep IWebScraper in signature for backward-compatibility with DI wiring.
    @Suppress("UNUSED_VARIABLE")
    val unused = webScrapingService

    route("/v1/admin/imports") {
        // List imports
        get {
            // Parse optional status filter
            val statusParam = call.request.queryParameters["status"]
            val status = statusParam?.let { ImportStatus.valueOf(it) }
            
            val imports = importService.getImports(status)
            call.respond(imports)
        }

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

        post("/scrape") {
            val request = call.receive<ScrapeRequest>()

            // TRACK-064: Queue HTML extraction instead of scraping inline in Kotlin.
            val importRequest = ImportKrithiRequest(
                source = "WebScraper",
                sourceKey = request.url,
            )

            val createdList = importService.submitImports(listOf(importRequest))
            val created = createdList.firstOrNull() 
                ?: return@post call.respondText("Failed to create import", status = HttpStatusCode.InternalServerError)

            call.respond(HttpStatusCode.Accepted, created)
        }

        // TRACK-012: Bulk review endpoint
        post("/bulk-review") {
            val request = call.receive<BulkReviewRequest>()
            val logger = LoggerFactory.getLogger("ImportRoutes")

            val results = request.importIds.map { importIdStr ->
                try {
                    val importId = Uuid.parse(importIdStr)
                    when (request.action) {
                        "APPROVE" -> {
                            importService.reviewImport(
                                id = importId,
                                request = ImportReviewRequest(
                                    status = ImportStatusDto.APPROVED,
                                    overrides = request.overrides?.let { 
                                        ImportOverridesDto(
                                            composer = it["composer"],
                                            raga = it["raga"],
                                            tala = it["tala"],
                                            title = it["title"],
                                            language = it["language"],
                                            lyrics = it["lyrics"]
                                        )
                                    }
                                )
                            )
                            BulkReviewResult(importIdStr, "APPROVED", null)
                        }
                        "REJECT" -> {
                            importService.reviewImport(
                                id = importId,
                                request = ImportReviewRequest(
                                    status = ImportStatusDto.REJECTED,
                                    reviewerNotes = request.reason
                                )
                            )
                            BulkReviewResult(importIdStr, "REJECTED", null)
                        }
                        else -> BulkReviewResult(importIdStr, "ERROR", "Invalid action: ${request.action}")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to review import $importIdStr", e)
                    BulkReviewResult(importIdStr, "ERROR", e.message ?: "Unknown error")
                }
            }

            call.respond(mapOf(
                "total" to results.size,
                "succeeded" to results.count { it.status != "ERROR" },
                "failed" to results.count { it.status == "ERROR" },
                "results" to results
            ))
        }

        // TRACK-012: Auto-approve queue endpoint
        get("/auto-approve-queue") {
            val batchIdStr = call.request.queryParameters["batchId"]
            val batchId = batchIdStr?.let { Uuid.parse(it) }
            val qualityTier = call.request.queryParameters["qualityTier"]
            val confidenceMin = call.request.queryParameters["confidenceMin"]?.toDoubleOrNull()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            val queue = importService.getAutoApproveQueue(
                batchId = batchId,
                qualityTier = qualityTier,
                confidenceMin = confidenceMin,
                limit = limit,
                offset = offset
            )

            call.respond(queue)
        }
    }
}

@Serializable
data class BulkReviewRequest(
    val importIds: List<String>,
    val action: String,
    val overrides: Map<String, String>? = null,
    val reason: String? = null
)

@Serializable
data class BulkReviewResult(
    val importId: String,
    val status: String,
    val error: String?
)
