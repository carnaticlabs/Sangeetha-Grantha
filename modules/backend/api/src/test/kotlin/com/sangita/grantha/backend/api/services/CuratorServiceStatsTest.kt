package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
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
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * TRACK-133 (folded-in cleanup): guards that CuratorService.getStats() computes
 * sectionIssuesCount via its SQL aggregate identically to the previous in-memory
 * row-diff over krithi_sections + krithi_lyric_sections. The aggregate replaced two
 * full table scans per dashboard load.
 */
class CuratorServiceStatsTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var curatorService: CuratorService
    private lateinit var importService: IImportService
    private lateinit var extractionProcessor: ExtractionResultProcessor

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        curatorService = CuratorService(dal)

        val dummyReviewer = object : ImportReviewer {
            override suspend fun reviewImport(
                id: kotlin.uuid.Uuid,
                request: ImportReviewRequest,
                reviewerUserId: kotlin.uuid.Uuid?
            ) = throw UnsupportedOperationException("Not used in tests")
        }
        val autoApproval = AutoApprovalService(dummyReviewer)
        val env = ApiEnvironment(adminToken = "test", geminiApiKey = "test")
        val normalizer = NameNormalizationService()
        val entityResolver = EntityResolutionServiceImpl(dal, normalizer)
        val lyricPersistence = LyricVariantPersistenceService(dal)
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
    }

    @Test
    fun `sectionIssuesCount aggregate equals in-memory row-diff`() = runTest {
        // Baseline: whatever the seed provides (expected 0 mismatches on a clean seed).
        val baseline = referenceRowDiff()
        assertEquals(baseline, curatorService.getStats().sectionIssuesCount)

        // Introduce a krithi whose Telugu variant is under-segmented (2 of 3 canonical
        // sections) while its Sanskrit variant matches — exactly one mismatch variant row.
        approveKrithiWithMismatch()

        val expected = referenceRowDiff()
        assertEquals(baseline + 1, expected, "fixture should add exactly one mismatch variant row")
        assertEquals(expected, curatorService.getStats().sectionIssuesCount)
    }

    /** Recomputes the mismatch count the old way: per-(krithi,language) variant vs canonical, in Kotlin. */
    private suspend fun referenceRowDiff(): Long = DatabaseFactory.dbQuery {
        val sectionCountCol = KrithiSectionsTable.id.count()
        val canonicalCounts = KrithiSectionsTable
            .select(KrithiSectionsTable.krithiId, sectionCountCol)
            .groupBy(KrithiSectionsTable.krithiId)
            .associate { it[KrithiSectionsTable.krithiId] to it[sectionCountCol] }

        val lyricSectionCountCol = KrithiLyricSectionsTable.id.count()
        val withSections = KrithiLyricSectionsTable
            .innerJoin(KrithiLyricVariantsTable, { KrithiLyricSectionsTable.lyricVariantId }, { KrithiLyricVariantsTable.id })
            .select(KrithiLyricVariantsTable.krithiId, KrithiLyricVariantsTable.language, lyricSectionCountCol)
            .groupBy(KrithiLyricVariantsTable.krithiId, KrithiLyricVariantsTable.language)
            .associate { (it[KrithiLyricVariantsTable.krithiId] to it[KrithiLyricVariantsTable.language]) to it[lyricSectionCountCol] }

        // Include variants with zero lyric sections (LEFT JOIN semantics), matching the SQL aggregate.
        val allVariants = KrithiLyricVariantsTable
            .select(KrithiLyricVariantsTable.krithiId, KrithiLyricVariantsTable.language)
            .map { it[KrithiLyricVariantsTable.krithiId] to it[KrithiLyricVariantsTable.language] }
            .distinct()

        allVariants.count { (krithiId, language) ->
            val actual = withSections[krithiId to language] ?: 0L
            val expected = canonicalCounts[krithiId] ?: 0L
            actual != expected
        }.toLong()
    }

    private suspend fun approveKrithiWithMismatch() {
        val sourceUrl = "http://example.com/curator-stats-mismatch-fixture"
        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        val importId = submitted.first().id

        val (tasks, _) = dal.extractionQueue.list(format = listOf("HTML"), limit = 50)
        val queuedTask = tasks.single { it.sourceUrl == sourceUrl }

        val extraction = CanonicalExtractionDto(
            title = "Curator Stats Mismatch Fixture",
            composer = "Tyagaraja",
            ragas = listOf(CanonicalRagaDto(name = "Atana")),
            tala = "Adi",
            sections = listOf(
                CanonicalSectionDto(type = CanonicalSectionType.PALLAVI, order = 1),
                CanonicalSectionDto(type = CanonicalSectionType.ANUPALLAVI, order = 2),
                CanonicalSectionDto(type = CanonicalSectionType.CHARANAM, order = 3),
            ),
            lyricVariants = listOf(
                // Matches canonical (3 sections).
                CanonicalLyricVariantDto(
                    language = "sa",
                    script = "devanagari",
                    sections = listOf(
                        CanonicalLyricSectionDto(sectionOrder = 1, text = "अ"),
                        CanonicalLyricSectionDto(sectionOrder = 2, text = "ब"),
                        CanonicalLyricSectionDto(sectionOrder = 3, text = "स"),
                    ),
                ),
                // Under-segmented (2 of 3) -> one mismatch variant row.
                CanonicalLyricVariantDto(
                    language = "te",
                    script = "telugu",
                    sections = listOf(
                        CanonicalLyricSectionDto(sectionOrder = 1, text = "అ"),
                        CanonicalLyricSectionDto(sectionOrder = 2, text = "బ"),
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

        val report = extractionProcessor.processCompletedExtractions(batchSize = 10)
        assertEquals(0, report.errorTasks)

        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED, reviewerNotes = "Curator stats fixture"),
            reviewerUserId = null,
        )

        val approved = dal.imports.findById(importId)
        assertNotNull(approved?.mappedKrithiId, "fixture krithi should be created")
    }
}
