package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import com.sangita.grantha.shared.mobile.usage.MobileSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** TRACK-159 D8: the filter sheet could reach only the first 30 of 1,014 ragas. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchFilterPickerTest {
    private val ragas = (1..70).map { n ->
        CatalogueRagaSummaryDto(
            id = Uuid.parse("00000000-0000-0000-0000-${n.toString().padStart(12, '0')}"),
            name = if (n % 10 == 0) "Kalyani $n" else "Raga $n",
            publishedCompositionCount = 1,
        )
    }
    private val composers = (1..65).map { n ->
        CatalogueComposerSummaryDto(
            id = Uuid.parse("00000000-0000-0000-0001-${n.toString().padStart(12, '0')}"),
            name = "Composer $n",
            publishedCompositionCount = 1,
        )
    }
    private val ragaQueries = mutableListOf<Pair<String?, Int>>()

    private inner class PagedApi(private val gate: CompletableDeferred<Unit>? = null) : CatalogueApi by FixtureCatalogueApi() {
        override suspend fun searchRagas(query: String?, page: Int, pageSize: Int, interaction: InteractionContext): CataloguePagedResponse<CatalogueRagaSummaryDto> {
            ragaQueries += query to page
            if (query == "slow") withContext(NonCancellable) { gate?.await() }
            val matching = ragas.filter { query.isNullOrBlank() || it.name.contains(query, ignoreCase = true) }
            return CataloguePagedResponse(matching.drop(page * pageSize).take(pageSize), matching.size.toLong(), page, pageSize)
        }

        override suspend fun searchComposers(query: String?, page: Int, pageSize: Int, interaction: InteractionContext): CataloguePagedResponse<CatalogueComposerSummaryDto> =
            CataloguePagedResponse(composers.drop(page * pageSize).take(pageSize), composers.size.toLong(), page, pageSize)
    }

    private fun TestScope.presenter(api: CatalogueApi = PagedApi()) = SearchPresenter(
        catalogue = CatalogueRepository(api),
        session = MobileSession(clockMs = { 0L }),
        scope = this,
    )

    @Test
    fun openingTheSheetLoadsTheFirstPageAndReportsHowManyMoreExist() = runTest {
        val presenter = presenter()
        presenter.openFilters()
        advanceUntilIdle()
        val state = presenter.state.value
        assertEquals(SearchPresenter.PAGE_SIZE, state.filterRagas.size)
        assertEquals(70L, state.filterRagasTotal)
        assertTrue(state.hasMoreFilterRagas)
        assertEquals(65L, state.filterComposersTotal)
        assertTrue(state.hasMoreFilterComposers)
    }

    @Test
    fun loadMoreAppendsUntilEveryRagaIsReachableAndThenStops() = runTest {
        val presenter = presenter()
        presenter.openFilters()
        advanceUntilIdle()
        presenter.loadMoreFilterRagas(); advanceUntilIdle()
        presenter.loadMoreFilterRagas(); advanceUntilIdle()
        assertEquals(70, presenter.state.value.filterRagas.size)
        assertEquals(ragas.map { it.id }, presenter.state.value.filterRagas.map { it.id })
        assertFalse(presenter.state.value.hasMoreFilterRagas)
        val calls = ragaQueries.size
        presenter.loadMoreFilterRagas(); advanceUntilIdle()
        assertEquals(calls, ragaQueries.size, "no request once everything is loaded")
    }

    @Test
    fun searchingReplacesTheListWithPageZeroOfTheMatchesAndLoadMoreKeepsTheQuery() = runTest {
        val presenter = presenter()
        presenter.openFilters()
        advanceUntilIdle()
        presenter.onFilterRagaTextChange("  kalyani ")
        presenter.searchFilterRagas()
        advanceUntilIdle()
        val state = presenter.state.value
        assertEquals("kalyani", state.filterRagaQuery)
        assertEquals(7, state.filterRagas.size)
        assertTrue(state.filterRagas.all { it.name.startsWith("Kalyani") })
        assertEquals(7L, state.filterRagasTotal)
        assertFalse(state.hasMoreFilterRagas)
        assertEquals("kalyani" to 0, ragaQueries.last())
    }

    @Test
    fun aBlankSearchRestoresTheFullDirectory() = runTest {
        val presenter = presenter()
        presenter.openFilters(); advanceUntilIdle()
        presenter.searchFilterRagas("kalyani"); advanceUntilIdle()
        presenter.searchFilterRagas(""); advanceUntilIdle()
        assertEquals(70L, presenter.state.value.filterRagasTotal)
        assertEquals(SearchPresenter.PAGE_SIZE, presenter.state.value.filterRagas.size)
        assertEquals("", presenter.state.value.filterRagaQuery)
    }

    @Test
    fun aLateResponseForAnEarlierSearchCannotReplaceTheNewerOne() = runTest {
        val gate = CompletableDeferred<Unit>()
        val presenter = presenter(PagedApi(gate))
        presenter.openFilters(); advanceUntilIdle()
        presenter.searchFilterRagas("slow"); runCurrent()
        presenter.searchFilterRagas("kalyani"); advanceUntilIdle()
        val newer = presenter.state.value.filterRagas
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(newer, presenter.state.value.filterRagas)
        assertEquals("kalyani", presenter.state.value.filterRagaQuery)
    }

    @Test
    fun aNoMatchSearchEmptiesTheListWithoutAnError() = runTest {
        val presenter = presenter()
        presenter.openFilters(); advanceUntilIdle()
        presenter.searchFilterRagas("zzzz"); advanceUntilIdle()
        assertTrue(presenter.state.value.filterRagas.isEmpty())
        assertEquals(0L, presenter.state.value.filterRagasTotal)
    }

    @Test
    fun theChosenRagaSurvivesASearchThatDoesNotListIt() = runTest {
        val presenter = presenter()
        presenter.openFilters(); advanceUntilIdle()
        val chosen = ragas[3]
        presenter.draftRaga(chosen.id, chosen.name)
        presenter.searchFilterRagas("kalyani"); advanceUntilIdle()
        assertEquals(chosen.id, presenter.state.value.draftFacets.ragaId)
        assertFalse(presenter.state.value.filterRagas.any { it.id == chosen.id })
        presenter.applyFilters()
        assertEquals(chosen.name, presenter.state.value.appliedFacets.ragaLabel)
    }

    @Test
    fun reopeningTheSheetResetsAStaleRagaSearch() = runTest {
        val presenter = presenter()
        presenter.openFilters(); advanceUntilIdle()
        presenter.searchFilterRagas("kalyani"); advanceUntilIdle()
        presenter.cancelFilters()
        presenter.openFilters(); advanceUntilIdle()
        assertEquals("", presenter.state.value.filterRagaQuery)
        assertEquals(70L, presenter.state.value.filterRagasTotal)
    }

    @Test
    fun composersPageToo() = runTest {
        val presenter = presenter()
        presenter.openFilters(); advanceUntilIdle()
        presenter.loadMoreFilterComposers(); advanceUntilIdle()
        presenter.loadMoreFilterComposers(); advanceUntilIdle()
        assertEquals(65, presenter.state.value.filterComposers.size)
        assertFalse(presenter.state.value.hasMoreFilterComposers)
    }

    @Test
    fun aFailedSearchKeepsThePreviousList() = runTest {
        val failing = object : CatalogueApi by PagedApi() {
            override suspend fun searchRagas(query: String?, page: Int, pageSize: Int, interaction: InteractionContext): CataloguePagedResponse<CatalogueRagaSummaryDto> {
                if (query == "boom") throw CatalogueFailure.Unavailable(serverMessage = "down")
                return PagedApi().searchRagas(query, page, pageSize, interaction)
            }
        }
        val presenter = presenter(failing)
        presenter.openFilters(); advanceUntilIdle()
        val before = presenter.state.value.filterRagas
        presenter.searchFilterRagas("boom"); advanceUntilIdle()
        assertEquals(before, presenter.state.value.filterRagas)
    }
}
