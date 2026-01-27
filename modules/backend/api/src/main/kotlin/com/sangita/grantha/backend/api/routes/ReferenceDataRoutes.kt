package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.*
import com.sangita.grantha.backend.api.services.IReferenceDataService
import com.sangita.grantha.backend.api.support.computeEtag
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.referenceDataRoutes(service: IReferenceDataService) {
    route("/v1/reference") {
        get("/stats") {
            call.respond(service.getStats())
        }
    }

    // Admin Composers CRUD
    route("/v1/admin/composers") {
        get {
            val composers = service.listComposers()
            val etag = computeEtag(composers.joinToString("|") { "${it.id}-${it.updatedAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(composers)
        }

        get("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "composerId")
                ?: return@get call.respondText("Missing composer ID", status = HttpStatusCode.BadRequest)
            val composer = service.getComposer(id)
            if (composer == null) {
                call.respondText("Composer not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(composer)
            }
        }

        post {
            val request = call.receive<ComposerCreateRequest>()
            val created = service.createComposer(request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "composerId")
                ?: return@put call.respondText("Missing composer ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<ComposerUpdateRequest>()
            val updated = service.updateComposer(id, request)
            if (updated == null) {
                call.respondText("Composer not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(updated)
            }
        }

        delete("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "composerId")
                ?: return@delete call.respondText("Missing composer ID", status = HttpStatusCode.BadRequest)
            val deleted = service.deleteComposer(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("Composer not found", status = HttpStatusCode.NotFound)
            }
        }
    }

    // Admin Ragas CRUD
    route("/v1/admin/ragas") {
        get {
            val ragas = service.listRagas()
            val etag = computeEtag(ragas.joinToString("|") { "${it.id}-${it.updatedAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(ragas)
        }

        get("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "ragaId")
                ?: return@get call.respondText("Missing raga ID", status = HttpStatusCode.BadRequest)
            val raga = service.getRaga(id)
            if (raga == null) {
                call.respondText("Raga not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(raga)
            }
        }

        post {
            val request = call.receive<RagaCreateRequest>()
            val created = service.createRaga(request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "ragaId")
                ?: return@put call.respondText("Missing raga ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<RagaUpdateRequest>()
            val updated = service.updateRaga(id, request)
            if (updated == null) {
                call.respondText("Raga not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(updated)
            }
        }

        delete("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "ragaId")
                ?: return@delete call.respondText("Missing raga ID", status = HttpStatusCode.BadRequest)
            val deleted = service.deleteRaga(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("Raga not found", status = HttpStatusCode.NotFound)
            }
        }
    }

    // Admin Talas CRUD
    route("/v1/admin/talas") {
        get {
            val talas = service.listTalas()
            val etag = computeEtag(talas.joinToString("|") { "${it.id}-${it.updatedAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(talas)
        }

        get("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "talaId")
                ?: return@get call.respondText("Missing tala ID", status = HttpStatusCode.BadRequest)
            val tala = service.getTala(id)
            if (tala == null) {
                call.respondText("Tala not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(tala)
            }
        }

        post {
            val request = call.receive<TalaCreateRequest>()
            val created = service.createTala(request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "talaId")
                ?: return@put call.respondText("Missing tala ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<TalaUpdateRequest>()
            val updated = service.updateTala(id, request)
            if (updated == null) {
                call.respondText("Tala not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(updated)
            }
        }

        delete("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "talaId")
                ?: return@delete call.respondText("Missing tala ID", status = HttpStatusCode.BadRequest)
            val deleted = service.deleteTala(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("Tala not found", status = HttpStatusCode.NotFound)
            }
        }
    }

    // Admin Temples CRUD
    route("/v1/admin/temples") {
        get {
            val temples = service.listTemples()
            val etag = computeEtag(temples.joinToString("|") { "${it.id}-${it.updatedAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(temples)
        }

        get("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "templeId")
                ?: return@get call.respondText("Missing temple ID", status = HttpStatusCode.BadRequest)
            val temple = service.getTemple(id)
            if (temple == null) {
                call.respondText("Temple not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(temple)
            }
        }

        post {
            val request = call.receive<TempleCreateRequest>()
            val created = service.createTemple(request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "templeId")
                ?: return@put call.respondText("Missing temple ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<TempleUpdateRequest>()
            val updated = service.updateTemple(id, request)
            if (updated == null) {
                call.respondText("Temple not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(updated)
            }
        }

        delete("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "templeId")
                ?: return@delete call.respondText("Missing temple ID", status = HttpStatusCode.BadRequest)
            val deleted = service.deleteTemple(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("Temple not found", status = HttpStatusCode.NotFound)
            }
        }
    }

    // Admin Deities CRUD
    route("/v1/admin/deities") {
        get {
            val deities = service.listDeities()
            val etag = computeEtag(deities.joinToString("|") { "${it.id}-${it.updatedAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(deities)
        }

        get("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "deityId")
                ?: return@get call.respondText("Missing deity ID", status = HttpStatusCode.BadRequest)
            val deity = service.getDeity(id)
            if (deity == null) {
                call.respondText("Deity not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(deity)
            }
        }

        post {
            val request = call.receive<DeityCreateRequest>()
            val created = service.createDeity(request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "deityId")
                ?: return@put call.respondText("Missing deity ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<DeityUpdateRequest>()
            val updated = service.updateDeity(id, request)
            if (updated == null) {
                call.respondText("Deity not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(updated)
            }
        }

        delete("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "deityId")
                ?: return@delete call.respondText("Missing deity ID", status = HttpStatusCode.BadRequest)
            val deleted = service.deleteDeity(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("Deity not found", status = HttpStatusCode.NotFound)
            }
        }
    }

    // Admin tag management routes
    route("/v1/admin/tags") {
        get {
            val tags = service.listTags()
            val etag = computeEtag(tags.joinToString("|") { "${it.id}-${it.createdAt}" })
            call.response.headers.append(HttpHeaders.ETag, "\"$etag\"")
            call.respond(tags)
        }

        get("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "tagId")
                ?: return@get call.respondText("Missing tag ID", status = HttpStatusCode.BadRequest)
            val tag = service.getTag(id)
            if (tag == null) {
                call.respondText("Tag not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(tag)
            }
        }

        post {
            val request = call.receive<TagCreateRequest>()
            val created = service.createTag(request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "tagId")
                ?: return@put call.respondText("Missing tag ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<TagUpdateRequest>()
            val updated = service.updateTag(id, request)
            if (updated == null) {
                call.respondText("Tag not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(updated)
            }
        }

        delete("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "tagId")
                ?: return@delete call.respondText("Missing tag ID", status = HttpStatusCode.BadRequest)
            val deleted = service.deleteTag(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("Tag not found", status = HttpStatusCode.NotFound)
            }
        }
    }
}
