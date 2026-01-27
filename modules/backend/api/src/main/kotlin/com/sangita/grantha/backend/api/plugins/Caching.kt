package com.sangita.grantha.backend.api.plugins

import io.ktor.http.CacheControl
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.request.path

private const val REFERENCE_DATA_CACHE_SECONDS = 3600

fun Application.configureCaching() {
    install(ConditionalHeaders)
    install(CachingHeaders) {
        options { _, _ -> null }
    }
}
