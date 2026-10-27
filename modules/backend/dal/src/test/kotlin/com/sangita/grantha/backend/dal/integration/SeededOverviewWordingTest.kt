package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Seeded descriptions become the overview text Rasika shows (cycle and kshetra cards and detail
 * screens, TRACK-152), so they must read as prose to a listener, not as notes to developers.
 */
class SeededOverviewWordingTest : IntegrationTestBase() {
    private val developerWording = Regex(
        """\b(worker|seeded|axis_value|TRACK-\d+|KSHETRA_OVERVIEW|CYCLE_OVERVIEW|search_documents)\b|\bnull\b""",
        RegexOption.IGNORE_CASE,
    )

    @Test
    fun `seeded cycle and temple descriptions carry no developer wording`() = runTest {
        val offenders = mutableListOf<String>()
        DatabaseFactory.dbQuery {
            listOf(
                "SELECT 'tags.description_en' AS src, slug AS ref, description_en AS txt FROM tags WHERE description_en IS NOT NULL",
                "SELECT 'temples.notes' AS src, name AS ref, notes AS txt FROM temples WHERE notes IS NOT NULL",
            ).forEach { sql ->
                exec(sql, explicitStatementType = StatementType.SELECT) { rs ->
                    while (rs.next()) {
                        val text = rs.getString("txt")
                        if (developerWording.containsMatchIn(text)) {
                            offenders += "${rs.getString("src")} ${rs.getString("ref")}: $text"
                        }
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "developer wording in seeded overview text:\n" + offenders.joinToString("\n"))
    }
}
