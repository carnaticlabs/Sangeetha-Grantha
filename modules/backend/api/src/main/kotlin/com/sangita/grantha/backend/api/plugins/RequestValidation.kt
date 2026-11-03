package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.backend.api.models.AssignRoleRequest
import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.models.KrithiCreateRequest
import com.sangita.grantha.backend.api.models.KrithiUpdateRequest
import com.sangita.grantha.backend.api.models.LyricVariantCreateRequest
import com.sangita.grantha.backend.api.models.LyricVariantUpdateRequest
import com.sangita.grantha.backend.api.models.UserCreateRequest
import com.sangita.grantha.backend.api.models.UserUpdateRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import com.sangita.grantha.backend.api.routes.PREVIEW_SEARCH_RATE_LIMIT
import com.sangita.grantha.backend.api.routes.previewRateLimitKey
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.response.respondText
import kotlin.time.Duration.Companion.minutes
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.application.install

private const val MAX_BODY_SIZE_BYTES: Long = 15 * 1024 * 1024 // 15MB
private const val GLOBAL_RATE_LIMIT = 100
internal const val PREVIEW_SEARCH_LIMIT_PER_MINUTE = 20

private class BodySizeLimitConfig {
    var maxBytes: Long = MAX_BODY_SIZE_BYTES
}

private val BodySizeLimit = createApplicationPlugin("BodySizeLimit", ::BodySizeLimitConfig) {
    val maxBytes = pluginConfig.maxBytes
    onCall { call ->
        val length = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (length != null && length > maxBytes) {
            call.respondText("Request body too large", status = HttpStatusCode.PayloadTooLarge)
            return@onCall
        }
    }
}

fun Application.configureRequestValidation() {
    install(BodySizeLimit) {
        maxBytes = MAX_BODY_SIZE_BYTES
    }

    install(RateLimit) {
        // TRACK-158: a curator's preview searches each may embed a query (a paid call), so they get their own
        // per-user allowance on top of the global per-IP one. Repeated queries are served from the query-vector cache.
        register(PREVIEW_SEARCH_RATE_LIMIT) {
            rateLimiter(limit = PREVIEW_SEARCH_LIMIT_PER_MINUTE, refillPeriod = 1.minutes)
            requestKey { call -> call.previewRateLimitKey() }
        }
        global {
            rateLimiter(limit = GLOBAL_RATE_LIMIT, refillPeriod = 1.minutes)
            requestKey { call -> call.request.local.remoteHost }
        }
    }

    install(RequestValidation) {
        validate<KrithiCreateRequest> { request ->
            when {
                request.title.isBlank() -> ValidationResult.Invalid("title must not be blank")
                request.composerId.isBlank() -> ValidationResult.Invalid("composerId must not be blank")
                else -> ValidationResult.Valid
            }
        }

        validate<KrithiUpdateRequest> { request ->
            if (request.title != null && request.title.isBlank()) {
                ValidationResult.Invalid("title must not be blank")
            } else {
                ValidationResult.Valid
            }
        }

        validate<LyricVariantCreateRequest> { request ->
            if (request.lyrics.isBlank()) {
                ValidationResult.Invalid("lyrics must not be blank")
            } else {
                ValidationResult.Valid
            }
        }

        validate<LyricVariantUpdateRequest> { request ->
            if (request.lyrics != null && request.lyrics.isBlank()) {
                ValidationResult.Invalid("lyrics must not be blank")
            } else {
                ValidationResult.Valid
            }
        }

        validate<ImportKrithiRequest> { request ->
            if (request.source.isBlank()) {
                ValidationResult.Invalid("source must not be blank")
            } else {
                ValidationResult.Valid
            }
        }

        validate<ImportReviewRequest> { request ->
            if (request.status.name.isBlank()) {
                ValidationResult.Invalid("status must not be blank")
            } else {
                ValidationResult.Valid
            }
        }

        validate<UserCreateRequest> { request ->
            when {
                request.fullName.isBlank() -> ValidationResult.Invalid("fullName must not be blank")
                request.email != null && !request.email.contains("@") -> ValidationResult.Invalid("email must be valid")
                else -> ValidationResult.Valid
            }
        }

        validate<UserUpdateRequest> { request ->
            if (request.email != null && !request.email.contains("@")) {
                ValidationResult.Invalid("email must be valid")
            } else {
                ValidationResult.Valid
            }
        }

        validate<AssignRoleRequest> { request ->
            if (request.roleCode.isBlank()) {
                ValidationResult.Invalid("roleCode must not be blank")
            } else {
                ValidationResult.Valid
            }
        }
    }
}
