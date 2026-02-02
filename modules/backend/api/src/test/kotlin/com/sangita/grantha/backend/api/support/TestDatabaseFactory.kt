package com.sangita.grantha.backend.api.support

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.tables.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object TestDatabaseFactory {
    fun connectTestDb() {
        DatabaseFactory.connect(
            DatabaseFactory.ConnectionConfig(
                databaseUrl = "jdbc:h2:mem:test_${System.currentTimeMillis()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
                username = "sa",
                password = "",
                driverClassName = "org.h2.Driver",
                enableQueryLogging = false
            )
        )

        kotlinx.coroutines.runBlocking {
            DatabaseFactory.dbQuery {
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
