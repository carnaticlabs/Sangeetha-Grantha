package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.KrithiDto
import kotlinx.serialization.Serializable
import kotlin.math.max

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

    suspend fun findDuplicates(
        imported: ImportedKrithiDto,
        resolvedComposerId: kotlin.uuid.Uuid?,
        resolvedRagaId: kotlin.uuid.Uuid?
    ): DeduplicationResult {
        val matches = mutableListOf<DuplicateMatch>()
        val titleNormalized = normalizer.normalizeTitle(imported.rawTitle) ?: ""

        // 1. Check against existing Krithis (Canonical)
        if (titleNormalized.isNotEmpty()) {
            val canonicalCandidates = dal.krithis.findDuplicateCandidates(
                titleNormalized = titleNormalized,
                composerId = resolvedComposerId?.toJavaUuid(),
                ragaId = resolvedRagaId?.toJavaUuid()
            )
            
            canonicalCandidates.forEach { candidate ->
                matches.add(DuplicateMatch(
                    krithiId = candidate.id.toString(),
                    reason = "Canonical match: ${candidate.title}",
                    confidence = "HIGH"
                ))
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
            otherTitleNorm == titleNormalized || ratio(titleNormalized, otherTitleNorm) > 90
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

    private fun ratio(s1: String, s2: String): Int {
        val rows = s1.length + 1
        val cols = s2.length + 1
        val distance = Array(rows) { IntArray(cols) }

        for (i in 0 until rows) distance[i][0] = i
        for (j in 0 until cols) distance[0][j] = j

        for (i in 1 until rows) {
            for (j in 1 until cols) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                distance[i][j] = minOf(
                    distance[i - 1][j] + 1,
                    distance[i][j - 1] + 1,
                    distance[i - 1][j - 1] + cost
                )
            }
        }

        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 100
        val dist = distance[s1.length][s2.length]
        return ((1.0 - dist.toDouble() / maxLen) * 100).toInt()
    }
}
