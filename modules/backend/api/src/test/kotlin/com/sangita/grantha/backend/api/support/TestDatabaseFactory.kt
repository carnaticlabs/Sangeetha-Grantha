package com.sangita.grantha.backend.api.support

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.tables.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object TestDatabaseFactory {
    fun connectTestDb() {
        DatabaseFactory.connect(
            DatabaseFactory.ConnectionConfig(
                databaseUrl = "jdbc:postgresql://localhost:5432/sangita_grantha_test",
                username = "postgres",
                password = "postgres",
                driverClassName = "org.postgresql.Driver",
                enableQueryLogging = false
            )
        )

        kotlinx.coroutines.runBlocking {
            DatabaseFactory.dbQuery {
                // Drop existing types to ensure clean slate if reset() wasn't called or failed
                exec("DROP TYPE IF EXISTS workflow_state_enum CASCADE")
                exec("DROP TYPE IF EXISTS language_code_enum CASCADE")
                exec("DROP TYPE IF EXISTS script_code_enum CASCADE")
                exec("DROP TYPE IF EXISTS raga_section_enum CASCADE")
                exec("DROP TYPE IF EXISTS import_status_enum CASCADE")
                exec("DROP TYPE IF EXISTS batch_status_enum CASCADE")
                exec("DROP TYPE IF EXISTS job_type_enum CASCADE")
                exec("DROP TYPE IF EXISTS task_status_enum CASCADE")
                exec("DROP TYPE IF EXISTS musical_form_enum CASCADE")

                // Create ENUM types
                exec("CREATE TYPE workflow_state_enum AS ENUM ('draft', 'in_review', 'published', 'archived')")
                exec("CREATE TYPE language_code_enum AS ENUM ('sa', 'ta', 'te', 'kn', 'ml', 'hi', 'en')")
                exec("CREATE TYPE script_code_enum AS ENUM ('devanagari', 'tamil', 'telugu', 'kannada', 'malayalam', 'latin')")
                exec("CREATE TYPE raga_section_enum AS ENUM ('pallavi', 'anupallavi', 'charanam', 'samashti_charanam', 'chittaswaram', 'swara_sahitya', 'madhyama_kala', 'solkattu_swara', 'anubandha', 'muktayi_swara', 'ettugada_swara', 'ettugada_sahitya', 'viloma_chittaswaram', 'other')")
                exec("CREATE TYPE import_status_enum AS ENUM ('pending', 'in_review', 'approved', 'mapped', 'rejected', 'discarded')")
                exec("CREATE TYPE musical_form_enum AS ENUM ('KRITHI', 'VARNAM', 'SWARAJATHI')")
                exec("CREATE TYPE batch_status_enum AS ENUM ('pending', 'running', 'paused', 'succeeded', 'failed', 'cancelled')")
                exec("CREATE TYPE job_type_enum AS ENUM ('manifest_ingest', 'scrape', 'enrich', 'entity_resolution', 'review_prep')")
                exec("CREATE TYPE task_status_enum AS ENUM ('pending', 'running', 'succeeded', 'failed', 'retryable', 'blocked', 'cancelled')")

                SchemaUtils.create(
                    RolesTable,
                    UsersTable,
                    RoleAssignmentsTable,
                    ComposersTable,
                    RagasTable,
                    TalasTable,
                    DeitiesTable,
                    TemplesTable,
                    TempleNamesTable,
                    TagsTable,
                    SampradayasTable,
                    KrithisTable,
                    KrithiRagasTable,
                    KrithiLyricVariantsTable,
                    KrithiSectionsTable,
                    KrithiLyricSectionsTable,
                    KrithiNotationVariantsTable,
                    KrithiNotationRowsTable,
                    KrithiTagsTable,
                    ImportSourcesTable,
                    ImportedKrithisTable,
                    AuditLogTable,
                    ImportBatchTable,
                    ImportJobTable,
                    ImportTaskRunTable,
                    ImportEventTable,
                    EntityResolutionCacheTable,
                    TempleSourceCacheTable
                )
            }
        }
    }

    fun reset() {
        kotlinx.coroutines.runBlocking {
            DatabaseFactory.dbQuery {
                SchemaUtils.drop(
                    TempleSourceCacheTable,
                    EntityResolutionCacheTable,
                    ImportEventTable,
                    ImportTaskRunTable,
                    ImportJobTable,
                    ImportBatchTable,
                    AuditLogTable,
                    ImportedKrithisTable,
                    ImportSourcesTable,
                    KrithiTagsTable,
                    KrithiNotationRowsTable,
                    KrithiNotationVariantsTable,
                    KrithiLyricSectionsTable,
                    KrithiSectionsTable,
                    KrithiLyricVariantsTable,
                    KrithiRagasTable,
                    KrithisTable,
                    SampradayasTable,
                    TagsTable,
                    TempleNamesTable,
                    TemplesTable,
                    DeitiesTable,
                    TalasTable,
                    RagasTable,
                    ComposersTable,
                    RoleAssignmentsTable,
                    UsersTable,
                    RolesTable
                )
            }
        }
        DatabaseFactory.close()
    }
}
