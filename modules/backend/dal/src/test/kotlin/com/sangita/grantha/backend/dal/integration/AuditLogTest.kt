package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * D6: the AUDIT_LOG write path produces a durable row (Critical Rule #3 — every mutation must be
 * audited). Exercises the repository's `append` helper and read-back.
 */
class AuditLogTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `append writes a retrievable audit row`() = runTest {
        val entityId = Uuid.random()
        dal.auditLogs.append(
            action = "CREATE",
            entityTable = "krithis",
            entityId = entityId,
            diff = """{"title":"Vatapi Ganapatim"}""",
        )

        val recent = dal.auditLogs.listRecent(limit = 20)
        assertTrue(
            recent.any { it.action == "CREATE" && it.entityTable == "krithis" },
            "expected the appended audit row to be retrievable",
        )
    }
}
