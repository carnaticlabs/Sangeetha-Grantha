package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.ExtractionIntent
import com.sangita.grantha.shared.domain.model.*
import kotlin.uuid.Uuid

/**
 * TRACK-045: Unified service for all sourcing & extraction monitoring operations.
 * Delegates to the specialized DAL repositories and handles audit logging.
 */
class SourcingService(private val dal: SangitaDal) {

    // =========================================================================
    // Source Registry
    // =========================================================================

    suspend fun listSources(
        tier: List<Int>? = null,
        format: List<String>? = null,
        search: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<ImportSourceDto> {
        val offset = (page - 1) * pageSize
        val (items, total) = dal.sourceRegistry.list(tier, format, search, pageSize, offset)
        return PaginatedResponse(items, total, page, pageSize)
    }

    suspend fun getSourceDetail(id: Uuid): ImportSourceDetailDto? =
        dal.sourceRegistry.findDetailById(id)

    suspend fun createSource(
        request: CreateSourceRequestDto,
        actorUserId: Uuid?,
    ): ImportSourceDto {
        val result = dal.sourceRegistry.create(
            name = request.name,
            baseUrl = request.baseUrl,
            description = request.description,
            sourceTier = request.sourceTier,
            supportedFormats = request.supportedFormats,
            composerAffinity = request.composerAffinity,
        )
        dal.auditLogs.append(
            action = "CREATE_SOURCE",
            entityTable = "import_sources",
            entityId = result.id,
            actorUserId = actorUserId,
        )
        return result
    }

    suspend fun updateSource(
        id: Uuid,
        request: UpdateSourceRequestDto,
        actorUserId: Uuid?,
    ): ImportSourceDto? {
        val result = dal.sourceRegistry.update(
            id = id,
            name = request.name,
            baseUrl = request.baseUrl,
            description = request.description,
            sourceTier = request.sourceTier,
            supportedFormats = request.supportedFormats,
            composerAffinity = request.composerAffinity,
        )
        if (result != null) {
            dal.auditLogs.append(
                action = "UPDATE_SOURCE",
                entityTable = "import_sources",
                entityId = id,
                actorUserId = actorUserId,
            )
        }
        return result
    }

    suspend fun deactivateSource(id: Uuid, actorUserId: Uuid?) {
        // Soft deactivate via update
        dal.sourceRegistry.update(id = id)
        dal.auditLogs.append(
            action = "DEACTIVATE_SOURCE",
            entityTable = "import_sources",
            entityId = id,
            actorUserId = actorUserId,
        )
    }

    // =========================================================================
    // Extraction Queue
    // =========================================================================

    suspend fun listExtractions(
        status: List<String>? = null,
        format: List<String>? = null,
        sourceId: String? = null,
        batchId: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<ExtractionTaskDto> {
        val offset = (page - 1) * pageSize
        val (items, total) = dal.extractionQueue.list(status, format, sourceId, batchId, pageSize, offset)
        return PaginatedResponse(items, total, page, pageSize)
    }

    suspend fun getExtractionDetail(id: Uuid): ExtractionDetailDto? =
        dal.extractionQueue.findById(id)

    suspend fun createExtraction(
        request: CreateExtractionRequestDto,
        actorUserId: Uuid?,
    ): ExtractionDetailDto {
        val requestPayload = buildString {
            append("{")
            request.composerHint?.let { append("\"composerHint\":\"$it\",") }
            request.expectedKrithiCount?.let { append("\"expectedKrithiCount\":$it,") }
            request.pageRange?.let { append("\"pageRange\":\"$it\",") }
            append("}")
        }.replace(",}", "}")

        val intent = try {
            ExtractionIntent.entries.first { it.dbValue == request.extractionIntent }
        } catch (_: Exception) {
            ExtractionIntent.PRIMARY
        }

        val result = dal.extractionQueue.create(
            sourceUrl = request.sourceUrl,
            sourceFormat = request.sourceFormat,
            importBatchId = request.importBatchId?.let { java.util.UUID.fromString(it.toString()) },
            pageRange = request.pageRange,
            composerHint = request.composerHint,
            maxAttempts = request.maxAttempts,
            requestPayload = requestPayload,
            contentLanguage = request.contentLanguage,
            extractionIntent = intent,
            relatedExtractionId = request.relatedExtractionId?.let { java.util.UUID.fromString(it.toString()) },
        )
        dal.auditLogs.append(
            action = "CREATE_EXTRACTION",
            entityTable = "extraction_queue",
            entityId = result.id,
            actorUserId = actorUserId,
        )
        return result
    }

    suspend fun retryExtraction(id: Uuid, actorUserId: Uuid?): Boolean {
        val result = dal.extractionQueue.retry(id)
        if (result) {
            dal.auditLogs.append(
                action = "RETRY_EXTRACTION",
                entityTable = "extraction_queue",
                entityId = id,
                actorUserId = actorUserId,
            )
        }
        return result
    }

    suspend fun cancelExtraction(id: Uuid, actorUserId: Uuid?): Boolean {
        val result = dal.extractionQueue.cancel(id)
        if (result) {
            dal.auditLogs.append(
                action = "CANCEL_EXTRACTION",
                entityTable = "extraction_queue",
                entityId = id,
                actorUserId = actorUserId,
            )
        }
        return result
    }

    suspend fun retryAllFailedExtractions(actorUserId: Uuid?): Int {
        val count = dal.extractionQueue.retryAllFailed()
        if (count > 0) {
            dal.auditLogs.append(
                action = "RETRY_ALL_FAILED_EXTRACTIONS",
                entityTable = "extraction_queue",
                actorUserId = actorUserId,
                metadata = """{"retriedCount":$count}""",
            )
        }
        return count
    }

    suspend fun getExtractionStats(): ExtractionStatsDto =
        dal.extractionQueue.getStats()

    // =========================================================================
    // Source Evidence
    // =========================================================================

    suspend fun listEvidence(
        minSourceCount: Int? = null,
        search: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<SourceEvidenceSummaryDto> {
        val offset = (page - 1) * pageSize
        val (items, total) = dal.sourceEvidence.listEvidenceSummaries(minSourceCount, search, pageSize, offset)
        return PaginatedResponse(items, total, page, pageSize)
    }

    suspend fun getKrithiEvidence(krithiId: Uuid): KrithiEvidenceResponseDto? =
        dal.sourceEvidence.getKrithiEvidence(krithiId)

    // =========================================================================
    // Structural Voting
    // =========================================================================

    suspend fun listVotingDecisions(
        consensusType: List<String>? = null,
        confidence: List<String>? = null,
        hasDissents: Boolean? = null,
        pendingReview: Boolean? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<VotingDecisionDto> {
        val offset = (page - 1) * pageSize
        val (items, total) = dal.structuralVoting.list(consensusType, confidence, hasDissents, pendingReview, pageSize, offset)
        return PaginatedResponse(items, total, page, pageSize)
    }

    suspend fun getVotingDetail(id: Uuid): VotingDetailDto? =
        dal.structuralVoting.findById(id)

    suspend fun submitOverride(
        votingId: Uuid,
        structure: String,
        notes: String,
        reviewerId: Uuid,
    ): VotingDetailDto? {
        val result = dal.structuralVoting.createOverride(votingId, structure, notes, reviewerId)
        if (result != null) {
            dal.auditLogs.append(
                action = "MANUAL_STRUCTURE_OVERRIDE",
                entityTable = "structural_vote_log",
                entityId = result.id,
                actorUserId = reviewerId,
            )
        }
        return result
    }

    suspend fun getVotingStats(): VotingStatsDto =
        dal.structuralVoting.getStats()

    // =========================================================================
    // Quality Dashboard
    // =========================================================================

    suspend fun getQualitySummary(): QualitySummaryDto =
        dal.qualityDashboard.getSummary()
}
