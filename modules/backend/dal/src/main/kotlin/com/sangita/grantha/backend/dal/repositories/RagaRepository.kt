package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toRagaDto
import com.sangita.grantha.backend.dal.tables.RagasTable
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * Repository for raga reference data.
 */
class RagaRepository {
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

    /**
     * List all ragas ordered by normalized name.
     */
    suspend fun listAll(): List<RagaDto> = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .orderBy(RagasTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toRagaDto() }
    }

    /**
     * Find a raga by ID.
     */
    suspend fun findById(id: Uuid): RagaDto? = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .where { RagasTable.id eq id.toJavaUuid() }
            .map { it.toRagaDto() }
            .singleOrNull()
    }

    /**
     * Find a raga by exact name.
     */
    suspend fun findByName(name: String): RagaDto? = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .where { RagasTable.name eq name }
            .map { it.toRagaDto() }
            .singleOrNull()
    }

    /**
     * Find a raga by normalized name.
     */
    suspend fun findByNameNormalized(nameNormalized: String): RagaDto? = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .where { RagasTable.nameNormalized eq nameNormalized }
            .map { it.toRagaDto() }
            .singleOrNull()
    }

    /**
     * Find an existing raga or create a new record.
     */
    suspend fun findOrCreate(
        name: String,
        nameNormalized: String? = null,
        melakartaNumber: Int? = null,
        parentRagaId: UUID? = null,
        arohanam: String? = null,
        avarohanam: String? = null,
        notes: String? = null
    ): RagaDto {
        val normalized = nameNormalized ?: normalize(name)
        
        findByNameNormalized(normalized)?.let { return it }
        findByName(name)?.let { return it }

        return try {
            create(name, normalized, melakartaNumber, parentRagaId, arohanam, avarohanam, notes)
        } catch (e: Exception) {
            findByNameNormalized(normalized) ?: findByName(name) ?: throw e
        }
    }

    /**
     * Create a new raga record.
     */
    suspend fun create(
        name: String,
        nameNormalized: String? = null,
        melakartaNumber: Int? = null,
        parentRagaId: UUID? = null,
        arohanam: String? = null,
        avarohanam: String? = null,
        notes: String? = null
    ): RagaDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val ragaId = UUID.randomUUID()
        val normalized = nameNormalized ?: normalize(name)

        RagasTable.insert {
            it[id] = ragaId
            it[RagasTable.name] = name
            it[RagasTable.nameNormalized] = normalized
            it[RagasTable.melakartaNumber] = melakartaNumber
            it[RagasTable.parentRagaId] = parentRagaId
            it[RagasTable.arohanam] = arohanam
            it[RagasTable.avarohanam] = avarohanam
            it[RagasTable.notes] = notes
            it[RagasTable.createdAt] = now
            it[RagasTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toRagaDto()
            ?: error("Failed to insert raga")
    }

    /**
     * Update a raga and return the updated record.
     */
    suspend fun update(
        id: Uuid,
        name: String? = null,
        nameNormalized: String? = null,
        melakartaNumber: Int? = null,
        parentRagaId: UUID? = null,
        arohanam: String? = null,
        avarohanam: String? = null,
        notes: String? = null
    ): RagaDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaId = id.toJavaUuid()
        
        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        RagasTable
            .updateReturning(
                where = { RagasTable.id eq javaId }
            ) {
                name?.let { value -> 
                    it[RagasTable.name] = value
                    it[RagasTable.nameNormalized] = nameNormalized ?: normalize(value)
                }
                nameNormalized?.let { value -> it[RagasTable.nameNormalized] = value }
                melakartaNumber?.let { value -> it[RagasTable.melakartaNumber] = value }
                parentRagaId?.let { value -> it[RagasTable.parentRagaId] = value }
                arohanam?.let { value -> it[RagasTable.arohanam] = value }
                avarohanam?.let { value -> it[RagasTable.avarohanam] = value }
                notes?.let { value -> it[RagasTable.notes] = value }
                it[RagasTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toRagaDto()
    }

    /**
     * Delete a raga by ID.
     */
    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = RagasTable.deleteWhere { RagasTable.id eq id.toJavaUuid() }
        deleted > 0
    }

    /**
     * Count all ragas.
     */
    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        RagasTable.selectAll().count()
    }
}
