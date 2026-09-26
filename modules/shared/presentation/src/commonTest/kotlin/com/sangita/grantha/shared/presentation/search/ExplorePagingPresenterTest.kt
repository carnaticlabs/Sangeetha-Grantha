package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.explore.ExploreCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorePagingPresenterTest {
    @Test
    fun loadNextPageAppendsTheSecondPageFromThePagingFixture() = runTest {
        val presenter = SearchPresenter(
            catalogue = CatalogueRepository(FixtureCatalogueApi(includePagingPages = true)),
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
        presenter.selectMode(KrithiSearchMode.Lexical)
        presenter.submit()
        advanceUntilIdle()
        assertEquals(SearchPresenter.PAGE_SIZE, presenter.state.value.items.size)
        assertEquals(
            (CatalogueFixtures.summaries.size + CatalogueFixtures.PAGING_EXTRA_COUNT).toLong(),
            presenter.state.value.total,
        )
        assertTrue(presenter.state.value.hasMore)
        presenter.loadNextPage()
        advanceUntilIdle()
        assertEquals(
            CatalogueFixtures.summaries.size + CatalogueFixtures.PAGING_EXTRA_COUNT,
            presenter.state.value.items.size,
        )
        assertEquals(CatalogueFixtures.pagingLastTitle, presenter.state.value.items.last().title)
        assertEquals(false, presenter.state.value.hasMore)
    }

    @Test
    fun loadNextPageAllowsRagasToPageBeyondDefaultMaxPages() = runTest {
        val totalRagas = 150L
        val fakeApi = object : CatalogueApi by FixtureCatalogueApi() {
            override suspend fun searchRagas(
                query: String?,
                page: Int,
                pageSize: Int,
                interaction: InteractionContext,
            ): CataloguePagedResponse<CatalogueRagaSummaryDto> {
                val dummyItems = (1..pageSize).map { i ->
                    val idx = page * pageSize + i
                    CatalogueRagaSummaryDto(
                        id = Uuid.random(),
                        name = "Raga $idx",
                        matchingAliases = emptyList(),
                        publishedCompositionCount = 1,
                        melakartaNumber = null,
                        parentRagaName = null,
                        parentMelakartaNumber = null,
                    )
                }
                return CataloguePagedResponse(
                    items = dummyItems,
                    total = totalRagas,
                    page = page,
                    pageSize = pageSize,
                )
            }
        }
        val presenter = SearchPresenter(
            catalogue = CatalogueRepository(fakeApi),
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
        presenter.selectCategory(ExploreCategory.Ragas)
        presenter.submit()
        advanceUntilIdle()
        assertEquals(SearchPresenter.PAGE_SIZE, presenter.state.value.ragaItems.size)
        assertTrue(presenter.state.value.hasMore)

        // Load page 1, 2, 3 (which exceeds MAX_PAGES = 3)
        for (p in 1..3) {
            presenter.loadNextPage()
            advanceUntilIdle()
        }
        assertEquals(4 * SearchPresenter.PAGE_SIZE, presenter.state.value.ragaItems.size)
        assertTrue(presenter.state.value.hasMore)
    }
}
