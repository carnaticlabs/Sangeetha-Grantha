package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toDeityDto
import com.sangita.grantha.backend.dal.tables.DeitiesTable
import com.sangita.grantha.shared.domain.model.DeityDto
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll

class DeityRepository {
    suspend fun listAll(): List<DeityDto> = DatabaseFactory.dbQuery {
        DeitiesTable
            .selectAll()
            .orderBy(DeitiesTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toDeityDto() }
    }
}
