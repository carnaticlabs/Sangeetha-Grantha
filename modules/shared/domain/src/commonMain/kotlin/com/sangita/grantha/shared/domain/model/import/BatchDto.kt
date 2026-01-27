package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ImportBatchDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val sourceManifest: String,
    @Serializable(with = UuidSerializer::class)
    val createdByUserId: Uuid? = null,
    val status: BatchStatusDto,
    val totalTasks: Int,
    val processedTasks: Int,
    val succeededTasks: Int,
    val failedTasks: Int,
    val blockedTasks: Int,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class ImportJobDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val batchId: Uuid,
    val jobType: JobTypeDto,
    val status: TaskStatusDto,
    val retryCount: Int,
    val payload: String? = null,
    val result: String? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class ImportTaskRunDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val jobId: Uuid,
    val krithiKey: String? = null,
    val idempotencyKey: String? = null,
    val status: TaskStatusDto,
    val attempt: Int,
    val sourceUrl: String? = null,
    val error: String? = null,
    val durationMs: Int? = null,
    val checksum: String? = null,
    val evidencePath: String? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class ImportEventDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val refType: String,
    @Serializable(with = UuidSerializer::class)
    val refId: Uuid,
    val eventType: String,
    val data: String? = null,
    val createdAt: Instant,
)
