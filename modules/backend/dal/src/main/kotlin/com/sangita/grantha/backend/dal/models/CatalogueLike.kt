package com.sangita.grantha.backend.dal.models

/**
 * Catalogue LIKE patterns treat user input as a literal substring (TRACK-138).
 * `%`, `_`, and `\` are escaped so they cannot widen a search into a wildcard.
 */
object CatalogueLike {
    const val ESCAPE_CHAR: Char = '\\'

    fun containsPattern(raw: String): String {
        val escaped = raw.lowercase()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return "%$escaped%"
    }
}
