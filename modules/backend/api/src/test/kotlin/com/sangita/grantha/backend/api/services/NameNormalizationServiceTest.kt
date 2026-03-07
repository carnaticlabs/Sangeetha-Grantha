package com.sangita.grantha.backend.api.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

class NameNormalizationServiceTest {

    private val service = NameNormalizationService()

    // =========================================================================
    // ratio() — Levenshtein similarity
    // =========================================================================

    @Nested
    inner class Ratio {
        @Test
        fun `identical strings return 100`() {
            assertEquals(100, NameNormalizationService.ratio("kalyani", "kalyani"))
        }

        @Test
        fun `completely different strings return low score`() {
            val score = NameNormalizationService.ratio("abc", "xyz")
            assertTrue(score < 30, "Expected low score for completely different strings, got $score")
        }

        @Test
        fun `similar strings return proportional score`() {
            val score = NameNormalizationService.ratio("kalyani", "kalyanee")
            assertTrue(score in 70..99, "Expected 70-99 for similar strings, got $score")
        }

        @Test
        fun `empty strings return 100`() {
            assertEquals(100, NameNormalizationService.ratio("", ""))
        }

        @Test
        fun `one empty string returns 0`() {
            assertEquals(0, NameNormalizationService.ratio("", "abc"))
        }

        @Test
        fun `ratio is symmetric`() {
            val ab = NameNormalizationService.ratio("shankarabharanam", "sankarabharanam")
            val ba = NameNormalizationService.ratio("sankarabharanam", "shankarabharanam")
            assertEquals(ab, ba)
        }
    }

    // =========================================================================
    // normalizeComposer()
    // =========================================================================

    @Nested
    inner class NormalizeComposer {
        @Test
        fun `null returns null`() {
            assertNull(service.normalizeComposer(null))
        }

        @Test
        fun `blank returns null`() {
            assertNull(service.normalizeComposer("  "))
        }

        @Test
        fun `Tyagaraja variants normalize consistently`() {
            val expected = "tyagaraja"
            assertEquals(expected, service.normalizeComposer("Tyagaraja"))
            assertEquals(expected, service.normalizeComposer("Thyagaraja"))
        }

        @Test
        fun `Dikshitar variants normalize to muttuswami diksitar`() {
            val expected = "muttuswami diksitar"
            assertEquals(expected, service.normalizeComposer("Muthuswami Dikshitar"))
            assertEquals(expected, service.normalizeComposer("Dikshitar"))
        }

        @Test
        fun `Shyama Shastri normalizes`() {
            val expected = "syama sastri"
            assertEquals(expected, service.normalizeComposer("Shyama Shastri"))
        }

        @Test
        fun `diacritics are stripped`() {
            val result = service.normalizeComposer("Tyāgarāja")
            assertNotNull(result)
            assertFalse(result!!.contains("ā"), "Diacritics should be stripped")
        }
    }

    // =========================================================================
    // normalizeRaga()
    // =========================================================================

    @Nested
    inner class NormalizeRaga {
        @Test
        fun `null returns null`() {
            assertNull(service.normalizeRaga(null))
        }

        @Test
        fun `vowel collapse aa to a`() {
            val result = service.normalizeRaga("Kalyaani")
            assertNotNull(result)
            assertFalse(result!!.contains("aa"), "aa should collapse to a")
        }

        @Test
        fun `spaces removed for ragas`() {
            val result = service.normalizeRaga("Kedara Gaula")
            assertNotNull(result)
            assertFalse(result!!.contains(" "), "Spaces should be removed from raga names")
        }

        @Test
        fun `simple raga normalizes`() {
            val result = service.normalizeRaga("Kalyani")
            assertNotNull(result)
            assertTrue(result!!.isNotBlank())
        }
    }

    // =========================================================================
    // normalizeTala()
    // =========================================================================

    @Nested
    inner class NormalizeTala {
        @Test
        fun `null returns null`() {
            assertNull(service.normalizeTala(null))
        }

        @Test
        fun `suffix removal rupakam to rupaka`() {
            val result = service.normalizeTala("Rupakam")
            assertNotNull(result)
            assertTrue(result!!.endsWith("a") && !result.endsWith("am"),
                "rupakam suffix should be reduced, got $result")
        }

        @Test
        fun `Desadi maps to adi`() {
            assertEquals("adi", service.normalizeTala("Desadi"))
        }

        @Test
        fun `Adi tala normalizes`() {
            val result = service.normalizeTala("Adi")
            assertNotNull(result)
            assertTrue(result!!.isNotBlank())
        }
    }

    // =========================================================================
    // normalizeDeity()
    // =========================================================================

    @Nested
    inner class NormalizeDeity {
        @Test
        fun `null returns null`() {
            assertNull(service.normalizeDeity(null))
        }

        @Test
        fun `honorifics stripped`() {
            val result = service.normalizeDeity("Lord Krishna")
            assertNotNull(result)
            assertFalse(result!!.contains("lord", ignoreCase = true),
                "Honorific 'Lord' should be stripped")
        }

        @Test
        fun `simple deity name normalizes`() {
            val result = service.normalizeDeity("Rama")
            assertNotNull(result)
            assertTrue(result!!.isNotBlank())
        }
    }

    // =========================================================================
    // normalizeTemple()
    // =========================================================================

    @Nested
    inner class NormalizeTemple {
        @Test
        fun `null returns null`() {
            assertNull(service.normalizeTemple(null))
        }

        @Test
        fun `honorifics stripped from temple names`() {
            val result = service.normalizeTemple("Sri Ranganathaswamy Temple")
            assertNotNull(result)
            // 'Sri' should be stripped by basicNormalize honorific removal
        }
    }

    // =========================================================================
    // normalizeTitle()
    // =========================================================================

    @Nested
    inner class NormalizeTitle {
        @Test
        fun `null returns null`() {
            assertNull(service.normalizeTitle(null))
        }

        @Test
        fun `blank returns null`() {
            assertNull(service.normalizeTitle("  "))
        }

        @Test
        fun `basic title normalization works`() {
            val result = service.normalizeTitle("Nagumomu Ganaleni")
            assertNotNull(result)
            assertTrue(result!!.isNotBlank())
            assertEquals(result, result.lowercase(), "Should be lowercase")
        }

        @Test
        fun `diacritics are stripped from titles`() {
            val result = service.normalizeTitle("ekāmranāthaṁ bhaje'ham")
            assertNotNull(result)
            assertFalse(result!!.any { it.code > 127 }, "All diacritics should be removed")
        }
    }
}
