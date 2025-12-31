package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.NotationRowCreateRequest
import com.sangita.grantha.backend.api.models.NotationRowUpdateRequest
import com.sangita.grantha.backend.api.models.NotationVariantCreateRequest
import com.sangita.grantha.backend.api.models.NotationVariantUpdateRequest
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.repositories.NotationRowWithSectionOrder
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.KrithiNotationResponseDto
import com.sangita.grantha.shared.domain.model.KrithiNotationSectionGroupDto
import com.sangita.grantha.shared.domain.model.KrithiNotationVariantWithRowsDto
import com.sangita.grantha.shared.domain.model.KrithiNotationRowDto
import com.sangita.grantha.shared.domain.model.KrithiNotationVariantDto
import com.sangita.grantha.shared.domain.model.WorkflowStateDto
import java.util.UUID
import kotlin.uuid.Uuid

class KrithiNotationService(private val dal: SangitaDal) {
    suspend fun getPublishedNotation(krithiId: Uuid): KrithiNotationResponseDto? =
        getNotation(krithiId, includeUnpublished = false)

    suspend fun getAdminNotation(krithiId: Uuid): KrithiNotationResponseDto? =
        getNotation(krithiId, includeUnpublished = true)

    suspend fun createVariant(krithiId: Uuid, request: NotationVariantCreateRequest): KrithiNotationVariantDto {
        val krithi = dal.krithis.findById(krithiId) ?: throw NoSuchElementException("Krithi not found")
        val talaId = request.talaId?.let { parseUuidOrThrow(it, "talaId") }

        val created = dal.krithiNotation.createVariant(
            krithiId = krithi.id.toJavaUuid(),
            notationType = request.notationType.name,
            talaId = talaId,
            kalai = request.kalai,
            eduppuOffsetBeats = request.eduppuOffsetBeats,
            variantLabel = request.variantLabel,
            sourceReference = request.sourceReference,
            isPrimary = request.isPrimary,
            createdByUserId = null,
            updatedByUserId = null
        )

        dal.auditLogs.append(
            action = "CREATE_NOTATION_VARIANT",
            entityTable = "krithi_notation_variants",
            entityId = created.id
        )

        return created
    }

    suspend fun updateVariant(
        variantId: Uuid,
        request: NotationVariantUpdateRequest
    ): KrithiNotationVariantDto {
        val talaId = request.talaId?.let { parseUuidOrThrow(it, "talaId") }
        val updated = dal.krithiNotation.updateVariant(
            variantId = variantId,
            notationType = request.notationType?.name,
            talaId = talaId,
            kalai = request.kalai,
            eduppuOffsetBeats = request.eduppuOffsetBeats,
            variantLabel = request.variantLabel,
            sourceReference = request.sourceReference,
            isPrimary = request.isPrimary,
            updatedByUserId = null
        ) ?: throw NoSuchElementException("Notation variant not found")

        dal.auditLogs.append(
            action = "UPDATE_NOTATION_VARIANT",
            entityTable = "krithi_notation_variants",
            entityId = updated.id
        )

        return updated
    }

    suspend fun deleteVariant(variantId: Uuid): Boolean {
        val deleted = dal.krithiNotation.deleteVariant(variantId)
        if (deleted) {
            dal.auditLogs.append(
                action = "DELETE_NOTATION_VARIANT",
                entityTable = "krithi_notation_variants",
                entityId = variantId
            )
        }
        return deleted
    }

    suspend fun createRow(variantId: Uuid, request: NotationRowCreateRequest): KrithiNotationRowDto {
        val variant = dal.krithiNotation.findVariantById(variantId)
            ?: throw NoSuchElementException("Notation variant not found")
        val sectionId = parseUuidOrThrow(request.sectionId, "sectionId")

        val created = dal.krithiNotation.createRow(
            notationVariantId = variant.id.toJavaUuid(),
            sectionId = sectionId,
            orderIndex = request.orderIndex,
            swaraText = request.swaraText,
            sahityaText = request.sahityaText,
            talaMarkers = request.talaMarkers
        )

        dal.auditLogs.append(
            action = "CREATE_NOTATION_ROW",
            entityTable = "krithi_notation_rows",
            entityId = created.id
        )

        return created
    }

    suspend fun updateRow(rowId: Uuid, request: NotationRowUpdateRequest): KrithiNotationRowDto {
        val sectionId = request.sectionId?.let { parseUuidOrThrow(it, "sectionId") }
        val updated = dal.krithiNotation.updateRow(
            rowId = rowId,
            sectionId = sectionId,
            orderIndex = request.orderIndex,
            swaraText = request.swaraText,
            sahityaText = request.sahityaText,
            talaMarkers = request.talaMarkers
        ) ?: throw NoSuchElementException("Notation row not found")

        dal.auditLogs.append(
            action = "UPDATE_NOTATION_ROW",
            entityTable = "krithi_notation_rows",
            entityId = updated.id
        )

        return updated
    }

    suspend fun deleteRow(rowId: Uuid): Boolean {
        val deleted = dal.krithiNotation.deleteRow(rowId)
        if (deleted) {
            dal.auditLogs.append(
                action = "DELETE_NOTATION_ROW",
                entityTable = "krithi_notation_rows",
                entityId = rowId
            )
        }
        return deleted
    }

    private suspend fun getNotation(
        krithiId: Uuid,
        includeUnpublished: Boolean,
    ): KrithiNotationResponseDto? {
        val krithi = dal.krithis.findById(krithiId) ?: return null
        if (!includeUnpublished && krithi.workflowState != WorkflowStateDto.PUBLISHED) {
            return null
        }

        val variants = dal.krithiNotation.listVariantsByKrithiId(krithiId)
        val rows = dal.krithiNotation.listRowsByVariantIds(variants.map { it.id })

        val groupedVariants = variants.map { variant ->
            KrithiNotationVariantWithRowsDto(
                variant = variant,
                sections = groupRowsBySection(rows, variant.id)
            )
        }

        val talaId = variants.firstOrNull { it.isPrimary }?.talaId ?: variants.firstOrNull()?.talaId

        return KrithiNotationResponseDto(
            krithiId = krithi.id,
            musicalForm = krithi.musicalForm,
            talaId = talaId,
            variants = groupedVariants
        )
    }

    private fun groupRowsBySection(
        rows: List<NotationRowWithSectionOrder>,
        variantId: Uuid,
    ): List<KrithiNotationSectionGroupDto> {
        val sectionRows = linkedMapOf<Uuid, MutableList<KrithiNotationRowDto>>()
        val sectionOrder = linkedMapOf<Uuid, Int>()

        rows.asSequence()
            .filter { it.row.notationVariantId == variantId }
            .forEach { entry ->
                val sectionId = entry.row.sectionId
                sectionOrder.putIfAbsent(sectionId, entry.sectionOrderIndex)
                sectionRows.getOrPut(sectionId) { mutableListOf() }.add(entry.row)
            }

        return sectionRows.map { (sectionId, orderedRows) ->
            KrithiNotationSectionGroupDto(
                sectionId = sectionId,
                sectionOrderIndex = sectionOrder[sectionId] ?: 0,
                rows = orderedRows
            )
        }
    }

    private fun parseUuidOrThrow(value: String, label: String): UUID =
        try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid $label")
        }
}
