package com.sangita.grantha.shared.presentation.reader

import kotlin.test.Test
import kotlin.test.assertEquals

/** TRACK-156: Tamil-script lyrics carry the source's digit notation (ka³ = ga); display it as superscripts. */
class TamilNotationTest {
    @Test
    fun herambayaPallaviAttachesMarkersAsSuperscripts() {
        assertEquals(
            "ஹேரம்பா³ ய நமஸ்தே ஹரி ப்³ரஹ்மேந்த்³ராதி³ -",
            TamilNotation.format("ஹேரம்பா 3 ய நமஸ்தே ஹரி ப் 3 ரஹ்மேந்த் 3 ராதி 3 -"),
        )
    }

    @Test
    fun coversAspirateVoicedAndVoicedAspirate() {
        assertEquals("நாதா² ய", TamilNotation.format("நாதா 2 ய"))
        assertEquals("வீர க³ ணாதி³", TamilNotation.format("வீர க 3 ணாதி 3"))
        assertEquals("பாஸா¹ ங்குஸ¹ த⁴ ராய", TamilNotation.format("பாஸா 1 ங்குஸ 1 த 4 ராய"))
    }

    @Test
    fun spacingIsOptionalAndAttachedFormsAreAccepted() {
        assertEquals("ஜக³ந்து³", TamilNotation.format("ஜக3ந்து3"))
    }

    @Test
    fun vowelSignsAndVirama() {
        assertEquals("கௌ³ ரி", TamilNotation.format("கௌ 3 ரி"))
        assertEquals("ஹ்ரு² த³ ய", TamilNotation.format("ஹ்ரு 2 த 3 ய"))
        assertEquals("ஸ்¹ரித", TamilNotation.format("ஸ் 1 ரித"))
        assertEquals("ஜ² ர", TamilNotation.format("ஜ 2 ர"))
    }

    @Test
    fun invalidBaseDigitPairsAreLeftAlone() {
        assertEquals("கீதம் 1", TamilNotation.format("கீதம் 1"))
        assertEquals("க 1", TamilNotation.format("க 1"))
        assertEquals("ஸ 3", TamilNotation.format("ஸ 3"))
        assertEquals("ர 2", TamilNotation.format("ர 2"))
    }

    @Test
    fun ordinalsAndMultiDigitNumbersAreUntouched() {
        assertEquals(
            "1. ஸ்ரீ ராக³ ம்\nஸ்ரீ விஸ்¹வ நாத² ம்\n2 . ஆரபி⁴ ராக³ ம்",
            TamilNotation.format("1. ஸ்ரீ ராக 3 ம்\nஸ்ரீ விஸ் 1 வ நாத 2 ம்\n2 . ஆரபி 4 ராக 3 ம்"),
        )
        assertEquals("த 12", TamilNotation.format("த 12"))
    }

    @Test
    fun leakedNotationLegendIsNotFormatted() {
        val legend = "க,ச,ட,த,ப - 2-ख छ ठ थ फ; 3-ग ड द ब\n(ச3 - ஜ)\nஸ1 श - शिव - சிவன்\nரு2 ऋ - कृप - கிருபை"
        assertEquals(legend + "\nநாதா² ய", TamilNotation.format(legend + "\nநாதா 2 ய"))
    }

    @Test
    fun spaceAfterDigitIsKeptAfterOpenSyllableButDroppedAfterVirama() {
        assertEquals("நிதே⁴ ஜய", TamilNotation.format("நிதே 4 ஜய"))
        assertEquals("ப்³ரஹ்ம", TamilNotation.format("ப் 3 ரஹ்ம"))
    }

    @Test
    fun isIdempotentAndRevertIsLossless() {
        val stored = "ப் 3 ரஹ்மேந்த் 3 ராதி 3 -\n1. ஸ்ரீ ராக 3 ம்"
        val once = TamilNotation.format(stored)
        assertEquals(once, TamilNotation.format(once))
        assertEquals(stored.replace(" ", ""), TamilNotation.revert(once).replace(" ", ""))
    }

    @Test
    fun textWithoutMarkersIsReturnedAsIs() {
        val plain = "வீர ஹனுமதே நமோ நமோ\nமாருத தனய நமோ நமோ"
        assertEquals(plain, TamilNotation.format(plain))
    }
}
