package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.*
import com.sangita.grantha.backend.api.support.toJavaUuidOrNull
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.DeityDto
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.shared.domain.model.SampradayaDto
import com.sangita.grantha.shared.domain.model.TagDto
import com.sangita.grantha.shared.domain.model.TagCategoryDto
import com.sangita.grantha.shared.domain.model.TalaDto
import com.sangita.grantha.shared.domain.model.TempleDto
import kotlin.uuid.Uuid

interface IReferenceDataService {
    /**
     * List all composers.
     */
    suspend fun listComposers(): List<ComposerDto>

    /**
     * Fetch a composer by ID.
     */
    suspend fun getComposer(id: Uuid): ComposerDto?

    /**
     * Create a composer.
     */
    suspend fun createComposer(request: ComposerCreateRequest): ComposerDto

    /**
     * Update a composer.
     */
    suspend fun updateComposer(id: Uuid, request: ComposerUpdateRequest): ComposerDto?

    /**
     * Delete a composer by ID.
     */
    suspend fun deleteComposer(id: Uuid): Boolean

    /**
     * List all ragas.
     */
    suspend fun listRagas(): List<RagaDto>

    /**
     * Fetch a raga by ID.
     */
    suspend fun getRaga(id: Uuid): RagaDto?

    /**
     * Create a raga.
     */
    suspend fun createRaga(request: RagaCreateRequest): RagaDto

    /**
     * Update a raga.
     */
    suspend fun updateRaga(id: Uuid, request: RagaUpdateRequest): RagaDto?

    /**
     * Delete a raga by ID.
     */
    suspend fun deleteRaga(id: Uuid): Boolean

    /**
     * List all talas.
     */
    suspend fun listTalas(): List<TalaDto>

    /**
     * Fetch a tala by ID.
     */
    suspend fun getTala(id: Uuid): TalaDto?

    /**
     * Create a tala.
     */
    suspend fun createTala(request: TalaCreateRequest): TalaDto

    /**
     * Update a tala.
     */
    suspend fun updateTala(id: Uuid, request: TalaUpdateRequest): TalaDto?

    /**
     * Delete a tala by ID.
     */
    suspend fun deleteTala(id: Uuid): Boolean

    /**
     * List all temples.
     */
    suspend fun listTemples(): List<TempleDto>

    /**
     * Fetch a temple by ID.
     */
    suspend fun getTemple(id: Uuid): TempleDto?

    /**
     * Create a temple.
     */
    suspend fun createTemple(request: TempleCreateRequest): TempleDto

    /**
     * Update a temple.
     */
    suspend fun updateTemple(id: Uuid, request: TempleUpdateRequest): TempleDto?

    /**
     * Delete a temple by ID.
     */
    suspend fun deleteTemple(id: Uuid): Boolean

    /**
     * List all deities.
     */
    suspend fun listDeities(): List<DeityDto>

    /**
     * Fetch a deity by ID.
     */
    suspend fun getDeity(id: Uuid): DeityDto?

    /**
     * Create a deity.
     */
    suspend fun createDeity(request: DeityCreateRequest): DeityDto

    /**
     * Update a deity.
     */
    suspend fun updateDeity(id: Uuid, request: DeityUpdateRequest): DeityDto?

    /**
     * Delete a deity by ID.
     */
    suspend fun deleteDeity(id: Uuid): Boolean

    /**
     * List all tags.
     */
    suspend fun listTags(): List<TagDto>

    /**
     * Fetch a tag by ID.
     */
    suspend fun getTag(id: Uuid): TagDto?

    /**
     * Create a tag.
     */
    suspend fun createTag(request: TagCreateRequest): TagDto

    /**
     * Update a tag.
     */
    suspend fun updateTag(id: Uuid, request: TagUpdateRequest): TagDto?

    /**
     * Delete a tag.
     */
    suspend fun deleteTag(id: Uuid): Boolean

    /**
     * List all sampradayas.
     */
    suspend fun listSampradayas(): List<SampradayaDto>

