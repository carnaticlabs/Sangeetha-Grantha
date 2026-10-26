package com.sangita.grantha.backend.api.services

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMacroHitDto
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RasikaSearchSnippetTest {
    private val max = CatalogueMacroHitDto.MAX_SNIPPET_LENGTH

    @Test
    fun `short text is returned whole with whitespace collapsed`() {
        assertEquals("One two. Three.", RasikaSearchService.snippetOf("  One   two.\n Three.  "))
    }

    @Test
    fun `long text is a prefix cut at a sentence end`() {
        val first = "A".repeat(150) + "."
        val text = "$first " + "B".repeat(200)
        val snippet = RasikaSearchService.snippetOf(text)
        assertEquals(first, snippet)
    }

    @Test
    fun `long text without a sentence end is cut at a word boundary within the limit`() {
        val text = (1..200).joinToString(" ") { "word$it" }
        val snippet = RasikaSearchService.snippetOf(text)
        assertTrue(snippet.length <= max)
        assertTrue(snippet.endsWith("…"))
        assertTrue(text.startsWith(snippet.removeSuffix("…")))
    }
}
