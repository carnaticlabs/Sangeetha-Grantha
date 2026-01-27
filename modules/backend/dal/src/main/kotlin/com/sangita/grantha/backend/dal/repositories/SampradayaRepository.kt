package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toSampradayaDto
import com.sangita.grantha.backend.dal.tables.SampradayasTable
import com.sangita.grantha.shared.domain.model.SampradayaDto
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.util.UUID
import kotlin.uuid.Uuid
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Repository for sampradaya reference data.
 */
class SampradayaRepository {
    /**
     * List all sampradayas ordered by name.
     */
    suspend fun listAll(): List<SampradayaDto> = DatabaseFactory.dbQuery {
        SampradayasTable
            .selectAll()
            .orderBy(SampradayasTable.name to SortOrder.ASC)
            .map { row: ResultRow -> row.toSampradayaDto() }
    }

    /**
     * Count all sampradayas.
     */
    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        SampradayasTable.selectAll().count()
    }

    /**
     * Find a sampradaya by ID.
     */
    suspend fun findById(id: Uuid): SampradayaDto? = DatabaseFactory.dbQuery {
        SampradayasTable
            .selectAll()
            .where { SampradayasTable.id eq id.toJavaUuid() }
            .map { it.toSampradayaDto() }
            .singleOrNull()
    }

    /**
     * Create a new sampradaya.
     */
    suspend fun create(
        name: String,
        nameNormalized: String? = null,
        description: String? = null
    ): SampradayaDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val newId = UUID.randomUUID()

        SampradayasTable.insert {
            it[SampradayasTable.id] = newId
            it[SampradayasTable.name] = name
            it[SampradayasTable.type] = "SCHOOL" // Defaulting to SCHOOL as type is required but not in request
            it[SampradayasTable.description] = description
            it[SampradayasTable.createdAt] = now
        }
            .resultedValues
            ?.single()
            ?.toSampradayaDto()
            ?: error("Failed to insert sampradaya")
    }

    /**
     * Update a sampradaya.
     */
    suspend fun update(
        id: Uuid,
        name: String? = null,
        nameNormalized: String? = null,
        description: String? = null
    ): SampradayaDto? = DatabaseFactory.dbQuery {
        val rowsUpdated = SampradayasTable.update({ SampradayasTable.id eq id.toJavaUuid() }) {
            name?.let { value -> it[SampradayasTable.name] = value }
            description?.let { value -> it[SampradayasTable.description] = value }
        }
        
        if (rowsUpdated > 0) {
            findById(id)
        } else {
            null
        }
    }

    /**
     * Delete a sampradaya.
     */
    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = SampradayasTable.deleteWhere { SampradayasTable.id eq id.toJavaUuid() }
        deleted > 0
    }
}
