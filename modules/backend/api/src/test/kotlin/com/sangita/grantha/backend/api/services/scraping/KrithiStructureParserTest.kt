package com.sangita.grantha.backend.api.services.scraping

import com.sangita.grantha.shared.domain.model.RagaSectionDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KrithiStructureParserTest {

    private val blocker = KrithiStructureParser()

    @Test
    fun `test multi script duplication via extractSections`() {
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
            
            English - Word Division
            
            pallavi
            avyAja karuNA kaTAkshi
            
            Devanagari
            
            पलल्लवि
            avyAja karuNA kaTAkshi
        """.trimIndent()

        val sections = blocker.extractSections(rawText)
        
        // Should only have the first block's sections
        val types = sections.map { it.type }
        assertEquals(listOf(RagaSectionDto.PALLAVI, RagaSectionDto.SAMASHTI_CHARANAM, RagaSectionDto.MADHYAMA_KALA), types)
    }

    @Test
    fun `test dikshitar bala kuchambike structure`() {
        val rawText = """
            pallavi
            bAla kucAmbikE mAmava vara dAyikE SrI
            
            samashTi caraNam
            bAlEndu SEkhari bhakta janAvana Sankari
            (madhyama kAla sAhityam)
            nIla kaNTha manOranjani guru guha janani
            nIrajAsanAdi pOshiNi kadamba vana vAsini
        """.trimIndent()

        val sections = blocker.extractSections(rawText)
        
        assertEquals(3, sections.size)
        assertEquals(RagaSectionDto.PALLAVI, sections[0].type)
        assertEquals(RagaSectionDto.SAMASHTI_CHARANAM, sections[1].type)
        assertEquals(RagaSectionDto.MADHYAMA_KALA, sections[2].type)
        
        assertTrue(sections[2].text.contains("nIla kaNTha"))
        assertTrue(sections[2].text.contains("kadamba vana"))
    }

    @Test
    fun `test thyagaraja vaibhavam structure with extractSections`() {
        val rawText = """
            Transliteration–Telugu
            
            amma rA(va)mma tuLas(a)mma
            
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
        """.trimIndent()

        val sections = blocker.extractSections(rawText)
        
        val types = sections.map { it.type }
        assertEquals(listOf(RagaSectionDto.PALLAVI, RagaSectionDto.ANUPALLAVI, RagaSectionDto.CHARANAM), types)
    }

    @Test
    fun `test noise filtering of pronunciation guides`() {
        val rawText = """
            Pronunciation Guide
            A i I u U
            ch j jh n/J
            ph b bh m
            
            pallavi
            Real Content
        """.trimIndent()

        val sections = blocker.extractSections(rawText)
        assertEquals(1, sections.size)
        assertEquals(RagaSectionDto.PALLAVI, sections[0].type)
        assertEquals("Real Content", sections[0].text)
    }

    @Test
    fun `test madhyama kala short header and tamil subscript normalization`() {
        val rawText = """
            pallavi
            க₁ வா
            
            (m.k)
            நீ வா
        """.trimIndent()

        val sections = blocker.extractSections(rawText)
        val types = sections.map { it.type }
        assertEquals(listOf(RagaSectionDto.PALLAVI, RagaSectionDto.MADHYAMA_KALA), types)
        assertTrue(sections[0].text.contains("க வா"))
    }
}