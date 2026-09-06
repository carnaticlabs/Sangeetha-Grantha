package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithiRagasTable
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
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TRACK-139: reingest must write ordered ragamalika membership from the payload
 * (parser-owned), not leave a single bogus raga for SQL (V61) to patch.
 */
class ReingestRagamalikaMetadataTest : IntegrationTestBase() {
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
    fun `reingest sets is_ragamalika and ordered krithi_ragas from payload`() = runTest {
        val sourceUrl = "http://example.com/madhavo-ragamalika-reingest"
        val submitted = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = sourceUrl))
        )
        val importId = submitted.first().id

        val emptyExtraction = CanonicalExtractionDto(
            title = "mAdhavO mAM pAtu",
            composer = "Muthuswami Dikshitar",
            ragas = listOf(CanonicalRagaDto(name = "Unknown")),
            tala = "rupakam",
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
            extraSql = ", raw_title = 'mAdhavO mAM pAtu', raw_composer = 'Muthuswami Dikshitar', raw_raga = 'Unknown', raw_tala = 'rupakam'",
        )
        importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED),
            reviewerUserId = null,
        )
        val mappedId = dal.imports.findById(importId)?.mappedKrithiId
        assertNotNull(mappedId)
        assertEquals(false, dal.krithis.findById(mappedId)?.isRagamalika)

        val dashavatara = listOf(
            "Nāṭṭai", "Gowla", "SrI", "Arabhi", "varALi",
            "Kedaram", "vasanta", "suraTi", "saurAshTraM", "madhyamAvati",
        )
        val corrected = CanonicalExtractionDto(
            title = "mAdhavO mAM pAtu",
            composer = "Muthuswami Dikshitar",
            ragas = dashavatara.mapIndexed { i, name -> CanonicalRagaDto(name = name, order = i + 1) },
            tala = "rupakam",
            sections = (1..10).map { CanonicalSectionDto(type = CanonicalSectionType.OTHER, order = it) },
            lyricVariants = listOf(
                CanonicalLyricVariantDto(
                    language = "en",
                    script = "latin",
                    sections = (1..10).map { CanonicalLyricSectionDto(sectionOrder = it, text = "stanza $it") },
                ),
            ),
            sourceUrl = sourceUrl,
            sourceName = "example.com",
            sourceTier = 3,
            extractionMethod = CanonicalExtractionMethod.HTML_JSOUP,
        )
        setPayload(importId, Json.encodeToString(corrected))
        importService.reingestMappedKrithi(importId, reviewerUserId = null)

        val krithi = dal.krithis.findById(mappedId)
        assertNotNull(krithi)
        assertTrue(krithi.isRagamalika, "Dashavatara must be flagged ragamalika")

        val junction = DatabaseFactory.dbQuery {
            KrithiRagasTable.selectAll()
                .where { KrithiRagasTable.krithiId eq mappedId.toJavaUuid() }
                .map { it[KrithiRagasTable.orderIndex] }
                .sorted()
        }
        assertTrue(
            junction.size > 1,
            "reingest should replace the placeholder with ordered krithi_ragas, got $junction",
        )
    }
}
