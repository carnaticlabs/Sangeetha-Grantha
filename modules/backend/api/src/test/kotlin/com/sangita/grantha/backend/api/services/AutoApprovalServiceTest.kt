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

class AutoApprovalServiceTest {

    private lateinit var dal: SangitaDal
    private lateinit var importService: ImportService
    private lateinit var config: AutoApprovalConfig
    private lateinit var service: AutoApprovalService

    @BeforeEach
    fun setUp() {
        dal = mockk(relaxed = true)
        importService = mockk(relaxed = true)
        config = AutoApprovalConfig.fromEnvironment() // Use default config
        service = AutoApprovalService(dal, importService, config)
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
        val permissiveService = AutoApprovalService(dal, importService, permissiveConfig)

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
            AutoApprovalService(dal, importService, invalidConfig)
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
            sourceId = Uuid.random(),
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
            createdAt = "2026-01-23T00:00:00Z",
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
        val data = mapOf(
            "composerCandidates" to listOf(
                mapOf("entity" to mapOf("id" to "c1", "name" to "Thyagaraja"), "confidence" to composerConfidence, "score" to 0.95)
            ),
            "ragaCandidates" to listOf(
                mapOf("entity" to mapOf("id" to "r1", "name" to "Kalyani"), "confidence" to ragaConfidence, "score" to 0.92)
            ),
            "talaCandidates" to listOf(
                mapOf("entity" to mapOf("id" to "t1", "name" to "Adi"), "confidence" to talaConfidence, "score" to 0.90)
            ),
            "resolved" to true
        )
        return Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), Json.parseToJsonElement(Json.encodeToString(kotlinx.serialization.serializer(), data)).jsonObject)
    }

    private fun createDuplicateCandidates(confidence: String = "LOW"): String {
        val data = mapOf(
            "matches" to listOf(
                mapOf("krithiId" to "k1", "title" to "Similar Krithi", "confidence" to confidence, "score" to 0.60)
            )
        )
        return Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), Json.parseToJsonElement(Json.encodeToString(kotlinx.serialization.serializer(), data)).jsonObject)
    }
}
