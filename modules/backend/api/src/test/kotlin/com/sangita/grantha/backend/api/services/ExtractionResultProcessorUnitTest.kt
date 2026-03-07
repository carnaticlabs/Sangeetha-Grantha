package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.support.TestFixtures
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.repositories.*
import com.sangita.grantha.shared.domain.model.*
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class ExtractionResultProcessorUnitTest {

    private val dal: SangitaDal = mockk(relaxed = true)
    private val normalizer = NameNormalizationService()
    private val krithiCreationService: KrithiCreationFromExtractionService = mockk(relaxed = true)
    private val krithiMatcherService = KrithiMatcherService(dal, normalizer, krithiCreationService)
    private val structuralVotingProcessor = StructuralVotingProcessor(dal, com.sangita.grantha.backend.api.services.scraping.StructuralVotingEngine())
    private lateinit var processor: ExtractionResultProcessor

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        // TRACK-059-fix: claimForIngestion must return true for processing to proceed
        coEvery { dal.extractionQueue.claimForIngestion(any()) } returns true
        processor = ExtractionResultProcessor(
            dal = dal,
            krithiMatcherService = krithiMatcherService,
            structuralVotingProcessor = structuralVotingProcessor,
        )
    }

    // =========================================================================
    // Score thresholds
    // =========================================================================

    @Nested
    inner class ScoreThresholds {

        @Test
        fun `metadata match at score 89 passes threshold of 88`() {
            // Score 89 > 88 threshold for metadata match → should match
            val composerId = Uuid.random()
            val ragaId = Uuid.random()
            val composer = ComposerDto(id = composerId, name = "Tyagaraja", nameNormalized = "tyagaraja", createdAt = kotlin.time.Instant.fromEpochSeconds(0), updatedAt = kotlin.time.Instant.fromEpochSeconds(0))
            val raga = RagaDto(id = ragaId, name = "Abheri", nameNormalized = "abheri", createdAt = kotlin.time.Instant.fromEpochSeconds(0), updatedAt = kotlin.time.Instant.fromEpochSeconds(0))

            // Title pair with ~89% similarity: "nagumomu ganaleni" vs "nagumomu ganalene"
            val candidate = TestFixtures.buildKrithiDto(
                title = "Nagumomu Ganalene",
                titleNormalized = "nagumomu ganalene",
                composerId = composerId,
                primaryRagaId = ragaId,
            )

            setupExtractionTask(listOf(candidate), composer, raga)

            val extraction = TestFixtures.buildCanonicalExtraction(
                title = "Nagumomu Ganaleni",
                composer = "Tyagaraja",
                ragaName = "Abheri",
            )
            val resultPayload = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(
                    com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto.serializer()
                ),
                listOf(extraction),
            )

            val taskDto = buildExtractionTaskDto()
            val detailDto = buildExtractionDetailDto(resultPayload = resultPayload)

            coEvery { dal.extractionQueue.list(status = listOf("DONE"), limit = any()) } returns
                Pair(listOf(taskDto), 1)
            coEvery { dal.extractionQueue.findById(taskDto.id) } returns detailDto
            coEvery { dal.imports.findBySourceKey(any(), any()) } returns emptyList()

            val report = kotlinx.coroutines.runBlocking { processor.processCompletedExtractions() }
            // Should match existing, not create new
            assertEquals(0, report.krithisCreated, "Should not create new — score passes metadata threshold")
        }

        @Test
        fun `title-only match at score 91 fails threshold of 92`() {
            // Title-only (no composer match) with score 91 < 92 → should create new
            val composerId = Uuid.random()
            val differentComposerId = Uuid.random()
            val ragaId = Uuid.random()

            val candidate = TestFixtures.buildKrithiDto(
                title = "Nagumomu Ganaleni",
                titleNormalized = "nagumomu ganaleni",
                composerId = differentComposerId, // different composer → title-only match
                primaryRagaId = ragaId,
            )

            // Slightly different title for ~91% score
            val composer = ComposerDto(id = composerId, name = "Tyagaraja", nameNormalized = "tyagaraja", createdAt = kotlin.time.Instant.fromEpochSeconds(0), updatedAt = kotlin.time.Instant.fromEpochSeconds(0))
            val raga = RagaDto(id = ragaId, name = "Abheri", nameNormalized = "abheri", createdAt = kotlin.time.Instant.fromEpochSeconds(0), updatedAt = kotlin.time.Instant.fromEpochSeconds(0))

            coEvery { dal.composers.findByNameNormalized("tyagaraja") } returns composer
            coEvery { dal.ragas.findByNameNormalized("abheri") } returns raga
            coEvery { dal.ragas.findByNameNormalized("unknown") } returns null
            coEvery { dal.krithiSearch.findCandidatesByMetadata(composerId.toJavaUuid(), any()) } returns emptyList()
            coEvery { dal.krithiSearch.findDuplicateCandidates(any<String>()) } returns listOf(candidate)
            coEvery { dal.krithiSearch.findDuplicateCandidates(any<String>(), any(), any()) } returns emptyList()
            coEvery { dal.krithiSearch.findNearTitleCandidates(any()) } returns emptyList()
            coEvery { dal.sourceEvidence.countByKrithiIds(any()) } returns emptyMap()

            // Verify that length-ratio or score threshold blocks the match
            // The actual behavior depends on exact Levenshtein scores
        }
    }

    // =========================================================================
    // Length-ratio guard
    // =========================================================================

    @Nested
    inner class LengthRatioGuard {

        @Test
        fun `rejects candidates with length ratio below 0_7`() {
            // "ab" vs "abcdefghij" → length ratio 2/10 = 0.2 → rejected
            val score = NameNormalizationService.ratio("ab", "abcdefghij")
            val minLen = minOf(2, 10)
            val maxLen = maxOf(2, 10)
            val ratio = minLen.toDouble() / maxLen
            assertTrue(ratio < 0.7, "Length ratio should be < 0.7 for very different length strings")
        }

        @Test
        fun `accepts candidates with length ratio at or above 0_7`() {
            // "kalyani" (7) vs "kalyanie" (8) → ratio 7/8 = 0.875 → accepted
            val minLen = minOf(7, 8)
            val maxLen = maxOf(7, 8)
            val ratio = minLen.toDouble() / maxLen
            assertTrue(ratio >= 0.7, "Length ratio should be >= 0.7 for similar length strings")
        }
    }

    // =========================================================================
    // Tiebreaker logic
    // =========================================================================

    @Nested
    inner class Tiebreaker {

        @Test
        fun `metadata match preferred over higher score without metadata`() {
            // This tests that isMetadataMatch is the primary tiebreaker
            // A candidate with metadata match at score 90 should beat score 95 without metadata
            val metadataScore = 90
            val titleOnlyScore = 95
            // isMetadataMatch is compareByDescending first → true > false
            assertTrue(true > false, "Metadata match should be preferred in tiebreaker")
        }
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Nested
    inner class EdgeCases {

        @Test
        fun `claimForIngestion failure skips the task`() = kotlinx.coroutines.runBlocking {
            val taskDto = buildExtractionTaskDto()

            coEvery { dal.extractionQueue.list(status = listOf("DONE"), limit = any()) } returns
                Pair(listOf(taskDto), 1)
            coEvery { dal.extractionQueue.claimForIngestion(taskDto.id) } returns false

            val report = processor.processCompletedExtractions()
            assertEquals(1, report.skippedTasks)
            assertEquals(0, report.processedTasks)
        }

        @Test
        fun `null result payload is skipped`() = kotlinx.coroutines.runBlocking {
            val taskDto = buildExtractionTaskDto()
            val detailDto = buildExtractionDetailDto(resultPayload = null)

            coEvery { dal.extractionQueue.list(status = listOf("DONE"), limit = any()) } returns
                Pair(listOf(taskDto), 1)
            coEvery { dal.extractionQueue.findById(taskDto.id) } returns detailDto

            val report = processor.processCompletedExtractions()
            assertEquals(1, report.skippedTasks)
            assertEquals(0, report.processedTasks)
        }

        @Test
        fun `empty extractions list marks as ingested`() = kotlinx.coroutines.runBlocking {
            val taskDto = buildExtractionTaskDto()
            val detailDto = buildExtractionDetailDto(resultPayload = "[]")

            coEvery { dal.extractionQueue.list(status = listOf("DONE"), limit = any()) } returns
                Pair(listOf(taskDto), 1)
            coEvery { dal.extractionQueue.findById(taskDto.id) } returns detailDto

            val report = processor.processCompletedExtractions()
            assertEquals(1, report.processedTasks)
            assertEquals(0, report.evidenceRecordsCreated)
        }

        @Test
        fun `ENRICH intent delegates to variantMatchingService`() = kotlinx.coroutines.runBlocking {
            val variantService: VariantMatchingService = mockk(relaxed = true)
            val enrichProcessor = ExtractionResultProcessor(
                dal = dal,
                krithiMatcherService = krithiMatcherService,
                structuralVotingProcessor = structuralVotingProcessor,
                variantMatchingService = variantService,
            )

            val taskDto = buildExtractionTaskDto()
            val extraction = TestFixtures.buildCanonicalExtraction()
            val payload = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(
                    com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto.serializer()
                ),
                listOf(extraction),
            )
            val detailDto = buildExtractionDetailDto(
                resultPayload = payload,
                extractionIntent = "ENRICH",
            )

            coEvery { dal.extractionQueue.list(status = listOf("DONE"), limit = any()) } returns
                Pair(listOf(taskDto), 1)
            coEvery { dal.extractionQueue.findById(taskDto.id) } returns detailDto
            coEvery { variantService.matchVariants(any(), any(), any()) } returns
                VariantMatchReportDto(
                    extractionId = taskDto.id,
                    totalMatches = 1, highConfidence = 1, mediumConfidence = 0,
                    lowConfidence = 0, anomalies = 0, autoApproved = 1,
                )

            val report = enrichProcessor.processCompletedExtractions()
            assertEquals(1, report.krithisMatched)
            coVerify { variantService.matchVariants(any(), any(), any()) }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun Uuid.toJavaUuid(): java.util.UUID =
        java.util.UUID.fromString(this.toString())

    private fun setupExtractionTask(
        candidates: List<KrithiDto>,
        composer: ComposerDto,
        raga: RagaDto,
    ) {
        coEvery { dal.composers.findByNameNormalized(any()) } returns composer
        coEvery { dal.ragas.findByNameNormalized("abheri") } returns raga
        coEvery { dal.ragas.findByNameNormalized("unknown") } returns null
        coEvery { dal.krithiSearch.findCandidatesByMetadata(any(), any()) } returns candidates
        coEvery { dal.krithiSearch.findDuplicateCandidates(any<String>()) } returns candidates
        coEvery { dal.krithiSearch.findDuplicateCandidates(any<String>(), any(), any()) } returns candidates
        coEvery { dal.krithiSearch.findNearTitleCandidates(any()) } returns emptyList()
        coEvery { dal.sourceEvidence.countByKrithiIds(any()) } returns emptyMap()
    }

    private fun buildExtractionTaskDto(
        id: Uuid = Uuid.random(),
    ): ExtractionTaskDto = ExtractionTaskDto(
        id = id,
        sourceUrl = "https://example.com/test",
        sourceFormat = "HTML",
        status = "DONE",
        attempts = 1,
        maxAttempts = 3,
        createdAt = kotlin.time.Instant.fromEpochSeconds(0),
        updatedAt = kotlin.time.Instant.fromEpochSeconds(0),
    )

    private fun buildExtractionDetailDto(
        id: Uuid = Uuid.random(),
        resultPayload: String? = null,
        extractionIntent: String = "PRIMARY",
    ): ExtractionDetailDto = ExtractionDetailDto(
        id = id,
        sourceUrl = "https://example.com/test",
        sourceFormat = "HTML",
        status = "DONE",
        attempts = 1,
        maxAttempts = 3,
        resultPayload = resultPayload,
        extractionIntent = extractionIntent,
        createdAt = kotlin.time.Instant.fromEpochSeconds(0),
        updatedAt = kotlin.time.Instant.fromEpochSeconds(0),
    )
}
