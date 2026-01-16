package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toDeityDto
import com.sangita.grantha.backend.dal.tables.DeitiesTable
import com.sangita.grantha.shared.domain.model.DeityDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid

class DeityRepository {
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

    suspend fun listAll(): List<DeityDto> = DatabaseFactory.dbQuery {
        DeitiesTable
            .selectAll()
            .orderBy(DeitiesTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toDeityDto() }
    }

    suspend fun findById(id: Uuid): DeityDto? = DatabaseFactory.dbQuery {
        DeitiesTable
            .selectAll()
            .where { DeitiesTable.id eq id.toJavaUuid() }
            .map { it.toDeityDto() }
            .singleOrNull()
    }

    suspend fun findByName(name: String): DeityDto? = DatabaseFactory.dbQuery {
        DeitiesTable
            .selectAll()
            .where { DeitiesTable.name eq name }
            .map { it.toDeityDto() }
            .singleOrNull()
    }

    suspend fun create(
        name: String,
        nameNormalized: String? = null,
        description: String? = null
    ): DeityDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val deityId = UUID.randomUUID()
        val normalized = nameNormalized ?: normalize(name)

        DeitiesTable.insert {
            it[id] = deityId
            it[DeitiesTable.name] = name
            it[DeitiesTable.nameNormalized] = normalized
            it[DeitiesTable.description] = description
            it[DeitiesTable.createdAt] = now
            it[DeitiesTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toDeityDto()
            ?: error("Failed to insert deity")
    }

    suspend fun update(
        id: Uuid,
        name: String? = null,
        nameNormalized: String? = null,
        description: String? = null
    ): DeityDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaId = id.toJavaUuid()
        
        DeitiesTable
            .updateReturning(
                where = { DeitiesTable.id eq javaId }
            ) {
                name?.let { value -> 
                    it[DeitiesTable.name] = value
                    it[DeitiesTable.nameNormalized] = nameNormalized ?: normalize(value)
                }
                nameNormalized?.let { value -> it[DeitiesTable.nameNormalized] = value }
                description?.let { value -> it[DeitiesTable.description] = value }
                it[DeitiesTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toDeityDto()
    }

    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = DeitiesTable.deleteWhere { DeitiesTable.id eq id.toJavaUuid() }
        deleted > 0
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        DeitiesTable.selectAll().count()
    }
}
