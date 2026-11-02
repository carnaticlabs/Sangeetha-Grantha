package com.sangita.grantha.shared.presentation.reader

/**
 * Display form of the digit notation used in Tamil-script editions of Sanskrit lyrics (TRACK-156).
 *
 * Tamil script has one letter where Sanskrit has four, so the source blogs write the sound as a digit after
 * the base letter: `க 3` is ga, `த 4` dha, `ஸ 1` ś, `ரு 2` ṛ. Stored text keeps the source's plain digits
 * (reviewed lyric content is never rewritten); the reader shows them as superscripts attached to their letter,
 * the way print editions do: `ப் 3 ரஹ்மேந்த் 3 ராதி 3` is shown as `ப்³ரஹ்மேந்த்³ராதி³`.
 *
 * A digit is a marker only when its base letter and digit form a pair the notation defines. Line-start
 * ordinals (`1. ஸ்ரீ ராகம்`), Geetham numbers (`கீதம் 1`) and multi-digit numbers are never touched.
 * The source's four-line notation legend, leaked into some sections, is left as stored.
 *
 * Spacing: the space before a digit is always removed (the digit belongs to the letter before it). The space
 * after it is removed only after a virama, which must continue into the next consonant of the same word. After an
 * open syllable the source's single space may be a word boundary (`நிதே 4 ஜய`, nidhE jaya) or mid-word
 * (`தே 3 வ`, dEva), and the stored text cannot say which, so it is kept.
 */
object TamilNotation {
    // Base consonant, optional vowel sign or virama, optional single space, digit 1-4 not followed by a digit.
    // ரு is ர + ு. Vowel signs: U+0BBE..U+0BCC; virama U+0BCD.
    private val marker = Regex("(ரு|[கசடதபஸஜ][\\u0BBE-\\u0BCD]?) ?([1-4])(?![0-9])( ?)")
    private val superscript = Regex("(ரு|[கசடதபஸஜ][\\u0BBE-\\u0BCD]?)([¹²³⁴])")
    private const val VIRAMA = '\u0BCD'
    private const val LEGEND_START = "க,ச,ட,த,ப"
    private const val LEGEND_LINES = 4
    private const val SUPERSCRIPTS = "¹²³⁴"

    // Which digits each base letter takes: 2 aspirate, 3 voiced, 4 voiced aspirate; ஸ 1 = ś; ரு 2 = ṛ; ஜ 2 = jha.
    private fun accepts(base: String, digit: Char): Boolean = when {
        base == "ரு" -> digit == '2'
        base[0] == 'ஸ' -> digit == '1'
        base[0] == 'ஜ' -> digit == '2'
        else -> digit in '2'..'4'
    }

    fun format(text: String): String {
        if (text.none { it in '1'..'4' }) return text
        var legendLeft = 0
        return text.split('\n').joinToString("\n") { line ->
            if (line.startsWith(LEGEND_START)) legendLeft = LEGEND_LINES
            if (legendLeft > 0) {
                legendLeft--
                line
            } else {
                marker.replace(line) { m ->
                    val base = m.groupValues[1]
                    val digit = m.groupValues[2][0]
                    when {
                        !accepts(base, digit) -> m.value
                        base.last() == VIRAMA -> base + SUPERSCRIPTS[digit - '1']
                        else -> base + SUPERSCRIPTS[digit - '1'] + m.groupValues[3]
                    }
                }
            }
        }
    }

    /** Inverse of [format] (the removed space aside): superscripts back to the stored ASCII digits. */
    fun revert(text: String): String =
        superscript.replace(text) { m -> m.groupValues[1] + (SUPERSCRIPTS.indexOf(m.groupValues[2][0]) + 1) }
}
