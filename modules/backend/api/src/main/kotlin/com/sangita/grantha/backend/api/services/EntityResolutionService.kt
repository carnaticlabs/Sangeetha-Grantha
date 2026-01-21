package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.shared.domain.model.TalaDto
import kotlinx.serialization.Serializable
import kotlin.math.max

class EntityResolutionService(private val dal: SangitaDal) {

    @Serializable
    data class ResolutionResult(
        val composerCandidates: List<Candidate<ComposerDto>>,
        val ragaCandidates: List<Candidate<RagaDto>>,
        val talaCandidates: List<Candidate<TalaDto>>,
        val resolved: Boolean = false
    )

    @Serializable
    data class Candidate<T>(
        val entity: T,
        val score: Int,
        val confidence: String // HIGH, MEDIUM, LOW
    )

    suspend fun resolve(importedKrithi: ImportedKrithiDto): ResolutionResult {
        // Fetch all reference data (caching would be good here, but for now fetch all)
        val composers = dal.composers.listAll()
        val ragas = dal.ragas.listAll()
        val talas = dal.talas.listAll()

        val composerCandidates = match(importedKrithi.rawComposer, composers) { it.name }
        val ragaCandidates = match(importedKrithi.rawRaga, ragas) { it.name }
        val talaCandidates = match(importedKrithi.rawTala, talas) { it.name }

        return ResolutionResult(
            composerCandidates = composerCandidates,
            ragaCandidates = ragaCandidates,
            talaCandidates = talaCandidates,
            resolved = false 
        )
    }

    private fun <T> match(raw: String?, candidates: List<T>, nameSelector: (T) -> String): List<Candidate<T>> {
        if (raw.isNullOrBlank()) return emptyList()
        val rawLower = raw.trim().lowercase()

        return candidates.map { candidate ->
            val name = nameSelector(candidate)
            val score = ratio(rawLower, name.trim().lowercase())
            val confidence = when {
                score >= 90 -> "HIGH"
                score >= 70 -> "MEDIUM"
                else -> "LOW"
            }
            Candidate(candidate, score, confidence)
        }
        .filter { it.score > 50 }
        .sortedByDescending { it.score }
        .take(5)
    }

    // Simple Levenshtein ratio implementation (0-100)
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
                    distance[i - 1][j] + 1,      // deletion
                    distance[i][j - 1] + 1,      // insertion
                    distance[i - 1][j - 1] + cost // substitution
                )
            }
        }

        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 100
        val dist = distance[s1.length][s2.length]
        return ((1.0 - dist.toDouble() / maxLen) * 100).toInt()
    }
}
