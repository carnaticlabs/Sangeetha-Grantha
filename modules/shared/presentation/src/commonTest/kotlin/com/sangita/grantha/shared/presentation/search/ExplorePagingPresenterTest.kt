package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.MobileSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorePagingPresenterTest {
    @Test
    fun loadNextPageAppendsTheSecondPageFromThePagingFixture() = runTest {
        val presenter = SearchPresenter(
            catalogue = CatalogueRepository(FixtureCatalogueApi(includePagingPages = true)),
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
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
}
