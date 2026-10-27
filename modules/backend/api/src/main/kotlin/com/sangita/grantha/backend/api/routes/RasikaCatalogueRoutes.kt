package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.dal.models.CatalogueVisibility
import com.sangita.grantha.shared.domain.model.catalogue.RasikaCatalogueContract
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.rasikaCatalogueRoutes(catalogueService: CatalogueService) {
    route(RasikaCatalogueContract.ROOT) {
        get("/discovery") {
            call.respondCatalogue(
                catalogueService.discovery(call.request.queryParameters, CatalogueVisibility.ALL_PUBLISHED),
            )
        }
        get("/krithis") {
            call.respondCatalogue(
                catalogueService.searchKrithis(call.request.queryParameters, CatalogueVisibility.ALL_PUBLISHED),
            )
        }
        get("/facets") {
            call.respondCatalogue(catalogueService.listFacets(call.request.queryParameters))
        }
        get("/krithis/{id}") {
            call.respondCatalogue(
                catalogueService.getKrithi(
                    call.parameters["id"],
                    call.request.queryParameters,
                    CatalogueVisibility.ALL_PUBLISHED,
                ),
            )
        }
        get("/krithis/{id}/lyrics/{variantId}") {
            call.respondCatalogue(
                catalogueService.getLyrics(
                    call.parameters["id"],
                    call.parameters["variantId"],
                    call.request.queryParameters,
                    CatalogueVisibility.ALL_PUBLISHED,
                ),
            )
        }
        get("/collections") {
            call.respondCatalogue(catalogueService.listCollections(call.request.queryParameters))
        }
        get("/collections/{slug}") {
            call.respondCatalogue(
                catalogueService.getCollection(call.parameters["slug"], call.request.queryParameters),
            )
        }
        get("/temples") {
            call.respondCatalogue(catalogueService.listTemples(call.request.queryParameters))
        }
        get("/temples/{id}") {
            call.respondCatalogue(catalogueService.getTemple(call.parameters["id"], call.request.queryParameters))
        }
        get("/ragas") {
            call.respondCatalogue(
                catalogueService.searchRagas(call.request.queryParameters, CatalogueVisibility.ALL_PUBLISHED),
            )
        }
        get("/ragas/{id}") {
            call.respondCatalogue(
                catalogueService.getRaga(
                    call.parameters["id"],
                    call.request.queryParameters,
                    CatalogueVisibility.ALL_PUBLISHED,
                ),
            )
        }
        get("/composers") {
            call.respondCatalogue(
                catalogueService.searchComposers(call.request.queryParameters, CatalogueVisibility.ALL_PUBLISHED),
            )
        }
        get("/composers/{id}") {
            call.respondCatalogue(
                catalogueService.getComposer(
                    call.parameters["id"],
                    call.request.queryParameters,
                    CatalogueVisibility.ALL_PUBLISHED,
                ),
            )
        }
    }
}
