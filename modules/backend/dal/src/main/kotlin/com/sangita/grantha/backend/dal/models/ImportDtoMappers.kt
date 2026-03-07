package com.sangita.grantha.backend.dal.models

import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.AuditLogTable
import com.sangita.grantha.backend.dal.tables.EntityResolutionCacheTable
import com.sangita.grantha.backend.dal.tables.ImportBatchTable
import com.sangita.grantha.backend.dal.tables.ImportEventTable
import com.sangita.grantha.backend.dal.tables.ImportJobTable
import com.sangita.grantha.backend.dal.tables.ImportSourcesTable
import com.sangita.grantha.backend.dal.tables.ImportTaskRunTable
import com.sangita.grantha.backend.dal.tables.ImportedKrithisTable
import com.sangita.grantha.shared.domain.model.AuditLogDto
import com.sangita.grantha.shared.domain.model.EntityResolutionCacheDto
import com.sangita.grantha.shared.domain.model.ImportBatchDto
import com.sangita.grantha.shared.domain.model.ImportEventDto
import com.sangita.grantha.shared.domain.model.ImportJobDto
import com.sangita.grantha.shared.domain.model.ImportSourceDto
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.exposed.v1.core.ResultRow

// =============================================================================
// Import & Orchestration Mappers: ImportSource, ImportedKrithi, AuditLog,
// Batch, Job, Task, Event, EntityResolutionCache
// (Extracted from DtoMappers.kt as part of TRACK-073)
// =============================================================================

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportSourceDto(): ImportSourceDto = ImportSourceDto(
    id = this[ImportSourcesTable.id].value.toKotlinUuid(),
    name = this[ImportSourcesTable.name],
    baseUrl = this[ImportSourcesTable.baseUrl],
    description = this[ImportSourcesTable.description],
    contactInfo = this[ImportSourcesTable.contactInfo],
    createdAt = this.kotlinInstant(ImportSourcesTable.createdAt),
    updatedAt = this.kotlinInstant(ImportSourcesTable.createdAt), // basic table lacks updatedAt
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportedKrithiDto(): ImportedKrithiDto = ImportedKrithiDto(
    id = this[ImportedKrithisTable.id].value.toKotlinUuid(),
    importSourceId = this[ImportedKrithisTable.importSourceId].toKotlinUuid(),
    importBatchId = this[ImportedKrithisTable.importBatchId]?.toKotlinUuid(),
    sourceKey = this[ImportedKrithisTable.sourceKey],
    rawTitle = this[ImportedKrithisTable.rawTitle],
    rawLyrics = this[ImportedKrithisTable.rawLyrics],
    rawComposer = this[ImportedKrithisTable.rawComposer],
    rawRaga = this[ImportedKrithisTable.rawRaga],
    rawTala = this[ImportedKrithisTable.rawTala],
    rawDeity = this[ImportedKrithisTable.rawDeity],
    rawTemple = this[ImportedKrithisTable.rawTemple],
    rawLanguage = this[ImportedKrithisTable.rawLanguage],
    parsedPayload = this[ImportedKrithisTable.parsedPayload],
    resolutionData = this[ImportedKrithisTable.resolutionData],
    duplicateCandidates = this[ImportedKrithisTable.duplicateCandidates],
    importStatus = this[ImportedKrithisTable.importStatus].toDto(),
    mappedKrithiId = this[ImportedKrithisTable.mappedKrithiId]?.toKotlinUuid(),
    reviewerUserId = this[ImportedKrithisTable.reviewerUserId]?.toKotlinUuid(),
    reviewerNotes = this[ImportedKrithisTable.reviewerNotes],
    reviewedAt = this.kotlinInstantOrNull(ImportedKrithisTable.reviewedAt),
    createdAt = this.kotlinInstant(ImportedKrithisTable.createdAt),
    // TRACK-011: Quality scoring fields
    qualityScore = this[ImportedKrithisTable.qualityScore]?.toDouble(),
    qualityTier = this[ImportedKrithisTable.qualityTier],
    completenessScore = this[ImportedKrithisTable.completenessScore]?.toDouble(),
    resolutionConfidence = this[ImportedKrithisTable.resolutionConfidence]?.toDouble(),
    sourceQuality = this[ImportedKrithisTable.sourceQuality]?.toDouble(),
    validationScore = this[ImportedKrithisTable.validationScore]?.toDouble()
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toAuditLogDto(): AuditLogDto = AuditLogDto(
    id = this[AuditLogTable.id].value.toKotlinUuid(),
    actorUserId = this[AuditLogTable.actorUserId]?.toKotlinUuid(),
    actorIp = this[AuditLogTable.actorIp],
    action = this[AuditLogTable.action],
    entityTable = this[AuditLogTable.entityTable],
    entityId = this[AuditLogTable.entityId]?.toKotlinUuid(),
    changedAt = this.kotlinInstant(AuditLogTable.changedAt),
    diff = this[AuditLogTable.diff],
    metadata = this[AuditLogTable.metadata]
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportBatchDto(): ImportBatchDto = ImportBatchDto(
    id = this[ImportBatchTable.id].value.toKotlinUuid(),
    sourceManifest = this[ImportBatchTable.sourceManifest],
    createdByUserId = this[ImportBatchTable.createdByUserId]?.toKotlinUuid(),
    status = this[ImportBatchTable.status].toDto(),
    totalTasks = this[ImportBatchTable.totalTasks],
    processedTasks = this[ImportBatchTable.processedTasks],
    succeededTasks = this[ImportBatchTable.succeededTasks],
    failedTasks = this[ImportBatchTable.failedTasks],
    blockedTasks = this[ImportBatchTable.blockedTasks],
    startedAt = this.kotlinInstantOrNull(ImportBatchTable.startedAt),
    completedAt = this.kotlinInstantOrNull(ImportBatchTable.completedAt),
    createdAt = this.kotlinInstant(ImportBatchTable.createdAt),
    updatedAt = this.kotlinInstant(ImportBatchTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportJobDto(): ImportJobDto = ImportJobDto(
    id = this[ImportJobTable.id].value.toKotlinUuid(),
    batchId = this[ImportJobTable.batchId].toKotlinUuid(),
    jobType = this[ImportJobTable.jobType].toDto(),
    status = this[ImportJobTable.status].toDto(),
    retryCount = this[ImportJobTable.retryCount],
    payload = this[ImportJobTable.payload],
    result = this[ImportJobTable.result],
    startedAt = this.kotlinInstantOrNull(ImportJobTable.startedAt),
    completedAt = this.kotlinInstantOrNull(ImportJobTable.completedAt),
    createdAt = this.kotlinInstant(ImportJobTable.createdAt),
    updatedAt = this.kotlinInstant(ImportJobTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportTaskRunDto(): ImportTaskRunDto = ImportTaskRunDto(
    id = this[ImportTaskRunTable.id].value.toKotlinUuid(),
    jobId = this[ImportTaskRunTable.jobId].toKotlinUuid(),
    krithiKey = this[ImportTaskRunTable.krithiKey],
    idempotencyKey = this[ImportTaskRunTable.idempotencyKey],
    status = this[ImportTaskRunTable.status].toDto(),
    attempt = this[ImportTaskRunTable.attempt],
    sourceUrl = this[ImportTaskRunTable.sourceUrl],
    error = this[ImportTaskRunTable.error],
    durationMs = this[ImportTaskRunTable.durationMs],
    checksum = this[ImportTaskRunTable.checksum],
    evidencePath = this[ImportTaskRunTable.evidencePath],
    startedAt = this.kotlinInstantOrNull(ImportTaskRunTable.startedAt),
    completedAt = this.kotlinInstantOrNull(ImportTaskRunTable.completedAt),
    createdAt = this.kotlinInstant(ImportTaskRunTable.createdAt),
    updatedAt = this.kotlinInstant(ImportTaskRunTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportEventDto(): ImportEventDto = ImportEventDto(
    id = this[ImportEventTable.id].value.toKotlinUuid(),
    refType = this[ImportEventTable.refType],
    refId = this[ImportEventTable.refId].toKotlinUuid(),
    eventType = this[ImportEventTable.eventType],
    data = this[ImportEventTable.data],
    createdAt = this.kotlinInstant(ImportEventTable.createdAt)
)

// TRACK-013: Entity Resolution Cache Mapper
@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toEntityResolutionCacheDto(): EntityResolutionCacheDto = EntityResolutionCacheDto(
    id = this[EntityResolutionCacheTable.id].value.toKotlinUuid(),
    entityType = this[EntityResolutionCacheTable.entityType],
    rawName = this[EntityResolutionCacheTable.rawName],
    normalizedName = this[EntityResolutionCacheTable.normalizedName],
    resolvedEntityId = this[EntityResolutionCacheTable.resolvedEntityId].toKotlinUuid(),
    confidence = this[EntityResolutionCacheTable.confidence],
    createdAt = this.kotlinInstant(EntityResolutionCacheTable.createdAt),
    updatedAt = this.kotlinInstant(EntityResolutionCacheTable.updatedAt)
)
