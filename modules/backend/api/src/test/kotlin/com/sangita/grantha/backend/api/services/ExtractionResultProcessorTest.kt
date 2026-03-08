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
import com.sangita.grantha.backend.dal.DatabaseFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        importService = ImportServiceImpl(dal, env, entityResolver, normalizer, ImportReportGenerator(), LyricVariantPersistenceService(dal)) { autoApproval }

        val krithiMatcherService = KrithiMatcherService(dal, normalizer)
        val structuralVotingProcessor = StructuralVotingProcessor(dal, com.sangita.grantha.backend.api.services.scraping.StructuralVotingEngine())
        extractionProcessor = ExtractionResultProcessor(
            dal = dal,
            krithiMatcherService = krithiMatcherService,
            structuralVotingProcessor = structuralVotingProcessor,
        )

        // Seed the "PDF Extraction (Unmatched)" import source required by KrithiMatcherService
        kotlinx.coroutines.runBlocking {
            DatabaseFactory.dbQuery {
                TransactionManager.current().connection.prepareStatement(
                    """INSERT INTO import_sources (name, description, source_tier, supported_formats)
                       VALUES ('PDF Extraction (Unmatched)', 'For unmatched extraction results', 3, '{PDF}')
                       ON CONFLICT DO NOTHING""",
                    false
                ).executeUpdate()
            }
        }
    }

    @Test
    fun `unmatched extraction creates pending import for manual review`() = runTest {
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

        // With no pre-existing krithis to match, unmatched extractions should create
        // a pending import for manual curator review — NOT auto-create a new krithi
        val pendingImports = dal.imports.listImports(
            status = com.sangita.grantha.backend.dal.enums.ImportStatus.PENDING,
        )
        val unmatchedImport = pendingImports.find {
            it.sourceKey == "$sourceUrl::Akhilandesvari Rakshamaam"
        }
        assertNotNull(unmatchedImport, "Expected a pending import for unmatched extraction")
        assertEquals("Akhilandesvari Rakshamaam", unmatchedImport.rawTitle)
        assertNull(unmatchedImport.mappedKrithiId, "Unmatched extraction should not be auto-mapped to a krithi")
    }
}
