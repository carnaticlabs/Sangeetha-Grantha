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

class ThinOverviewFactsTest {
    private val temple = com.sangita.grantha.backend.dal.repositories.TempleSearchFacts(
        templeId = java.util.UUID.randomUUID(),
        name = "Varadaraja Perumal Temple",
        place = "Kanchipuram",
        publishedKrithiCount = 5,
        recordedGap = false,
        mandalam = "TONDAI",
        sthalaVriksha = "Athi",
        sthalaTirtha = "Ananta Sarovar",
        deityPosture = "STHANAKA",
        composerNames = listOf("Muthuswami Dikshitar"),
    )

    @Test
    fun `a thin overview gains the recorded facts in the approved wording`() {
        assertEquals(
            "Hastigiri Varadaraja Perumal Divya Desam at Kanchipuram. In the Tondai region. " +
                "Sacred water: Ananta Sarovar; sacred tree: Athi; standing deity. " +
                "5 kritis recorded here, by Muthuswami Dikshitar.",
            RasikaSearchService.thinOverviewWithFacts("Hastigiri Varadaraja Perumal Divya Desam at Kanchipuram.", temple),
        )
    }

    @Test
    fun `only recorded facts are used and singular and multiple composers read naturally`() {
        val bare = temple.copy(
            sthalaVriksha = null, sthalaTirtha = null, deityPosture = null, publishedKrithiCount = 1,
            composerNames = listOf("A", "B", "C"), mandalam = "UTTARA",
        )
        assertEquals(
            "Badrinath shrine in the Himalayas. In the Uttara region. 1 kriti recorded here, by A, B and C.",
            RasikaSearchService.thinOverviewWithFacts("Badrinath shrine in the Himalayas.", bare),
        )
        val none = bare.copy(mandalam = null, publishedKrithiCount = 0, composerNames = emptyList())
        assertEquals("Only a line.", RasikaSearchService.thinOverviewWithFacts("Only a line.", none))
    }

    @Test
    fun `a longer overview is left alone`() {
        val long = "A".repeat(60)
        assertEquals(long, RasikaSearchService.thinOverviewWithFacts(long, temple))
    }
}