    /**
     * Fetch a sampradaya by ID.
     */
    suspend fun getSampradaya(id: Uuid): SampradayaDto?

    /**
     * Create a sampradaya.
     */
    suspend fun createSampradaya(request: SampradayaCreateRequest): SampradayaDto

    /**
     * Update a sampradaya.
     */
    suspend fun updateSampradaya(id: Uuid, request: SampradayaUpdateRequest): SampradayaDto?

    /**
     * Delete a sampradaya.
     */
    suspend fun deleteSampradaya(id: Uuid): Boolean

    /**
     * List tag categories.
     */
    suspend fun listTagCategories(): List<TagCategoryDto>

    /**
     * Fetch reference data counts for dashboards and health.
     */
    suspend fun getStats(): com.sangita.grantha.shared.domain.model.ReferenceDataStatsDto
}

class ReferenceDataServiceImpl(private val dal: SangitaDal) : IReferenceDataService {

    // Composers
    override suspend fun listComposers(): List<ComposerDto> = dal.composers.listAll()

    override suspend fun getComposer(id: Uuid): ComposerDto? = dal.composers.findById(id)

    override suspend fun createComposer(request: ComposerCreateRequest): ComposerDto {
        val created = dal.composers.create(
            name = request.name,
            nameNormalized = request.nameNormalized,
            birthYear = request.birthYear,
            deathYear = request.deathYear,
            place = request.place,
            notes = request.notes
        )
        dal.auditLogs.append(
            action = "CREATE_COMPOSER",
            entityTable = "composers",
            entityId = created.id
        )
        return created
    }

    override suspend fun updateComposer(id: Uuid, request: ComposerUpdateRequest): ComposerDto? {
        val updated = dal.composers.update(
            id = id,
            name = request.name,
            nameNormalized = request.nameNormalized,
            birthYear = request.birthYear,
            deathYear = request.deathYear,
            place = request.place,
            notes = request.notes
        )
        if (updated != null) {
            dal.auditLogs.append(
                action = "UPDATE_COMPOSER",
                entityTable = "composers",
                entityId = id
            )
        }
        return updated
    }

    override suspend fun deleteComposer(id: Uuid): Boolean {
        val deleted = dal.composers.delete(id)
        if (deleted) {
            dal.auditLogs.append(
                action = "DELETE_COMPOSER",
                entityTable = "composers",
                entityId = id
            )
        }
        return deleted
    }

    // Ragas
    override suspend fun listRagas(): List<RagaDto> = dal.ragas.listAll()

    override suspend fun getRaga(id: Uuid): RagaDto? = dal.ragas.findById(id)

    override suspend fun createRaga(request: RagaCreateRequest): RagaDto {
        val created = dal.ragas.create(
            name = request.name,
            nameNormalized = request.nameNormalized,
            melakartaNumber = request.melakartaNumber,
            parentRagaId = request.parentRagaId.toJavaUuidOrNull("parentRagaId"),
            arohanam = request.arohanam,
            avarohanam = request.avarohanam,
            notes = request.notes
        )
        dal.auditLogs.append(
            action = "CREATE_RAGA",
            entityTable = "ragas",
            entityId = created.id
        )
        return created
    }

    override suspend fun updateRaga(id: Uuid, request: RagaUpdateRequest): RagaDto? {
        val updated = dal.ragas.update(
            id = id,
            name = request.name,
            nameNormalized = request.nameNormalized,
            melakartaNumber = request.melakartaNumber,
            parentRagaId = request.parentRagaId.toJavaUuidOrNull("parentRagaId"),
            arohanam = request.arohanam,
            avarohanam = request.avarohanam,
            notes = request.notes
        )
        if (updated != null) {
            dal.auditLogs.append(
                action = "UPDATE_RAGA",
                entityTable = "ragas",
                entityId = id
            )
        }
        return updated
    }

