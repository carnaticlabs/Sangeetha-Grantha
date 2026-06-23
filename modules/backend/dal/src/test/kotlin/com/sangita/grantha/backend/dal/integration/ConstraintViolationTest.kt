package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.errors.DuplicateKeyException
import com.sangita.grantha.backend.dal.errors.ForeignKeyViolationException
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * D5: constraint violations surface as typed [com.sangita.grantha.backend.dal.errors.DalException]s
 * (mapped centrally in `DatabaseFactory.dbQuery`, TRACK-111) rather than a raw `PSQLException`
 * leaking up to services/routes.
 */
class ConstraintViolationTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `duplicate name_normalized surfaces DuplicateKeyException`() = runTest {
        // Probe name absent from the R__ reference seed, so the first insert is the original.
        dal.composers.create(name = "Dup-Probe Composer")
        assertFailsWith<DuplicateKeyException> {
            dal.composers.create(name = "Dup-Probe Composer")
        }
    }

    @Test
    fun `fk to a missing composer surfaces ForeignKeyViolationException`() = runTest {
        assertFailsWith<ForeignKeyViolationException> {
            dal.krithis.create(
                KrithiCreateParams(
                    title = "Orphan",
                    titleNormalized = "orphan",
                    composerId = UUID.randomUUID(), // no such composer row
                    musicalForm = MusicalForm.KRITHI,
                    primaryLanguage = LanguageCode.TE,
                    isRagamalika = false,
                    workflowState = WorkflowState.DRAFT,
                )
            )
        }
    }
}
