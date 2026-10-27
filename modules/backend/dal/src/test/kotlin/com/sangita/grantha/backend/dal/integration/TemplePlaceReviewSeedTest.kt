package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * TRACK-154: behaviour of the generated place-review seed, using the SYNTHETIC fixture
 * (`tests/fixtures/temple_place/synthetic-seed.sql`, kept current by a worker test that compares it
 * with the generator's output). The real R__seed_12 is generated from curator-reviewed data later.
 */
class TemplePlaceReviewSeedTest : IntegrationTestBase() {
    private val fixture: String = Files.readString(
        Path.of(
            System.getProperty("user.dir"),
            "tools", "krithi-extract-enrich-worker", "tests", "fixtures", "temple_place", "synthetic-seed.sql",
        ),
    )

    private suspend fun sql(statement: String) {
        DatabaseFactory.dbQuery { exec(statement) }
    }

    private suspend fun <T> query(statement: String, read: (java.sql.ResultSet) -> T): T =
        DatabaseFactory.dbQuery { exec(statement) { rs -> rs.next(); read(rs) } }!!

    private suspend fun seedTemples() {
        for (name in listOf("alpha temple", "beta temple", "gamma temple")) {
            sql(
                "INSERT INTO temples (name, name_normalized, city, state, country, place_kind, latitude, longitude) " +
                    "VALUES ('${name.replaceFirstChar { it.uppercase() }}', '$name', 'Townville', 'Tamil Nadu', 'India', " +
                    "'COMPLEX', 10.25, 79.5)",
            )
        }
        sql(
            "INSERT INTO temples (name, name_normalized, city, state, country, place_kind, parent_temple_id) " +
                "SELECT 'Sub Shrine', 'sub shrine', 'Townville', 'Tamil Nadu', 'India', 'SANNIDHI', id " +
                "FROM temples WHERE name_normalized = 'alpha temple'",
        )
    }

    private suspend fun auditCount(): Int =
        query("SELECT count(*) FROM audit_log WHERE entity_table = 'temples' AND metadata ->> 'actor' = 'system:TRACK-154'") {
            it.getInt(1)
        }

    private suspend fun placeOf(name: String): List<String?> =
        query(
            "SELECT google_place_id, place_review_state::text, place_reviewed_by FROM temples WHERE name_normalized = '$name'",
        ) { listOf(it.getString(1), it.getString(2), it.getString(3)) }

    @Test
    fun `seed applies reviewed states, skips absent temples, and audits each change with a system actor`() = runTest {
        seedTemples()
        sql(fixture) // the fixture also lists "absent temple", which does not exist here: skipped, not an error

        assertEquals(listOf("synthetic-place-1", "CONFIRMED", "system:TRACK-154:auto"), placeOf("alpha temple"))
        assertEquals(listOf(null, "LOCALITY_ONLY", "Test Curator"), placeOf("beta temple"))
        assertEquals(listOf(null, "UNRESOLVED", "Test Curator"), placeOf("gamma temple"))
        assertEquals(3, auditCount())
        val nullActors = query(
            "SELECT count(*) FROM audit_log WHERE entity_table = 'temples' AND actor_user_id IS NULL " +
                "AND action = 'SET_TEMPLE_PLACE_REVIEW'",
        ) { it.getInt(1) }
        assertEquals(3, nullActors)
    }

    @Test
    fun `sub-shrines get no place of their own`() = runTest {
        seedTemples()
        sql(fixture)
        assertEquals(listOf(null, null, null), placeOf("sub shrine"))
    }

    @Test
    fun `hand-entered coordinates are never written or erased`() = runTest {
        seedTemples()
        sql(fixture)
        val coords = query("SELECT count(*) FROM temples WHERE latitude = 10.25 AND longitude = 79.5") { it.getInt(1) }
        assertEquals(3, coords)
        val untouched = query("SELECT count(*) FROM temples WHERE name_normalized = 'sub shrine' AND latitude IS NULL") {
            it.getInt(1)
        }
        assertEquals(1, untouched)
    }

    @Test
    fun `rerun is idempotent - no new audit rows and no updated_at churn`() = runTest {
        seedTemples()
        sql(fixture)
        val stamp = query("SELECT max(updated_at)::text FROM temples") { it.getString(1) }
        sql(fixture)
        sql(fixture)
        assertEquals(3, auditCount())
        assertEquals(stamp, query("SELECT max(updated_at)::text FROM temples") { it.getString(1) })
    }

    @Test
    fun `rerun is stable across session time zones`() = runTest {
        seedTemples()
        // One transaction so SET LOCAL really applies to the seed runs.
        DatabaseFactory.dbQuery {
            exec("SET LOCAL TIME ZONE 'Asia/Kolkata'")
            exec(fixture)
            exec("SET LOCAL TIME ZONE 'America/Los_Angeles'")
            exec(fixture)
        }
        assertEquals(3, auditCount())
    }

    @Test
    fun `a review set by another actor is left alone and reported`() = runTest {
        seedTemples()
        sql(fixture)
        sql(
            "UPDATE temples SET google_place_id = 'hand-picked', place_review_state = 'CONFIRMED', " +
                "place_reviewed_by = 'Another Curator' WHERE name_normalized = 'beta temple'",
        )
        sql(fixture)
        assertEquals(listOf("hand-picked", "CONFIRMED", "Another Curator"), placeOf("beta temple"))
        assertEquals(3, auditCount())
    }

    @Test
    fun `a pre-existing review by another actor with no seed history is protected`() = runTest {
        seedTemples()
        sql(
            "UPDATE temples SET place_review_state = 'UNRESOLVED', place_reviewed_by = 'Another Curator', " +
                "place_reviewed_at = now() WHERE name_normalized = 'gamma temple'",
        )
        sql(fixture)
        assertEquals(listOf(null, "UNRESOLVED", "Another Curator"), placeOf("gamma temple"))
        assertEquals(2, auditCount()) // alpha and beta applied; gamma protected
    }

    @Test
    fun `rows still as the seed wrote them are updated when the reviewed data changes`() = runTest {
        seedTemples()
        sql(fixture)
        sql(fixture.replace("synthetic-place-1", "synthetic-place-2"))
        assertEquals(listOf("synthetic-place-2", "CONFIRMED", "system:TRACK-154:auto"), placeOf("alpha temple"))
        assertEquals(4, auditCount())
    }

    @Test
    fun `a sub-shrine listed in the reviewed data fails the seed`() = runTest {
        seedTemples()
        val bad = fixture.replace("\"name_normalized\": \"beta temple\"", "\"name_normalized\": \"sub shrine\"")
        val failure = assertFailsWith<Throwable> { sql(bad) }
        val chain = generateSequence(failure) { it.cause }.mapNotNull { it.message }.joinToString(" | ")
        assertTrue(chain.contains("sub-shrine"), chain)
        assertNull(placeOf("sub shrine")[0])
    }
}
