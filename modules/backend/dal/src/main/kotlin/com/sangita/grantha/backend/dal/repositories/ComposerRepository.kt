package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toComposerDto
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * Repository for composer reference data.
 * TRACK-031: Uses composer aliases to avoid creating duplicates for known short names.
 */
class ComposerRepository(
    private val composerAliases: ComposerAliasRepository
) {
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

    /**
     * List all composers ordered by normalized name.
     */
    suspend fun listAll(): List<ComposerDto> = DatabaseFactory.dbQuery {
        ComposersTable
            .selectAll()
            .orderBy(ComposersTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toComposerDto() }
    }

    /**
     * Find a composer by ID.
     */
    suspend fun findById(id: Uuid): ComposerDto? = DatabaseFactory.dbQuery {
        ComposersTable
            .selectAll()
            .where { ComposersTable.id eq id.toJavaUuid() }
            .map { it.toComposerDto() }
            .singleOrNull()
    }

    /**
     * Find a composer by exact name.
     */
    suspend fun findByName(name: String): ComposerDto? = DatabaseFactory.dbQuery {
        ComposersTable
            .selectAll()
            .where { ComposersTable.name eq name }
            .map { it.toComposerDto() }
            .singleOrNull()
    }

    /**
     * Find a composer by normalized name.
     */
    suspend fun findByNameNormalized(nameNormalized: String): ComposerDto? = DatabaseFactory.dbQuery {
        ComposersTable
            .selectAll()
            .where { ComposersTable.nameNormalized eq nameNormalized }
            .map { it.toComposerDto() }
            .singleOrNull()
    }

    /**
     * Find an existing composer or create a new record.
     */
    suspend fun findOrCreate(
        name: String,
        nameNormalized: String? = null,
        birthYear: Int? = null,
        deathYear: Int? = null,
        place: String? = null,
        notes: String? = null
    ): ComposerDto {
        val normalized = nameNormalized ?: normalize(name)

        // TRACK-031: Resolve known alias to canonical composer before create
        composerAliases.findComposerByAliasNormalized(normalized)?.let { return it }

        findByNameNormalized(normalized)?.let { return it }
        findByName(name)?.let { return it }

        return try {
            create(name, normalized, birthYear, deathYear, place, notes)
        } catch (e: Exception) {
            findByNameNormalized(normalized) ?: findByName(name) ?: throw e
        }
    }

    /**
     * Create a new composer record.
     */
    suspend fun create(
        name: String,
        nameNormalized: String? = null,
        birthYear: Int? = null,
        deathYear: Int? = null,
        place: String? = null,
        notes: String? = null
    ): ComposerDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val composerId = UUID.randomUUID()
        val normalized = nameNormalized ?: normalize(name)

        ComposersTable.insert {
            it[id] = composerId
            it[ComposersTable.name] = name
            it[ComposersTable.nameNormalized] = normalized
            it[ComposersTable.birthYear] = birthYear
            it[ComposersTable.deathYear] = deathYear
            it[ComposersTable.place] = place
            it[ComposersTable.notes] = notes
            it[ComposersTable.createdAt] = now
            it[ComposersTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toComposerDto()
            ?: error("Failed to insert composer")
    }

    /**
     * Update an existing composer and return the updated record.
     */
    suspend fun update(
        id: Uuid,
        name: String? = null,
        nameNormalized: String? = null,
        birthYear: Int? = null,
        deathYear: Int? = null,
        place: String? = null,
        notes: String? = null
    ): ComposerDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaId = id.toJavaUuid()
        
        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        ComposersTable
            .updateReturning(
                where = { ComposersTable.id eq javaId }
            ) {
                name?.let { value -> 
                    it[ComposersTable.name] = value
                    it[ComposersTable.nameNormalized] = nameNormalized ?: normalize(value)
                }
                nameNormalized?.let { value -> it[ComposersTable.nameNormalized] = value }
                birthYear?.let { value -> it[ComposersTable.birthYear] = value }
                deathYear?.let { value -> it[ComposersTable.deathYear] = value }
                place?.let { value -> it[ComposersTable.place] = value }
                notes?.let { value -> it[ComposersTable.notes] = value }
                it[ComposersTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toComposerDto()
    }

    /**
     * Delete a composer by ID.
     */
    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = ComposersTable.deleteWhere { ComposersTable.id eq id.toJavaUuid() }
        deleted > 0
    }

    /**
     * Count all composers.
     */
    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        ComposersTable.selectAll().count()
    }
}
