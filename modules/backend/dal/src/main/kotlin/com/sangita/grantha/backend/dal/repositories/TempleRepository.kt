package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toTempleDto
import com.sangita.grantha.backend.dal.tables.TemplesTable
import com.sangita.grantha.shared.domain.model.TempleDto
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll

class TempleRepository {
    suspend fun listAll(): List<TempleDto> = DatabaseFactory.dbQuery {
        TemplesTable
            .selectAll()
            .orderBy(TemplesTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toTempleDto() }
    }
}
