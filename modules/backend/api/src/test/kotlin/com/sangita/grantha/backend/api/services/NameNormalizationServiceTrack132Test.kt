package com.sangita.grantha.backend.api.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * TRACK-132: ITRANS internal capitals must fold to the same raga key as the
 * Wikipedia-form spelling after [NameNormalizationService.normalizeRaga].
 * Space stripping is intentional; the DAL then matches the spaced seed key.
 */
class NameNormalizationServiceTrack132Test {
    private val service = NameNormalizationService()

    @Test
    fun `ITRANS Yadukula Kambhoji matches Wikipedia form`() {
        assertEquals(
            service.normalizeRaga("Yadukula Kāmbhoji"),
            service.normalizeRaga("yadukula kAmbhOji"),
        )
        assertEquals("yadukulakambhoji", service.normalizeRaga("yadukula kAmbhOji"))
    }

    @Test
    fun `ITRANS Deva Manohari matches Wikipedia form`() {
        assertEquals(
            service.normalizeRaga("Deva Manohari"),
            service.normalizeRaga("dEva manOhari"),
        )
        assertEquals("devamanohari", service.normalizeRaga("dEva manOhari"))
    }

    @Test
    fun `Kanadā and Kannada stay distinct`() {
        assertEquals("kanada", service.normalizeRaga("Kanadā"))
        assertEquals("kannada", service.normalizeRaga("Kannada"))
    }
}
