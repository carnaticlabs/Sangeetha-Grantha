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

class BulkImportRepository {
    
    // Batch Operations
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
    
    suspend fun findBatchById(id: Uuid): ImportBatchDto? = DatabaseFactory.dbQuery {
        ImportBatchTable
            .selectAll()
            .andWhere { ImportBatchTable.id eq id.toJavaUuid() }
            .map { it.toImportBatchDto() }
            .singleOrNull()
    }
    
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
    
    suspend fun findJobById(id: Uuid): ImportJobDto? = DatabaseFactory.dbQuery {
        ImportJobTable
            .selectAll()
            .andWhere { ImportJobTable.id eq id.toJavaUuid() }
            .map { it.toImportJobDto() }
            .singleOrNull()
    }
    
    suspend fun listJobsByBatch(batchId: Uuid): List<ImportJobDto> = DatabaseFactory.dbQuery {
        ImportJobTable
            .selectAll()
            .andWhere { ImportJobTable.batchId eq batchId.toJavaUuid() }
            .orderBy(ImportJobTable.createdAt to SortOrder.ASC)
            .map { it.toImportJobDto() }
    }
    
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
    suspend fun createTask(
        jobId: Uuid,
        krithiKey: String? = null,
        sourceUrl: String? = null
    ): ImportTaskRunDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val taskId = UUID.randomUUID()
        
        ImportTaskRunTable.insert {
            it[id] = taskId
            it[ImportTaskRunTable.jobId] = jobId.toJavaUuid()
            it[ImportTaskRunTable.krithiKey] = krithiKey
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
    
    suspend fun createTasks(
        jobId: Uuid,
        tasks: List<Pair<String?, String?>> // List of (krithiKey, sourceUrl) pairs
    ): List<ImportTaskRunDto> = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        
        tasks.map { (krithiKey, sourceUrl) ->
            val taskId = UUID.randomUUID()
            ImportTaskRunTable.insert {
                it[id] = taskId
                it[ImportTaskRunTable.jobId] = jobId.toJavaUuid()
                it[ImportTaskRunTable.krithiKey] = krithiKey
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
    }
    
    /**
     * Atomically claims the next pending task (sets it to RUNNING) and returns it.
     *
     * Implemented as "SELECT ... FOR UPDATE SKIP LOCKED" + "UPDATE" inside one DB transaction,
     * to prevent multiple workers from taking the same task.
     */
    suspend fun claimNextPendingTask(
        jobType: JobType,
        allowedBatchStatuses: Set<BatchStatus> = setOf(BatchStatus.RUNNING),
    ): ImportTaskRunDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val row = ImportTaskRunTable
            .innerJoin(ImportJobTable, { ImportTaskRunTable.jobId }, { ImportJobTable.id })
            .innerJoin(ImportBatchTable, { ImportJobTable.batchId }, { ImportBatchTable.id })
            .selectAll()
            .andWhere { ImportTaskRunTable.status eq TaskStatus.PENDING }
            .andWhere { ImportJobTable.jobType eq jobType }
            .andWhere { ImportBatchTable.status inList allowedBatchStatuses.toList() }
            .orderBy(ImportTaskRunTable.createdAt to SortOrder.ASC)
            .limit(1)
            .forUpdate()
            .singleOrNull()
            ?: return@dbQuery null

        val taskId: Uuid = row[ImportTaskRunTable.id].value.toKotlinUuid()

        ImportTaskRunTable.update(where = { ImportTaskRunTable.id eq taskId.toJavaUuid() }) {
            it[ImportTaskRunTable.status] = TaskStatus.RUNNING
            it[ImportTaskRunTable.startedAt] = now
            it[ImportTaskRunTable.updatedAt] = now
        }

        ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.id eq taskId.toJavaUuid() }
            .map { it.toImportTaskRunDto() }
            .singleOrNull()
    }
    
    suspend fun findTaskById(id: Uuid): ImportTaskRunDto? = DatabaseFactory.dbQuery {
        ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.id eq id.toJavaUuid() }
            .map { it.toImportTaskRunDto() }
            .singleOrNull()
    }
    
    suspend fun listTasksByJob(jobId: Uuid): List<ImportTaskRunDto> = DatabaseFactory.dbQuery {
        ImportTaskRunTable
            .selectAll()
            .andWhere { ImportTaskRunTable.jobId eq jobId.toJavaUuid() }
            .orderBy(ImportTaskRunTable.createdAt to SortOrder.ASC)
            .map { it.toImportTaskRunDto() }
    }
    
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
                it[ImportTaskRunTable.updatedAt] = now
            }
    }
    
    // Event Operations
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
}
