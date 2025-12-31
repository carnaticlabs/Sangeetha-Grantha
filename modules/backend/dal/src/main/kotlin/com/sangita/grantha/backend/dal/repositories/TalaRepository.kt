package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toTalaDto
import com.sangita.grantha.backend.dal.tables.TalasTable
import com.sangita.grantha.shared.domain.model.TalaDto
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.util.UUID

class TalaRepository {
    suspend fun listAll(): List<TalaDto> = DatabaseFactory.dbQuery {
        TalasTable
            .selectAll()
            .orderBy(TalasTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toTalaDto() }
    }
    suspend fun findByName(name: String): TalaDto? = DatabaseFactory.dbQuery {
        TalasTable
            .selectAll()
            .where { TalasTable.name eq name }
            .map { it.toTalaDto() }
            .singleOrNull()
    }

    suspend fun create(name: String, normalized: String, beats: Int?, structure: String?): UUID = DatabaseFactory.dbQuery {
        val newId = UUID.randomUUID()
        TalasTable.insert {
            it[id] = newId
            it[TalasTable.name] = name
            it[nameNormalized] = normalized
            it[beatCount] = beats
            it[angaStructure] = structure
            it[createdAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            it[updatedAt] = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
        }
        newId
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        TalasTable.selectAll().count()
    }
}
