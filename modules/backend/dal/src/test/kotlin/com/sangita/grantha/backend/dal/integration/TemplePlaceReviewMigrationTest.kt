package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.PlaceReviewState
import com.sangita.grantha.backend.dal.tables.TemplesTable
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestDatabase
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test

/** TRACK-154 / V68: place review columns, CHECKs and the CONFIRMED-only unique index. */
class TemplePlaceReviewMigrationTest : IntegrationTestBase() {

    private suspend fun insertTemple(
        name: String,
        placeKind: String = "COMPLEX",
        placeId: String? = null,
        state: String? = null,
        by: String? = null,
        at: String? = null,
    ) {
        fun lit(v: String?) = v?.let { "'$it'" } ?: "NULL"
        DatabaseFactory.dbQuery {
            exec(
                "INSERT INTO temples (name, name_normalized, city, state, country, place_kind, " +
                    "google_place_id, place_review_state, place_reviewed_by, place_reviewed_at) VALUES " +
                    "('$name', '${name.lowercase()}', 'City', 'State', 'India', '$placeKind', " +
                    "${lit(placeId)}, ${lit(state)}::place_review_state_enum, ${lit(by)}, ${lit(at)}::timestamptz)",
            )
        }
    }

    private suspend fun assertRejected(constraint: String, block: suspend () -> Unit) {
        val failure = assertFailsWith<Throwable> { block() }
        val chain = generateSequence(failure) { it.cause }.mapNotNull { it.message }.joinToString(" | ")
        assertTrue(chain.contains(constraint), "expected $constraint in: $chain")
    }

    @Test
    fun `new temples start unreviewed and carry no Google content columns beyond the place id`() = runTest {
        insertTemple("fresh temple")
        val columns = DatabaseFactory.dbQuery {
            exec(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' " +
                    "AND table_name = 'temples' AND (column_name LIKE 'google%' OR column_name LIKE 'place_%' " +
                    "OR column_name LIKE 'formatted%' OR column_name LIKE '%address%') ORDER BY 1",
            ) { rs -> buildList { while (rs.next()) add(rs.getString(1)) } }
        }
        assertEquals(
            listOf("google_place_id", "place_kind", "place_review_state", "place_reviewed_at", "place_reviewed_by"),
            columns,
        )
        val unreviewed = DatabaseFactory.dbQuery {
            exec("SELECT count(*) FROM temples WHERE google_place_id IS NULL AND place_review_state IS NULL") { rs ->
                rs.next(); rs.getInt(1)
            }
        }
        assertEquals(1, unreviewed)
    }

    @Test
    fun `Exposed table round-trips the review columns`() = runTest {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        DatabaseFactory.dbQuery {
            TemplesTable.insert {
                it[TemplesTable.id] = id
                it[name] = "Round Trip"
                it[nameNormalized] = "round trip"
                it[googlePlaceId] = "ChIJsynthetic0000000000000001"
                it[placeReviewState] = PlaceReviewState.CONFIRMED
                it[placeReviewedBy] = "Seshadri"
                it[placeReviewedAt] = now
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
        val row = DatabaseFactory.dbQuery { TemplesTable.selectAll().where { TemplesTable.id eq id }.single() }
        assertEquals("ChIJsynthetic0000000000000001", row[TemplesTable.googlePlaceId])
        assertEquals(PlaceReviewState.CONFIRMED, row[TemplesTable.placeReviewState])
        assertEquals("Seshadri", row[TemplesTable.placeReviewedBy])
        assertNull(row[TemplesTable.latitude])
        assertNull(row[TemplesTable.longitude])
    }

    @Test
    fun `CONFIRMED requires a place id`() = runTest {
        assertRejected("chk_temples_place_confirmed_has_id") {
            insertTemple("a", state = "CONFIRMED", by = "x", at = "2026-10-25T00:00:00Z")
        }
    }

    @Test
    fun `UNRESOLVED must not carry a place id`() = runTest {
        assertRejected("chk_temples_place_unresolved_has_no_id") {
            insertTemple("b", placeId = "p1", state = "UNRESOLVED", by = "x", at = "2026-10-25T00:00:00Z")
        }
    }

    @Test
    fun `a place id requires a review state`() = runTest {
        assertRejected("chk_temples_place_id_needs_state") { insertTemple("c", placeId = "p2") }
    }

    @Test
    fun `a review state requires approver and date, and approver without state is rejected`() = runTest {
        assertRejected("chk_temples_place_review_audit_fields") { insertTemple("d", state = "UNRESOLVED") }
        assertRejected("chk_temples_place_review_audit_fields") { insertTemple("e", by = "x") }
    }

    @Test
    fun `sub-shrines cannot hold their own place or review state`() = runTest {
        for (kind in listOf("SANNIDHI", "MANDAPAM")) {
            assertRejected("chk_temples_subshrine_no_own_place") {
                insertTemple("sub $kind", placeKind = kind, state = "UNRESOLVED", by = "x", at = "2026-10-25T00:00:00Z")
            }
        }
    }

    @Test
    fun `two CONFIRMED temples cannot share a place id but other states can`() = runTest {
        val ok = arrayOf("x", "2026-10-25T00:00:00Z")
        insertTemple("one", placeId = "shared", state = "CONFIRMED", by = ok[0], at = ok[1])
        assertRejected("uq_temples_confirmed_google_place_id") {
            insertTemple("two", placeId = "shared", state = "CONFIRMED", by = ok[0], at = ok[1])
        }
        // The partial index covers CONFIRMED only.
        insertTemple("three", placeId = "shared", state = "LOCALITY_ONLY", by = ok[0], at = ok[1])
    }

    @Test
    fun `V68 is recorded and a second migrate is a no-op`() = runTest {
        val conn = TestDatabase.connection
        val flyway = Flyway.configure()
            .dataSource(conn.jdbcUrl, conn.username, conn.password)
            .locations("filesystem:${Paths.get(System.getProperty("user.dir")).resolve("database/migrations")}")
            .load()
        val applied = flyway.info().applied().map { it.version?.version }
        assertTrue("68" in applied, "V68 not applied: $applied")
        assertEquals(0, flyway.migrate().migrationsExecuted)
    }
}
