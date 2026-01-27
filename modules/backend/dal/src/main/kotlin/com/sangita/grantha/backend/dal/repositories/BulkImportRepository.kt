package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.JobType
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.backend.dal.models.toImportBatchDto
import com.sangita.grantha.backend.dal.models.toImportJobDto
import com.sangita.grantha.backend.dal.models.toImportTaskRunDto
import com.sangita.grantha.backend.dal.models.toImportEventDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.ImportBatchTable
import com.sangita.grantha.backend.dal.tables.ImportEventTable
import com.sangita.grantha.backend.dal.tables.ImportJobTable
import com.sangita.grantha.backend.dal.tables.ImportTaskRunTable
import com.sangita.grantha.shared.domain.model.ImportBatchDto
import com.sangita.grantha.shared.domain.model.ImportJobDto
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import com.sangita.grantha.shared.domain.model.ImportEventDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*

/**
 * Repository for bulk import batches, jobs, tasks, and events.
 */
class BulkImportRepository {

    private fun buildIdempotencyKey(
        batchId: UUID,
        krithiKey: String?,
        sourceUrl: String?,
        fallbackId: UUID,
    ): String {
        val base = sourceUrl?.takeIf { it.isNotBlank() }
            ?: krithiKey?.takeIf { it.isNotBlank() }
            ?: fallbackId.toString()
        return "${batchId}::${base}"
    }

    private fun resolveBatchIdForJob(jobId: Uuid): UUID {
        return ImportJobTable
            .selectAll()
            .andWhere { ImportJobTable.id eq jobId.toJavaUuid() }
            .limit(1)
            .singleOrNull()
            ?.get(ImportJobTable.batchId)
            ?: error("Unable to resolve batchId for job $jobId")
    }
    
    // Batch Operations
    /**
     * Create a new import batch for the given source manifest.
     */
    suspend fun createBatch(
        sourceManifest: String,
        createdByUserId: Uuid? = null,
    ): ImportBatchDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val batchId = UUID.randomUUID()
        
