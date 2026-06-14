package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * D3: primary keys default to PostgreSQL 18 `uuidv7()` — version-7 and time-ordered. The whole
 * keyspace (27 keyed tables) depends on this column default; V37 once silently reset the defaults
 * to `gen_random_uuid()` (see TRACK-110), so this guards that regression directly.
 *
 * Note: repositories assign ids app-side (`UUID.randomUUID()`), so to exercise the **DB default**
 * we insert without an id and let Postgres generate it.
 */
class UuidV7Test : IntegrationTestBase() {

    /**
     * Inserts a composer letting the `id` column default (`uuidv7()`) fire, then reads the id back.
     * Raw SQL deliberately bypasses the repository (which assigns ids app-side) and Exposed's
     * client-side UUID default, so the assertion is genuinely about the Postgres column default.
     */
    private suspend fun insertWithDefaultId(name: String): UUID = DatabaseFactory.dbQuery {
        exec(
            "INSERT INTO composers (name, name_normalized, created_at, updated_at) " +
                "VALUES ('$name', '$name', now(), now())"
        )
        exec("SELECT id FROM composers WHERE name_normalized = '$name'") { rs ->
            if (rs.next()) UUID.fromString(rs.getString(1)) else error("inserted row not found")
        } ?: error("select returned no result")
    }

    @Test
    fun `db default generates a uuid version 7`() = runTest {
        val id = insertWithDefaultId("probe_${Uuid.random()}")
        assertEquals(7, id.version(), "the id column default must be uuidv7()")
    }

    @Test
    fun `db default ids are time-monotonic`() = runTest {
        val ids = (1..12).map { insertWithDefaultId("probe_${it}_${Uuid.random()}") }
        ids.forEach { assertEquals(7, it.version()) }

        // The most-significant 48 bits of a v7 UUID are the unix-ms timestamp. Sequential inserts
        // must produce non-decreasing timestamps (equal is fine within the same millisecond).
        val timestamps = ids.map { (it.mostSignificantBits ushr 16) and 0xFFFFFFFFFFFFL }
        assertEquals(timestamps.sorted(), timestamps, "v7 ids must be time-ordered by insertion")
    }
}
