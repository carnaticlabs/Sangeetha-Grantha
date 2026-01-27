package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.config.AutoApprovalConfig
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Service for determining if imports should be auto-approved and performing auto-approval.
 * 
 * This service is independent of ImportService to avoid circular dependencies.
 * It uses the ImportReviewer interface to perform reviews.
 */
class AutoApprovalService(
    private val importReviewer: ImportReviewer,
    private val config: AutoApprovalConfig = AutoApprovalConfig.fromEnvironment()
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        val errors = config.validate()
        if (errors.isNotEmpty()) {
            logger.error("Invalid AutoApprovalConfig: ${errors.joinToString()}")
            throw IllegalArgumentException("Invalid AutoApprovalConfig: ${errors.joinToString()}")
        }
        logger.info("AutoApprovalService initialized with config: minQualityScore=${config.minQualityScore}, qualityTiers=${config.qualityTiers}")
    }

    /**
     * TRACK-012: Check if an import should be auto-approved based on configurable rules
     */
    fun shouldAutoApprove(imported: ImportedKrithiDto): Boolean {
        if (imported.importStatus != ImportStatusDto.PENDING) return false

        // Check quality tier requirement
        if (imported.qualityTier != null && !config.qualityTiers.contains(imported.qualityTier)) {
            logger.debug("Import ${imported.id} failed quality tier check: ${imported.qualityTier} not in ${config.qualityTiers}")
            return false
        }

        // Check quality score requirement
        val qualityScore = imported.qualityScore
        if (qualityScore != null && qualityScore < config.minQualityScore) {
            logger.debug("Import ${imported.id} failed quality score check: $qualityScore < ${config.minQualityScore}")
            return false
        }

        // Check minimal metadata requirement
        if (config.requireMinimalMetadata) {
            val hasMinimalMetadata = !imported.rawLyrics.isNullOrBlank() && !imported.rawTitle.isNullOrBlank()
            if (!hasMinimalMetadata) {
                logger.debug("Import ${imported.id} failed minimal metadata check")
                return false
            }
        }

        // Parse resolution data
        val resolution = try {
            imported.resolutionData?.let {
                Json.decodeFromString<ResolutionResult>(it)
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse resolution data for import ${imported.id}: ${e.message}")
            null
        } ?: return false

        // Check composer confidence requirement
        if (config.requireComposerMatch) {
            val hasHighConfidenceComposer = resolution.composerCandidates.any { it.confidence == "HIGH" }
            if (!hasHighConfidenceComposer) {
                logger.debug("Import ${imported.id} failed composer confidence check")
                return false
            }
        }

        // Check raga confidence requirement
        if (config.requireRagaMatch) {
            val hasHighConfidenceRaga = resolution.ragaCandidates.any { it.confidence == "HIGH" }
            if (!hasHighConfidenceRaga) {
                logger.debug("Import ${imported.id} failed raga confidence check")
                return false
            }
        }

        // Check for duplicate conflicts
        val duplicates = try {
            imported.duplicateCandidates?.let {
                Json.decodeFromString<DeduplicationService.DeduplicationResult>(it)
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse duplicate candidates for import ${imported.id}: ${e.message}")
            null
        }

        val hasNoHighConfidenceDuplicates = duplicates?.matches?.none { it.confidence == "HIGH" } ?: true
        if (!hasNoHighConfidenceDuplicates) {
            logger.debug("Import ${imported.id} failed duplicate check")
            return false
        }

        logger.info("Import ${imported.id} passed all auto-approval checks")
        return true
    }

    /**
     * Get the current configuration
     */
    fun getConfig(): AutoApprovalConfig = config

    /**
     * Auto-approve an import if it meets all criteria.
     * 
     * @param imported The import to check and potentially approve
     */
    suspend fun autoApproveIfHighConfidence(imported: ImportedKrithiDto) {
        if (!shouldAutoApprove(imported)) return

        logger.info("Auto-approving imported krithi {}: {}", imported.id, imported.rawTitle)
        try {
            importReviewer.reviewImport(
                id = imported.id,
                request = ImportReviewRequest(
                    status = ImportStatusDto.APPROVED,
                    reviewerNotes = "Auto-approved: High confidence resolution and no canonical duplicates found."
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to auto-approve krithi {}", imported.id, e)
        }
    }
}
