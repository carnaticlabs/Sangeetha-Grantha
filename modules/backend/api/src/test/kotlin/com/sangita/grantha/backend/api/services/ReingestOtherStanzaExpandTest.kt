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
 * TRACK-133: persistFromCanonical used to skip saveSections when both existing
 * and new sections were OTHER — so 1 blob → 10 ragamalika stanzas never rebuilt
 * the canonical rows, and trailing variant text was dropped.
 */
class ReingestOtherStanzaExpandTest : IntegrationTestBase() {
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
    fun `reingest replaces canonical OTHER blob with expanded OTHER stanzas`() = runTest {
        val sourceUrl = "http://example.com/madhavo-other-expand"
        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        val importId = submitted.first().id

        fun extraction(sectionCount: Int) = CanonicalExtractionDto(
            title = "Dashavatara",
            composer = "Muthuswami Dikshitar",
            ragas = listOf(CanonicalRagaDto(name = "Nata")),
            tala = "Rupaka",
            sections = (1..sectionCount).map {
                CanonicalSectionDto(type = CanonicalSectionType.OTHER, order = it, label = "Stanza $it")
            },
            lyricVariants = listOf(
                CanonicalLyricVariantDto(
                    language = "sa", script = "devanagari",
                    sections = (1..sectionCount).map {
                        CanonicalLyricSectionDto(sectionOrder = it, text = "stanza $it")
                    },
                ),
            ),
            sourceUrl = sourceUrl,
            sourceName = "example.com",
            sourceTier = 3,
            extractionMethod = CanonicalExtractionMethod.HTML_JSOUP,
        )

        setPayload(
            importId,
            Json.encodeToString(extraction(1)),
            extraSql = ", raw_title = 'Dashavatara', raw_composer = 'Muthuswami Dikshitar', raw_raga = 'Nata', raw_tala = 'Rupaka'",
        )
        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED),
            reviewerUserId = null,
        )
        val mappedId = dal.imports.findById(importId)?.mappedKrithiId
        assertNotNull(mappedId)
        assertEquals(1, dal.krithis.getSections(mappedId).size)

        setPayload(importId, Json.encodeToString(extraction(10)))
        importService.reingestMappedKrithi(importId, reviewerUserId = null)

        assertEquals(10, dal.krithis.getSections(mappedId).size)
        val variants = dal.krithiLyrics.getLyricVariants(mappedId)
        assertEquals(1, variants.size)
        assertEquals(10, variants.first().sections.size)
    }
}
