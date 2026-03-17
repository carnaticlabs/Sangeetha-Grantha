package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.models.ImportOverridesDto
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.backend.api.services.LyricVariantPersistenceService
import com.sangita.grantha.backend.dal.SangitaDal
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

@Serializable
data class ImportValidationResponse(
    val importId: String,
    val krithiId: String,
    val status: String,
    val hasParsedPayload: Boolean,
    val payloadFormat: String,
    val sectionCount: Int,
    val variantCount: Int,
    val lyricSectionCount: Int,
    val issues: List<String>,
)

@Serializable
data class ReExtractResponse(
    val totalMatching: Int,
    val requeued: Int,
    val variantsCleared: Int,
)

@Serializable
data class ValidationSummaryResponse(
    val totalImports: Int,
    val withParsedPayload: Int,
    val withLyricVariants: Int,
    val needsBackfill: Int,
)

fun Route.importRoutes(
    importService: IImportService,
    lyricPersistence: LyricVariantPersistenceService? = null,
    dal: SangitaDal? = null,
) {
    route("/v1/admin/imports") {
        // List imports (with optional pagination and multi-status filter)
        get {
            val statusParam = call.request.queryParameters["status"]
            val status = statusParam?.let { ImportStatus.valueOf(it) }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            val imports = importService.getImports(status, limit = limit, offset = offset)
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

        // TRACK-094: Backfill lyrics for approved imports missing lyric variants
        post("/backfill-lyrics") {
            if (lyricPersistence == null || dal == null) {
                return@post call.respondText(
                    "Backfill not available — service not wired",
                    status = HttpStatusCode.ServiceUnavailable
                )
            }
            val report = lyricPersistence.backfillApprovedImports(dal)
            call.respond(report)
        }

        // TRACK-097: Re-queue extraction for imports matching a source URL pattern.
        // Resets extraction_queue entries from INGESTED → PENDING so the Python
        // worker re-processes them with the latest parser fixes.
        post("/re-extract") {
            if (dal == null) {
                return@post call.respondText(
                    "Re-extraction not available — DAL not wired",
                    status = HttpStatusCode.ServiceUnavailable
                )
            }
            val body = call.receive<Map<String, String>>()
            val sourcePattern = body["sourceUrlPattern"]
                ?: return@post call.respondText("Missing sourceUrlPattern", status = HttpStatusCode.BadRequest)

            // Find extraction queue entries matching the pattern
            val (tasks, total) = dal.extractionQueue.list(
                status = listOf("INGESTED", "DONE", "FAILED"),
                limit = 200,
            )
            val matching = tasks.filter { it.sourceUrl.contains(sourcePattern, ignoreCase = true) }

            var requeued = 0
            for (task in matching) {
                val success = dal.extractionQueue.retry(task.id)
                if (success) requeued++
            }

            // Also clear existing lyric variants for affected imports (any status)
            // so the backfill can re-persist after re-extraction
            val allImports = listOf(
                com.sangita.grantha.backend.dal.enums.ImportStatus.APPROVED,
                com.sangita.grantha.backend.dal.enums.ImportStatus.MAPPED,
                com.sangita.grantha.backend.dal.enums.ImportStatus.IN_REVIEW,
            ).flatMap { dal.imports.listImports(status = it) }

            var variantsCleared = 0
            for (imp in allImports) {
                // Match by source_key (URL) rather than parsed_payload content
                val sourceKey = imp.sourceKey ?: ""
                if (!sourceKey.contains(sourcePattern, ignoreCase = true)) continue
                val mappedId = imp.mappedKrithiId ?: continue
                val variants = dal.krithiLyrics.getLyricVariants(mappedId)
                if (variants.isNotEmpty()) {
                    dal.krithiLyrics.deleteAllVariants(mappedId)
                    variantsCleared++
                }
            }

            call.respond(ReExtractResponse(
                totalMatching = matching.size,
                requeued = requeued,
                variantsCleared = variantsCleared,
            ))
        }

        // TRACK-095: Validation endpoint for a single import
        get("/{id}/validation") {
            if (dal == null) {
                return@get call.respondText("Validation not available", status = HttpStatusCode.ServiceUnavailable)
            }
            val id = parseUuidParam(call.parameters["id"], "importId")
                ?: return@get call.respondText("Missing import ID", status = HttpStatusCode.BadRequest)

            val importDto = dal.imports.findById(id)
                ?: return@get call.respondText("Import not found", status = HttpStatusCode.NotFound)

            val mappedId = importDto.mappedKrithiId
            val sectionCount: Int
            val variantCount: Int
            val lyricSectionCount: Int
            val issues = mutableListOf<String>()

            if (mappedId != null) {
                val sections = dal.krithis.getSections(mappedId)
                sectionCount = sections.size
                val variants = dal.krithiLyrics.getLyricVariants(mappedId)
                variantCount = variants.size
                lyricSectionCount = variants.sumOf { it.sections.size }

                if (variantCount == 0 && importDto.parsedPayload != null) {
                    issues.add("Approved with parsed_payload but zero lyric variants — needs backfill")
                }
                for (v in variants) {
                    if (v.sections.isEmpty() && sectionCount > 0) {
                        issues.add("Variant ${v.variant.language} has 0 lyric sections (expected $sectionCount)")
                    }
                }
                if (sectionCount == 0 && importDto.parsedPayload != null) {
                    issues.add("No krithi_sections created despite having parsed_payload")
                }
            } else {
                sectionCount = 0
                variantCount = 0
                lyricSectionCount = 0
                if (importDto.importStatus.name == "APPROVED") {
                    issues.add("Approved but no mapped krithi ID")
                }
            }

            val payloadFormat = when {
                importDto.parsedPayload == null -> "none"
                importDto.parsedPayload!!.contains("sourceUrl") -> "CanonicalExtractionDto"
                importDto.parsedPayload!!.contains("\"title\"") -> "ScrapedKrithiMetadata"
                else -> "unknown"
            }

            call.respond(ImportValidationResponse(
                importId = importDto.id.toString(),
                krithiId = mappedId?.toString() ?: "",
                status = importDto.importStatus.name,
                hasParsedPayload = importDto.parsedPayload != null,
                payloadFormat = payloadFormat,
                sectionCount = sectionCount,
                variantCount = variantCount,
                lyricSectionCount = lyricSectionCount,
                issues = issues,
            ))
        }

        // TRACK-095: Batch validation summary
        get("/validation/summary") {
            if (dal == null) {
                return@get call.respondText("Validation not available", status = HttpStatusCode.ServiceUnavailable)
            }
            val statusParam = call.request.queryParameters["status"]
            val status = statusParam?.let { ImportStatus.valueOf(it) }
            val imports = dal.imports.listImports(status = status)

            var withPayload = 0
            var withVariants = 0
            var needsBackfill = 0

            for (imp in imports) {
                if (imp.parsedPayload != null) withPayload++
                val mapped = imp.mappedKrithiId ?: continue
                val variants = dal.krithiLyrics.getLyricVariants(mapped)
                if (variants.isNotEmpty()) {
                    withVariants++
                } else if (imp.parsedPayload != null) {
                    needsBackfill++
                }
            }

            call.respond(ValidationSummaryResponse(
                totalImports = imports.size,
                withParsedPayload = withPayload,
                withLyricVariants = withVariants,
                needsBackfill = needsBackfill,
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
