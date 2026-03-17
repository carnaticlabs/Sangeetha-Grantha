package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

/**
 * TRACK-041 / TRACK-053: Processes completed extraction queue results from the Python extraction service.
 *
 * Flow:
 * 1. Polls extraction_queue for DONE items
 * 2. Parses result_payload as List<CanonicalExtractionDto>
 * 3. Delegates matching/creation to KrithiMatcherService
 * 4. Delegates structural voting to StructuralVotingProcessor
 * 5. Marks extraction queue items as INGESTED
 */
class ExtractionResultProcessor(
    private val dal: SangitaDal,
    private val krithiMatcherService: KrithiMatcherService,
    private val structuralVotingProcessor: StructuralVotingProcessor,
    private val variantMatchingService: VariantMatchingService? = null,
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
                // TRACK-059-fix: Atomically claim the task to prevent concurrent processing.
                // claimForIngestion transitions DONE → INGESTED; only the winner proceeds.
                val claimed = dal.extractionQueue.claimForIngestion(task.id)
                if (!claimed) {
                    logger.info("Task ${task.id} already claimed by another processor, skipping")
                    skippedTasks++
                    continue
                }

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
                    logger.info("Extraction ${task.id} produced zero results, already marked ingested")
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
                    processedTasks++
                    logger.info("Processed ENRICH extraction ${task.id}: ${report.totalMatches} variant matches " +
                        "(${report.autoApproved} auto-approved)")
                    continue
                }

                // TRACK-059-fix: Deduplicate extractions within the batch by normalized title
                // The PDF segmenter can produce duplicate entries for the same composition on the same page.
                val seenTitles = mutableSetOf<String>()
                val dedupedExtractions = extractions.filter { extraction ->
                    val titleNorm = krithiMatcherService.normalizeExtractionTitle(extraction)
                    if (titleNorm == null) true // let downstream handle nulls
                    else seenTitles.add(titleNorm) // add returns false if already present
                }
                if (dedupedExtractions.size < extractions.size) {
                    logger.info("Deduped ${extractions.size - dedupedExtractions.size} duplicate extractions " +
                        "within batch for task ${task.id}")
                }

                // Check if this extraction task originated from a CSV bulk import (has importId).
                // If so, enrich the existing imported_krithis record instead of creating a new one.
                val importId = parseImportIdFromRequestPayload(detail.requestPayload)

                var taskEvidenceCount = 0
                for (extraction in dedupedExtractions) {
                    // When an importId is present, enrich the existing import record with extraction data
                    if (importId != null) {
                        enrichExistingImport(importId, extraction, task.id)
                    }

                    val result = krithiMatcherService.processExtractionResult(
                        extraction, task.id, skipPendingImportCreation = importId != null
                    )
                    if (result != null) {
                        linkImportsForSourceUrl(
                            sourceUrl = extraction.sourceUrl,
                            krithiId = result.krithiId,
                            extractionTaskId = task.id,
                        )
                        taskEvidenceCount++
                        affectedKrithiIds.add(result.krithiId)
                        if (result.wasCreated) krithisCreated++ else krithisMatched++
                    }
                }

                evidenceCreated += taskEvidenceCount
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
                val voted = structuralVotingProcessor.runVotingForKrithi(krithiId)
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
     * Parse the importId from the extraction task's request_payload JSON.
     * Returns null if not present (e.g., standalone PDF extractions).
     */
    private fun parseImportIdFromRequestPayload(requestPayload: String?): Uuid? {
        if (requestPayload.isNullOrBlank()) return null
        return try {
            val obj = json.decodeFromString<JsonObject>(requestPayload)
            obj["importId"]?.jsonPrimitive?.content?.let { Uuid.parse(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Enrich an existing imported_krithis record with extraction results.
     * This is called when the extraction was triggered by a CSV bulk import (importId in request_payload).
     * The existing record already has CSV-provided metadata; we add the parsed extraction payload
     * and update any fields that the extraction discovered (title, raga, tala, etc.).
     */
    private suspend fun enrichExistingImport(
        importId: Uuid,
        extraction: CanonicalExtractionDto,
        extractionTaskId: Uuid,
    ) {
        try {
            val existing = dal.imports.findById(importId)
            if (existing == null) {
                logger.warn("Import $importId from request_payload not found, skipping enrichment")
                return
            }

            val parsedPayload = json.encodeToString(CanonicalExtractionDto.serializer(), extraction)

            // Enrich: extraction data fills in gaps, but doesn't overwrite CSV-provided values
            dal.imports.enrichWithExtraction(
                id = importId,
                rawTitle = if (existing.rawTitle.isNullOrBlank()) extraction.title else null,
                rawComposer = if (existing.rawComposer.isNullOrBlank()) extraction.composer else null,
                rawRaga = if (existing.rawRaga.isNullOrBlank()) extraction.ragas.firstOrNull()?.name else null,
                rawTala = if (existing.rawTala.isNullOrBlank()) extraction.tala else null,
                rawDeity = if (existing.rawDeity.isNullOrBlank()) extraction.deity else null,
                rawTemple = if (existing.rawTemple.isNullOrBlank()) extraction.temple else null,
                rawLanguage = if (existing.rawLanguage.isNullOrBlank()) extraction.lyricVariants.firstOrNull()?.language else null,
                parsedPayload = parsedPayload,
                status = ImportStatus.IN_REVIEW,
            )

            val auditMetadata = buildJsonObject {
                put("importId", importId.toString())
                put("extractionTaskId", extractionTaskId.toString())
                put("sourceUrl", extraction.sourceUrl)
            }
            dal.auditLogs.append(
                action = "ENRICH_IMPORT_FROM_EXTRACTION",
                entityTable = "imported_krithis",
                entityId = importId,
                metadata = auditMetadata.toString(),
            )

            logger.info("Enriched existing import $importId with extraction from ${extraction.sourceUrl}")
        } catch (e: Exception) {
            logger.error("Failed to enrich import $importId: ${e.message}", e)
        }
    }

    suspend fun linkImportsForSourceUrl(
        sourceUrl: String,
        krithiId: Uuid,
        extractionTaskId: Uuid,
    ) {
        val importsToLink = dal.imports.findBySourceKey(
            sourceKey = sourceUrl,
            statuses = listOf(ImportStatus.PENDING, ImportStatus.IN_REVIEW),
        )
        if (importsToLink.isEmpty()) return

        var linked = 0
        importsToLink.forEach { import ->
            val alreadyLinkedToSameKrithi =
                import.importStatus == ImportStatusDto.MAPPED && import.mappedKrithiId == krithiId
            if (alreadyLinkedToSameKrithi) return@forEach

            val updated = dal.imports.reviewImport(
                id = import.id,
                status = ImportStatus.MAPPED,
                mappedKrithiId = krithiId.toJavaUuid(),
                reviewerNotes = "Auto-linked by extraction task $extractionTaskId",
            ) ?: return@forEach

            val linkMetadata = buildJsonObject {
                put("sourceUrl", sourceUrl)
                put("krithiId", krithiId.toString())
                put("extractionTaskId", extractionTaskId.toString())
            }
            dal.auditLogs.append(
                action = "LINK_IMPORT_FROM_EXTRACTION",
                entityTable = "imported_krithis",
                entityId = updated.id,
                metadata = linkMetadata.toString(),
            )
            linked++
        }

        if (linked > 0) {
            logger.info(
                "Linked {} import(s) for sourceUrl={} to krithi={} via extraction task {}",
                linked,
                sourceUrl,
                krithiId,
                extractionTaskId,
            )
        }
    }
}
