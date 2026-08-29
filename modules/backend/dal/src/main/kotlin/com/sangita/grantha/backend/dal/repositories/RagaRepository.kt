package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toRagaDto
import com.sangita.grantha.backend.dal.tables.RagaIdentityKeysTable
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
     * Space-insensitive `name_normalized` lookup.
     *
     * Import computes keys with spaces stripped (`yadukulakambhoji`) while the
     * Wikipedia seed stores spaced keys (`yadukula kambhoji`). Without this
     * probe, [findOrCreate] misses the curated row and mints an ITRANS twin
     * (TRACK-132). Prefers a row that already has lakshana when two compact
     * to the same key.
     */
    private suspend fun findByCompactNormalized(compact: String): RagaDto? {
        if (compact.isBlank()) return null
        return DatabaseFactory.dbQuery {
            val compactName = CustomFunction<String>(
                "replace",
                TextColumnType(),
                RagasTable.nameNormalized,
                stringLiteral(" "),
                stringLiteral(""),
            )
            RagasTable
                .selectAll()
                .where { compactName eq compact }
                .map { it.toRagaDto() }
                .sortedWith(
                    compareByDescending<RagaDto> { !it.arohanam.isNullOrBlank() }
                        .thenByDescending { it.parentRagaId != null }
                )
                .firstOrNull()
        }
    }

    /**
     * Unambiguous identity lookup (TRACK-136 sequencing guard).
     *
     * Looks up `raga_identity_keys` by `raga_match_key(name)`. Returns the raga
     * only on a singleton hit — homonym sets (hits > 1) are *not* auto-picked
     * (D1). Phase 2 replaces the create branch with the resolution queue.
     */
    private suspend fun findUnambiguousByIdentity(name: String): RagaDto? {
        if (name.isBlank()) return null
        val ragaIds = DatabaseFactory.dbQuery {
            val keyExpr = CustomFunction<String>(
                "raga_match_key",
                TextColumnType(),
                stringLiteral(name),
            )
            RagaIdentityKeysTable
                .select(RagaIdentityKeysTable.ragaId)
                .where { RagaIdentityKeysTable.matchKey eq keyExpr }
                .map { it[RagaIdentityKeysTable.ragaId] }
                .distinct()
        }
        if (ragaIds.size != 1) return null
        return findById(ragaIds.single().toKotlinUuid())
    }

    /**
     * Find an existing raga or create a new record.
     *
     * Identity lookup runs first so spelling twins and cited aliases resolve
     * instead of hitting Phase 1's UNIQUE. The create branch remains until
     * Phase 2's `resolve_raga`; unique violations fall back to identity lookup.
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
        findUnambiguousByIdentity(name)?.let { return it }

        val normalized = nameNormalized ?: normalize(name)
        val compact = normalized.replace(" ", "")

        findByNameNormalized(normalized)?.let { return it }
        findByName(name)?.let { return it }
        findByCompactNormalized(compact)?.let { return it }

        return try {
            create(name, normalized, melakartaNumber, parentRagaId, arohanam, avarohanam, notes)
        } catch (e: Exception) {
            findUnambiguousByIdentity(name)
                ?: findByNameNormalized(normalized)
                ?: findByName(name)
                ?: findByCompactNormalized(compact)
                ?: throw e
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
