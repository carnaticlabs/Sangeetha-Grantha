package com.sangita.grantha.backend.api.clients

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class GeminiApiClientTest {

    @Test
    fun `escapeNewlinesInsideJsonStrings replaces raw newlines`() {
        val client = GeminiApiClient("test-key", "http://localhost")
        val input = "{\"lyrics\":\"line1\nline2\"}"
        val escaped = client.escapeNewlinesInsideJsonStrings(input)

        assertTrue(escaped.contains("\\n"))
        assertFalse(escaped.contains("\nline2"))
    }
}
