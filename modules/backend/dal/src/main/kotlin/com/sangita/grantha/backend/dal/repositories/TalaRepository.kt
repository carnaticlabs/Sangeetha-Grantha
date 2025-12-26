package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toTalaDto
import com.sangita.grantha.backend.dal.tables.TalasTable
import com.sangita.grantha.shared.domain.model.TalaDto
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll

class TalaRepository {
    suspend fun listAll(): List<TalaDto> = DatabaseFactory.dbQuery {
        TalasTable
            .selectAll()
            .orderBy(TalasTable.nameNormalized to SortOrder.ASC)
            .map { row: ResultRow -> row.toTalaDto() }
    }
}
