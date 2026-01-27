package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toTalaDto
import com.sangita.grantha.backend.dal.tables.TalasTable
import com.sangita.grantha.shared.domain.model.TalaDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * Repository for tala reference data.
 */
class TalaRepository {
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

    /**
     * List all talas ordered by normalized name.
     */
    suspend fun listAll(): List<TalaDto> = DatabaseFactory.dbQuery {
        TalasTable
            .selectAll()
            .orderBy(TalasTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toTalaDto() }
    }

    /**
     * Find a tala by ID.
     */
    suspend fun findById(id: Uuid): TalaDto? = DatabaseFactory.dbQuery {
        TalasTable
            .selectAll()
            .where { TalasTable.id eq id.toJavaUuid() }
            .map { it.toTalaDto() }
            .singleOrNull()
    }

    /**
     * Find a tala by exact name.
     */
    suspend fun findByName(name: String): TalaDto? = DatabaseFactory.dbQuery {
        TalasTable
            .selectAll()
            .where { TalasTable.name eq name }
            .map { it.toTalaDto() }
            .singleOrNull()
    }

    /**
     * Find a tala by normalized name.
     */
    suspend fun findByNameNormalized(nameNormalized: String): TalaDto? = DatabaseFactory.dbQuery {
        TalasTable
            .selectAll()
            .where { TalasTable.nameNormalized eq nameNormalized }
            .map { it.toTalaDto() }
            .singleOrNull()
    }

    /**
     * Find an existing tala or create a new record.
     */
    suspend fun findOrCreate(
        name: String,
        nameNormalized: String? = null,
        beatCount: Int? = null,
        angaStructure: String? = null,
        notes: String? = null
    ): TalaDto {
        val normalized = nameNormalized ?: normalize(name)
        
        // Try finding by normalized name first (most robust)
        findByNameNormalized(normalized)?.let { return it }
        
        // Try finding by exact name as fallback
        findByName(name)?.let { return it }

        // Attempt create, handling potential race condition
        return try {
            create(name, normalized, beatCount, angaStructure, notes)
        } catch (e: Exception) {
            // If duplicate violation, try finding again
            findByNameNormalized(normalized) ?: findByName(name) ?: throw e
        }
    }

    /**
     * Create a new tala record.
     */
    suspend fun create(
        name: String,
        nameNormalized: String? = null,
        beatCount: Int? = null,
        angaStructure: String? = null,
        notes: String? = null
    ): TalaDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val talaId = UUID.randomUUID()
        val normalized = nameNormalized ?: normalize(name)

        TalasTable.insert {
            it[id] = talaId
            it[TalasTable.name] = name
            it[TalasTable.nameNormalized] = normalized
            it[TalasTable.beatCount] = beatCount
            it[TalasTable.angaStructure] = angaStructure
            it[TalasTable.notes] = notes
            it[TalasTable.createdAt] = now
            it[TalasTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toTalaDto()
            ?: error("Failed to insert tala")
    }

    /**
     * Update a tala and return the updated record.
     */
    suspend fun update(
        id: Uuid,
        name: String? = null,
        nameNormalized: String? = null,
        beatCount: Int? = null,
        angaStructure: String? = null,
        notes: String? = null
    ): TalaDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaId = id.toJavaUuid()
        
        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        TalasTable
            .updateReturning(
                where = { TalasTable.id eq javaId }
            ) {
                name?.let { value -> 
                    it[TalasTable.name] = value
                    it[TalasTable.nameNormalized] = nameNormalized ?: normalize(value)
                }
                nameNormalized?.let { value -> it[TalasTable.nameNormalized] = value }
                beatCount?.let { value -> it[TalasTable.beatCount] = value }
                angaStructure?.let { value -> it[TalasTable.angaStructure] = value }
                notes?.let { value -> it[TalasTable.notes] = value }
                it[TalasTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toTalaDto()
    }

    /**
     * Delete a tala by ID.
     */
    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = TalasTable.deleteWhere { TalasTable.id eq id.toJavaUuid() }
        deleted > 0
    }

    /**
     * Count all talas.
     */
    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        TalasTable.selectAll().count()
    }
}
