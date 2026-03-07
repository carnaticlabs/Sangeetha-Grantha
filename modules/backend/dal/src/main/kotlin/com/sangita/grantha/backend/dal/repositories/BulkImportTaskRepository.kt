package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.JobType
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.backend.dal.models.toImportTaskRunDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.ImportBatchTable
import com.sangita.grantha.backend.dal.tables.ImportJobTable
import com.sangita.grantha.backend.dal.tables.ImportTaskRunTable
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*

/**
 * Repository for import task run operations.
 * Extracted from BulkImportRepository as part of TRACK-074.
 */
class BulkImportTaskRepository {

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

        ImportBatchTable.update(where = { ImportBatchTable.id eq javaBatchId }) {
            it.update(ImportBatchTable.totalTasks, ImportBatchTable.totalTasks + 1)
            it[ImportBatchTable.updatedAt] = now
        }

        ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.id eq taskId }
            .single()
            .toImportTaskRunDto()
    }

    /**
     * Create tasks in bulk for a job while deduplicating idempotency keys.
     */
    suspend fun createTasks(
        jobId: Uuid,
        batchId: Uuid,
        tasks: List<Pair<String?, String?>>
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

        val insertedTasks = toInsert.map { task ->
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

        ImportBatchTable.update(where = { ImportBatchTable.id eq javaBatchId }) {
            it.update(ImportBatchTable.totalTasks, ImportBatchTable.totalTasks + insertedTasks.size)
            it[ImportBatchTable.updatedAt] = now
        }

        insertedTasks
    }

    /**
     * Atomically claims the next batch of pending tasks.
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
}
