package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toTagDto
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.shared.domain.model.TagDto
import com.sangita.grantha.shared.domain.model.TagCategoryDto
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * Repository for tag reference data.
 */
class TagRepository {
    /**
     * List all tags ordered by display name.
     */
    suspend fun listAll(): List<TagDto> = DatabaseFactory.dbQuery {
        TagsTable
            .selectAll()
            .orderBy(TagsTable.displayNameEn to SortOrder.ASC)
            .map { row: ResultRow -> row.toTagDto() }
    }

    /**
     * Count all tags.
     */
    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        TagsTable.selectAll().count()
    }

    /**
     * Find a tag by ID.
     */
    suspend fun findById(id: Uuid): TagDto? = DatabaseFactory.dbQuery {
        TagsTable
            .selectAll()
            .where { TagsTable.id eq id.toJavaUuid() }
            .map { it.toTagDto() }
            .singleOrNull()
    }

    /**
     * Create a new tag.
     */
    suspend fun create(
        category: TagCategoryDto,
        slug: String,
        displayNameEn: String,
        descriptionEn: String? = null
    ): TagDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val tagId = UUID.randomUUID()

        TagsTable.insert {
            it[TagsTable.id] = tagId
            it[TagsTable.category] = category.name
            it[TagsTable.slug] = slug
            it[TagsTable.displayNameEn] = displayNameEn
            it[TagsTable.descriptionEn] = descriptionEn
            it[TagsTable.createdAt] = now
        }
            .resultedValues
            ?.single()
            ?.toTagDto()
            ?: error("Failed to insert tag")
    }

    /**
     * Update a tag and return the updated record.
     */
    suspend fun update(
        id: Uuid,
        category: TagCategoryDto? = null,
        slug: String? = null,
        displayNameEn: String? = null,
        descriptionEn: String? = null
    ): TagDto? = DatabaseFactory.dbQuery {
        val javaId = id.toJavaUuid()
        
        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        TagsTable
            .updateReturning(
                where = { TagsTable.id eq javaId }
            ) {
                category?.let { value -> it[TagsTable.category] = value.name }
                slug?.let { value -> it[TagsTable.slug] = value }
                displayNameEn?.let { value -> it[TagsTable.displayNameEn] = value }
                descriptionEn?.let { value -> it[TagsTable.descriptionEn] = value }
            }
            .singleOrNull()
            ?.toTagDto()
    }

    /**
     * Delete a tag by ID.
     */
    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = TagsTable.deleteWhere { TagsTable.id eq id.toJavaUuid() }
        deleted > 0
    }
}
