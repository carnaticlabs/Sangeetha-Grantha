package com.sangita.grantha.backend.api.services

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CatalogueUsageRecorderTest {
    @Test
    fun `malformed analytics headers are ignored without dropping the event`() {
        val lines = mutableListOf<String>()
        val recorder = CatalogueUsageRecorder(environment = "test", sink = { lines += it })
        recorder.record(
            route = CatalogueContract.KRITHIS_PATH,
            status = 200,
            elapsedMs = 12,
            sessionHeader = "not-a-uuid",
            interactionHeader = "also-bad",
            resultCount = 0,
        )
        assertEquals(1, lines.size)
        val body = Json.parseToJsonElement(lines.single()).jsonObject
        assertEquals("search", body["action"]?.jsonPrimitive?.content)
        assertNull(body["sessionId"])
        assertEquals(0, recorder.droppedCount())
    }

    @Test
    fun `valid headers are retained and sink failures do not throw`() {
        val recorder = CatalogueUsageRecorder(environment = "test", sink = { error("boom") })
        recorder.record(
            route = "${CatalogueContract.KRITHIS_PATH}/11111111-1111-4111-8111-111111111111",
            status = 200,
            elapsedMs = 9,
            sessionHeader = "11111111-1111-4111-8111-111111111111",
            interactionHeader = "22222222-2222-4222-8222-222222222222",
        )
        assertEquals(1, recorder.droppedCount())
    }

    @Test
    fun `lyrics stay a separate action from reader views`() {
        assertEquals(
            "lyrics",
            CatalogueUsageRecorder.actionFor(
                "${CatalogueContract.KRITHIS_PATH}/11111111-1111-4111-8111-111111111111/lyrics/22222222-2222-4222-8222-222222222222",
            ),
        )
        assertEquals(
            "reader",
            CatalogueUsageRecorder.actionFor(
                "${CatalogueContract.KRITHIS_PATH}/11111111-1111-4111-8111-111111111111",
            ),
        )
        assertTrue(CatalogueUsageRecorder.parseAnalyticsUuid("  ") == null)
    }
}
