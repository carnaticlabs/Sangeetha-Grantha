package com.sangita.grantha.shared.presentation.kshetra

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import kotlin.uuid.Uuid

/**
 * Scroll positions for the kshetra screens, owned by [com.sangita.grantha.shared.presentation.RasikaApp].
 *
 * `AnimatedContent` drops a destination from composition as soon as another one is opened, so
 * `rememberScrollState()` inside the screen starts again at the top on back. These states live above
 * that boundary instead. They are plain objects, not Compose state: a screen reads them while it is
 * being composed and nothing here changes while a screen is showing.
 *
 * Positions are kept only for "back": [retain] drops every position whose screen is no longer on the
 * navigation stack, so a fresh visit from Home or Search starts at the top.
 */
class KshetraScrollStates(private val maxDetails: Int = DEFAULT_MAX_DETAILS) {
    /** The Kshetras list. Replaced, not scrolled, when its position is forgotten. */
    var list: ScrollState = ScrollState(0)
        private set

    // Insertion order is recency order: the first entry is the oldest.
    private val details = LinkedHashMap<Uuid, LazyListState>()

    // Null until the first retain(): every id may be cached before the stack is known.
    private var onStack: Set<Uuid>? = null

    val detailCount: Int get() = details.size

    /**
     * One state per temple, so opening a shrine from Tiruvarur does not inherit its offset. A temple
     * that is no longer on the stack (a screen fading out after back) gets the cached state if there
     * still is one, or a throwaway at the top, and is never cached again.
     */
    fun detail(id: Uuid): LazyListState {
        val existing = details.remove(id)
        if (existing == null && onStack?.contains(id) == false) return LazyListState()
        val state = existing ?: LazyListState()
        details[id] = state
        while (details.size > maxDetails) details.remove(details.keys.first())
        return state
    }

    /** Keeps the positions of [openDetails] and of the list only while [listOpen]; forgets the rest. */
    fun retain(listOpen: Boolean, openDetails: Set<Uuid>) {
        if (!listOpen) list = ScrollState(0)
        onStack = openDetails
        details.keys.retainAll(openDetails)
    }

    companion object {
        /** The navigation stack holds at most 8 destinations, so no more details can be open. */
        const val DEFAULT_MAX_DETAILS: Int = 8
    }
}
