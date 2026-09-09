package com.sangita.grantha.shared.presentation.home

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDiscoveryFeatureDto
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

data class HomeUiState(
    val query: String = "",
    val feature: CatalogueDiscoveryFeatureDto? = null,
    val featureLoad: LoadState = LoadState.Idle,
)

class HomePresenter(
    private val catalogue: CatalogueRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private var generation = 0
    private var job: Job? = null
    private var attempted = false

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun loadIfNeeded() {
        if (attempted) return
        attempted = true
        refreshFeature()
    }

    fun retryFeature() = refreshFeature()

    private fun refreshFeature() {
        val requested = ++generation
        job?.cancel()
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update { it.copy(featureLoad = LoadState.Loading) }
            try {
                val discovery = catalogue.getDiscovery(interaction)
                if (requested != generation) return@launch
                _state.update {
                    it.copy(feature = discovery.feature, featureLoad = LoadState.Idle)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update {
                    it.copy(
                        feature = null,
                        featureLoad = LoadState.Error(
                            failure.userMessage(),
                            retryable = failure !is CatalogueFailure.NotFound,
                        ),
                    )
                }
            }
        }
    }
}
