package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CatalogueSearchLiteralWildcardTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `percent and underscore in the query are literals not wildcards`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Literal Underscore_Token",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "No Wildcard Marks",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )

        val all = dal.catalogue.searchKrithis(null, null, null, page = 0, pageSize = 1)
        val percent = dal.catalogue.searchKrithis("%", null, null, page = 0, pageSize = 100)
        val underscore = dal.catalogue.searchKrithis("_", null, null, page = 0, pageSize = 100)

        assertTrue(all.total > percent.total)
        assertTrue(percent.items.all { "%" in it.title })
        assertTrue(underscore.items.any { it.title == "Literal Underscore_Token" })
        assertTrue(underscore.items.all { "_" in it.title })
        assertTrue(underscore.items.none { it.title == "No Wildcard Marks" })
        assertTrue(all.total > underscore.total)
    }
}
