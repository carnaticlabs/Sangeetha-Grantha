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

    /**
     * Pattern for matching a phrase inside stored lyrics (TRACK-159). Lyrics break lines with newlines, so each
     * whitespace run in the query matches exactly one character (space or newline) instead of a literal space.
     * Everything else is escaped as in [containsPattern].
     */
    fun lyricPhrasePattern(raw: String): String =
        raw.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            .joinToString("_") { token ->
                token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            }
            .let { "%$it%" }
}
