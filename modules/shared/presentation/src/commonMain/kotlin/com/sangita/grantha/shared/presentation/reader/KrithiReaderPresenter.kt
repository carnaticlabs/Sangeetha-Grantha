package com.sangita.grantha.shared.presentation.reader

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricsDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueVariantRefDto
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.repository.PreferencesRepository
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

data class ReaderUiState(
    val krithiId: Uuid? = null,
    val reader: CatalogueKrithiReaderDto? = null,
    val lyrics: CatalogueLyricsDto? = null,
    val selectedVariantId: Uuid? = null,
    val pendingVariantId: Uuid? = null,
    val lastFailedVariantId: Uuid? = null,
    val load: LoadState = LoadState.Idle,
    val lyricsLoad: LoadState = LoadState.Idle,
)

class KrithiReaderPresenter(
    private val catalogue: CatalogueRepository,
    private val preferences: PreferencesRepository,
    private val session: MobileSession,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()
    private var generation = 0
    private var job: Job? = null

    fun open(krithiId: Uuid) {
        val requested = ++generation
        job?.cancel()
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update {
                ReaderUiState(krithiId = krithiId, load = LoadState.Loading, lyricsLoad = LoadState.Idle)
            }
            try {
                val reader = catalogue.getKrithi(krithiId, interaction)
                if (requested != generation) return@launch
                val variantId = chooseVariant(reader)
                _state.update { it.copy(reader = reader, selectedVariantId = variantId) }
                if (variantId != null) {
                    val lyrics = catalogue.getLyrics(krithiId, variantId, interaction)
                    if (requested != generation) return@launch
                    _state.update { it.copy(lyrics = lyrics, load = LoadState.Idle) }
                } else {
                    _state.update { it.copy(load = LoadState.Empty) }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update {
                    it.copy(
                        load = LoadState.Error(
                            failure.userMessage(),
                            retryable = failure !is CatalogueFailure.NotFound,
                        ),
                    )
                }
            }
        }
    }

    fun selectVariant(variantId: Uuid) {
        val snapshot = _state.value
        val krithiId = snapshot.krithiId ?: return
        if (variantId == snapshot.selectedVariantId && snapshot.lyricsLoad !is LoadState.Error) return
        val requested = ++generation
        job?.cancel()
        val interaction = session.beginInteraction()
        job = scope.launch {
            _state.update {
                it.copy(
                    pendingVariantId = variantId,
                    lastFailedVariantId = null,
                    lyricsLoad = LoadState.Loading,
                )
            }
            try {
                val lyrics = catalogue.getLyrics(krithiId, variantId, interaction)
                if (requested != generation) return@launch
                _state.update {
                    it.copy(
                        lyrics = lyrics,
                        selectedVariantId = variantId,
                        pendingVariantId = null,
                        lastFailedVariantId = null,
                        lyricsLoad = LoadState.Idle,
                        load = LoadState.Idle,
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: CatalogueFailure) {
                if (requested != generation) return@launch
                _state.update {
                    it.copy(
                        pendingVariantId = null,
                        lastFailedVariantId = variantId,
                        lyricsLoad = LoadState.Error(
                            failure.userMessage(),
                            retryable = failure !is CatalogueFailure.NotFound,
                        ),
                    )
                }
            }
        }
    }

    fun retry() {
        val snapshot = _state.value
        if (snapshot.load is LoadState.Idle && snapshot.lyricsLoad is LoadState.Error) {
            snapshot.lastFailedVariantId?.let { selectVariant(it); return }
        }
        val id = snapshot.krithiId ?: return
        open(id)
    }

    private fun chooseVariant(reader: CatalogueKrithiReaderDto): Uuid? {
        val preferred = preferences.read().preferredScript
        val matching = reader.variants.filter { preferred != null && it.script == preferred }
        val fromPreference = when {
            matching.size == 1 -> matching.first()
            matching.count { it.isPrimary } == 1 -> matching.first { it.isPrimary }
            else -> matching.minByOrNull { it.id.toString() }
        }
        return fromPreference?.id
            ?: reader.defaultVariantId
            ?: reader.variants.firstOrNull { it.isPrimary }?.id
            ?: reader.variants.minByOrNull { it.id.toString() }?.id
    }

    fun selectedVariant(): CatalogueVariantRefDto? {
        val id = _state.value.selectedVariantId ?: return null
        return _state.value.reader?.variants?.find { it.id == id }
    }
}
