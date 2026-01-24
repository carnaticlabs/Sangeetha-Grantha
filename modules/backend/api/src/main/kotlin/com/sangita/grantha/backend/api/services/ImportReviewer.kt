package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import kotlin.uuid.Uuid

/**
 * Interface for reviewing imports to break circular dependency between
 * ImportService and AutoApprovalService.
 * 
 * AutoApprovalService can call this interface to review imports without
 * depending on the full ImportService class.
 */
interface ImportReviewer {
    /**
     * Review an import with the given request.
     * 
     * @param id The ID of the import to review
     * @param request The review request containing status, notes, etc.
     * @return The updated import DTO
     */
    suspend fun reviewImport(id: Uuid, request: ImportReviewRequest): ImportedKrithiDto
}
