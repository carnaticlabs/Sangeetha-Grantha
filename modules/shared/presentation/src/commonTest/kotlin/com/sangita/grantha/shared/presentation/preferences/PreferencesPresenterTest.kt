package com.sangita.grantha.shared.presentation.preferences

import com.sangita.grantha.shared.mobile.repository.PreferencesRepository
import com.sangita.grantha.shared.mobile.storage.AppearancePreference
import com.sangita.grantha.shared.mobile.storage.CodecBackedPreferencesStore
import com.sangita.grantha.shared.mobile.storage.InMemoryKeyValueStore
import com.sangita.grantha.shared.mobile.storage.KeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreferencesPresenterTest {
    @Test
    fun appearanceChangePersistsImmediatelyWithoutSaveStep() {
        val store = InMemoryKeyValueStore()
        val presenter = PreferencesPresenter(PreferencesRepository(CodecBackedPreferencesStore(store)))
        assertEquals(AppearancePreference.SYSTEM, presenter.state.value.appearance)

        presenter.setAppearance(AppearancePreference.DARK)
        assertEquals(AppearancePreference.DARK, presenter.state.value.appearance)
        assertNull(presenter.saveError.value)

        val reloaded = PreferencesPresenter(PreferencesRepository(CodecBackedPreferencesStore(store)))
        assertEquals(AppearancePreference.DARK, reloaded.state.value.appearance)
    }

    @Test
    fun failedAppearanceWriteKeepsLastSavedChoiceAndRetryRestoresIt() {
        val inner = InMemoryKeyValueStore()
        val failing = object : KeyValueStore {
            var failNext = false
            override fun read(key: String) = inner.read(key)
            override fun write(key: String, value: String) {
                if (failNext) error("disk full")
                inner.write(key, value)
            }
            override fun remove(key: String) = inner.remove(key)
        }
        val presenter = PreferencesPresenter(PreferencesRepository(CodecBackedPreferencesStore(failing)))
        presenter.setAppearance(AppearancePreference.LIGHT)
        assertEquals(AppearancePreference.LIGHT, presenter.state.value.appearance)

        failing.failNext = true
        presenter.setAppearance(AppearancePreference.DARK)
        assertEquals(AppearancePreference.LIGHT, presenter.state.value.appearance)
        assertEquals(
            "Preferences could not be saved. Your last saved choices are unchanged.",
            presenter.saveError.value,
        )

        failing.failNext = false
        presenter.retrySave()
        assertEquals(AppearancePreference.DARK, presenter.state.value.appearance)
        assertNull(presenter.saveError.value)
    }
}
