package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** TRACK-154 / V69: the Saraswati row moves from Basrur to Basara by id, keeping its identity and place. */
class SaraswatiTempleBasaraMigrationTest : IntegrationTestBase() {
    private val sarasvatiId = "01930000-0000-7000-8000-00000000004d"
    private val v69: String = Files.readString(
        Path.of(System.getProperty("user.dir"), "database", "migrations", "V69__saraswati_temple_basara.sql"),
    )

    private suspend fun row(): List<String?> = DatabaseFactory.dbQuery {
        var out: List<String?> = emptyList()
        exec("SELECT city, state, nadi_tirtha, google_place_id FROM temples WHERE id = '$sarasvatiId'") { rs ->
            if (rs.next()) out = listOf(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4))
        }
        out
    }

    @Test
    fun `a Basrur row is corrected in place and an already corrected row is left alone`() = runTest {
        DatabaseFactory.dbQuery {
            exec("DELETE FROM temples WHERE id = '$sarasvatiId'")
            exec(
                "INSERT INTO temples (id, name, name_normalized, city, state, country, place_kind, nadi_tirtha, " +
                    "google_place_id, place_review_state, place_reviewed_by, place_reviewed_at) VALUES " +
                    "('$sarasvatiId', 'Saraswati Temple', 'saraswati temple', 'Basrur', 'Karnataka', 'India', 'COMPLEX', " +
                    "'Sharavati', 'place-synthetic', 'CONFIRMED', 'tester', now())",
            )
            exec(v69)
        }
        assertEquals(listOf("Basara", "Telangana", "Godavari", "place-synthetic"), row())
        DatabaseFactory.dbQuery { exec(v69) }
        assertEquals(listOf("Basara", "Telangana", "Godavari", "place-synthetic"), row())
    }
}
