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

        // Exposed 1.0.0-rc-2 doesn't reliably expose an insert-on-conflict API here; do a safe
        // "select then update/insert" instead (unique constraint enforced in DB).
        val existingId =
            EntityResolutionCacheTable
                .selectAll()
                .andWhere {
                    (EntityResolutionCacheTable.entityType eq entityType) and
                        (EntityResolutionCacheTable.normalizedName eq normalizedName)
                }
                .limit(1)
                .singleOrNull()
                ?.get(EntityResolutionCacheTable.id)
                ?.value

        if (existingId != null) {
            EntityResolutionCacheTable.update(
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
            existingId
        } else {
            val id = UUID.randomUUID()
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
        }
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
