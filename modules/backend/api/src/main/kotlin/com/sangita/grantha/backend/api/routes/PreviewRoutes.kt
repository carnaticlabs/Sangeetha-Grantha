package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.catalogue.CatalogueParameters
import com.sangita.grantha.backend.api.catalogue.CatalogueParseResult
import com.sangita.grantha.backend.api.services.PreviewSearchService
import com.sangita.grantha.backend.api.services.RasikaSearchMode
import com.sangita.grantha.shared.domain.model.catalogue.PreviewSearchRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Name of the per-user limit on the preview search posts; registered in `configureRequestValidation`. */
val PREVIEW_SEARCH_RATE_LIMIT = RateLimitName("preview-search")

const val PREVIEW_ROOT = "/v1/admin/preview"

/**
 * Curator Mobile Preview search and index status (TRACK-158). Mount inside `requireRole(ADMIN)`. Read-only: the
 * posts carry a query, nothing is stored, and the paths sit outside `/v1/catalogue` and `/v2/catalogue`, so the
 * mobile usage log never sees them.
 */
fun Route.previewSearchRoutes(service: PreviewSearchService) {
    route(PREVIEW_ROOT) {
        rateLimit(PREVIEW_SEARCH_RATE_LIMIT) {
            post("/search/hybrid") { call.respond(service.search(call.receive<PreviewSearchRequest>(), RasikaSearchMode.HYBRID)) }
            post("/search/semantic") { call.respond(service.search(call.receive<PreviewSearchRequest>(), RasikaSearchMode.SEMANTIC)) }
        }
        get("/krithis/{id}/index-status") {
            val id = when (val parsed = CatalogueParameters.parsePathUuid(call.parameters["id"], "id")) {
                is CatalogueParseResult.Ok -> parsed.value
                is CatalogueParseResult.Invalid -> return@get call.respondText("id must be a valid UUID", status = HttpStatusCode.BadRequest)
            }
            val status = service.indexStatus(id) ?: return@get call.respondText("Krithi not found", status = HttpStatusCode.NotFound)
            call.respond(status)
        }
    }
}

/** The authenticated curator's id, used as the rate-limit key so one curator cannot spend another's allowance. */
fun io.ktor.server.application.ApplicationCall.previewRateLimitKey(): String =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString() ?: request.local.remoteHost
