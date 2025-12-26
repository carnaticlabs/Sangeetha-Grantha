package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toSampradayaDto
import com.sangita.grantha.backend.dal.tables.SampradayasTable
import com.sangita.grantha.shared.domain.model.SampradayaDto
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll

class SampradayaRepository {
    suspend fun listAll(): List<SampradayaDto> = DatabaseFactory.dbQuery {
        SampradayasTable
            .selectAll()
            .orderBy(SampradayasTable.name to SortOrder.ASC)
            .map { row: ResultRow -> row.toSampradayaDto() }
    }
}
