package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.config.Environment
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors(env: ApiEnvironment) {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowNonSimpleContentTypes = true
        allowCredentials = true

        val allowedOrigins = env.corsAllowedOrigins.toMutableSet()
        if (env.environment == Environment.DEV) {
            allowedOrigins.add("http://localhost:${env.frontendPort}")
            allowedOrigins.add("http://127.0.0.1:${env.frontendPort}")
        }

        allowOrigins { origin -> allowedOrigins.contains(origin) }
    }
}
