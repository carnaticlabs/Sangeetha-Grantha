package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class CatalogueReaderTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `reader hides drafts and returns variant inventory`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        val published = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Reader Published",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            talaId = seed.tala.id.toJavaUuid(),
        )
        val draft = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Reader Draft",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.DRAFT,
        )
        val primary = CatalogueTestFixtures.createVariant(
            dal,
            published.id,
            lyrics = "Pallavi text only",
            isPrimary = true,
            label = "Latin primary",
        )
        CatalogueTestFixtures.createVariant(
            dal,
            published.id,
            lyrics = "देवनागरी",
            language = LanguageCode.SA,
            script = ScriptCode.DEVANAGARI,
            isPrimary = false,
            label = "Devanagari",
        )

        val reader = assertNotNull(dal.catalogue.findPublishedReader(published.id))
        assertEquals("Reader Published", reader.title)
        assertEquals(seed.composer.name, reader.composer.name)
        assertEquals(1, reader.ragas.size)
        assertEquals(primary.id, reader.defaultVariantId)
        assertEquals(2, reader.variants.size)
        assertNull(dal.catalogue.findPublishedReader(draft.id))
        assertNull(dal.catalogue.findPublishedReader(Uuid.random()))
    }

    @Test
    fun `lyrics return unsegmented text when sections are absent`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        val krithi = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Unsegmented Reader",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        val variant = CatalogueTestFixtures.createVariant(
            dal,
            krithi.id,
            lyrics = "Stored unsegmented lyrics",
            isPrimary = true,
        )

        val lyrics = assertNotNull(dal.catalogue.findPublishedLyrics(krithi.id, variant.id))
        assertEquals("Stored unsegmented lyrics", lyrics.unsegmentedText)
        assertTrue(lyrics.sections.isEmpty())
    }

    @Test
    fun `lyrics 404 for draft owner or wrong variant`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        val published = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Owner Published",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        val other = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Other Published",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        val draft = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Owner Draft",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.DRAFT,
        )
        val publishedVariant = CatalogueTestFixtures.createVariant(dal, published.id, "a", isPrimary = true)
        val otherVariant = CatalogueTestFixtures.createVariant(dal, other.id, "b", isPrimary = true)
        val draftVariant = CatalogueTestFixtures.createVariant(dal, draft.id, "c", isPrimary = true)

        assertNull(dal.catalogue.findPublishedLyrics(published.id, otherVariant.id))
        assertNull(dal.catalogue.findPublishedLyrics(draft.id, draftVariant.id))
        assertNotNull(dal.catalogue.findPublishedLyrics(published.id, publishedVariant.id))
    }

    @Test
    fun `raga directory published count uses junction`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        val extra = dal.ragas.create(name = "Catalogue-Count-Raga")
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Count Published",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid(), extra.id.toJavaUuid()),
            isRagamalika = true,
            primaryRagaId = seed.raga.id.toJavaUuid(),
        )
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Count Draft",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(extra.id.toJavaUuid()),
            workflowState = WorkflowState.DRAFT,
        )
        dal.ragas.insertAlias(extra.id, "count-alias", aliasType = "common", source = "test")

        val page = dal.catalogue.searchRagas("count-alias", 0, 30)
        val hit = page.items.single { it.id == extra.id }
        assertEquals(1, hit.publishedCompositionCount)
        assertEquals(listOf("count-alias"), hit.matchingAliases)

        val detail = assertNotNull(dal.catalogue.findRaga(extra.id))
        assertEquals(1, detail.publishedCompositionCount)
        assertTrue(detail.aliases.contains("count-alias"))
    }
}
