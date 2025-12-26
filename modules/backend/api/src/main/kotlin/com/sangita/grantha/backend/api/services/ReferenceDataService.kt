package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.DeityDto
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.shared.domain.model.SampradayaDto
import com.sangita.grantha.shared.domain.model.TagDto
import com.sangita.grantha.shared.domain.model.TalaDto
import com.sangita.grantha.shared.domain.model.TempleDto

class ReferenceDataService(private val dal: SangitaDal) {
    suspend fun listComposers(): List<ComposerDto> = dal.composers.listAll()

    suspend fun listRagas(): List<RagaDto> = dal.ragas.listAll()

    suspend fun listTalas(): List<TalaDto> = dal.talas.listAll()

    suspend fun listDeities(): List<DeityDto> = dal.deities.listAll()

    suspend fun listTemples(): List<TempleDto> = dal.temples.listAll()

    suspend fun listTags(): List<TagDto> = dal.tags.listAll()

    suspend fun listSampradayas(): List<SampradayaDto> = dal.sampradayas.listAll()
}
