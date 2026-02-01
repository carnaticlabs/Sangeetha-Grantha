package com.sangita.grantha.backend.api.services.scraping

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HtmlTextExtractorTest {

    @Test
    fun `extracts main content and preserves structure`() {
        val html = """
            <html>
              <head><title>Sample Krithi</title></head>
              <body>
                <nav>Navigation</nav>
                <div class="post-body">
                  <p>Pallavi</p>
                  <p>Line 1<br>Line 2</p>
                  <a href="https://example.com/lyrics">Lyrics Link</a>
                </div>
                <footer>Footer</footer>
              </body>
            </html>
        """.trimIndent()

        val extractor = HtmlTextExtractor(maxChars = 10_000)
        val result = extractor.extract(html, "https://example.com/page")

        assertEquals("Sample Krithi", result.title)
        assertTrue(result.text.contains("Pallavi"))
        assertTrue(result.text.contains("Line 1"))
        assertTrue(result.text.contains("Line 2"))
        assertTrue(result.text.contains("Lyrics Link (https://example.com/lyrics)"))
        assertFalse(result.text.contains("Navigation"))
        assertFalse(result.text.contains("Footer"))
    }
}
