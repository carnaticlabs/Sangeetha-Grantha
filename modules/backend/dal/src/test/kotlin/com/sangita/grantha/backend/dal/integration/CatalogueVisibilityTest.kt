package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.models.CatalogueVisibility
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSelectionDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogueVisibilityTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `v1 hides unestablished published compositions and v2 includes them`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Known Form Alpha",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            musicalForm = MusicalForm.KRITHI,
        )
        val unknown = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Unknown Form Alpha",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            musicalForm = MusicalForm.UNESTABLISHED,
        )

        val v1 = dal.catalogue.searchKrithis("Alpha", null, null, 0, 30, CatalogueVisibility.V1)
        assertEquals(1, v1.total)
        assertEquals(listOf("Known Form Alpha"), v1.items.map { it.title })
        assertNull(dal.catalogue.findPublishedReader(unknown.id, CatalogueVisibility.V1))

        val v2 = dal.catalogue.searchKrithis("Alpha", null, null, 0, 30, CatalogueVisibility.V2)
        assertEquals(2, v2.total)
        assertTrue(v2.items.any { it.musicalForm == MusicalFormDto.UNESTABLISHED })
        assertEquals("Unknown Form Alpha", dal.catalogue.findPublishedReader(unknown.id, CatalogueVisibility.V2)?.title)

        val discovery = dal.catalogue.findDiscovery(CatalogueVisibility.V2)
        assertEquals(CatalogueSelectionDto.CATALOGUE_ORDER, discovery.feature?.selection)
        assertEquals("From the collection", discovery.feature?.heading)
    }

    @Test
    fun `v1 reference counts exclude unestablished memberships`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        val raga = dal.ragas.create(name = "Visibility-Count-Raga")
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Count Known",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(raga.id.toJavaUuid()),
            primaryRagaId = raga.id.toJavaUuid(),
            musicalForm = MusicalForm.KRITHI,
        )
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Count Unknown",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(raga.id.toJavaUuid()),
            primaryRagaId = raga.id.toJavaUuid(),
            musicalForm = MusicalForm.UNESTABLISHED,
        )
        val v1 = dal.catalogue.findRaga(raga.id, CatalogueVisibility.V1)
        val v2 = dal.catalogue.findRaga(raga.id, CatalogueVisibility.V2)
        assertEquals(1, v1?.publishedCompositionCount)
        assertEquals(2, v2?.publishedCompositionCount)
    }

    @Test
    fun `explicit krithi classification is not rewritten by the unestablished default`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        val created = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Preserved Krithi Form",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            musicalForm = MusicalForm.KRITHI,
        )
        assertEquals(MusicalFormDto.KRITHI, created.musicalForm)
        assertEquals(
            MusicalFormDto.KRITHI,
            dal.catalogue.findPublishedReader(created.id, CatalogueVisibility.V1)?.musicalForm,
        )
    }
}