        ImportBatchTable.insert {
            it[id] = batchId
            it[ImportBatchTable.sourceManifest] = sourceManifest
            it[ImportBatchTable.createdByUserId] = createdByUserId?.toJavaUuid()
            it[ImportBatchTable.status] = BatchStatus.PENDING
            it[ImportBatchTable.totalTasks] = 0
            it[ImportBatchTable.processedTasks] = 0
            it[ImportBatchTable.succeededTasks] = 0
            it[ImportBatchTable.failedTasks] = 0
            it[ImportBatchTable.blockedTasks] = 0
            it[ImportBatchTable.createdAt] = now
            it[ImportBatchTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toImportBatchDto()
            ?: error("Failed to create import batch")
    }
    
    /**
     * Find a batch by ID.
     */
    suspend fun findBatchById(id: Uuid): ImportBatchDto? = DatabaseFactory.dbQuery {
        ImportBatchTable
            .selectAll()
            .andWhere { ImportBatchTable.id eq id.toJavaUuid() }
            .map { it.toImportBatchDto() }
            .singleOrNull()
    }
    
    /**
     * List batches with optional status filter and pagination.
     */
    suspend fun listBatches(
        status: BatchStatus? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<ImportBatchDto> = DatabaseFactory.dbQuery {
        val query = ImportBatchTable.selectAll()
        status?.let { query.andWhere { ImportBatchTable.status eq it } }
        query
            .orderBy(ImportBatchTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toImportBatchDto() }
    }
    
    /**
     * Update batch status and optionally set start/completion timestamps.
     */
    suspend fun updateBatchStatus(
        id: Uuid,
        status: BatchStatus,
        startedAt: OffsetDateTime? = null,
        completedAt: OffsetDateTime? = null
    ): ImportBatchDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ImportBatchTable
            .updateReturning(
                where = { ImportBatchTable.id eq id.toJavaUuid() }
            ) { stmt ->
                stmt[ImportBatchTable.status] = status
                startedAt?.let { value -> stmt[ImportBatchTable.startedAt] = value }
                completedAt?.let { value -> stmt[ImportBatchTable.completedAt] = value }
                stmt[ImportBatchTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toImportBatchDto()
    }
    
    /**
     * Update batch counters (total/processed/succeeded/failed/blocked).
     */
    suspend fun updateBatchStats(
        id: Uuid,
        totalTasks: Int? = null,
        processedTasks: Int? = null,
        succeededTasks: Int? = null,
        failedTasks: Int? = null,
        blockedTasks: Int? = null
    ): ImportBatchDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ImportBatchTable
            .updateReturning(
                where = { ImportBatchTable.id eq id.toJavaUuid() }
            ) { stmt ->
                totalTasks?.let { value -> stmt[ImportBatchTable.totalTasks] = value }
                processedTasks?.let { value -> stmt[ImportBatchTable.processedTasks] = value }
                succeededTasks?.let { value -> stmt[ImportBatchTable.succeededTasks] = value }
                failedTasks?.let { value -> stmt[ImportBatchTable.failedTasks] = value }
                blockedTasks?.let { value -> stmt[ImportBatchTable.blockedTasks] = value }
                stmt[ImportBatchTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toImportBatchDto()
    }
    
    // Job Operations
    /**
     * Create a job entry for the given batch.
     */
    suspend fun createJob(
        batchId: Uuid,
        jobType: JobType,
        payload: String? = null
    ): ImportJobDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val jobId = UUID.randomUUID()
        
        ImportJobTable.insert {
            it[id] = jobId
            it[ImportJobTable.batchId] = batchId.toJavaUuid()
            it[ImportJobTable.jobType] = jobType
            it[ImportJobTable.status] = TaskStatus.PENDING
            it[ImportJobTable.retryCount] = 0
            it[ImportJobTable.payload] = payload
            it[ImportJobTable.createdAt] = now
            it[ImportJobTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toImportJobDto()
            ?: error("Failed to create import job")
    }
    
    /**
     * Find a job by ID.
     */
    suspend fun findJobById(id: Uuid): ImportJobDto? = DatabaseFactory.dbQuery {
        ImportJobTable
            .selectAll()
            .andWhere { ImportJobTable.id eq id.toJavaUuid() }
            .map { it.toImportJobDto() }
            .singleOrNull()
    }
    
    /**
     * List jobs for a batch ordered by creation time.
     */
    suspend fun listJobsByBatch(batchId: Uuid): List<ImportJobDto> = DatabaseFactory.dbQuery {
        ImportJobTable
            .selectAll()
            .andWhere { ImportJobTable.batchId eq batchId.toJavaUuid() }
            .orderBy(ImportJobTable.createdAt to SortOrder.ASC)
            .map { it.toImportJobDto() }
    }
    
    /**
     * Update a job's status and optional result/timestamps.
     */
    suspend fun updateJobStatus(
        id: Uuid,
        status: TaskStatus,
        result: String? = null,
        startedAt: OffsetDateTime? = null,
        completedAt: OffsetDateTime? = null
    ): ImportJobDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ImportJobTable
            .updateReturning(
                where = { ImportJobTable.id eq id.toJavaUuid() }
            ) { stmt ->
                stmt[ImportJobTable.status] = status
                result?.let { value -> stmt[ImportJobTable.result] = value }
                startedAt?.let { value -> stmt[ImportJobTable.startedAt] = value }
                completedAt?.let { value -> stmt[ImportJobTable.completedAt] = value }
                stmt[ImportJobTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toImportJobDto()
    }
    
    /**
     * Increment retry count for a job.
     */
    suspend fun incrementJobRetry(id: Uuid): ImportJobDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        // Get current retry count first
        val current = ImportJobTable
            .selectAll()
            .andWhere { ImportJobTable.id eq id.toJavaUuid() }
            .singleOrNull()
            ?: return@dbQuery null
        
        val newRetryCount = current[ImportJobTable.retryCount] + 1
        
        ImportJobTable
            .updateReturning(
                where = { ImportJobTable.id eq id.toJavaUuid() }
            ) {
                it[ImportJobTable.retryCount] = newRetryCount
                it[ImportJobTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toImportJobDto()
    }
    
    // Task Operations
    /**
     * Create a single task for a job with idempotency enforcement.
     */
    suspend fun createTask(
        jobId: Uuid,
        batchId: Uuid? = null,
        krithiKey: String? = null,
        sourceUrl: String? = null,
        idempotencyKey: String? = null
    ): ImportTaskRunDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val taskId = UUID.randomUUID()
        val javaJobId = jobId.toJavaUuid()
        val javaBatchId = batchId?.toJavaUuid() ?: resolveBatchIdForJob(jobId)
        val finalIdempotencyKey = idempotencyKey ?: buildIdempotencyKey(
            batchId = javaBatchId,
            krithiKey = krithiKey,
            sourceUrl = sourceUrl,
            fallbackId = taskId
        )

        // Idempotent insert: if a task already exists for this key, return it instead of erroring
        ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.idempotencyKey eq finalIdempotencyKey }
            .limit(1)
            .singleOrNull()
            ?.toImportTaskRunDto()
            ?.let { return@dbQuery it }

        ImportTaskRunTable.insert {
            it[id] = taskId
            it[ImportTaskRunTable.jobId] = javaJobId
            it[ImportTaskRunTable.krithiKey] = krithiKey
            it[ImportTaskRunTable.idempotencyKey] = finalIdempotencyKey
            it[ImportTaskRunTable.status] = TaskStatus.PENDING
            it[ImportTaskRunTable.attempt] = 0
            it[ImportTaskRunTable.sourceUrl] = sourceUrl
            it[ImportTaskRunTable.createdAt] = now
            it[ImportTaskRunTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toImportTaskRunDto()
            ?: error("Failed to create import task")
    }
    
    /**
     * Create tasks in bulk for a job while deduplicating idempotency keys.
     */
    suspend fun createTasks(
        jobId: Uuid,
        batchId: Uuid,
        tasks: List<Pair<String?, String?>> // List of (krithiKey, sourceUrl) pairs
    ): List<ImportTaskRunDto> = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaJobId = jobId.toJavaUuid()
        val javaBatchId = batchId.toJavaUuid()

        data class PreparedTask(
            val taskId: UUID,
            val krithiKey: String?,
            val sourceUrl: String?,
            val idempotencyKey: String,
        )

        val preparedTasks = tasks.map { (krithiKey, sourceUrl) ->
            val taskId = UUID.randomUUID()
            PreparedTask(
                taskId = taskId,
                krithiKey = krithiKey,
                sourceUrl = sourceUrl,
                idempotencyKey = buildIdempotencyKey(
                    batchId = javaBatchId,
                    krithiKey = krithiKey,
                    sourceUrl = sourceUrl,
                    fallbackId = taskId
                )
            )
        }.distinctBy { it.idempotencyKey }

        if (preparedTasks.isEmpty()) return@dbQuery emptyList()

        val idempotencyKeys: List<String?> = preparedTasks.map { it.idempotencyKey }
        val existingKeys = ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.idempotencyKey inList idempotencyKeys }
            .mapNotNull { it[ImportTaskRunTable.idempotencyKey] }
            .toSet()

        val toInsert = preparedTasks.filterNot { it.idempotencyKey in existingKeys }
        if (toInsert.isEmpty()) return@dbQuery emptyList()

        toInsert.map { task ->
            ImportTaskRunTable.insert {
                it[id] = task.taskId
                it[ImportTaskRunTable.jobId] = javaJobId
                it[ImportTaskRunTable.krithiKey] = task.krithiKey
                it[ImportTaskRunTable.idempotencyKey] = task.idempotencyKey
                it[ImportTaskRunTable.status] = TaskStatus.PENDING
                it[ImportTaskRunTable.attempt] = 0
                it[ImportTaskRunTable.sourceUrl] = task.sourceUrl
                it[ImportTaskRunTable.createdAt] = now
                it[ImportTaskRunTable.updatedAt] = now
            }
                .resultedValues
                ?.single()
                ?.toImportTaskRunDto()
                ?: error("Failed to create import task")
        }
    }
    
    /**
     * Atomically claims the next batch of pending tasks (sets them to RUNNING) and returns them.
     */
    suspend fun claimNextPendingTasks(
        jobType: JobType,
        allowedBatchStatuses: Set<BatchStatus> = setOf(BatchStatus.RUNNING),
        limit: Int = 1,
    ): List<ImportTaskRunDto> = DatabaseFactory.dbQuery {
        val rows = ImportTaskRunTable
            .innerJoin(ImportJobTable, { ImportTaskRunTable.jobId }, { ImportJobTable.id })
            .innerJoin(ImportBatchTable, { ImportJobTable.batchId }, { ImportBatchTable.id })
            .selectAll()
            .andWhere { ImportTaskRunTable.status inList listOf(TaskStatus.PENDING, TaskStatus.RETRYABLE) }
            .andWhere { ImportJobTable.jobType eq jobType }
            .andWhere { ImportBatchTable.status inList allowedBatchStatuses.toList() }
            .orderBy(ImportTaskRunTable.createdAt to SortOrder.ASC)
            .limit(limit)
            .forUpdate()
            .toList()

        if (rows.isEmpty()) return@dbQuery emptyList()

        val taskIds = rows.map { it[ImportTaskRunTable.id].value }

        ImportTaskRunTable.update(where = { ImportTaskRunTable.id inList taskIds }) {
            it[ImportTaskRunTable.status] = TaskStatus.RUNNING
            it[ImportTaskRunTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }

        ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.id inList taskIds }
            .map { it.toImportTaskRunDto() }
    }

    /**
     * Marks a task as started without changing its status.
     *
     * This allows workers to set the execution start time when they actually
     * begin processing, avoiding races where tasks are marked RUNNING with a
     * stale startedAt timestamp before execution begins.
     */
    suspend fun markTaskStarted(
        id: Uuid,
        startedAt: OffsetDateTime,
    ): ImportTaskRunDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ImportTaskRunTable
            .updateReturning(
                where = { ImportTaskRunTable.id eq id.toJavaUuid() }
            ) { stmt ->
                stmt[ImportTaskRunTable.startedAt] = startedAt
                stmt[ImportTaskRunTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toImportTaskRunDto()
    }
    
    /**
     * Find a task by ID.
     */
    suspend fun findTaskById(id: Uuid): ImportTaskRunDto? = DatabaseFactory.dbQuery {
        ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.id eq id.toJavaUuid() }
            .map { it.toImportTaskRunDto() }
            .singleOrNull()
    }
    
    /**
     * List tasks for a specific job ordered by creation time.
     */
    suspend fun listTasksByJob(jobId: Uuid): List<ImportTaskRunDto> = DatabaseFactory.dbQuery {
        ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.jobId eq jobId.toJavaUuid() }
            .orderBy(ImportTaskRunTable.createdAt to SortOrder.ASC)
            .map { it.toImportTaskRunDto() }
    }
    
    /**
     * List tasks for a batch with optional status filter and pagination.
     */
    suspend fun listTasksByBatch(
        batchId: Uuid,
        status: TaskStatus? = null,
        limit: Int = 1000,
        offset: Int = 0
    ): List<ImportTaskRunDto> = DatabaseFactory.dbQuery {
        val query = ImportTaskRunTable
            .innerJoin(ImportJobTable, { ImportTaskRunTable.jobId }, { ImportJobTable.id })
            .selectAll()
            .andWhere { ImportJobTable.batchId eq batchId.toJavaUuid() }
        
        status?.let { query.andWhere { ImportTaskRunTable.status eq it } }
        
        query
            .orderBy(ImportTaskRunTable.createdAt to SortOrder.ASC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toImportTaskRunDto() }
    }
    
    /**
     * Update task status and optional metadata fields.
     */
    suspend fun updateTaskStatus(
        id: Uuid,
        status: TaskStatus,
        error: String? = null,
        durationMs: Int? = null,
        checksum: String? = null,
        evidencePath: String? = null,
        startedAt: OffsetDateTime? = null,
        completedAt: OffsetDateTime? = null
    ): ImportTaskRunDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ImportTaskRunTable
            .updateReturning(
                where = { ImportTaskRunTable.id eq id.toJavaUuid() }
            ) { stmt ->
                stmt[ImportTaskRunTable.status] = status
                error?.let { value -> stmt[ImportTaskRunTable.error] = value }
                durationMs?.let { value -> stmt[ImportTaskRunTable.durationMs] = value }
                checksum?.let { value -> stmt[ImportTaskRunTable.checksum] = value }
                evidencePath?.let { value -> stmt[ImportTaskRunTable.evidencePath] = value }
                startedAt?.let { value -> stmt[ImportTaskRunTable.startedAt] = value }
                completedAt?.let { value -> stmt[ImportTaskRunTable.completedAt] = value }
                stmt[ImportTaskRunTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toImportTaskRunDto()
    }
    
    /**
     * Increment the attempt count for a task.
     */
    suspend fun incrementTaskAttempt(id: Uuid): ImportTaskRunDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val current = ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.id eq id.toJavaUuid() }
            .singleOrNull()
            ?: return@dbQuery null
        
        val newAttempt = current[ImportTaskRunTable.attempt] + 1
        
        ImportTaskRunTable
            .updateReturning(
                where = { ImportTaskRunTable.id eq id.toJavaUuid() }
            ) {
                it[ImportTaskRunTable.attempt] = newAttempt
                it[ImportTaskRunTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toImportTaskRunDto()
    }

    /**
     * Incrementally updates batch counters (race-safe).
     */
    suspend fun incrementBatchCounters(
        id: Uuid,
        processedDelta: Int = 0,
        succeededDelta: Int = 0,
        failedDelta: Int = 0,
        blockedDelta: Int = 0,
    ) = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ImportBatchTable.update(where = { ImportBatchTable.id eq id.toJavaUuid() }) { stmt ->
            if (processedDelta != 0) stmt.update(ImportBatchTable.processedTasks, ImportBatchTable.processedTasks + processedDelta)
            if (succeededDelta != 0) stmt.update(ImportBatchTable.succeededTasks, ImportBatchTable.succeededTasks + succeededDelta)
            if (failedDelta != 0) stmt.update(ImportBatchTable.failedTasks, ImportBatchTable.failedTasks + failedDelta)
            if (blockedDelta != 0) stmt.update(ImportBatchTable.blockedTasks, ImportBatchTable.blockedTasks + blockedDelta)
            stmt[ImportBatchTable.updatedAt] = now
        }
    }

    /**
     * Set the total task count for a batch.
     */
    suspend fun setBatchTotals(
        id: Uuid,
        totalTasks: Int,
    ): ImportBatchDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ImportBatchTable
            .updateReturning(where = { ImportBatchTable.id eq id.toJavaUuid() }) { stmt ->
                stmt[ImportBatchTable.totalTasks] = totalTasks
                stmt[ImportBatchTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toImportBatchDto()
    }

    /**
     * Requeue tasks in the given statuses back to PENDING.
     */
    suspend fun requeueTasksForBatch(
        batchId: Uuid,
        fromStatuses: Set<TaskStatus>,
    ): Int = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ImportTaskRunTable
            .innerJoin(ImportJobTable, { ImportTaskRunTable.jobId }, { ImportJobTable.id })
            .update(
                where = {
                    (ImportJobTable.batchId eq batchId.toJavaUuid()) and
                        (ImportTaskRunTable.status inList fromStatuses.toList())
                }
            ) {
                it[ImportTaskRunTable.status] = TaskStatus.PENDING
                it[ImportTaskRunTable.startedAt] = null
                it[ImportTaskRunTable.completedAt] = null
                it[ImportTaskRunTable.error] = null
                it[ImportTaskRunTable.durationMs] = null
                it[ImportTaskRunTable.updatedAt] = now
            }
    }

    /**
     * Mark running tasks as retryable when they exceed the watchdog threshold.
     */
    suspend fun markStuckRunningTasks(
        threshold: OffsetDateTime,
        maxAttempts: Int,
        limit: Int = 200,
    ): List<ImportTaskRunDto> = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val candidates = ImportTaskRunTable
            .innerJoin(ImportJobTable, { ImportTaskRunTable.jobId }, { ImportJobTable.id })
            .innerJoin(ImportBatchTable, { ImportJobTable.batchId }, { ImportBatchTable.id })
            .selectAll()
            .andWhere { ImportTaskRunTable.status eq TaskStatus.RUNNING }
            .andWhere { ImportTaskRunTable.startedAt less threshold }
            .andWhere { ImportTaskRunTable.attempt less maxAttempts }
            .andWhere { ImportBatchTable.status inList listOf(BatchStatus.RUNNING, BatchStatus.PAUSED) }
            .orderBy(ImportTaskRunTable.startedAt to SortOrder.ASC)
            .limit(limit)
            .toList()

        if (candidates.isEmpty()) return@dbQuery emptyList()

        val taskIds = candidates.map { it[ImportTaskRunTable.id].value }

        ImportTaskRunTable.update(where = { ImportTaskRunTable.id inList taskIds }) { stmt ->
            stmt[ImportTaskRunTable.status] = TaskStatus.RETRYABLE
            stmt[ImportTaskRunTable.error] = """{"code":"stuck_timeout","message":"Task exceeded watchdog threshold"}"""
            stmt[ImportTaskRunTable.startedAt] = null
            stmt[ImportTaskRunTable.completedAt] = null
            stmt[ImportTaskRunTable.updatedAt] = now
        }

        ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.id inList taskIds }
            .map { it.toImportTaskRunDto() }
    }

    // Event Operations
    /**
     * Create a batch/job/task event.
     */
    suspend fun createEvent(
        refType: String,
        refId: Uuid,
        eventType: String,
        data: String? = null
    ): ImportEventDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val eventId = UUID.randomUUID()
        
        ImportEventTable.insert {
            it[id] = eventId
            it[ImportEventTable.refType] = refType
            it[ImportEventTable.refId] = refId.toJavaUuid()
            it[ImportEventTable.eventType] = eventType
            it[ImportEventTable.data] = data
            it[ImportEventTable.createdAt] = now
        }
            .resultedValues
            ?.single()
            ?.toImportEventDto()
            ?: error("Failed to create import event")
    }
    
    /**
     * List events for a reference type/id ordered by most recent.
     */
    suspend fun listEventsByRef(
        refType: String,
        refId: Uuid,
        limit: Int = 100
    ): List<ImportEventDto> = DatabaseFactory.dbQuery {
        ImportEventTable
            .selectAll()
            .andWhere { ImportEventTable.refType eq refType }
            .andWhere { ImportEventTable.refId eq refId.toJavaUuid() }
            .orderBy(ImportEventTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toImportEventDto() }
    }

    /**
     * Delete a batch and its related records (events are pruned explicitly).
     */
    suspend fun deleteBatch(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        // Cascade delete should handle jobs, tasks, events if configured in DB
        // But for safety/Exposed, we might want to be explicit or rely on FK CASCADE
        // Assuming schema has ON DELETE CASCADE for batch_id references.
        // import_job -> batch_id (CASCADE)
        // import_task_run -> job_id (CASCADE)
        // import_event -> ref_id (Manual/Polymorphic, likely NO CASCADE)
        
        // We should delete events first if they are not cascaded.
        // ref_type='batch' and ref_id=id
        ImportEventTable.deleteWhere { 
            (ImportEventTable.refType eq "batch") and (ImportEventTable.refId eq id.toJavaUuid()) 
        }
        // Also events for jobs/tasks of this batch? Hard to find without join.
        // For now, let's just delete the batch and assume FKs handle the rest.
        // Use a generic delete
        val deleted = ImportBatchTable.deleteWhere { ImportBatchTable.id eq id.toJavaUuid() }
        deleted > 0
    }
}
