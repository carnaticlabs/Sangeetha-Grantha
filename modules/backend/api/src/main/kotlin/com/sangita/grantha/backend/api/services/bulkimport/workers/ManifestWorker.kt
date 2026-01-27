package com.sangita.grantha.backend.api.services.bulkimport.workers

import com.sangita.grantha.backend.api.services.bulkimport.BulkImportWorkerConfig
import com.sangita.grantha.backend.api.services.bulkimport.ManifestParser
import com.sangita.grantha.backend.api.services.bulkimport.TaskErrorBuilder
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.JobType
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.shared.domain.model.ImportJobDto
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import com.sangita.grantha.shared.domain.model.JobTypeDto
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.io.path.exists
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.Logger

class ManifestWorker(
    private val dal: SangitaDal,
    private val parser: ManifestParser,
    private val errorBuilder: TaskErrorBuilder,
    private val logger: Logger,
    private val json: Json = Json
) {
    @Serializable
    private data class ManifestJobPayload(
        val sourceManifestPath: String,
    )

    suspend fun run(config: BulkImportWorkerConfig, channel: Channel<ImportTaskRunDto>) {
        for (task in channel) {
            process(task, config)
        }
    }

    private suspend fun process(task: ImportTaskRunDto, config: BulkImportWorkerConfig) {
        val startedAt = OffsetDateTime.now(ZoneOffset.UTC)
        dal.bulkImport.markTaskStarted(task.id, startedAt)

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
                error = errorBuilder.build(
                    code = "max_attempts_exceeded",
                    message = "Manifest ingest exceeded max attempts",
                    attempt = attempt
                ),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.FAILED, completedAt = OffsetDateTime.now(ZoneOffset.UTC))
            return
        }

        val payload = job.payload ?: run {
            failManifestTask(task, job, startedAt, errorBuilder.build(code = "missing_payload", message = "Missing job payload"))
            return
        }

        val manifestPath = try {
            json.decodeFromString<ManifestJobPayload>(payload).sourceManifestPath
        } catch (e: Exception) {
            failManifestTask(
                task,
                job,
                startedAt,
                errorBuilder.build(code = "invalid_payload", message = "Invalid manifest payload", cause = e.message)
            )
            return
        }

        val csvPath = Path.of(manifestPath)
        if (!csvPath.exists()) {
            failManifestTask(
                task,
                job,
                startedAt,
                errorBuilder.build(code = "manifest_missing", message = "Manifest not found", cause = manifestPath)
            )
            return
        }

        try {
            val rows = parser.parse(csvPath)
            if (rows.isEmpty()) {
                failManifestTask(
                    task,
                    job,
                    startedAt,
                    errorBuilder.build(code = "manifest_empty", message = "Manifest contained no rows", cause = manifestPath)
                )
                return
            }

            val dedupedRows = rows.distinctBy { it.hyperlink.trim().lowercase() }

            val existingScrapeJob = dal.bulkImport.listJobsByBatch(batchId).firstOrNull { it.jobType == JobTypeDto.SCRAPE }
            val scrapeJob = existingScrapeJob ?: dal.bulkImport.createJob(
                batchId = batchId,
                jobType = JobType.SCRAPE,
                payload = json.encodeToString(mapOf("sourceManifestPath" to manifestPath))
            )

            val createdTasks = dal.bulkImport.createTasks(
                jobId = scrapeJob.id,
                batchId = batchId,
                tasks = dedupedRows.map { row ->
                    val key = "${row.krithi}|${row.raga ?: ""}".trim()
                    key to row.hyperlink
                }
            )

            val totalTasks = dal.bulkImport.listTasksByJob(scrapeJob.id).size
            dal.bulkImport.setBatchTotals(id = batchId, totalTasks = totalTasks)
            dal.bulkImport.createEvent(
                refType = "batch",
                refId = batchId,
                eventType = "MANIFEST_INGEST_SUCCEEDED",
                data = buildJsonObject {
                    put("rows", dedupedRows.size)
                    put("newTasks", createdTasks.size)
                    put("totalTasks", totalTasks)
                    put("manifestPath", manifestPath)
                }.toString()
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
                result = buildJsonObject {
                    put("rows", dedupedRows.size)
                    put("newTasks", createdTasks.size)
                    put("totalTasks", totalTasks)
                }.toString(),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        } catch (e: Exception) {
            failManifestTask(
                task,
                job,
                startedAt,
                errorBuilder.build(code = "manifest_ingest_failed", message = "Manifest ingest failed", cause = e.message)
            )
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
        dal.bulkImport.updateBatchStatus(id = job.batchId, status = BatchStatus.FAILED, completedAt = now)
    }

    private fun elapsedMsSince(startedAt: OffsetDateTime): Int {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val ms = java.time.Duration.between(startedAt, now).toMillis()
        return ms.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }
}
