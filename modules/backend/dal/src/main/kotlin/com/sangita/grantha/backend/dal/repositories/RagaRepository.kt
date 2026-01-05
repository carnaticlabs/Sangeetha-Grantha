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

class RagaRepository {
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

    suspend fun listAll(): List<RagaDto> = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .orderBy(RagasTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toRagaDto() }
    }

    suspend fun findById(id: Uuid): RagaDto? = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .where { RagasTable.id eq id.toJavaUuid() }
            .map { it.toRagaDto() }
            .singleOrNull()
    }

    suspend fun findByName(name: String): RagaDto? = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .where { RagasTable.name eq name }
            .map { it.toRagaDto() }
            .singleOrNull()
    }

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

        RagasTable
            .selectAll()
            .where { RagasTable.id eq ragaId }
            .map { it.toRagaDto() }
            .single()
    }

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
        val updated = RagasTable.update({ RagasTable.id eq id.toJavaUuid() }) {
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

        if (updated == 0) {
            return@dbQuery null
        }

        RagasTable
            .selectAll()
            .where { RagasTable.id eq id.toJavaUuid() }
            .map { it.toRagaDto() }
            .singleOrNull()
    }

    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = RagasTable.deleteWhere { RagasTable.id eq id.toJavaUuid() }
        deleted > 0
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        RagasTable.selectAll().count()
    }
}
