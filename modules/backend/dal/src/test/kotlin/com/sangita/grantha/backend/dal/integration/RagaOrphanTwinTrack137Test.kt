package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.repositories.RagaResolution
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * TRACK-137: orphan-twin merges resolve (or stay correctly ambiguous) against seed,
 * and — the data-loss-risk part — actually relocate krithi links (incl. repeated
 * ragamalika `order_index` slots) onto the keeper without loss when `track132_merge_raga`
 * runs. The seed carries no corpus, so link relocation is proven on synthetic fixtures.
 */
class RagaOrphanTwinTrack137Test : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    /** Run the merge SQL the V57 cleanup uses. */
    private suspend fun mergeRaga(loser: String, keeper: String) = DatabaseFactory.dbQuery {
        exec("SELECT track132_merge_raga('${loser.replace("'", "''")}', '${keeper.replace("'", "''")}')")
    }

    /** order_index values a raga occupies within one krithi (multiset → set is fine here). */
    private suspend fun ragaSlots(krithiId: java.util.UUID, ragaId: java.util.UUID): Set<Int> =
        DatabaseFactory.dbQuery {
            val out = mutableSetOf<Int>()
            exec(
                "SELECT order_index FROM krithi_ragas WHERE krithi_id = '$krithiId' AND raga_id = '$ragaId'",
            ) { rs -> while (rs.next()) out.add(rs.getInt(1)) }
            out
        }

    private suspend fun junctionCount(krithiId: java.util.UUID): Int = DatabaseFactory.dbQuery {
        var n = 0
        exec("SELECT count(*) FROM krithi_ragas WHERE krithi_id = '$krithiId'") { rs ->
            if (rs.next()) n = rs.getInt(1)
        }
        n
    }

    private suspend fun primaryRagaId(krithiId: java.util.UUID): java.util.UUID? = DatabaseFactory.dbQuery {
        var id: java.util.UUID? = null
        exec("SELECT primary_raga_id FROM krithis WHERE id = '$krithiId'") { rs ->
            if (rs.next()) id = rs.getObject(1) as java.util.UUID?
        }
        id
    }

    @Test
    fun `orphan twin nIlAmbari resolves to Neelambari`() = runTest {
        val raga = assertIs<RagaResolution.Resolved>(dal.ragas.resolveRaga("nIlAmbari")).raga
        assertEquals("Neelāmbari", raga.name)
    }

    @Test
    fun `orphan twin bhauLi resolves to Bowli`() = runTest {
        val raga = assertIs<RagaResolution.Resolved>(dal.ragas.resolveRaga("bhauLi")).raga
        assertEquals("Bowli", raga.name)
    }

    @Test
    fun `jIvantikA stays ambiguous against Jeevantika and Jeevanthika`() = runTest {
        val unresolved = assertIs<RagaResolution.Unresolved>(dal.ragas.resolveRaga("jIvantikA"))
        assertEquals("ambiguous", unresolved.kind)
        assertNotNull(dal.ragas.findByName("Jeevantikā"))
    }

    @Test
    fun `Shreemati is ambiguous not a unique hit on Srimati`() = runTest {
        val resolution = dal.ragas.resolveRaga("Shreemati")
        val unresolved = assertIs<RagaResolution.Unresolved>(resolution)
        assertEquals("ambiguous", unresolved.kind)
        assertNull(dal.ragas.findByName("Shreemati"), "Shreemati must not remain a ragas row")
        assertNotNull(dal.ragas.findByName("Srimati"))
        val hitNames = dal.ragas.lookupIdentityHits("Shreemati").map { it.raga.name }.toSet()
        assertEquals(setOf("Shreemani", "Srimati"), hitNames)
    }

    @Test
    fun `merge relocates all krithi_ragas slots and repoints primary, losing none`() = runTest {
        val keeper = dal.ragas.create(name = "Track137-Reloc-Keeper")
        val orphan = dal.ragas.create(name = "Track137-Reloc-Orphan")
        val other = dal.ragas.create(name = "Track137-Reloc-Other")
        val composer = dal.composers.create(name = "Track137-Reloc-Composer")

        // Ragamalika-shaped krithi: the orphan appears at THREE order_index slots (the
        // malika-slot hazard B1/S2 flagged), the keeper co-occurs at another slot, plus
        // an untouched control raga. primary points at the orphan.
        val krithi = dal.krithis.create(
            KrithiCreateParams(
                title = "Track137 Relocation Probe",
                titleNormalized = "track137 relocation probe",
                composerId = composer.id.toJavaUuid(),
                musicalForm = MusicalForm.KRITHI,
                primaryLanguage = LanguageCode.TE,
                isRagamalika = true,
                ragaIds = listOf(orphan.id.toJavaUuid(), other.id.toJavaUuid()),
                workflowState = WorkflowState.DRAFT,
            ),
        )
        val kid = krithi.id.toJavaUuid()
        val orphanId = orphan.id.toJavaUuid()
        val keeperId = keeper.id.toJavaUuid()

        // Add two more orphan slots and one co-occurring keeper slot at high indices,
        // and make the orphan the primary raga.
        DatabaseFactory.dbQuery {
            exec("INSERT INTO krithi_ragas (krithi_id, raga_id, order_index) VALUES ('$kid','$orphanId',90),('$kid','$orphanId',91)")
            exec("INSERT INTO krithi_ragas (krithi_id, raga_id, order_index) VALUES ('$kid','$keeperId',92)")
            exec("UPDATE krithis SET primary_raga_id = '$orphanId' WHERE id = '$kid'")
        }

        val beforeCount = junctionCount(kid)
        val orphanSlotsBefore = ragaSlots(kid, orphanId)
        val keeperSlotsBefore = ragaSlots(kid, keeperId)
        assertEquals(3, orphanSlotsBefore.size, "orphan should hold 3 slots pre-merge")

        mergeRaga("Track137-Reloc-Orphan", "Track137-Reloc-Keeper")

        // Orphan row is gone; every slot it held now belongs to the keeper (union with
        // the keeper's own), nothing lost, nothing duplicated, primary repointed.
        assertNull(dal.ragas.findByName("Track137-Reloc-Orphan"), "orphan row must be deleted")
        assertEquals(
            keeperSlotsBefore + orphanSlotsBefore,
            ragaSlots(kid, keeperId),
            "keeper must hold the union of its own and the orphan's order_index slots",
        )
        assertEquals(0, ragaSlots(kid, orphanId).size, "no slot may remain on the deleted orphan id")
        assertEquals(beforeCount, junctionCount(kid), "total krithi_ragas count must be unchanged (no link lost)")
        assertEquals(keeperId, primaryRagaId(kid), "primary_raga_id must repoint to the keeper (SET-NULL trap)")
    }

    @Test
    fun `merge with same order_index on both rows is rejected, not a silent overwrite`() = runTest {
        val keeper = dal.ragas.create(name = "Track137-Collide-Keeper")
        val orphan = dal.ragas.create(name = "Track137-Collide-Orphan")
        val composer = dal.composers.create(name = "Track137-Collide-Composer")
        val krithi = dal.krithis.create(
            KrithiCreateParams(
                title = "Track137 Collision Probe",
                titleNormalized = "track137 collision probe",
                composerId = composer.id.toJavaUuid(),
                musicalForm = MusicalForm.KRITHI,
                primaryLanguage = LanguageCode.TE,
                isRagamalika = true,
                ragaIds = listOf(orphan.id.toJavaUuid()),
                workflowState = WorkflowState.DRAFT,
            ),
        )
        val kid = krithi.id.toJavaUuid()
        val orphanSlot = ragaSlots(kid, orphan.id.toJavaUuid()).single()
        // Keeper occupies the SAME order_index as the orphan → merging would collide on the PK.
        DatabaseFactory.dbQuery {
            exec("INSERT INTO krithi_ragas (krithi_id, raga_id, order_index) VALUES ('$kid','${keeper.id.toJavaUuid()}',$orphanSlot)")
        }
        val threw = try {
            mergeRaga("Track137-Collide-Orphan", "Track137-Collide-Keeper")
            false
        } catch (_: Exception) {
            true
        }
        assertEquals(true, threw, "a same-order_index collision must RAISE, never silently drop a malika slot")
        assertNotNull(dal.ragas.findByName("Track137-Collide-Orphan"), "orphan must survive a failed merge")
    }
}
