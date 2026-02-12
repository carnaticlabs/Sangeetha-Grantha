package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.KrithiDto
import kotlinx.serialization.Serializable

/**
 * Service that detects potential duplicate krithis during import review.
 */
class DeduplicationService(
    private val dal: SangitaDal,
    private val normalizer: NameNormalizationService = NameNormalizationService()
) {

    @Serializable
    data class DuplicateMatch(
        val krithiId: String? = null,
        val importedKrithiId: String? = null,
        val reason: String,
        val confidence: String // HIGH, MEDIUM, LOW
    )

    @Serializable
    data class DeduplicationResult(
        val matches: List<DuplicateMatch>
    )

    /**
     * Find duplicate candidates across canonical and staging data.
     */
    suspend fun findDuplicates(
        imported: ImportedKrithiDto,
        resolvedComposerId: kotlin.uuid.Uuid?,
        resolvedRagaId: kotlin.uuid.Uuid?
    ): DeduplicationResult {
        val matches = mutableListOf<DuplicateMatch>()
        val titleNormalized = normalizer.normalizeTitle(imported.rawTitle) ?: ""

        // 1. Check against existing Krithis (Canonical)
        if (titleNormalized.isNotEmpty()) {
            val candidatesByMetadata = if (resolvedComposerId != null && resolvedRagaId != null) {
                dal.krithis.findDuplicateCandidates(titleNormalized, resolvedComposerId.toJavaUuid(), resolvedRagaId.toJavaUuid())
            } else {
                emptyList()
            }

            val candidatesByTitle = dal.krithis.findDuplicateCandidates(titleNormalized)
            val allCandidates = (candidatesByMetadata + candidatesByTitle).distinctBy { it.id }
            
            allCandidates.forEach { candidate ->
                val titleScore = NameNormalizationService.ratio(titleNormalized, candidate.titleNormalized)
                val compressedScore = NameNormalizationService.ratio(
                    titleNormalized.replace(" ", ""),
                    candidate.titleNormalized.replace(" ", "")
                )
                val bestScore = maxOf(titleScore, compressedScore)
                val metadataMatch = resolvedComposerId?.toJavaUuid() == candidate.composerId.toJavaUuid() &&
                                  resolvedRagaId?.toJavaUuid() == candidate.primaryRagaId?.toJavaUuid()

                val threshold = if (metadataMatch) 75 else 85

                if (bestScore >= threshold) {
                    matches.add(DuplicateMatch(
                        krithiId = candidate.id.toString(),
                        reason = "Canonical match: ${candidate.title} (Score: $bestScore)",
                        confidence = if (bestScore > 90) "HIGH" else "MEDIUM"
                    ))
                }
            }
        }
        
        // 2. Check against Staging (ImportedKrithis) - TRACK-013: Optimized DB query
        // Use optimized repository method instead of loading all pending imports
        val stagingCandidates = dal.imports.findSimilarPendingImports(
            normalizedTitle = titleNormalized,
            excludeId = imported.id,
            batchId = imported.importBatchId,
            limit = 20 // Limit results for performance
        )
        .filter { candidate ->
            val otherTitleNorm = normalizer.normalizeTitle(candidate.rawTitle) ?: ""
            otherTitleNorm == titleNormalized || NameNormalizationService.ratio(titleNormalized, otherTitleNorm) > 90
        }
        
        stagingCandidates.forEach { candidate ->
             matches.add(DuplicateMatch(
                 importedKrithiId = candidate.id.toString(),
                 reason = "Similar pending import: ${candidate.rawTitle}",
                 confidence = "MEDIUM"
             ))
        }

        return DeduplicationResult(matches)
    }

}
