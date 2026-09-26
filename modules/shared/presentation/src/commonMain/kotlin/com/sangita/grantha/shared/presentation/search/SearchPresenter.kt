package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.SemanticSearchResultItem
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.userMessage
import com.sangita.grantha.shared.presentation.explore.ExploreCategory
import com.sangita.grantha.shared.presentation.explore.ExploreFacets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

enum class KrithiSearchMode { Lexical, Hybrid, Semantic }

data class SearchUiState(
    val query: String = "",
    val committedQuery: String = "",
    val category: ExploreCategory = ExploreCategory.Krithis,
    val mode: KrithiSearchMode = KrithiSearchMode.Hybrid,
    val draftFacets: ExploreFacets = ExploreFacets(),
    val appliedFacets: ExploreFacets = ExploreFacets(),
    val filterSheetOpen: Boolean = false,
    val searchOptionsExpanded: Boolean = false,
    val items: List<CatalogueKrithiSummaryDto> = emptyList(),
    val discoveryItems: List<SemanticSearchResultItem> = emptyList(),
    val ragaItems: List<CatalogueRagaSummaryDto> = emptyList(),
    val composerItems: List<CatalogueComposerSummaryDto> = emptyList(),
    val filterRagas: List<CatalogueRagaSummaryDto> = emptyList(),
    val filterComposers: List<CatalogueComposerSummaryDto> = emptyList(),
    val total: Long = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val load: LoadState = LoadState.Idle,
    val nextPageLoad: LoadState = LoadState.Idle,
) {
    val hasUnappliedFilters: Boolean get() = draftFacets != appliedFacets
}

