package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.ExtractionIntent
import com.sangita.grantha.shared.domain.model.*
import io.ktor.server.plugins.BadRequestException
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.uuid.Uuid

class SourcingServiceTest {

    private val dal: SangitaDal = mockk(relaxed = true)
    private lateinit var service: SourcingService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        service = SourcingService(dal)
    }

    // =========================================================================
    // createExtraction
    // =========================================================================

    @Nested
    inner class CreateExtraction {

        @Test
        fun `throws BadRequestException for invalid extractionIntent`() = runBlocking {
            val request = CreateExtractionRequestDto(
                sourceUrl = "https://example.com",
                sourceFormat = "HTML",
                extractionIntent = "INVALID_INTENT",
            )

            assertThrows<BadRequestException> {
                runBlocking { service.createExtraction(request, null) }
            }
        }

        @Test
        fun `accepts PRIMARY intent`() = runBlocking {
            val request = CreateExtractionRequestDto(
                sourceUrl = "https://example.com",
                sourceFormat = "HTML",
                extractionIntent = "PRIMARY",
            )

            val detailDto = ExtractionDetailDto(
                id = Uuid.random(),
                sourceUrl = "https://example.com",
                sourceFormat = "HTML",
                status = "PENDING",
                attempts = 0,
                maxAttempts = 3,
                extractionIntent = "PRIMARY",
                createdAt = kotlin.time.Instant.fromEpochSeconds(0),
                updatedAt = kotlin.time.Instant.fromEpochSeconds(0),
            )
            coEvery { dal.extractionQueue.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns detailDto

            val result = service.createExtraction(request, null)
            assertEquals("PRIMARY", result.extractionIntent)
            coVerify { dal.auditLogs.append(action = "CREATE_EXTRACTION", entityTable = "extraction_queue", entityId = any(), actorUserId = any(), metadata = any()) }
        }

        @Test
        fun `accepts ENRICH intent`() = runBlocking {
            val request = CreateExtractionRequestDto(
                sourceUrl = "https://example.com",
                sourceFormat = "PDF",
                extractionIntent = "ENRICH",
            )

            val detailDto = ExtractionDetailDto(
                id = Uuid.random(),
                sourceUrl = "https://example.com",
                sourceFormat = "PDF",
                status = "PENDING",
                attempts = 0,
                maxAttempts = 3,
                extractionIntent = "ENRICH",
                createdAt = kotlin.time.Instant.fromEpochSeconds(0),
                updatedAt = kotlin.time.Instant.fromEpochSeconds(0),
            )
            coEvery { dal.extractionQueue.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns detailDto

            val result = service.createExtraction(request, null)
            assertEquals("ENRICH", result.extractionIntent)
        }
    }

    // =========================================================================
    // deactivateSource
    // =========================================================================

    @Nested
    inner class DeactivateSource {

        @Test
        fun `throws BadRequestException when source not found`() {
            coEvery { dal.sourceRegistry.findDetailById(any()) } returns null

            assertThrows<BadRequestException> {
                runBlocking { service.deactivateSource(Uuid.random(), null) }
            }
        }

        @Test
        fun `logs audit when source exists`() = runBlocking {
            val sourceId = Uuid.random()
            coEvery { dal.sourceRegistry.findDetailById(sourceId) } returns ImportSourceDetailDto(
                id = sourceId,
                name = "test-source",
                sourceTier = 3,
                supportedFormats = emptyList(),
                composerAffinity = emptyMap(),
                createdAt = kotlin.time.Instant.fromEpochSeconds(0),
                updatedAt = kotlin.time.Instant.fromEpochSeconds(0),
            )

            service.deactivateSource(sourceId, null)

            coVerify { dal.auditLogs.append(action = "DEACTIVATE_SOURCE", entityTable = "import_sources", entityId = sourceId, actorUserId = any(), metadata = any()) }
        }
    }

    // =========================================================================
    // retryExtraction
    // =========================================================================

    @Nested
    inner class RetryExtraction {

        @Test
        fun `audit logged on success`() = runBlocking {
            val id = Uuid.random()
            coEvery { dal.extractionQueue.retry(id) } returns true

            val result = service.retryExtraction(id, null)
            assertTrue(result)
            coVerify { dal.auditLogs.append(action = "RETRY_EXTRACTION", entityTable = "extraction_queue", entityId = id, actorUserId = any(), metadata = any()) }
        }

        @Test
        fun `no audit on failure`() = runBlocking {
            val id = Uuid.random()
            coEvery { dal.extractionQueue.retry(id) } returns false

            val result = service.retryExtraction(id, null)
            assertFalse(result)
            coVerify(exactly = 0) { dal.auditLogs.append(any(), any(), any(), any(), any()) }
        }
    }
}
