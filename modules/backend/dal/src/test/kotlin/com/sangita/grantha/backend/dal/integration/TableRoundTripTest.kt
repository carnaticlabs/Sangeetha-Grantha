package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.WorkflowStateDto
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * D2: Exposed `Table` definitions round-trip insert→select against the migrated schema. Catches the
 * drift class — a column type, nullability, or enum mapping that disagrees with the SQL migration.
 * Covers a representative core set including the enum-bearing `krithis` row.
 */
class TableRoundTripTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `composer round-trips with nullable int columns`() = runTest {
        // Probe entities use names absent from the R__ reference seed (which is preserved across tests).
        val created = dal.composers.create(name = "RoundTrip-Probe Composer", birthYear = 1767, deathYear = 1847)
        val fetched = dal.composers.findById(created.id)
        assertNotNull(fetched)
        assertEquals("RoundTrip-Probe Composer", fetched.name)
        assertEquals(1767, fetched.birthYear)
        assertEquals(1847, fetched.deathYear)
    }

    @Test
    fun `raga and tala round-trip`() = runTest {
        val raga = dal.ragas.create(name = "RoundTrip-Probe Raga", arohanam = "S R2 G3 M2 P D2 N3 S")
        val tala = dal.talas.create(name = "RoundTrip-Probe Tala", beatCount = 8, angaStructure = "I4 O O")
        assertEquals("S R2 G3 M2 P D2 N3 S", dal.ragas.findById(raga.id)?.arohanam)
        val fetchedTala = dal.talas.findById(tala.id)
        assertNotNull(fetchedTala)
        assertEquals(8, fetchedTala.beatCount)
        assertEquals("I4 O O", fetchedTala.angaStructure)
    }

    @Test
    fun `krithi round-trips with enum columns intact`() = runTest {
        val composer = dal.composers.create(name = "RoundTrip-Krithi Composer")
        val krithi = dal.krithis.create(
            KrithiCreateParams(
                title = "Vatapi Ganapatim",
                titleNormalized = "vatapi ganapatim",
                composerId = composer.id.toJavaUuid(),
                musicalForm = MusicalForm.KRITHI,
                primaryLanguage = LanguageCode.TE,
                isRagamalika = false,
                workflowState = WorkflowState.DRAFT,
            )
        )
        val fetched = dal.krithis.findById(krithi.id)
        assertNotNull(fetched)
        assertEquals("Vatapi Ganapatim", fetched.title)
        assertEquals(MusicalFormDto.KRITHI, fetched.musicalForm)
        assertEquals(LanguageCodeDto.TE, fetched.primaryLanguage)
        assertEquals(WorkflowStateDto.DRAFT, fetched.workflowState)
    }
}
