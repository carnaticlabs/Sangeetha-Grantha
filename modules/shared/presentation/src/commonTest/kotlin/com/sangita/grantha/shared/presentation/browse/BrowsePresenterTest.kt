package com.sangita.grantha.shared.presentation.browse

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
class BrowsePresenterTest {
    @Test
    fun loadDirectoryReturnsFixtureRagas() = runTest {
        val presenter = presenter()
        presenter.loadDirectory()
        advanceUntilIdle()
        assertEquals(2, presenter.state.value.ragas.size)
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun openRagaLoadsAssociatedKritis() = runTest {
        val presenter = presenter()
        presenter.openRaga(CatalogueFixtures.hamsadhvaniId)
        advanceUntilIdle()
        assertEquals(1, presenter.state.value.associated.size)
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.associated.single().id)
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun newerDirectorySwitchSupersedesOlder() = runTest {
        val presenter = presenter()
        presenter.onDirectory(BrowseDirectory.Ragas)
        presenter.onDirectory(BrowseDirectory.Composers)
        advanceUntilIdle()
        assertEquals(BrowseDirectory.Composers, presenter.state.value.directory)
        assertEquals(2, presenter.state.value.composers.size)
        assertTrue(presenter.state.value.ragas.isEmpty() || presenter.state.value.associated.isEmpty())
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    private fun kotlinx.coroutines.test.TestScope.presenter() = BrowsePresenter(
        catalogue = CatalogueRepository(FixtureCatalogueApi()),
        session = MobileSession(clockMs = { 0L }),
        scope = this,
    )
}
