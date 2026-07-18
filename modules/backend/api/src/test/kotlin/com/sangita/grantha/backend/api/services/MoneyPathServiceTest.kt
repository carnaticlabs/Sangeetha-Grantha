package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportOverridesDto
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.models.KrithiSectionRequest
import com.sangita.grantha.backend.api.models.KrithiUpdateRequest
import com.sangita.grantha.backend.api.models.LyricVariantSectionRequest
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.api.testsupport.MoneyPathFixtures
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithiRagasTable
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalLyricSectionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalLyricVariantDto
import com.sangita.grantha.shared.domain.model.import.CanonicalSectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * TRACK-112: Money-path service integration tests.
 *
 * These tests exercise the business-critical flows with a real database (Testcontainers),
 * covering ingestion → review → canonical krithi creation with full assertion coverage
 * on sections, junction rows, audit trail, and revisions.
 */
class MoneyPathServiceTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var importService: IImportService
    private lateinit var normalizer: NameNormalizationService
    private lateinit var autoApproval: AutoApprovalService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        val dummyReviewer = object : ImportReviewer {
            override suspend fun reviewImport(
                id: kotlin.uuid.Uuid,
                request: ImportReviewRequest,
                reviewerUserId: kotlin.uuid.Uuid?
            ) = throw UnsupportedOperationException("Not used directly")
        }
        autoApproval = AutoApprovalService(dummyReviewer)
        val env = com.sangita.grantha.backend.api.config.ApiEnvironment(
            adminToken = "test",
            geminiApiKey = "test"
        )
        normalizer = NameNormalizationService()
        val entityResolver = EntityResolutionServiceImpl(dal, normalizer)
        importService = ImportServiceImpl(
            dal, env, entityResolver, normalizer,
            ImportReportGenerator(), LyricVariantPersistenceService(dal)
        ) { autoApproval }
    }

    /**
     * Create a real curator row. Revision attribution is a FK to `users`
     * (`krithi_revisions_created_by_user_id_fkey`), so a random UUID will not do.
     */
    private suspend fun aCurator(name: String = "Test Curator"): Uuid =
        dal.users.create(email = "curator-${Uuid.random()}@example.test", fullName = name).id

    // =========================================================================
    // S1: Ingestion → Review → Canon end-to-end
    // =========================================================================

    @Nested
    @DisplayName("S1: Ingestion → Review → Canon end-to-end")
    inner class IngestionToCanon {

        @Test
        fun `approved import creates canonical krithi with sections, raga junction, and audit trail`() = runTest {
            val imports = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "money-path-test",
                        sourceKey = "http://example.com/s1-krithi",
                        rawTitle = "Brochevarevare",
                        rawComposer = "Tyagaraja",
                        rawRaga = "Kamas",
                        rawTala = "Adi",
                        rawLanguage = "te"
                    )
                )
            )
            assertEquals(1, imports.size)
            val importId = imports.first().id

            val reviewed = importService.reviewImport(
                importId,
                ImportReviewRequest(status = ImportStatusDto.APPROVED),
                reviewerUserId = null
            )

            assertEquals(ImportStatusDto.APPROVED, reviewed.importStatus)
            assertNotNull(reviewed.mappedKrithiId, "approved import must map to a canonical krithi")
            val krithiId = reviewed.mappedKrithiId!!

            // Verify krithi exists in the canonical table
            val krithi = dal.krithis.findById(krithiId)
            assertNotNull(krithi, "canonical krithi must exist")
            assertEquals("Brochevarevare", krithi.title)

            // Verify raga junction row
            val ragaJunctionCount = DatabaseFactory.dbQuery {
                KrithiRagasTable.selectAll()
                    .where { KrithiRagasTable.krithiId eq krithiId.toJavaUuid() }
                    .count()
            }
            assertTrue(ragaJunctionCount > 0, "krithi must have at least one raga junction row")

            // Verify source evidence
            val evidence = dal.sourceEvidence.getKrithiEvidence(krithiId)
            assertNotNull(evidence, "source evidence must be created")
            assertTrue(evidence.sources.isNotEmpty(), "at least one source evidence record")
            assertEquals("http://example.com/s1-krithi", evidence.sources.first().sourceUrl)

            // Verify audit trail (Critical Rule #3)
            val auditLogs = dal.auditLogs.listByEntity("krithis", krithiId)
            assertTrue(auditLogs.isNotEmpty(), "audit log must record krithi creation")
            assertTrue(
                auditLogs.any { it.action == "CREATE_KRITHI_FROM_IMPORT" || it.action == "LINK_IMPORT_TO_EXISTING_KRITHI" },
                "audit log must contain a creation or link action, found: ${auditLogs.map { it.action }}"
            )
        }

        @Test
        fun `approved import with canonical payload creates sections and lyric variants`() = runTest {
            val extraction = TestFixtures.buildCanonicalExtraction(
                title = "Manasa Sancharare",
                composer = "Syama Sastri",
                ragaName = "Sama",
                tala = "Adi",
                lyricVariants = listOf(
                    CanonicalLyricVariantDto(
                        language = "te",
                        script = "latin",
                        sections = listOf(
                            CanonicalLyricSectionDto(sectionOrder = 1, text = "Manasa sancharare brhmani"),
                            CanonicalLyricSectionDto(sectionOrder = 2, text = "Manasija lavanya"),
                            CanonicalLyricSectionDto(sectionOrder = 3, text = "Charana text here"),
                        )
                    )
                ),
                sourceUrl = "http://example.com/s1-sections"
            )
            val payloadJson = Json.encodeToString(CanonicalExtractionDto.serializer(), extraction)

            val imports = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "money-path-test",
                        sourceKey = "http://example.com/s1-sections",
                        rawTitle = "Manasa Sancharare",
                        rawComposer = "Syama Sastri",
                        rawRaga = "Sama",
                        rawTala = "Adi",
                    )
                )
            )
            val importId = imports.first().id

            // Simulate extraction having populated the parsed_payload
            dal.imports.enrichWithExtraction(importId, parsedPayload = payloadJson)

            val reviewed = importService.reviewImport(
                importId,
                ImportReviewRequest(status = ImportStatusDto.APPROVED),
                reviewerUserId = null
            )

            val krithiId = reviewed.mappedKrithiId!!

            // Verify sections created
            val sections = dal.krithis.getSections(krithiId)
            assertTrue(sections.isNotEmpty(), "krithi must have sections")
            val sectionTypes = sections.map { it.sectionType }
            assertTrue("PALLAVI" in sectionTypes, "must have PALLAVI section")
            assertTrue("ANUPALLAVI" in sectionTypes, "must have ANUPALLAVI section")
            assertTrue("CHARANAM" in sectionTypes, "must have CHARANAM section")

            // Verify lyric variants created
            val variants = dal.krithiLyrics.getLyricVariants(krithiId)
            assertTrue(variants.isNotEmpty(), "must have lyric variants from canonical extraction")
        }

        @Test
        fun `import status transitions follow the expected lifecycle`() = runTest {
            val imports = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "lifecycle-test",
                        sourceKey = "http://example.com/lifecycle",
                        rawTitle = "Lifecycle Test Krithi",
                        rawComposer = "Tyagaraja",
                        rawRaga = "Bhairavi",
                    )
                )
            )
            val importId = imports.first().id

            // Starts as PENDING
            val pending = dal.imports.findById(importId)
            assertNotNull(pending)
            assertEquals(ImportStatusDto.PENDING, pending.importStatus)

            // Approve → APPROVED
            val approved = importService.reviewImport(
                importId,
                ImportReviewRequest(status = ImportStatusDto.APPROVED),
                reviewerUserId = null
            )
            assertEquals(ImportStatusDto.APPROVED, approved.importStatus)
        }
    }

    // =========================================================================
    // S2: Auto-approval boundary cases
    // =========================================================================

    @Nested
    @DisplayName("S2: Auto-approval boundaries — threshold, missing raga, conflicting composer")
    inner class AutoApprovalBoundaries {

        /** Build a resolution payload with the given confidences, using real seeded entities. */
        private suspend fun resolutionJson(
            composerConfidence: String?,
            ragaConfidence: String?,
            extraComposerConfidence: String? = null,
        ): String {
            val composer = dal.composers.findOrCreate(name = "Tyagaraja")
            val raga = dal.ragas.findOrCreate(name = "Kalyani")
            val tala = dal.talas.findOrCreate(name = "Adi")
            val composerCandidates = buildList {
                composerConfidence?.let { add(Candidate(composer, 100, it)) }
                extraComposerConfidence?.let {
                    add(Candidate(dal.composers.findOrCreate(name = "Syama Sastri"), 95, it))
                }
            }
            return Json.encodeToString(
                ResolutionResult.serializer(),
                ResolutionResult(
                    composerCandidates = composerCandidates,
                    ragaCandidates = ragaConfidence?.let { listOf(Candidate(raga, 100, it)) } ?: emptyList(),
                    talaCandidates = listOf(Candidate(tala, 100, "HIGH")),
                    resolved = true,
                )
            )
        }

        private fun importedWith(
            qualityScore: Double?,
            qualityTier: String?,
            resolutionData: String?,
            status: ImportStatusDto = ImportStatusDto.PENDING,
            duplicateCandidates: String? = null,
        ) = ImportedKrithiDto(
            id = Uuid.random(),
            importSourceId = Uuid.random(),
            sourceKey = "http://example.com/s2",
            rawTitle = "Boundary Case Krithi",
            rawLyrics = "Pallavi text present",
            rawComposer = "Tyagaraja",
            rawRaga = "Kalyani",
            importStatus = status,
            createdAt = Clock.System.now(),
            qualityScore = qualityScore,
            qualityTier = qualityTier,
            resolutionData = resolutionData,
            duplicateCandidates = duplicateCandidates,
        )

        @Test
        fun `score exactly at the threshold is approved, just below is not`() = runTest {
            val config = autoApproval.getConfig()
            val threshold = config.minQualityScore
            val resolution = resolutionJson(composerConfidence = "HIGH", ragaConfidence = "HIGH")

            val atThreshold = importedWith(threshold, "EXCELLENT", resolution)
            val justBelow = importedWith(threshold - 0.0001, "EXCELLENT", resolution)

            assertTrue(
                autoApproval.shouldAutoApprove(atThreshold),
                "score exactly at minQualityScore ($threshold) must auto-approve — the check is >=, not >"
            )
            assertFalse(
                autoApproval.shouldAutoApprove(justBelow),
                "score just below minQualityScore must not auto-approve"
            )
        }

        @Test
        fun `missing raga resolution blocks auto-approval`() = runTest {
            val withRaga = importedWith(0.99, "EXCELLENT", resolutionJson("HIGH", "HIGH"))
            val noRaga = importedWith(0.99, "EXCELLENT", resolutionJson("HIGH", null))

            assertTrue(autoApproval.shouldAutoApprove(withRaga), "control: fully resolved import auto-approves")
            assertFalse(
                autoApproval.shouldAutoApprove(noRaga),
                "an import with no high-confidence raga candidate must go to review"
            )
        }

        @Test
        fun `low-confidence raga blocks auto-approval even at a perfect score`() = runTest {
            val lowRaga = importedWith(1.0, "EXCELLENT", resolutionJson("HIGH", "MEDIUM"))
            assertFalse(
                autoApproval.shouldAutoApprove(lowRaga),
                "a MEDIUM-confidence raga must not satisfy requireRagaMatch"
            )
        }

        @Test
        fun `conflicting composer candidates block auto-approval unless one is high confidence`() = runTest {
            // Two candidates, neither HIGH — genuinely ambiguous, must go to review.
            val ambiguous = importedWith(
                0.99, "EXCELLENT",
                resolutionJson(composerConfidence = "MEDIUM", ragaConfidence = "HIGH", extraComposerConfidence = "MEDIUM")
            )
            assertFalse(
                autoApproval.shouldAutoApprove(ambiguous),
                "two MEDIUM composer candidates is a conflict — must never auto-merge"
            )
        }

        @Test
        fun `a high-confidence duplicate blocks auto-approval`() = runTest {
            val dupJson = Json.encodeToString(
                DeduplicationService.DeduplicationResult.serializer(),
                DeduplicationService.DeduplicationResult(
                    matches = listOf(
                        DeduplicationService.DuplicateMatch(
                            krithiId = Uuid.random().toString(),
                            reason = "exact title + composer match",
                            confidence = "HIGH",
                        )
                    )
                )
            )
            val withDup = importedWith(0.99, "EXCELLENT", resolutionJson("HIGH", "HIGH"), duplicateCandidates = dupJson)
            assertFalse(
                autoApproval.shouldAutoApprove(withDup),
                "a HIGH-confidence duplicate candidate must route to review, not auto-approve"
            )
        }

        @Test
        fun `a malformed duplicate-candidates payload fails OPEN and still auto-approves`() = runTest {
            // Documents current behaviour, which is a fail-open risk: AutoApprovalService catches the
            // deserialization error and treats an unparseable payload as "no duplicates found".
            // See TRACK-112 findings — if this test starts failing because the service was hardened
            // to fail closed, invert the assertion; do not re-loosen the service.
            val malformed = """{"matches":[{"krithiId":"x","title":"Dup","score":98,"confidence":"HIGH"}]}"""
            val withBadDup = importedWith(0.99, "EXCELLENT", resolutionJson("HIGH", "HIGH"), duplicateCandidates = malformed)
            assertTrue(
                autoApproval.shouldAutoApprove(withBadDup),
                "current behaviour: unparseable duplicateCandidates is treated as 'no duplicates' (fail-open)"
            )
        }

        @Test
        fun `missing lyrics blocks auto-approval under requireMinimalMetadata`() = runTest {
            val noLyrics = importedWith(0.99, "EXCELLENT", resolutionJson("HIGH", "HIGH")).copy(rawLyrics = null)
            assertFalse(
                autoApproval.shouldAutoApprove(noLyrics),
                "requireMinimalMetadata must reject an import with no lyrics"
            )
        }

        @Test
        fun `an already-reviewed import is never re-auto-approved`() = runTest {
            val alreadyApproved = importedWith(
                0.99, "EXCELLENT", resolutionJson("HIGH", "HIGH"),
                status = ImportStatusDto.APPROVED
            )
            assertFalse(
                autoApproval.shouldAutoApprove(alreadyApproved),
                "auto-approval must only ever act on PENDING imports"
            )
        }

        @Test
        fun `an ineligible quality tier blocks auto-approval regardless of score`() = runTest {
            val poorTier = importedWith(0.99, "POOR", resolutionJson("HIGH", "HIGH"))
            assertFalse(
                autoApproval.shouldAutoApprove(poorTier),
                "tier must be in ${autoApproval.getConfig().qualityTiers}"
            )
        }
    }

    // =========================================================================
    // S3: Entity resolution
    // =========================================================================

    @Nested
    @DisplayName("S3: Entity resolution — same composer in multiple transliterations")
    inner class EntityResolution {

        @Test
        fun `known aliases resolve to the same canonical composer`() = runTest {
            // "Thyagaraja" and "Saint Tyagaraja" are seeded aliases for "Tyagaraja"
            val variants = listOf("Tyagaraja", "Thyagaraja", "Saint Tyagaraja")
            val ids = mutableSetOf<kotlin.uuid.Uuid>()

            for (name in variants) {
                val composer = dal.composers.findOrCreate(name = name)
                ids.add(composer.id)
            }

            assertEquals(1, ids.size, "all known aliases must resolve to one composer entity")
        }
    }

    // =========================================================================
    // S4: Duplicate/variant handling
    // =========================================================================

    @Nested
    @DisplayName("S4: Duplicate detection — re-import maps to existing, no duplicates")
    inner class DuplicateDetection {

        @Test
        fun `re-import of same krithi maps to existing canonical record`() = runTest {
            // First import creates the canonical krithi
            val first = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "dedup-test",
                        sourceKey = "http://example.com/dedup-1",
                        rawTitle = "Aparadhamula Kshamimpa",
                        rawComposer = "Tyagaraja",
                        rawRaga = "Rasali",
                    )
                )
            )
            val firstReview = importService.reviewImport(
                first.first().id,
                ImportReviewRequest(status = ImportStatusDto.APPROVED),
                reviewerUserId = null
            )
            val originalKrithiId = firstReview.mappedKrithiId!!

            // Second import with same title/composer but different source
            val second = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "dedup-test",
                        sourceKey = "http://example.com/dedup-2",
                        rawTitle = "Aparadhamula Kshamimpa",
                        rawComposer = "Tyagaraja",
                        rawRaga = "Rasali",
                    )
                )
            )
            val secondReview = importService.reviewImport(
                second.first().id,
                ImportReviewRequest(status = ImportStatusDto.APPROVED),
                reviewerUserId = null
            )

            // Same krithi, no duplicate
            assertEquals(originalKrithiId, secondReview.mappedKrithiId,
                "re-import must map to the existing canonical krithi")
            assertEquals(1L, dal.krithiSearch.countAll(),
                "only one canonical krithi should exist")

            // Both sources recorded
            val evidence = dal.sourceEvidence.getKrithiEvidence(originalKrithiId)
            assertNotNull(evidence)
            assertEquals(2, evidence.sources.size,
                "both source URLs must be recorded as evidence")
        }
    }

    // =========================================================================
    // S5: Bulk import with malformed rows
    // =========================================================================

    @Nested
    @DisplayName("S5: Bulk import partial failure — good rows succeed, bad rows fail cleanly")
    inner class BulkPartialFailure {

        @Test
        fun `a 50-row batch with 5 malformed rows approves 45 and reports 5 row-level errors`() = runTest {
            val batch = MoneyPathFixtures.anImportBatch(count = 50, malformed = 5, keyPrefix = "s5")

            val imports = importService.submitImports(batch)
            assertEquals(50, imports.size, "every row is accepted for staging; validation happens at review")

            val reviewer = MoneyPathFixtures.aCurator(dal, "Batch Reviewer")
            var approved = 0
            val errors = mutableListOf<String>()
            for (imp in imports) {
                runCatching {
                    importService.reviewImport(
                        imp.id,
                        ImportReviewRequest(status = ImportStatusDto.APPROVED),
                        reviewerUserId = reviewer,
                    )
                }.onSuccess { if (it.mappedKrithiId != null) approved++ }
                    .onFailure { errors += it.message.orEmpty() }
            }

            assertEquals(45, approved, "the 45 well-formed rows must all reach canon")
            assertEquals(5, errors.size, "the 5 composer-less rows must fail individually, got: $errors")
            assertTrue(
                errors.all { it.contains("Composer is required") },
                "each failure must name its cause rather than a generic error: $errors"
            )

            // Row-level isolation: a bad row must not roll back or block its neighbours.
            assertEquals(
                45L, dal.krithiSearch.countAll(),
                "exactly the good rows are persisted — nothing partially written for the bad ones"
            )
        }

        @Test
        fun `a malformed row leaves its own import un-approved rather than silently passing`() = runTest {
            val batch = MoneyPathFixtures.anImportBatch(count = 2, malformed = 1, keyPrefix = "s5-small")
            val imports = importService.submitImports(batch)
            val reviewer = MoneyPathFixtures.aCurator(dal)

            val results = imports.map { imp ->
                imp.id to runCatching {
                    importService.reviewImport(
                        imp.id,
                        ImportReviewRequest(status = ImportStatusDto.APPROVED),
                        reviewerUserId = reviewer,
                    )
                }
            }

            val failed = results.single { it.second.isFailure }.first
            assertEquals(
                ImportStatusDto.PENDING,
                dal.imports.findById(failed)!!.importStatus,
                "a row that could not be promoted must stay PENDING for a curator to fix"
            )
        }
    }

    // =========================================================================
    // S6: Revertibility of a curator edit (via ADR-014 revision history)
    // =========================================================================

    @Nested
    @DisplayName("S6: A curator edit is revertible — prior state survives in revision history")
    inner class Revertibility {

        /** Approve an import carrying real sections, returning the new canonical krithi id. */
        private suspend fun importKrithiWithSections(key: String, charanamText: String): Uuid {
            val extraction = TestFixtures.buildCanonicalExtraction(
                title = "Revertible Krithi",
                composer = "Tyagaraja",
                ragaName = "Kalyani",
                tala = "Adi",
                lyricVariants = listOf(
                    CanonicalLyricVariantDto(
                        language = "te",
                        script = "latin",
                        sections = listOf(
                            CanonicalLyricSectionDto(sectionOrder = 1, text = "Original pallavi"),
                            CanonicalLyricSectionDto(sectionOrder = 2, text = "Original anupallavi"),
                            CanonicalLyricSectionDto(sectionOrder = 3, text = charanamText),
                        )
                    )
                ),
                sourceUrl = key
            )
            val imports = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "revert-test",
                        sourceKey = key,
                        rawTitle = "Revertible Krithi",
                        rawComposer = "Tyagaraja",
                        rawRaga = "Kalyani",
                        rawTala = "Adi",
                    )
                )
            )
            val importId = imports.first().id
            val payload = Json.encodeToString(CanonicalExtractionDto.serializer(), extraction)
            dal.imports.enrichWithExtraction(importId, parsedPayload = payload)

            // A COMPLETED extraction for this URL must exist: the revision writer derives a
            // source-document node from the payload, and `ksr_doc_requires_extraction_ck`
            // rejects a document without an extraction run.
            val task = dal.extractionQueue.create(sourceUrl = key, sourceFormat = "HTML", sourceName = "test-source")
            dal.extractionQueue.markDone(
                id = task.id,
                resultPayload = payload,
                resultCount = 1,
                extractionMethod = "HTML_JSOUP",
                extractorVersion = "test",
            )

            // Attribution is mandatory for a revision (ADR-014): an approval with neither a
            // resolvable extraction nor a reviewer is deliberately skipped by ImportService.
            return importService.reviewImport(
                importId,
                ImportReviewRequest(status = ImportStatusDto.APPROVED),
                reviewerUserId = aCurator()
            ).mappedKrithiId!!
        }

        @Test
        fun `a curator edit appends a revision snapshotting the section state, leaving history intact`() = runTest {
            val krithiService = KrithiServiceImpl(dal)
            val krithiId = importKrithiWithSections("http://example.com/revert-1", "Original charanam")

            val revisionsAfterImport = dal.revisions.listRevisions(krithiId.toJavaUuid())
            assertTrue(
                revisionsAfterImport.isNotEmpty(),
                "the import path must write a creation revision (ADR-014)"
            )
            assertTrue(
                revisionsAfterImport.first().sections.isNotEmpty(),
                "the creation revision must snapshot the krithi's sections"
            )

            // Curator edit → CURATOR_EDIT revision appended (requires attribution).
            krithiService.updateKrithi(
                krithiId,
                KrithiUpdateRequest(title = "Revertible Krithi (edited)"),
                userId = aCurator("Editing Curator")
            )

            assertEquals(
                "Revertible Krithi (edited)",
                dal.krithis.findById(krithiId)!!.title,
                "the edit must be live in the serving layer"
            )

            val revisionsAfterEdit = dal.revisions.listRevisions(krithiId.toJavaUuid())
            assertTrue(
                revisionsAfterEdit.size > revisionsAfterImport.size,
                "the edit must append a revision, not mutate the existing one " +
                    "(was ${revisionsAfterImport.size}, now ${revisionsAfterEdit.size})"
            )
            // Append-only: every pre-existing revision id survives untouched.
            assertTrue(
                revisionsAfterEdit.map { it.id }.containsAll(revisionsAfterImport.map { it.id }),
                "existing revisions must never be deleted or rewritten"
            )
            assertEquals(
                revisionsAfterEdit.map { it.revisionNo }.sorted(),
                revisionsAfterEdit.map { it.revisionNo },
                "revision numbers must be monotonically increasing"
            )
            assertTrue(
                revisionsAfterEdit.any { it.changeKind == "CURATOR_EDIT" },
                "the edit must be recorded as CURATOR_EDIT, got: ${revisionsAfterEdit.map { it.changeKind }}"
            )
        }

        /**
         * F2 (fixed) — ADR-014 now covers the section-edit path.
         *
         * `saveKrithiSections` mutates exactly the state revisions capture, but used to write no
         * revision: only `updateKrithi` (metadata) snapshotted, and `KrithiRevisionDto` carries no
         * metadata anyway. A destructive section edit therefore left no recovery point, and
         * "what did this krithi say on date X" was answerable only for import-time changes.
         */
        @Test
        fun `editing sections appends a revision that preserves the pre-edit section state`() = runTest {
            val krithiService = KrithiServiceImpl(dal)
            val krithiId = importKrithiWithSections("http://example.com/section-edit", "Original charanam")

            val before = dal.revisions.listRevisions(krithiId.toJavaUuid())
            val sectionsBefore = dal.krithis.getSections(krithiId).map { it.sectionType }
            assertTrue("CHARANAM" in sectionsBefore, "precondition: the krithi has a charanam")

            // Destructive edit — drop the charanam.
            krithiService.saveKrithiSections(
                krithiId,
                listOf(
                    KrithiSectionRequest(sectionType = "PALLAVI", orderIndex = 1),
                    KrithiSectionRequest(sectionType = "ANUPALLAVI", orderIndex = 2),
                ),
                userId = aCurator("Section Editor"),
            )

            assertFalse(
                "CHARANAM" in dal.krithis.getSections(krithiId).map { it.sectionType },
                "the edit must be live in the serving layer"
            )

            val after = dal.revisions.listRevisions(krithiId.toJavaUuid())
            assertTrue(
                after.size > before.size,
                "the section edit must append a revision (was ${before.size}, now ${after.size})"
            )
            assertTrue(
                after.any { it.changeKind == "CURATOR_EDIT" },
                "recorded as CURATOR_EDIT, got: ${after.map { it.changeKind }}"
            )

            // The recovery point: the dropped charanam is still readable from history.
            assertTrue(
                before.any { rev -> rev.sections.any { it.sectionType == "CHARANAM" } },
                "the pre-edit revision must still carry the charanam that was removed"
            )
        }

        @Test
        fun `editing lyric variant text appends a revision against the owning krithi`() = runTest {
            val krithiService = KrithiServiceImpl(dal)
            val krithiId = importKrithiWithSections("http://example.com/lyric-edit", "Original charanam")

            val variantWithSections = dal.krithiLyrics.getLyricVariants(krithiId).firstOrNull()
                ?: error("precondition: the imported krithi has a lyric variant")
            val before = dal.revisions.listRevisions(krithiId.toJavaUuid())

            val targetSection = variantWithSections.sections.first()
            krithiService.saveLyricVariantSections(
                variantWithSections.variant.id,
                listOf(
                    LyricVariantSectionRequest(
                        sectionId = targetSection.sectionId.toString(),
                        text = "Rewritten pallavi text",
                    )
                ),
                userId = aCurator("Lyric Editor"),
            )

            val after = dal.revisions.listRevisions(krithiId.toJavaUuid())
            assertTrue(
                after.size > before.size,
                "a lyric text edit must append a revision (was ${before.size}, now ${after.size})"
            )
        }

        @Test
        fun `an unattributed section edit still saves but records no revision`() = runTest {
            // ADR-014 requires attribution, so a call with no user cannot write a revision. It must
            // degrade — save the edit, skip the revision — rather than fail the curator's edit.
            val krithiService = KrithiServiceImpl(dal)
            val krithiId = importKrithiWithSections("http://example.com/section-edit-anon", "Original charanam")
            val before = dal.revisions.listRevisions(krithiId.toJavaUuid())

            krithiService.saveKrithiSections(
                krithiId,
                listOf(KrithiSectionRequest(sectionType = "PALLAVI", orderIndex = 1)),
                userId = null,
            )

            assertEquals(
                1, dal.krithis.getSections(krithiId).size,
                "the edit must still be applied"
            )
            assertEquals(
                before.size, dal.revisions.listRevisions(krithiId.toJavaUuid()).size,
                "no attribution means no revision — the edit is not rejected for it"
            )
        }
    }

    // =========================================================================
    // S6b: Rejection is safe — no partial writes
    // =========================================================================

    @Nested
    @DisplayName("S6b: Rejected import leaves no orphaned data")
    inner class RejectionSafety {

        @Test
        fun `rejected import does not create canonical krithi or junction rows`() = runTest {
            val imports = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "reject-test",
                        sourceKey = "http://example.com/reject",
                        rawTitle = "Rejected Krithi",
                        rawComposer = "Tyagaraja",
                        rawRaga = "Kalyani",
                    )
                )
            )
            val importId = imports.first().id

            val reviewed = importService.reviewImport(
                importId,
                ImportReviewRequest(status = ImportStatusDto.REJECTED),
                reviewerUserId = null
            )

            assertEquals(ImportStatusDto.REJECTED, reviewed.importStatus)
            assertEquals(null, reviewed.mappedKrithiId,
                "rejected import must not map to any krithi")
            assertEquals(0L, dal.krithiSearch.countAll(),
                "no canonical krithi should be created for rejected imports")
        }
    }

    // =========================================================================
    // S6c: Regression — payload without a completed extraction degrades gracefully
    // =========================================================================

    @Nested
    @DisplayName("S6c: Approving a payload-bearing import with no completed extraction")
    inner class PayloadWithoutExtraction {

        /**
         * Regression test for a defect found by TRACK-112 and fixed in `ImportService.reviewImport`.
         *
         * The revision writer used to derive `sourceDocumentId` from the canonical payload
         * independently of `extractionId`. When the payload parsed but no COMPLETED
         * `extraction_queue` row existed for the source URL, it wrote a document with no
         * extraction, violating `ksr_doc_requires_extraction_ck` (V44__versioned_canon.sql:68).
         * That aborted the approval *after* the krithi had been committed — leaving an orphaned
         * krithi behind and the import still PENDING, so a retry could create a second one.
         *
         * Reachable whenever an import carries a payload but its extraction row is missing, not
         * yet COMPLETED, or filed under a different URL (queue purge, re-scrape).
         *
         * The approval must now succeed, dropping only the unattributable document node.
         */
        @Test
        fun `approval succeeds and records a reviewer-attributed revision without a source document`() = runTest {
            val key = "http://example.com/payload-no-extraction"
            val extraction = TestFixtures.buildCanonicalExtraction(
                title = "Orphan Payload Krithi",
                composer = "Tyagaraja",
                ragaName = "Kalyani",
                tala = "Adi",
                sourceUrl = key,
            )
            val imports = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "orphan-payload-test",
                        sourceKey = key,
                        rawTitle = "Orphan Payload Krithi",
                        rawComposer = "Tyagaraja",
                        rawRaga = "Kalyani",
                        rawTala = "Adi",
                    )
                )
            )
            val importId = imports.first().id
            dal.imports.enrichWithExtraction(
                importId,
                parsedPayload = Json.encodeToString(CanonicalExtractionDto.serializer(), extraction)
            )
            // Note: deliberately NO extraction_queue row for `key`.

            val curator = aCurator()
            val reviewed = importService.reviewImport(
                importId,
                ImportReviewRequest(status = ImportStatusDto.APPROVED),
                reviewerUserId = curator
            )

            assertEquals(
                ImportStatusDto.APPROVED, reviewed.importStatus,
                "a payload with no completed extraction must not block approval"
            )
            val krithiId = assertNotNull(reviewed.mappedKrithiId, "approval must produce a canonical krithi")
            assertEquals(1L, dal.krithiSearch.countAll(), "exactly one krithi, no orphan")

            // Provenance degrades rather than failing: the revision is still written, attributed
            // to the curator, just without a source-document node.
            val revisions = dal.revisions.listRevisions(krithiId.toJavaUuid())
            assertTrue(revisions.isNotEmpty(), "the approval must still record a revision")
            assertTrue(
                revisions.any { it.createdByUserId == curator },
                "the revision must be attributed to the reviewing curator"
            )
        }
    }

    // =========================================================================
    // S6d: Atomicity — a failed promotion leaves nothing behind
    // =========================================================================

    @Nested
    @DisplayName("S6d: Approval is atomic — a mid-flight failure commits nothing")
    inner class ApprovalAtomicity {

        /**
         * TRACK-112: `reviewImport` promotes an import inside one transaction, so a failure at any
         * step rolls the whole krithi back. Before this, each `dal.*` call committed independently
         * and a late failure left an orphaned krithi with the import still PENDING — the curator saw
         * an error while the krithi silently existed, and a retry could create a second one.
         *
         * The trigger used here is a composer-less import, which throws partway through promotion
         * (after the import row is read, before the krithi is complete).
         */
        @Test
        fun `a promotion that fails after the krithi is written rolls the krithi back`() = runTest {
            val importId = MoneyPathFixtures.aPendingImport(
                importService,
                MoneyPathFixtures.anImportRequest(
                    sourceKey = "http://example.com/atomicity",
                    rawTitle = "Doomed Promotion",
                )
            )

            // Trigger: a reviewer id with no `users` row. Promotion gets all the way through krithi
            // creation, sections, junction rows and source evidence, then the ADR-014 revision write
            // fails on `krithi_revisions_created_by_user_id_fkey` — a genuinely LATE failure. A
            // trigger that throws early (e.g. a missing composer) would not exercise rollback at all,
            // because nothing has been written yet.
            val outcome = runCatching {
                importService.reviewImport(
                    importId,
                    ImportReviewRequest(status = ImportStatusDto.APPROVED),
                    reviewerUserId = Uuid.random(),
                )
            }

            assertTrue(outcome.isFailure, "an unresolvable reviewer must fail the promotion")
            assertEquals(
                0L, dal.krithiSearch.countAll(),
                "the krithi written before the failure must roll back — if this is 1, the promotion " +
                    "is no longer atomic and DatabaseFactory.dbQuery has stopped joining nested calls"
            )
            assertEquals(
                ImportStatusDto.PENDING,
                dal.imports.findById(importId)!!.importStatus,
                "the import must stay PENDING so a curator can retry it"
            )
        }

        @Test
        fun `retrying a failed promotion after the cause is fixed produces exactly one krithi`() = runTest {
            val request = MoneyPathFixtures.anImportRequest(
                sourceKey = "http://example.com/atomicity-retry",
                rawTitle = "Retried Promotion",
                rawComposer = null,
            )
            val importId = MoneyPathFixtures.aPendingImport(importService, request)
            val curator = MoneyPathFixtures.aCurator(dal)

            // First attempt fails — nothing persisted.
            runCatching {
                importService.reviewImport(
                    importId,
                    ImportReviewRequest(status = ImportStatusDto.APPROVED),
                    reviewerUserId = curator,
                )
            }
            assertEquals(0L, dal.krithiSearch.countAll(), "the failed attempt persisted nothing")

            // Curator supplies the missing composer and retries.
            val retried = importService.reviewImport(
                importId,
                ImportReviewRequest(
                    status = ImportStatusDto.APPROVED,
                    overrides = ImportOverridesDto(composer = "Tyagaraja"),
                ),
                reviewerUserId = curator,
            )

            assertNotNull(retried.mappedKrithiId, "the retry must succeed")
            assertEquals(
                1L, dal.krithiSearch.countAll(),
                "the retry must produce exactly one krithi — no duplicate from the failed attempt"
            )
        }
    }

    // =========================================================================
    // S7: Concurrency — two curators approve the same import simultaneously
    // =========================================================================

    @Nested
    @DisplayName("S7: Concurrent approval of one import yields exactly one canonical krithi")
    inner class ConcurrentApproval {

        @Test
        fun `two simultaneous approvals of the same import do not create two krithis`() = runTest {
            val imports = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "concurrency-test",
                        sourceKey = "http://example.com/concurrent",
                        rawTitle = "Concurrently Approved Krithi",
                        rawComposer = "Tyagaraja",
                        rawRaga = "Kalyani",
                        rawTala = "Adi",
                    )
                )
            )
            val importId = imports.first().id

            // Real parallelism — runTest's scheduler is single-threaded, so hop to Dispatchers.IO.
            val curatorA = aCurator("Curator A")
            val curatorB = aCurator("Curator B")

            val outcomes = withContext(Dispatchers.IO) {
                listOf(
                    async {
                        runCatching {
                            importService.reviewImport(
                                importId,
                                ImportReviewRequest(status = ImportStatusDto.APPROVED),
                                reviewerUserId = curatorA
                            )
                        }
                    },
                    async {
                        runCatching {
                            importService.reviewImport(
                                importId,
                                ImportReviewRequest(status = ImportStatusDto.APPROVED),
                                reviewerUserId = curatorB
                            )
                        }
                    },
                ).awaitAll()
            }

            val succeeded = outcomes.filter { it.isSuccess }
            assertTrue(
                succeeded.isNotEmpty(),
                "at least one approval must succeed; failures were: " +
                    outcomes.mapNotNull { it.exceptionOrNull() }.map { "${it::class.simpleName}: ${it.message}" }
            )

            // The invariant that matters: the corpus gained exactly one krithi, whatever
            // the two callers each observed.
            assertEquals(
                1L, dal.krithiSearch.countAll(),
                "concurrent approval must yield exactly one canonical krithi, not a duplicate pair"
            )

            // And both callers agree on which krithi it is (or one failed cleanly).
            val mappedIds = succeeded.mapNotNull { it.getOrNull()?.mappedKrithiId }.distinct()
            assertEquals(
                1, mappedIds.size,
                "successful approvals must all map to the same canonical krithi, got: $mappedIds"
            )
        }
    }

    // =========================================================================
    // S7b: Submission idempotency — same sourceKey does not create duplicates
    // =========================================================================

    @Nested
    @DisplayName("S7b: Import submission idempotency")
    inner class Idempotency {

        @Test
        fun `submitting the same sourceKey twice returns the existing record`() = runTest {
            val request = ImportKrithiRequest(
                source = "idem-test",
                sourceKey = "http://example.com/idempotent",
                rawTitle = "Idempotent Krithi",
                rawComposer = "Tyagaraja",
                rawRaga = "Shankarabharanam",
            )

            val first = importService.submitImports(listOf(request))
            val second = importService.submitImports(listOf(request))

            assertEquals(first.first().id, second.first().id,
                "same sourceKey must return the same import record")
        }
    }
}
