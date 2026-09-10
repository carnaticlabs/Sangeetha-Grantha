package com.sangita.grantha.shared.mobile.storage

import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LocalDocumentStoreTest {
    @Test
    fun corruptDocumentIsNotOverwrittenOnMutate() {
        val store = InMemoryKeyValueStore(mapOf(LocalSettingsCodec.SETTINGS_KEY to "{not-json"))
        val documents = LocalDocumentStore(store)
        val result = documents.mutate { it.copy(preferences = PreferencesRecord(appearance = AppearancePreference.DARK)) }
        assertIs<LocalWriteResult.Failed>(result)
        assertEquals("{not-json", store.read(LocalSettingsCodec.SETTINGS_KEY))
        assertEquals("{not-json", result.recoverableRaw)
    }

    @Test
    fun unsupportedVersionIsNotReplacedWithEmptyDocument() {
        val future = """{"bookmarks":[],"preferences":{"textSize":"MEDIUM","appearance":"SYSTEM","schemaVersion":1},"schemaVersion":99}"""
        val store = InMemoryKeyValueStore(mapOf(LocalSettingsCodec.SETTINGS_KEY to future))
        val documents = LocalDocumentStore(store)
        val result = documents.mutate { it.copy(bookmarks = emptyList()) }
        assertIs<LocalWriteResult.Failed>(result)
        assertEquals(future, store.read(LocalSettingsCodec.SETTINGS_KEY))
        val loaded = documents.load()
        assertIs<LocalDocumentLoad.UnsupportedVersion>(loaded)
        assertEquals(99, loaded.version)
    }

    @Test
    fun v1BookmarksAndPreferencesSurviveSerializedWrites() {
        val store = InMemoryKeyValueStore()
        val documents = LocalDocumentStore(store)
        val saved = documents.mutate {
            it.copy(
                bookmarks = listOf(
                    BookmarkRecord(CatalogueFixtures.vatapiId, "Vatapi Ganapatim", createdAtEpochMs = 10),
                ),
                preferences = PreferencesRecord(appearance = AppearancePreference.LIGHT),
            )
        }
        assertIs<LocalWriteResult.Ok>(saved)
        val roundTrip = documents.load()
        assertIs<LocalDocumentLoad.Ok>(roundTrip)
        assertEquals(1, roundTrip.document.schemaVersion)
        assertEquals("Vatapi Ganapatim", roundTrip.document.bookmarks.single().label)
        assertEquals(AppearancePreference.LIGHT, roundTrip.document.preferences.appearance)
        assertTrue("query=" !in store.read(LocalSettingsCodec.SETTINGS_KEY).orEmpty())
        assertTrue("items" !in store.read(LocalSettingsCodec.SETTINGS_KEY).orEmpty())
    }

    @Test
    fun concurrentMutatesSerializeAndKeepBothUpdates() = runBlocking {
        val store = InMemoryKeyValueStore()
        val documents = LocalDocumentStore(store)
        coroutineScope {
            launch(Dispatchers.Default) {
                documents.mutate {
                    it.copy(preferences = PreferencesRecord(appearance = AppearancePreference.DARK))
                }
            }
            launch(Dispatchers.Default) {
                documents.mutate {
                    it.copy(
                        bookmarks = listOf(
                            BookmarkRecord(CatalogueFixtures.endaroId, "Endaro", createdAtEpochMs = 2),
                        ),
                    )
                }
            }
        }
        val loaded = documents.load()
        assertIs<LocalDocumentLoad.Ok>(loaded)
        assertEquals(AppearancePreference.DARK, loaded.document.preferences.appearance)
        assertEquals(1, loaded.document.bookmarks.size)
        assertEquals(CatalogueFixtures.endaroId, loaded.document.bookmarks.single().krithiId)
    }

    @Test
    fun writeFailureLeavesLastGoodDocument() {
        val store = FailingKeyValueStore()
        val documents = LocalDocumentStore(store)
        val first = documents.mutate {
            it.copy(preferences = PreferencesRecord(appearance = AppearancePreference.LIGHT))
        }
        assertIs<LocalWriteResult.Ok>(first)
        store.failWrites = true
        val failed = documents.mutate {
            it.copy(preferences = PreferencesRecord(appearance = AppearancePreference.DARK))
        }
        assertIs<LocalWriteResult.Failed>(failed)
        val loaded = documents.load()
        assertIs<LocalDocumentLoad.Ok>(loaded)
        assertEquals(AppearancePreference.LIGHT, loaded.document.preferences.appearance)
    }
}

private class FailingKeyValueStore : KeyValueStore {
    var failWrites: Boolean = false
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String) {
        if (failWrites) error("disk full")
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
