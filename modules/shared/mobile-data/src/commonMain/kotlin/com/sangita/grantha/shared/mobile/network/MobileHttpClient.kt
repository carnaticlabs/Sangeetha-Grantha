package com.sangita.grantha.shared.mobile.network

import com.sangita.grantha.shared.mobile.config.MobileApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createMobileHttpClient(
    engine: HttpClientEngine,
    config: MobileApiConfig,
): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) {
        json(catalogueJson)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = config.requestTimeoutMs
        connectTimeoutMillis = config.connectTimeoutMs
        socketTimeoutMillis = config.requestTimeoutMs
    }
    defaultRequest {
        url(config.baseUrl)
        contentType(ContentType.Application.Json)
        headers.append(HttpHeaders.CacheControl, "no-store")
    }
}

val catalogueJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    prettyPrint = false
}
