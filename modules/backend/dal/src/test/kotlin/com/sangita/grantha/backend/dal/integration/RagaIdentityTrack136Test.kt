package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.RagaIdentityKeysTable
import com.sangita.grantha.backend.dal.tables.RagaRelationsTable
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test

/**
 * TRACK-136 Phase 1: mela-qualified identity key, alias table, union guardrail.
 */
class RagaIdentityTrack136Test : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    private suspend fun matchKey(name: String): String = DatabaseFactory.dbQuery {
        exec("SELECT raga_match_key('${name.replace("'", "''")}')") { rs ->
            if (rs.next()) rs.getString(1) else error("raga_match_key returned no row")
        } ?: error("raga_match_key returned null")
    }

    @Test
    fun `frozen suite — 2 homonym pairs share a name-key, 4 defensive cases stay apart`() = runTest {
        assertEquals(matchKey("Kalāvathi"), matchKey("Kalāvati"))
        assertEquals(matchKey("Shreemati"), matchKey("Srimati"))

        assertNotEquals(matchKey("Kanadā"), matchKey("Kannada"))
        assertNotEquals(matchKey("Bhairavi"), matchKey("Bhairava"))
        assertNotEquals(matchKey("Bhairavi"), matchKey("Bhairavam"))
        assertNotEquals(matchKey("Abhogi"), matchKey("Bhogi"))

        val ranjani = setOf(matchKey("Ranjani"), matchKey("Niranjani"), matchKey("Shreeranjani"))
        assertEquals(3, ranjani.size, "digraphs must be mapped not deleted, got $ranjani")
    }

    @Test
    fun `homonyms remain distinct identities via mela disambiguator`() = runTest {
        data class Identity(val name: String, val matchKey: String, val mela: Int)

        val rows = DatabaseFactory.dbQuery {
            exec(
                """
                SELECT name, match_key, mela_disambiguator
                  FROM ragas
                 WHERE name IN ('Kalāvathi','Kalāvati','Srimati','Kanadā','Kannada')
                """.trimIndent(),
            ) { rs ->
                buildList {
                    while (rs.next()) {
                        add(Identity(rs.getString(1), rs.getString(2), rs.getInt(3)))
                    }
                }
            } ?: emptyList()
        }

        fun row(name: String) = rows.single { it.name == name }

        val kalavathi = row("Kalāvathi")
        val kalavati = row("Kalāvati")
        assertEquals(kalavathi.matchKey, kalavati.matchKey)
        assertEquals(31, kalavathi.mela)
        assertEquals(16, kalavati.mela)

        val srimati = row("Srimati")
        assertEquals("srimati", srimati.matchKey)
        assertEquals(8, srimati.mela)

        val shreematiRows = DatabaseFactory.dbQuery {
            exec("SELECT count(*) FROM ragas WHERE name = 'Shreemati'") { rs ->
                if (rs.next()) rs.getInt(1) else 0
            } ?: 0
        }
        assertEquals(0, shreematiRows, "Shreemati is a TRACK-137 alias of Shreemani, not a ragas row")

        assertNotEquals(row("Kanadā").matchKey, row("Kannada").matchKey)
    }

    @Test
    fun `Dhāmavathi alias resolves to Dharmavati via identity keys`() = runTest {
        val found = dal.ragas.findOrCreate(name = "Dhāmavathi")
        assertEquals("Dharmavati", found.name)

        val hits = DatabaseFactory.dbQuery {
            val key = exec("SELECT raga_match_key('Dhāmavathi')") { rs ->
                if (rs.next()) rs.getString(1) else error("no key")
            }!!
            RagaIdentityKeysTable
                .selectAll()
                .where { RagaIdentityKeysTable.matchKey eq key }
                .count()
        }
        assertEquals(1L, hits, "Dhāmavathi must be a singleton identity hit")
    }

    @Test
    fun `Gamanāśrama and Gamakakriyā are linked by nomenclature_equivalent, not aliased`() = runTest {
        val relations = DatabaseFactory.dbQuery {
            RagaRelationsTable.selectAll().count()
        }
        assertTrue(relations >= 1L, "expected Gamanāśrama ↔ Gamakakriyā relation")

        val aliased = DatabaseFactory.dbQuery {
            exec(
                """
                SELECT count(*) FROM raga_aliases a
                  JOIN ragas r ON a.raga_id = r.id
                 WHERE r.name = 'Gamakakriyā' AND a.alias IN ('Gamanāśrama','Gamanashrama')
                """.trimIndent(),
            ) { rs -> if (rs.next()) rs.getInt(1) else 0 } ?: 0
        }
        assertEquals(0, aliased, "Gamanāśrama must not be an alias of Gamakakriyā")
    }

    @Test
    fun `alias whose identity collides with a different raga fails at the DB`() = runTest {
        val dharmavati = dal.ragas.findByName("Dharmavati")
        assertNotNull(dharmavati)
        val probe = dal.ragas.create(
            name = "Track136-Collision-Probe",
            parentRagaId = dharmavati.id.toJavaUuid(),
        )
        val thrown = assertFailsWith<Exception> {
            DatabaseFactory.dbQuery {
                exec(
                    """
                    INSERT INTO raga_aliases (raga_id, alias, alias_type, source)
                    VALUES ('${probe.id.toJavaUuid()}', 'Dharmavati', 'transliteration', 'TRACK-136 collision probe')
                    """.trimIndent(),
                )
            }
        }
        val message = generateSequence<Throwable>(thrown) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
        assertTrue(message.contains("collides"), "expected identity collision, got $message")
    }
}
