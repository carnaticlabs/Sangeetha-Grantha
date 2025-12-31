package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toTempleDto
import com.sangita.grantha.backend.dal.tables.TemplesTable
import com.sangita.grantha.shared.domain.model.TempleDto
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.util.UUID

class TempleRepository {
    suspend fun listAll(): List<TempleDto> = DatabaseFactory.dbQuery {
        TemplesTable
            .selectAll()
            .orderBy(TemplesTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toTempleDto() }
    }
    suspend fun findByName(name: String): TempleDto? = DatabaseFactory.dbQuery {
        TemplesTable
            .selectAll()
            .where { TemplesTable.name eq name }
            .map { it.toTempleDto() }
            .singleOrNull()
    }

    suspend fun create(name: String, normalized: String, city: String?, state: String?): UUID = DatabaseFactory.dbQuery {
        val newId = UUID.randomUUID()
        TemplesTable.insert {
            it[id] = newId
            it[TemplesTable.name] = name
            it[nameNormalized] = normalized
            it[TemplesTable.city] = city
            it[TemplesTable.state] = state
            it[createdAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            it[updatedAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
        }
        newId
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        TemplesTable.selectAll().count()
    }
}
