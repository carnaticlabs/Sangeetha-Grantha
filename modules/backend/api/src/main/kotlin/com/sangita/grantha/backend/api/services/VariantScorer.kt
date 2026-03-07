package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import kotlin.uuid.Uuid

/**
 * Extracted from VariantMatchingService (TRACK-056).
 * Encapsulates candidate scoring logic: title similarity, raga/tala matching,
 * page-position bonus, and structure-mismatch detection.
 */
class VariantScorer(
    private val dal: SangitaDal,
    private val normalizer: NameNormalizationService,
) {

    data class MatchSignals(
        val titleScore: Double,
        val ragaTalaScore: Double,
        val pagePositionScore: Double,
    )

    data class ScoredCandidate(
        val krithiId: Uuid,
        val score: Double,
        val signals: MatchSignals,
    )

    /**
     * Compute match signals between an extraction and a candidate Krithi.
     */
    suspend fun computeMatchSignals(
        extraction: CanonicalExtractionDto,
        candidate: KrithiDto,
        primaryKrithiIds: Set<Uuid>,
        positionIndex: Int,
        extractedTitleNormalized: String,
    ): MatchSignals {
        // Title match: compare normalized titles
        val extractedTitle = extractedTitleNormalized
        val candidateTitle = candidate.titleNormalized
        val titleScore = if (extractedTitle.isNotEmpty() && candidateTitle.isNotEmpty()) {
            if (extractedTitle == candidateTitle) 1.0
            else levenshteinSimilarity(extractedTitle, candidateTitle)
        } else {
            0.0
        }

        // Raga+Tala match: look up candidate names from reference data
        val extractedRaga = normalizer.normalizeTitle(extraction.ragas.firstOrNull()?.name) ?: ""
        val candidateRagaName = candidate.primaryRagaId?.let { ragaId ->
            dal.ragas.findById(ragaId)?.name
        }
        val candidateRaga = candidateRagaName?.let { normalizer.normalizeTitle(it) } ?: ""
        val ragaMatch = if (extractedRaga.isNotEmpty() && candidateRaga.isNotEmpty()) {
            if (extractedRaga == candidateRaga) 1.0 else levenshteinSimilarity(extractedRaga, candidateRaga)
        } else {
            0.0
        }

        val extractedTala = normalizer.normalizeTitle(extraction.tala) ?: ""
        val candidateTalaName = candidate.talaId?.let { talaId ->
            dal.talas.findById(talaId)?.name
        }
        val candidateTala = candidateTalaName?.let { normalizer.normalizeTitle(it) } ?: ""
        val talaMatch = if (extractedTala.isNotEmpty() && candidateTala.isNotEmpty()) {
            if (extractedTala == candidateTala) 1.0 else levenshteinSimilarity(extractedTala, candidateTala)
        } else {
            0.0
        }

        val ragaTalaScore = if (ragaMatch > 0 || talaMatch > 0) {
            (ragaMatch + talaMatch) / 2.0
        } else {
            0.0
        }

        // Page position match: if the Krithi was in the primary extraction scope, give a bonus
        val pagePositionScore = if (primaryKrithiIds.isNotEmpty() && candidate.id in primaryKrithiIds) {
            1.0
        } else if (primaryKrithiIds.isEmpty()) {
            0.5 // No related extraction — neutral
        } else {
            0.0 // Anomaly: not in the primary scope
        }

        return MatchSignals(titleScore, ragaTalaScore, pagePositionScore)
    }

    /**
     * Check whether the extraction has a different section structure than the existing Krithi.
     */
    suspend fun checkStructureMismatch(
        extraction: CanonicalExtractionDto,
        candidate: KrithiDto,
    ): Boolean {
        val extractedSectionCount = extraction.sections.size
        if (extractedSectionCount == 0) return false

        val krithiSections = dal.krithis.getSections(candidate.id)
        val candidateSectionCount = krithiSections.size
        return candidateSectionCount > 0 && extractedSectionCount != candidateSectionCount
    }

    /**
     * Compute Levenshtein similarity (0.0 = completely different, 1.0 = identical).
     */
    fun levenshteinSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        val distance = levenshteinDistance(a, b)
        return 1.0 - distance.toDouble() / maxLen
    }

    fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }
}
