package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.VariantMatchDto
import com.sangita.grantha.shared.domain.model.VariantMatchReportDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

/**
 * TRACK-056: Service for matching enrichment extractions to existing Krithis.
 *
 * When a language variant (e.g. Sanskrit PDF) is submitted as an ENRICH extraction,
 * this service matches each extracted Krithi to existing Krithis using multi-signal
 * confidence scoring: title transliteration, raga+tala match, and page position.
 *
 * Matches with confidence >= 0.85 are auto-approved. Lower confidence matches
 * require manual review via the variant match review UI.
 */
class VariantMatchingService(
    private val dal: SangitaDal,
    private val normalizer: NameNormalizationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.85
        private const val MEDIUM_CONFIDENCE_THRESHOLD = 0.50

        // Signal weights — must sum to 1.0
        private const val WEIGHT_TITLE = 0.50
        private const val WEIGHT_RAGA_TALA = 0.30
        private const val WEIGHT_PAGE_POSITION = 0.20
    }

    /**
     * Process an ENRICH extraction: match each extracted Krithi to existing Krithis
     * linked to the related (primary) extraction.
     *
     * @param extractions The extracted Krithis from the enrichment PDF
     * @param enrichExtractionId The extraction queue ID for this ENRICH task
     * @param relatedExtractionId The primary extraction this enriches
     * @return A report summarising the matching results
     */
    suspend fun matchVariants(
        extractions: List<CanonicalExtractionDto>,
        enrichExtractionId: Uuid,
        relatedExtractionId: Uuid?,
    ): VariantMatchReportDto {
        logger.info("Starting variant matching for extraction $enrichExtractionId, " +
            "${extractions.size} extractions, related=$relatedExtractionId")

        // Get Krithis that were created/matched from the primary extraction
        val primaryKrithiIds = if (relatedExtractionId != null) {
            getPrimaryExtractionKrithiIds(relatedExtractionId)
        } else {
            emptySet()
        }

        for ((index, extraction) in extractions.withIndex()) {
            try {
                matchSingleExtraction(extraction, enrichExtractionId, primaryKrithiIds, index)
            } catch (e: Exception) {
                logger.error("Failed to match extraction '${extraction.title}': ${e.message}", e)
            }
        }

        val report = dal.variantMatch.getReport(enrichExtractionId)
        logger.info("Variant matching complete for $enrichExtractionId: " +
            "${report.totalMatches} matches (${report.highConfidence} high, " +
            "${report.mediumConfidence} medium, ${report.lowConfidence} low, " +
            "${report.anomalies} anomalies, ${report.autoApproved} auto-approved)")
        return report
    }

    /**
     * Match a single extraction result to the best existing Krithi.
     */
    private suspend fun matchSingleExtraction(
        extraction: CanonicalExtractionDto,
        enrichExtractionId: Uuid,
        primaryKrithiIds: Set<Uuid>,
        positionIndex: Int,
    ) {
        val primaryNormalized = normalizer.normalizeTitle(extraction.title)
        val alternateNormalized = extraction.alternateTitle?.let { normalizer.normalizeTitle(it) }
        val titleNormalized = if (!primaryNormalized.isNullOrEmpty()) primaryNormalized else alternateNormalized

        if (titleNormalized.isNullOrEmpty()) {
            logger.info("No valid normalized title for variant '${extraction.title}' (alternate: '${extraction.alternateTitle}')")
            return
        }

        // Search for candidate Krithis by normalized title
        val candidates = dal.krithis.findDuplicateCandidates(
            titleNormalized = titleNormalized,
        )

        if (candidates.isEmpty()) {
            logger.info("No Krithi match for variant '${extraction.title}' (normalised: '$titleNormalized')")
            return
        }

        // Score each candidate
        val scoredCandidates = mutableListOf<ScoredCandidate>()
        for (candidate in candidates) {
            val signals = computeMatchSignals(extraction, candidate, primaryKrithiIds, positionIndex, titleNormalized)
            val compositeScore = signals.titleScore * WEIGHT_TITLE +
                signals.ragaTalaScore * WEIGHT_RAGA_TALA +
                signals.pagePositionScore * WEIGHT_PAGE_POSITION
            scoredCandidates.add(ScoredCandidate(candidate.id, compositeScore, signals))
        }
        scoredCandidates.sortByDescending { it.score }

        val best = scoredCandidates.first()
        val confidenceTier = when {
            best.score >= HIGH_CONFIDENCE_THRESHOLD -> "HIGH"
            best.score >= MEDIUM_CONFIDENCE_THRESHOLD -> "MEDIUM"
            else -> "LOW"
        }
        val matchStatus = if (confidenceTier == "HIGH") "AUTO_APPROVED" else "PENDING"
        val isAnomaly = primaryKrithiIds.isNotEmpty() && best.krithiId !in primaryKrithiIds

        // Check structural mismatch
        val candidateKrithi = candidates.first { it.id == best.krithiId }
        val structureMismatch = checkStructureMismatch(extraction, candidateKrithi)

        val signalsJson = buildJsonObject {
            put("titleScore", best.signals.titleScore)
            put("ragaTalaScore", best.signals.ragaTalaScore)
            put("pagePositionScore", best.signals.pagePositionScore)
            put("titleNormalized", titleNormalized)
        }.toString()

        val extractionPayload = json.encodeToString(CanonicalExtractionDto.serializer(), extraction)

        val match = dal.variantMatch.create(
            extractionId = enrichExtractionId,
            krithiId = best.krithiId,
            confidence = best.score,
            confidenceTier = confidenceTier,
            matchSignals = signalsJson,
            matchStatus = matchStatus,
            extractionPayload = extractionPayload,
            isAnomaly = isAnomaly,
            structureMismatch = structureMismatch,
        )

        if (matchStatus == "AUTO_APPROVED") {
            persistMatch(match.id)
        }

        dal.auditLogs.append(
            action = "VARIANT_MATCH_CREATED",
            entityTable = "variant_match",
            entityId = best.krithiId,
            metadata = """{"extractionId":"$enrichExtractionId","confidence":${best.score},"tier":"$confidenceTier","status":"$matchStatus"}""",
        )

        logger.debug("Matched '${extraction.title}' -> Krithi ${best.krithiId} " +
            "(score=${String.format("%.4f", best.score)}, tier=$confidenceTier, status=$matchStatus)")
    }

    /**
     * Compute match signals between an extraction and a candidate Krithi.
     */
    private suspend fun computeMatchSignals(
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
     * Uses the sections count from the Krithi's stored sections.
     */
    private suspend fun checkStructureMismatch(
        extraction: CanonicalExtractionDto,
        candidate: KrithiDto,
    ): Boolean {
        val extractedSectionCount = extraction.sections.size
        if (extractedSectionCount == 0) return false

        // Look up stored section count for this Krithi
        val krithiSections = dal.krithis.getSections(candidate.id)
        val candidateSectionCount = krithiSections.size
        return candidateSectionCount > 0 && extractedSectionCount != candidateSectionCount
    }

    /**
     * Get Krithi IDs that were linked from a primary extraction via source evidence.
     */
    private suspend fun getPrimaryExtractionKrithiIds(extractionId: Uuid): Set<Uuid> {
        val detail = dal.extractionQueue.findById(extractionId) ?: return emptySet()
        val resultPayload = detail.resultPayload ?: return emptySet()

        return try {
            val extractions = json.decodeFromString<List<CanonicalExtractionDto>>(resultPayload)
            extractions.mapNotNull { extraction ->
                val titleNormalized = normalizer.normalizeTitle(extraction.title) ?: return@mapNotNull null
                val candidates = dal.krithis.findDuplicateCandidates(titleNormalized = titleNormalized)
                candidates.firstOrNull()?.id
            }.toSet()
        } catch (e: Exception) {
            logger.warn("Failed to parse primary extraction results: ${e.message}")
            emptySet()
        }
    }

    /**
     * Review a variant match (approve, reject, skip).
     */
    suspend fun reviewMatch(
        matchId: Uuid,
        action: String,
        reviewerId: Uuid,
        notes: String? = null,
    ): Boolean {
        val status = when (action.lowercase()) {
            "approve" -> "APPROVED"
            "reject" -> "REJECTED"
            else -> return false
        }

        val result = dal.variantMatch.updateStatus(matchId, status, reviewerId, notes)
        if (result && status == "APPROVED") {
            // Persist the variant to the Krithi
            persistMatch(matchId)
        }
        
        if (result) {
            dal.auditLogs.append(
                action = "VARIANT_MATCH_REVIEWED",
                entityTable = "variant_match",
                entityId = matchId,
                actorUserId = reviewerId,
                metadata = """{"matchStatus":"$status"}""",
            )
        }
        return result
    }

    /**
     * Persist an approved match as a lyric variant.
     */
    private suspend fun persistMatch(matchId: Uuid) {
        val matchPair = dal.variantMatch.findById(matchId) ?: return
        val (match, payload) = matchPair
        if (payload == null) return

        val extraction = json.decodeFromString<CanonicalExtractionDto>(payload)
        val krithiId = match.krithiId
        val savedSections = dal.krithis.getSections(krithiId)

        for (variant in extraction.lyricVariants) {
            val language = runCatching { LanguageCode.valueOf(variant.language.uppercase()) }.getOrNull()
            val script = runCatching { ScriptCode.valueOf(variant.script.uppercase()) }.getOrNull()
            if (language == null || script == null) continue

            val fullLyrics = variant.sections
                .sortedBy { it.sectionOrder }
                .joinToString("\n\n") { it.text }

            val lyricVariant = dal.krithis.createLyricVariant(
                krithiId = krithiId,
                language = language,
                script = script,
                lyrics = fullLyrics,
                isPrimary = false,
                sourceReference = extraction.sourceUrl,
            )

            // Map lyric sections to saved Krithi sections by order index
            if (savedSections.isNotEmpty() && variant.sections.isNotEmpty()) {
                val sectionPairs = variant.sections.mapNotNull { lyricSection ->
                    val matchingSection = savedSections.find {
                        it.orderIndex == lyricSection.sectionOrder - 1
                    }
                    matchingSection?.let { it.id.toJavaUuid() to lyricSection.text }
                }
                if (sectionPairs.isNotEmpty()) {
                    dal.krithis.saveLyricVariantSections(lyricVariant.id, sectionPairs)
                }
            }
        }
        logger.info("Persisted ${extraction.lyricVariants.size} variants for Krithi $krithiId from match $matchId")
    }

    /**
     * Get the match report for a given extraction.
     */
    suspend fun getMatchReport(extractionId: Uuid): VariantMatchReportDto =
        dal.variantMatch.getReport(extractionId)

    /**
     * List matches for an extraction.
     */
    suspend fun listMatches(
        extractionId: Uuid,
        status: List<String>? = null,
        page: Int = 1,
        pageSize: Int = 50,
    ): com.sangita.grantha.shared.domain.model.PaginatedResponse<VariantMatchDto> {
        val offset = (page - 1) * pageSize
        val (items, total) = dal.variantMatch.listByExtraction(extractionId, status, pageSize, offset)
        return com.sangita.grantha.shared.domain.model.PaginatedResponse(items, total, page, pageSize)
    }

    /**
     * List all pending matches across all extractions.
     */
    suspend fun listPendingMatches(
        page: Int = 1,
        pageSize: Int = 50,
    ): com.sangita.grantha.shared.domain.model.PaginatedResponse<VariantMatchDto> {
        val offset = (page - 1) * pageSize
        val (items, total) = dal.variantMatch.listPending(pageSize, offset)
        return com.sangita.grantha.shared.domain.model.PaginatedResponse(items, total, page, pageSize)
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private data class ScoredCandidate(
        val krithiId: Uuid,
        val score: Double,
        val signals: MatchSignals,
    )

    private data class MatchSignals(
        val titleScore: Double,
        val ragaTalaScore: Double,
        val pagePositionScore: Double,
    )

    /**
     * Compute Levenshtein similarity (0.0 = completely different, 1.0 = identical).
     */
    private fun levenshteinSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        val distance = levenshteinDistance(a, b)
        return 1.0 - distance.toDouble() / maxLen
    }

    private fun levenshteinDistance(a: String, b: String): Int {
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
