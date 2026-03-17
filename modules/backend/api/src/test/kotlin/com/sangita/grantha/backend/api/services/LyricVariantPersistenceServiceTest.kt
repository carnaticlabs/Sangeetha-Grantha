package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.support.IntegrationTestBase
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import com.sangita.grantha.shared.domain.model.LanguageCodeDto
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
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TRACK-095: Integration tests for the lyric persistence pipeline.
 *
 * Verifies the full flow: CSV import → extraction enrichment → approval → lyrics/sections persisted.
 * This test class specifically validates the TRACK-094 fix where CanonicalExtractionDto payloads
 * were silently failing to persist lyrics.
 */
class LyricVariantPersistenceServiceTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var importService: IImportService
    private lateinit var extractionProcessor: ExtractionResultProcessor
    private lateinit var lyricPersistence: LyricVariantPersistenceService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        lyricPersistence = LyricVariantPersistenceService(dal)

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
        importService = ImportServiceImpl(
            dal, env, entityResolver, normalizer,
            ImportReportGenerator(), lyricPersistence
        ) { autoApproval }

        val krithiMatcherService = KrithiMatcherService(dal, normalizer)
        val structuralVotingProcessor = StructuralVotingProcessor(
            dal, com.sangita.grantha.backend.api.services.scraping.StructuralVotingEngine()
        )
        extractionProcessor = ExtractionResultProcessor(
            dal = dal,
            krithiMatcherService = krithiMatcherService,
            structuralVotingProcessor = structuralVotingProcessor,
        )

        // Seed required import sources
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

    /**
     * The canonical test: submit import → enrich with CanonicalExtractionDto → approve → verify lyrics persisted.
     * This is the exact flow that failed silently before TRACK-094.
     */
    @Test
    fun `canonical extraction payload persists lyrics and sections on approval`() = runTest {
        val sourceUrl = "http://example.com/test-krithi-pallavi-anupallavi-charanam"

        // 1. Submit import (simulates CSV upload)
        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        assertEquals(1, submitted.size)
        val importId = submitted.first().id

        // 2. Queue extraction and mark done with CanonicalExtractionDto
        val (tasks, _) = dal.extractionQueue.list(format = listOf("HTML"), limit = 50)
        val queuedTask = tasks.single { it.sourceUrl == sourceUrl }

        val extraction = CanonicalExtractionDto(
            title = "Adi Kadu Bhajana",
            composer = "Tyagaraja",
            ragas = listOf(CanonicalRagaDto(name = "Atana")),
            tala = "Adi",
            sections = listOf(
                CanonicalSectionDto(type = CanonicalSectionType.PALLAVI, order = 1),
                CanonicalSectionDto(type = CanonicalSectionType.ANUPALLAVI, order = 2),
                CanonicalSectionDto(type = CanonicalSectionType.CHARANAM, order = 3),
            ),
            lyricVariants = listOf(
                CanonicalLyricVariantDto(
                    language = "sa",
                    script = "devanagari",
                    sections = listOf(
                        CanonicalLyricSectionDto(sectionOrder = 1, text = "अदि कडु भजन"),
                        CanonicalLyricSectionDto(sectionOrder = 2, text = "मधुर भक्ति"),
                        CanonicalLyricSectionDto(sectionOrder = 3, text = "त्यागराज नुत"),
                    ),
                ),
                CanonicalLyricVariantDto(
                    language = "te",
                    script = "telugu",
                    sections = listOf(
                        CanonicalLyricSectionDto(sectionOrder = 1, text = "అది కడు భజన"),
                        CanonicalLyricSectionDto(sectionOrder = 2, text = "మధుర భక్తి"),
                        CanonicalLyricSectionDto(sectionOrder = 3, text = "త్యాగరాజ నుత"),
                    ),
                ),
            ),
            sourceUrl = sourceUrl,
            sourceName = "example.com",
            sourceTier = 4,
            extractionMethod = CanonicalExtractionMethod.HTML_JSOUP,
        )

        dal.extractionQueue.markDone(
            id = queuedTask.id,
            resultPayload = Json.encodeToString(listOf(extraction)),
            resultCount = 1,
            extractionMethod = "HTML_JSOUP",
            extractorVersion = "test-worker",
        )

        // 3. Process extraction (enriches the import with CanonicalExtractionDto payload)
        val report = extractionProcessor.processCompletedExtractions(batchSize = 10)
        assertEquals(1, report.processedTasks)
        assertEquals(0, report.errorTasks)

        // Verify import is now IN_REVIEW with parsed payload
        val enrichedImport = dal.imports.findById(importId)
        assertNotNull(enrichedImport)
        assertEquals(ImportStatusDto.IN_REVIEW, enrichedImport.importStatus)
        assertNotNull(enrichedImport.parsedPayload, "Import should have CanonicalExtractionDto in parsed_payload")

        // 4. Approve the import (this triggers lyric persistence)
        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED, reviewerNotes = "Integration test approval")
        )

        // 5. VERIFY: lyrics, sections, and lyric sections are persisted
        val approvedImport = dal.imports.findById(importId)
        assertNotNull(approvedImport)
        assertEquals(ImportStatusDto.APPROVED, approvedImport.importStatus)
        val mappedId = approvedImport.mappedKrithiId
        assertNotNull(mappedId, "Approved import should have a mapped krithi ID")

        // Verify krithi_sections created (3: Pallavi, Anupallavi, Charanam)
        val sections = dal.krithis.getSections(mappedId)
        assertEquals(3, sections.size, "Should have 3 sections: Pallavi, Anupallavi, Charanam")
        assertEquals("PALLAVI", sections[0].sectionType)
        assertEquals("ANUPALLAVI", sections[1].sectionType)
        assertEquals("CHARANAM", sections[2].sectionType)

        // Verify krithi_lyric_variants created (2: Sanskrit, Telugu)
        val variants = dal.krithiLyrics.getLyricVariants(mappedId)
        assertEquals(2, variants.size, "Should have 2 lyric variants: SA + TE")

        val saVariant = variants.find { it.variant.language == LanguageCodeDto.SA }
        val teVariant = variants.find { it.variant.language == LanguageCodeDto.TE }
        assertNotNull(saVariant, "Should have a Sanskrit variant")
        assertNotNull(teVariant, "Should have a Telugu variant")

        // Verify krithi_lyric_sections linked (3 sections per variant)
        assertEquals(3, saVariant.sections.size, "Sanskrit variant should have 3 lyric sections")
        assertEquals(3, teVariant.sections.size, "Telugu variant should have 3 lyric sections")

        // Verify actual text content
        assertTrue(saVariant.sections.any { it.text.contains("अदि कडु भजन") }, "Sanskrit Pallavi text should match")
        assertTrue(teVariant.sections.any { it.text.contains("అది కడు భజన") }, "Telugu Pallavi text should match")
    }

    /**
     * Verify that the legacy ScrapedKrithiMetadata format still works (backward compatibility).
     */
    @Suppress("DEPRECATION")
    @Test
    fun `legacy scraped metadata payload persists lyrics on approval`() = runTest {
        val sourceUrl = "http://example.com/test-legacy-format"

        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        val importId = submitted.first().id

        // Manually set parsed_payload with ScrapedKrithiMetadata format (legacy)
        val legacyPayload = Json.encodeToString(
            ScrapedKrithiMetadata(
                title = "Entaro Mahanubhavulu",
                composer = "Tyagaraja",
                raga = "Sri",
                tala = "Adi",
                language = "TE",
                lyrics = "Pallavi\nentaro mahanubhavulu\n\nCharanam\ntyagaraja yogavaibhava",
                sections = listOf(
                    ScrapedSectionDto(
                        type = com.sangita.grantha.shared.domain.model.RagaSectionDto.PALLAVI,
                        text = "entaro mahanubhavulu"
                    ),
                    ScrapedSectionDto(
                        type = com.sangita.grantha.shared.domain.model.RagaSectionDto.CHARANAM,
                        text = "tyagaraja yogavaibhava"
                    ),
                ),
            )
        )

        // Enrich the import manually via raw SQL (bypass extraction queue)
        DatabaseFactory.dbQuery {
            val escapedPayload = legacyPayload.replace("'", "''")
            exec(
                "UPDATE imported_krithis SET parsed_payload = '$escapedPayload'::jsonb, import_status = 'in_review' WHERE id = '$importId'"
            )
        }

        // Approve
        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED, reviewerNotes = "Legacy format test")
        )

        val approved = dal.imports.findById(importId)
        assertNotNull(approved)
        val mappedId = approved.mappedKrithiId
        assertNotNull(mappedId)

        val variants = dal.krithiLyrics.getLyricVariants(mappedId)
        assertTrue(variants.isNotEmpty(), "Legacy format should still persist at least one lyric variant")
    }

    /**
     * Verify that a malformed payload logs an error but does not crash the approval flow.
     */
    @Test
    fun `malformed payload does not crash approval`() = runTest {
        val sourceUrl = "http://example.com/test-malformed"

        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        val importId = submitted.first().id

        // Set garbage in parsed_payload + required fields for approval
        DatabaseFactory.dbQuery {
            exec(
                "UPDATE imported_krithis SET parsed_payload = '{\"garbage\": true}'::jsonb, import_status = 'in_review', raw_title = 'Test Krithi', raw_composer = 'Tyagaraja', raw_raga = 'Mohanam', raw_tala = 'Adi' WHERE id = '$importId'"
            )
        }

        // Approval should succeed (krithi created) even if lyric persistence fails gracefully
        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED, reviewerNotes = "Malformed payload test")
        )

        val approved = dal.imports.findById(importId)
        assertNotNull(approved)
        assertEquals(ImportStatusDto.APPROVED, approved.importStatus)
    }
}
