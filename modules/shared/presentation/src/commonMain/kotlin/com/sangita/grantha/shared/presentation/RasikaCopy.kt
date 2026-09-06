package com.sangita.grantha.shared.presentation

object RasikaCopy {
    const val APP_NAME = "Rasika"
    const val CATALOGUE_NAME = "Sangita Grantha"
    const val TAB_SEARCH = "Search"
    const val TAB_BROWSE = "Browse"
    const val TAB_FAVOURITES = "Favourites"
    const val SEARCH_PLACEHOLDER = "Title, incipit or a line of sahitya"
    const val SEARCH_ACTION = "Search"
    const val SEARCH_TITLE = "Search"
    const val SEARCH_LABEL = "SEARCH"
    const val SEARCH_IDLE = "Search published kritis by title, incipit or a stored line of sahitya."
    const val EMPTY_SEARCH = "Nothing stored under that"
    const val EMPTY_SEARCH_BODY = "No published krithi matches. Try a shorter phrase, or browse by rāga."
    const val EMPTY_FAVOURITES = "No favourites yet"
    const val EMPTY_FAVOURITES_BODY = "Tap the heart on any krithi. Only the identifier and its title are kept on this device."
    const val RETRY = "Retry"
    const val CLEAR = "Clear"
    const val LOADING = "Loading the catalogue…"
    const val ERROR_TITLE = "Could not reach the catalogue"
    const val ERROR_BODY = "Your favourites are still here. The search needs a connection."
    const val OFFLINE_BODY = "Check the connection and try again. Favourites on this device are unchanged."
    const val ADD_RAGA_OR_COMPOSER = "+ Rāga or composer"
    const val FIND_A_KRITHI = "Find a krithi"
    const val BROWSE_RAGAS_ACTION = "Browse rāgas"
    const val TITLE_ORDER = "Title order"
    const val RAGAMALIKA = "Ragamalika"
    const val BROWSE_RAGAS = "Ragas"
    const val BROWSE_COMPOSERS = "Composers"
    const val BROWSE_TITLE = "Browse"
    const val BROWSE_FILTER = "Filter this directory"
    const val ALL_RAGAS = "All ragas"
    const val ALL_COMPOSERS = "All composers"
    const val FAVOURITES_TITLE = "Favourites"
    const val PREFERENCES = "Preferences"
    const val APPEARANCE = "Appearance"
    const val TEXT_SIZE = "Text size"
    const val SERVER_DEFAULT = "Server default"
    const val ADD_FAVOURITE = "Save favourite"
    const val REMOVE_FAVOURITE = "Remove favourite"
    const val SCRIPT = "Script"
    const val READING = "Reading"
    const val NOTATION_CAPTION = "This reader shows stored lyrics. It does not render notation or establish completeness of the whole composition."
    const val UNAVAILABLE = "This composition is not available."
    const val BACK = "Back"
    const val COMPOSITIONS = "published kritis"
    const val ORIGINAL_LANGUAGE = "Original language"
    const val BROWSE_NOTE =
        "Counts are published compositions. Same-name rāgas stay separate identities and are never merged."
    const val FAV_STORAGE_NONE = "Only identifiers and titles are held on this device."
    const val LYRICS = "Lyrics"
    const val DETAILS = "Details"
    const val MEMBERSHIP = "Membership"
    const val COMPLETENESS_FALLBACK =
        "This reader shows stored lyrics. It does not establish completeness of the whole composition."

    fun resultCount(n: Int): String = when (n) {
        1 -> "1 RESULT"
        else -> "$n RESULTS"
    }

    fun favStorageLine(n: Int): String = when (n) {
        0 -> FAV_STORAGE_NONE
        1 -> "1 saved · identifiers and titles only, on this device"
        else -> "$n saved · identifiers and titles only, on this device"
    }
}
