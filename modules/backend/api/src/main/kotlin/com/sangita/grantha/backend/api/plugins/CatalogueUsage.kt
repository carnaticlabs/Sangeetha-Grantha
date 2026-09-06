package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.backend.api.services.CatalogueUsageRecorder
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.path
import org.koin.ktor.ext.inject

fun Application.configureCatalogueUsage() {
    val recorder by inject<CatalogueUsageRecorder>()
    intercept(ApplicationCallPipeline.Monitoring) {
        val path = call.request.path()
        if (!path.startsWith("/v1/catalogue")) {
            return@intercept
        }
        val started = System.nanoTime()
        try {
            proceed()
        } finally {
            val elapsedMs = (System.nanoTime() - started) / 1_000_000L
            val status = call.response.status()?.value ?: 0
            recorder.record(
                route = path,
                status = status,
                elapsedMs = elapsedMs,
                sessionHeader = call.request.header(CatalogueContract.SESSION_HEADER),
                interactionHeader = call.request.header(CatalogueContract.INTERACTION_HEADER),
            )
        }
    }
}
