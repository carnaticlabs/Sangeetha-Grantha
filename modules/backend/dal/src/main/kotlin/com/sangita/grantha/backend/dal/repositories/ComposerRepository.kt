package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toComposerDto
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.shared.domain.model.ComposerDto
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.util.UUID

class ComposerRepository {
    suspend fun listAll(): List<ComposerDto> = DatabaseFactory.dbQuery {
        ComposersTable
            .selectAll()
            .orderBy(ComposersTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toComposerDto() }
    }
    suspend fun findByName(name: String): ComposerDto? = DatabaseFactory.dbQuery {
        ComposersTable
            .selectAll()
            .where { ComposersTable.name eq name }
            .map { it.toComposerDto() }
            .singleOrNull()
    }

    suspend fun create(name: String, normalized: String, start: Int?, end: Int?): UUID = DatabaseFactory.dbQuery {
        val newId = UUID.randomUUID()
        ComposersTable.insert {
            it[id] = newId
            it[ComposersTable.name] = name
            it[nameNormalized] = normalized
            it[birthYear] = start
            it[deathYear] = end
            it[createdAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            it[updatedAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
        }
        newId
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        ComposersTable.selectAll().count()
    }
}
