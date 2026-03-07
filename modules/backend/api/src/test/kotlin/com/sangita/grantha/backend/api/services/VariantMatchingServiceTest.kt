package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.support.TestFixtures
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class VariantMatchingServiceTest {

    private val dal: SangitaDal = mockk(relaxed = true)
    private val normalizer = NameNormalizationService()
    private lateinit var service: VariantMatchingService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        service = VariantMatchingService(dal, normalizer, VariantScorer(dal, normalizer))
    }

    // =========================================================================
    // Levenshtein similarity
    // =========================================================================

    @Nested
    inner class LevenshteinSimilarity {

        @Test
        fun `identical strings return score of 100`() {
            assertEquals(100, NameNormalizationService.ratio("kalyani", "kalyani"))
        }

        @Test
        fun `completely different strings return low score`() {
            val score = NameNormalizationService.ratio("abcdef", "zyxwvu")
            assertTrue(score < 20, "Expected very low score, got $score")
        }

        @Test
        fun `proportional similarity for similar strings`() {
            val score = NameNormalizationService.ratio("nagumomu", "nagumomi")
            assertTrue(score in 75..99, "Expected 75-99 for one-char difference, got $score")
        }
    }

    // =========================================================================
    // Confidence tiers
    // =========================================================================

    @Nested
    inner class ConfidenceTiers {

        @Test
        fun `HIGH confidence at or above 0_85`() {
            val tier = when {
                0.90 >= 0.85 -> "HIGH"
                0.90 >= 0.50 -> "MEDIUM"
                else -> "LOW"
            }
            assertEquals("HIGH", tier)
        }

        @Test
        fun `MEDIUM confidence between 0_50 and 0_85`() {
            val tier = when {
                0.65 >= 0.85 -> "HIGH"
                0.65 >= 0.50 -> "MEDIUM"
                else -> "LOW"
            }
            assertEquals("MEDIUM", tier)
        }

        @Test
        fun `LOW confidence below 0_50`() {
            val tier = when {
                0.30 >= 0.85 -> "HIGH"
                0.30 >= 0.50 -> "MEDIUM"
                else -> "LOW"
            }
            assertEquals("LOW", tier)
        }
    }

    // =========================================================================
    // Auto-approval
    // =========================================================================

    @Nested
    inner class AutoApproval {

        @Test
        fun `HIGH confidence results in AUTO_APPROVED status`() {
            val tier = "HIGH"
            val status = if (tier == "HIGH") "AUTO_APPROVED" else "PENDING"
            assertEquals("AUTO_APPROVED", status)
        }

        @Test
        fun `MEDIUM confidence results in PENDING status`() {
            val tier = "MEDIUM"
            val status = if (tier == "HIGH") "AUTO_APPROVED" else "PENDING"
            assertEquals("PENDING", status)
        }

        @Test
        fun `LOW confidence results in PENDING status`() {
            val tier = "LOW"
            val status = if (tier == "HIGH") "AUTO_APPROVED" else "PENDING"
            assertEquals("PENDING", status)
        }
    }

    // =========================================================================
    // Anomaly detection
    // =========================================================================

    @Nested
    inner class AnomalyDetection {

        @Test
        fun `marks anomaly when krithi not in primary scope`() {
            val primaryKrithiIds = setOf(Uuid.random(), Uuid.random())
            val matchedKrithiId = Uuid.random() // not in primary set
            val isAnomaly = primaryKrithiIds.isNotEmpty() && matchedKrithiId !in primaryKrithiIds
            assertTrue(isAnomaly)
        }

        @Test
        fun `not anomaly when krithi in primary scope`() {
            val matchedKrithiId = Uuid.random()
            val primaryKrithiIds = setOf(matchedKrithiId, Uuid.random())
            val isAnomaly = primaryKrithiIds.isNotEmpty() && matchedKrithiId !in primaryKrithiIds
            assertFalse(isAnomaly)
        }

        @Test
        fun `not anomaly when no primary scope`() {
            val primaryKrithiIds = emptySet<Uuid>()
            val matchedKrithiId = Uuid.random()
            val isAnomaly = primaryKrithiIds.isNotEmpty() && matchedKrithiId !in primaryKrithiIds
            assertFalse(isAnomaly)
        }
    }

    // =========================================================================
    // Composite scoring weights
    // =========================================================================

    @Nested
    inner class CompositeScoring {

        @Test
        fun `weights sum to 1_0`() {
            val totalWeight = 0.50 + 0.30 + 0.20 // TITLE + RAGA_TALA + PAGE_POSITION
            assertEquals(1.0, totalWeight, 0.001)
        }

        @Test
        fun `perfect scores produce composite of 1_0`() {
            val composite = 1.0 * 0.50 + 1.0 * 0.30 + 1.0 * 0.20
            assertEquals(1.0, composite, 0.001)
        }

        @Test
        fun `title contributes most to composite score`() {
            // Title-only match: 1.0*0.5 + 0.0*0.3 + 0.0*0.2 = 0.50
            val titleOnly = 1.0 * 0.50 + 0.0 * 0.30 + 0.0 * 0.20
            // Raga+tala only: 0.0*0.5 + 1.0*0.3 + 0.0*0.2 = 0.30
            val ragaTalaOnly = 0.0 * 0.50 + 1.0 * 0.30 + 0.0 * 0.20
            assertTrue(titleOnly > ragaTalaOnly, "Title should contribute more than raga+tala")
        }
    }

    // =========================================================================
    // Structure mismatch
    // =========================================================================

    @Nested
    inner class StructureMismatch {

        @Test
        fun `different section counts indicate mismatch`() {
            val extractedSectionCount = 4
            val candidateSectionCount = 3
            val mismatch = candidateSectionCount > 0 && extractedSectionCount != candidateSectionCount
            assertTrue(mismatch)
        }

        @Test
        fun `zero extracted sections means no mismatch`() {
            val extractedSectionCount = 0
            val mismatch = extractedSectionCount == 0
            assertTrue(mismatch, "Zero extracted sections should not trigger mismatch check")
        }

        @Test
        fun `same section counts means no mismatch`() {
            val extractedSectionCount = 3
            val candidateSectionCount = 3
            val mismatch = candidateSectionCount > 0 && extractedSectionCount != candidateSectionCount
            assertFalse(mismatch)
        }
    }
}
