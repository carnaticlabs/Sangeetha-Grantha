package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.KrithiCreateRequest
import com.sangita.grantha.backend.api.models.KrithiUpdateRequest
import com.sangita.grantha.backend.api.models.LyricVariantCreateRequest
import com.sangita.grantha.backend.api.models.LyricVariantUpdateRequest
import com.sangita.grantha.backend.api.models.LyricVariantSectionRequest
import com.sangita.grantha.backend.api.support.toJavaUuidOrNull
import com.sangita.grantha.backend.api.support.toJavaUuidOrThrow
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.repositories.KrithiSearchFilters
import com.sangita.grantha.backend.dal.repositories.KrithiUpdateParams
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiSearchRequest
import com.sangita.grantha.shared.domain.model.KrithiSearchResult
import com.sangita.grantha.shared.domain.model.KrithiSectionDto
import com.sangita.grantha.shared.domain.model.KrithiLyricVariantDto
import com.sangita.grantha.shared.domain.model.KrithiLyricVariantWithSectionsDto
import com.sangita.grantha.shared.domain.model.TagDto
import kotlin.uuid.Uuid

interface IKrithiService {
    /**
     * Search krithis using filters and pagination, optionally restricting to published items.
     */
    suspend fun search(request: KrithiSearchRequest, publishedOnly: Boolean = true): KrithiSearchResult

    /**
     * Fetch a single krithi by its ID.
     */
    suspend fun getKrithi(id: Uuid): KrithiDto?

    /**
     * Create a new krithi in DRAFT state.
     *
     * @param userId Optional user ID for audit attribution.
     */
    suspend fun createKrithi(request: KrithiCreateRequest, userId: Uuid? = null): KrithiDto

    /**
     * Update an existing krithi and return the updated record.
     *
     * @param userId Optional user ID for audit attribution.
     */
    suspend fun updateKrithi(id: Uuid, request: KrithiUpdateRequest, userId: Uuid? = null): KrithiDto

    /**
     * Fetch the section structure for a krithi.
     */
    suspend fun getKrithiSections(id: Uuid): List<KrithiSectionDto>

    /**
     * Fetch lyric variants (with sections) for a krithi.
     */
    suspend fun getKrithiLyricVariants(id: Uuid): List<KrithiLyricVariantWithSectionsDto>

    /**
     * Fetch tags attached to a krithi.
     */
    suspend fun getKrithiTags(id: Uuid): List<TagDto>

    /**
     * Replace/update the krithi section list.
     */
    suspend fun saveKrithiSections(id: Uuid, sections: List<com.sangita.grantha.backend.api.models.KrithiSectionRequest>)

    /**
     * Create a lyric variant for a krithi with optional user attribution.
     */
    suspend fun createLyricVariant(
        krithiId: Uuid,
        request: LyricVariantCreateRequest,
        userId: Uuid? = null
    ): KrithiLyricVariantDto

    /**
     * Update a lyric variant with optional user attribution.
     */
    suspend fun updateLyricVariant(
        variantId: Uuid,
        request: LyricVariantUpdateRequest,
        userId: Uuid? = null
    ): KrithiLyricVariantDto

    /**
     * Replace/update the section text for a lyric variant.
     */
    suspend fun saveLyricVariantSections(
        variantId: Uuid,
        sections: List<LyricVariantSectionRequest>
    )
}

class KrithiServiceImpl(private val dal: SangitaDal) : IKrithiService {
    override suspend fun search(request: KrithiSearchRequest, publishedOnly: Boolean): KrithiSearchResult {
        val filters = KrithiSearchFilters(
            query = request.query,
            composerId = request.composerId.toJavaUuidOrNull("composerId"),
            ragaId = request.ragaId.toJavaUuidOrNull("ragaId"),
            talaId = request.talaId.toJavaUuidOrNull("talaId"),
            deityId = request.deityId.toJavaUuidOrNull("deityId"),
            templeId = request.templeId.toJavaUuidOrNull("templeId"),
            lyric = request.lyric,
            primaryLanguage = request.language?.let { LanguageCode.valueOf(it.name) }
        )
        return dal.krithis.search(filters, request.page, request.pageSize, publishedOnly = publishedOnly)
    }

    override suspend fun getKrithi(id: Uuid): KrithiDto? = dal.krithis.findById(id)

    override suspend fun createKrithi(request: KrithiCreateRequest, userId: Uuid?): KrithiDto {
        val composerId = request.composerId.toJavaUuidOrThrow("composerId")
        val talaId = request.talaId?.toJavaUuidOrThrow("talaId")
        val primaryRagaId = request.primaryRagaId?.toJavaUuidOrThrow("primaryRagaId")
        val deityId = request.deityId?.toJavaUuidOrThrow("deityId")
        val templeId = request.templeId?.toJavaUuidOrThrow("templeId")
        val ragaIds = request.ragaIds.map { it.toJavaUuidOrThrow("ragaId") }
        val normalizedTitle = normalize(request.title)
        val normalizedIncipit = request.incipit?.let { normalize(it) }
        val isRagamalika = request.isRagamalika || ragaIds.size > 1

        val created = dal.krithis.create(
            KrithiCreateParams(
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
                notes = request.notes,
                createdByUserId = userId?.toJavaUuid(),
                updatedByUserId = userId?.toJavaUuid()
            )
        )

        dal.auditLogs.append(
            action = "CREATE_KRITHI",
            entityTable = "krithis",
            entityId = created.id,
            actorUserId = userId
        )

        return created
    }

