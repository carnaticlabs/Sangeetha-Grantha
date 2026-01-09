package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toTempleDto
import com.sangita.grantha.backend.dal.tables.TemplesTable
import com.sangita.grantha.shared.domain.model.TempleDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid

class TempleRepository {
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

    suspend fun listAll(): List<TempleDto> = DatabaseFactory.dbQuery {
        TemplesTable
            .selectAll()
            .orderBy(TemplesTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toTempleDto() }
    }

    suspend fun findById(id: Uuid): TempleDto? = DatabaseFactory.dbQuery {
        TemplesTable
            .selectAll()
            .where { TemplesTable.id eq id.toJavaUuid() }
            .map { it.toTempleDto() }
            .singleOrNull()
    }

    suspend fun findByName(name: String): TempleDto? = DatabaseFactory.dbQuery {
        TemplesTable
            .selectAll()
            .where { TemplesTable.name eq name }
            .map { it.toTempleDto() }
            .singleOrNull()
    }

    suspend fun create(
        name: String,
        nameNormalized: String? = null,
        city: String? = null,
        state: String? = null,
        country: String? = null,
        primaryDeityId: UUID? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        notes: String? = null
    ): TempleDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val templeId = UUID.randomUUID()
        val normalized = nameNormalized ?: normalize(name)

        TemplesTable.insert {
            it[id] = templeId
            it[TemplesTable.name] = name
            it[TemplesTable.nameNormalized] = normalized
            it[TemplesTable.city] = city
            it[TemplesTable.state] = state
            it[TemplesTable.country] = country
            it[TemplesTable.primaryDeityId] = primaryDeityId
            it[TemplesTable.latitude] = latitude
            it[TemplesTable.longitude] = longitude
            it[TemplesTable.notes] = notes
            it[TemplesTable.createdAt] = now
            it[TemplesTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toTempleDto()
            ?: error("Failed to insert temple")
    }

    suspend fun update(
        id: Uuid,
        name: String? = null,
        nameNormalized: String? = null,
        city: String? = null,
        state: String? = null,
        country: String? = null,
        primaryDeityId: UUID? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        notes: String? = null
    ): TempleDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaId = id.toJavaUuid()
        
        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        TemplesTable
            .updateReturning(
                where = { TemplesTable.id eq javaId }
            ) {
                name?.let { value -> 
                    it[TemplesTable.name] = value
                    it[TemplesTable.nameNormalized] = nameNormalized ?: normalize(value)
                }
                nameNormalized?.let { value -> it[TemplesTable.nameNormalized] = value }
                city?.let { value -> it[TemplesTable.city] = value }
                state?.let { value -> it[TemplesTable.state] = value }
                country?.let { value -> it[TemplesTable.country] = value }
                primaryDeityId?.let { value -> it[TemplesTable.primaryDeityId] = value }
                latitude?.let { value -> it[TemplesTable.latitude] = value }
                longitude?.let { value -> it[TemplesTable.longitude] = value }
                notes?.let { value -> it[TemplesTable.notes] = value }
                it[TemplesTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toTempleDto()
    }

    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = TemplesTable.deleteWhere { TemplesTable.id eq id.toJavaUuid() }
        deleted > 0
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        TemplesTable.selectAll().count()
    }
}
