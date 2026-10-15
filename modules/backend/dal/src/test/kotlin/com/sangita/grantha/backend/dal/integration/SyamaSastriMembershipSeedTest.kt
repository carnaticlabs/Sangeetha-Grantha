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

/** Real Flyway reruns in an isolated schema: legacy repeatables and R__seed_10 must preserve curation and audit changes. */
class SyamaSastriMembershipSeedTest : IntegrationTestBase() {
    @Test
    fun `retired heuristics and R__seed_10 apply approved Ratnatrayam memberships idempotently and abort on conflict`() = runTest {
        val schema = "track146_${UUID.randomUUID().toString().replace("-", "")}"
        val directory = Files.createTempDirectory("track146-flyway-")
        val migrations = Path.of(System.getProperty("user.dir"), "database", "migrations")
        val composerIds = listOf(
            "00000000-0000-0000-0000-000000000146",
            "00000000-0000-0000-0000-000000000147",
            "00000000-0000-0000-0000-000000000148",
        )
        val canonicalComposerId = "c259e3ea-a85c-41b8-951a-19bb3d0d2392"
        val ratnatrayamIds = listOf(
            "f5a415fd-b4ed-4394-ae0b-6f89cacb4fca",
            "2974996a-7cee-433f-b523-dbecbf691f68",
            "7c15e1c9-5c30-4f68-8ab4-a414847e06be",
        )
        val ratnatrayamTagId = "01920000-0000-7000-8000-000000000007"
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
            "R__seed_08_update_musical_forms.sql",
            "R__seed_10_syama_sastri_reviewed_memberships.sql",
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
                composerIds.forEachIndexed { i, id ->
                    val name = listOf("Syama Sastri", "Subbaraya Sastri", "Annaswami Sastri")[i]
                    exec("INSERT INTO $schema.composers VALUES ('$id', '$name')")
                    exec("INSERT INTO $schema.krithis VALUES ('$id', 'kamakshi anudinamu', '$id', 'VARNAM', now())")
                    val provenance = if (i == 0) "curator_reviewed" else "canonical_cycle"
                    exec("INSERT INTO $schema.krithi_tags VALUES ('$id', '$ratnatrayamTagId', '$provenance', 100)")
                }
                // Insert canonical Syama Sastri and the 3 Ratnatrayam compositions with legacy canonical_cycle tags
                exec("INSERT INTO $schema.composers VALUES ('$canonicalComposerId', 'Syama Sastri')")
                val titles = listOf("(ambA) kAmAkshi anudinamu", "rAvE hima giri kumAri", "kAmAkshi nI pada")
                ratnatrayamIds.forEachIndexed { idx, kid ->
                    exec("INSERT INTO $schema.krithis VALUES ('$kid', '${titles[idx]}', '$canonicalComposerId', 'SWARAJATHI', now())")
                    exec("INSERT INTO $schema.krithi_tags VALUES ('$kid', '$ratnatrayamTagId', 'canonical_cycle', 100)")
                }
            }
            // Changed checksums force actual repeatable execution, as on a future deployment.
            // Rerun 0 applies the 3 memberships + 3 tag upgrades (6 system audit events); Rerun 1 is a no-op (still 6 audit events).
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
                            "(SELECT count(*) FROM $schema.krithis WHERE musical_form = 'VARNAM'), " +
                            "(SELECT count(*) FROM $schema.audit_log WHERE actor_user_id IS NULL AND metadata->>'actor' = 'system:TRACK-146'), " +
                            "(SELECT count(*) FROM $schema.krithi_tags WHERE source = 'curator_reviewed'), " +
                            "(SELECT count(*) FROM $schema.krithi_tags WHERE source = 'reviewed_cycle'), " +
                            "(SELECT count(*) FROM $schema.krithi_cycle_memberships WHERE tag_id = '$ratnatrayamTagId')"
                    ) { rs ->
                        rs.next()
                        listOf(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6))
                    }
                }
                assertEquals(listOf(6, 3, 6, 1, 3, 3), result)
            }

            // Verify only approved discriminative_attributes are present (no raga_character, no bhava, no Todi laya, no 'Swarasahitya' label)
            val attrsCheck = DatabaseFactory.dbQuery {
                exec(
                    "SELECT " +
                        "bool_or(discriminative_attributes -> 'raga_character' IS NOT NULL), " +
                        "bool_or(discriminative_attributes -> 'bhava' IS NOT NULL), " +
                        "bool_or(krithi_id = '2974996a-7cee-433f-b523-dbecbf691f68' AND discriminative_attributes -> 'laya' IS NOT NULL), " +
                        "bool_or(discriminative_attributes->>'structure' = 'Swarasahitya') " +
                        "FROM $schema.krithi_cycle_memberships WHERE tag_id = '$ratnatrayamTagId'"
                ) { rs ->
                    rs.next()
                    listOf(rs.getBoolean(1), rs.getBoolean(2), rs.getBoolean(3), rs.getBoolean(4))
                }
            }
            assertEquals(listOf(false, false, false, false), attrsCheck)

            // Verify conflict protection: an independently reviewed membership row causes R__seed_10 to abort rather than overwrite
            DatabaseFactory.dbQuery {
                exec(
                    "UPDATE $schema.krithi_cycle_memberships " +
                        "SET discriminative_attributes = '{\"provenance_owner\": \"INDEPENDENT_CURATOR\"}'::jsonb " +
                        "WHERE krithi_id = '${ratnatrayamIds[0]}'"
                )
            }
            val seed10 = directory.resolve("R__seed_10_syama_sastri_reviewed_memberships.sql")
            Files.writeString(seed10, Files.readString(seed10) + "\n-- conflict test rerun\n")
            val ex = assertFailsWith<FlywayException> {
                DatabaseFactory.dbQuery { flyway.migrate() }
            }
            assertTrue(ex.message.orEmpty().contains("TRACK-146"))
        } finally {
            DatabaseFactory.dbQuery { exec("DROP SCHEMA IF EXISTS $schema CASCADE") }
            Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
        }
    }
}
