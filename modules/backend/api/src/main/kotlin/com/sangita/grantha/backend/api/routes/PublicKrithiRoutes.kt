package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.IKrithiService
import com.sangita.grantha.backend.api.services.IReferenceDataService
import com.sangita.grantha.backend.api.services.KrithiNotationService
import com.sangita.grantha.backend.api.support.computeEtag
import com.sangita.grantha.shared.domain.model.KrithiSearchRequest
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.publicKrithiRoutes(
    krithiService: IKrithiService,
    referenceDataService: IReferenceDataService,
    notationService: KrithiNotationService,
) {
    route("/v1") {
        authenticate("admin-auth", optional = true) {
            get("/krithis/search") {
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
                val isAdmin = call.principal<JWTPrincipal>() != null
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
            val composers = referenceDataService.listComposers()
            val etag = computeEtag(composers.joinToString("|") { "${it.id}-${it.updatedAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(composers)
        }
        get("/ragas") {
            val ragas = referenceDataService.listRagas()
            val etag = computeEtag(ragas.joinToString("|") { "${it.id}-${it.updatedAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(ragas)
        }
        get("/talas") {
            val talas = referenceDataService.listTalas()
            val etag = computeEtag(talas.joinToString("|") { "${it.id}-${it.updatedAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(talas)
        }
        get("/deities") {
            val deities = referenceDataService.listDeities()
            val etag = computeEtag(deities.joinToString("|") { "${it.id}-${it.updatedAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(deities)
        }
        get("/temples") {
            val temples = referenceDataService.listTemples()
            val etag = computeEtag(temples.joinToString("|") { "${it.id}-${it.updatedAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(temples)
        }
        get("/tags") {
            val tags = referenceDataService.listTags()
            val etag = computeEtag(tags.joinToString("|") { "${it.id}-${it.createdAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(tags)
        }
        get("/sampradayas") {
            val sampradayas = referenceDataService.listSampradayas()
            val etag = computeEtag(sampradayas.joinToString("|") { "${it.id}-${it.createdAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(sampradayas)
        }
    }
}
