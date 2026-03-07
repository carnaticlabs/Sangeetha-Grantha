package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.JobType
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.backend.dal.models.toImportBatchDto
import com.sangita.grantha.backend.dal.models.toImportJobDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.ImportBatchTable
import com.sangita.grantha.backend.dal.tables.ImportJobTable
import com.sangita.grantha.shared.domain.model.ImportBatchDto
import com.sangita.grantha.shared.domain.model.ImportJobDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*

/**
 * Repository for import batch and job operations.
 * Task operations are in BulkImportTaskRepository.
 * Event operations are in BulkImportEventRepository.
 */
class BulkImportRepository {

    // --- Batch Operations ---

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

    /**
     * Atomically increments total_tasks using SQL addition (race-safe).
     * Use this when adding tasks for subsequent job types (EXTRACTION, ENRICHMENT)
     * to avoid the read-then-write race that causes the 50% stall bug.
     */
    suspend fun incrementBatchTotalTasks(
        id: Uuid,
        delta: Int,
    ) = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ImportBatchTable.update(where = { ImportBatchTable.id eq id.toJavaUuid() }) { stmt ->
            stmt.update(ImportBatchTable.totalTasks, ImportBatchTable.totalTasks + delta)
            stmt[ImportBatchTable.updatedAt] = now
        }
    }

    // --- Job Operations ---

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
}
