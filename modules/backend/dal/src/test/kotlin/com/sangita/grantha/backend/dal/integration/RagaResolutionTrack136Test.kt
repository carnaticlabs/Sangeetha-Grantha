package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.repositories.RagaResolution
import com.sangita.grantha.backend.dal.repositories.RagaResolveContext
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithiRagasTable
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test

/**
 * TRACK-136 Phase 2–3: no silent mint, queue semantics, lakshana standing checks.
 */
class RagaResolutionTrack136Test : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `unknown name enqueues and does not mint a ragas row`() = runTest {
        val before = dal.ragas.countAll()
        val resolution = dal.ragas.resolveRaga("Track136-Completely-Unknown-Raga")
        assertIs<RagaResolution.Unresolved>(resolution)
        assertEquals("unknown", resolution.kind)
        assertEquals(before, dal.ragas.countAll())
        assertTrue(dal.ragas.countPendingQueue() >= 1L)
    }

    @Test
    fun `Kalyani common alias resolves to Mechakalyāni`() = runTest {
        val resolution = dal.ragas.resolveRaga("Kalyani")
        val raga = assertIs<RagaResolution.Resolved>(resolution).raga
        assertEquals("Mechakalyāni", raga.name)
        assertEquals(65, raga.melakartaNumber)
    }

    @Test
    fun `Kalāvati without mela is ambiguous not a silent pick`() = runTest {
        val resolution = dal.ragas.resolveRaga("Kalāvati")
        val unresolved = assertIs<RagaResolution.Unresolved>(resolution)
        assertEquals("ambiguous", unresolved.kind)
        val hits = dal.ragas.lookupIdentityHits("Kalāvati")
        assertTrue(hits.size >= 2, "expected both Kalāvati homonyms, got ${hits.map { it.raga.name }}")
    }

    @Test
    fun `attach-alias then re-resolve succeeds`() = runTest {
        val unknown = dal.ragas.resolveRaga("Track136-Alias-Probe")
        val queueId = assertIs<RagaResolution.Unresolved>(unknown).queueId
        val target = dal.ragas.findByName("Abheri") ?: error("seeded Abheri missing")
        dal.ragas.insertAlias(
            ragaId = target.id,
            alias = "Track136-Alias-Probe",
            aliasType = "transliteration",
            source = "TRACK-136 test",
        )
        dal.ragas.resolveQueueItem(queueId, target.id, status = "attached")
        val again = dal.ragas.resolveRaga("Track136-Alias-Probe")
        assertEquals(target.id, assertIs<RagaResolution.Resolved>(again).raga.id)
    }

    @Test
    fun `held krithi_ragas slot is written at its own order_index`() = runTest {
        val unknown = dal.ragas.resolveRaga("Track136-Held-Slot")
        val queueId = assertIs<RagaResolution.Unresolved>(unknown).queueId
        val composer = dal.composers.create(name = "Track136-Held-Composer")
        val krithi = dal.krithis.create(
            KrithiCreateParams(
                title = "Track136 Held Slot",
                titleNormalized = "track136 held slot",
                composerId = composer.id.toJavaUuid(),
                musicalForm = MusicalForm.KRITHI,
                primaryLanguage = LanguageCode.TE,
                isRagamalika = true,
                workflowState = WorkflowState.DRAFT,
            ),
        )
        dal.ragas.appendQueueContext(
            queueId,
            RagaResolveContext(
                krithiId = krithi.id.toString(),
                orderIndex = 2,
                isPrimary = false,
            ),
        )
        val abheri = dal.ragas.findByName("Abheri") ?: error("Abheri")
        dal.ragas.resolveQueueItem(queueId, abheri.id, status = "attached")
        val orders = DatabaseFactory.dbQuery {
            KrithiRagasTable.selectAll()
                .where { KrithiRagasTable.krithiId eq krithi.id.toJavaUuid() }
                .map { it[KrithiRagasTable.orderIndex] }
        }
        assertEquals(listOf(2), orders)
    }

    @Test
    fun `standing lakshana checks flag zero known-good rows`() = runTest {
        assertEquals(0L, dal.ragas.countJanyaNotSubsetOfParent(), "janya⊄parent false positive")
        assertEquals(0L, dal.ragas.countMelaAsOwnJanya(), "mela-as-own-janya false positive")
    }
}
