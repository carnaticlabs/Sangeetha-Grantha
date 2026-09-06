package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogueSearchTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `search is published-only and ignores drafts`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Published Alpha",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            talaId = seed.tala.id.toJavaUuid(),
            workflowState = WorkflowState.PUBLISHED,
        )
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Draft Alpha",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            talaId = seed.tala.id.toJavaUuid(),
            workflowState = WorkflowState.DRAFT,
        )

        val page = dal.catalogue.searchKrithis(
            query = "Alpha",
            composerId = null,
            ragaId = null,
            page = 0,
            pageSize = 30,
        )
        assertEquals(1, page.total)
        assertEquals(listOf("Published Alpha"), page.items.map { it.title })
    }

    @Test
    fun `raga filter matches junction membership not only primary FK`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        val secondary = dal.ragas.create(name = "Catalogue-Secondary-Raga")
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Junction Member",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid(), secondary.id.toJavaUuid()),
            talaId = seed.tala.id.toJavaUuid(),
            primaryRagaId = seed.raga.id.toJavaUuid(),
            isRagamalika = true,
        )
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Primary Only",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            talaId = seed.tala.id.toJavaUuid(),
        )

        val page = dal.catalogue.searchKrithis(
            query = null,
            composerId = null,
            ragaId = secondary.id.toJavaUuid(),
            page = 0,
            pageSize = 30,
        )
        assertEquals(1, page.total)
        assertEquals("Junction Member", page.items.single().title)
        assertEquals(2, page.items.single().ragas.size)
    }

    @Test
    fun `ragamalika preserves repeated membership and orderIndex`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        val other = dal.ragas.create(name = "Catalogue-Repeat-Raga")
        val ragaId = seed.raga.id.toJavaUuid()
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Repeated Raga",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = emptyList(),
            talaId = seed.tala.id.toJavaUuid(),
            isRagamalika = true,
            ragaSlots = listOf(0 to ragaId, 1 to other.id.toJavaUuid(), 2 to ragaId),
            primaryRagaId = ragaId,
        )

        val page = dal.catalogue.searchKrithis(
            query = "Repeated Raga",
            composerId = null,
            ragaId = null,
            page = 0,
            pageSize = 10,
        )
        val ragas = page.items.single().ragas
        assertEquals(listOf(0, 1, 2), ragas.map { it.orderIndex })
        assertEquals(2, ragas.count { it.id == seed.raga.id })
    }

    @Test
    fun `pages distinct compositions in title then UUID order`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Zeta Page",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Alpha Page",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Mu Page",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )

        val first = dal.catalogue.searchKrithis("Page", null, null, page = 0, pageSize = 2)
        val second = dal.catalogue.searchKrithis("Page", null, null, page = 1, pageSize = 2)
        assertEquals(3, first.total)
        assertEquals(listOf("Alpha Page", "Mu Page"), first.items.map { it.title })
        assertEquals(listOf("Zeta Page"), second.items.map { it.title })
        assertTrue(first.items.none { item -> second.items.any { it.id == item.id } })
    }

    @Test
    fun `composer alias query finds published works`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Alias Query Krithi",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        val page = dal.catalogue.searchKrithis("thyagaraja", null, null, 0, 30)
        assertTrue(page.items.any { it.title == "Alias Query Krithi" })
    }
}
