package com.sangita.grantha.backend.api.services.bulkimport.workers

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.backend.api.services.bulkimport.BatchCompletionHandler
import com.sangita.grantha.backend.api.services.bulkimport.BulkImportWorkerConfig
import com.sangita.grantha.backend.api.services.bulkimport.RateLimiter
import com.sangita.grantha.backend.api.services.bulkimport.TaskErrorBuilder
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.channels.Channel

class ScrapeWorker(
    private val dal: SangitaDal,
    private val importService: IImportService,
    private val rateLimiter: RateLimiter,
    private val errorBuilder: TaskErrorBuilder,
    private val completionHandler: BatchCompletionHandler,
) {
    suspend fun run(
        config: BulkImportWorkerConfig,
        channel: Channel<ImportTaskRunDto>,
        isActive: () -> Boolean
    ) {
        for (task in channel) {
            process(task, config, isActive)
        }
    }

    private suspend fun process(task: ImportTaskRunDto, config: BulkImportWorkerConfig, isActive: () -> Boolean) {
        val startedAt = OffsetDateTime.now(ZoneOffset.UTC)
        dal.bulkImportTasks.markTaskStarted(task.id, startedAt)

        val job = dal.bulkImport.findJobById(task.jobId)
            ?: run {
                dal.bulkImportTasks.updateTaskStatus(id = task.id, status = TaskStatus.FAILED, error = """{"message":"Missing job"}""")
                return
            }

        val batchId = job.batchId
        val attemptRow = dal.bulkImportTasks.incrementTaskAttempt(task.id)
        val attempt = attemptRow?.attempt ?: task.attempt

        val url = task.sourceUrl
        if (url.isNullOrBlank()) {
            dal.bulkImportTasks.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = errorBuilder.build(code = "missing_source_url", message = "Task missing sourceUrl", attempt = attempt),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
            completionHandler.checkAndTriggerNextStage(job.id)
            return
        }

        if (attempt > config.maxAttempts) {
            dal.bulkImportTasks.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = errorBuilder.build(
                    code = "max_attempts_exceeded",
                    message = "Task exceeded max attempts",
                    url = url,
                    attempt = attempt
                ),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
            completionHandler.checkAndTriggerNextStage(job.id)
            return
        }

        if (TaskStatus.valueOf(job.status.name) != TaskStatus.RUNNING) {
            dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.RUNNING, startedAt = startedAt)
        }

        try {
            // Parse CSV-provided metadata from krithiKey ("krithi|raga").
            // The ScrapeWorker no longer performs Kotlin-side HTML scraping.
            // Instead, it creates the import record with CSV metadata only,
            // which triggers the Python extraction worker (via extraction_queue)
            // to handle HTML scraping, section parsing, and Word Division filtering.
            val csvKrithi: String?
            val csvRaga: String?
            val key = task.krithiKey
            if (!key.isNullOrBlank() && key.contains("|")) {
                val parts = key.split("|", limit = 2)
                csvKrithi = parts[0].trim().ifBlank { null }
                csvRaga = parts[1].trim().ifBlank { null }
            } else {
                csvKrithi = null
                csvRaga = null
            }

            // Infer composer from well-known blog URL patterns when the CSV
            // does not include a composer column.
            val csvComposer = inferComposerFromUrl(url)

            val importRequest = ImportKrithiRequest(
                source = "BulkImportCSV",
                sourceKey = url,
                batchId = batchId.toString(),
                rawTitle = csvKrithi,
                rawRaga = csvRaga,
                rawComposer = csvComposer,
                // rawLyrics and rawPayload intentionally left null so that
                // ImportService.shouldEnqueueHtmlExtraction() returns true,
                // delegating HTML scraping to the Python extraction worker.
            )

            importService.submitImports(listOf(importRequest))

            dal.bulkImportTasks.updateTaskStatus(
                id = task.id,
                status = TaskStatus.SUCCEEDED,
                durationMs = elapsedMsSince(startedAt),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, succeededDelta = 1)
            completionHandler.checkAndTriggerNextStage(job.id)
        } catch (e: Exception) {
            val errorJson = errorBuilder.build(
                code = "scrape_failed",
                message = "Scrape/import failed",
                url = url,
                attempt = attempt,
                cause = e.message
            )
            val finalAttempt = attempt >= config.maxAttempts
            dal.bulkImportTasks.updateTaskStatus(
                id = task.id,
                status = if (finalAttempt) TaskStatus.FAILED else TaskStatus.RETRYABLE,
                error = errorJson,
                durationMs = elapsedMsSince(startedAt),
                completedAt = if (finalAttempt) OffsetDateTime.now(ZoneOffset.UTC) else null
            )

            if (finalAttempt) {
                dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
                completionHandler.checkAndTriggerNextStage(job.id)
            } else {
                dal.bulkImportEvents.createEvent(
                    refType = "batch",
                    refId = batchId,
                    eventType = "TASK_RETRY_SCHEDULED",
                    data = errorJson
                )
            }
        }
    }

    private fun elapsedMsSince(startedAt: OffsetDateTime): Int {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val ms = java.time.Duration.between(startedAt, now).toMillis()
        return ms.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    companion object {
        /**
         * Infer composer name from well-known Carnatic music blog URL patterns.
         *
         * Known mappings:
         *  - guru-guha.blogspot.com → Muthuswami Dikshitar
         *  - syamakrishnavaibhavam.blogspot.com → Syama Sastri
         *  - thyagaraja-vaibhavam.blogspot.com → Tyagaraja
         */
        internal fun inferComposerFromUrl(url: String): String? {
            val lower = url.lowercase()
            return when {
                "guru-guha.blogspot" in lower -> "Muthuswami Dikshitar"
                "syamakrishnavaibhavam.blogspot" in lower -> "Syama Sastri"
                "thyagaraja-vaibhavam.blogspot" in lower -> "Tyagaraja"
                else -> null
            }
        }
    }
}
