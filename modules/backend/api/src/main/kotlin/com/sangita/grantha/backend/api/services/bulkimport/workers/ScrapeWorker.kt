package com.sangita.grantha.backend.api.services.bulkimport.workers

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.backend.api.services.IWebScraper
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class ScrapeWorker(
    private val dal: SangitaDal,
    private val importService: IImportService,
    private val webScrapingService: IWebScraper,
    private val rateLimiter: RateLimiter,
    private val errorBuilder: TaskErrorBuilder,
    private val completionHandler: BatchCompletionHandler,
    private val json: Json = Json
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
        dal.bulkImport.markTaskStarted(task.id, startedAt)

        val job = dal.bulkImport.findJobById(task.jobId)
            ?: run {
                dal.bulkImport.updateTaskStatus(id = task.id, status = TaskStatus.FAILED, error = """{"message":"Missing job"}""")
                return
            }

        val batchId = job.batchId
        val attemptRow = dal.bulkImport.incrementTaskAttempt(task.id)
        val attempt = attemptRow?.attempt ?: task.attempt

        val url = task.sourceUrl
        if (url.isNullOrBlank()) {
            dal.bulkImport.updateTaskStatus(
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
            dal.bulkImport.updateTaskStatus(
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
            rateLimiter.throttle(url, config, isActive)
            val scraped = webScrapingService.scrapeKrithi(url)
            val importRequest = ImportKrithiRequest(
                source = "BulkImportCSV",
                sourceKey = url,
                batchId = batchId.toString(),
                rawTitle = scraped.title,
                rawLyrics = scraped.lyrics,
                rawComposer = scraped.composer,
                rawRaga = scraped.raga,
                rawTala = scraped.tala,
                rawDeity = scraped.deity,
                rawTemple = scraped.temple,
                rawLanguage = scraped.language,
                rawPayload = json.encodeToJsonElement(scraped)
            )

            importService.submitImports(listOf(importRequest))

            dal.bulkImport.updateTaskStatus(
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
            dal.bulkImport.updateTaskStatus(
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
                dal.bulkImport.createEvent(
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
}
