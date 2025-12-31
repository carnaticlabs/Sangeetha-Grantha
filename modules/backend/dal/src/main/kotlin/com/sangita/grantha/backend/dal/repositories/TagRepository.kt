package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toTagDto
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.shared.domain.model.TagDto
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll

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
}
