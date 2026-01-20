package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.JobType
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.shared.domain.model.ImportJobDto
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.io.path.exists
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.slf4j.LoggerFactory

class BulkImportWorkerService(
    private val dal: SangitaDal,
    private val importService: ImportService,
    private val webScrapingService: WebScrapingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class WorkerConfig(
        val manifestWorkerCount: Int = 1,
        val scrapeWorkerCount: Int = 3,
        val pollIntervalMs: Long = 750,
        val maxAttempts: Int = 3,
    )

    private var scope: CoroutineScope? = null

    fun start(config: WorkerConfig = WorkerConfig()) {
        if (scope != null) return
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("BulkImportWorkers"))
        scope = workerScope

        repeat(config.manifestWorkerCount) { idx ->
            workerScope.launch(CoroutineName("BulkImportManifestWorker-$idx")) {
                runManifestIngestLoop(config)
            }
        }
        repeat(config.scrapeWorkerCount) { idx ->
            workerScope.launch(CoroutineName("BulkImportScrapeWorker-$idx")) {
                runScrapeLoop(config)
            }
        }

        logger.info("Bulk import workers started (manifest={}, scrape={})", config.manifestWorkerCount, config.scrapeWorkerCount)
    }

    fun stop() {
        scope?.cancel("Stopping bulk import workers")
        scope = null
        logger.info("Bulk import workers stopped")
    }

    @Serializable
    private data class ManifestJobPayload(
        val sourceManifestPath: String,
    )

    @Serializable
    private data class CsvRow(
        val krithi: String,
        val raga: String?,
        val hyperlink: String,
    )

    private suspend fun runManifestIngestLoop(config: WorkerConfig) {
        while (scope?.isActive == true) {
            val task = dal.bulkImport.claimNextPendingTask(
                jobType = JobType.MANIFEST_INGEST,
                allowedBatchStatuses = setOf(BatchStatus.PENDING, BatchStatus.RUNNING)
            )

            if (task == null) {
                delay(config.pollIntervalMs)
                continue
            }

            processManifestTask(task, config)
        }
    }

    private suspend fun processManifestTask(task: ImportTaskRunDto, config: WorkerConfig) {
        val startedAt = OffsetDateTime.now(ZoneOffset.UTC)
        val job: ImportJobDto = dal.bulkImport.findJobById(task.jobId)
            ?: run {
                logger.warn("Manifest task {} has missing job {}", task.id, task.jobId)
                dal.bulkImport.updateTaskStatus(id = task.id, status = TaskStatus.FAILED, error = """{"message":"Missing job"}""")
                return
            }

        val batchId = job.batchId
        dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.RUNNING, startedAt = startedAt)
        dal.bulkImport.updateBatchStatus(id = batchId, status = BatchStatus.RUNNING, startedAt = startedAt)

        val attemptRow = dal.bulkImport.incrementTaskAttempt(task.id)
        val attempt = attemptRow?.attempt ?: task.attempt
        if (attempt > config.maxAttempts) {
            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = """{"message":"Max attempts exceeded"}""",
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.FAILED, completedAt = OffsetDateTime.now(ZoneOffset.UTC))
            return
        }

        val payload = job.payload ?: run {
            failManifestTask(task, job, startedAt, """{"message":"Missing job payload"}""")
            return
        }

        val manifestPath = try {
            Json.decodeFromString<ManifestJobPayload>(payload).sourceManifestPath
        } catch (e: Exception) {
            failManifestTask(task, job, startedAt, Json.encodeToString(mapOf("message" to "Invalid payload", "error" to (e.message ?: ""))))
            return
        }

        val csvPath = Path.of(manifestPath)
        if (!csvPath.exists()) {
            failManifestTask(task, job, startedAt, Json.encodeToString(mapOf("message" to "Manifest not found", "path" to manifestPath)))
            return
        }

        try {
            val rows = parseCsvManifest(csvPath)
            if (rows.isEmpty()) {
                failManifestTask(task, job, startedAt, Json.encodeToString(mapOf("message" to "Manifest contained no rows", "path" to manifestPath)))
                return
            }

            // Create SCRAPE job + per-row tasks
            val scrapeJob = dal.bulkImport.createJob(
                batchId = batchId,
                jobType = JobType.SCRAPE,
                payload = Json.encodeToString(mapOf("sourceManifestPath" to manifestPath))
            )

            dal.bulkImport.createTasks(
                jobId = scrapeJob.id,
                tasks = rows.map { row ->
                    val key = "${row.krithi}|${row.raga ?: ""}".trim()
                    key to row.hyperlink
                }
            )

            dal.bulkImport.setBatchTotals(id = batchId, totalTasks = rows.size)
            dal.bulkImport.createEvent(
                refType = "batch",
                refId = batchId,
                eventType = "MANIFEST_INGEST_SUCCEEDED",
                data = Json.encodeToString(mapOf("rows" to rows.size, "manifestPath" to manifestPath))
            )

            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.SUCCEEDED,
                durationMs = elapsedMsSince(startedAt),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.updateJobStatus(
                id = job.id,
                status = TaskStatus.SUCCEEDED,
                result = Json.encodeToString(mapOf("rows" to rows.size)),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        } catch (e: Exception) {
            failManifestTask(task, job, startedAt, Json.encodeToString(mapOf("message" to "Manifest ingest failed", "error" to (e.message ?: ""))))
        }
    }

    private suspend fun failManifestTask(task: ImportTaskRunDto, job: ImportJobDto, startedAt: OffsetDateTime, errorJson: String) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        dal.bulkImport.updateTaskStatus(
            id = task.id,
            status = TaskStatus.FAILED,
            error = errorJson,
            durationMs = elapsedMsSince(startedAt),
            completedAt = now
        )
        dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.FAILED, result = errorJson, completedAt = now)
        dal.bulkImport.createEvent(refType = "batch", refId = job.batchId, eventType = "MANIFEST_INGEST_FAILED", data = errorJson)
    }

    private suspend fun runScrapeLoop(config: WorkerConfig) {
        while (scope?.isActive == true) {
            val task = dal.bulkImport.claimNextPendingTask(
                jobType = JobType.SCRAPE,
                allowedBatchStatuses = setOf(BatchStatus.RUNNING)
            )

            if (task == null) {
                delay(config.pollIntervalMs)
                continue
            }

            processScrapeTask(task, config)
        }
    }

    private suspend fun processScrapeTask(task: ImportTaskRunDto, config: WorkerConfig) {
        val startedAt = OffsetDateTime.now(ZoneOffset.UTC)

        val job = dal.bulkImport.findJobById(task.jobId)
            ?: run {
                dal.bulkImport.updateTaskStatus(id = task.id, status = TaskStatus.FAILED, error = """{"message":"Missing job"}""")
                return
            }

        val batchId = job.batchId

        val attemptRow = dal.bulkImport.incrementTaskAttempt(task.id)
        val attempt = attemptRow?.attempt ?: task.attempt
        if (attempt > config.maxAttempts) {
            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = """{"message":"Max attempts exceeded"}""",
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
            maybeCompleteBatch(batchId)
            return
        }

        val url = task.sourceUrl
        if (url.isNullOrBlank()) {
            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = """{"message":"Missing sourceUrl"}""",
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
            maybeCompleteBatch(batchId)
            return
        }

        try {
            val scraped = webScrapingService.scrapeKrithi(url)
            val importRequest = ImportKrithiRequest(
                source = "BulkImportCSV",
                sourceKey = url,
                rawTitle = scraped.title,
                rawLyrics = scraped.lyrics,
                rawComposer = scraped.composer,
                rawRaga = scraped.raga,
                rawTala = scraped.tala,
                rawDeity = scraped.deity,
                rawTemple = scraped.temple,
                rawLanguage = scraped.language,
                rawPayload = Json.encodeToJsonElement(scraped)
            )

            importService.submitImports(listOf(importRequest))

            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.SUCCEEDED,
                durationMs = elapsedMsSince(startedAt),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, succeededDelta = 1)
        } catch (e: Exception) {
            val errorJson = Json.encodeToString(mapOf("message" to "Scrape/import failed", "url" to url, "error" to (e.message ?: "")))
            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = errorJson,
                durationMs = elapsedMsSince(startedAt),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
        } finally {
            maybeCompleteBatch(batchId)
        }
    }

    private suspend fun maybeCompleteBatch(batchId: kotlin.uuid.Uuid) {
        val batch = dal.bulkImport.findBatchById(batchId) ?: return
        if (batch.status != com.sangita.grantha.shared.domain.model.BatchStatusDto.RUNNING) return
        if (batch.totalTasks <= 0) return
        if (batch.processedTasks < batch.totalTasks) return

        val finalStatus = if (batch.failedTasks == 0 && batch.blockedTasks == 0) BatchStatus.SUCCEEDED else BatchStatus.FAILED
        dal.bulkImport.updateBatchStatus(id = batchId, status = finalStatus, completedAt = OffsetDateTime.now(ZoneOffset.UTC))
        dal.bulkImport.createEvent(
            refType = "batch",
            refId = batchId,
            eventType = "BATCH_COMPLETED",
            data = Json.encodeToString(
                mapOf(
                    "status" to finalStatus.name,
                    "totalTasks" to batch.totalTasks,
                    "processedTasks" to batch.processedTasks,
                    "succeededTasks" to batch.succeededTasks,
                    "failedTasks" to batch.failedTasks
                )
            )
        )
    }

    private fun elapsedMsSince(startedAt: OffsetDateTime): Int {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val ms = java.time.Duration.between(startedAt, now).toMillis()
        return ms.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    private fun parseCsvManifest(path: Path): List<CsvRow> {
        val lines = Files.readAllLines(path)
        if (lines.isEmpty()) return emptyList()

        val header = parseCsvLine(lines.first())
        val krithiIdx = header.indexOfFirst { it.equals("Krithi", ignoreCase = true) }.takeIf { it >= 0 } ?: 0
        val ragaIdx = header.indexOfFirst { it.equals("Raga", ignoreCase = true) }.takeIf { it >= 0 } ?: 1
        val hyperlinkIdx = header.indexOfFirst { it.equals("Hyperlink", ignoreCase = true) }.takeIf { it >= 0 } ?: 2

        return lines.drop(1)
            .asSequence()
            .mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                val cols = parseCsvLine(line)
                val krithi = cols.getOrNull(krithiIdx)?.trim().orEmpty()
                val hyperlink = cols.getOrNull(hyperlinkIdx)?.trim().orEmpty()
                if (krithi.isBlank() || hyperlink.isBlank()) return@mapNotNull null
                val raga = cols.getOrNull(ragaIdx)?.trim()?.takeIf { it.isNotBlank() }
                CsvRow(krithi = krithi, raga = raga, hyperlink = hyperlink)
            }
            .toList()
    }

    /**
     * Minimal CSV parsing with support for quoted fields.
     */
    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    // Escaped quote ("")
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    out.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}

