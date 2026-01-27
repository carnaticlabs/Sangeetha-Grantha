package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

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
    @Serializable(with = UuidSerializer::class)
    val importBatchId: Uuid? = null,
    val sourceKey: String? = null,
    val rawTitle: String? = null,
    val rawLyrics: String? = null,
    val rawComposer: String? = null,
    val rawRaga: String? = null,
    val rawTala: String? = null,
    val rawDeity: String? = null,
    val rawTemple: String? = null,
    val rawLanguage: String? = null,
    val parsedPayload: String? = null,
    val resolutionData: String? = null,
    val duplicateCandidates: String? = null,
    val importStatus: ImportStatusDto,
    @Serializable(with = UuidSerializer::class)
    val mappedKrithiId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val reviewerUserId: Uuid? = null,
    val reviewerNotes: String? = null,
    val reviewedAt: Instant? = null,
    val createdAt: Instant,
    val qualityScore: Double? = null,
    val qualityTier: String? = null,
    val completenessScore: Double? = null,
    val resolutionConfidence: Double? = null,
    val sourceQuality: Double? = null,
    val validationScore: Double? = null,
)

@Serializable
data class EntityResolutionCacheDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val entityType: String,
    val rawName: String,
    val normalizedName: String,
    @Serializable(with = UuidSerializer::class)
    val resolvedEntityId: Uuid,
    val confidence: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
