package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.support.IntegrationTestBase
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ImportServiceTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var service: IImportService
    private lateinit var normalizer: NameNormalizationService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        val dummyReviewer = object : ImportReviewer {
            override suspend fun reviewImport(id: kotlin.uuid.Uuid, request: ImportReviewRequest) =
                throw UnsupportedOperationException("Not used in tests")
        }
        val autoApproval = AutoApprovalService(dummyReviewer)
        val env = com.sangita.grantha.backend.api.config.ApiEnvironment(
            adminToken = "test",
            geminiApiKey = "test"
        )
        normalizer = NameNormalizationService()
        val entityResolver = EntityResolutionServiceImpl(dal, normalizer)
        service = ImportServiceImpl(dal, env, entityResolver, normalizer) { autoApproval }
    }

    @Test
    fun `submitImports stores imports`() = runTest {
        val created = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "TestSource",
                    sourceKey = "test-key",
                    rawTitle = "Test Krithi"
                )
            )
        )

        assertEquals(1, created.size)
        assertNotNull(created.first().id)

        val imports = service.getImports()
        assertEquals(1, imports.size)
    }

    @Test
    fun `reviewImport updates status`() = runTest {
        val created = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "TestSource",
                    sourceKey = "review-key",
                    rawTitle = "Review Krithi"
                )
            )
        )

        val importId = created.first().id
        val updated = service.reviewImport(importId, ImportReviewRequest(status = ImportStatusDto.REJECTED))

        assertEquals(ImportStatusDto.REJECTED, updated.importStatus)
    }

    @Test
    fun `reviewImport deduplicates existing krithi`() = runTest {
        // Setup: Create an existing Krithi
        val composer = dal.composers.create(name = "Tyagaraja")
        val raga = dal.ragas.create(name = "Kalyani")
        val existingKrithi = dal.krithis.create(
            KrithiCreateParams(
                title = "Nidhi Chala Sukhama",
                titleNormalized = normalizer.normalizeTitle("Nidhi Chala Sukhama")!!,
                composerId = composer.id.toJavaUuid(),
                primaryRagaId = raga.id.toJavaUuid(),
                musicalForm = MusicalForm.KRITHI,
                primaryLanguage = LanguageCode.TE,
                isRagamalika = false,
                workflowState = WorkflowState.PUBLISHED,
                ragaIds = listOf(raga.id.toJavaUuid())
            )
        )

        // Action: Submit import with same details
        val imports = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "test-source",
                    sourceKey = "http://example.com/nidhi",
                    rawTitle = "Nidhi Chala Sukhama",
                    rawComposer = "Tyagaraja",
                    rawRaga = "Kalyani"
                )
            )
        )
        val importId = imports.first().id

        // Action: Approve import
        val reviewed = service.reviewImport(importId, ImportReviewRequest(
            status = ImportStatusDto.APPROVED
        ))

        // Assert: mappedKrithiId should be existingKrithi.id
        assertNotNull(reviewed.mappedKrithiId)
        assertEquals(existingKrithi.id, reviewed.mappedKrithiId!!)

        // Assert: No new krithi created (count should be 1)
        val krithiCount: Long = dal.krithis.countAll()
        assertEquals(1L, krithiCount)

        // Assert: Source Evidence created even for deduplicated
        val evidence = dal.sourceEvidence.getKrithiEvidence(existingKrithi.id)
        assertNotNull(evidence)
        assertEquals(1, evidence.sources.size)
        assertEquals("http://example.com/nidhi", evidence.sources.first().sourceUrl)
    }

    @Test
    fun `reviewImport creates source evidence`() = runTest {
        val imports = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "test-source",
                    sourceKey = "http://example.com/endaro",
                    rawTitle = "Endaro Mahanubhavulu",
                    rawComposer = "Tyagaraja",
                    rawRaga = "Sri"
                )
            )
        )
        val importId = imports.first().id

        val reviewed = service.reviewImport(importId, ImportReviewRequest(
            status = ImportStatusDto.APPROVED
        ))

        assertNotNull(reviewed.mappedKrithiId)
        val createdKrithiId = reviewed.mappedKrithiId!!

        val evidence = dal.sourceEvidence.getKrithiEvidence(createdKrithiId)
        assertNotNull(evidence)
        assertEquals(1, evidence.sources.size)
        assertEquals("http://example.com/endaro", evidence.sources.first().sourceUrl)
        assertEquals("example.com", evidence.sources.first().sourceName)
    }

    @Test
    fun `submitImports enqueues HTML extraction task for url-only import`() = runTest {
        val sourceUrl = "http://guru-guha.blogspot.com/2007/07/dikshitar-kriti-abhayaambaa-jagadambaa.html"

        val created = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "WebScraper",
                    sourceKey = sourceUrl
                )
            )
        )

        assertEquals(1, created.size)
        assertEquals(sourceUrl, created.first().sourceKey)

        val (tasks, _) = dal.extractionQueue.list(format = listOf("HTML"), limit = 200)
        val matching = tasks.filter { it.sourceUrl == sourceUrl }
        assertEquals(1, matching.size)
        assertEquals("PENDING", matching.first().status)
    }

    @Test
    fun `submitImports url-only idempotency avoids duplicate HTML queue tasks`() = runTest {
        val sourceUrl = "http://guru-guha.blogspot.com/2007/07/dikshitar-kriti-akhilandesvari-raksha.html"
        val request = ImportKrithiRequest(
            source = "WebScraper",
            sourceKey = sourceUrl
        )

        val first = service.submitImports(listOf(request))
        val second = service.submitImports(listOf(request))

        assertEquals(1, first.size)
        assertEquals(1, second.size)
        assertEquals(first.first().id, second.first().id)

        val (tasks, _) = dal.extractionQueue.list(format = listOf("HTML"), limit = 200)
        val matching = tasks.filter { it.sourceUrl == sourceUrl }
        assertEquals(1, matching.size)
    }

    // TRACK-062: Idempotency Tests

    @Test
    fun `submitImports is idempotent on same sourceKey`() = runTest {
        val request = ImportKrithiRequest(
            source = "TestSource",
            sourceKey = "http://example.com/idempotent-test",
            rawTitle = "Idempotent Test Krithi"
        )

        val first = service.submitImports(listOf(request))
        val second = service.submitImports(listOf(request))

        // Same sourceKey should return the same import record, not create a duplicate
        assertEquals(1, first.size)
        assertEquals(1, second.size)
        assertEquals(first.first().id, second.first().id)

        // Verify only one import record exists
        val allImports = service.getImports()
        val matching = allImports.filter { it.sourceKey == "http://example.com/idempotent-test" }
        assertEquals(1, matching.size)
    }

    @Test
    fun `reviewImport twice does not create duplicate krithis`() = runTest {
        // Submit an import
        val imports = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "TestSource",
                    sourceKey = "http://example.com/double-approve",
                    rawTitle = "Double Approve Krithi",
                    rawComposer = "Tyagaraja",
                    rawRaga = "Mohanam"
                )
            )
        )
        val importId = imports.first().id

        // First approval: creates krithi
        val firstReview = service.reviewImport(importId, ImportReviewRequest(status = ImportStatusDto.APPROVED))
        assertNotNull(firstReview.mappedKrithiId)
        val krithiId = firstReview.mappedKrithiId!!

        // Submit a second import with same title/composer/raga but different sourceKey
        val imports2 = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "TestSource",
                    sourceKey = "http://example.com/double-approve-2",
                    rawTitle = "Double Approve Krithi",
                    rawComposer = "Tyagaraja",
                    rawRaga = "Mohanam"
                )
            )
        )
        val importId2 = imports2.first().id

        // Second approval: should match existing krithi, not create a new one
        val secondReview = service.reviewImport(importId2, ImportReviewRequest(status = ImportStatusDto.APPROVED))
        assertNotNull(secondReview.mappedKrithiId)
        assertEquals(krithiId, secondReview.mappedKrithiId)

        // Verify only one krithi exists
        val krithiCount: Long = dal.krithis.countAll()
        assertEquals(1L, krithiCount)
    }

    @Test
    fun `source evidence is idempotent on same krithi and sourceUrl`() = runTest {
        val imports = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "TestSource",
                    sourceKey = "http://example.com/evidence-idem",
                    rawTitle = "Evidence Idempotency Krithi",
                    rawComposer = "Tyagaraja",
                    rawRaga = "Bhairavi"
                )
            )
        )
        val importId = imports.first().id

        val reviewed = service.reviewImport(importId, ImportReviewRequest(status = ImportStatusDto.APPROVED))
        val krithiId = reviewed.mappedKrithiId!!

        val evidence1 = dal.sourceEvidence.getKrithiEvidence(krithiId)
        assertNotNull(evidence1)
        assertEquals(1, evidence1.sources.size)

        // Submit same sourceKey again — import-level dedup returns same record
        val imports2 = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "TestSource",
                    sourceKey = "http://example.com/evidence-idem",
                    rawTitle = "Evidence Idempotency Krithi",
                    rawComposer = "Tyagaraja",
                    rawRaga = "Bhairavi"
                )
            )
        )
        val importId2 = imports2.first().id
        assertEquals(importId, importId2) // confirms import-level dedup

        // Verify still only one evidence record
        val evidence2 = dal.sourceEvidence.getKrithiEvidence(krithiId)
        assertNotNull(evidence2)
        assertEquals(1, evidence2.sources.size)
    }

    @Test
    fun `bulk import task creation is idempotent via idempotency key`() = runTest {
        val batch = dal.bulkImport.createBatch(sourceManifest = "test-manifest.csv", createdByUserId = null)
        val job = dal.bulkImport.createJob(
            batchId = batch.id,
            jobType = com.sangita.grantha.backend.dal.enums.JobType.SCRAPE
        )

        // Create tasks with same URLs twice
        val tasks1 = dal.bulkImport.createTasks(
            jobId = job.id,
            batchId = batch.id,
            tasks = listOf(
                "key1" to "http://example.com/a",
                "key2" to "http://example.com/b"
            )
        )
        assertEquals(2, tasks1.size)

        // Create same tasks again — should be deduplicated
        val tasks2 = dal.bulkImport.createTasks(
            jobId = job.id,
            batchId = batch.id,
            tasks = listOf(
                "key1" to "http://example.com/a",
                "key2" to "http://example.com/b"
            )
        )
        assertEquals(0, tasks2.size) // all already exist

        // Verify total task count
        val allTasks = dal.bulkImport.listTasksByJob(job.id)
        assertEquals(2, allTasks.size)
    }
}
