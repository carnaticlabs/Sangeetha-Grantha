package com.sangita.grantha.shared.presentation.entities

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaDetailDto
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

data class EntityDetailUiState(
    val raga: CatalogueRagaDetailDto? = null,
    val composer: CatalogueComposerDetailDto? = null,
    val works: List<CatalogueKrithiSummaryDto> = emptyList(),
    val worksTotal: Long = 0,
    val load: LoadState = LoadState.Idle,
)

class EntityDetailPresenter(
    private val catalogue: CatalogueRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(EntityDetailUiState())
    val state: StateFlow<EntityDetailUiState> = _state.asStateFlow()
    private var generation = 0
    private var job: Job? = null
    private var openedRagaId: Uuid? = null
    private var openedComposerId: Uuid? = null

    fun openRaga(id: Uuid) = load(ragaId = id)

    fun openComposer(id: Uuid) = load(composerId = id)

    fun retry() {
        openedRagaId?.let { openRaga(it); return }
        openedComposerId?.let { openComposer(it) }
    }

    private fun load(ragaId: Uuid? = null, composerId: Uuid? = null) {
        val requested = ++generation
        job?.cancel()
        openedRagaId = ragaId
        openedComposerId = composerId
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update { EntityDetailUiState(load = LoadState.Loading) }
            try {
                val detailRaga = ragaId?.let { catalogue.getRaga(it, interaction) }
                val detailComposer = composerId?.let { catalogue.getComposer(it, interaction) }
                if (requested != generation) return@launch
                val works = catalogue.searchKrithis(
                    ragaId = ragaId,
                    composerId = composerId,
                    interaction = interaction,
                )
                if (requested != generation) return@launch
                _state.update {
                    EntityDetailUiState(
                        raga = detailRaga,
                        composer = detailComposer,
                        works = works.items,
                        worksTotal = works.total,
                        load = LoadState.Idle,
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update {
                    EntityDetailUiState(
                        load = LoadState.Error(
                            failure.userMessage(),
                            retryable = failure !is CatalogueFailure.NotFound,
                        ),
                    )
                }
            }
        }
    }
}
