package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMacroHitDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchHitKindDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchResponseDto
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** TRACK-152 Phase C: the macro group in presenter state. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchMacroPresenterTest {
    private fun TestScope.presenter(
        api: CatalogueApi = FixtureCatalogueApi(),
        kshetraDestinationAvailable: Boolean = false,
    ) = SearchPresenter(
        catalogue = CatalogueRepository(api),
        session = MobileSession(clockMs = { 0L }),
        scope = this,
        kshetraDestinationAvailable = kshetraDestinationAvailable,
    )

    private suspend fun TestScope.search(presenter: SearchPresenter, query: String) {
        presenter.onQueryChange(query)
        presenter.submit()
        advanceUntilIdle()
    }

    @Test
    fun collectionHitIsHeldBesideTheKritiItems() = runTest {
        val presenter = presenter()
        search(presenter, "navavarnam")
        val hit = presenter.state.value.macroHits.single()
        assertEquals(CatalogueSearchHitKindDto.COLLECTION, hit.kind)
        assertEquals(CatalogueFixtures.KAMALAMBA_SLUG, hit.collectionSlug)
    }

    @Test
    fun emptyMacroGroupLeavesStateUntouched() = runTest {
        val presenter = presenter()
        search(presenter, "Vatapi")
        assertTrue(presenter.state.value.macroHits.isEmpty())
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.discoveryItems.single().krithiId)
    }

    @Test
    fun kshetraHitIsDroppedUntilItsDestinationExists() = runTest {
        val dropped = presenter(kshetraDestinationAvailable = false)
        search(dropped, "kanyakumari")
        assertTrue(dropped.state.value.macroHits.isEmpty())
        assertEquals(LoadState.Empty, dropped.state.value.load)

        val kept = presenter(kshetraDestinationAvailable = true)
        search(kept, "kanyakumari")
        val hit = kept.state.value.macroHits.single()
        assertEquals(CatalogueSearchHitKindDto.KSHETRA, hit.kind)
        assertTrue(hit.recordedGap)
        assertEquals(LoadState.Idle, kept.state.value.load)
    }

    @Test
    fun aMacroOnlyResultIsNotAnEmptyScreen() = runTest {
        val presenter = presenter()
        search(presenter, "navavarnam")
        assertTrue(presenter.state.value.discoveryItems.isEmpty())
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun exactTitleMatchRendersFirstAndKeepsServerOrderForTheRest() = runTest {
        val endaro = CatalogueFixtures.discoveryItem(CatalogueFixtures.endaroSummary, hybrid = true)
        val vatapi = CatalogueFixtures.discoveryItem(CatalogueFixtures.vatapiSummary, hybrid = true)
        val api = object : CatalogueApi by FixtureCatalogueApi() {
            override suspend fun searchHybridWithCards(
                request: SemanticSearchRequest,
                interaction: InteractionContext,
            ) = CatalogueSearchResponseDto(
                query = request.query,
                totalMatches = 2,
                items = listOf(endaro, vatapi),
                exactTitleMatchId = vatapi.krithiId,
            )
        }
        val presenter = presenter(api)
        search(presenter, "Vatapi Ganapatim")
        val state = presenter.state.value
        assertEquals(vatapi.krithiId, state.exactTitleItem?.krithiId)
        assertEquals(listOf(endaro.krithiId), state.remainingDiscoveryItems.map { it.krithiId })
        assertEquals(listOf(endaro.krithiId, vatapi.krithiId), state.discoveryItems.map { it.krithiId })
    }

    @Test
    fun noExactMatchMeansNoReordering() = runTest {
        val presenter = presenter()
        search(presenter, "Vatapi")
        assertNull(presenter.state.value.exactTitleItem)
        assertEquals(presenter.state.value.discoveryItems, presenter.state.value.remainingDiscoveryItems)
    }

    @Test
    fun lexicalModeAndModeChangeClearTheMacroGroup() = runTest {
        val presenter = presenter()
        search(presenter, "navavarnam")
        assertEquals(1, presenter.state.value.macroHits.size)
        presenter.selectMode(KrithiSearchMode.Lexical)
        advanceUntilIdle()
        assertTrue(presenter.state.value.macroHits.isEmpty())
        assertNull(presenter.state.value.exactTitleMatchId)
    }

    @Test
    fun anUnknownKindIsNeverShown() = runTest {
        val api = object : CatalogueApi by FixtureCatalogueApi() {
            override suspend fun searchHybridWithCards(
                request: SemanticSearchRequest,
                interaction: InteractionContext,
            ) = CatalogueSearchResponseDto(
                query = request.query,
                totalMatches = 1,
                macroHits = listOf(
                    CatalogueMacroHitDto(
                        kind = CatalogueSearchHitKindDto.KSHETRA,
                        title = "T",
                        snippet = "s",
                        templeId = CatalogueFixtures.KANYAKUMARI_TEMPLE_ID,
                        similarityScore = 0.9,
                    ),
                ),
                items = emptyList(),
            )
        }
        val presenter = presenter(api, kshetraDestinationAvailable = false)
        search(presenter, "anything")
        assertTrue(presenter.state.value.macroHits.isEmpty())
    }
}
