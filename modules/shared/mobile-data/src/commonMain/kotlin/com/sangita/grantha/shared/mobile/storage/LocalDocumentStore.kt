package com.sangita.grantha.shared.mobile.storage

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class LocalDocumentLoad {
    data class Ok(val document: LocalSettingsDocument) : LocalDocumentLoad()
    data class Corrupt(val raw: String) : LocalDocumentLoad()
    data class UnsupportedVersion(val raw: String, val version: Int) : LocalDocumentLoad()
}

sealed class LocalWriteResult {
    data class Ok(val document: LocalSettingsDocument) : LocalWriteResult()
    data class Failed(val message: String, val recoverableRaw: String?) : LocalWriteResult()
}

class LocalDocumentStore(
    private val store: KeyValueStore,
    private val key: String = LocalSettingsCodec.SETTINGS_KEY,
) {
    private val mutex = Mutex()

    fun load(): LocalDocumentLoad = LocalSettingsCodec.load(store.read(key))

    fun mutate(
        transform: (LocalSettingsDocument) -> LocalSettingsDocument,
    ): LocalWriteResult = runBlocking {
        mutex.withLock {
            when (val loaded = LocalSettingsCodec.load(store.read(key))) {
                is LocalDocumentLoad.Ok -> {
                    val next = transform(loaded.document)
                    try {
                        store.write(key, LocalSettingsCodec.encode(next))
                        LocalWriteResult.Ok(next)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (cause: Exception) {
                        LocalWriteResult.Failed(
                            message = "Preferences could not be saved. Your last saved choices are unchanged.",
                            recoverableRaw = store.read(key),
                        )
                    }
                }
                is LocalDocumentLoad.Corrupt -> LocalWriteResult.Failed(
                    message = "Saved preferences could not be read. They were not overwritten.",
                    recoverableRaw = loaded.raw,
                )
                is LocalDocumentLoad.UnsupportedVersion -> LocalWriteResult.Failed(
                    message = "This saved library uses a newer format and was left unchanged.",
                    recoverableRaw = loaded.raw,
                )
            }
        }
    }
}
