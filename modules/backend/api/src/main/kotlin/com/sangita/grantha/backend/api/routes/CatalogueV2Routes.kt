package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.dal.models.CatalogueVisibility
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueV2Contract
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.catalogueV2Routes(catalogueService: CatalogueService) {
    route(CatalogueV2Contract.ROOT) {
        get("/discovery") {
            call.respondCatalogue(
                catalogueService.discovery(call.request.queryParameters, CatalogueVisibility.V2),
            )
        }
        get("/krithis") {
            call.respondCatalogue(
                catalogueService.searchKrithis(call.request.queryParameters, CatalogueVisibility.V2),
            )
        }
        get("/krithis/{id}") {
            call.respondCatalogue(
                catalogueService.getKrithi(
                    call.parameters["id"],
                    call.request.queryParameters,
                    CatalogueVisibility.V2,
                ),
            )
        }
        get("/krithis/{id}/lyrics/{variantId}") {
            call.respondCatalogue(
                catalogueService.getLyrics(
                    call.parameters["id"],
                    call.parameters["variantId"],
                    call.request.queryParameters,
                    CatalogueVisibility.V2,
                ),
            )
        }
        get("/ragas") {
            call.respondCatalogue(
                catalogueService.searchRagas(call.request.queryParameters, CatalogueVisibility.V2),
            )
        }
        get("/ragas/{id}") {
            call.respondCatalogue(
                catalogueService.getRaga(
                    call.parameters["id"],
                    call.request.queryParameters,
                    CatalogueVisibility.V2,
                ),
            )
        }
        get("/composers") {
            call.respondCatalogue(
                catalogueService.searchComposers(call.request.queryParameters, CatalogueVisibility.V2),
            )
        }
        get("/composers/{id}") {
            call.respondCatalogue(
                catalogueService.getComposer(
                    call.parameters["id"],
                    call.request.queryParameters,
                    CatalogueVisibility.V2,
                ),
            )
        }
    }
}
