package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.services.ImportService
import com.sangita.grantha.backend.api.services.WebScrapingService
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.shared.domain.model.ScrapeRequest

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

fun Route.importRoutes(
    importService: ImportService,
    webScrapingService: WebScrapingService
) {
    route("/v1") {
        route("/imports") {
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

        route("/admin/imports") {
            // New route to list imports
            get {
                // Parse optional status filter
                val statusParam = call.request.queryParameters["status"]
                val status = statusParam?.let { ImportStatus.valueOf(it) }
                
                val imports = importService.getImports(status)
                call.respond(imports)
            }

            post("/scrape") {
                val request = call.receive<ScrapeRequest>()
                
                // 1. Scrape the content
                val scraped = webScrapingService.scrapeShivkumarKrithi(request.url)

                // 2. Prepare the import request
                val importRequest = ImportKrithiRequest(
                    source = "WebScraper",
                    sourceKey = request.url,
                    rawTitle = scraped.title,
                    rawLyrics = scraped.lyrics,
                    rawComposer = scraped.composer,
                    rawRaga = scraped.raga,
                    rawTala = scraped.tala,
                    rawDeity = scraped.deity,
                    rawTemple = scraped.temple,
                    rawLanguage = scraped.language,
                    rawPayload = Json.encodeToJsonElement(scraped)
                )

                // 3. Submit for import review
                val createdList = importService.submitImports(listOf(importRequest))
                val created = createdList.firstOrNull() 
                    ?: return@post call.respondText("Failed to create import", status = HttpStatusCode.InternalServerError)

                call.respond(HttpStatusCode.Created, created)
            }
        }
    }
}
