package com.sangita.grantha.shared.presentation.favourites

import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.repository.FavouritesRepository
import com.sangita.grantha.shared.mobile.storage.CodecBackedBookmarkStore
import com.sangita.grantha.shared.mobile.storage.InMemoryKeyValueStore
import com.sangita.grantha.shared.mobile.storage.LocalSettingsCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavouritesPresenterTest {
    @Test
    fun toggleSavesThenRemovesLocalBookmarkOnly() {
        val store = InMemoryKeyValueStore()
        val presenter = FavouritesPresenter(FavouritesRepository(CodecBackedBookmarkStore(store)))
        val id = CatalogueFixtures.vatapiId

        presenter.toggle(id, "Vatapi Ganapatim")
        assertTrue(presenter.isFavourite(id))
        assertEquals("Vatapi Ganapatim", presenter.state.value.bookmarks.single().label)

        presenter.toggle(id, "ignored")
        assertFalse(presenter.isFavourite(id))
        assertTrue(presenter.state.value.bookmarks.isEmpty())
        assertTrue(store.read(LocalSettingsCodec.SETTINGS_KEY)?.contains("Vatapi") != true)
    }

    @Test
    fun clipsLongLabelToContractLimit() {
        val presenter = FavouritesPresenter(
            FavouritesRepository(CodecBackedBookmarkStore(InMemoryKeyValueStore())),
        )
        val long = "E".repeat(200)
        presenter.toggle(CatalogueFixtures.endaroId, long)
        assertEquals(
            com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract.BOOKMARK_LABEL_MAX_CODE_POINTS,
            presenter.state.value.bookmarks.single().label.length,
        )
    }

    @Test
    fun removeDoesNotAffectOtherBookmarks() {
        val presenter = FavouritesPresenter(
            FavouritesRepository(CodecBackedBookmarkStore(InMemoryKeyValueStore())),
        )
        presenter.toggle(CatalogueFixtures.vatapiId, "Vatapi")
        presenter.toggle(CatalogueFixtures.endaroId, "Endaro")
        presenter.remove(CatalogueFixtures.vatapiId)
        assertEquals(listOf(CatalogueFixtures.endaroId), presenter.state.value.bookmarks.map { it.krithiId })
    }
}
