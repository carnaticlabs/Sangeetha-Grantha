package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.support.TestFixtures
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.*
import com.sangita.grantha.shared.domain.model.import.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Instant
import kotlin.uuid.Uuid

class KrithiCreationFromExtractionServiceTest {

    private val dal: SangitaDal = mockk(relaxed = true)
    private val normalizer: NameNormalizationService = mockk(relaxed = true)
    private lateinit var service: KrithiCreationFromExtractionService
    private val epoch = Instant.fromEpochSeconds(0)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        service = KrithiCreationFromExtractionService(dal, normalizer)
    }

    private fun composer(id: Uuid = Uuid.random(), name: String = "Tyagaraja", norm: String = "tyagaraja") =
        ComposerDto(id = id, name = name, nameNormalized = norm, createdAt = epoch, updatedAt = epoch)

    private fun raga(id: Uuid = Uuid.random(), name: String = "Abheri", norm: String = "abheri") =
        RagaDto(id = id, name = name, nameNormalized = norm, createdAt = epoch, updatedAt = epoch)

    private fun tala(id: Uuid = Uuid.random(), name: String = "Adi", norm: String = "adi") =
        TalaDto(id = id, name = name, nameNormalized = norm, createdAt = epoch, updatedAt = epoch)

    private fun deity(id: Uuid = Uuid.random(), name: String = "Rama", norm: String = "rama") =
        DeityDto(id = id, name = name, nameNormalized = norm, createdAt = epoch, updatedAt = epoch)

    // =========================================================================
    // Composer resolution
    // =========================================================================

    @Nested
    inner class ComposerResolution {

        @Test
        fun `returns null when composer cannot be normalized`() = runBlocking {
            every { normalizer.normalizeComposer(any()) } returns null
            val result = service.createFromExtraction(TestFixtures.buildCanonicalExtraction(composer = "???"), Uuid.random())
            assertNull(result)
        }

        @Test
        fun `returns null when composer normalizes to blank`() = runBlocking {
            every { normalizer.normalizeComposer(any()) } returns ""
            val result = service.createFromExtraction(TestFixtures.buildCanonicalExtraction(composer = " "), Uuid.random())
            assertNull(result)
        }

        @Test
        fun `creates krithi when composer resolves`() = runBlocking {
            val composerId = Uuid.random()
            val krithiId = Uuid.random()

            every { normalizer.normalizeComposer(any()) } returns "tyagaraja"
            every { normalizer.normalizeRaga(any()) } returns "abheri"
            every { normalizer.normalizeTala(any()) } returns "adi"
            every { normalizer.normalizeTitle(any()) } returns "nagumomu ganaleni"

            coEvery { dal.composers.findOrCreate(any(), any()) } returns composer(composerId)
            coEvery { dal.ragas.findOrCreate(any(), any()) } returns raga()
            coEvery { dal.talas.findOrCreate(any(), any()) } returns tala()
            coEvery { dal.krithis.create(any()) } returns TestFixtures.buildKrithiDto(id = krithiId, composerId = composerId)
            coEvery { dal.krithis.getSections(any()) } returns emptyList()

            val result = service.createFromExtraction(TestFixtures.buildCanonicalExtraction(), Uuid.random())
            assertEquals(krithiId, result)
        }
    }

    // =========================================================================
    // Raga handling
    // =========================================================================

    @Nested
    inner class RagaHandling {

        @Test
        fun `skips placeholder ragas`() = runBlocking {
            val composerId = Uuid.random()
            val krithiId = Uuid.random()

            every { normalizer.normalizeComposer(any()) } returns "tyagaraja"
            every { normalizer.normalizeRaga(any()) } returns "unknown"
            every { normalizer.normalizeTala(any()) } returns "adi"
            every { normalizer.normalizeTitle(any()) } returns "test title"

            coEvery { dal.composers.findOrCreate(any(), any()) } returns composer(composerId)
            coEvery { dal.talas.findOrCreate(any(), any()) } returns tala()
            coEvery { dal.krithis.create(any()) } returns TestFixtures.buildKrithiDto(id = krithiId, composerId = composerId, primaryRagaId = null)
            coEvery { dal.krithis.getSections(any()) } returns emptyList()

            val result = service.createFromExtraction(TestFixtures.buildCanonicalExtraction(ragaName = "Unknown"), Uuid.random())
            assertNotNull(result)
            coVerify(exactly = 0) { dal.ragas.findOrCreate(any(), any()) }
        }

        @Test
        fun `detects ragamalika with multiple ragas`() = runBlocking {
            val composerId = Uuid.random()
            val krithiId = Uuid.random()
            val ragaId1 = Uuid.random()
            val ragaId2 = Uuid.random()

            every { normalizer.normalizeComposer(any()) } returns "tyagaraja"
            every { normalizer.normalizeRaga("Kalyani") } returns "kalyani"
            every { normalizer.normalizeRaga("Bhairavi") } returns "bhairavi"
            every { normalizer.normalizeTala(any()) } returns "adi"
            every { normalizer.normalizeTitle(any()) } returns "test ragamalika"

            coEvery { dal.composers.findOrCreate(any(), any()) } returns composer(composerId)
            coEvery { dal.ragas.findOrCreate("Kalyani", "kalyani") } returns raga(ragaId1, "Kalyani", "kalyani")
            coEvery { dal.ragas.findOrCreate("Bhairavi", "bhairavi") } returns raga(ragaId2, "Bhairavi", "bhairavi")
            coEvery { dal.talas.findOrCreate(any(), any()) } returns tala()
            coEvery { dal.krithis.create(any()) } returns TestFixtures.buildKrithiDto(id = krithiId, composerId = composerId, isRagamalika = true)
            coEvery { dal.krithis.getSections(any()) } returns emptyList()

            val extraction = TestFixtures.buildCanonicalExtraction().copy(
                ragas = listOf(CanonicalRagaDto(name = "Kalyani", order = 1), CanonicalRagaDto(name = "Bhairavi", order = 2)),
            )
            val result = service.createFromExtraction(extraction, Uuid.random())
            assertNotNull(result)
            coVerify { dal.ragas.findOrCreate("Kalyani", "kalyani") }
            coVerify { dal.ragas.findOrCreate("Bhairavi", "bhairavi") }
        }
    }

    // =========================================================================
    // Deity handling
    // =========================================================================

    @Nested
    inner class DeityHandling {

        @Test
        fun `deity resolved when present`() = runBlocking {
            val composerId = Uuid.random()
            val deityId = Uuid.random()
            val krithiId = Uuid.random()

            every { normalizer.normalizeComposer(any()) } returns "tyagaraja"
            every { normalizer.normalizeRaga(any()) } returns "abheri"
            every { normalizer.normalizeTala(any()) } returns "adi"
            every { normalizer.normalizeDeity(any()) } returns "rama"
            every { normalizer.normalizeTitle(any()) } returns "test"

            coEvery { dal.composers.findOrCreate(any(), any()) } returns composer(composerId)
            coEvery { dal.ragas.findOrCreate(any(), any()) } returns raga()
            coEvery { dal.talas.findOrCreate(any(), any()) } returns tala()
            coEvery { dal.deities.findOrCreate(any(), any()) } returns deity(deityId)
            coEvery { dal.krithis.create(any()) } returns TestFixtures.buildKrithiDto(id = krithiId, composerId = composerId, deityId = deityId)
            coEvery { dal.krithis.getSections(any()) } returns emptyList()

            val result = service.createFromExtraction(TestFixtures.buildCanonicalExtraction(deity = "Rama"), Uuid.random())
            assertNotNull(result)
            coVerify { dal.deities.findOrCreate("Rama", "rama") }
        }

        @Test
        fun `deity skipped when null`() = runBlocking {
            val composerId = Uuid.random()
            val krithiId = Uuid.random()

            every { normalizer.normalizeComposer(any()) } returns "tyagaraja"
            every { normalizer.normalizeRaga(any()) } returns "abheri"
            every { normalizer.normalizeTala(any()) } returns "adi"
            every { normalizer.normalizeTitle(any()) } returns "test"

            coEvery { dal.composers.findOrCreate(any(), any()) } returns composer(composerId)
            coEvery { dal.ragas.findOrCreate(any(), any()) } returns raga()
            coEvery { dal.talas.findOrCreate(any(), any()) } returns tala()
            coEvery { dal.krithis.create(any()) } returns TestFixtures.buildKrithiDto(id = krithiId, composerId = composerId)
            coEvery { dal.krithis.getSections(any()) } returns emptyList()

            service.createFromExtraction(TestFixtures.buildCanonicalExtraction(deity = null), Uuid.random())
            coVerify(exactly = 0) { dal.deities.findOrCreate(any(), any()) }
        }
    }

    // =========================================================================
    // Musical form mapping
    // =========================================================================

    @Nested
    inner class MusicalFormMapping {
        @Test
        fun `VARNAM maps correctly`() {
            val extraction = TestFixtures.buildCanonicalExtraction(musicalForm = CanonicalMusicalForm.VARNAM)
            assertEquals(CanonicalMusicalForm.VARNAM, extraction.musicalForm)
        }
    }

    // =========================================================================
    // Title normalization fallback
    // =========================================================================

    @Nested
    inner class TitleFallback {
        @Test
        fun `falls back to alternateTitle when primary normalizes to blank`() = runBlocking {
            val composerId = Uuid.random()
            val krithiId = Uuid.random()

            every { normalizer.normalizeComposer(any()) } returns "tyagaraja"
            every { normalizer.normalizeRaga(any()) } returns "abheri"
            every { normalizer.normalizeTala(any()) } returns "adi"
            every { normalizer.normalizeTitle("???") } returns ""
            every { normalizer.normalizeTitle("Alternate Title") } returns "alternate title"

            coEvery { dal.composers.findOrCreate(any(), any()) } returns composer(composerId)
            coEvery { dal.ragas.findOrCreate(any(), any()) } returns raga()
            coEvery { dal.talas.findOrCreate(any(), any()) } returns tala()
            coEvery { dal.krithis.create(any()) } returns TestFixtures.buildKrithiDto(id = krithiId, composerId = composerId)
            coEvery { dal.krithis.getSections(any()) } returns emptyList()

            val result = service.createFromExtraction(
                TestFixtures.buildCanonicalExtraction(title = "???", alternateTitle = "Alternate Title"),
                Uuid.random(),
            )
            assertNotNull(result)
        }
    }
}
