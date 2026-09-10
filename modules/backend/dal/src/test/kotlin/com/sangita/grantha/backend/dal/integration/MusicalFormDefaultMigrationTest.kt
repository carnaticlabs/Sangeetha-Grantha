package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class MusicalFormDefaultMigrationTest : IntegrationTestBase() {
    @Test
    fun `column default is unestablished and existing krithi values are not rewritten`() = runTest {
        val defaultValue = DatabaseFactory.dbQuery {
            exec(
                "SELECT column_default FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'krithis' AND column_name = 'musical_form'",
            ) { rs -> if (rs.next()) rs.getString(1) else "" }
        }
        assertTrue(defaultValue.orEmpty().contains("UNESTABLISHED"))

        val enumValues = DatabaseFactory.dbQuery {
            exec("SELECT enumlabel FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'musical_form_enum' ORDER BY enumlabel") { rs ->
                val labels = mutableListOf<String>()
                while (rs.next()) labels += rs.getString(1)
                labels
            }
        }.orEmpty()
        assertEquals(listOf("KRITHI", "SWARAJATHI", "UNESTABLISHED", "VARNAM").sorted(), enumValues.sorted())
    }
}
