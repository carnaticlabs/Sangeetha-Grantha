package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.SemanticSearchResponse
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class SearchPresenterTest {
    @Test
    fun committedSearchLoadsFixtureMatches() = runTest {
        val presenter = presenter()
        assertEquals(KrithiSearchMode.Hybrid, presenter.state.value.mode)
        presenter.selectMode(KrithiSearchMode.Lexical)
        presenter.onQueryChange("Vatapi")
        presenter.submit()
        advanceUntilIdle()
        assertEquals(KrithiSearchMode.Lexical, presenter.state.value.mode)
        assertEquals(1, presenter.state.value.items.size)
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.items.single().id)
        assertTrue(presenter.state.value.discoveryItems.isEmpty())
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun hybridSubmitLoadsDiscoveryMatchesAndIgnoresFacets() = runTest {
        val api = RecordingCatalogueApi()
        val presenter = presenter(api)
        presenter.onQueryChange("Vatapi")
        presenter.draftComposer(CatalogueFixtures.tyagarajaId, "Tyagaraja")
        presenter.draftRaga(CatalogueFixtures.hamsadhvaniId, "Hamsadhvani")
        presenter.applyFilters()
        presenter.submit()
        advanceUntilIdle()
        val request = api.hybridRequests.last()
        assertEquals("Vatapi", request.query)
        assertNull(request.composerId)
        assertNull(request.ragaId)
        assertEquals(SearchPresenter.DISCOVERY_LIMIT, request.limit)
        assertEquals(CatalogueFixtures.tyagarajaId, presenter.state.value.appliedFacets.composerId)
        assertTrue(presenter.state.value.items.isEmpty())
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.discoveryItems.single().krithiId)
        assertEquals(CatalogueFixtures.VATAPI_RRF, presenter.state.value.discoveryItems.single().rrfScore)
        assertEquals(false, presenter.state.value.hasMore)
        assertEquals(0L, presenter.state.value.total)
        assertEquals(LoadState.Idle, presenter.state.value.load)
        assertEquals(0, api.lexicalCalls)
    }

    @Test
    fun semanticSubmitLoadsSimilarityWithoutFusionScores() = runTest {
        val api = RecordingCatalogueApi()
        val presenter = presenter(api)
        presenter.selectMode(KrithiSearchMode.Semantic)
        presenter.onQueryChange("Vatapi")
        presenter.submit()
        advanceUntilIdle()
        val request = api.semanticRequests.single()
        assertEquals("Vatapi", request.query)
        assertNull(request.composerId)
        assertNull(request.ragaId)
        assertEquals(30, request.limit)
        val item = presenter.state.value.discoveryItems.single()
        assertEquals(CatalogueFixtures.vatapiId, item.krithiId)
        assertNull(item.rrfScore)
        assertNull(item.lexicalScore)
        assertEquals(CatalogueFixtures.VATAPI_SIMILARITY, item.similarityScore)
        assertEquals(false, presenter.state.value.hasMore)
        assertTrue(api.hybridRequests.isEmpty())
        assertEquals(0, api.lexicalCalls)
    }

    @Test
    fun blankHybridQueryStaysEmptyAndDoesNotBrowseCatalogue() = runTest {
        val api = RecordingCatalogueApi()
        val presenter = presenter(api)
        presenter.onQueryChange("   ")
        presenter.submit()
        advanceUntilIdle()
        assertEquals("", api.hybridRequests.single().query)
        assertEquals(30, api.hybridRequests.single().limit)
        assertTrue(presenter.state.value.discoveryItems.isEmpty())
        assertTrue(presenter.state.value.items.isEmpty())
        assertEquals(LoadState.Empty, presenter.state.value.load)
        assertEquals(0, api.lexicalCalls)
    }

    @Test
    fun emptyLexicalQueryCanMiss() = runTest {
        val presenter = presenter()
        presenter.selectMode(KrithiSearchMode.Lexical)
        presenter.onQueryChange("zzzz-not-a-kriti")
        presenter.submit()
        advanceUntilIdle()
        assertTrue(presenter.state.value.items.isEmpty())
        assertEquals(LoadState.Empty, presenter.state.value.load)
    }

    @Test
    fun newerHybridQuerySupersedesOlder() = runTest {
        val presenter = presenter()
        presenter.onQueryChange("Endaro")
        presenter.submit()
        presenter.onQueryChange("Vatapi")
        presenter.submit()
        advanceUntilIdle()
        assertEquals("Vatapi Ganapatim", presenter.state.value.discoveryItems.single().title)
        assertTrue(presenter.state.value.items.isEmpty())
    }

    @Test
    fun modeChangeRefetchesCommittedQueryAndClearsTheOtherRows() = runTest {
        val presenter = presenter()
        presenter.selectMode(KrithiSearchMode.Lexical)
        presenter.onQueryChange("Vatapi")
        presenter.submit()
        advanceUntilIdle()
        assertEquals(1, presenter.state.value.items.size)
        presenter.selectMode(KrithiSearchMode.Hybrid)
        advanceUntilIdle()
        assertTrue(presenter.state.value.items.isEmpty())
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.discoveryItems.single().krithiId)
        assertEquals(false, presenter.state.value.hasMore)
    }

    @Test
    fun switchingAwayKeepsModeAndRefetchesIt() = runTest {
        val presenter = presenter()
        presenter.selectMode(KrithiSearchMode.Semantic)
        presenter.onQueryChange("Vatapi")
        presenter.submit()
        advanceUntilIdle()
        presenter.selectCategory(ExploreCategory.Ragas)
        advanceUntilIdle()
        assertEquals(KrithiSearchMode.Semantic, presenter.state.value.mode)
        assertEquals(ExploreCategory.Ragas, presenter.state.value.category)
        assertEquals("Vatapi", presenter.state.value.committedQuery)
        presenter.selectCategory(ExploreCategory.Krithis)
        advanceUntilIdle()
        assertEquals(KrithiSearchMode.Semantic, presenter.state.value.mode)
        assertNull(presenter.state.value.discoveryItems.single().rrfScore)
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.discoveryItems.single().krithiId)
    }

    @Test
    fun applyCommittedQueryFollowsCurrentMode() = runTest {
        val api = RecordingCatalogueApi()
        val presenter = presenter(api)
        presenter.selectMode(KrithiSearchMode.Semantic)
        presenter.applyCommittedQuery("Endaro")
        advanceUntilIdle()
        assertEquals(ExploreCategory.Krithis, presenter.state.value.category)
        assertEquals(KrithiSearchMode.Semantic, presenter.state.value.mode)
        assertEquals("Endaro", api.semanticRequests.single().query)
        assertNull(api.semanticRequests.single().composerId)
        assertNull(api.semanticRequests.single().ragaId)
        assertEquals(CatalogueFixtures.endaroId, presenter.state.value.discoveryItems.single().krithiId)
        assertTrue(presenter.state.value.items.isEmpty())
        assertEquals(0, api.lexicalCalls)
    }

    @Test
    fun discoveryFailureIsRetryableAndNotAnEmptyList() = runTest {
        val presenter = SearchPresenter(
            catalogue = CatalogueRepository(
                object : CatalogueApi by FixtureCatalogueApi() {
                    override suspend fun searchHybrid(
                        request: SemanticSearchRequest,
                        interaction: InteractionContext,
                    ): SemanticSearchResponse {
                        throw CatalogueFailure.Unavailable(serverMessage = "No compatible embedding profile.")
                    }
                },
            ),
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
        presenter.onQueryChange("Vatapi")
        presenter.submit()
        advanceUntilIdle()
        val load = presenter.state.value.load
        assertTrue(load is LoadState.Error)
        assertEquals("No compatible embedding profile.", load.message)
        assertEquals(true, load.retryable)
        assertTrue(presenter.state.value.discoveryItems.isEmpty())
        assertTrue(presenter.state.value.items.isEmpty())
    }

    @Test
    fun changingModeBeforeSubmitDoesNotFetch() = runTest {
        val api = RecordingCatalogueApi()
        val presenter = presenter(api)
        presenter.selectMode(KrithiSearchMode.Semantic)
        presenter.selectMode(KrithiSearchMode.Lexical)
        advanceUntilIdle()
        assertEquals(KrithiSearchMode.Lexical, presenter.state.value.mode)
        assertTrue(api.hybridRequests.isEmpty())
        assertTrue(api.semanticRequests.isEmpty())
        assertEquals(0, api.lexicalCalls)
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    private fun kotlinx.coroutines.test.TestScope.presenter(
        api: CatalogueApi = FixtureCatalogueApi(),
    ) = SearchPresenter(
        catalogue = CatalogueRepository(api),
        session = MobileSession(clockMs = { 0L }),
        scope = this,
    )
}

private class RecordingCatalogueApi : CatalogueApi by FixtureCatalogueApi() {
    private val fixture = FixtureCatalogueApi()
    val hybridRequests = mutableListOf<SemanticSearchRequest>()
    val semanticRequests = mutableListOf<SemanticSearchRequest>()
    var lexicalCalls: Int = 0

    override suspend fun searchKrithis(
        query: String?,
        composerId: Uuid?,
        ragaId: Uuid?,
        page: Int,
        pageSize: Int,
        interaction: InteractionContext,
    ) = fixture.searchKrithis(query, composerId, ragaId, page, pageSize, interaction).also {
        lexicalCalls += 1
    }

    override suspend fun searchHybrid(
        request: SemanticSearchRequest,
        interaction: InteractionContext,
    ): SemanticSearchResponse {
        hybridRequests += request
        return fixture.searchHybrid(request, interaction)
    }

    override suspend fun searchSemantic(
        request: SemanticSearchRequest,
        interaction: InteractionContext,
    ): SemanticSearchResponse {
        semanticRequests += request
        return fixture.searchSemantic(request, interaction)
    }
}
