package com.sangita.grantha.shared.presentation.reader

import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricsDto
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.repository.PreferencesRepository
import com.sangita.grantha.shared.mobile.storage.CodecBackedPreferencesStore
import com.sangita.grantha.shared.mobile.storage.InMemoryKeyValueStore
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class KrithiReaderAtomicSwapTest {
    @Test
    fun failedSwapKeepsPreviousReadingAndSelectedVariant() = runTest {
        val presenter = presenter(
            object : CatalogueApi by FixtureCatalogueApi() {
                override suspend fun getLyrics(
                    krithiId: Uuid,
                    variantId: Uuid,
                    interaction: InteractionContext,
                ): CatalogueLyricsDto {
                    if (variantId == CatalogueFixtures.vatapiDevanagariId) {
                        throw CatalogueFailure.Unavailable()
                    }
                    return FixtureCatalogueApi().getLyrics(krithiId, variantId, interaction)
                }
            },
        )
        presenter.open(CatalogueFixtures.vatapiId)
        advanceUntilIdle()
        val previousText = presenter.state.value.lyrics?.sections?.first()?.text
        presenter.selectVariant(CatalogueFixtures.vatapiDevanagariId)
        advanceUntilIdle()
        assertEquals(CatalogueFixtures.vatapiLatinId, presenter.state.value.selectedVariantId)
        assertEquals(previousText, presenter.state.value.lyrics?.sections?.first()?.text)
        assertNull(presenter.state.value.pendingVariantId)
        assertIs<LoadState.Error>(presenter.state.value.lyricsLoad)
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun loadingSwapDoesNotRelabelOldLyrics() = runTest {
        val gate = CompletableDeferred<Unit>()
        val presenter = presenter(
            object : CatalogueApi by FixtureCatalogueApi() {
                override suspend fun getLyrics(
                    krithiId: Uuid,
                    variantId: Uuid,
                    interaction: InteractionContext,
                ): CatalogueLyricsDto {
                    if (variantId == CatalogueFixtures.vatapiDevanagariId) {
                        gate.await()
                    }
                    return FixtureCatalogueApi().getLyrics(krithiId, variantId, interaction)
                }
            },
        )
        presenter.open(CatalogueFixtures.vatapiId)
        advanceUntilIdle()
        val previousText = presenter.state.value.lyrics?.sections?.first()?.text
        presenter.selectVariant(CatalogueFixtures.vatapiDevanagariId)
        runCurrent()
        assertEquals(CatalogueFixtures.vatapiLatinId, presenter.state.value.selectedVariantId)
        assertEquals(CatalogueFixtures.vatapiDevanagariId, presenter.state.value.pendingVariantId)
        assertEquals(previousText, presenter.state.value.lyrics?.sections?.first()?.text)
        assertEquals(LoadState.Loading, presenter.state.value.lyricsLoad)
        assertEquals(ScriptCodeDto.LATIN, presenter.state.value.lyrics?.script)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(CatalogueFixtures.vatapiDevanagariId, presenter.state.value.selectedVariantId)
        assertEquals(ScriptCodeDto.DEVANAGARI, presenter.state.value.lyrics?.script)
        assertNull(presenter.state.value.pendingVariantId)
        assertEquals(LoadState.Idle, presenter.state.value.lyricsLoad)
    }

    private fun kotlinx.coroutines.test.TestScope.presenter(api: CatalogueApi): KrithiReaderPresenter =
        KrithiReaderPresenter(
            catalogue = CatalogueRepository(api),
            preferences = PreferencesRepository(CodecBackedPreferencesStore(InMemoryKeyValueStore())),
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
}
