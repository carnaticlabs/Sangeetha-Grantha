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

class TagRepository {
    suspend fun listAll(): List<TagDto> = DatabaseFactory.dbQuery {
        TagsTable
            .selectAll()
            .orderBy(TagsTable.displayNameEn to SortOrder.ASC)
            .map { row: ResultRow -> row.toTagDto() }
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        TagsTable.selectAll().count()
    }

    suspend fun findById(id: Uuid): TagDto? = DatabaseFactory.dbQuery {
        TagsTable
            .selectAll()
            .where { TagsTable.id eq id.toJavaUuid() }
            .map { it.toTagDto() }
            .singleOrNull()
    }

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

        TagsTable
            .selectAll()
            .where { TagsTable.id eq tagId }
            .map { it.toTagDto() }
            .single()
    }

    suspend fun update(
        id: Uuid,
        category: TagCategoryDto? = null,
        slug: String? = null,
        displayNameEn: String? = null,
        descriptionEn: String? = null
    ): TagDto? = DatabaseFactory.dbQuery {
        val updated = TagsTable.update({ TagsTable.id eq id.toJavaUuid() }) {
            category?.let { value -> it[TagsTable.category] = value.name }
            slug?.let { value -> it[TagsTable.slug] = value }
            displayNameEn?.let { value -> it[TagsTable.displayNameEn] = value }
            descriptionEn?.let { value -> it[TagsTable.descriptionEn] = value }
        }

        if (updated == 0) {
            return@dbQuery null
        }

        TagsTable
            .selectAll()
            .where { TagsTable.id eq id.toJavaUuid() }
            .map { it.toTagDto() }
            .singleOrNull()
    }

    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = TagsTable.deleteWhere { TagsTable.id eq id.toJavaUuid() }
        deleted > 0
    }
}
