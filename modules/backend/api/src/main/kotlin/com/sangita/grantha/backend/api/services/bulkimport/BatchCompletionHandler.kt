package com.sangita.grantha.backend.api.services.bulkimport

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.JobType
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.shared.domain.model.BatchStatusDto
import com.sangita.grantha.shared.domain.model.JobTypeDto
import com.sangita.grantha.shared.domain.model.TaskStatusDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.uuid.Uuid
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BatchCompletionHandler(
    private val dal: SangitaDal,
    private val json: Json = Json
) {
    suspend fun checkAndTriggerNextStage(jobId: Uuid) {
        val job = dal.bulkImport.findJobById(jobId) ?: return
        val batch = dal.bulkImport.findBatchById(job.batchId) ?: return

        if (batch.processedTasks < batch.totalTasks) {
            return
        }

        val tasks = dal.bulkImport.listTasksByJob(jobId)
        val isComplete = tasks.all {
            val s = TaskStatus.valueOf(it.status.name)
            s == TaskStatus.SUCCEEDED || s == TaskStatus.FAILED || s == TaskStatus.BLOCKED || s == TaskStatus.CANCELLED
        }

        if (!isComplete) return

        if (TaskStatus.valueOf(job.status.name) != TaskStatus.SUCCEEDED) {
            dal.bulkImport.updateJobStatus(
                id = jobId,
                status = TaskStatus.SUCCEEDED,
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        }

        when (job.jobType) {
            JobTypeDto.SCRAPE -> {
                val existing = dal.bulkImport.listJobsByBatch(job.batchId).find { it.jobType == JobTypeDto.ENTITY_RESOLUTION }
                if (existing == null) {
                    val resolutionJob = dal.bulkImport.createJob(
                        batchId = job.batchId,
                        jobType = JobType.ENTITY_RESOLUTION,
                        payload = null
                    )

                    val succeededTasks = tasks.filter { TaskStatus.valueOf(it.status.name) == TaskStatus.SUCCEEDED }
                    val newTasks = succeededTasks.mapNotNull { t ->
                        if (t.krithiKey != null && t.sourceUrl != null) {
                            t.krithiKey to t.sourceUrl!!
                        } else null
                    }

                    if (newTasks.isNotEmpty()) {
                        dal.bulkImport.createTasks(
                            jobId = resolutionJob.id,
                            batchId = job.batchId,
                            tasks = newTasks
                        )

                        val updatedBatch = dal.bulkImport.findBatchById(job.batchId)
                        if (updatedBatch != null) {
                            dal.bulkImport.setBatchTotals(updatedBatch.id, updatedBatch.totalTasks + newTasks.size)
                        }
                    } else {
                        maybeCompleteBatch(job.batchId)
                    }
                }
            }
            JobTypeDto.ENTITY_RESOLUTION -> maybeCompleteBatch(job.batchId)
            else -> Unit
        }
    }

    private suspend fun maybeCompleteBatch(batchId: Uuid) {
        val batch = dal.bulkImport.findBatchById(batchId) ?: return
        if (batch.status != BatchStatusDto.RUNNING) return
        if (batch.totalTasks <= 0) return
        if (batch.processedTasks < batch.totalTasks) return

        val finalStatus = if (batch.failedTasks == 0 && batch.blockedTasks == 0) BatchStatus.SUCCEEDED else BatchStatus.FAILED
        dal.bulkImport.updateBatchStatus(id = batchId, status = finalStatus, completedAt = OffsetDateTime.now(ZoneOffset.UTC))
        dal.bulkImport.createEvent(
            refType = "batch",
            refId = batchId,
            eventType = "BATCH_COMPLETED",
            data = json.encodeToString(
                buildJsonObject {
                    put("status", finalStatus.name)
                    put("totalTasks", batch.totalTasks)
                    put("processedTasks", batch.processedTasks)
                    put("succeededTasks", batch.succeededTasks)
                    put("failedTasks", batch.failedTasks)
                }
            )
        )
    }
}