    override suspend fun updateKrithi(id: Uuid, request: KrithiUpdateRequest, userId: Uuid?): KrithiDto {
        val composerId = request.composerId?.toJavaUuidOrThrow("composerId")
        val talaId = request.talaId?.toJavaUuidOrThrow("talaId")
        val primaryRagaId = request.primaryRagaId?.toJavaUuidOrThrow("primaryRagaId")
        val deityId = request.deityId?.toJavaUuidOrThrow("deityId")
        val templeId = request.templeId?.toJavaUuidOrThrow("templeId")
        val ragaIds = request.ragaIds?.map { it.toJavaUuidOrThrow("ragaId") }
        val normalizedTitle = request.title?.let { normalize(it) }
        val normalizedIncipit = request.incipit?.let { normalize(it) }

        val updated = dal.krithis.update(
            KrithiUpdateParams(
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
                notes = request.notes,
                updatedByUserId = userId?.toJavaUuid()
            )
        ) ?: throw NoSuchElementException("Krithi not found")

        // Update tags if provided
        request.tagIds?.let { tagIds ->
            val tagUuids = tagIds.map { it.toJavaUuidOrThrow("tagId") }
            dal.krithis.updateTags(id, tagUuids)
        }

        dal.auditLogs.append(
            action = "UPDATE_KRITHI",
            entityTable = "krithis",
            entityId = updated.id,
            actorUserId = userId
        )

        return updated
    }

    override suspend fun getKrithiSections(id: Uuid): List<KrithiSectionDto> = dal.krithis.getSections(id)

    override suspend fun getKrithiLyricVariants(id: Uuid): List<KrithiLyricVariantWithSectionsDto> = dal.krithis.getLyricVariants(id)

    override suspend fun getKrithiTags(id: Uuid): List<TagDto> = dal.krithis.getTags(id)

    override suspend fun saveKrithiSections(id: Uuid, sections: List<com.sangita.grantha.backend.api.models.KrithiSectionRequest>) {
        // Pass full section data including label for efficient updates
        val sectionsData = sections.map { 
            Triple(it.sectionType, it.orderIndex, it.label) 
        }
        dal.krithis.saveSections(id, sectionsData)
        
        dal.auditLogs.append(
            action = "UPDATE_KRITHI_SECTIONS",
            entityTable = "krithi_sections",
            entityId = id
        )
    }

    override suspend fun createLyricVariant(
        krithiId: Uuid,
        request: LyricVariantCreateRequest,
        userId: Uuid?
    ): KrithiLyricVariantDto {
        // Verify krithi exists
        val krithi = dal.krithis.findById(krithiId) 
            ?: throw NoSuchElementException("Krithi not found")
        
        val sampradayaId = request.sampradayaId?.toJavaUuidOrThrow("sampradayaId")
        
        val created = dal.krithis.createLyricVariant(
            krithiId = krithiId,
            language = LanguageCode.valueOf(request.language.name),
            script = ScriptCode.valueOf(request.script.name),
            transliterationScheme = request.transliterationScheme,
            sampradayaId = sampradayaId,
            variantLabel = request.variantLabel,
            sourceReference = request.sourceReference,
            lyrics = request.lyrics,
            isPrimary = request.isPrimary,
            createdByUserId = userId?.toJavaUuid(),
            updatedByUserId = userId?.toJavaUuid()
        )
        
        dal.auditLogs.append(
            action = "CREATE_LYRIC_VARIANT",
            entityTable = "krithi_lyric_variants",
            entityId = created.id,
            actorUserId = userId
        )
        
        return created
    }

    override suspend fun updateLyricVariant(
        variantId: Uuid,
        request: LyricVariantUpdateRequest,
        userId: Uuid?
    ): KrithiLyricVariantDto {
        val sampradayaId = request.sampradayaId?.toJavaUuidOrThrow("sampradayaId")
        
        val updated = dal.krithis.updateLyricVariant(
            variantId = variantId,
            language = request.language?.let { LanguageCode.valueOf(it.name) },
            script = request.script?.let { ScriptCode.valueOf(it.name) },
            transliterationScheme = request.transliterationScheme,
            sampradayaId = sampradayaId,
            variantLabel = request.variantLabel,
            sourceReference = request.sourceReference,
            lyrics = request.lyrics,
            isPrimary = request.isPrimary,
            updatedByUserId = userId?.toJavaUuid()
        ) ?: throw NoSuchElementException("Lyric variant not found")
        
        dal.auditLogs.append(
            action = "UPDATE_LYRIC_VARIANT",
            entityTable = "krithi_lyric_variants",
            entityId = updated.id,
            actorUserId = userId
        )
        
        return updated
    }

    override suspend fun saveLyricVariantSections(
        variantId: Uuid,
        sections: List<LyricVariantSectionRequest>
    ) {
        // Verify variant exists
        val variant = dal.krithis.findLyricVariantById(variantId)
            ?: throw NoSuchElementException("Lyric variant not found")
        
        val sectionsData = sections.map {
            it.sectionId.toJavaUuidOrThrow("sectionId") to it.text
        }
        
        dal.krithis.saveLyricVariantSections(variantId, sectionsData)
        
        dal.auditLogs.append(
            action = "UPDATE_LYRIC_VARIANT_SECTIONS",
            entityTable = "krithi_lyric_sections",
            entityId = variantId
        )
    }

    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
}
