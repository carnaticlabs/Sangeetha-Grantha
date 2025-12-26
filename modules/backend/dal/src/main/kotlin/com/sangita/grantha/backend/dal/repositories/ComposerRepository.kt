package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toComposerDto
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.shared.domain.model.ComposerDto
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll

class ComposerRepository {
    suspend fun listAll(): List<ComposerDto> = DatabaseFactory.dbQuery {
        ComposersTable
            .selectAll()
            .orderBy(ComposersTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toComposerDto() }
    }
}
