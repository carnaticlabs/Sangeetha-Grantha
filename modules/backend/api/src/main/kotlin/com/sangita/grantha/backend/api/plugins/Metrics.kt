package com.sangita.grantha.backend.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.configureMetrics(registry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        this.registry = registry
    }
}
