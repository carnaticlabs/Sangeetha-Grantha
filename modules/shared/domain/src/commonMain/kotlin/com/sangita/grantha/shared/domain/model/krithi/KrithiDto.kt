package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class KrithiDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val title: String,
    val incipit: String? = null,
    val titleNormalized: String,
    val incipitNormalized: String? = null,
    @Serializable(with = UuidSerializer::class)
    val composerId: Uuid,
    @Serializable(with = UuidSerializer::class)
    val primaryRagaId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val talaId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val deityId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val templeId: Uuid? = null,
    val musicalForm: MusicalFormDto = MusicalFormDto.UNESTABLISHED,
    val primaryLanguage: LanguageCodeDto,
    val isRagamalika: Boolean = false,
    val workflowState: WorkflowStateDto,
    val sahityaSummary: String? = null,
    val notes: String? = null,
    val vibhaktiCase: String? = null,
    val vibhaktiStem: String? = null,
    val ragaMudraKind: String = "NONE",
    val ragaMudraPhrase: String? = null,
    val yatiPattern: String = "NONE",
    val isManipravala: Boolean = false,
    val occasionNote: String? = null,
    @Serializable(with = UuidSerializer::class)
    val createdByUserId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val updatedByUserId: Uuid? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class KrithiRagaDto(
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    @Serializable(with = UuidSerializer::class)
    val ragaId: Uuid,
    val orderIndex: Int = 0,
    val section: RagaSectionDto? = null,
    val notes: String? = null,
)

@Serializable
data class KrithiLyricVariantDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val language: LanguageCodeDto,
    val script: ScriptCodeDto,
    val transliterationScheme: String? = null,
    val isPrimary: Boolean = false,
    val variantLabel: String? = null,
    val sourceReference: String? = null,
    val lyrics: String,
    @Serializable(with = UuidSerializer::class)
    val createdByUserId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val updatedByUserId: Uuid? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class KrithiCycleMembershipDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    @Serializable(with = UuidSerializer::class)
    val tagId: Uuid,
    val sequenceOrder: Int = 0,
    val role: String,
    val axisValue: String? = null,
    val discriminativeAttributes: String = "{}",
    val createdAt: Instant,
)
