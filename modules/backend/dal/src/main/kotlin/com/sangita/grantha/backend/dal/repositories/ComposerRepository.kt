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

class ComposerRepository {
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

    suspend fun listAll(): List<ComposerDto> = DatabaseFactory.dbQuery {
        ComposersTable
            .selectAll()
            .orderBy(ComposersTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toComposerDto() }
    }

    suspend fun findById(id: Uuid): ComposerDto? = DatabaseFactory.dbQuery {
        ComposersTable
            .selectAll()
            .where { ComposersTable.id eq id.toJavaUuid() }
            .map { it.toComposerDto() }
            .singleOrNull()
    }

    suspend fun findByName(name: String): ComposerDto? = DatabaseFactory.dbQuery {
        ComposersTable
            .selectAll()
            .where { ComposersTable.name eq name }
            .map { it.toComposerDto() }
            .singleOrNull()
    }

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

        ComposersTable
            .selectAll()
            .where { ComposersTable.id eq composerId }
            .map { it.toComposerDto() }
            .single()
    }

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
        val updated = ComposersTable.update({ ComposersTable.id eq id.toJavaUuid() }) {
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

        if (updated == 0) {
            return@dbQuery null
        }

        ComposersTable
            .selectAll()
            .where { ComposersTable.id eq id.toJavaUuid() }
            .map { it.toComposerDto() }
            .singleOrNull()
    }

    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = ComposersTable.deleteWhere { ComposersTable.id eq id.toJavaUuid() }
        deleted > 0
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        ComposersTable.selectAll().count()
    }
}
