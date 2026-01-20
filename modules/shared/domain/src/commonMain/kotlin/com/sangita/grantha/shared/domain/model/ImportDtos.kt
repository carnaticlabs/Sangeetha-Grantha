package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
enum class ImportStatusDto { PENDING, IN_REVIEW, APPROVED, MAPPED, REJECTED, DISCARDED }

@Serializable
data class ImportSourceDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val baseUrl: String? = null,
    val description: String? = null,
    val contactInfo: String? = null,
    val createdAt: Instant,
)

@Serializable
data class ImportedKrithiDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val importSourceId: Uuid,
    val sourceKey: String? = null,
    val rawTitle: String? = null,
    val rawLyrics: String? = null,
    val rawComposer: String? = null,
    val rawRaga: String? = null,
    val rawTala: String? = null,
    val rawDeity: String? = null,
    val rawTemple: String? = null,
    val rawLanguage: String? = null,
    val parsedPayload: String? = null, // JSON string; services can parse as needed
    val importStatus: ImportStatusDto,
    @Serializable(with = UuidSerializer::class)
    val mappedKrithiId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val reviewerUserId: Uuid? = null,
    val reviewerNotes: String? = null,
    val reviewedAt: Instant? = null,
    val createdAt: Instant,
)

// Bulk Import Orchestration DTOs
@Serializable
enum class BatchStatusDto { PENDING, RUNNING, PAUSED, SUCCEEDED, FAILED, CANCELLED }

@Serializable
enum class JobTypeDto { MANIFEST_INGEST, SCRAPE, ENRICH, ENTITY_RESOLUTION, REVIEW_PREP }

@Serializable
enum class TaskStatusDto { PENDING, RUNNING, SUCCEEDED, FAILED, RETRYABLE, BLOCKED, CANCELLED }

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
    val payload: String? = null, // JSON string
    val result: String? = null, // JSON string
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
    val status: TaskStatusDto,
    val attempt: Int,
    val sourceUrl: String? = null,
    val error: String? = null, // JSON string
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
    val data: String? = null, // JSON string
    val createdAt: Instant,
)
