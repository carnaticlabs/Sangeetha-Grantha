package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import java.util.UUID
import kotlin.uuid.Uuid

class ImportService(private val dal: SangitaDal) {
    suspend fun submitImports(requests: List<ImportKrithiRequest>): List<ImportedKrithiDto> {
        if (requests.isEmpty()) return emptyList()

        val created = mutableListOf<ImportedKrithiDto>()
        for (request in requests) {
            val sourceId = dal.imports.findOrCreateSource(request.source)
            val parsedPayload = request.rawPayload?.toString()
            val importRow = dal.imports.createImport(
                sourceId = sourceId,
                sourceKey = request.sourceKey,
                rawTitle = request.rawTitle,
                rawLyrics = request.rawLyrics,
                rawComposer = request.rawComposer,
                rawRaga = request.rawRaga,
                rawTala = request.rawTala,
                rawDeity = request.rawDeity,
                rawTemple = request.rawTemple,
                rawLanguage = request.rawLanguage,
                parsedPayload = parsedPayload
            )

            dal.auditLogs.append(
                action = "IMPORT_KRITHI",
                entityTable = "imported_krithis",
                entityId = importRow.id
            )

            created.add(importRow)
        }

        return created
    }

    suspend fun reviewImport(id: Uuid, request: ImportReviewRequest): ImportedKrithiDto {
        val mappedId = request.mappedKrithiId?.let { parseUuidOrThrow(it, "mappedKrithiId") }
        val status = ImportStatus.valueOf(request.status.name)
        val updated = dal.imports.reviewImport(
            id = id,
            status = status,
            mappedKrithiId = mappedId,
            reviewerNotes = request.reviewerNotes
        ) ?: throw NoSuchElementException("Import not found")

        dal.auditLogs.append(
            action = "REVIEW_IMPORT",
            entityTable = "imported_krithis",
            entityId = updated.id
        )

        return updated
    }

    private fun parseUuidOrThrow(value: String, label: String): UUID =
        try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid $label")
        }
}