    override suspend fun deleteRaga(id: Uuid): Boolean {
        val deleted = dal.ragas.delete(id)
        if (deleted) {
            dal.auditLogs.append(
                action = "DELETE_RAGA",
                entityTable = "ragas",
                entityId = id
            )
        }
        return deleted
    }

    // Talas
    override suspend fun listTalas(): List<TalaDto> = dal.talas.listAll()

    override suspend fun getTala(id: Uuid): TalaDto? = dal.talas.findById(id)

    override suspend fun createTala(request: TalaCreateRequest): TalaDto {
        val created = dal.talas.create(
            name = request.name,
            nameNormalized = request.nameNormalized,
            beatCount = request.beatCount,
            angaStructure = request.angaStructure,
            notes = request.notes
        )
        dal.auditLogs.append(
            action = "CREATE_TALA",
            entityTable = "talas",
            entityId = created.id
        )
        return created
    }

    override suspend fun updateTala(id: Uuid, request: TalaUpdateRequest): TalaDto? {
        val updated = dal.talas.update(
            id = id,
            name = request.name,
            nameNormalized = request.nameNormalized,
            beatCount = request.beatCount,
            angaStructure = request.angaStructure,
            notes = request.notes
        )
        if (updated != null) {
            dal.auditLogs.append(
                action = "UPDATE_TALA",
                entityTable = "talas",
                entityId = id
            )
        }
        return updated
    }

    override suspend fun deleteTala(id: Uuid): Boolean {
        val deleted = dal.talas.delete(id)
        if (deleted) {
            dal.auditLogs.append(
                action = "DELETE_TALA",
                entityTable = "talas",
                entityId = id
            )
        }
        return deleted
    }

    // Temples
    override suspend fun listTemples(): List<TempleDto> = dal.temples.listAll()

    override suspend fun getTemple(id: Uuid): TempleDto? = dal.temples.findById(id)

    override suspend fun createTemple(request: TempleCreateRequest): TempleDto {
        val created = dal.temples.create(
            name = request.name,
            nameNormalized = request.nameNormalized,
            city = request.city,
            state = request.state,
            country = request.country,
            primaryDeityId = request.primaryDeityId.toJavaUuidOrNull("primaryDeityId"),
            latitude = request.latitude,
            longitude = request.longitude,
            notes = request.notes
        )
        dal.auditLogs.append(
            action = "CREATE_TEMPLE",
            entityTable = "temples",
            entityId = created.id
        )
        return created
    }

    override suspend fun updateTemple(id: Uuid, request: TempleUpdateRequest): TempleDto? {
        val updated = dal.temples.update(
            id = id,
            name = request.name,
            nameNormalized = request.nameNormalized,
            city = request.city,
            state = request.state,
            country = request.country,
            primaryDeityId = request.primaryDeityId.toJavaUuidOrNull("primaryDeityId"),
            latitude = request.latitude,
            longitude = request.longitude,
            notes = request.notes
        )
        if (updated != null) {
            dal.auditLogs.append(
                action = "UPDATE_TEMPLE",
                entityTable = "temples",
                entityId = id
            )
        }
        return updated
    }

    override suspend fun deleteTemple(id: Uuid): Boolean {
        val deleted = dal.temples.delete(id)
        if (deleted) {
            dal.auditLogs.append(
                action = "DELETE_TEMPLE",
                entityTable = "temples",
                entityId = id
            )
        }
        return deleted
    }

    // Deities
    override suspend fun listDeities(): List<DeityDto> = dal.deities.listAll()

    override suspend fun getDeity(id: Uuid): DeityDto? = dal.deities.findById(id)

    override suspend fun createDeity(request: DeityCreateRequest): DeityDto {
        val created = dal.deities.create(
            name = request.name,
            nameNormalized = request.nameNormalized,
            description = request.description
        )
        dal.auditLogs.append(
            action = "CREATE_DEITY",
            entityTable = "deities",
            entityId = created.id
        )
        return created
    }

