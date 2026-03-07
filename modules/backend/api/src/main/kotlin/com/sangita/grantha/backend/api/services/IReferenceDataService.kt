package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.*
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.DeityDto
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.shared.domain.model.ReferenceDataStatsDto
import com.sangita.grantha.shared.domain.model.SampradayaDto
import com.sangita.grantha.shared.domain.model.TagDto
import com.sangita.grantha.shared.domain.model.TagCategoryDto
import com.sangita.grantha.shared.domain.model.TalaDto
import com.sangita.grantha.shared.domain.model.TempleDto
import kotlin.uuid.Uuid

interface IReferenceDataService {
    suspend fun listComposers(): List<ComposerDto>
    suspend fun getComposer(id: Uuid): ComposerDto?
    suspend fun createComposer(request: ComposerCreateRequest): ComposerDto
    suspend fun updateComposer(id: Uuid, request: ComposerUpdateRequest): ComposerDto?
    suspend fun deleteComposer(id: Uuid): Boolean

    suspend fun listRagas(): List<RagaDto>
    suspend fun getRaga(id: Uuid): RagaDto?
    suspend fun createRaga(request: RagaCreateRequest): RagaDto
    suspend fun updateRaga(id: Uuid, request: RagaUpdateRequest): RagaDto?
    suspend fun deleteRaga(id: Uuid): Boolean

    suspend fun listTalas(): List<TalaDto>
    suspend fun getTala(id: Uuid): TalaDto?
    suspend fun createTala(request: TalaCreateRequest): TalaDto
    suspend fun updateTala(id: Uuid, request: TalaUpdateRequest): TalaDto?
    suspend fun deleteTala(id: Uuid): Boolean

    suspend fun listTemples(): List<TempleDto>
    suspend fun getTemple(id: Uuid): TempleDto?
    suspend fun createTemple(request: TempleCreateRequest): TempleDto
    suspend fun updateTemple(id: Uuid, request: TempleUpdateRequest): TempleDto?
    suspend fun deleteTemple(id: Uuid): Boolean

    suspend fun listDeities(): List<DeityDto>
    suspend fun getDeity(id: Uuid): DeityDto?
    suspend fun createDeity(request: DeityCreateRequest): DeityDto
    suspend fun updateDeity(id: Uuid, request: DeityUpdateRequest): DeityDto?
    suspend fun deleteDeity(id: Uuid): Boolean

    suspend fun listTags(): List<TagDto>
    suspend fun getTag(id: Uuid): TagDto?
    suspend fun createTag(request: TagCreateRequest): TagDto
    suspend fun updateTag(id: Uuid, request: TagUpdateRequest): TagDto?
    suspend fun deleteTag(id: Uuid): Boolean

    suspend fun listSampradayas(): List<SampradayaDto>
    suspend fun getSampradaya(id: Uuid): SampradayaDto?
    suspend fun createSampradaya(request: SampradayaCreateRequest): SampradayaDto
    suspend fun updateSampradaya(id: Uuid, request: SampradayaUpdateRequest): SampradayaDto?
    suspend fun deleteSampradaya(id: Uuid): Boolean

    suspend fun listTagCategories(): List<TagCategoryDto>
    suspend fun getStats(): ReferenceDataStatsDto
}
