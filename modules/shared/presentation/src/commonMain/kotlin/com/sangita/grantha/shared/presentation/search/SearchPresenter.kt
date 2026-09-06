package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val items: List<CatalogueKrithiSummaryDto> = emptyList(),
    val total: Long = 0,
    val load: LoadState = LoadState.Idle,
)

class SearchPresenter(
    private val catalogue: CatalogueRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var generation: Int = 0
    private var job: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun submit() {
        val requested = ++generation
        job?.cancel()
        val query = _state.value.query
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update { it.copy(load = LoadState.Loading, items = emptyList(), total = 0) }
            try {
                val page = catalogue.searchKrithis(query = query, interaction = interaction)
                if (requested != generation) return@launch
                _state.update {
                    it.copy(
                        items = page.items,
                        total = page.total,
                        load = if (page.items.isEmpty()) LoadState.Empty else LoadState.Idle,
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update {
                    it.copy(load = LoadState.Error(failure.userMessage(), retryable = failure !is CatalogueFailure.NotFound))
                }
            }
        }
    }

    fun retry() = submit()
}
