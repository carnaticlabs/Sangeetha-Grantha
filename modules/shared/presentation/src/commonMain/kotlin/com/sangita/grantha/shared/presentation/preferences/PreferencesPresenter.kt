package com.sangita.grantha.shared.presentation.preferences

import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.mobile.repository.PreferencesRepository
import com.sangita.grantha.shared.mobile.storage.AppearancePreference
import com.sangita.grantha.shared.mobile.storage.LocalWriteResult
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
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()
    private var pending: PreferencesRecord? = null

    fun setAppearance(appearance: AppearancePreference) = write(_state.value.copy(appearance = appearance))

    fun setTextSize(textSize: TextSizePreference) = write(_state.value.copy(textSize = textSize))

    fun setScript(script: ScriptCodeDto?) = write(_state.value.copy(preferredScript = script))

    fun retrySave() {
        val record = pending ?: return
        write(record)
    }

    private fun write(record: PreferencesRecord) {
        pending = record
        when (val result = preferences.write(record)) {
            is LocalWriteResult.Ok -> {
                pending = null
                _saveError.value = null
                _state.value = result.document.preferences
            }
            is LocalWriteResult.Failed -> _saveError.value = result.message
        }
    }
}
