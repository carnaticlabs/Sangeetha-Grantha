package com.sangita.grantha.shared.presentation.kshetra

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMandalamDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleElementDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleSummaryDto
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
import kotlin.uuid.Uuid

/** One mandalam of the list. [mandalam] is null for temples with none, shown as "Other places". */
data class KshetraGroup(
    val mandalam: CatalogueMandalamDto?,
    val items: List<CatalogueTempleSummaryDto>,
)

/**
 * Groups root temples by mandalam in declaration order, with unassigned temples last. Groups with no
 * temple are never produced, so an element filter or an unused mandalam leaves no empty header.
 * Items keep the server order inside a group (name, then id).
 */
fun groupKshetras(
    items: List<CatalogueTempleSummaryDto>,
    element: CatalogueTempleElementDto? = null,
): List<KshetraGroup> {
    val visible = if (element == null) items else items.filter { it.element == element }
    val byMandalam = visible.groupBy { it.mandalam }
    val ordered = CatalogueMandalamDto.entries.mapNotNull { m -> byMandalam[m]?.let { KshetraGroup(m, it) } }
    return ordered + listOfNotNull(byMandalam[null]?.let { KshetraGroup(null, it) })
}

data class KshetraListUiState(
    val items: List<CatalogueTempleSummaryDto> = emptyList(),
    val element: CatalogueTempleElementDto? = null,
    val load: LoadState = LoadState.Idle,
) {
    val groups: List<KshetraGroup> get() = groupKshetras(items, element)

    /** Elements present in the loaded list, in enum order; the filter row offers only these. */
    val availableElements: List<CatalogueTempleElementDto>
        get() = CatalogueTempleElementDto.entries.filter { e -> items.any { it.element == e } }

    /** A filter that matches nothing, as opposed to an empty catalogue. */
    val filterMatchesNothing: Boolean get() = items.isNotEmpty() && groups.isEmpty()
}

/** Kshetras list. Shared by the Home shelf and the full list so both load once. */
class KshetraListPresenter(
    private val catalogue: CatalogueRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(KshetraListUiState())
    val state: StateFlow<KshetraListUiState> = _state.asStateFlow()
    private var generation = 0
    private var job: Job? = null
    private var attempted = false

    fun loadIfNeeded() {
        if (attempted) return
        attempted = true
        refresh()
    }

    fun retry() = refresh()

    /** Selecting the active element again clears the filter. */
    fun selectElement(element: CatalogueTempleElementDto?) {
        _state.update { it.copy(element = element.takeIf { e -> e != it.element }) }
    }

    fun clearElement() {
        _state.update { it.copy(element = null) }
    }

    private fun refresh() {
        val requested = ++generation
        job?.cancel()
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update { it.copy(load = LoadState.Loading) }
            try {
                val list = catalogue.listTemples(interaction)
                if (requested != generation) return@launch
                _state.update {
                    KshetraListUiState(
                        items = list.items,
                        element = it.element?.takeIf { e -> list.items.any { item -> item.element == e } },
                        load = if (list.items.isEmpty()) LoadState.Empty else LoadState.Idle,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update {
                    KshetraListUiState(
                        load = LoadState.Error(failure.userMessage(), retryable = failure !is CatalogueFailure.NotFound),
                    )
                }
            }
        }
    }
}

data class KshetraDetailUiState(
    val templeId: Uuid? = null,
    val detail: CatalogueTempleDetailDto? = null,
    val load: LoadState = LoadState.Idle,
)

class KshetraDetailPresenter(
    private val catalogue: CatalogueRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(KshetraDetailUiState())
    val state: StateFlow<KshetraDetailUiState> = _state.asStateFlow()
    private var generation = 0
    private var job: Job? = null

    fun open(id: Uuid) {
        val requested = ++generation
        job?.cancel()
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update { KshetraDetailUiState(templeId = id, load = LoadState.Loading) }
            try {
                val detail = catalogue.getTemple(id, interaction)
                if (requested != generation) return@launch
                _state.update { KshetraDetailUiState(templeId = id, detail = detail, load = LoadState.Idle) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update {
                    KshetraDetailUiState(
                        templeId = id,
                        load = LoadState.Error(failure.userMessage(), retryable = failure !is CatalogueFailure.NotFound),
                    )
                }
            }
        }
    }

    fun retry() {
        _state.value.templeId?.let(::open)
    }
}

data class TempleLinkUiState(
    val templeId: Uuid? = null,
    /** True only once the detail has loaded; false while pending and after any failure, 404 included. */
    val linkable: Boolean = false,
)

/**
 * Decides whether the reader's temple chip is a link (TRACK-151 Spec 24). A temple whose detail is
 * not served (ineligible, so 404) stays a plain label; so does one that cannot be checked right now.
 */
class TempleLinkPresenter(
    private val catalogue: CatalogueRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(TempleLinkUiState())
    val state: StateFlow<TempleLinkUiState> = _state.asStateFlow()
    private var generation = 0
    private var job: Job? = null

    fun resolve(id: Uuid?) {
        val requested = ++generation
        job?.cancel()
        if (id == null) {
            _state.value = TempleLinkUiState()
            return
        }
        _state.value = TempleLinkUiState(templeId = id)
        val interaction = session.beginInteraction()
        job = scope.launch {
            try {
                catalogue.getTemple(id, interaction)
                if (requested != generation) return@launch
                _state.value = TempleLinkUiState(templeId = id, linkable = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.value = TempleLinkUiState(templeId = id, linkable = false)
            }
        }
    }
}
