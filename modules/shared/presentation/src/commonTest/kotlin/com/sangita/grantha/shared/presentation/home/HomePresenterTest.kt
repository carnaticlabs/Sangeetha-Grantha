package com.sangita.grantha.shared.presentation.home

import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomePresenterTest {
    @Test
    fun featureLoadDoesNotBlockSearchDraft() = runTest {
        val presenter = HomePresenter(
            catalogue = CatalogueRepository(FixtureCatalogueApi()),
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
        presenter.onQueryChange("Vatapi")
        presenter.loadIfNeeded()
        advanceUntilIdle()
        assertEquals("Vatapi", presenter.state.value.query)
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.feature?.krithi?.id)
        assertEquals(LoadState.Idle, presenter.state.value.featureLoad)
        presenter.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.feature?.krithi?.id)
    }

    @Test
    fun failedFeatureLeavesQueryUsable() = runTest {
        val presenter = HomePresenter(
            catalogue = CatalogueRepository(
                object : CatalogueApi by FixtureCatalogueApi() {
                    override suspend fun getDiscovery(interaction: InteractionContext) =
                        throw CatalogueFailure.Unavailable()
                },
            ),
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
        presenter.onQueryChange("Endaro")
        presenter.loadIfNeeded()
        advanceUntilIdle()
        assertEquals("Endaro", presenter.state.value.query)
        assertNull(presenter.state.value.feature)
        assertIs<LoadState.Error>(presenter.state.value.featureLoad)
        assertTrue((presenter.state.value.featureLoad as LoadState.Error).retryable)
    }
}
