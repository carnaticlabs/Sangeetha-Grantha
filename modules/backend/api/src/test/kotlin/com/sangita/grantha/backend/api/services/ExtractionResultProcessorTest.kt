package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.support.IntegrationTestBase
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionMethod
import com.sangita.grantha.shared.domain.model.import.CanonicalLyricSectionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalLyricVariantDto
import com.sangita.grantha.shared.domain.model.import.CanonicalRagaDto
import com.sangita.grantha.shared.domain.model.import.CanonicalSectionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalSectionType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExtractionResultProcessorTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var importService: IImportService
    private lateinit var extractionProcessor: ExtractionResultProcessor

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        val dummyReviewer = object : ImportReviewer {
            override suspend fun reviewImport(
                id: kotlin.uuid.Uuid,
                request: ImportReviewRequest
            ) = throw UnsupportedOperationException("Not used in tests")
        }
        val autoApproval = AutoApprovalService(dummyReviewer)
        val env = ApiEnvironment(adminToken = "test", geminiApiKey = "test")
        val normalizer = NameNormalizationService()
        val entityResolver = EntityResolutionServiceImpl(dal, normalizer)
        importService = ImportServiceImpl(dal, env, entityResolver, normalizer) { autoApproval }

        val krithiCreationService = KrithiCreationFromExtractionService(dal, normalizer)
        extractionProcessor = ExtractionResultProcessor(
            dal = dal,
            normalizer = normalizer,
            krithiCreationService = krithiCreationService,
        )
    }

    @Test
    fun `html import url flows through ingestion and links import to evidence-backed krithi`() = runTest {
        val sourceUrl = "http://guru-guha.blogspot.com/2007/07/dikshitar-kriti-akhilandesvari-raksha.html"

        val submitted = importService.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "WebScraper",
                    sourceKey = sourceUrl,
                )
            )
        )
        assertEquals(1, submitted.size)
        val importId = submitted.first().id

        val (tasks, _) = dal.extractionQueue.list(format = listOf("HTML"), limit = 50)
        val queuedTask = tasks.single { it.sourceUrl == sourceUrl }
        assertEquals("PENDING", queuedTask.status)

        val extractionPayload = listOf(
            CanonicalExtractionDto(
                title = "Akhilandesvari Rakshamaam",
                composer = "Muthuswami Dikshitar",
                ragas = listOf(CanonicalRagaDto(name = "Dwijavanti")),
                tala = "Adi",
                sections = listOf(
                    CanonicalSectionDto(type = CanonicalSectionType.PALLAVI, order = 1),
                    CanonicalSectionDto(type = CanonicalSectionType.CHARANAM, order = 2),
                ),
                lyricVariants = listOf(
                    CanonicalLyricVariantDto(
                        language = "sa",
                        script = "latin",
                        sections = listOf(
                            CanonicalLyricSectionDto(sectionOrder = 1, text = "akhilANDEsvari rakSamAm"),
                            CanonicalLyricSectionDto(sectionOrder = 2, text = "citsabhEsvari cinmayi"),
                        ),
                    )
                ),
                sourceUrl = sourceUrl,
                sourceName = "guru-guha.blogspot.com",
                sourceTier = 4,
                extractionMethod = CanonicalExtractionMethod.HTML_JSOUP,
            )
        )

        val markedDone = dal.extractionQueue.markDone(
            id = queuedTask.id,
            resultPayload = Json.encodeToString(extractionPayload),
            resultCount = extractionPayload.size,
            extractionMethod = "HTML_JSOUP",
            extractorVersion = "test-worker",
        )
        assertTrue(markedDone)

        val processingReport = extractionProcessor.processCompletedExtractions(batchSize = 10)
        assertEquals(1, processingReport.processedTasks)
        assertEquals(0, processingReport.errorTasks)

        val queueAfter = dal.extractionQueue.findById(queuedTask.id)
        assertNotNull(queueAfter)
        assertEquals("INGESTED", queueAfter.status)

        val updatedImport = dal.imports.findById(importId)
        assertNotNull(updatedImport)
        assertEquals(ImportStatusDto.MAPPED, updatedImport.importStatus)
        assertNotNull(updatedImport.mappedKrithiId)

        val evidence = dal.sourceEvidence.getKrithiEvidence(updatedImport.mappedKrithiId!!)
        assertNotNull(evidence)
        assertTrue(evidence.sources.any { it.sourceUrl == sourceUrl })
    }
}
