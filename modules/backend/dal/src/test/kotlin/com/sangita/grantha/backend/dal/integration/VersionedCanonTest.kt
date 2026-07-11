package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.repositories.RevisionWrite
import com.sangita.grantha.backend.dal.repositories.SectionRevisionWrite
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * TRACK-117 / ADR-014: the versioned-canon contract against the real,
 * Flyway-migrated schema (V44) — append-only revisions, attribution floor,
 * source-document dedup, snapshot writes, and the N5 one-query provenance.
 */
class VersionedCanonTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    private suspend fun createKrithi(title: String): UUID {
        val seed = TestFixtures.seedReferenceData(dal)
        return dal.krithis.create(
            KrithiCreateParams(
                title = title,
                titleNormalized = title.lowercase(),
                composerId = seed.composer.id.toJavaUuid(),
                musicalForm = MusicalForm.KRITHI,
                primaryLanguage = LanguageCode.SA,
                primaryRagaId = seed.raga.id.toJavaUuid(),
                talaId = seed.tala.id.toJavaUuid(),
                isRagamalika = false,
                ragaIds = listOf(seed.raga.id.toJavaUuid()),
                workflowState = WorkflowState.DRAFT,
            ),
        ).id.toJavaUuid()
    }

    @Test
    fun `revisions append with monotonic numbers and latest wins`() = runTest {
        val krithiId = createKrithi("Revision Envelope Test")
        val user = dal.users.create(fullName = "Curator One")

        val first = dal.revisions.appendRevision(
            RevisionWrite(
                krithiId = krithiId,
                changeKind = "IMPORT",
                createdByUserId = user.id.toJavaUuid(),
                sections = listOf(
                    SectionRevisionWrite(
                        sectionType = "PALLAVI", orderIndex = 0,
                        language = LanguageCode.SA, script = ScriptCode.DEVANAGARI,
                        text = "original pallavi text",
                    ),
                ),
            ),
        )
        val second = dal.revisions.appendRevision(
            RevisionWrite(
                krithiId = krithiId,
                changeKind = "CURATOR_EDIT",
                changeReason = "typo fix",
                createdByUserId = user.id.toJavaUuid(),
                sections = listOf(
                    SectionRevisionWrite(
                        sectionType = "PALLAVI", orderIndex = 0,
                        language = LanguageCode.SA, script = ScriptCode.DEVANAGARI,
                        text = "corrected pallavi text",
                    ),
                ),
            ),
        )

        assertEquals(1, first.revisionNo)
        assertEquals(2, second.revisionNo)

        val latest = dal.revisions.latestRevision(krithiId)
        assertNotNull(latest)
        assertEquals(2, latest.revisionNo)
        assertEquals("corrected pallavi text", latest.sections.single().text)

        val history = dal.revisions.listRevisions(krithiId)
        assertEquals(listOf(1, 2), history.map { it.revisionNo })
        assertEquals("original pallavi text", history.first().sections.single().text)
    }

    @Test
    fun `revision without attribution is rejected`() = runTest {
        val krithiId = createKrithi("Attribution Floor Test")
        assertFailsWith<IllegalArgumentException> {
            dal.revisions.appendRevision(
                RevisionWrite(krithiId = krithiId, changeKind = "CURATOR_EDIT"),
            )
        }
    }

    @Test
    fun `source documents deduplicate on registry + url + checksum`() = runTest {
        val a = dal.revisions.ensureSourceDocumentForSource(
            sourceName = "guruguha.org",
            sourceUrl = "https://guruguha.org/krithi/dedup-test",
            sourceTier = 2,
            sourceFormat = "HTML",
            checksum = "sha256:dedup",
        )
        val b = dal.revisions.ensureSourceDocumentForSource(
            sourceName = "guruguha.org",
            sourceUrl = "https://guruguha.org/krithi/dedup-test",
            sourceTier = 2,
            sourceFormat = "HTML",
            checksum = "sha256:dedup",
        )
        assertEquals(a, b, "same registry+url+checksum must resolve to one document node")
    }

    @Test
    fun `provenance lineage resolves in one query`() = runTest {
        val krithiId = createKrithi("Provenance Lineage Test")
        val extraction = dal.extractionQueue.create(
            sourceUrl = "https://guruguha.org/krithi/lineage-test",
            sourceFormat = "HTML",
            sourceName = "guruguha.org",
        )
        val documentId = dal.revisions.ensureSourceDocumentForSource(
            sourceName = "guruguha.org",
            sourceUrl = "https://guruguha.org/krithi/lineage-test",
            sourceTier = 2,
            sourceFormat = "HTML",
            checksum = "sha256:lineage",
        )
        dal.revisions.appendRevision(
            RevisionWrite(
                krithiId = krithiId,
                changeKind = "IMPORT",
                extractionId = extraction.id.toJavaUuid(),
                sections = listOf(
                    SectionRevisionWrite(
                        sectionType = "PALLAVI", orderIndex = 0,
                        language = LanguageCode.SA, script = ScriptCode.LATIN,
                        text = "vAtApi gaNapatiM bhajE",
                        extractionId = extraction.id.toJavaUuid(),
                        sourceDocumentId = documentId,
                    ),
                ),
            ),
        )

        val lineage = dal.revisions.sectionProvenance(krithiId)
        assertEquals(1, lineage.size)
        val row = lineage.single()
        assertEquals("PALLAVI", row.sectionType)
        assertEquals("https://guruguha.org/krithi/lineage-test", row.sourceUrl)
        assertEquals("sha256:lineage", row.sourceChecksum)
        assertEquals("guruguha.org", row.sourceRegistryName)
        assertEquals("IMPORT", row.changeKind)
    }

    @Test
    fun `as-of reads return the revision current at that time`() = runTest {
        val krithiId = createKrithi("As-Of Test")
        val user = dal.users.create(fullName = "Curator AsOf")

        dal.revisions.appendRevision(
            RevisionWrite(
                krithiId = krithiId, changeKind = "IMPORT",
                createdByUserId = user.id.toJavaUuid(),
                sections = listOf(SectionRevisionWrite(sectionType = "PALLAVI", orderIndex = 0, text = "v1")),
            ),
        )
        val betweenRevisions = OffsetDateTime.now(ZoneOffset.UTC)
        Thread.sleep(25) // distinct valid_from for the second revision
        dal.revisions.appendRevision(
            RevisionWrite(
                krithiId = krithiId, changeKind = "CURATOR_EDIT",
                createdByUserId = user.id.toJavaUuid(),
                sections = listOf(SectionRevisionWrite(sectionType = "PALLAVI", orderIndex = 0, text = "v2")),
            ),
        )

        val asOfV1 = dal.revisions.sectionProvenance(krithiId, asOf = betweenRevisions)
        assertEquals("v1", asOfV1.single().text)
        assertEquals(1, asOfV1.single().revisionNo)

        val current = dal.revisions.sectionProvenance(krithiId)
        assertEquals("v2", current.single().text)

        val beforeEverything = dal.revisions.sectionProvenance(
            krithiId, asOf = betweenRevisions.minusDays(1),
        )
        assertTrue(beforeEverything.isEmpty(), "no revision existed a day earlier")
    }

    @Test
    fun `snapshotCurrentState captures serving-layer sections and lyrics`() = runTest {
        val krithiId = createKrithi("Snapshot Test")
        val user = dal.users.create(fullName = "Curator Snapshot")
        val krithiUuid = kotlin.uuid.Uuid.parse(krithiId.toString())

        dal.krithis.saveSections(
            krithiUuid,
            listOf(Triple("PALLAVI", 0, "Pallavi"), Triple("ANUPALLAVI", 1, "Anupallavi")),
        )
        val sections = dal.krithis.getSections(krithiUuid)
        val variant = dal.krithiLyrics.createLyricVariant(
            krithiId = krithiUuid,
            language = LanguageCode.SA,
            script = ScriptCode.LATIN,
            lyrics = "line one\n\nline two",
            isPrimary = true,
            sourceReference = null,
        )
        dal.krithiLyrics.saveLyricVariantSections(
            variant.id,
            sections.sortedBy { it.orderIndex }.map { it.id.toJavaUuid() to "text for ${it.sectionType}" },
        )

        val revision = dal.revisions.snapshotCurrentState(
            krithiId = krithiId,
            changeKind = "CURATOR_EDIT",
            changeReason = "snapshot after edit",
            createdByUserId = user.id.toJavaUuid(),
        )

        assertEquals(1, revision.revisionNo)
        assertEquals(2, revision.sections.size)
        val pallavi = revision.sections.first { it.sectionType == "PALLAVI" }
        assertEquals("text for PALLAVI", pallavi.text)
        assertEquals("SA", pallavi.language)
        assertEquals("LATIN", pallavi.script)
        assertNull(pallavi.extractionId, "manual snapshot carries no extraction attribution")
    }
}
