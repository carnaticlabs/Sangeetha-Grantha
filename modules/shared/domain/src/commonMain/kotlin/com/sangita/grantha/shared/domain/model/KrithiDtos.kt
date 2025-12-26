package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
enum class WorkflowStateDto { DRAFT, IN_REVIEW, PUBLISHED, ARCHIVED }

@Serializable
enum class LanguageCodeDto { SA, TA, TE, KN, ML, HI, EN }

@Serializable
enum class ScriptCodeDto { DEVANAGARI, TAMIL, TELUGU, KANNADA, MALAYALAM, LATIN }

@Serializable
enum class RagaSectionDto { PALLAVI, ANUPALLAVI, CHARANAM, OTHER }

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
    val primaryLanguage: LanguageCodeDto,
    val isRagamalika: Boolean = false,
    val workflowState: WorkflowStateDto,
    val sahityaSummary: String? = null,
    val notes: String? = null,
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

// Simple search request/response DTOs for /v1/krithis/search

@Serializable
data class KrithiSearchRequest(
    val query: String? = null,
    val lyric: String? = null,
    val composerId: String? = null,
    val ragaId: String? = null,
    val talaId: String? = null,
    val deityId: String? = null,
    val templeId: String? = null,
    val language: LanguageCodeDto? = null,
    val page: Int = 0,
    val pageSize: Int = 50,
)

@Serializable
data class KrithiSearchResult(
    val items: List<KrithiDto>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
)
