package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.*
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.DeityDto
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.shared.domain.model.SampradayaDto
import com.sangita.grantha.shared.domain.model.TagDto
import com.sangita.grantha.shared.domain.model.TagCategoryDto
import com.sangita.grantha.shared.domain.model.TalaDto
import com.sangita.grantha.shared.domain.model.TempleDto
import java.util.UUID
import kotlin.uuid.Uuid

class ReferenceDataService(private val dal: SangitaDal) {
    private fun parseUuidOrThrow(value: String?, label: String): UUID? {
        if (value == null) return null
        return try {
            UUID.fromString(value)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid $label: $value")
        }
    }

    // Composers
    suspend fun listComposers(): List<ComposerDto> = dal.composers.listAll()

    suspend fun getComposer(id: Uuid): ComposerDto? = dal.composers.findById(id)

    suspend fun createComposer(request: ComposerCreateRequest): ComposerDto {
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

    suspend fun updateComposer(id: Uuid, request: ComposerUpdateRequest): ComposerDto? {
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

    suspend fun deleteComposer(id: Uuid): Boolean {
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
    suspend fun listRagas(): List<RagaDto> = dal.ragas.listAll()

    suspend fun getRaga(id: Uuid): RagaDto? = dal.ragas.findById(id)

    suspend fun createRaga(request: RagaCreateRequest): RagaDto {
        val created = dal.ragas.create(
            name = request.name,
            nameNormalized = request.nameNormalized,
            melakartaNumber = request.melakartaNumber,
            parentRagaId = parseUuidOrThrow(request.parentRagaId, "parentRagaId"),
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

    suspend fun updateRaga(id: Uuid, request: RagaUpdateRequest): RagaDto? {
        val updated = dal.ragas.update(
            id = id,
            name = request.name,
            nameNormalized = request.nameNormalized,
            melakartaNumber = request.melakartaNumber,
            parentRagaId = parseUuidOrThrow(request.parentRagaId, "parentRagaId"),
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

    suspend fun deleteRaga(id: Uuid): Boolean {
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
    suspend fun listTalas(): List<TalaDto> = dal.talas.listAll()

    suspend fun getTala(id: Uuid): TalaDto? = dal.talas.findById(id)

    suspend fun createTala(request: TalaCreateRequest): TalaDto {
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

    suspend fun updateTala(id: Uuid, request: TalaUpdateRequest): TalaDto? {
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

    suspend fun deleteTala(id: Uuid): Boolean {
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
    suspend fun listTemples(): List<TempleDto> = dal.temples.listAll()

    suspend fun getTemple(id: Uuid): TempleDto? = dal.temples.findById(id)

    suspend fun createTemple(request: TempleCreateRequest): TempleDto {
        val created = dal.temples.create(
            name = request.name,
            nameNormalized = request.nameNormalized,
            city = request.city,
            state = request.state,
            country = request.country,
            primaryDeityId = parseUuidOrThrow(request.primaryDeityId, "primaryDeityId"),
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

    suspend fun updateTemple(id: Uuid, request: TempleUpdateRequest): TempleDto? {
        val updated = dal.temples.update(
            id = id,
            name = request.name,
            nameNormalized = request.nameNormalized,
            city = request.city,
            state = request.state,
            country = request.country,
            primaryDeityId = parseUuidOrThrow(request.primaryDeityId, "primaryDeityId"),
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

    suspend fun deleteTemple(id: Uuid): Boolean {
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

    // Other reference data
    suspend fun listDeities(): List<DeityDto> = dal.deities.listAll()

    suspend fun listTags(): List<TagDto> = dal.tags.listAll()

    suspend fun listSampradayas(): List<SampradayaDto> = dal.sampradayas.listAll()

    suspend fun getTag(id: Uuid): TagDto? = dal.tags.findById(id)

    suspend fun createTag(
        category: TagCategoryDto,
        slug: String,
        displayNameEn: String,
        descriptionEn: String? = null
    ): TagDto {
        val created = dal.tags.create(category, slug, displayNameEn, descriptionEn)
        dal.auditLogs.append(
            action = "CREATE_TAG",
            entityTable = "tags",
            entityId = created.id
        )
        return created
    }

    suspend fun updateTag(
        id: Uuid,
        category: TagCategoryDto? = null,
        slug: String? = null,
        displayNameEn: String? = null,
        descriptionEn: String? = null
    ): TagDto? {
        val updated = dal.tags.update(id, category, slug, displayNameEn, descriptionEn)
        if (updated != null) {
            dal.auditLogs.append(
                action = "UPDATE_TAG",
                entityTable = "tags",
                entityId = id
            )
        }
        return updated
    }

    suspend fun deleteTag(id: Uuid): Boolean {
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

    suspend fun getStats(): com.sangita.grantha.shared.domain.model.ReferenceDataStatsDto {
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
