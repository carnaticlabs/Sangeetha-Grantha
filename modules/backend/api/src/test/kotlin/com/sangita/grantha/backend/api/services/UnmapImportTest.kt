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
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * TRACK-149: split a krithi that absorbed a distinct composition.
 *
 * Two imports with the same title and different text are promoted. The second one is linked to the
 * first one's krithi by the TRACK-062 title dedup (the original collision). `unmapImport` detaches it
 * and `reviewImport(forceNew = true)` gives it a krithi of its own, without touching the first
 * witness's lyrics.
 */
class UnmapImportTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var importService: IImportService

    private val urlA = "http://example.com/split-a"
    private val urlB = "http://example.com/split-b"

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        val dummyReviewer = object : ImportReviewer {
            override suspend fun reviewImport(
                id: Uuid,
                request: ImportReviewRequest,
                reviewerUserId: Uuid?,
            ) = throw UnsupportedOperationException("Not used in tests")
        }
        val autoApproval = AutoApprovalService(dummyReviewer)
        val env = ApiEnvironment(adminToken = "test", geminiApiKey = "test")
        val normalizer = NameNormalizationService()
        val entityResolver = EntityResolutionServiceImpl(dal, normalizer)
        importService = ImportServiceImpl(
            dal, env, entityResolver, normalizer,
            ImportReportGenerator(), LyricVariantPersistenceService(dal),
        ) { autoApproval }
    }

    private fun extraction(url: String, pallavi: String, charanam: String) = CanonicalExtractionDto(
        title = "Rama Split Test",
        composer = "Tyagaraja",
        ragas = listOf(CanonicalRagaDto(name = "Kalyani")),
        tala = "Adi",
        sections = listOf(
            CanonicalSectionDto(type = CanonicalSectionType.PALLAVI, order = 1),
            CanonicalSectionDto(type = CanonicalSectionType.CHARANAM, order = 2),
        ),
        lyricVariants = listOf(
            CanonicalLyricVariantDto(
                language = "te", script = "latin",
                sections = listOf(
                    CanonicalLyricSectionDto(sectionOrder = 1, text = pallavi),
                    CanonicalLyricSectionDto(sectionOrder = 2, text = charanam),
                ),
            ),
        ),
        sourceUrl = url,
        sourceName = "example.com",
        sourceTier = 3,
        extractionMethod = CanonicalExtractionMethod.HTML_JSOUP,
    )

    private suspend fun submit(url: String, payload: CanonicalExtractionDto): Uuid {
        val id = importService.submitImports(
            listOf(ImportKrithiRequest(source = "WebScraper", sourceKey = url)),
        ).first().id
        val escaped = Json.encodeToString(payload).replace("'", "''")
        DatabaseFactory.dbQuery {
            exec(
                "UPDATE imported_krithis SET parsed_payload = '$escaped'::jsonb, import_status = 'in_review', " +
                    "raw_title = 'Rama Split Test', raw_composer = 'Tyagaraja', raw_raga = 'Kalyani', raw_tala = 'Adi' " +
                    "WHERE id = '$id'",
            )
        }
        return id
    }

    private suspend fun approve(id: Uuid, forceNew: Boolean = false) =
        importService.reviewImport(
            id,
            ImportReviewRequest(status = ImportStatusDto.APPROVED, forceNew = forceNew),
            reviewerUserId = null,
        )

    private suspend fun collide(): Triple<Uuid, Uuid, Uuid> {
        val a = submit(urlA, extraction(urlA, "rama rama sita pallavi one", "charanam one"))
        approve(a)
        val krithiId = assertNotNull(dal.imports.findById(a)?.mappedKrithiId)
        val b = submit(urlB, extraction(urlB, "rama rama sita pallavi two", "charanam two"))
        approve(b)
        return Triple(a, b, krithiId)
    }

    @Test
    fun `title dedup collapses the second import onto the first krithi`() = runTest {
        val (_, b, krithiId) = collide()
        assertEquals(krithiId, dal.imports.findById(b)?.mappedKrithiId, "this is the collision being repaired")
    }

    @Test
    fun `unmap then forceNew gives the import a krithi of its own and leaves the first witness alone`() = runTest {
        val (a, b, krithiId) = collide()
        val variantsBefore = dal.krithiLyrics.getLyricVariants(krithiId)
        val evidenceBefore = dal.sourceEvidence.countByKrithiIds(listOf(krithiId))[krithiId] ?: 0

        val unmapped = importService.unmapImport(b, "distinct composition, see TRACK-149 sheet", actorUserId = null)
        assertEquals(ImportStatusDto.PENDING, unmapped.importStatus)
        assertNull(unmapped.mappedKrithiId)
        assertEquals(krithiId, dal.imports.findById(a)?.mappedKrithiId, "the retained witness stays mapped")
        assertEquals(evidenceBefore - 1, dal.sourceEvidence.countByKrithiIds(listOf(krithiId))[krithiId] ?: 0)
        assertEquals(variantsBefore.size, dal.krithiLyrics.getLyricVariants(krithiId).size, "no lyric data removed")

        approve(b, forceNew = true)
        val newKrithiId = assertNotNull(dal.imports.findById(b)?.mappedKrithiId)
        assertNotEquals(krithiId, newKrithiId, "forceNew bypasses the title dedup")

        // Junction/child rows exist for the new krithi, not just the FK columns.
        assertTrue(dal.krithiLyrics.getLyricVariants(newKrithiId).isNotEmpty(), "new krithi has lyric variants")
        assertTrue(dal.krithis.getSections(newKrithiId).isNotEmpty(), "new krithi has sections")
        assertEquals(1, dal.sourceEvidence.countByKrithiIds(listOf(newKrithiId))[newKrithiId] ?: 0)

        val audit = dal.auditLogs.listByEntity("imported_krithis", b).map { it.action }
        assertTrue("UNMAP_IMPORT" in audit, "unmap is audited on the import")
        assertTrue(
            dal.auditLogs.listByEntity("krithis", krithiId).any { it.action == "IMPORT_UNMAPPED_FROM_KRITHI" },
            "unmap is audited on the krithi",
        )
    }

    @Test
    fun `without forceNew the dedup re-links an unmapped import to the same krithi`() = runTest {
        val (_, b, krithiId) = collide()
        importService.unmapImport(b, "pin: dedup would undo the split", actorUserId = null)
        approve(b)
        assertEquals(krithiId, dal.imports.findById(b)?.mappedKrithiId)
    }

    @Test
    fun `refuses to unmap the witness that supplied the krithi lyrics`() = runTest {
        val (a, _, krithiId) = collide()
        assertFailsWith<IllegalArgumentException> { importService.unmapImport(a, "wrong witness", null) }
        assertEquals(krithiId, dal.imports.findById(a)?.mappedKrithiId)
    }

    @Test
    fun `refuses to unmap the only witness of a krithi`() = runTest {
        val only = submit(urlA, extraction(urlA, "solo pallavi", "solo charanam"))
        approve(only)
        assertFailsWith<IllegalArgumentException> { importService.unmapImport(only, "orphan", null) }
    }

    @Test
    fun `requires a reason and a mapped import`() = runTest {
        val (_, b, _) = collide()
        assertFailsWith<IllegalArgumentException> { importService.unmapImport(b, "  ", null) }
        importService.unmapImport(b, "first unmap", null)
        assertFailsWith<IllegalArgumentException> { importService.unmapImport(b, "already unmapped", null) }
    }
}
