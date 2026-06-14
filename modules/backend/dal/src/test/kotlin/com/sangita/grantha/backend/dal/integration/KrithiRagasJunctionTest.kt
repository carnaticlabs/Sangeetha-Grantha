package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithiRagasTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test

/**
 * D4: junction-table integrity for `krithi_ragas` — the exact regression class flagged in project
 * memory (seed/ingest populating FK columns but not the junction). A krithi created with N ragas
 * yields N rows; deleting the krithi cascade-deletes them (FK `ON DELETE CASCADE`, V02).
 */
class KrithiRagasJunctionTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `krithi with N ragas creates N junction rows and cascades on delete`() = runTest {
        val composer = dal.composers.create(name = "Tyagaraja")
        val ragaIds = listOf("Kalyani", "Todi", "Bhairavi")
            .map { dal.ragas.create(name = it).id.toJavaUuid() }

        val krithi = dal.krithis.create(
            KrithiCreateParams(
                title = "Ragamalika Probe",
                titleNormalized = "ragamalika probe",
                composerId = composer.id.toJavaUuid(),
                musicalForm = MusicalForm.KRITHI,
                primaryLanguage = LanguageCode.TE,
                isRagamalika = true,
                ragaIds = ragaIds,
                workflowState = WorkflowState.DRAFT,
            )
        )
        val krithiJavaId = krithi.id.toJavaUuid()

        val junctionCount = DatabaseFactory.dbQuery {
            KrithiRagasTable.selectAll()
                .where { KrithiRagasTable.krithiId eq krithiJavaId }
                .count()
        }
        assertEquals(3L, junctionCount, "expected one krithi_ragas row per raga")

        DatabaseFactory.dbQuery {
            KrithisTable.deleteWhere { KrithisTable.id eq krithiJavaId }
        }

        val afterDelete = DatabaseFactory.dbQuery {
            KrithiRagasTable.selectAll()
                .where { KrithiRagasTable.krithiId eq krithiJavaId }
                .count()
        }
        assertEquals(0L, afterDelete, "deleting the krithi must cascade-delete its krithi_ragas rows")
    }
}
