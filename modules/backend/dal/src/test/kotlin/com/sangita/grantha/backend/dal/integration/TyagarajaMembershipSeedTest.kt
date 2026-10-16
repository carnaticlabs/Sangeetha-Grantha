package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.junit.jupiter.api.Test

/** Real Flyway reruns in an isolated schema: retired heuristics in R__seed_07 and R__seed_11 must preserve curation and audit changes. */
class TyagarajaMembershipSeedTest : IntegrationTestBase() {
    @Test
    fun `retired heuristics and R__seed_11 apply approved Ghana-raga Pancharatnam memberships idempotently and abort on conflict`() = runTest {
        val schema = "track147_${UUID.randomUUID().toString().replace("-", "")}"
        val directory = Files.createTempDirectory("track147-flyway-")
        val migrations = Path.of(System.getProperty("user.dir"), "database", "migrations")
        val canonicalComposerId = "b1bdccd8-8cb1-4a14-a9e6-7ffc843cd7b6"
        val dikshitarComposerId = "00000000-0000-0000-0000-000000000145"
        val nonCycleTyagarajaKrithiId = "00000000-0000-0000-0000-000000000675"
        val ghanaPancharatnamIds = listOf(
            "2e115182-dcc2-4a41-962c-8381821a7489",
            "b4029df0-a209-470b-ba56-3d64fa8aebc4",
            "6fd7860c-0e5d-428f-af9f-cd02c3ad2da8",
            "d59984e7-fabc-4536-b5a2-60e550277626",
            "2b65609e-9fbf-4232-baae-096cafdf4e79",
        )
        val pancharatnamTagId = "01920000-0000-7000-8000-000000000004"
        val baseline = """
            CREATE TYPE cycle_member_role_enum AS ENUM (
                'CORE', 'DHYANA', 'MANGALAM', 'OPTIONAL', 'DISPUTED_CONJECTURE'
            );
            CREATE TABLE tags (id uuid PRIMARY KEY, category text, slug text UNIQUE,
                display_name_en text, description_en text, created_at timestamptz);
            CREATE TABLE composers (id uuid PRIMARY KEY, name text);
            CREATE TABLE krithis (id uuid PRIMARY KEY, title text, composer_id uuid,
                musical_form text, updated_at timestamptz);
            CREATE TABLE krithi_tags (krithi_id uuid, tag_id uuid, source text, confidence int,
                PRIMARY KEY (krithi_id, tag_id));
            CREATE TABLE krithi_cycle_memberships (
                id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                krithi_id uuid NOT NULL REFERENCES krithis (id) ON DELETE CASCADE,
                tag_id uuid NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
                sequence_order int NOT NULL DEFAULT 0,
                role cycle_member_role_enum NOT NULL DEFAULT 'CORE',
                axis_value varchar(64),
                discriminative_attributes jsonb NOT NULL DEFAULT '{}'::jsonb,
                created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
                CONSTRAINT uq_krithi_cycle UNIQUE (krithi_id, tag_id)
            );
            CREATE TABLE users (id uuid, created_at timestamptz);
            CREATE TABLE audit_log (id uuid DEFAULT gen_random_uuid(), entity_table text, entity_id uuid, actor_user_id uuid,
                action text, diff jsonb, metadata jsonb);
        """.trimIndent()
        Files.writeString(directory.resolve("V1__fixture.sql"), baseline)
        val names = listOf(
            "R__seed_07_canonical_cycle_tags.sql",
            "R__seed_11_tyagaraja_reviewed_memberships.sql",
        )
        names.forEach { Files.copy(migrations.resolve(it), directory.resolve(it)) }
        val connection = TestDatabase.connection
        val flyway = Flyway.configure()
            .dataSource(connection.jdbcUrl, connection.username, connection.password)
            .locations("filesystem:$directory")
            .schemas(schema)
            .defaultSchema(schema)
            .load()
        try {
            DatabaseFactory.dbQuery { flyway.migrate() }
            DatabaseFactory.dbQuery {
                exec("INSERT INTO $schema.composers VALUES ('$canonicalComposerId', 'Tyagaraja')")
                exec("INSERT INTO $schema.composers VALUES ('$dikshitarComposerId', 'Muthuswami Dikshitar')")
                // Insert an independent curator-reviewed tag row that must be preserved untouched
                exec("INSERT INTO $schema.krithis VALUES ('$nonCycleTyagarajaKrithiId', 'sundarESvaruni', '$canonicalComposerId', 'KRITHI', now())")
                exec("INSERT INTO $schema.krithi_tags VALUES ('$nonCycleTyagarajaKrithiId', '$pancharatnamTagId', 'curator_reviewed', 100)")
                // Insert a Dikshitar Tyagaraja-deity krithi that must never be matched as composer Tyagaraja
                exec("INSERT INTO $schema.krithis VALUES ('$dikshitarComposerId', 'tyAgarAja yOga vaibhavaM', '$dikshitarComposerId', 'KRITHI', now())")

                val titles = listOf(
                    "jagadAnanda kAraka",
                    "duDuku gala",
                    "sAdhincenE",
                    "kana kana rucirA",
                    "Endaro Mahaanubhaavulu",
                )
                ghanaPancharatnamIds.forEachIndexed { idx, kid ->
                    exec("INSERT INTO $schema.krithis VALUES ('$kid', '${titles[idx]}', '$canonicalComposerId', 'KRITHI', now())")
                    exec("INSERT INTO $schema.krithi_tags VALUES ('$kid', '$pancharatnamTagId', 'canonical_cycle', 100)")
                }
            }
            // Rerun 0 applies 5 memberships + 5 tag upgrades (10 system audit events); Rerun 1 is a no-op (still 10 audit events).
            repeat(2) { rerun ->
                names.forEach { name ->
                    val file = directory.resolve(name)
                    Files.writeString(file, Files.readString(file) + "\n-- fixture rerun $rerun\n")
                }
                DatabaseFactory.dbQuery { flyway.migrate() }
                val result = DatabaseFactory.dbQuery {
                    exec(
                        "SELECT " +
                            "(SELECT count(*) FROM $schema.krithi_tags), " +
                            "(SELECT count(*) FROM $schema.audit_log WHERE actor_user_id IS NULL AND metadata->>'actor' = 'system:TRACK-147'), " +
                            "(SELECT count(*) FROM $schema.krithi_tags WHERE source = 'curator_reviewed'), " +
                            "(SELECT count(*) FROM $schema.krithi_tags WHERE source = 'reviewed_cycle'), " +
                            "(SELECT count(*) FROM $schema.krithi_cycle_memberships WHERE tag_id = '$pancharatnamTagId')"
                    ) { rs ->
                        rs.next()
                        listOf(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5))
                    }
                }
                assertEquals(listOf(6, 10, 1, 5, 5), result)
            }

            // Verify Aradhana performance order and approved attributes on the 5 members
            val attrsCheck = DatabaseFactory.dbQuery {
                exec(
                    "SELECT " +
                        "bool_and(discriminative_attributes->>'provenance_owner' = 'TRACK-147'), " +
                        "bool_and(discriminative_attributes->>'review_state' = 'reviewed'), " +
                        "bool_and(discriminative_attributes->>'mudra' = 'Tyagaraja'), " +
                        "bool_or(krithi_id = '6fd7860c-0e5d-428f-af9f-cd02c3ad2da8' AND discriminative_attributes->>'structure' = '7 Swara-Sahitya sections') " +
                        "FROM $schema.krithi_cycle_memberships WHERE tag_id = '$pancharatnamTagId'"
                ) { rs ->
                    rs.next()
                    listOf(rs.getBoolean(1), rs.getBoolean(2), rs.getBoolean(3), rs.getBoolean(4))
                }
            }
            assertEquals(listOf(true, true, true, true), attrsCheck)

            // Verify conflict protection: an independently reviewed membership row causes R__seed_11 to abort rather than overwrite
            DatabaseFactory.dbQuery {
                exec(
                    "UPDATE $schema.krithi_cycle_memberships " +
                        "SET discriminative_attributes = '{\"provenance_owner\": \"INDEPENDENT_CURATOR\"}'::jsonb " +
                        "WHERE krithi_id = '${ghanaPancharatnamIds[0]}'"
                )
            }
            val seed11 = directory.resolve("R__seed_11_tyagaraja_reviewed_memberships.sql")
            Files.writeString(seed11, Files.readString(seed11) + "\n-- conflict test rerun\n")
            val ex = assertFailsWith<FlywayException> {
                DatabaseFactory.dbQuery { flyway.migrate() }
            }
            assertTrue(ex.message.orEmpty().contains("TRACK-147"))
        } finally {
            DatabaseFactory.dbQuery { exec("DROP SCHEMA IF EXISTS $schema CASCADE") }
            Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
        }
    }
}
