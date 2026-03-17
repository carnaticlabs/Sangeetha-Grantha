package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionMethod
import com.sangita.grantha.shared.domain.model.import.CanonicalSectionType
import com.sangita.grantha.shared.domain.model.RagaSectionDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * Result of processing a single extraction.
 */
data class ExtractionProcessingResult(
    val krithiId: Uuid,
    val wasCreated: Boolean,
)

/**
 * Handles matching a single CanonicalExtractionDto to an existing Krithi or creating a new one,
 * plus persisting source evidence and lyric variants.
 *
 * Extracted from ExtractionResultProcessor (TRACK-075).
 */
class KrithiMatcherService(
    private val dal: SangitaDal,
    private val normalizer: NameNormalizationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Process a single extraction result: match to an existing Krithi or create a new one.
     * Returns the Krithi ID and whether it was newly created, or null if processing failed.
     */
    suspend fun processExtractionResult(
        extraction: CanonicalExtractionDto,
        extractionTaskId: Uuid,
        skipPendingImportCreation: Boolean = false,
    ): ExtractionProcessingResult? {
        val titleNormalized = normalizeExtractionTitle(extraction) ?: return null

        // 1. Resolve Composer and Raga IDs if possible
        val composerNormalized = normalizer.normalizeComposer(extraction.composer)
        val resolvedComposer = composerNormalized?.let { dal.composers.findByNameNormalized(it) }
        val composerId = resolvedComposer?.id?.toJavaUuid()

        val ragaNormalized = extraction.ragas.firstOrNull()
            ?.let { normalizer.normalizeRaga(it.name) }
            ?.takeUnless { isPlaceholderRagaNormalized(it) }
        val resolvedRaga = ragaNormalized?.let { dal.ragas.findByNameNormalized(it) }
        val ragaId = resolvedRaga?.id?.toJavaUuid()

        // 2. Fetch candidates from multiple strategies and merge
        // A. Broad metadata search (all krithis for this composer+raga combo)
        val candidatesByMetadata = if (composerId != null) {
            dal.krithiSearch.findCandidatesByMetadata(composerId, ragaId)
        } else {
            emptyList()
        }

        // B. Compressed title match (catches spacing variants across any composer)
        val candidatesByTitle = dal.krithiSearch.findDuplicateCandidates(titleNormalized)
        val candidatesByNearTitle = dal.krithiSearch.findNearTitleCandidates(titleNormalized)

        // C. Title match narrowed by composer (high-confidence combo)
        val candidatesByTitleAndComposer = if (composerId != null) {
            dal.krithiSearch.findDuplicateCandidates(titleNormalized, composerId, ragaId)
        } else {
            emptyList()
        }

        // Merge and deduplicate candidates by ID.
        val allCandidates =
            (candidatesByTitleAndComposer + candidatesByMetadata + candidatesByTitle + candidatesByNearTitle)
                .distinctBy { it.id }

        if (allCandidates.isEmpty()) {
            if (skipPendingImportCreation) {
                logger.info("No match for '${extraction.title}' but skipping pending import creation (import already exists)")
                return null
            }
            return createPendingImport(extraction, titleNormalized)
        }

        val evidenceCounts = dal.sourceEvidence.countByKrithiIds(allCandidates.map { it.id })
        val unknownRagaId = dal.ragas.findByNameNormalized("unknown")?.id?.toJavaUuid()
        data class CandidateEvaluation(
            val candidate: com.sangita.grantha.shared.domain.model.KrithiDto,
            val score: Int,
            val isMetadataMatch: Boolean,
            val hasKnownRaga: Boolean,
            val evidenceCount: Int,
        )

        // 3. Score Candidates
        val evaluations = allCandidates
            .map { candidate ->
                val titleScore = NameNormalizationService.ratio(titleNormalized, candidate.titleNormalized)
                val compressedScore = NameNormalizationService.ratio(
                    titleNormalized.replace(" ", ""),
                    candidate.titleNormalized.replace(" ", "")
                )
                val bestTitleScore = maxOf(titleScore, compressedScore)

                val dbComposerId = candidate.composerId.toJavaUuid()
                val dbRagaId = candidate.primaryRagaId?.toJavaUuid()

                val composerMatch = (composerId != null && dbComposerId == composerId)
                val ragaMatch = (ragaId == null) || (dbRagaId == ragaId)

                val isMetadataMatch = composerMatch && ragaMatch
                val hasKnownRaga = candidate.primaryRagaId
                    ?.toJavaUuid()
                    ?.let { candidateRagaId ->
                        unknownRagaId == null || candidateRagaId != unknownRagaId
                    }
                    ?: false
                CandidateEvaluation(
                    candidate = candidate,
                    score = bestTitleScore,
                    isMetadataMatch = isMetadataMatch,
                    hasKnownRaga = hasKnownRaga,
                    evidenceCount = evidenceCounts[candidate.id] ?: 0,
                )
            }
            .filter { evaluation ->
                val candidateNorm = evaluation.candidate.titleNormalized
                val minLen = minOf(titleNormalized.length, candidateNorm.length)
                val maxLen = maxOf(titleNormalized.length, candidateNorm.length)
                if (maxLen > 0 && minLen.toDouble() / maxLen < 0.7) return@filter false

                val score = evaluation.score
                val isMetadataMatch = evaluation.isMetadataMatch
                if (isMetadataMatch) {
                    score > 88
                } else {
                    score > 92
                }
            }
        val maxScore = evaluations.maxOfOrNull { it.score }
        val bestMatch = if (maxScore == null) {
            null
        } else {
            val shortlist = evaluations
                .asSequence()
                .filter { (maxScore - it.score) <= 5 }
            val bestEvaluation = if (ragaId != null) {
                shortlist.maxWithOrNull(
                    compareByDescending<CandidateEvaluation> { it.isMetadataMatch }
                        .thenByDescending { it.score }
                        .thenByDescending { it.hasKnownRaga }
                )
            } else {
                shortlist.maxWithOrNull(
                    compareByDescending<CandidateEvaluation> { it.hasKnownRaga }
                        .thenByDescending { it.score }
                        .thenByDescending { it.isMetadataMatch }
                )
            }
            bestEvaluation?.candidate
        }

        if (bestMatch == null) {
            if (skipPendingImportCreation) {
                logger.info("No match for '${extraction.title}' but skipping pending import creation (import already exists)")
                return null
            }
            return createPendingImport(extraction, titleNormalized)
        }

        val matched = bestMatch
        logger.info("Matched extraction '${extraction.title}' to existing Krithi '${matched.title}' " +
            "(Score: High, RagaMatch: ${ragaId != null && matched.primaryRagaId?.toJavaUuid() == ragaId})")

        // If this source brings a new script/language variant, persist it on the matched Krithi.
        persistMissingLyricVariants(matched.id, extraction)

        // Determine contributed fields
        val contributedFields = buildContributedFields(extraction)

        // Build the extraction method string
        val extractionMethod = extraction.extractionMethod.name

        // Build raw extraction JSON for audit/replay
        val rawExtractionJson = json.encodeToString(CanonicalExtractionDto.serializer(), extraction)

        // Create source evidence record
        dal.sourceEvidence.createEvidence(
            krithiId = matched.id,
            sourceUrl = extraction.sourceUrl,
            sourceName = extraction.sourceName,
            sourceTier = extraction.sourceTier,
            sourceFormat = mapExtractionMethodToFormat(extraction.extractionMethod),
            extractionMethod = extractionMethod,
            pageRange = extraction.pageRange,
            checksum = extraction.checksum,
            confidence = null,
            contributedFields = contributedFields,
            rawExtraction = rawExtractionJson,
        )

        // Log to audit
        val auditMetadata = buildJsonObject {
            put("sourceUrl", extraction.sourceUrl)
            put("sourceName", extraction.sourceName)
            put("extractionTaskId", extractionTaskId.toString())
        }
        dal.auditLogs.append(
            action = "CREATE_SOURCE_EVIDENCE",
            entityTable = "krithi_source_evidence",
            entityId = matched.id,
            metadata = auditMetadata.toString(),
        )

        return ExtractionProcessingResult(krithiId = matched.id, wasCreated = false)
    }

    fun normalizeExtractionTitle(extraction: CanonicalExtractionDto): String? {
        val primary = normalizer.normalizeTitle(extraction.title)?.takeIf { it.isNotBlank() }
        if (primary != null) return primary
        return extraction.alternateTitle
            ?.let { normalizer.normalizeTitle(it) }
            ?.takeIf { it.isNotBlank() }
    }

    fun isPlaceholderRagaNormalized(normalized: String): Boolean {
        val value = normalized.trim().lowercase()
        return value.isBlank() || value in setOf("unknown", "na", "n a", "none")
    }

    suspend fun persistMissingLyricVariants(
        krithiId: Uuid,
        extraction: CanonicalExtractionDto,
    ) {
        if (extraction.lyricVariants.isEmpty()) return

        val existingKeys = dal.krithiLyrics.getLyricVariants(krithiId)
            .map { "${it.variant.language.name}:${it.variant.script.name}" }
            .toSet()
            .toMutableSet()
        val savedSections = dal.krithis.getSections(krithiId)

        for (variant in extraction.lyricVariants) {
            val language = runCatching { LanguageCode.valueOf(variant.language.uppercase()) }.getOrNull()
            val script = runCatching { ScriptCode.valueOf(variant.script.uppercase()) }.getOrNull()
            if (language == null || script == null) continue

            val variantKey = "${language.name}:${script.name}"
            if (variantKey in existingKeys) continue

            val fullLyrics = variant.sections
                .sortedBy { it.sectionOrder }
                .joinToString("\n\n") { it.text }
            if (fullLyrics.isBlank()) continue

            val lyricVariant = dal.krithiLyrics.createLyricVariant(
                krithiId = krithiId,
                language = language,
                script = script,
                lyrics = fullLyrics,
                isPrimary = false,
                sourceReference = extraction.sourceUrl,
            )

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

            existingKeys.add(variantKey)
            logger.info("Added missing lyric variant {} for matched Krithi {}", variantKey, krithiId)
        }
    }

    /**
     * When no existing Krithi matches, create a PENDING imported_krithis record
     * for manual curator review instead of auto-creating a new Krithi.
     */
    private suspend fun createPendingImport(
        extraction: CanonicalExtractionDto,
        titleNormalized: String,
    ): ExtractionProcessingResult? {
        try {
            val sourceId = getOrCreateUnmatchedImportSourceId()
            val sourceKey = "${extraction.sourceUrl}::${extraction.title}"
            val parsedPayload = json.encodeToString(CanonicalExtractionDto.serializer(), extraction)

            dal.imports.createImport(
                sourceId = sourceId,
                sourceKey = sourceKey,
                rawTitle = extraction.title,
                rawLyrics = null,
                rawComposer = extraction.composer,
                rawRaga = extraction.ragas.firstOrNull()?.name,
                rawTala = extraction.tala,
                rawDeity = extraction.deity,
                rawTemple = extraction.temple,
                rawLanguage = extraction.lyricVariants.firstOrNull()?.language,
                parsedPayload = parsedPayload,
            )
            logger.info("No match for '${extraction.title}' (normalised: '$titleNormalized'), " +
                "created pending import for manual review")
        } catch (e: Exception) {
            logger.error("Failed to create pending import for '${extraction.title}': ${e.message}", e)
        }
        return null
    }

    private var cachedUnmatchedSourceId: UUID? = null

    private suspend fun getOrCreateUnmatchedImportSourceId(): UUID {
        cachedUnmatchedSourceId?.let { return it }
        val source = dal.sourceRegistry.findByName("PDF Extraction (Unmatched)")
            ?: throw IllegalStateException(
                "Import source 'PDF Extraction (Unmatched)' not found. Run 'make seed' to create it."
            )
        val javaId = source.id.toJavaUuid()
        cachedUnmatchedSourceId = javaId
        return javaId
    }

    fun buildContributedFields(extraction: CanonicalExtractionDto): List<String> {
        val fields = mutableListOf<String>()
        fields.add("title")
        if (extraction.ragas.isNotEmpty()) fields.add("raga")
        fields.add("tala")
        fields.add("composer")
        if (extraction.sections.isNotEmpty()) fields.add("sections")
        if (extraction.deity != null) fields.add("deity")
        if (extraction.temple != null) fields.add("temple")
        extraction.lyricVariants.forEach { variant ->
            fields.add("lyrics_${variant.language}")
        }
        return fields
    }

    fun mapExtractionMethodToFormat(method: CanonicalExtractionMethod): String = when (method) {
        CanonicalExtractionMethod.PDF_PYMUPDF, CanonicalExtractionMethod.PDF_OCR -> "PDF"
        CanonicalExtractionMethod.HTML_JSOUP, CanonicalExtractionMethod.HTML_JSOUP_GEMINI -> "HTML"
        CanonicalExtractionMethod.DOCX_PYTHON -> "DOCX"
        CanonicalExtractionMethod.MANUAL -> "MANUAL"
        CanonicalExtractionMethod.TRANSLITERATION -> "HTML"
    }

    fun mapCanonicalSectionType(type: CanonicalSectionType): RagaSectionDto = when (type) {
        CanonicalSectionType.PALLAVI -> RagaSectionDto.PALLAVI
        CanonicalSectionType.ANUPALLAVI -> RagaSectionDto.ANUPALLAVI
        CanonicalSectionType.CHARANAM -> RagaSectionDto.CHARANAM
        CanonicalSectionType.SAMASHTI_CHARANAM -> RagaSectionDto.SAMASHTI_CHARANAM
        CanonicalSectionType.CHITTASWARAM -> RagaSectionDto.CHITTASWARAM
        CanonicalSectionType.SWARA_SAHITYA -> RagaSectionDto.SWARA_SAHITYA
        CanonicalSectionType.MADHYAMA_KALA -> RagaSectionDto.MADHYAMA_KALA
        CanonicalSectionType.OTHER -> RagaSectionDto.OTHER
    }
}
