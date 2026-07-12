package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
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
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression tests for section-order matching during lyric variant persistence.
 *
 * Validates the fix for the off-by-one bug where `sectionOrder - 1` was used
 * against 1-based `orderIndex`, causing the first section (Pallavi) to be dropped
 * and the last section (Charanam) to never match.
 *
 * Three persistence paths are covered:
 *  1. KrithiMatcherService.persistMissingLyricVariants — re-extraction of existing krithis
 *  2. KrithiCreationFromExtractionService.persistLyricVariants — new krithi from extraction
 *  3. LyricVariantPersistenceService.persistFromCanonical — import approval (was already correct)
 */
@Tag("integration")
class SectionOrderMatchingTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var importService: IImportService
    private lateinit var extractionProcessor: ExtractionResultProcessor
    private lateinit var krithiMatcherService: KrithiMatcherService
    private lateinit var normalizer: NameNormalizationService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        normalizer = NameNormalizationService()

        val dummyReviewer = object : ImportReviewer {
            override suspend fun reviewImport(
                id: kotlin.uuid.Uuid,
                request: ImportReviewRequest,
                reviewerUserId: kotlin.uuid.Uuid?
            ) = throw UnsupportedOperationException("Not used in tests")
        }
        val autoApproval = AutoApprovalService(dummyReviewer)
        val env = ApiEnvironment(adminToken = "test", geminiApiKey = "test")
        val entityResolver = EntityResolutionServiceImpl(dal, normalizer)
        val lyricPersistence = LyricVariantPersistenceService(dal)
        importService = ImportServiceImpl(
            dal, env, entityResolver, normalizer,
            ImportReportGenerator(), lyricPersistence
        ) { autoApproval }

        krithiMatcherService = KrithiMatcherService(dal, normalizer)
        val structuralVotingProcessor = StructuralVotingProcessor(
            dal, com.sangita.grantha.backend.api.services.scraping.StructuralVotingEngine()
        )
        extractionProcessor = ExtractionResultProcessor(
            dal = dal,
            krithiMatcherService = krithiMatcherService,
            structuralVotingProcessor = structuralVotingProcessor,
        )

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

    private fun dikshitarExtraction(
        sourceUrl: String,
        language: String = "sa",
        script: String = "latin",
        pallaviText: String = "shrI rAmachandro rakSatu mAm",
        anupallaviText: String = "pArijAta taru mUla vasantam",
        charanamText: String = "karAravinda dhRta kodaNDa",
    ) = CanonicalExtractionDto(
        title = "Sri Ramachandro Rakshatumam",
        composer = "Muthuswami Dikshitar",
        ragas = listOf(CanonicalRagaDto(name = "Manirangu")),
        tala = "Adi",
        sections = listOf(
            CanonicalSectionDto(type = CanonicalSectionType.PALLAVI, order = 1),
            CanonicalSectionDto(type = CanonicalSectionType.ANUPALLAVI, order = 2),
            CanonicalSectionDto(type = CanonicalSectionType.CHARANAM, order = 3),
        ),
        lyricVariants = listOf(
            CanonicalLyricVariantDto(
                language = language,
                script = script,
                sections = listOf(
                    CanonicalLyricSectionDto(sectionOrder = 1, text = pallaviText),
                    CanonicalLyricSectionDto(sectionOrder = 2, text = anupallaviText),
                    CanonicalLyricSectionDto(sectionOrder = 3, text = charanamText),
                ),
            ),
        ),
        sourceUrl = sourceUrl,
        sourceName = "test-source",
        sourceTier = 4,
        extractionMethod = CanonicalExtractionMethod.HTML_JSOUP,
    )

    /**
     * Create a krithi via import+approve, then simulate a re-extraction that matches it
     * and adds a new language variant. All 3 lyric sections must be linked.
     *
     * Before the fix, sectionOrder=1 matched orderIndex=0 (miss), sectionOrder=2 matched
     * orderIndex=1 (PALLAVI), sectionOrder=3 matched orderIndex=2 (ANUPALLAVI) — yielding
     * only 2 linked sections with wrong content mapping.
     */
    @Test
    fun `re-extraction of existing krithi links all three lyric sections`() = runTest {
        val sourceUrl = "http://example.com/test-section-order-reextract"

        // 1. Create the krithi via import + extraction + approval (the "correct" path)
        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        val importId = submitted.first().id
        val (tasks, _) = dal.extractionQueue.list(format = listOf("HTML"), limit = 50)
        val queuedTask = tasks.single { it.sourceUrl == sourceUrl }

        val firstExtraction = dikshitarExtraction(sourceUrl)
        dal.extractionQueue.markDone(
            id = queuedTask.id,
            resultPayload = Json.encodeToString(listOf(firstExtraction)),
            resultCount = 1,
            extractionMethod = "HTML_JSOUP",
            extractorVersion = "test-worker",
        )
        extractionProcessor.processCompletedExtractions(batchSize = 10)

        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED, reviewerNotes = "test"),
            reviewerUserId = null
        )

        val approvedImport = dal.imports.findById(importId)!!
        val krithiId = approvedImport.mappedKrithiId!!

        // Verify baseline: 3 sections, 1 variant with 3 lyric sections
        val sections = dal.krithis.getSections(krithiId)
        assertEquals(3, sections.size, "Krithi should have 3 sections")
        val baselineVariants = dal.krithiLyrics.getLyricVariants(krithiId)
        assertEquals(1, baselineVariants.size)
        assertEquals(3, baselineVariants.first().sections.size, "Baseline variant should have 3 lyric sections")

        // 2. Simulate re-extraction with a NEW language variant (Tamil)
        val tamilExtraction = dikshitarExtraction(
            sourceUrl = "http://example.com/test-section-order-reextract-tamil",
            language = "ta",
            script = "tamil",
            pallaviText = "ஸ்ரீ ராமசந்த்ரோ",
            anupallaviText = "பாரிஜாத தரு",
            charanamText = "கராரவிந்த",
        )

        // Call persistMissingLyricVariants directly (the path triggered by re-extraction)
        krithiMatcherService.persistMissingLyricVariants(krithiId, tamilExtraction)

        // 3. Verify: Tamil variant should have ALL 3 lyric sections
        val allVariants = dal.krithiLyrics.getLyricVariants(krithiId)
        assertEquals(2, allVariants.size, "Should now have 2 lyric variants (SA + TA)")

        val tamilVariant = allVariants.find { it.variant.language == LanguageCodeDto.TA }
        assertNotNull(tamilVariant, "Tamil variant should exist")
        assertEquals(
            3, tamilVariant.sections.size,
            "Tamil variant must have 3 lyric sections (Pallavi + Anupallavi + Charanam). " +
                "If only 2, the off-by-one bug (sectionOrder - 1) is still present."
        )

        // Verify section text mapping is correct (not shifted)
        assertTrue(
            tamilVariant.sections.any { it.text.contains("ஸ்ரீ ராமசந்த்ரோ") },
            "Pallavi text should be present"
        )
        assertTrue(
            tamilVariant.sections.any { it.text.contains("பாரிஜாத தரு") },
            "Anupallavi text should be present"
        )
        assertTrue(
            tamilVariant.sections.any { it.text.contains("கராரவிந்த") },
            "Charanam text should be present"
        )
    }

    /**
     * New krithi created directly from extraction must also link all sections.
     * Tests KrithiCreationFromExtractionService.persistLyricVariants.
     */
    @Test
    fun `new krithi from extraction links all three lyric sections`() = runTest {
        val sourceUrl = "http://example.com/test-section-order-new-krithi"

        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        val importId = submitted.first().id
        val (tasks, _) = dal.extractionQueue.list(format = listOf("HTML"), limit = 50)
        val queuedTask = tasks.single { it.sourceUrl == sourceUrl }

        val extraction = dikshitarExtraction(sourceUrl)
        dal.extractionQueue.markDone(
            id = queuedTask.id,
            resultPayload = Json.encodeToString(listOf(extraction)),
            resultCount = 1,
            extractionMethod = "HTML_JSOUP",
            extractorVersion = "test-worker",
        )

        val report = extractionProcessor.processCompletedExtractions(batchSize = 10)
        assertEquals(1, report.processedTasks)

        // The import should now be enriched — approve it to create the krithi
        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED, reviewerNotes = "test"),
            reviewerUserId = null
        )

        val approved = dal.imports.findById(importId)!!
        val krithiId = approved.mappedKrithiId!!

        val sections = dal.krithis.getSections(krithiId)
        assertEquals(3, sections.size, "Krithi should have 3 sections (P + A + C)")

        val variants = dal.krithiLyrics.getLyricVariants(krithiId)
        assertTrue(variants.isNotEmpty(), "Should have at least one lyric variant")

        val variant = variants.first()
        assertEquals(
            3, variant.sections.size,
            "Lyric variant must have 3 sections. If only 2, the off-by-one is present in " +
                "KrithiCreationFromExtractionService.persistLyricVariants."
        )
    }

    /**
     * Verify section-order matching works for krithis with non-standard section counts
     * (e.g. Pallavi + Samashti Charanam = 2 sections).
     */
    @Test
    fun `two-section krithi links both lyric sections`() = runTest {
        val sourceUrl = "http://example.com/test-section-order-two-sections"

        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        val importId = submitted.first().id
        val (tasks, _) = dal.extractionQueue.list(format = listOf("HTML"), limit = 50)
        val queuedTask = tasks.single { it.sourceUrl == sourceUrl }

        val extraction = CanonicalExtractionDto(
            title = "Vatapi Ganapatim Bhaje",
            composer = "Muthuswami Dikshitar",
            ragas = listOf(CanonicalRagaDto(name = "Hamsadhwani")),
            tala = "Adi",
            sections = listOf(
                CanonicalSectionDto(type = CanonicalSectionType.PALLAVI, order = 1),
                CanonicalSectionDto(type = CanonicalSectionType.SAMASHTI_CHARANAM, order = 2),
            ),
            lyricVariants = listOf(
                CanonicalLyricVariantDto(
                    language = "sa",
                    script = "latin",
                    sections = listOf(
                        CanonicalLyricSectionDto(sectionOrder = 1, text = "vAtApi gaNapatim bhajE"),
                        CanonicalLyricSectionDto(sectionOrder = 2, text = "bhUtEshvara pArishAdaM"),
                    ),
                ),
            ),
            sourceUrl = sourceUrl,
            sourceName = "test-source",
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
        extractionProcessor.processCompletedExtractions(batchSize = 10)

        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED, reviewerNotes = "test"),
            reviewerUserId = null
        )

        val approved = dal.imports.findById(importId)!!
        val krithiId = approved.mappedKrithiId!!

        val sections = dal.krithis.getSections(krithiId)
        assertEquals(2, sections.size, "Should have 2 sections (Pallavi + Samashti Charanam)")

        val variants = dal.krithiLyrics.getLyricVariants(krithiId)
        assertTrue(variants.isNotEmpty())
        assertEquals(
            2, variants.first().sections.size,
            "Both lyric sections must be linked for a 2-section krithi"
        )

        // Now add a second variant via persistMissingLyricVariants
        val tamilExtraction = extraction.copy(
            sourceUrl = "http://example.com/test-section-order-two-sections-tamil",
            lyricVariants = listOf(
                CanonicalLyricVariantDto(
                    language = "ta",
                    script = "tamil",
                    sections = listOf(
                        CanonicalLyricSectionDto(sectionOrder = 1, text = "வாதாபி கணபதிம்"),
                        CanonicalLyricSectionDto(sectionOrder = 2, text = "பூதேச்வர"),
                    ),
                ),
            ),
        )
        krithiMatcherService.persistMissingLyricVariants(krithiId, tamilExtraction)

        val allVariants = dal.krithiLyrics.getLyricVariants(krithiId)
        val tamilVariant = allVariants.find { it.variant.language == LanguageCodeDto.TA }
        assertNotNull(tamilVariant)
        assertEquals(
            2, tamilVariant.sections.size,
            "Tamil variant must have 2 linked sections for the 2-section krithi"
        )
    }
}