    override suspend fun updateDeity(id: Uuid, request: DeityUpdateRequest): DeityDto? {
        val updated = dal.deities.update(
            id = id,
            name = request.name,
            nameNormalized = request.nameNormalized,
            description = request.description
        )
        if (updated != null) {
            dal.auditLogs.append(
                action = "UPDATE_DEITY",
                entityTable = "deities",
                entityId = id
            )
        }
        return updated
    }

    override suspend fun deleteDeity(id: Uuid): Boolean {
        val deleted = dal.deities.delete(id)
        if (deleted) {
            dal.auditLogs.append(
                action = "DELETE_DEITY",
                entityTable = "deities",
                entityId = id
            )
        }
        return deleted
    }

    // Sampradayas

    override suspend fun listSampradayas(): List<SampradayaDto> = dal.sampradayas.listAll()

    override suspend fun getSampradaya(id: Uuid): SampradayaDto? = dal.sampradayas.findById(id)

    override suspend fun createSampradaya(request: SampradayaCreateRequest): SampradayaDto {
        val created = dal.sampradayas.create(
            name = request.name,
            nameNormalized = request.nameNormalized,
            description = request.description
        )
        dal.auditLogs.append(
            action = "CREATE_SAMPRADAYA",
            entityTable = "sampradayas",
            entityId = created.id
        )
        return created
    }

    override suspend fun updateSampradaya(id: Uuid, request: SampradayaUpdateRequest): SampradayaDto? {
        val updated = dal.sampradayas.update(
            id = id,
            name = request.name,
            nameNormalized = request.nameNormalized,
            description = request.description
        )
        if (updated != null) {
            dal.auditLogs.append(
                action = "UPDATE_SAMPRADAYA",
                entityTable = "sampradayas",
                entityId = id
            )
        }
        return updated
    }

    override suspend fun deleteSampradaya(id: Uuid): Boolean {
        val deleted = dal.sampradayas.delete(id)
        if (deleted) {
            dal.auditLogs.append(
                action = "DELETE_SAMPRADAYA",
                entityTable = "sampradayas",
                entityId = id
            )
        }
        return deleted
    }

    // Other reference data

    override suspend fun listTags(): List<TagDto> = dal.tags.listAll()

    override suspend fun listTagCategories(): List<TagCategoryDto> =
        TagCategoryDto.entries


    override suspend fun getTag(id: Uuid): TagDto? = dal.tags.findById(id)

    override suspend fun createTag(request: TagCreateRequest): TagDto {
        val created = dal.tags.create(request.category, request.slug, request.displayNameEn, request.descriptionEn)
        dal.auditLogs.append(
            action = "CREATE_TAG",
            entityTable = "tags",
            entityId = created.id
        )
        return created
    }

    override suspend fun updateTag(id: Uuid, request: TagUpdateRequest): TagDto? {
        val updated = dal.tags.update(id, request.category, request.slug, request.displayNameEn, request.descriptionEn)
        if (updated != null) {
            dal.auditLogs.append(
                action = "UPDATE_TAG",
                entityTable = "tags",
                entityId = id
            )
        }
        return updated
    }

    override suspend fun deleteTag(id: Uuid): Boolean {
        val deleted = dal.tags.delete(id)
        if (deleted) {
            dal.auditLogs.append(
                action = "DELETE_TAG",
                entityTable = "tags",
                entityId = id
            )
        }
        return deleted
    }

    override suspend fun getStats(): com.sangita.grantha.shared.domain.model.ReferenceDataStatsDto {
        return com.sangita.grantha.shared.domain.model.ReferenceDataStatsDto(
            composerCount = dal.composers.countAll(),
            ragaCount = dal.ragas.countAll(),
            talaCount = dal.talas.countAll(),
            deityCount = dal.deities.countAll(),
            templeCount = dal.temples.countAll(),
            tagCount = dal.tags.countAll(),
            sampradayaCount = dal.sampradayas.countAll()
        )
    }
}
