package com.sangita.grantha.shared.presentation.browse

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
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
import kotlin.uuid.Uuid

enum class BrowseDirectory { Ragas, Composers }

data class BrowseUiState(
    val directory: BrowseDirectory = BrowseDirectory.Ragas,
    val query: String = "",
    val ragas: List<CatalogueRagaSummaryDto> = emptyList(),
    val composers: List<CatalogueComposerSummaryDto> = emptyList(),
    val associated: List<CatalogueKrithiSummaryDto> = emptyList(),
    val selectedRagaId: Uuid? = null,
    val selectedComposerId: Uuid? = null,
    val load: LoadState = LoadState.Idle,
)

class BrowsePresenter(
    private val catalogue: CatalogueRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()
    private var generation = 0
    private var job: Job? = null

    fun onDirectory(directory: BrowseDirectory) {
        _state.update {
            it.copy(directory = directory, associated = emptyList(), selectedRagaId = null, selectedComposerId = null)
        }
        loadDirectory()
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun loadDirectory() {
        val requested = ++generation
        job?.cancel()
        val snapshot = _state.value
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update { it.copy(load = LoadState.Loading) }
            try {
                when (snapshot.directory) {
                    BrowseDirectory.Ragas -> {
                        val page = catalogue.searchRagas(query = snapshot.query, interaction = interaction)
                        if (requested != generation) return@launch
                        _state.update {
                            it.copy(
                                ragas = page.items,
                                load = if (page.items.isEmpty()) LoadState.Empty else LoadState.Idle,
                            )
                        }
                    }
                    BrowseDirectory.Composers -> {
                        val page = catalogue.searchComposers(query = snapshot.query, interaction = interaction)
                        if (requested != generation) return@launch
                        _state.update {
                            it.copy(
                                composers = page.items,
                                load = if (page.items.isEmpty()) LoadState.Empty else LoadState.Idle,
                            )
                        }
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update { it.copy(load = LoadState.Error(failure.userMessage())) }
            }
        }
    }

    fun openRaga(id: Uuid) = loadAssociated(ragaId = id)

    fun openComposer(id: Uuid) = loadAssociated(composerId = id)

    private fun loadAssociated(ragaId: Uuid? = null, composerId: Uuid? = null) {
        val requested = ++generation
        job?.cancel()
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update {
                it.copy(
                    load = LoadState.Loading,
                    selectedRagaId = ragaId,
                    selectedComposerId = composerId,
                    associated = emptyList(),
                )
            }
            try {
                val page = catalogue.searchKrithis(
                    composerId = composerId,
                    ragaId = ragaId,
                    interaction = interaction,
                )
                if (requested != generation) return@launch
                _state.update {
                    it.copy(
                        associated = page.items,
                        load = if (page.items.isEmpty()) LoadState.Empty else LoadState.Idle,
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update { it.copy(load = LoadState.Error(failure.userMessage())) }
            }
        }
    }

    fun retry() {
        val state = _state.value
        when {
            state.selectedRagaId != null -> openRaga(state.selectedRagaId)
            state.selectedComposerId != null -> openComposer(state.selectedComposerId)
            else -> loadDirectory()
        }
    }
}
