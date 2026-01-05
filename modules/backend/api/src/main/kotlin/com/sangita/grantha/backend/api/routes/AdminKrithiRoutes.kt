package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.KrithiCreateRequest
import com.sangita.grantha.backend.api.models.KrithiUpdateRequest
import com.sangita.grantha.backend.api.models.SaveKrithiSectionsRequest
import com.sangita.grantha.backend.api.models.LyricVariantCreateRequest
import com.sangita.grantha.backend.api.models.LyricVariantUpdateRequest
import com.sangita.grantha.backend.api.models.SaveLyricVariantSectionsRequest
import com.sangita.grantha.backend.api.services.KrithiService
import com.sangita.grantha.backend.api.services.TransliterationService
import com.sangita.grantha.shared.domain.model.TransliterationRequest
import com.sangita.grantha.shared.domain.model.TransliterationResponse
import com.sangita.grantha.shared.domain.model.ValidateKrithiRequest
import com.sangita.grantha.shared.domain.model.ValidationResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.adminKrithiRoutes(
    krithiService: KrithiService,
    transliterationService: TransliterationService
) {
    route("/v1/admin/krithis") {
        post {
            val request = call.receive<KrithiCreateRequest>()
            val created = krithiService.createKrithi(request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "krithiId")
                ?: return@put call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<KrithiUpdateRequest>()
            val updated = krithiService.updateKrithi(id, request)
            call.respond(updated)
        }

        // AI-powered admin routes
        post("/{id}/transliterate") {
                 val id = parseUuidParam(call.parameters["id"], "krithiId")
                 // We expect the client to send the content to be transliterated.
                 // If the client sends empty content, we could fetch from DB (future enhancement),
                 // but for now we assume the editor sends the current draft state.
                 val request = call.receive<TransliterationRequest>()
                 
                 val result = transliterationService.transliterate(
                     content = request.content,
                     sourceScript = request.sourceScript,
                     targetScript = request.targetScript
                 )
                 
                 call.respond(TransliterationResponse(result, request.targetScript))
             }

        post("/{id}/validate") {
                 val id = parseUuidParam(call.parameters["id"], "krithiId")
                 val request = call.receiveNullable<ValidateKrithiRequest>() ?: ValidateKrithiRequest()
                 
                 // TODO: Implement actual validation logic (Phase 4)
                 // For now, return a stub response
                 call.respond(ValidationResult(isValid = true, issues = listOf("Validation service not yet fully implemented")))
             }

        get("/{id}/sections") {
                 val id = parseUuidParam(call.parameters["id"], "krithiId")
                     ?: return@get call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
                 val sections = krithiService.getKrithiSections(id)
                 call.respond(sections)
             }

        get("/{id}/variants") {
                 val id = parseUuidParam(call.parameters["id"], "krithiId")
                     ?: return@get call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
                 val variants = krithiService.getKrithiLyricVariants(id)
                 call.respond(variants)
             }

        post("/{id}/variants") {
                 val id = parseUuidParam(call.parameters["id"], "krithiId")
                     ?: return@post call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
                 val request = call.receive<LyricVariantCreateRequest>()
                 val created = krithiService.createLyricVariant(id, request)
                 call.respond(HttpStatusCode.Created, created)
             }

        get("/{id}/tags") {
                 val id = parseUuidParam(call.parameters["id"], "krithiId")
                     ?: return@get call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
                 val tags = krithiService.getKrithiTags(id)
                 call.respond(tags)
             }

        post("/{id}/sections") {
                 val id = parseUuidParam(call.parameters["id"], "krithiId")
                     ?: return@post call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
                 val request = call.receive<SaveKrithiSectionsRequest>()
                 krithiService.saveKrithiSections(id, request.sections)
            call.respond(HttpStatusCode.NoContent)
        }
    }

    route("/v1/admin/variants") {
        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "variantId")
                ?: return@put call.respondText("Missing variant ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<LyricVariantUpdateRequest>()
            val updated = krithiService.updateLyricVariant(id, request)
            call.respond(updated)
        }

        post("/{id}/sections") {
            val id = parseUuidParam(call.parameters["id"], "variantId")
                ?: return@post call.respondText("Missing variant ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<SaveLyricVariantSectionsRequest>()
            krithiService.saveLyricVariantSections(id, request.sections)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
