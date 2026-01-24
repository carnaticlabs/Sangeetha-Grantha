package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.BulkImportCreateBatchRequest
import com.sangita.grantha.backend.api.models.BulkImportRetryRequest
import com.sangita.grantha.backend.api.services.BulkImportOrchestrationService
import com.sangita.grantha.backend.api.services.ImportService
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.TaskStatus
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.delete
import io.ktor.server.routing.route
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.uuid.Uuid
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readBytes
import org.apache.commons.csv.CSVFormat
import java.nio.charset.StandardCharsets

fun Route.bulkImportRoutes(service: BulkImportOrchestrationService, importService: ImportService) {
    route("/v1/admin/bulk-import") {
        route("/upload") {
            post {
                val multipart = call.receiveMultipart()
                var savedFilePath: String? = null
                val maxFileSizeBytes = 10 * 1024 * 1024 // 10MB hard limit

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val originalFileName = part.originalFileName
                            ?: run {
                                part.dispose()
                                call.respondText(
                                    "File name is required",
                                    status = HttpStatusCode.BadRequest
                                )
                                return@forEachPart
                            }

                        // Sanitize file name to avoid path traversal and unsafe characters
                        val sanitizedFileName = Paths.get(originalFileName).fileName.toString()
                            .replace(Regex("[^a-zA-Z0-9._-]"), "_")

                        if (sanitizedFileName.isBlank()) {
                            part.dispose()
                            call.respondText(
                                "Invalid file name",
                                status = HttpStatusCode.BadRequest
                            )
                            return@forEachPart
                        }

                        // Only allow CSV uploads for bulk import manifests
                        if (!sanitizedFileName.endsWith(".csv", ignoreCase = true)) {
                            part.dispose()
                            call.respondText(
                                "Only CSV files are allowed for bulk import",
                                status = HttpStatusCode.BadRequest
                            )
                            return@forEachPart
                        }

                        val fileBytes = part.provider().readRemaining().readBytes()

                        // Enforce maximum file size to prevent OOM and abuse
                        if (fileBytes.size > maxFileSizeBytes) {
                            part.dispose()
                            call.respondText(
                                "File size exceeds maximum allowed size (10MB)",
                                status = HttpStatusCode.BadRequest
                            )
                            return@forEachPart
                        }
                        
                        // Ensure storage directory exists
                        val storageDir = Paths.get("storage/imports")
                        if (!Files.exists(storageDir)) {
                            Files.createDirectories(storageDir)
                        }

                        // Create unique file name to avoid collisions
                        val timestamp = System.currentTimeMillis()
                        val uniqueName = "${timestamp}_${sanitizedFileName}"
                        val file = File(storageDir.toFile(), uniqueName)
                        file.writeBytes(fileBytes)
                        
                        // Fast-fail CSV validation at upload time (TRACK-010)
                        val validationResult = validateCsvFile(file)
                        if (!validationResult.isValid) {
                            file.delete() // Clean up invalid file
                            part.dispose()
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                    "error" to "Invalid CSV file",
                                    "details" to validationResult.errors
                                )
                            )
                            return@forEachPart
                        }
                        
                        savedFilePath = file.absolutePath
                    }
                    part.dispose()
                }

                if (savedFilePath != null) {
                    val created = service.createBatch(savedFilePath!!)
                    call.respond(HttpStatusCode.Accepted, created)
                } else {
                    call.respondText("No file uploaded", status = HttpStatusCode.BadRequest)
                }
            }
        }

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

            delete("/{id}") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@delete call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                service.deleteBatch(id)
                call.respond(HttpStatusCode.NoContent)
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

            post("/{id}/approve-all") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@post call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                importService.approveAllInBatch(id)
                call.respond(HttpStatusCode.OK)
            }

            post("/{id}/reject-all") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@post call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)
                importService.rejectAllInBatch(id)
                call.respond(HttpStatusCode.OK)
            }

            // TRACK-004: Finalize batch
            post("/{id}/finalize") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@post call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)

                val summary = importService.finalizeBatch(id)
                call.respond(summary)
            }

            // TRACK-004: Export QA report
            get("/{id}/export") {
                val id = parseUuidParam(call.parameters["id"], "batchId")
                    ?: return@get call.respondText("Missing batch ID", status = HttpStatusCode.BadRequest)

                val format = call.request.queryParameters["format"] ?: "json"
                val report = importService.generateQAReport(id, format)

                when (format.lowercase()) {
                    "csv" -> {
                        call.response.headers.append("Content-Disposition", "attachment; filename=batch-${id}-report.csv")
                        call.respondText(report, contentType = io.ktor.http.ContentType.Text.CSV)
                    }
                    "json" -> {
                        call.response.headers.append("Content-Disposition", "attachment; filename=batch-${id}-report.json")
                        call.respondText(report, contentType = io.ktor.http.ContentType.Application.Json)
                    }
                    else -> call.respondText("Unsupported format. Use 'json' or 'csv'", status = HttpStatusCode.BadRequest)
                }
            }
        }
    }
}

private data class CsvValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

private fun validateCsvFile(file: File): CsvValidationResult {
    val errors = mutableListOf<String>()
    
    try {
        file.bufferedReader(StandardCharsets.UTF_8).use { reader ->
            val parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build()
                .parse(reader)
            
            val headerMap = parser.headerMap
            if (headerMap == null) {
                errors.add("CSV has no header row")
                return CsvValidationResult(false, errors)
            }
            
            // Check required columns
            val required = listOf("krithi", "hyperlink")
            val headerKeys = headerMap.keys.map { it.lowercase() }.toSet()
            val missing = required.filter { !headerKeys.contains(it) }
            if (missing.isNotEmpty()) {
                errors.add("Missing required columns: ${missing.joinToString()}")
            }
            
            // Validate first few rows for URL format
            var rowCount = 0
            parser.forEach { record ->
                rowCount++
                if (rowCount > 10) return@forEach // Only check first 10 rows
                
                val hyperlink = record.get("hyperlink") ?: ""
                if (hyperlink.isNotBlank() && !isValidUrl(hyperlink)) {
                    errors.add("Invalid URL in row ${rowCount + 1}: $hyperlink")
                }
            }
            
            if (rowCount == 0) {
                errors.add("CSV contains no data rows")
            }
        }
    } catch (e: Exception) {
        errors.add("CSV parsing error: ${e.message}")
    }
    
    return CsvValidationResult(errors.isEmpty(), errors)
}

private fun isValidUrl(url: String): Boolean {
    return try {
        val uri = URI(url)
        val scheme = uri.scheme
        val host = uri.host
        (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) && !host.isNullOrBlank()
    } catch (e: Exception) {
        false
    }
}

