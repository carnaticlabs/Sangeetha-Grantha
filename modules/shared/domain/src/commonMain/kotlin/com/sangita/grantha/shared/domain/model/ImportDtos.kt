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
