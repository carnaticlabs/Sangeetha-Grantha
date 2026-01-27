package com.sangita.grantha.backend.api.services.bulkimport.workers

import com.sangita.grantha.backend.api.services.AutoApprovalService
import com.sangita.grantha.backend.api.services.DeduplicationService
import com.sangita.grantha.backend.api.services.IEntityResolver
import com.sangita.grantha.backend.api.services.IQualityScorer
import com.sangita.grantha.backend.api.services.bulkimport.BatchCompletionHandler
import com.sangita.grantha.backend.api.services.bulkimport.BulkImportWorkerConfig
import com.sangita.grantha.backend.api.services.bulkimport.TaskErrorBuilder
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ResolutionWorker(
    private val dal: SangitaDal,
    private val entityResolutionService: IEntityResolver,
    private val deduplicationService: DeduplicationService,
    private val autoApprovalService: AutoApprovalService,
    private val qualityScoringService: IQualityScorer,
    private val errorBuilder: TaskErrorBuilder,
    private val completionHandler: BatchCompletionHandler,
    private val json: Json = Json
) {
    suspend fun run(config: BulkImportWorkerConfig, channel: Channel<ImportTaskRunDto>) {
        for (task in channel) {
            process(task, config)
        }
    }

    private suspend fun process(task: ImportTaskRunDto, config: BulkImportWorkerConfig) {
        val startedAt = OffsetDateTime.now(ZoneOffset.UTC)
        dal.bulkImport.markTaskStarted(task.id, startedAt)
        val job = dal.bulkImport.findJobById(task.jobId)
            ?: run {
                dal.bulkImport.updateTaskStatus(id = task.id, status = TaskStatus.FAILED, error = """{"message":"Missing job"}""")
                return
            }
        val batchId = job.batchId

        if (TaskStatus.valueOf(job.status.name) != TaskStatus.RUNNING) {
            dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.RUNNING, startedAt = startedAt)
        }

        try {
            val sourceId = dal.imports.findOrCreateSource("BulkImportCSV")
            val importedKrithi = dal.imports.findBySourceAndKey(sourceId, task.sourceUrl ?: "")

            if (importedKrithi == null) {
                dal.bulkImport.updateTaskStatus(
                    id = task.id,
                    status = TaskStatus.FAILED,
                    error = errorBuilder.build(code = "import_missing", message = "ImportedKrithi not found", url = task.sourceUrl),
                    completedAt = OffsetDateTime.now(ZoneOffset.UTC)
                )
                dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
                completionHandler.checkAndTriggerNextStage(job.id)
                return
            }

            val resolution = entityResolutionService.resolve(importedKrithi)
            val resolutionJson = json.encodeToString(resolution)
            dal.imports.saveResolution(importedKrithi.id, resolutionJson)

            val composerId = resolution.composerCandidates.firstOrNull { it.confidence == "HIGH" }?.entity?.id
            val ragaId = resolution.ragaCandidates.firstOrNull { it.confidence == "HIGH" }?.entity?.id

            val duplicates = deduplicationService.findDuplicates(
                imported = importedKrithi,
                resolvedComposerId = composerId,
                resolvedRagaId = ragaId
            )

            if (duplicates.matches.isNotEmpty()) {
                val duplicatesJson = json.encodeToString(duplicates)
                dal.imports.saveDuplicates(importedKrithi.id, duplicatesJson)
            }

            val updatedImported = dal.imports.findById(importedKrithi.id)
            if (updatedImported != null) {
                val qualityScore = qualityScoringService.calculateQualityScore(
                    imported = updatedImported,
                    resolutionDataJson = resolutionJson
                )
                dal.imports.updateQualityScores(
                    id = updatedImported.id,
                    qualityScore = qualityScore.overall,
                    qualityTier = qualityScore.tier.name,
                    completenessScore = qualityScore.completeness,
                    resolutionConfidence = qualityScore.resolutionConfidence,
                    sourceQuality = qualityScore.sourceQuality,
                    validationScore = qualityScore.validationScore
                )
            }

            val finalImported = dal.imports.findById(importedKrithi.id)
            if (finalImported != null) {
                autoApprovalService.autoApproveIfHighConfidence(finalImported)
            }

            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.SUCCEEDED,
                durationMs = elapsedMsSince(startedAt),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, succeededDelta = 1)
            completionHandler.checkAndTriggerNextStage(job.id)
        } catch (e: Exception) {
            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = errorBuilder.build(code = "resolution_failed", message = "Resolution failed", cause = e.message),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
            completionHandler.checkAndTriggerNextStage(job.id)
        }
    }

    private fun elapsedMsSince(startedAt: OffsetDateTime): Int {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val ms = java.time.Duration.between(startedAt, now).toMillis()
        return ms.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }
}
