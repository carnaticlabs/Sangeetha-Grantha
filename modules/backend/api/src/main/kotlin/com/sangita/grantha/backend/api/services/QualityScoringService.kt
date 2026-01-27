package com.sangita.grantha.backend.api.services

import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.net.URI
import org.slf4j.LoggerFactory

/**
 * TRACK-011: Quality Scoring System
 * 
 * Calculates quality scores for imported krithis based on:
 * - Completeness (40%): Has title, lyrics, composer, raga, tala
 * - Resolution Confidence (30%): Entity resolution confidence scores
 * - Source Quality (20%): blogspot.com = 0.8, other = 0.6
 * - Validation (10%): Passed header validation, URL valid
 */
interface IQualityScorer {
    /**
     * Calculate quality score for an imported krithi.
     */
    suspend fun calculateQualityScore(
        imported: ImportedKrithiDto,
        resolutionDataJson: String? = null
    ): QualityScore
}

data class QualityScore(
    val overall: Double,
    val completeness: Double,           // 40% weight
    val resolutionConfidence: Double,    // 30% weight
    val sourceQuality: Double,          // 20% weight
    val validationScore: Double,        // 10% weight
    val tier: QualityTier
)

enum class QualityTier {
    EXCELLENT,  // ≥ 0.90
    GOOD,       // ≥ 0.75
    FAIR,       // ≥ 0.60
    POOR        // < 0.60
}

class QualityScoringServiceImpl : IQualityScorer {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Calculate quality score for an imported krithi.
     */
    override suspend fun calculateQualityScore(
        imported: ImportedKrithiDto,
        resolutionDataJson: String?
    ): QualityScore {
        val completeness = calculateCompleteness(imported)
        val resolutionConfidence = calculateResolutionConfidence(resolutionDataJson)
        val sourceQuality = calculateSourceQuality(imported.sourceKey)
        val validationScore = calculateValidationScore(imported)

        val overall = (completeness * 0.40) +
                     (resolutionConfidence * 0.30) +
                     (sourceQuality * 0.20) +
                     (validationScore * 0.10)

        val tier = determineTier(overall)

        return QualityScore(
            overall = overall,
            completeness = completeness,
            resolutionConfidence = resolutionConfidence,
            sourceQuality = sourceQuality,
            validationScore = validationScore,
            tier = tier
        )
    }

    private fun calculateCompleteness(imported: ImportedKrithiDto): Double {
        var score = 0.0
        var maxScore = 0.0

        // Title (required)
        maxScore += 1.0
        if (!imported.rawTitle.isNullOrBlank()) score += 1.0

        // Lyrics (required)
        maxScore += 1.0
        if (!imported.rawLyrics.isNullOrBlank()) score += 1.0

        // Composer (required)
        maxScore += 1.0
        if (!imported.rawComposer.isNullOrBlank()) score += 1.0

        // Raga (required)
        maxScore += 1.0
        if (!imported.rawRaga.isNullOrBlank()) score += 1.0

        // Tala (optional but valuable)
        maxScore += 0.5
        if (!imported.rawTala.isNullOrBlank()) score += 0.5

        // Sections (optional but valuable - check if parsedPayload has sections)
        maxScore += 0.5
        val parsedPayloadStr = imported.parsedPayload
        if (parsedPayloadStr != null) {
            try {
                val payload = Json.parseToJsonElement(parsedPayloadStr)
                if (payload is JsonObject) {
                    val sections = payload["sections"]
                    if (sections != null && sections.toString().isNotBlank()) {
                        score += 0.5
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }

        return if (maxScore > 0) score / maxScore else 0.0
    }

    private fun calculateResolutionConfidence(resolutionDataJson: String?): Double {
        if (resolutionDataJson.isNullOrBlank()) return 0.0

        return try {
            val resolutionData = Json.parseToJsonElement(resolutionDataJson)
            if (resolutionData !is JsonObject) return 0.0

            val composerConfidence = resolutionData["composerCandidates"]
                ?.let { candidates ->
                    if (candidates is JsonArray && candidates.isNotEmpty()) {
                        val first = candidates[0]
                        if (first is JsonObject) {
                            first["confidence"]?.jsonPrimitive?.content
                        } else null
                    } else null
                }?.let { if (it == "HIGH") 1.0 else if (it == "MEDIUM") 0.7 else 0.5 } ?: 0.0

            val ragaConfidence = resolutionData["ragaCandidates"]
                ?.let { candidates ->
                    if (candidates is JsonArray && candidates.isNotEmpty()) {
                        val first = candidates[0]
                        if (first is JsonObject) {
                            first["confidence"]?.jsonPrimitive?.content
                        } else null
                    } else null
                }?.let { if (it == "HIGH") 1.0 else if (it == "MEDIUM") 0.7 else 0.5 } ?: 0.0

            val talaConfidence = resolutionData["talaCandidates"]
                ?.let { candidates ->
                    if (candidates is JsonArray && candidates.isNotEmpty()) {
                        val first = candidates[0]
                        if (first is JsonObject) {
                            first["confidence"]?.jsonPrimitive?.content
                        } else null
                    } else null
                }?.let { if (it == "HIGH") 1.0 else if (it == "MEDIUM") 0.7 else 0.5 } ?: 0.0

            // Average of all confidence scores
            val count = listOf(composerConfidence, ragaConfidence, talaConfidence).count { it > 0 }
            if (count > 0) {
                (composerConfidence + ragaConfidence + talaConfidence) / count
            } else {
                0.0
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse resolution data for quality scoring: ${e.message}")
            0.0
        }
    }

    private fun calculateSourceQuality(sourceKey: String?): Double {
        val sourceKeyStr = sourceKey
        if (sourceKeyStr.isNullOrBlank()) return 0.0

        // blogspot.com sources are considered higher quality
        return if (sourceKeyStr.contains("blogspot.com", ignoreCase = true)) {
            0.8
        } else {
            0.6
        }
    }

    private fun calculateValidationScore(imported: ImportedKrithiDto): Double {
        var score = 0.0

        // URL validation (syntax check passed)
        val sourceKey = imported.sourceKey
        if (sourceKey != null && isValidUrl(sourceKey)) {
            score += 0.5
        }

        // Header validation (passed CSV header checks - assume all imported krithis passed)
        score += 0.5

        return score
    }

    private fun determineTier(overall: Double): QualityTier {
        return when {
            overall >= 0.90 -> QualityTier.EXCELLENT
            overall >= 0.75 -> QualityTier.GOOD
            overall >= 0.60 -> QualityTier.FAIR
            else -> QualityTier.POOR
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            uri.scheme != null && (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true))
        } catch (e: Exception) {
            false
        }
    }
}
