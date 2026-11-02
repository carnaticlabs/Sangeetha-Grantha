package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.models.CatalogueLike
import com.sangita.grantha.backend.dal.models.CatalogueVisibility
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** TRACK-159 D7: "Titles & lyrics" must match stored lyrics, not only titles, composers and ragas. */
class CatalogueLyricSearchTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    private suspend fun krithiWithLyrics(
        title: String,
        lyrics: String,
        script: ScriptCode = ScriptCode.LATIN,
        state: WorkflowState = WorkflowState.PUBLISHED,
    ): String {
        val seed = TestFixtures.seedReferenceData(dal)
        val krithi = CatalogueTestFixtures.createKrithi(
            dal, title = title, composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()), workflowState = state,
        )
        CatalogueTestFixtures.createVariant(dal, krithi.id, lyrics, language = LanguageCode.SA, script = script, isPrimary = true)
        return title
    }

    private suspend fun titlesFor(query: String, visibility: CatalogueVisibility = CatalogueVisibility.ALL_PUBLISHED): List<String> =
        dal.catalogue.searchKrithis(query, null, null, 0, 30, visibility).items.map { it.title }

    @Test
    fun `a phrase from the middle of a lyric finds the krithi, ignoring case`() = runTest {
        krithiWithLyrics("Lyric Alpha", "pallavi line one\nnIvE(y)anucunu bhakti kUDina\ncharanam text")
        krithiWithLyrics("Unrelated Beta", "something else entirely")

        assertEquals(listOf("Lyric Alpha"), titlesFor("nIvE(y)anucunu bhakti"))
        assertEquals(listOf("Lyric Alpha"), titlesFor("NIVE(Y)ANUCUNU BHAKTI"))
        assertEquals(1L, dal.catalogue.searchKrithis("anucunu", null, null, 0, 30, CatalogueVisibility.ALL_PUBLISHED).total)
    }

    @Test
    fun `a phrase that spans a line break still matches`() = runTest {
        krithiWithLyrics("Lyric Gamma", "first line ends here\nsecond line begins there")
        assertEquals(listOf("Lyric Gamma"), titlesFor("ends here second line"))
        assertEquals(listOf("Lyric Gamma"), titlesFor("ends   here\n  second"))
    }

    @Test
    fun `lyrics in another script are searched in that script`() = runTest {
        krithiWithLyrics("Lyric Tamil", "வாதாபி கணபதிம் பஜேஹம்", script = ScriptCode.TAMIL)
        assertEquals(listOf("Lyric Tamil"), titlesFor("கணபதிம்"))
        assertEquals(emptyList(), titlesFor("ganapatim"))
    }

    @Test
    fun `wildcard characters in the query are literal`() = runTest {
        krithiWithLyrics("Lyric Delta", "plain words only")
        assertEquals(emptyList(), titlesFor("pl%ain"))
        assertEquals(emptyList(), titlesFor("pl_in"))
        assertEquals(CatalogueLike.lyricPhrasePattern("a b"), "%a_b%")
        assertEquals(CatalogueLike.lyricPhrasePattern("50%_x"), "%50\\%\\_x%")
    }

    @Test
    fun `a draft's lyrics never match for the published catalogue`() = runTest {
        krithiWithLyrics("Lyric Draft", "secret draft phrase here", state = WorkflowState.DRAFT)
        assertEquals(emptyList(), titlesFor("secret draft phrase"))
        assertEquals(emptyList(), titlesFor("secret draft phrase", CatalogueVisibility.LEGACY_KNOWN_FORMS))
        assertEquals(listOf("Lyric Draft"), dal.catalogue.previewing().searchKrithis("secret draft phrase", null, null, 0, 30, CatalogueVisibility.ALL_PUBLISHED).items.map { it.title })
    }

    @Test
    fun `title, composer and raga matching and title ordering are unchanged`() = runTest {
        krithiWithLyrics("Zeta Title Match", "unrelated words")
        krithiWithLyrics("Alpha Title Match", "other words")
        assertEquals(listOf("Alpha Title Match", "Zeta Title Match"), titlesFor("Title Match"))
    }

    @Test
    fun `a krithi with several matching variants is counted once`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        val krithi = CatalogueTestFixtures.createKrithi(dal, title = "Lyric Multi", composerId = seed.composer.id.toJavaUuid(), ragaIds = listOf(seed.raga.id.toJavaUuid()))
        CatalogueTestFixtures.createVariant(dal, krithi.id, "common phrase here", script = ScriptCode.LATIN, isPrimary = true)
        CatalogueTestFixtures.createVariant(dal, krithi.id, "common phrase here too", script = ScriptCode.DEVANAGARI)
        val page = dal.catalogue.searchKrithis("common phrase", null, null, 0, 30, CatalogueVisibility.ALL_PUBLISHED)
        assertEquals(1L, page.total)
        assertEquals(1, page.items.size)
    }
}
