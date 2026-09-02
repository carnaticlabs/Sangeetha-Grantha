package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * TRACK-133: reingestMappedKrithi() re-persists sections/lyrics onto an
 * already-mapped krithi from the import's latest parsed_payload.
 *
 * Reproduces mAdhavO: a krithi promoted while its extraction had 0 sections, then
 * re-extracted with corrected sections. reviewImport() short-circuits on the
 * already-APPROVED-and-mapped branch, so re-approval was a no-op. The reingest path
 * is the supported way to pick up the corrected sections without minting a new krithi.
 */
class ReingestMappedKrithiTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var importService: IImportService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
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
        importService = ImportServiceImpl(
            dal, env, entityResolver, normalizer,
            ImportReportGenerator(), LyricVariantPersistenceService(dal)
        ) { autoApproval }
    }

    private fun setPayload(importId: kotlin.uuid.Uuid, payloadJson: String, extraSql: String = "") {
        kotlinx.coroutines.runBlocking {
            DatabaseFactory.dbQuery {
                val escaped = payloadJson.replace("'", "''")
                exec(
                    "UPDATE imported_krithis SET parsed_payload = '$escaped'::jsonb, import_status = 'in_review'$extraSql WHERE id = '$importId'"
                )
            }
        }
    }

    @Test
    fun `reingest persists corrected sections and lyrics onto an already-mapped krithi`() = runTest {
        val sourceUrl = "http://example.com/madhavo-reingest"

        // 1. Submit import.
        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        val importId = submitted.first().id

        // 2. Promote with a payload that has NO sections (the original mAdhavO defect):
        //    krithi is created and mapped, but with 0 canonical sections and 0 variants.
        val emptyExtraction = CanonicalExtractionDto(
            title = "Madhavo Mam Patu",
            composer = "Tyagaraja",
            ragas = listOf(CanonicalRagaDto(name = "Kalyani")),
            tala = "Adi",
            sections = emptyList(),
            lyricVariants = emptyList(),
            sourceUrl = sourceUrl,
            sourceName = "example.com",
            sourceTier = 3,
            extractionMethod = CanonicalExtractionMethod.HTML_JSOUP,
        )
        setPayload(
            importId,
            Json.encodeToString(emptyExtraction),
            extraSql = ", raw_title = 'Madhavo Mam Patu', raw_composer = 'Tyagaraja', raw_raga = 'Kalyani', raw_tala = 'Adi'",
        )

        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED, reviewerNotes = "Initial promotion (0 sections)"),
            reviewerUserId = null,
        )

        val approved = dal.imports.findById(importId)
        assertNotNull(approved)
        assertEquals(ImportStatusDto.APPROVED, approved.importStatus)
        val mappedId = approved.mappedKrithiId
        assertNotNull(mappedId, "Import should be mapped after promotion")
        assertEquals(0, dal.krithis.getSections(mappedId).size, "Krithi starts with 0 sections")

        // 3. Re-extraction produced corrected sections — swap the parsed_payload to a
        //    canonical with 3 sections + 2 variants (N = 3). Re-approval is a no-op here.
        val correctedExtraction = CanonicalExtractionDto(
            title = "Madhavo Mam Patu",
            composer = "Tyagaraja",
            ragas = listOf(CanonicalRagaDto(name = "Kalyani")),
            tala = "Adi",
            sections = listOf(
                CanonicalSectionDto(type = CanonicalSectionType.PALLAVI, order = 1),
                CanonicalSectionDto(type = CanonicalSectionType.ANUPALLAVI, order = 2),
                CanonicalSectionDto(type = CanonicalSectionType.CHARANAM, order = 3),
            ),
            lyricVariants = listOf(
                CanonicalLyricVariantDto(
                    language = "sa", script = "devanagari",
                    sections = listOf(
                        CanonicalLyricSectionDto(sectionOrder = 1, text = "माधवो माम् पातु"),
                        CanonicalLyricSectionDto(sectionOrder = 2, text = "मधुर भक्ति"),
                        CanonicalLyricSectionDto(sectionOrder = 3, text = "त्यागराज नुत"),
                    ),
                ),
                CanonicalLyricVariantDto(
                    language = "te", script = "telugu",
                    sections = listOf(
                        CanonicalLyricSectionDto(sectionOrder = 1, text = "మాధవో మామ్ పాతు"),
                        CanonicalLyricSectionDto(sectionOrder = 2, text = "మధుర భక్తి"),
                        CanonicalLyricSectionDto(sectionOrder = 3, text = "త్యాగరాజ నుత"),
                    ),
                ),
            ),
            sourceUrl = sourceUrl,
            sourceName = "example.com",
            sourceTier = 3,
            extractionMethod = CanonicalExtractionMethod.HTML_JSOUP,
        )
        setPayload(importId, Json.encodeToString(correctedExtraction))

        // 4. Reingest — the supported re-persist path.
        importService.reingestMappedKrithi(importId, reviewerUserId = null)

        // 5. VERIFY: krithi now has 3 canonical sections and 3 lyric sections per variant,
        //    still the SAME krithi (no duplicate minted).
        assertEquals(mappedId, dal.imports.findById(importId)?.mappedKrithiId, "Must not remap to a new krithi")

        val sections = dal.krithis.getSections(mappedId)
        assertEquals(3, sections.size, "Reingest should create 3 canonical sections")

        val variants = dal.krithiLyrics.getLyricVariants(mappedId)
        assertEquals(2, variants.size, "Reingest should create 2 lyric variants")
        variants.forEach { v ->
            assertEquals(3, v.sections.size, "Each variant should have 3 lyric sections")
        }
    }

    @Test
    fun `reingest is idempotent — a second run does not duplicate variants`() = runTest {
        val sourceUrl = "http://example.com/madhavo-idempotent"
        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        val importId = submitted.first().id

        val extraction = CanonicalExtractionDto(
            title = "Idempotent Krithi",
            composer = "Tyagaraja",
            ragas = listOf(CanonicalRagaDto(name = "Kalyani")),
            tala = "Adi",
            sections = listOf(
                CanonicalSectionDto(type = CanonicalSectionType.PALLAVI, order = 1),
                CanonicalSectionDto(type = CanonicalSectionType.CHARANAM, order = 2),
            ),
            lyricVariants = listOf(
                CanonicalLyricVariantDto(
                    language = "te", script = "telugu",
                    sections = listOf(
                        CanonicalLyricSectionDto(sectionOrder = 1, text = "పల్లవి"),
                        CanonicalLyricSectionDto(sectionOrder = 2, text = "చరణం"),
                    ),
                ),
            ),
            sourceUrl = sourceUrl,
            sourceName = "example.com",
            sourceTier = 3,
            extractionMethod = CanonicalExtractionMethod.HTML_JSOUP,
        )
        setPayload(
            importId,
            Json.encodeToString(extraction),
            extraSql = ", raw_title = 'Idempotent Krithi', raw_composer = 'Tyagaraja', raw_raga = 'Kalyani', raw_tala = 'Adi'",
        )
        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED),
            reviewerUserId = null,
        )
        val mappedId = dal.imports.findById(importId)?.mappedKrithiId
        assertNotNull(mappedId)

        importService.reingestMappedKrithi(importId, reviewerUserId = null)
        importService.reingestMappedKrithi(importId, reviewerUserId = null)

        assertEquals(1, dal.krithiLyrics.getLyricVariants(mappedId).size, "Repeated reingest must not stack variants")
        assertEquals(2, dal.krithis.getSections(mappedId).size)
    }
}
