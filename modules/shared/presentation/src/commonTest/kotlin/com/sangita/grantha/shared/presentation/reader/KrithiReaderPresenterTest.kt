package com.sangita.grantha.shared.presentation.reader

import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.repository.PreferencesRepository
import com.sangita.grantha.shared.mobile.storage.CodecBackedPreferencesStore
import com.sangita.grantha.shared.mobile.storage.InMemoryKeyValueStore
import com.sangita.grantha.shared.mobile.storage.PreferencesRecord
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class KrithiReaderPresenterTest {
    @Test
    fun openLoadsDefaultLatinReadingAndStoredSections() = runTest {
        val presenter = presenter()
        presenter.open(CatalogueFixtures.vatapiId)
        advanceUntilIdle()
        assertEquals(CatalogueFixtures.vatapiLatinId, presenter.state.value.selectedVariantId)
        assertEquals(2, presenter.state.value.lyrics?.sections?.size)
        assertEquals("Vatapi Ganapatim Bhajeham", presenter.state.value.lyrics?.sections?.first()?.text)
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun preferredScriptSelectsUnambiguousStoredVariant() = runTest {
        val presenter = presenter(preferredScript = ScriptCodeDto.DEVANAGARI)
        presenter.open(CatalogueFixtures.vatapiId)
        advanceUntilIdle()
        assertEquals(CatalogueFixtures.vatapiDevanagariId, presenter.state.value.selectedVariantId)
        assertEquals("वातापि गणपतिं भजेहं", presenter.state.value.lyrics?.sections?.first()?.text)
    }

    @Test
    fun selectVariantFetchesThatReadingOnly() = runTest {
        val presenter = presenter()
        presenter.open(CatalogueFixtures.vatapiId)
        advanceUntilIdle()
        presenter.selectVariant(CatalogueFixtures.vatapiDevanagariId)
        advanceUntilIdle()
        assertEquals(CatalogueFixtures.vatapiDevanagariId, presenter.state.value.selectedVariantId)
        assertEquals(ScriptCodeDto.DEVANAGARI, presenter.state.value.lyrics?.script)
    }

    @Test
    fun missingCompositionIsNotFound() = runTest {
        val presenter = presenter()
        presenter.open(Uuid.parse("00000000-0000-4000-8000-000000000000"))
        advanceUntilIdle()
        assertNull(presenter.state.value.reader)
        val load = presenter.state.value.load
        assertEquals(true, load is LoadState.Error && !load.retryable)
    }

    @Test
    fun newerOpenSupersedesOlder() = runTest {
        val presenter = presenter()
        presenter.open(CatalogueFixtures.endaroId)
        presenter.open(CatalogueFixtures.vatapiId)
        advanceUntilIdle()
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.reader?.id)
        assertEquals(CatalogueFixtures.vatapiLatinId, presenter.state.value.selectedVariantId)
    }

    private fun kotlinx.coroutines.test.TestScope.presenter(
        preferredScript: ScriptCodeDto? = null,
    ): KrithiReaderPresenter {
        val prefs = PreferencesRepository(CodecBackedPreferencesStore(InMemoryKeyValueStore()))
        if (preferredScript != null) {
            prefs.write(PreferencesRecord(preferredScript = preferredScript))
        }
        return KrithiReaderPresenter(
            catalogue = CatalogueRepository(FixtureCatalogueApi()),
            preferences = prefs,
            session = MobileSession(clockMs = { 0L }),
            scope = this,
        )
    }
}
