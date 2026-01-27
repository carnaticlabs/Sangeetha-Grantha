package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.config.AutoApprovalConfig
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import kotlinx.serialization.json.Json
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import kotlin.uuid.Uuid
import kotlin.time.Instant
import com.sangita.grantha.backend.api.services.Candidate
import com.sangita.grantha.backend.api.services.ResolutionResult
import com.sangita.grantha.backend.api.services.DeduplicationService
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.shared.domain.model.TalaDto

class AutoApprovalServiceTest {

    private lateinit var importReviewer: ImportReviewer
    private lateinit var config: AutoApprovalConfig
    private lateinit var service: AutoApprovalService

    @BeforeEach
    fun setUp() {
        importReviewer = mockk(relaxed = true)
        config = AutoApprovalConfig.fromEnvironment() // Use default config
        service = AutoApprovalService(importReviewer, config)
    }

    @Test
    fun `should reject non-pending imports`() {
        val import = createTestImport(status = ImportStatusDto.APPROVED)
        assertFalse(service.shouldAutoApprove(import))
    }

    @Test
    fun `should reject imports with low quality score`() {
        val import = createTestImport(
            qualityScore = 0.70, // Below 0.90 threshold
            qualityTier = "FAIR",
            resolutionData = createHighConfidenceResolutionData()
        )
        assertFalse(service.shouldAutoApprove(import))
    }

    @Test
    fun `should reject imports with wrong quality tier`() {
        val import = createTestImport(
            qualityScore = 0.95,
            qualityTier = "POOR", // Not in EXCELLENT, GOOD tiers
            resolutionData = createHighConfidenceResolutionData()
        )
        assertFalse(service.shouldAutoApprove(import))
    }

    @Test
    fun `should reject imports without minimal metadata when required`() {
        val import = createTestImport(
            rawTitle = null, // Missing title
            rawLyrics = "Some lyrics",
            qualityScore = 0.95,
            qualityTier = "EXCELLENT",
            resolutionData = createHighConfidenceResolutionData()
        )
        assertFalse(service.shouldAutoApprove(import))
    }

    @Test
    fun `should reject imports with low composer confidence`() {
        val import = createTestImport(
            qualityScore = 0.95,
            qualityTier = "EXCELLENT",
            resolutionData = createResolutionData(composerConfidence = "LOW")
        )
        assertFalse(service.shouldAutoApprove(import))
    }

    @Test
    fun `should reject imports with low raga confidence`() {
        val import = createTestImport(
            qualityScore = 0.95,
            qualityTier = "EXCELLENT",
            resolutionData = createResolutionData(ragaConfidence = "MEDIUM")
        )
        assertFalse(service.shouldAutoApprove(import))
    }

    @Test
    fun `should reject imports with high-confidence duplicates`() {
        val import = createTestImport(
            qualityScore = 0.95,
            qualityTier = "EXCELLENT",
            resolutionData = createHighConfidenceResolutionData(),
            duplicateCandidates = createDuplicateCandidates(confidence = "HIGH")
        )
        assertFalse(service.shouldAutoApprove(import))
    }

    @Test
    fun `should approve imports meeting all criteria`() {
        val import = createTestImport(
            qualityScore = 0.95,
            qualityTier = "EXCELLENT",
            rawTitle = "Test Krithi",
            rawLyrics = "Full lyrics text",
            resolutionData = createHighConfidenceResolutionData(),
            duplicateCandidates = createDuplicateCandidates(confidence = "LOW")
        )
        assertTrue(service.shouldAutoApprove(import))
    }

    @Test
    fun `should approve imports with GOOD quality tier`() {
        val import = createTestImport(
            qualityScore = 0.85,
            qualityTier = "GOOD",
            rawTitle = "Test Krithi",
            rawLyrics = "Full lyrics text",
            resolutionData = createHighConfidenceResolutionData()
        )
        assertTrue(service.shouldAutoApprove(import))
    }

    @Test
    fun `should use custom configuration correctly`() {
        val permissiveConfig = AutoApprovalConfig(
            minQualityScore = 0.70,
            qualityTiers = setOf("EXCELLENT", "GOOD", "FAIR"),
            requireRagaMatch = false // Don't require raga
        )
        val permissiveService = AutoApprovalService(importReviewer, permissiveConfig)

        val import = createTestImport(
            qualityScore = 0.75,
            qualityTier = "FAIR",
            rawTitle = "Test Krithi",
            rawLyrics = "Full lyrics text",
            resolutionData = createResolutionData(
                composerConfidence = "HIGH",
                ragaConfidence = "LOW" // Would fail with default config
            )
        )
        assertTrue(permissiveService.shouldAutoApprove(import))
    }

