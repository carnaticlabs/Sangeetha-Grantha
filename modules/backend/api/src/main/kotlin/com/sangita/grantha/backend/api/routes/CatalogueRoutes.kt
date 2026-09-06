package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.CatalogueResult
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueErrorCodeDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueErrorDto
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.catalogueRoutes(catalogueService: CatalogueService) {
    route("/v1/catalogue") {
        get("/krithis") {
            call.respondCatalogue(catalogueService.searchKrithis(call.request.queryParameters))
        }
        get("/krithis/{id}") {
            call.respondCatalogue(catalogueService.getKrithi(call.parameters["id"], call.request.queryParameters))
        }
        get("/krithis/{id}/lyrics/{variantId}") {
            call.respondCatalogue(
                catalogueService.getLyrics(
                    call.parameters["id"],
                    call.parameters["variantId"],
                    call.request.queryParameters,
                ),
            )
        }
        get("/ragas") {
            call.respondCatalogue(catalogueService.searchRagas(call.request.queryParameters))
        }
        get("/ragas/{id}") {
            call.respondCatalogue(catalogueService.getRaga(call.parameters["id"], call.request.queryParameters))
        }
        get("/composers") {
            call.respondCatalogue(catalogueService.searchComposers(call.request.queryParameters))
        }
        get("/composers/{id}") {
            call.respondCatalogue(catalogueService.getComposer(call.parameters["id"], call.request.queryParameters))
        }
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.respondCatalogue(result: CatalogueResult<T>) {
    when (result) {
        is CatalogueResult.Ok -> {
            response.headers.append(HttpHeaders.CacheControl, "no-store")
            respond(HttpStatusCode.OK, result.value)
        }
        is CatalogueResult.Invalid -> respond(
            HttpStatusCode.BadRequest,
            CatalogueErrorDto(CatalogueErrorCodeDto.VALIDATION_ERROR, result.message),
        )
        CatalogueResult.NotFound -> respond(
            HttpStatusCode.NotFound,
            CatalogueErrorDto(CatalogueErrorCodeDto.NOT_FOUND, "This composition is not available."),
        )
    }
}
