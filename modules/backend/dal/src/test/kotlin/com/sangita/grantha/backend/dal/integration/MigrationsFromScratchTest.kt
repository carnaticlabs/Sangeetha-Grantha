package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * D1 (Integration Tests Approach §4): the full `V__` migration set applies cleanly to a fresh
 * container. The cheapest high-value test in the plan — any migration that breaks ordering or
 * checksums fails here in CI rather than during `make db-reset`. Also exercises the TRACK-110
 * Flyway cutover end-to-end (the substrate migrates via the Flyway JVM API).
 */
class MigrationsFromScratchTest : IntegrationTestBase() {

    @Test
    fun `all versioned migrations applied with no failures`() = runTest {
        val (total, failed) = DatabaseFactory.dbQuery {
            val total = exec(
                "SELECT count(*) FROM flyway_schema_history WHERE version IS NOT NULL"
            ) { rs -> if (rs.next()) rs.getInt(1) else 0 } ?: 0
            val failed = exec(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false"
            ) { rs -> if (rs.next()) rs.getInt(1) else 0 } ?: 0
            total to failed
        }
        assertTrue(total >= 40, "expected the full V__ migration set; found $total")
        assertEquals(0, failed, "no migration may be recorded as failed")
    }

    @Test
    fun `core tables exist after migration`() = runTest {
        val present = DatabaseFactory.dbQuery {
            exec(
                """
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN
                    ('krithis','composers','ragas','talas','krithi_ragas','audit_log','users')
                """.trimIndent()
            ) { rs -> if (rs.next()) rs.getInt(1) else 0 } ?: 0
        }
        assertEquals(7, present, "all 7 core tables must exist post-migration")
    }
}
