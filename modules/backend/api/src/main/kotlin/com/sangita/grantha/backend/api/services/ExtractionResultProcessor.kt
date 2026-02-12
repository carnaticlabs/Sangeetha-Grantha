package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.services.scraping.StructuralVotingEngine
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionMethod
import com.sangita.grantha.shared.domain.model.import.CanonicalSectionType
import com.sangita.grantha.shared.domain.model.RagaSectionDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

/**
 * TRACK-041 / TRACK-053: Processes completed extraction queue results from the Python extraction service.
 *
 * Flow:
 * 1. Polls extraction_queue for DONE items
 * 2. Parses result_payload as List<CanonicalExtractionDto>
 * 3. Matches each extraction to an existing Krithi (by normalised title + composer)
 * 4. If no match is found, creates a new Krithi via KrithiCreationFromExtractionService
 * 5. Creates krithi_source_evidence records
 * 6. For Krithis with multiple sources, runs structural voting
 * 7. Marks extraction queue items as INGESTED
 */
class ExtractionResultProcessor(
    private val dal: SangitaDal,
    private val normalizer: NameNormalizationService,
    private val krithiCreationService: KrithiCreationFromExtractionService,
    private val variantMatchingService: VariantMatchingService? = null,
    private val votingEngine: StructuralVotingEngine = StructuralVotingEngine(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class ProcessingReport(
        val totalTasks: Int,
        val processedTasks: Int,
        val skippedTasks: Int,
        val errorTasks: Int,
        val evidenceRecordsCreated: Int,
        val krithisCreated: Int,
        val krithisMatched: Int,
        val affectedKrithiCount: Int,
        val votingDecisionsMade: Int,
        val errors: List<String> = emptyList(),
    )

    /**
     * Process all DONE extraction queue items.
     * Idempotent: skips items that already have evidence records for the same source URL + Krithi.
     */
    suspend fun processCompletedExtractions(batchSize: Int = 50): ProcessingReport {
        val (doneTasks, totalDone) = dal.extractionQueue.list(
            status = listOf("DONE"),
            limit = batchSize,
        )
        logger.info("Found $totalDone DONE extraction tasks, processing up to $batchSize")

        var processedTasks = 0
        var skippedTasks = 0
        var errorTasks = 0
        var evidenceCreated = 0
        var krithisCreated = 0
        var krithisMatched = 0
        var votingDecisions = 0
        val affectedKrithiIds = mutableSetOf<Uuid>()
        val errors = mutableListOf<String>()

        for (task in doneTasks) {
            try {
                val detail = dal.extractionQueue.findById(task.id) ?: run {
                    skippedTasks++
                    continue
                }
                val resultPayload = detail.resultPayload
                if (resultPayload.isNullOrBlank() || resultPayload == "null") {
                    logger.warn("Extraction ${task.id} has no result payload, skipping")
                    skippedTasks++
                    continue
                }

                val extractions = try {
                    json.decodeFromString<List<CanonicalExtractionDto>>(resultPayload)
                } catch (e: Exception) {
                    logger.error("Failed to parse result payload for extraction ${task.id}: ${e.message}")
                    errors.add("Parse error for ${task.id}: ${e.message}")
                    errorTasks++
                    continue
                }

                if (extractions.isEmpty()) {
                    logger.info("Extraction ${task.id} produced zero results, marking as ingested")
                    dal.extractionQueue.markIngested(task.id)
                    processedTasks++
                    continue
                }

                // TRACK-056: Branch on extraction intent
                val isEnrich = detail.extractionIntent == "ENRICH"
                if (isEnrich && variantMatchingService != null) {
                    val relatedId = detail.relatedExtractionId
                    val report = variantMatchingService.matchVariants(
                        extractions = extractions,
                        enrichExtractionId = task.id,
                        relatedExtractionId = relatedId,
                    )
                    krithisMatched += report.totalMatches
                    dal.extractionQueue.markIngested(task.id)
                    processedTasks++
                    logger.info("Processed ENRICH extraction ${task.id}: ${report.totalMatches} variant matches " +
                        "(${report.autoApproved} auto-approved)")
                    continue
                }

                var taskEvidenceCount = 0
                for (extraction in extractions) {
                    val result = processExtractionResult(extraction, task.id)
                    if (result != null) {
                        taskEvidenceCount++
                        affectedKrithiIds.add(result.krithiId)
                        if (result.wasCreated) krithisCreated++ else krithisMatched++
                    }
                }

                evidenceCreated += taskEvidenceCount
                dal.extractionQueue.markIngested(task.id)
                processedTasks++
                logger.info("Processed extraction ${task.id}: $taskEvidenceCount evidence records created")
            } catch (e: Exception) {
                logger.error("Error processing extraction ${task.id}", e)
                errors.add("Error for ${task.id}: ${e.message}")
                errorTasks++
            }
        }

        // Run structural voting for affected Krithis with multiple sources
        for (krithiId in affectedKrithiIds) {
            try {
                val voted = runVotingForKrithi(krithiId)
                if (voted) votingDecisions++
            } catch (e: Exception) {
                logger.warn("Voting failed for Krithi $krithiId: ${e.message}")
            }
        }

        val report = ProcessingReport(
            totalTasks = totalDone,
            processedTasks = processedTasks,
            skippedTasks = skippedTasks,
            errorTasks = errorTasks,
            evidenceRecordsCreated = evidenceCreated,
            krithisCreated = krithisCreated,
            krithisMatched = krithisMatched,
            affectedKrithiCount = affectedKrithiIds.size,
            votingDecisionsMade = votingDecisions,
            errors = errors,
        )
        logger.info("Processing complete: $report")
        return report
    }

    /**
     * Result of processing a single extraction.
     */
    private data class ExtractionProcessingResult(
        val krithiId: Uuid,
        val wasCreated: Boolean,
    )

    /**
     * Process a single extraction result: match to an existing Krithi or create a new one.
     * Returns the Krithi ID and whether it was newly created, or null if processing failed.
     */
    private suspend fun processExtractionResult(
        extraction: CanonicalExtractionDto,
        extractionTaskId: Uuid,
    ): ExtractionProcessingResult? {
        val titleNormalized = normalizer.normalizeTitle(extraction.title) ?: return null
        
        // 1. Resolve Composer and Raga IDs if possible
        val composerNormalized = normalizer.normalizeComposer(extraction.composer)
        val resolvedComposer = composerNormalized?.let { dal.composers.findByNameNormalized(it) }
        val composerId = resolvedComposer?.id?.toJavaUuid()

        val ragaNormalized = extraction.ragas.firstOrNull()?.let { normalizer.normalizeRaga(it.name) }
        val resolvedRaga = ragaNormalized?.let { dal.ragas.findByNameNormalized(it) }
        val ragaId = resolvedRaga?.id?.toJavaUuid()

        // 2. Fetch candidates from multiple strategies and merge
        // A. Broad metadata search (all krithis for this composer+raga combo)
        val candidatesByMetadata = if (composerId != null) {
            dal.krithis.findCandidatesByMetadata(composerId, ragaId)
        } else {
            emptyList()
        }

        // B. Compressed title match (catches spacing variants across any composer)
        val candidatesByTitle = dal.krithis.findDuplicateCandidates(titleNormalized)

        // C. Title match narrowed by composer (high-confidence combo)
        val candidatesByTitleAndComposer = if (composerId != null) {
            dal.krithis.findDuplicateCandidates(titleNormalized, composerId, ragaId)
        } else {
            emptyList()
        }

        // Merge and deduplicate candidates by ID
        val allCandidates = (candidatesByTitleAndComposer + candidatesByMetadata + candidatesByTitle).distinctBy { it.id }

        if (allCandidates.isEmpty()) {
            return createNewKrithi(extraction, titleNormalized, extractionTaskId)
        }

        // 3. Score Candidates — compare both raw and compressed (space-removed) titles
        val bestMatch = allCandidates
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
                val ragaMatch = (ragaId != null && dbRagaId == ragaId)

                val isMetadataMatch = composerMatch && ragaMatch
                candidate to (bestTitleScore to isMetadataMatch)
            }
            .filter { (_, scorePair) ->
                val (score, isMetadataMatch) = scorePair
                if (isMetadataMatch) {
                    score > 75 // Lower threshold if Raga+Composer match (handles typos like "Ananada")
                } else {
                    score > 85 // Higher threshold if only title matches
                }
            }
            .maxByOrNull { it.second.first }
            ?.first

        if (bestMatch == null) {
            return createNewKrithi(extraction, titleNormalized, extractionTaskId)
        }

        val matched = bestMatch
        logger.info("Matched extraction '${extraction.title}' to existing Krithi '${matched.title}' " +
            "(Score: High, RagaMatch: ${ragaId != null && matched.primaryRagaId?.toJavaUuid() == ragaId})")

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
            confidence = null, // Will be computed by quality scoring
            contributedFields = contributedFields,
            rawExtraction = rawExtractionJson,
        )

        // Log to audit
        dal.auditLogs.append(
            action = "CREATE_SOURCE_EVIDENCE",
            entityTable = "krithi_source_evidence",
            entityId = matched.id,
            metadata = """{"sourceUrl":"${extraction.sourceUrl}","sourceName":"${extraction.sourceName}","extractionTaskId":"$extractionTaskId"}""",
        )

        return ExtractionProcessingResult(krithiId = matched.id, wasCreated = false)
    }

    private suspend fun createNewKrithi(
        extraction: CanonicalExtractionDto,
        titleNormalized: String,
        extractionTaskId: Uuid
    ): ExtractionProcessingResult? {
        // TRACK-053: No existing Krithi matches — create a new one
        logger.info("No Krithi match for '${extraction.title}' (normalised: '$titleNormalized'), creating new Krithi")
        val createdId = try {
            krithiCreationService.createFromExtraction(extraction, extractionTaskId)
        } catch (e: Exception) {
            logger.error("Failed to create Krithi for '${extraction.title}': ${e.message}", e)
            null
        }
        return createdId?.let { ExtractionProcessingResult(krithiId = it, wasCreated = true) }
    }

    /**
     * Run structural voting for a Krithi that has multiple source evidence records.
     */
    private suspend fun runVotingForKrithi(krithiId: Uuid): Boolean {
        val evidence = dal.sourceEvidence.getKrithiEvidence(krithiId) ?: return false
        if (evidence.sources.size < 2) return false

        // Build voting candidates from source evidence
        val candidates = evidence.sources.mapNotNull { source ->
            val rawExtraction = source.rawExtraction ?: return@mapNotNull null
            try {
                val extraction = json.decodeFromString<CanonicalExtractionDto>(rawExtraction)
                StructuralVotingEngine.SectionCandidate(
                    sections = extraction.sections.map { section ->
                        ScrapedSectionDto(
                            type = mapCanonicalSectionType(section.type),
                            text = section.label ?: section.type.name,
                            label = section.label,
                        )
                    },
                    isAuthoritySource = source.sourceTier <= 2,
                    label = source.sourceName,
                )
            } catch (e: Exception) {
                logger.warn("Failed to parse extraction for voting: ${e.message}")
                null
            }
        }

        if (candidates.size < 2) return false

        val bestStructure = votingEngine.pickBestStructure(candidates)
        if (bestStructure.isEmpty()) return false

        // Determine consensus type
        val allSame = candidates.all { candidate ->
            candidate.sections.map { it.type } == bestStructure.map { it.type }
        }
        val consensusType = when {
            allSame -> "UNANIMOUS"
            candidates.any { it.isAuthoritySource } -> "AUTHORITY_OVERRIDE"
            else -> "MAJORITY"
        }

        // Build participating sources JSON
        val participatingJson = json.encodeToString(
            ListSerializer(String.serializer()),
            evidence.sources.map { it.sourceName },
        )

        // Build consensus structure JSON
        val consensusJson = json.encodeToString(
            ListSerializer(String.serializer()),
            bestStructure.map { "${it.type.name}:${it.label ?: ""}" },
        )

        // Build dissenting sources
        val dissentingSources = candidates.filter { candidate ->
            candidate.sections.map { it.type } != bestStructure.map { it.type }
        }
        val dissentingJson = json.encodeToString(
            ListSerializer(String.serializer()),
            dissentingSources.mapNotNull { it.label },
        )

        // Determine confidence
        val confidence = when {
            allSame -> "HIGH"
            candidates.count { c -> c.sections.map { it.type } == bestStructure.map { it.type } } > candidates.size / 2 -> "MEDIUM"
            else -> "LOW"
        }

        dal.structuralVoting.createVotingRecord(
            krithiId = krithiId,
            participatingSources = participatingJson,
            consensusStructure = consensusJson,
            consensusType = consensusType,
            confidence = confidence,
            dissentingSources = dissentingJson,
        )

        dal.auditLogs.append(
            action = "STRUCTURAL_VOTING_DECISION",
            entityTable = "structural_vote_log",
            entityId = krithiId,
            metadata = """{"consensusType":"$consensusType","confidence":"$confidence","sourceCount":${evidence.sources.size}}""",
        )

        return true
    }

    private fun buildContributedFields(extraction: CanonicalExtractionDto): List<String> {
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

    private fun mapExtractionMethodToFormat(method: CanonicalExtractionMethod): String = when (method) {
        CanonicalExtractionMethod.PDF_PYMUPDF, CanonicalExtractionMethod.PDF_OCR -> "PDF"
        CanonicalExtractionMethod.HTML_JSOUP, CanonicalExtractionMethod.HTML_JSOUP_GEMINI -> "HTML"
        CanonicalExtractionMethod.DOCX_PYTHON -> "DOCX"
        CanonicalExtractionMethod.MANUAL -> "MANUAL"
        CanonicalExtractionMethod.TRANSLITERATION -> "HTML"
    }

    private fun mapCanonicalSectionType(type: CanonicalSectionType): RagaSectionDto = when (type) {
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
