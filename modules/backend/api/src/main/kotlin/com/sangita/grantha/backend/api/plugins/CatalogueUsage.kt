package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.backend.api.services.CatalogueUsageRecorder
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import org.koin.ktor.ext.inject

internal val CatalogueResultCountAttr = AttributeKey<Long>("catalogue-result-count")

fun resultCountFromPayload(payload: Any): Long? =
    (payload as? CataloguePagedResponse<*>)?.total

fun ApplicationCall.captureCatalogueResultCount(payload: Any) {
    val total = resultCountFromPayload(payload) ?: return
    attributes.put(CatalogueResultCountAttr, total)
}

fun Application.configureCatalogueUsage() {
    val recorder by inject<CatalogueUsageRecorder>()
    installCatalogueUsage(recorder)
}

fun Application.installCatalogueUsage(recorder: CatalogueUsageRecorder) {
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
                resultCount = call.attributes.getOrNull(CatalogueResultCountAttr),
            )
        }
    }
}
