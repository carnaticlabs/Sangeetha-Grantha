package com.sangita.grantha.shared.mobile.repository

import com.sangita.grantha.shared.mobile.storage.PreferencesRecord
import com.sangita.grantha.shared.mobile.storage.PreferencesStore

class PreferencesRepository(
    private val store: PreferencesStore,
) {
    fun read(): PreferencesRecord = store.read()

    fun write(record: PreferencesRecord) {
        store.write(record)
    }
}
