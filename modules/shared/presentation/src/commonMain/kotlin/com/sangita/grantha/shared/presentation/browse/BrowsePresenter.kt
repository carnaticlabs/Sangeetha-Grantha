package com.sangita.grantha.shared.presentation.browse

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetKeyDto
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.network.LakshanaFacetSelection
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

/** One selectable value in a lakshana facet; [count] is null until the picker asked for counts. */
data class LakshanaFacetValueUi(val value: String, val label: String, val count: Long? = null)

data class LakshanaFacetUi(val key: CatalogueFacetKeyDto, val values: List<LakshanaFacetValueUi>)

data class BrowseUiState(
    val directory: BrowseDirectory = BrowseDirectory.Ragas,
    val query: String = "",
    val ragas: List<CatalogueRagaSummaryDto> = emptyList(),
    val composers: List<CatalogueComposerSummaryDto> = emptyList(),
    val associated: List<CatalogueKrithiSummaryDto> = emptyList(),
    val selectedRagaId: Uuid? = null,
    val selectedComposerId: Uuid? = null,
    val facets: List<LakshanaFacetUi> = emptyList(),
    val facetSelection: LakshanaFacetSelection = LakshanaFacetSelection.NONE,
    /** Title when Browse was opened from a reader chip with no composer or raga. */
    val facetTitle: String? = null,
    val openFacet: CatalogueFacetKeyDto? = null,
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
            it.copy(
                directory = directory,
                associated = emptyList(),
                selectedRagaId = null,
                selectedComposerId = null,
                facets = emptyList(),
                facetSelection = LakshanaFacetSelection.NONE,
                facetTitle = null,
                openFacet = null,
            )
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

    /** Re-scope to [composerId] carrying [selection]; values the new scope does not offer are dropped. */
    internal fun openComposerWith(composerId: Uuid, selection: LakshanaFacetSelection) =
        loadAssociated(composerId = composerId, selection = selection)

    /** A reader chip: Browse with this one facet, composer and raga cleared (TRACK-153). */
    fun openFacet(key: CatalogueFacetKeyDto, value: String, label: String) =
        loadAssociated(selection = LakshanaFacetSelection.NONE.with(key, value), title = label)

    /** Apply [value] for [key], or clear it when it is already the selection. */
    fun onFacetValue(key: CatalogueFacetKeyDto, value: String) {
        val current = _state.value
        val next = if (current.facetSelection[key] == value) {
            current.facetSelection.without(key)
        } else {
            current.facetSelection.with(key, value)
        }
        _state.update { it.copy(openFacet = null) }
        loadAssociated(
            ragaId = current.selectedRagaId,
            composerId = current.selectedComposerId,
            selection = next,
            title = current.facetTitle,
        )
    }

    fun onClearFacet(key: CatalogueFacetKeyDto) {
        val current = _state.value
        loadAssociated(
            ragaId = current.selectedRagaId,
            composerId = current.selectedComposerId,
            selection = current.facetSelection.without(key),
            title = current.facetTitle,
        )
    }

    /** Opens a facet's picker and only then asks the server for counts. */
    fun onOpenFacetPicker(key: CatalogueFacetKeyDto) {
        val snapshot = _state.value
        _state.update { it.copy(openFacet = key) }
        val interaction = session.beginInteraction()
        scope.launch {
            try {
                val list = catalogue.listFacets(
                    composerId = snapshot.selectedComposerId,
                    ragaId = snapshot.selectedRagaId,
                    facets = snapshot.facetSelection,
                    counts = true,
                    interaction = interaction,
                )
                _state.update { current ->
                    if (current.openFacet != key || current.facetSelection != snapshot.facetSelection) {
                        current
                    } else {
                        current.copy(facets = list.facets.map { it.toUi() })
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: CatalogueFailure) {
                // The picker still lists the values without counts.
            }
        }
    }

    fun onCloseFacetPicker() {
        _state.update { it.copy(openFacet = null) }
    }

    private fun loadAssociated(
        ragaId: Uuid? = null,
        composerId: Uuid? = null,
        selection: LakshanaFacetSelection = LakshanaFacetSelection.NONE,
        title: String? = null,
    ) {
        val requested = ++generation
        job?.cancel()
        val interaction = session.beginInteraction()
        _state.update {
            it.copy(
                load = LoadState.Loading,
                selectedRagaId = ragaId,
                selectedComposerId = composerId,
                facetSelection = selection,
                facetTitle = title,
                associated = emptyList(),
            )
        }
        job = scope.launch {
            try {
                val offered = offeredFacets(ragaId, composerId, selection, interaction)
                // A selected value the scope no longer offers is dropped before searching.
                val effective = if (offered == null) selection else selection.retainOffered(offered)
                val page = catalogue.searchKrithis(
                    composerId = composerId,
                    ragaId = ragaId,
                    facets = effective,
                    interaction = interaction,
                )
                if (requested != generation) return@launch
                _state.update {
                    it.copy(
                        associated = page.items,
                        facets = offered.orEmpty(),
                        facetSelection = effective,
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

    /** Facets for the scope, or null when the server cannot say; the list then simply has no facet row. */
    private suspend fun offeredFacets(
        ragaId: Uuid?,
        composerId: Uuid?,
        selection: LakshanaFacetSelection,
        interaction: com.sangita.grantha.shared.mobile.usage.InteractionContext,
    ): List<LakshanaFacetUi>? = try {
        catalogue.listFacets(
            composerId = composerId,
            ragaId = ragaId,
            facets = selection,
            interaction = interaction,
        ).facets.map { it.toUi() }
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: CatalogueFailure) {
        null
    }

    fun retry() {
        val state = _state.value
        when {
            state.selectedRagaId != null || state.selectedComposerId != null || !state.facetSelection.isEmpty ->
                loadAssociated(
                    ragaId = state.selectedRagaId,
                    composerId = state.selectedComposerId,
                    selection = state.facetSelection,
                    title = state.facetTitle,
                )
            else -> loadDirectory()
        }
    }
}

private fun com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetDto.toUi() =
    LakshanaFacetUi(key, values.map { LakshanaFacetValueUi(it.value, it.label ?: it.value, it.count) })

private fun LakshanaFacetSelection.retainOffered(offered: List<LakshanaFacetUi>): LakshanaFacetSelection =
    LakshanaFacetSelection(
        values.filter { (key, value) ->
            offered.firstOrNull { it.key == key }?.values?.any { it.value == value } == true
        },
    )