class SearchPresenter(
    private val catalogue: CatalogueRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var generation: Int = 0
    private var job: Job? = null
    private var searchCommitted: Boolean = false

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun submit() {
        val committed = _state.value.query.trim()
        _state.update {
            it.copy(query = committed, committedQuery = committed, page = 0, nextPageLoad = LoadState.Idle)
        }
        fetch(reset = true)
    }

    fun applyCommittedQuery(query: String) {
        val committed = query.trim()
        _state.update {
            it.copy(
                query = committed,
                committedQuery = committed,
                category = ExploreCategory.Krithis,
                draftFacets = ExploreFacets(),
                appliedFacets = ExploreFacets(),
                filterSheetOpen = false,
                page = 0,
            )
        }
        fetch(reset = true)
    }

    fun openDirectory(category: ExploreCategory) {
        _state.update {
            it.copy(
                query = "",
                committedQuery = "",
                category = category,
                draftFacets = ExploreFacets(),
                appliedFacets = ExploreFacets(),
                filterSheetOpen = false,
                page = 0,
                nextPageLoad = LoadState.Idle,
            )
        }
        fetch(reset = true)
    }

    fun selectCategory(category: ExploreCategory) {
        if (_state.value.category == category) return
        _state.update {
            it.copy(
                category = category,
                page = 0,
                nextPageLoad = LoadState.Idle,
                filterSheetOpen = false,
            )
        }
        fetch(reset = true)
    }

    fun toggleSearchOptions() {
        _state.update { it.copy(searchOptionsExpanded = !it.searchOptionsExpanded) }
    }

    fun selectMode(mode: KrithiSearchMode) {
        val snapshot = _state.value
        if (snapshot.category != ExploreCategory.Krithis) return
        if (snapshot.mode == mode) {
            _state.update { it.copy(searchOptionsExpanded = false) }
            return
        }
        val refetch = searchCommitted
        _state.update {
            it.copy(
                mode = mode,
                searchOptionsExpanded = false,
                page = 0,
                items = emptyList(),
                discoveryItems = emptyList(),
                total = 0,
                hasMore = false,
                nextPageLoad = LoadState.Idle,
                load = if (refetch) LoadState.Loading else it.load,
            )
        }
        if (refetch) fetch(reset = true)
    }

    fun openFilters() {
        _state.update { it.copy(filterSheetOpen = true, draftFacets = it.appliedFacets) }
        loadFilterOptions()
    }

    fun cancelFilters() {
        _state.update { it.copy(filterSheetOpen = false, draftFacets = it.appliedFacets) }
    }

    fun resetDraftFilters() {
        _state.update { it.copy(draftFacets = ExploreFacets()) }
    }

    fun applyFilters() {
        _state.update {
            it.copy(
                appliedFacets = it.draftFacets,
                filterSheetOpen = false,
                page = 0,
                nextPageLoad = LoadState.Idle,
            )
        }
        fetch(reset = true)
    }

    fun draftRaga(id: Uuid?, label: String?) {
        _state.update { it.copy(draftFacets = it.draftFacets.copy(ragaId = id, ragaLabel = label)) }
    }

    fun draftComposer(id: Uuid?, label: String?) {
        _state.update { it.copy(draftFacets = it.draftFacets.copy(composerId = id, composerLabel = label)) }
    }

    fun removeAppliedRaga() {
        _state.update {
            it.copy(
                appliedFacets = it.appliedFacets.copy(ragaId = null, ragaLabel = null),
                draftFacets = it.draftFacets.copy(ragaId = null, ragaLabel = null),
                page = 0,
            )
        }
        fetch(reset = true)
    }

    fun removeAppliedComposer() {
        _state.update {
            it.copy(
                appliedFacets = it.appliedFacets.copy(composerId = null, composerLabel = null),
                draftFacets = it.draftFacets.copy(composerId = null, composerLabel = null),
                page = 0,
            )
        }
        fetch(reset = true)
    }

    fun loadNextPage() {
        val snapshot = _state.value
        if (snapshot.category == ExploreCategory.Krithis && snapshot.mode != KrithiSearchMode.Lexical) return
        if (!snapshot.hasMore || snapshot.nextPageLoad is LoadState.Loading || snapshot.load is LoadState.Loading) {
            return
        }
        val cap = pageCap(snapshot.category)
        if (cap != null && snapshot.page + 1 >= cap) return
        fetch(reset = false)
    }

    fun retry() {
        if (_state.value.nextPageLoad is LoadState.Error) {
            fetch(reset = false)
        } else {
            fetch(reset = true)
        }
    }

    private fun fetch(reset: Boolean) {
        searchCommitted = true
        val requested = ++generation
        job?.cancel()
        val snapshot = _state.value
        val page = if (reset) 0 else snapshot.page + 1
        val interaction = session.beginInteraction()
        job = scope.launch {
            if (reset) {
                _state.update {
                    it.copy(
                        load = LoadState.Loading,
                        items = emptyList(),
                        discoveryItems = emptyList(),
                        ragaItems = emptyList(),
                        composerItems = emptyList(),
                        total = 0,
                        hasMore = false,
                        nextPageLoad = LoadState.Idle,
                    )
                }
            } else {
                _state.update { it.copy(nextPageLoad = LoadState.Loading) }
            }
            try {
                when (snapshot.category) {
                    ExploreCategory.Krithis -> when (snapshot.mode) {
                        KrithiSearchMode.Lexical -> {
                            val response = catalogue.searchKrithis(
                                query = snapshot.committedQuery,
                                composerId = snapshot.appliedFacets.composerId,
                                ragaId = snapshot.appliedFacets.ragaId,
                                page = page,
                                pageSize = PAGE_SIZE,
                                interaction = interaction,
                            )
                            if (requested != generation) return@launch
                            val merged = if (reset) response.items else snapshot.items + response.items
                            val capped = merged.take(PAGE_SIZE * MAX_PAGES)
                            val loadedPages = page + 1
                            _state.update {
                                it.copy(
                                    items = capped,
                                    discoveryItems = emptyList(),
                                    total = response.total,
                                    page = page,
                                    hasMore = capped.size < response.total && loadedPages < MAX_PAGES,
                                    load = if (capped.isEmpty()) LoadState.Empty else LoadState.Idle,
                                    nextPageLoad = LoadState.Idle,
                                )
                            }
                        }
                        KrithiSearchMode.Hybrid, KrithiSearchMode.Semantic -> {
                            val request = SemanticSearchRequest(
                                query = snapshot.committedQuery,
                                composerId = null,
                                ragaId = null,
                                limit = DISCOVERY_LIMIT,
                            )
                            val response = if (snapshot.mode == KrithiSearchMode.Hybrid) {
                                catalogue.searchHybrid(request, interaction)
                            } else {
                                catalogue.searchSemantic(request, interaction)
                            }
                            if (requested != generation) return@launch
                            _state.update {
                                it.copy(
                                    items = emptyList(),
                                    discoveryItems = response.items,
                                    total = 0,
                                    page = 0,
                                    hasMore = false,
                                    load = if (response.items.isEmpty()) LoadState.Empty else LoadState.Idle,
                                    nextPageLoad = LoadState.Idle,
                                )
                            }
                        }
                    }
                    ExploreCategory.Ragas -> {
                        val response = catalogue.searchRagas(
                            query = snapshot.committedQuery,
                            page = page,
                            pageSize = PAGE_SIZE,
                            interaction = interaction,
                        )
                        if (requested != generation) return@launch
                        val merged = if (reset) response.items else snapshot.ragaItems + response.items
                        _state.update {
                            it.copy(
                                ragaItems = merged,
                                total = response.total,
                                page = page,
                                hasMore = merged.size < response.total && response.items.isNotEmpty(),
                                load = if (merged.isEmpty()) LoadState.Empty else LoadState.Idle,
                                nextPageLoad = LoadState.Idle,
                            )
                        }
                    }
                    ExploreCategory.Composers -> {
                        val response = catalogue.searchComposers(
                            query = snapshot.committedQuery,
                            page = page,
                            pageSize = PAGE_SIZE,
                            interaction = interaction,
                        )
                        if (requested != generation) return@launch
                        val merged = if (reset) response.items else snapshot.composerItems + response.items
                        val capped = merged.take(PAGE_SIZE * MAX_PAGES)
                        val loadedPages = page + 1
                        _state.update {
                            it.copy(
                                composerItems = capped,
                                total = response.total,
                                page = page,
                                hasMore = capped.size < response.total && loadedPages < MAX_PAGES,
                                load = if (capped.isEmpty()) LoadState.Empty else LoadState.Idle,
                                nextPageLoad = LoadState.Idle,
                            )
                        }
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                val error = LoadState.Error(
                    failure.userMessage(),
                    retryable = failure !is CatalogueFailure.NotFound,
                )
                if (reset) {
                    _state.update { it.copy(load = error, nextPageLoad = LoadState.Idle) }
                } else {
                    _state.update { it.copy(nextPageLoad = error) }
                }
            }
        }
    }

    private fun loadFilterOptions() {
        val existing = _state.value
        if (existing.filterRagas.isNotEmpty() && existing.filterComposers.isNotEmpty()) return
        val interaction = session.beginInteraction()
        scope.launch {
            try {
                val ragas = catalogue.searchRagas(pageSize = PAGE_SIZE, interaction = interaction)
                val composers = catalogue.searchComposers(pageSize = PAGE_SIZE, interaction = interaction)
                _state.update {
                    it.copy(filterRagas = ragas.items, filterComposers = composers.items)
                }
            } catch (_: CatalogueFailure) {
                // Pickers stay empty; Apply still works if a facet was already chosen.
            }
        }
    }

    companion object {
        const val PAGE_SIZE: Int = CatalogueContract.DEFAULT_PAGE_SIZE
        const val MAX_PAGES: Int = 3

        const val DISCOVERY_LIMIT: Int = 30

        /**
         * Maximum pages loaded per explore category.
         * Ragas are unbounded (null) so the full catalogue (1,000+ ragas) can be browsed.
         */
        fun pageCap(category: ExploreCategory): Int? = when (category) {
            ExploreCategory.Ragas -> null
            else -> MAX_PAGES
        }
    }
}
