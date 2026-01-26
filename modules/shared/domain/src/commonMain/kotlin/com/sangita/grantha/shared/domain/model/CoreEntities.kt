package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ComposerDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val nameNormalized: String,
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val place: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class RagaDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val nameNormalized: String,
    val melakartaNumber: Int? = null,
    @Serializable(with = UuidSerializer::class)
    val parentRagaId: Uuid? = null,
    val arohanam: String? = null,
    val avarohanam: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class TalaDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val nameNormalized: String,
    val angaStructure: String? = null,
    val beatCount: Int? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class DeityDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val nameNormalized: String,
    val description: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class TempleDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val nameNormalized: String,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    @Serializable(with = UuidSerializer::class)
    val primaryDeityId: Uuid? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class UserDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val email: String?,
    val fullName: String,
    val displayName: String? = null,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class RoleAssignmentDto(
    @Serializable(with = UuidSerializer::class)
    val userId: Uuid,
    val roleCode: String,
    val assignedAt: Instant,
)

@Serializable
data class ReferenceDataStatsDto(
    val composerCount: Long,
    val ragaCount: Long,
    val talaCount: Long,
    val deityCount: Long,
    val templeCount: Long,
    val tagCount: Long,
    val sampradayaCount: Long,
    val musicalFormCount: Long = 18,
    val languageCount: Long = 12
)
