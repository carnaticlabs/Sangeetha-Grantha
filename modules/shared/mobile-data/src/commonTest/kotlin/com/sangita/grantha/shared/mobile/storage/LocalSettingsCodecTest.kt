package com.sangita.grantha.shared.mobile.storage

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class LocalSettingsCodecTest {
    @Test
    fun emptyAndCorruptInputRecoverWithoutThrowing() {
        assertEquals(0, LocalSettingsCodec.decode(null).bookmarks.size)
        assertEquals(0, LocalSettingsCodec.decode("{not-json").bookmarks.size)
        assertEquals(AppearancePreference.SYSTEM, LocalSettingsCodec.decode("").preferences.appearance)
    }

    @Test
    fun roundTripBookmarkAndPreferences() {
        val store = InMemoryKeyValueStore()
        val bookmarks = CodecBackedBookmarkStore(store)
        val prefs = CodecBackedPreferencesStore(store)
        val id = CatalogueFixtures.vatapiId
        bookmarks.upsert(BookmarkRecord(id, "Vatapi Ganapatim", createdAtEpochMs = 10))
        prefs.write(PreferencesRecord(textSize = TextSizePreference.LARGE, appearance = AppearancePreference.DARK))

        assertEquals("Vatapi Ganapatim", bookmarks.list().single().label)
        assertEquals(TextSizePreference.LARGE, prefs.read().textSize)
        assertEquals(AppearancePreference.DARK, prefs.read().appearance)
        bookmarks.remove(id)
        assertTrue(bookmarks.list().isEmpty())
        assertEquals(TextSizePreference.LARGE, prefs.read().textSize)
    }

    @Test
    fun clipsBookmarkLabelToContractLimit() {
        val long = "x".repeat(CatalogueContract.BOOKMARK_LABEL_MAX_CODE_POINTS + 20)
        val clipped = LocalSettingsCodec.clipLabel(long)
        assertEquals(CatalogueContract.BOOKMARK_LABEL_MAX_CODE_POINTS, clipped.length)
    }

    @Test
    fun replacingBookmarkKeepsSingleRow() {
        val store = InMemoryKeyValueStore()
        val bookmarks = CodecBackedBookmarkStore(store)
        val id = Uuid.parse("dddddddd-dddd-4ddd-8ddd-dddddddddddd")
        bookmarks.upsert(BookmarkRecord(id, "first", createdAtEpochMs = 1))
        bookmarks.upsert(BookmarkRecord(id, "second", createdAtEpochMs = 2))
        assertEquals(1, bookmarks.list().size)
        assertEquals("second", bookmarks.list().single().label)
    }
}
