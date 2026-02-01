package com.sangita.grantha.backend.api.services.scraping

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScrapeJsonSanitizerTest {

    @Test
    fun `removes invalid section entries`() {
        val raw = """
            {
              "title": "Test",
              "sections": [
                {"type": "PALLAVI", "text": "line"},
                {"type": "", "text": ""},
                {"text": "missing type"}
              ]
            }
        """.trimIndent()

        val cleaned = ScrapeJsonSanitizer.sanitizeScrapedKrithiJson(raw)
        assertTrue(cleaned.contains("PALLAVI"))
        assertFalse(cleaned.contains("missing type"))
    }
}
