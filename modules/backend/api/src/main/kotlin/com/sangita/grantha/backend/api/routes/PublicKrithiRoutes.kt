package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.KrithiNotationService
import com.sangita.grantha.backend.api.services.KrithiService
import com.sangita.grantha.backend.api.services.ReferenceDataService
import com.sangita.grantha.shared.domain.model.KrithiSearchRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.publicKrithiRoutes(
    krithiService: KrithiService,
    referenceDataService: ReferenceDataService,
    notationService: KrithiNotationService,
) {
    route("/v1") {
        authenticate("admin-auth", optional = true) {
            get("/krithis/search") {
                val isAdmin = call.principal<UserIdPrincipal>() != null
                val request = KrithiSearchRequest(
                    query = call.request.queryParameters["query"],
                    lyric = call.request.queryParameters["lyric"],
                    composerId = call.request.queryParameters["composerId"],
                    ragaId = call.request.queryParameters["ragaId"],
                    talaId = call.request.queryParameters["talaId"],
                    deityId = call.request.queryParameters["deityId"],
                    templeId = call.request.queryParameters["templeId"],
                    language = parseLanguageParam(call.request.queryParameters["primaryLanguage"], "primaryLanguage"),
                    page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0,
                    pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
                )
                // For admin console, show all items by default (not just published)
                // Allow publishedOnly to be controlled via query parameter if needed
                val publishedOnly = call.request.queryParameters["publishedOnly"]?.toBoolean() ?: false
                call.respond(krithiService.search(request, publishedOnly = publishedOnly))
            }
        }

        get("/krithis/{id}") {
            val id = parseUuidParam(call.parameters["id"], "krithiId")
                ?: return@get call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
            val krithi = krithiService.getKrithi(id)
            if (krithi == null) {
                call.respondText("Not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(krithi)
            }
        }

        authenticate("admin-auth", optional = true) {
            get("/krithis/{id}/notation") {
                val id = parseUuidParam(call.parameters["id"], "krithiId")
                    ?: return@get call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
                val isAdmin = call.principal<UserIdPrincipal>() != null
                val notation = if (isAdmin) {
                    notationService.getAdminNotation(id)
                } else {
                    notationService.getPublishedNotation(id)
                }
                if (notation == null) {
                    call.respondText("Not found", status = HttpStatusCode.NotFound)
                } else {
                    call.respond(notation)
                }
            }
        }

        get("/composers") {
            call.respond(referenceDataService.listComposers())
        }
        get("/ragas") {
            call.respond(referenceDataService.listRagas())
        }
        get("/talas") {
            call.respond(referenceDataService.listTalas())
        }
        get("/deities") {
            call.respond(referenceDataService.listDeities())
        }
        get("/temples") {
            call.respond(referenceDataService.listTemples())
        }
        get("/tags") {
            call.respond(referenceDataService.listTags())
        }
        get("/sampradayas") {
            call.respond(referenceDataService.listSampradayas())
        }
    }
}
