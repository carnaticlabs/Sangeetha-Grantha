package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.HybridSearchService
import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.SemanticSearchResponse
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.semanticSearchRoutes(
    hybridSearchService: HybridSearchService,
) {
    route("/v1/search") {
        authenticate("admin-auth", optional = true) {
            post("/hybrid") { call.respondSemanticSearch(hybridSearchService::searchHybrid) }
            post("/semantic") { call.respondSemanticSearch(hybridSearchService::searchSemantic) }
        }
    }
}

private suspend fun ApplicationCall.respondSemanticSearch(
    search: suspend (SemanticSearchRequest, Boolean) -> SemanticSearchResponse,
) {
    val request = receive<SemanticSearchRequest>()
    respond(search(request, !hasAdminRole()))
}
