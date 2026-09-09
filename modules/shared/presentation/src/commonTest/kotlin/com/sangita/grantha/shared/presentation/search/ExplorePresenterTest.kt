package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.explore.ExploreCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorePresenterTest {
    @Test
    fun categorySwitchUsesCommittedQueryNotDraft() = runTest {
        val presenter = presenter()
        presenter.onQueryChange("Sri")
        presenter.submit()
        advanceUntilIdle()
        assertEquals(LoadState.Empty, presenter.state.value.load)
        presenter.onQueryChange("untyped draft")
        presenter.selectCategory(ExploreCategory.Ragas)
        advanceUntilIdle()
        assertEquals("Sri", presenter.state.value.committedQuery)
        assertEquals("untyped draft", presenter.state.value.query)
        assertEquals(ExploreCategory.Ragas, presenter.state.value.category)
        assertEquals(1, presenter.state.value.ragaItems.size)
        assertEquals("Sri", presenter.state.value.ragaItems.single().name)
    }

    @Test
    fun homeSearchClearsFacetsAndSelectsKrithis() = runTest {
        val presenter = presenter()
        presenter.draftRaga(CatalogueFixtures.hamsadhvaniId, "Hamsadhvani")
        presenter.applyFilters()
        advanceUntilIdle()
        assertEquals(CatalogueFixtures.hamsadhvaniId, presenter.state.value.appliedFacets.ragaId)
        presenter.applyCommittedQuery("Endaro")
        advanceUntilIdle()
        assertEquals(ExploreCategory.Krithis, presenter.state.value.category)
        assertTrue(presenter.state.value.appliedFacets.isEmpty)
        assertEquals("Endaro", presenter.state.value.committedQuery)
        assertEquals(CatalogueFixtures.endaroId, presenter.state.value.items.single().id)
    }

    @Test
    fun removingChipCommitsImmediately() = runTest {
        val presenter = presenter()
        presenter.draftComposer(CatalogueFixtures.tyagarajaId, "Tyagaraja")
        presenter.applyFilters()
        advanceUntilIdle()
        assertEquals(CatalogueFixtures.endaroId, presenter.state.value.items.single().id)
        presenter.removeAppliedComposer()
        advanceUntilIdle()
        assertEquals(null, presenter.state.value.appliedFacets.composerId)
        assertEquals(2, presenter.state.value.items.size)
    }

    @Test
    fun failedNextPageKeepsLoadedItems() = runTest {
        val presenter = SearchPresenter(
            catalogue = CatalogueRepository(
                object : CatalogueApi by FixtureCatalogueApi() {
                    override suspend fun searchKrithis(
                        query: String?,
                        composerId: Uuid?,
                        ragaId: Uuid?,
                        page: Int,
                        pageSize: Int,
                        interaction: InteractionContext,
                    ): CataloguePagedResponse<CatalogueKrithiSummaryDto> {
                        if (page >= 1) throw CatalogueFailure.Unavailable()
                        return CataloguePagedResponse(
                            items = listOf(CatalogueFixtures.vatapiSummary),
                            total = 40,
                            page = 0,
                            pageSize = pageSize,
                        )
                    }
                },
            ),
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
        presenter.submit()
        advanceUntilIdle()
        assertEquals(1, presenter.state.value.items.size)
        presenter.loadNextPage()
        advanceUntilIdle()
        assertEquals(1, presenter.state.value.items.size)
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.items.single().id)
        assertIs<LoadState.Error>(presenter.state.value.nextPageLoad)
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun openDirectoryLoadsRagasWithoutPriorQuery() = runTest {
        val presenter = presenter()
        presenter.openDirectory(ExploreCategory.Ragas)
        advanceUntilIdle()
        assertEquals(ExploreCategory.Ragas, presenter.state.value.category)
        assertEquals("", presenter.state.value.committedQuery)
        assertEquals(2, presenter.state.value.ragaItems.size)
        assertTrue(
            presenter.state.value.ragaItems.any { it.parentMelakartaNumber == 22 },
        )
    }

    private fun kotlinx.coroutines.test.TestScope.presenter() = SearchPresenter(
        catalogue = CatalogueRepository(FixtureCatalogueApi()),
        session = MobileSession(clockMs = { 0L }),
        scope = this,
    )
}
