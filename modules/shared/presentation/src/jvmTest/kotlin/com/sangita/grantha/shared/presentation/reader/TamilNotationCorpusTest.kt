package com.sangita.grantha.shared.presentation.reader

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TRACK-156 corpus proof. Set TAMIL_CORPUS_FILE to a text file holding one stored Tamil lyric section
 * per line (newlines escaped as \n), dumped read-only from krithi_lyric_sections. Skipped when unset.
 */
class TamilNotationCorpusTest {
    private val corpus: List<String>? = System.getenv("TAMIL_CORPUS_FILE")
        ?.takeIf { it.isNotBlank() }
        ?.let { path -> File(path).readLines().map { it.replace("\\n", "\n") } }

    @Test
    fun formattingRemovesEveryMarkerDigitAndKeepsEverythingElse() {
        val sections = corpus ?: return
        assertTrue(sections.size > 4000, "expected the full Tamil corpus, got ${sections.size}")
        var markers = 0
        sections.forEach { stored ->
            val shown = TamilNotation.format(stored)
            assertEquals(shown, TamilNotation.format(shown), "not idempotent: $stored")
            assertEquals(stored.replace(" ", ""), TamilNotation.revert(shown).replace(" ", ""), "lossy: $stored")
            markers += shown.count { it in "¹²³⁴" }
        }
        // Reviewed measurement on the dev database: 31,656 pairs; allow only growth from new data.
        assertTrue(markers >= 31_000, "markers superscripted: $markers")
    }

    @Test
    fun onlyOrdinalsAndGeethamNumbersSurviveAsAsciiDigits() {
        val sections = corpus ?: return
        val leftovers = sections.flatMap { withoutLegend(TamilNotation.format(it)) }
            .flatMap { line ->
                Regex("(?<![0-9])[1-4](?![0-9])").findAll(line)
                    .map { m -> line.substring(maxOf(0, m.range.first - 5), m.range.last + 1) }
            }
            // Allowed: line-start ordinals, Geetham numbers, and one bracketed optional letter `(கீ)3` that the
            // notation does not define (left as stored rather than guessed).
            .filterNot { Regex("^\\s*[1-4]$").matches(it) || Regex("தம் [1-4]$|\\(கீ\\)3$").containsMatchIn(it) }
        assertEquals(emptyList(), leftovers)
    }

    private fun withoutLegend(text: String): List<String> {
        val lines = text.lines()
        val start = lines.indexOfFirst { it.startsWith("க,ச,ட,த,ப") }
        return if (start < 0) lines else lines.take(start) + lines.drop(start + 4)
    }
}
