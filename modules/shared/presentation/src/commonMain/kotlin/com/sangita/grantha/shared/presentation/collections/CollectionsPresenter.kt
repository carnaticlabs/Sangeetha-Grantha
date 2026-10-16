package com.sangita.grantha.shared.presentation.collections

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionSummaryDto
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectionsUiState(
    val items: List<CatalogueCollectionSummaryDto> = emptyList(),
    val load: LoadState = LoadState.Idle,
)

/** Collection directory. Shared by the Home shelf and the full list so both load once. */
class CollectionsPresenter(
    private val catalogue: CatalogueRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(CollectionsUiState())
    val state: StateFlow<CollectionsUiState> = _state.asStateFlow()
    private var generation = 0
    private var job: Job? = null
    private var attempted = false

    fun loadIfNeeded() {
        if (attempted) return
        attempted = true
        refresh()
    }

    fun retry() = refresh()

    private fun refresh() {
        val requested = ++generation
        job?.cancel()
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update { it.copy(load = LoadState.Loading) }
            try {
                val list = catalogue.listCollections(interaction)
                if (requested != generation) return@launch
                _state.update {
                    CollectionsUiState(
                        items = list.items,
                        load = if (list.items.isEmpty()) LoadState.Empty else LoadState.Idle,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update {
                    CollectionsUiState(
                        load = LoadState.Error(failure.userMessage(), retryable = failure !is CatalogueFailure.NotFound),
                    )
                }
            }
        }
    }
}

data class CollectionDetailUiState(
    val slug: String? = null,
    val detail: CatalogueCollectionDetailDto? = null,
    val load: LoadState = LoadState.Idle,
)

class CollectionDetailPresenter(
    private val catalogue: CatalogueRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(CollectionDetailUiState())
    val state: StateFlow<CollectionDetailUiState> = _state.asStateFlow()
    private var generation = 0
    private var job: Job? = null

    fun open(slug: String) {
        val requested = ++generation
        job?.cancel()
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update { CollectionDetailUiState(slug = slug, load = LoadState.Loading) }
            try {
                val detail = catalogue.getCollection(slug, interaction)
                if (requested != generation) return@launch
                _state.update { CollectionDetailUiState(slug = slug, detail = detail, load = LoadState.Idle) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update {
                    CollectionDetailUiState(
                        slug = slug,
                        load = LoadState.Error(failure.userMessage(), retryable = failure !is CatalogueFailure.NotFound),
                    )
                }
            }
        }
    }

    fun retry() {
        _state.value.slug?.let(::open)
    }
}
