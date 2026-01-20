package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.JobType
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.shared.domain.model.BatchStatusDto
import com.sangita.grantha.shared.domain.model.ImportBatchDto
import com.sangita.grantha.shared.domain.model.ImportEventDto
import com.sangita.grantha.shared.domain.model.ImportJobDto
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BulkImportOrchestrationService(
    private val dal: SangitaDal,
) {
    @Serializable
    private data class ManifestJobPayload(
        val sourceManifestPath: String,
    )

    suspend fun createBatch(sourceManifestPath: String): ImportBatchDto {
        val batch = dal.bulkImport.createBatch(sourceManifest = sourceManifestPath, createdByUserId = null)

        // Create a single MANIFEST_INGEST job + task. Workers will expand this into SCRAPE tasks.
        val manifestJob = dal.bulkImport.createJob(
            batchId = batch.id,
            jobType = JobType.MANIFEST_INGEST,
            payload = Json.encodeToString(ManifestJobPayload(sourceManifestPath))
        )
        dal.bulkImport.createTask(
            jobId = manifestJob.id,
            krithiKey = "manifest:${sourceManifestPath.substringAfterLast('/')}",
            sourceUrl = null
        )

        dal.bulkImport.createEvent(
            refType = "batch",
            refId = batch.id,
            eventType = "BATCH_CREATED",
            data = Json.encodeToString(mapOf("sourceManifestPath" to sourceManifestPath))
        )
        dal.auditLogs.append(
            action = "BULK_IMPORT_BATCH_CREATE",
            entityTable = "import_batch",
            entityId = batch.id
        )

        return batch
    }

    suspend fun listBatches(status: BatchStatus? = null, limit: Int = 100, offset: Int = 0): List<ImportBatchDto> =
        dal.bulkImport.listBatches(status = status, limit = limit, offset = offset)

    suspend fun getBatch(id: Uuid): ImportBatchDto? = dal.bulkImport.findBatchById(id)

    suspend fun getBatchJobs(id: Uuid): List<ImportJobDto> = dal.bulkImport.listJobsByBatch(id)

    suspend fun getBatchTasks(
        id: Uuid,
        status: TaskStatus? = null,
        limit: Int = 1000,
        offset: Int = 0,
    ): List<ImportTaskRunDto> = dal.bulkImport.listTasksByBatch(batchId = id, status = status, limit = limit, offset = offset)

    suspend fun getBatchEvents(id: Uuid, limit: Int = 200): List<ImportEventDto> =
        dal.bulkImport.listEventsByRef(refType = "batch", refId = id, limit = limit)

    suspend fun pauseBatch(id: Uuid): ImportBatchDto {
        val updated = dal.bulkImport.updateBatchStatus(id = id, status = BatchStatus.PAUSED)
            ?: throw NoSuchElementException("Batch not found")

        dal.bulkImport.createEvent(refType = "batch", refId = id, eventType = "BATCH_PAUSED")
        dal.auditLogs.append(action = "BULK_IMPORT_BATCH_PAUSE", entityTable = "import_batch", entityId = id)
        return updated
    }

    suspend fun resumeBatch(id: Uuid): ImportBatchDto {
        val updated = dal.bulkImport.updateBatchStatus(id = id, status = BatchStatus.RUNNING)
            ?: throw NoSuchElementException("Batch not found")

        dal.bulkImport.createEvent(refType = "batch", refId = id, eventType = "BATCH_RESUMED")
        dal.auditLogs.append(action = "BULK_IMPORT_BATCH_RESUME", entityTable = "import_batch", entityId = id)
        return updated
    }

    suspend fun cancelBatch(id: Uuid): ImportBatchDto {
        val updated = dal.bulkImport.updateBatchStatus(id = id, status = BatchStatus.CANCELLED)
            ?: throw NoSuchElementException("Batch not found")

        dal.bulkImport.createEvent(refType = "batch", refId = id, eventType = "BATCH_CANCELLED")
        dal.auditLogs.append(action = "BULK_IMPORT_BATCH_CANCEL", entityTable = "import_batch", entityId = id)
        return updated
    }

    suspend fun retryBatch(id: Uuid, includeFailed: Boolean = true): Int {
        val batch = dal.bulkImport.findBatchById(id) ?: throw NoSuchElementException("Batch not found")
        if (batch.status == BatchStatusDto.CANCELLED) return 0

        val fromStatuses = buildSet {
            add(TaskStatus.RETRYABLE)
            if (includeFailed) add(TaskStatus.FAILED)
        }

        val updatedCount = dal.bulkImport.requeueTasksForBatch(batchId = id, fromStatuses = fromStatuses)

        dal.bulkImport.createEvent(
            refType = "batch",
            refId = id,
            eventType = "BATCH_RETRIED",
            data = Json.encodeToString(mapOf("includeFailed" to includeFailed, "requeuedTasks" to updatedCount))
        )
        dal.auditLogs.append(action = "BULK_IMPORT_BATCH_RETRY", entityTable = "import_batch", entityId = id)
        return updatedCount
    }
}

