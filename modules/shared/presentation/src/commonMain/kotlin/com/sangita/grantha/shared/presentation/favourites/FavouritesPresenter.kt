package com.sangita.grantha.shared.presentation.favourites

import com.sangita.grantha.shared.mobile.repository.FavouritesRepository
import com.sangita.grantha.shared.mobile.storage.BookmarkRecord
import com.sangita.grantha.shared.mobile.storage.LocalWriteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.Uuid

data class FavouritesUiState(
    val bookmarks: List<BookmarkRecord> = emptyList(),
)

class FavouritesPresenter(
    private val favourites: FavouritesRepository,
) {
    private val _state = MutableStateFlow(FavouritesUiState(favourites.list()))
    val state: StateFlow<FavouritesUiState> = _state.asStateFlow()

    fun refresh() {
        _state.value = FavouritesUiState(favourites.list())
    }

    fun remove(id: Uuid) {
        if (favourites.remove(id) is LocalWriteResult.Ok) refresh()
    }

    fun isFavourite(id: Uuid): Boolean = favourites.isFavourite(id)

    fun toggle(id: Uuid, label: String) {
        val result =
            if (favourites.isFavourite(id)) favourites.remove(id) else favourites.save(id, label)
        if (result is LocalWriteResult.Ok) refresh()
    }
}