    @Test
    fun `should validate configuration on initialization`() {
        val invalidConfig = AutoApprovalConfig(
            minQualityScore = 1.5, // Invalid: > 1.0
            minComposerConfidence = -0.1 // Invalid: < 0.0
        )

        assertThrows(IllegalArgumentException::class.java) {
            AutoApprovalService(importReviewer, invalidConfig)
        }
    }

    @Test
    fun `should return current configuration`() {
        val returnedConfig = service.getConfig()
        assertEquals(config, returnedConfig)
    }

    // Helper functions to create test data

    private fun createTestImport(
        status: ImportStatusDto = ImportStatusDto.PENDING,
        qualityScore: Double? = 0.95,
        qualityTier: String? = "EXCELLENT",
        rawTitle: String? = "Test Krithi",
        rawLyrics: String? = "Full lyrics",
        resolutionData: String? = null,
        duplicateCandidates: String? = null
    ): ImportedKrithiDto {
        return ImportedKrithiDto(
            id = Uuid.random(),
            importSourceId = Uuid.random(),
            sourceKey = "https://example.com/krithi",
            rawTitle = rawTitle,
            rawLyrics = rawLyrics,
            rawComposer = "Thyagaraja",
            rawRaga = "Kalyani",
            rawTala = "Adi",
            rawDeity = null,
            rawTemple = null,
            rawLanguage = "Telugu",
            parsedPayload = null,
            resolutionData = resolutionData,
            duplicateCandidates = duplicateCandidates,
            importStatus = status,
            mappedKrithiId = null,
            reviewerNotes = null,
            qualityScore = qualityScore,
            qualityTier = qualityTier,
            createdAt = kotlin.time.Instant.fromEpochSeconds(1769126400), // 2026-01-23
            reviewedAt = null,
            importBatchId = null
        )
    }

    private fun createHighConfidenceResolutionData(): String {
        return createResolutionData("HIGH", "HIGH", "HIGH")
    }

    private fun createResolutionData(
        composerConfidence: String = "HIGH",
        ragaConfidence: String = "HIGH",
        talaConfidence: String = "HIGH"
    ): String {
        val result = ResolutionResult(
            composerCandidates = listOf(
                Candidate(createTestComposer("Thyagaraja"), 95, composerConfidence)
            ),
            ragaCandidates = listOf(
                Candidate(createTestRaga("Kalyani"), 92, ragaConfidence)
            ),
            talaCandidates = listOf(
                Candidate(createTestTala("Adi"), 90, talaConfidence)
            ),
            resolved = true
        )
        return Json.encodeToString(ResolutionResult.serializer(), result)
    }

    private fun createDuplicateCandidates(confidence: String = "LOW"): String {
        val result = DeduplicationService.DeduplicationResult(
            matches = listOf(
                DeduplicationService.DuplicateMatch(
                    krithiId = "k1",
                    reason = "Similar Krithi",
                    confidence = confidence
                )
            )
        )
        return Json.encodeToString(DeduplicationService.DeduplicationResult.serializer(), result)
    }

    private fun createTestComposer(name: String): ComposerDto {
        return ComposerDto(
            id = Uuid.random(),
            name = name,
            nameNormalized = "test",
            createdAt = Instant.fromEpochSeconds(1769126400),
            updatedAt = Instant.fromEpochSeconds(1769126400)
        )
    }

    private fun createTestRaga(name: String): RagaDto {
        return RagaDto(
            id = Uuid.random(),
            name = name,
            nameNormalized = "test",
            createdAt = Instant.fromEpochSeconds(1769126400),
            updatedAt = Instant.fromEpochSeconds(1769126400)
        )
    }

    private fun createTestTala(name: String): TalaDto {
        return TalaDto(
            id = Uuid.random(),
            name = name,
            nameNormalized = "test",
            createdAt = Instant.fromEpochSeconds(1769126400),
            updatedAt = Instant.fromEpochSeconds(1769126400)
        )
    }
}
