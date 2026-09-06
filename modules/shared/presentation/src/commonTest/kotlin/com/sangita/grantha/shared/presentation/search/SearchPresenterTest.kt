package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchPresenterTest {
    @Test
    fun committedSearchLoadsFixtureMatches() = runTest {
        val presenter = presenter()
        presenter.onQueryChange("Vatapi")
        presenter.submit()
        advanceUntilIdle()
        assertEquals(1, presenter.state.value.items.size)
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.items.single().id)
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun emptyQueryStillHitsCatalogueAndCanBeEmpty() = runTest {
        val presenter = presenter()
        presenter.onQueryChange("zzzz-not-a-kriti")
        presenter.submit()
        advanceUntilIdle()
        assertTrue(presenter.state.value.items.isEmpty())
        assertEquals(LoadState.Empty, presenter.state.value.load)
    }

    @Test
    fun newerQuerySupersedesOlder() = runTest {
        val presenter = presenter()
        presenter.onQueryChange("Endaro")
        presenter.submit()
        presenter.onQueryChange("Vatapi")
        presenter.submit()
        advanceUntilIdle()
        assertEquals("Vatapi Ganapatim", presenter.state.value.items.single().title)
    }

    private fun kotlinx.coroutines.test.TestScope.presenter() = SearchPresenter(
        catalogue = CatalogueRepository(FixtureCatalogueApi()),
        session = MobileSession(clockMs = { 0L }),
        scope = this,
    )
}
