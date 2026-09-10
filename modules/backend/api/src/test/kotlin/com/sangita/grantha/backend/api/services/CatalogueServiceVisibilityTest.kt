package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.models.CatalogueVisibility
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDiscoveryDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import io.ktor.http.parametersOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class CatalogueServiceVisibilityTest {
    private val dal: SangitaDal = mockk(relaxed = true)
    private val service = CatalogueService(dal)

    @Test
    fun v2SearchPassesVisibilityAndDiscoveryHasNoQuery() = runTest {
        val page = CataloguePagedResponse<com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto>(
            emptyList(),
            0,
            0,
            30,
        )
        coEvery {
            dal.catalogue.searchKrithis(eq("Vatapi"), any(), isNull(), eq(0), eq(30), CatalogueVisibility.V2)
        } returns page
        coEvery { dal.catalogue.findDiscovery(CatalogueVisibility.V2) } returns CatalogueDiscoveryDto()

        val search = service.searchKrithis(parametersOf("query" to listOf("Vatapi")), CatalogueVisibility.V2)
        assertIs<CatalogueResult.Ok<*>>(search)
        coVerify(exactly = 1) {
            dal.catalogue.searchKrithis(eq("Vatapi"), any(), isNull(), eq(0), eq(30), CatalogueVisibility.V2)
        }

        val discovery = service.discovery(parametersOf())
        assertIs<CatalogueResult.Ok<CatalogueDiscoveryDto>>(discovery)

        val extra = service.discovery(parametersOf("query" to listOf("x")))
        assertIs<CatalogueResult.Invalid>(extra)
        assertTrue(extra.message.contains("query"))
    }

    @Test
    fun v1ReaderUsesKnownFormVisibility() = runTest {
        val id = Uuid.parse("66666666-6666-4666-8666-666666666666")
        coEvery { dal.catalogue.findPublishedReader(id, CatalogueVisibility.V1) } returns null
        val result = service.getKrithi(id.toString(), parametersOf(), CatalogueVisibility.V1)
        assertIs<CatalogueResult.NotFound>(result)
        coVerify { dal.catalogue.findPublishedReader(id, CatalogueVisibility.V1) }
    }
}
