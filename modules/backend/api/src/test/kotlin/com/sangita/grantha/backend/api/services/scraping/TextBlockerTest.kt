package com.sangita.grantha.backend.api.services.scraping

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextBlockerTest {

    private val blocker = TextBlocker()

    @Test
    fun `test multi script duplication`() {
        val rawText = """
            English
            
            pallavi
            avyAja karuNA kaTAkshi aniSaM mAmava kAmAkshi
            
            samashTi caraNam
            ravyAdi nava grahOdayE rasAlanga nATaka kriyE
            divyAlaMkRtAnga SriyE dInAvana guru guha priyE
            (madhyama kAla sAhityam)
            savyApasavya mArgasthE sadA namastE Suka hastE
            
            variations -
            rasAlanga  - sAranga
            divyAlaMkRtAnga SriyE  - divyAlaMkRtAngASrayE - divyAlaMkRtAngASriyE
            
            English - Word Division
            
            pallavi
            avyAja karuNA kaTAkshi aniSaM mAm-ava kAmAkshi
            
            samashTi caraNam
            ravi-Adi nava graha-udayE rasAlanga nATaka kriyE
            
            Devanagari
            
            पलल्लवि
            avyAja karuNA kaTAkshi
            
            समष्टि चरणम्
            ravyAdi nava grahOdayE
        """.trimIndent()

        val blocks = blocker.buildBlocks(rawText).blocks
        
        // Debug output
        blocks.forEach { println("Block: ${it.label}, Lines: ${it.lines.size}") }

        // We expect TextBlocker to find ALL of them (it's dumb)
        // usage in WebScrapingService should filter them.
        
        // Check labels
        val labels = blocks.map { it.label }
        // Depending on regex matching:
        // 1. ENGLISH (from "English")
        // 2. PALLAVI
        // 3. SAMASHTI_CHARANAM (if matched)
        // 4. MADHYAMAKALA
        // 5. ENGLISH (from "English - Word Division")
        // 6. PALLAVI
        // 7. SAMASHTI_CHARANAM
        // 8. DEVANAGARI
        // 9. PALLAVI
        // 10. SAMASHTI_CHARANAM
        
        assertTrue(labels.contains("PALLAVI"))
        assertTrue(labels.contains("DEVANAGARI"))
        assertTrue(labels.count { it == "PALLAVI" } >= 2, "Should find multiple Pallavis")
    }

    @Test
    fun `test filtering logic`() {
        val rawText = """
            English
            
            pallavi
            Line 1
            
            samashTi caraNam
            Line 2
            
            (madhyama kAla sAhityam)
            Line 3
            
            English - Word Division
            
            pallavi
            Line 1 repeat
            
            Devanagari
            
            पलल्लवि
            Line 1 dev
        """.trimIndent()

        val blocks = blocker.buildBlocks(rawText).blocks
        
        // Replicating WebScrapingService logic
        val languageLabels = setOf("DEVANAGARI", "TAMIL", "TELUGU", "KANNADA", "MALAYALAM", "HINDI", "SANSKRIT", "ENGLISH", "LATIN")
        val sections = mutableListOf<String>() // Just storing labels for this test
        var foundFirstSection = false

        for (block in blocks) {
            if (block.label in languageLabels) {
                if (foundFirstSection) {
                    break
                }
                continue
            }
            
            // Map labels roughly
            val isSection = block.label in setOf("PALLAVI", "SAMASHTI_CHARANAM", "MADHYAMAKALA")
            if (isSection) {
                sections.add(block.label)
                foundFirstSection = true
            }
        }

        // Expected: PALLAVI, SAMASHTI_CHARANAM, MADHYAMAKALA
        // "Word Division" (ENGLISH) should trigger break.
        // "Devanagari" should be unreachable.
        
        assertEquals(listOf("PALLAVI", "SAMASHTI_CHARANAM", "MADHYAMAKALA"), sections)
    }

    @Test
    fun `test thyagaraja vaibhavam structure`() {
        val rawText = """
            Transliteration–Telugu
            Transliteration as per Modified Harvard-Kyoto (HK) Convention
            
            amma rAvamma-kalyANi
            
            pallavi
            amma rA(va)mma tuLas(a)mma
            
            anupallavi
            nemmadini nIv(i)ha parammul(o)saguduv(a)nucu
            
            caraNam
            nI mRdu tanuvunu kani nI parimaLamunu kani
            
            Gist
            O Mother tuLasi!
            
            Word-by-word Meaning
            
            pallavi
            amma rA-amma tuLasi-amma
            
            Devanagari
            
            प. अम&#2381;म र&#2366;वम&#2381;म त&#2369;ळ(स)म&#2381;म
            
            अ. न&#2374;म&#2381;मद&#2367;न&#2367; न&#2368;(व&#2367;)ह
            
            च. न&#2368; म&#2371;द&#2369; तन&#2369;व&#2369;न&#2369; कन&#2367;
        """.trimIndent()

        val blocks = blocker.buildBlocks(rawText).blocks
        
        // Replicating WebScrapingService logic
        val languageLabels = setOf("DEVANAGARI", "TAMIL", "TELUGU", "KANNADA", "MALAYALAM", "HINDI", "SANSKRIT", "ENGLISH", "LATIN", "WORD_DIVISION", "MEANING", "GIST", "NOTES", "VARIATIONS")
        val sections = mutableListOf<String>()
        var foundFirstSection = false

        for (block in blocks) {
            if (block.label in languageLabels) {
                if (foundFirstSection) {
                    break
                }
                continue
            }
            
            val isSection = block.label in setOf("PALLAVI", "ANUPALLAVI", "CHARANAM", "SAMASHTI_CHARANAM", "MADHYAMAKALA")
            if (isSection) {
                sections.add(block.label)
                foundFirstSection = true
            }
        }

        // Expected: PALLAVI, ANUPALLAVI, CHARANAM
        // "Gist" should trigger break.
        assertEquals(listOf("PALLAVI", "ANUPALLAVI", "CHARANAM"), sections)
    }
}
