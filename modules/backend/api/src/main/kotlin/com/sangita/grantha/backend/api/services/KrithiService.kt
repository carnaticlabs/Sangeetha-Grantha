package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.KrithiCreateRequest
import com.sangita.grantha.backend.api.models.KrithiUpdateRequest
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiSearchFilters
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiSearchRequest
import com.sangita.grantha.shared.domain.model.KrithiSearchResult
import com.sangita.grantha.shared.domain.model.KrithiSectionDto
import com.sangita.grantha.shared.domain.model.KrithiLyricVariantWithSectionsDto
import com.sangita.grantha.shared.domain.model.TagDto
import java.util.UUID
import kotlin.uuid.Uuid

class KrithiService(private val dal: SangitaDal) {
    suspend fun search(request: KrithiSearchRequest, publishedOnly: Boolean = true): KrithiSearchResult {
        val filters = KrithiSearchFilters(
            query = request.query,
            composerId = parseUuid(request.composerId, "composerId"),
            ragaId = parseUuid(request.ragaId, "ragaId"),
            talaId = parseUuid(request.talaId, "talaId"),
            deityId = parseUuid(request.deityId, "deityId"),
            templeId = parseUuid(request.templeId, "templeId"),
            lyric = request.lyric,
            primaryLanguage = request.language?.let { LanguageCode.valueOf(it.name) }
        )
        return dal.krithis.search(filters, request.page, request.pageSize, publishedOnly = publishedOnly)
    }

    suspend fun getKrithi(id: Uuid): KrithiDto? = dal.krithis.findById(id)

    suspend fun createKrithi(request: KrithiCreateRequest): KrithiDto {
        val composerId = parseUuidOrThrow(request.composerId, "composerId")
        val talaId = request.talaId?.let { parseUuidOrThrow(it, "talaId") }
        val primaryRagaId = request.primaryRagaId?.let { parseUuidOrThrow(it, "primaryRagaId") }
        val deityId = request.deityId?.let { parseUuidOrThrow(it, "deityId") }
        val templeId = request.templeId?.let { parseUuidOrThrow(it, "templeId") }
        val ragaIds = request.ragaIds.map { parseUuidOrThrow(it, "ragaId") }
        val normalizedTitle = normalize(request.title)
        val normalizedIncipit = request.incipit?.let { normalize(it) }
        val isRagamalika = request.isRagamalika || ragaIds.size > 1

        val created = dal.krithis.create(
            title = request.title,
            titleNormalized = normalizedTitle,
            incipit = request.incipit,
            incipitNormalized = normalizedIncipit,
            composerId = composerId,
            musicalForm = MusicalForm.valueOf(request.musicalForm.name),
            primaryLanguage = LanguageCode.valueOf(request.primaryLanguage.name),
            primaryRagaId = primaryRagaId ?: ragaIds.firstOrNull(),
            talaId = talaId,
            deityId = deityId,
            templeId = templeId,
            isRagamalika = isRagamalika,
            ragaIds = ragaIds,
            workflowState = WorkflowState.DRAFT,
            sahityaSummary = request.sahityaSummary,
            notes = request.notes
        )

        dal.auditLogs.append(
            action = "CREATE_KRITHI",
            entityTable = "krithis",
            entityId = created.id
        )

        return created
    }

    suspend fun updateKrithi(id: Uuid, request: KrithiUpdateRequest): KrithiDto {
        val composerId = request.composerId?.let { parseUuidOrThrow(it, "composerId") }
        val talaId = request.talaId?.let { parseUuidOrThrow(it, "talaId") }
        val primaryRagaId = request.primaryRagaId?.let { parseUuidOrThrow(it, "primaryRagaId") }
        val deityId = request.deityId?.let { parseUuidOrThrow(it, "deityId") }
        val templeId = request.templeId?.let { parseUuidOrThrow(it, "templeId") }
        val ragaIds = request.ragaIds?.map { parseUuidOrThrow(it, "ragaId") }
        val normalizedTitle = request.title?.let { normalize(it) }
        val normalizedIncipit = request.incipit?.let { normalize(it) }

        val updated = dal.krithis.update(
            id = id,
            title = request.title,
            titleNormalized = normalizedTitle,
            incipit = request.incipit,
            incipitNormalized = normalizedIncipit,
            composerId = composerId,
            musicalForm = request.musicalForm?.let { MusicalForm.valueOf(it.name) },
            primaryLanguage = request.primaryLanguage?.let { LanguageCode.valueOf(it.name) },
            primaryRagaId = primaryRagaId,
            talaId = talaId,
            deityId = deityId,
            templeId = templeId,
            isRagamalika = request.isRagamalika,
            ragaIds = ragaIds,
            workflowState = request.workflowState?.let { WorkflowState.valueOf(it.name) },
            sahityaSummary = request.sahityaSummary,
            notes = request.notes
        ) ?: throw NoSuchElementException("Krithi not found")

        // Update tags if provided
        request.tagIds?.let { tagIds ->
            val tagUuids = tagIds.map { parseUuidOrThrow(it, "tagId") }
            dal.krithis.updateTags(id, tagUuids)
        }

        dal.auditLogs.append(
            action = "UPDATE_KRITHI",
            entityTable = "krithis",
            entityId = updated.id
        )

        return updated
    }

    suspend fun getKrithiSections(id: Uuid): List<KrithiSectionDto> = dal.krithis.getSections(id)

    suspend fun getKrithiLyricVariants(id: Uuid): List<KrithiLyricVariantWithSectionsDto> = dal.krithis.getLyricVariants(id)

    suspend fun getKrithiTags(id: Uuid): List<TagDto> = dal.krithis.getTags(id)

    suspend fun saveKrithiSections(id: Uuid, sections: List<com.sangita.grantha.backend.api.models.KrithiSectionRequest>) {
        val sectionsData = sections.map { it.sectionType to it.orderIndex }
        dal.krithis.saveSections(id, sectionsData)
        
        dal.auditLogs.append(
            action = "UPDATE_KRITHI_SECTIONS",
            entityTable = "krithi_sections",
            entityId = id
        )
    }

    private fun parseUuid(value: String?, label: String): UUID? {
        if (value.isNullOrBlank()) return null
        return try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid $label")
        }
    }

    private fun parseUuidOrThrow(value: String, label: String): UUID =
        try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid $label")
        }

    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
}
