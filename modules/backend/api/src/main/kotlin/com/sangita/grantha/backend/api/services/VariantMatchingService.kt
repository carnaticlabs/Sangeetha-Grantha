package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.support.toJavaUuid
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
 * confidence scoring. Scoring logic is delegated to [VariantScorer].
 *
 * Matches with confidence >= 0.85 are auto-approved. Lower confidence matches
 * require manual review via the variant match review UI.
 */
class VariantMatchingService(
    private val dal: SangitaDal,
    private val normalizer: NameNormalizationService,
    private val scorer: VariantScorer,
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

        // Search for candidate Krithis: exact match first, then fuzzy fallback
        var candidates = dal.krithiSearch.findDuplicateCandidates(
            titleNormalized = titleNormalized,
        )

        // TRACK-059-fix: If exact match fails, try near-title fuzzy search.
        // Sanskrit/Devanagari titles may normalize differently from English/IAST titles.
        if (candidates.isEmpty()) {
            candidates = dal.krithiSearch.findNearTitleCandidates(titleNormalized)
                .filter { candidate ->
                    // Apply length-ratio guard and minimum fuzzy score
                    val minLen = minOf(titleNormalized.length, candidate.titleNormalized.length)
                    val maxLen = maxOf(titleNormalized.length, candidate.titleNormalized.length)
                    if (maxLen > 0 && minLen.toDouble() / maxLen < 0.7) return@filter false
                    val score = NameNormalizationService.ratio(titleNormalized, candidate.titleNormalized)
                    score > 85
                }
            if (candidates.isNotEmpty()) {
                logger.info("Fuzzy fallback found ${candidates.size} candidates for variant '${extraction.title}'")
            }
        }

        if (candidates.isEmpty()) {
            logger.info("No Krithi match for variant '${extraction.title}' (normalised: '$titleNormalized')")
            return
        }

        // Score each candidate via VariantScorer
        val scoredCandidates = mutableListOf<VariantScorer.ScoredCandidate>()
        for (candidate in candidates) {
            val signals = scorer.computeMatchSignals(extraction, candidate, primaryKrithiIds, positionIndex, titleNormalized)
            val compositeScore = signals.titleScore * WEIGHT_TITLE +
                signals.ragaTalaScore * WEIGHT_RAGA_TALA +
                signals.pagePositionScore * WEIGHT_PAGE_POSITION
            scoredCandidates.add(VariantScorer.ScoredCandidate(candidate.id, compositeScore, signals))
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

        val candidateKrithi = candidates.first { it.id == best.krithiId }
        val structureMismatch = scorer.checkStructureMismatch(extraction, candidateKrithi)

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
     * Get Krithi IDs that were linked from a primary extraction via source evidence.
     */
    private suspend fun getPrimaryExtractionKrithiIds(extractionId: Uuid): Set<Uuid> {
        val detail = dal.extractionQueue.findById(extractionId) ?: return emptySet()
        val resultPayload = detail.resultPayload ?: return emptySet()

        return try {
            val extractions = json.decodeFromString<List<CanonicalExtractionDto>>(resultPayload)
            extractions.mapNotNull { extraction ->
                val titleNormalized = normalizer.normalizeTitle(extraction.title) ?: return@mapNotNull null
                val candidates = dal.krithiSearch.findDuplicateCandidates(titleNormalized = titleNormalized)
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

            val lyricVariant = dal.krithiLyrics.createLyricVariant(
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
                    dal.krithiLyrics.saveLyricVariantSections(lyricVariant.id, sectionPairs)
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

}
