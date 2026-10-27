package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.RasikaSearchMode
import com.sangita.grantha.backend.api.services.RasikaSearchService
import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.catalogue.RasikaCatalogueContract
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/**
 * V2 search with typed macro hits (TRACK-152). Anonymous and published-only: unlike the V1 path
 * these routes never read the admin role. the V1 search paths are unchanged.
 */
fun Route.rasikaSearchRoutes(searchService: RasikaSearchService) {
    post(RasikaCatalogueContract.SEARCH_HYBRID_PATH) { call.respondSearch(searchService, RasikaSearchMode.HYBRID) }
    post(RasikaCatalogueContract.SEARCH_SEMANTIC_PATH) { call.respondSearch(searchService, RasikaSearchMode.SEMANTIC) }
}

private suspend fun ApplicationCall.respondSearch(service: RasikaSearchService, mode: RasikaSearchMode) {
    val request = receive<SemanticSearchRequest>()
    respond(service.search(request, mode))
}
