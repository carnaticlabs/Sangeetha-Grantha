package com.sangita.grantha.backend.api.services

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class CatalogueUsageEvent(
    val eventId: String,
    val timestamp: String,
    val environment: String,
    val route: String,
    val action: String,
    val status: Int,
    val elapsedMs: Long,
    val sessionId: String? = null,
    val interactionId: String? = null,
    val resultCount: Long? = null,
)

class CatalogueUsageRecorder(
    private val environment: String = "dev",
    private val sink: (String) -> Unit = { usageLogger.info(it) },
) {
    private val dropped = AtomicLong(0)

    fun droppedCount(): Long = dropped.get()

    fun record(
        route: String,
        status: Int,
        elapsedMs: Long,
        sessionHeader: String?,
        interactionHeader: String?,
        resultCount: Long? = null,
    ) {
        val event = CatalogueUsageEvent(
            eventId = UUID.randomUUID().toString(),
            timestamp = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString(),
            environment = environment,
            route = routeCategory(route),
            action = actionFor(route),
            status = status,
            elapsedMs = elapsedMs,
            sessionId = parseAnalyticsUuid(sessionHeader),
            interactionId = parseAnalyticsUuid(interactionHeader),
            resultCount = resultCount,
        )
        try {
            sink(json.encodeToString(event))
        } catch (_: Exception) {
            dropped.incrementAndGet()
        }
    }

    companion object {
        private val usageLogger = LoggerFactory.getLogger("catalogue-usage")
        private val json = Json { encodeDefaults = true; explicitNulls = false }

        fun parseAnalyticsUuid(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            return runCatching { Uuid.parse(raw.trim()).toString() }.getOrNull()
        }

        fun routeCategory(path: String): String = when {
            path.contains("/lyrics/") -> "catalogue.lyrics"
            path.startsWith(CatalogueContract.KRITHIS_PATH) && path != CatalogueContract.KRITHIS_PATH ->
                "catalogue.krithi"
            path == CatalogueContract.KRITHIS_PATH -> "catalogue.krithis"
            path.startsWith(CatalogueContract.RAGAS_PATH) && path != CatalogueContract.RAGAS_PATH ->
                "catalogue.raga"
            path.startsWith(CatalogueContract.RAGAS_PATH) -> "catalogue.ragas"
            path.startsWith(CatalogueContract.COMPOSERS_PATH) && path != CatalogueContract.COMPOSERS_PATH ->
                "catalogue.composer"
            path.startsWith(CatalogueContract.COMPOSERS_PATH) -> "catalogue.composers"
            else -> "catalogue.other"
        }

        fun actionFor(path: String): String = when (routeCategory(path)) {
            "catalogue.krithis" -> "search"
            "catalogue.krithi" -> "reader"
            "catalogue.lyrics" -> "lyrics"
            "catalogue.ragas", "catalogue.raga" -> "raga_directory"
            "catalogue.composers", "catalogue.composer" -> "composer_directory"
            else -> "other"
        }
    }
}
