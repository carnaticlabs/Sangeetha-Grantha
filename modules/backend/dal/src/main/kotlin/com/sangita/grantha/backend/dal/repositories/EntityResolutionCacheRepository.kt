package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toEntityResolutionCacheDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.EntityResolutionCacheTable
import com.sangita.grantha.shared.domain.model.EntityResolutionCacheDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.*

/**
 * TRACK-013: Entity resolution cache repository.
 */
class EntityResolutionCacheRepository {

    /**
     * Find cached resolution by entity type and normalized name
     */
    suspend fun findByNormalizedName(
        entityType: String,
        normalizedName: String
    ): EntityResolutionCacheDto? = DatabaseFactory.dbQuery {
        EntityResolutionCacheTable
            .selectAll()
            .andWhere {
                (EntityResolutionCacheTable.entityType eq entityType) and
                (EntityResolutionCacheTable.normalizedName eq normalizedName)
            }
            .singleOrNull()
            ?.toEntityResolutionCacheDto()
    }

    /**
     * Save or update cache entry (upsert on conflict)
     */
    suspend fun save(
        entityType: String,
        rawName: String,
        normalizedName: String,
        resolvedEntityId: Uuid,
        confidence: Int
    ): UUID = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UUID.randomUUID()

        try {
            EntityResolutionCacheTable.insert { stmt ->
                stmt[EntityResolutionCacheTable.id] = id
                stmt[EntityResolutionCacheTable.entityType] = entityType
                stmt[EntityResolutionCacheTable.rawName] = rawName
                stmt[EntityResolutionCacheTable.normalizedName] = normalizedName
                stmt[EntityResolutionCacheTable.resolvedEntityId] = resolvedEntityId.toJavaUuid()
                stmt[EntityResolutionCacheTable.confidence] = confidence
                stmt[EntityResolutionCacheTable.createdAt] = now
                stmt[EntityResolutionCacheTable.updatedAt] = now
            }
            id
        } catch (e: ExposedSQLException) {
            if (!isTypeAndNameUniqueViolation(e)) throw e

            EntityResolutionCacheTable
                .updateReturning(
                    where = {
                        (EntityResolutionCacheTable.entityType eq entityType) and
                            (EntityResolutionCacheTable.normalizedName eq normalizedName)
                    }
                ) { stmt ->
                    stmt[EntityResolutionCacheTable.rawName] = rawName
                    stmt[EntityResolutionCacheTable.resolvedEntityId] = resolvedEntityId.toJavaUuid()
                    stmt[EntityResolutionCacheTable.confidence] = confidence
                    stmt[EntityResolutionCacheTable.updatedAt] = now
                }
                .singleOrNull()
                ?.get(EntityResolutionCacheTable.id)
                ?.value
                ?: error(
                    "Failed to update existing entity_resolution_cache row for " +
                        "entityType=$entityType normalizedName=$normalizedName"
                )
        }
    }

    private fun isTypeAndNameUniqueViolation(e: ExposedSQLException): Boolean {
        val sqlState =
            generateSequence<Throwable>(e) { it.cause }
                .filterIsInstance<java.sql.SQLException>()
                .mapNotNull { it.sqlState }
                .firstOrNull()

        val combinedMessage =
            buildString {
                append(e.message.orEmpty())
                append(' ')
                append(e.cause?.message.orEmpty())
            }

        return sqlState == "23505" &&
            combinedMessage.contains(
                "entity_resolution_cache_entity_type_normalized_name_key",
                ignoreCase = true
            )
    }

    /**
     * Delete cache entries for a specific resolved entity ID
     * Used for cache invalidation when entities are updated/deleted
     */
    suspend fun deleteByEntityId(entityType: String, entityId: Uuid): Int =
        DatabaseFactory.dbQuery {
            EntityResolutionCacheTable
                .deleteWhere {
                    (EntityResolutionCacheTable.entityType eq entityType) and
                    (EntityResolutionCacheTable.resolvedEntityId eq entityId.toJavaUuid())
                }
        }

    /**
     * Clear all cache entries for a specific entity type
     */
    suspend fun clearByType(entityType: String): Int = DatabaseFactory.dbQuery {
        EntityResolutionCacheTable
            .deleteWhere { EntityResolutionCacheTable.entityType eq entityType }
    }

    /**
     * Clear all cache entries (use sparingly, mainly for testing)
     */
    suspend fun clearAll(): Int = DatabaseFactory.dbQuery {
        EntityResolutionCacheTable.deleteAll()
    }
}
