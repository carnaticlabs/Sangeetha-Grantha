package com.sangita.grantha.shared.presentation

import com.sangita.grantha.shared.domain.model.MusicalFormDto

object RasikaCopy {
    const val APP_NAME = "Rasika"
    const val CATALOGUE_NAME = "Sangita Grantha"
    const val TAB_HOME = "Home"
    const val TAB_EXPLORE = "Explore"
    const val TAB_LIBRARY = "Library"
    const val TAB_SETTINGS = "Settings"
    const val TAB_SEARCH = "Search"
    const val TAB_BROWSE = "Browse"
    const val TAB_FAVOURITES = "Favourites"
    const val SEARCH_PLACEHOLDER = "Title, incipit or a line of sahitya"
    const val SEARCH_ACTION = "Search"
    const val SEARCH_TITLE = "Explore"
    const val SEARCH_LABEL = "EXPLORE"
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
    const val FAVOURITES_TITLE = "Library"
    const val PREFERENCES = "Settings"
    const val HOME_TITLE = "Home"
    const val HOME_LABEL = "HOME"
    const val HOME_INVITE = "Search a title, or open rāgas and composers from here. Continue reading appears after a composition is opened on this device."
    const val EXPLORE_SHORTCUTS = "Explore the tradition"
    const val FEATURE_UNAVAILABLE = "The featured composition could not be loaded. Search and browse still work."
    const val FEATURE_PANEL = "From the collection"
    const val READ_COMPOSITION = "Read composition"
    const val APPEARANCE_GROUP = "Appearance"
    const val APPEARANCE_SELECTED = "Selected"
    const val SAVE_FAILED_RETRY = "Retry save"
    const val LYRIC_SAMPLE_LABEL = "Lyric size sample"
    const val LYRIC_SAMPLE = "vatāpi ganapatim bhajēham"
    const val RETURN_TO_ROOT = "Return to this tab’s start"
    const val STACK_LIMIT = "This path is as deep as it can go. Return to the tab start to keep going."
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
    const val COMPLETENESS_NOT_ESTABLISHED = "Completeness not established"
    const val CATEGORY_KRITHIS = "Krithis"
    const val FILTERS = "Filters"
    const val FILTERS_PENDING = "Filter changes are not applied yet"
    const val FILTERS_HINT = "Choose one rāga and one composer. Apply commits both. Cancel discards the draft."
    const val APPLY_FILTERS = "Apply filters"
    const val CANCEL = "Cancel"
    const val RESET_FILTERS = "Reset"
    const val LOAD_MORE = "Load more"
    const val WORKS_IN_LIBRARY = "Works in this library"
    const val AROHANAM = "Arohanam"
    const val AVAROHANAM = "Avarohanam"
    const val LOADING_READING = "Loading the selected reading…"
    const val READING_FAILED = "That reading could not be loaded. The previous reading is still shown."
    const val SCRIPT_CHOICE = "Script"
    const val SOURCE_CHOICE = "Source reading"
    const val JUMP_TO_SECTION = "Jump to section"
    const val VARNAM_CHARANAM_CAPTION = "pallavi of the charanam"
    const val PARTIAL_READING =
        "Some sections are not present in the stored source reading, so completeness is partial."
    const val COMPLETE_READING = "This stored reading is complete for the selected source."

    fun moreRagas(n: Int): String = "+$n more"

    fun inThisLibrary(n: Long): String = when (n) {
        1L -> "1 in this library"
        else -> "$n in this library"
    }

    fun inThisLibrary(n: Int): String = inThisLibrary(n.toLong())

    fun resultCount(n: Int): String = inThisLibrary(n)

    fun musicalFormLabel(form: MusicalFormDto): String? =
        when (form) {
            MusicalFormDto.KRITHI -> "Krithi"
            MusicalFormDto.VARNAM -> "Varnam"
            MusicalFormDto.SWARAJATHI -> "Swarajathi"
            MusicalFormDto.UNESTABLISHED -> null
        }

    fun favStorageLine(n: Int): String = when (n) {
        0 -> FAV_STORAGE_NONE
        1 -> "1 saved · identifiers and titles only, on this device"
        else -> "$n saved · identifiers and titles only, on this device"
    }
}
