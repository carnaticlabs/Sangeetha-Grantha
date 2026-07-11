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
     * @param reviewerUserId The curator performing the review (from JWT);
     *        null for system-driven reviews (auto-approval). Used for
     *        revision attribution (TRACK-117 / ADR-014).
     * @return The updated import DTO
     */
    suspend fun reviewImport(id: Uuid, request: ImportReviewRequest, reviewerUserId: Uuid? = null): ImportedKrithiDto
}
