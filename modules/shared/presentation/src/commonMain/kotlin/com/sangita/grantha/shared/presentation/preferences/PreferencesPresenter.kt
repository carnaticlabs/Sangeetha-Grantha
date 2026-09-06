package com.sangita.grantha.shared.presentation.preferences

import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.mobile.repository.PreferencesRepository
import com.sangita.grantha.shared.mobile.storage.AppearancePreference
import com.sangita.grantha.shared.mobile.storage.PreferencesRecord
import com.sangita.grantha.shared.mobile.storage.TextSizePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesPresenter(
    private val preferences: PreferencesRepository,
) {
    private val _state = MutableStateFlow(preferences.read())
    val state: StateFlow<PreferencesRecord> = _state.asStateFlow()

    fun setAppearance(appearance: AppearancePreference) = write(_state.value.copy(appearance = appearance))

    fun setTextSize(textSize: TextSizePreference) = write(_state.value.copy(textSize = textSize))

    fun setScript(script: ScriptCodeDto?) = write(_state.value.copy(preferredScript = script))

    private fun write(record: PreferencesRecord) {
        preferences.write(record)
        _state.value = preferences.read()
    }
}
