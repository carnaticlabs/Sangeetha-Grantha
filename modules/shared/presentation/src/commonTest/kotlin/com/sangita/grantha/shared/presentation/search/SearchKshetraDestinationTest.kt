package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.navigation.RasikaDestination
import com.sangita.grantha.shared.presentation.navigation.destination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** TRACK-151 Phase 2: KSHETRA search cards open the kshetra detail now that it exists. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchKshetraDestinationTest {
    @Test
    fun kshetraCardIsKeptByDefaultAndOpensTheKshetraDetail() = runTest {
        val presenter = SearchPresenter(
            catalogue = CatalogueRepository(FixtureCatalogueApi()),
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
        presenter.onQueryChange("kanyakumari")
        presenter.submit()
        advanceUntilIdle()

        val hit = presenter.state.value.macroHits.single()
        assertEquals(
            RasikaDestination.KshetraDetail(CatalogueFixtures.KANYAKUMARI_TEMPLE_ID),
            hit.destination(),
        )
    }

    @Test
    fun collectionCardStillOpensTheCollection() = runTest {
        val presenter = SearchPresenter(
            catalogue = CatalogueRepository(FixtureCatalogueApi()),
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
        presenter.onQueryChange("navavarnam")
        presenter.submit()
        advanceUntilIdle()

        assertEquals(
            RasikaDestination.CollectionDetail(CatalogueFixtures.KAMALAMBA_SLUG),
            presenter.state.value.macroHits.single().destination(),
        )
    }
}
