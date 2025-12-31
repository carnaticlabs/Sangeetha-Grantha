package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toDeityDto
import com.sangita.grantha.backend.dal.tables.DeitiesTable
import com.sangita.grantha.shared.domain.model.DeityDto
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.util.UUID

class DeityRepository {
    suspend fun listAll(): List<DeityDto> = DatabaseFactory.dbQuery {
        DeitiesTable
            .selectAll()
            .orderBy(DeitiesTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toDeityDto() }
    }
    suspend fun findByName(name: String): DeityDto? = DatabaseFactory.dbQuery {
        DeitiesTable
            .selectAll()
            .where { DeitiesTable.name eq name }
            .map { it.toDeityDto() }
            .singleOrNull()
    }

    suspend fun create(name: String, normalized: String): UUID = DatabaseFactory.dbQuery {
        val newId = UUID.randomUUID()
        DeitiesTable.insert {
            it[id] = newId
            it[DeitiesTable.name] = name
            it[nameNormalized] = normalized
            it[createdAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            it[updatedAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
        }
        newId
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        DeitiesTable.selectAll().count()
    }
}
