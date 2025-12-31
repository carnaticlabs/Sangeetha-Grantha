package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toRagaDto
import com.sangita.grantha.backend.dal.tables.RagasTable
import com.sangita.grantha.shared.domain.model.RagaDto
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.util.UUID

class RagaRepository {
    suspend fun listAll(): List<RagaDto> = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .orderBy(RagasTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toRagaDto() }
    }
    suspend fun findByName(name: String): RagaDto? = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .where { RagasTable.name eq name }
            .map { it.toRagaDto() }
            .singleOrNull()
    }

    suspend fun create(name: String, normalized: String, melakarta: Int?, aro: String?, ava: String?): UUID = DatabaseFactory.dbQuery {
        val newId = UUID.randomUUID()
        RagasTable.insert {
            it[id] = newId
            it[RagasTable.name] = name
            it[nameNormalized] = normalized
            it[melakartaNumber] = melakarta
            it[arohanam] = aro
            it[avarohanam] = ava
            it[createdAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            it[updatedAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
        }
        newId
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        RagasTable.selectAll().count()
    }
}
