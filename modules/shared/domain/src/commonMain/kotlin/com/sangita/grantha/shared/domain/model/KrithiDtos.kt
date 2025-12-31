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
enum class MusicalFormDto { KRITHI, VARNAM, SWARAJATHI }

@Serializable
enum class NotationTypeDto { SWARA, JATHI }

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
    val musicalForm: MusicalFormDto = MusicalFormDto.KRITHI,
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

@Serializable
data class KrithiNotationVariantDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val notationType: NotationTypeDto,
    @Serializable(with = UuidSerializer::class)
    val talaId: Uuid? = null,
    val kalai: Int,
    val eduppuOffsetBeats: Int? = null,
    val variantLabel: String? = null,
    val sourceReference: String? = null,
    val isPrimary: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class KrithiNotationRowDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val notationVariantId: Uuid,
    @Serializable(with = UuidSerializer::class)
    val sectionId: Uuid,
    val orderIndex: Int,
    val swaraText: String,
    val sahityaText: String? = null,
    val talaMarkers: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class KrithiNotationSectionGroupDto(
    @Serializable(with = UuidSerializer::class)
    val sectionId: Uuid,
    val sectionOrderIndex: Int,
    val rows: List<KrithiNotationRowDto>,
)

@Serializable
data class KrithiNotationVariantWithRowsDto(
    val variant: KrithiNotationVariantDto,
    val sections: List<KrithiNotationSectionGroupDto>,
)

@Serializable
data class KrithiNotationResponseDto(
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val musicalForm: MusicalFormDto,
    @Serializable(with = UuidSerializer::class)
    val talaId: Uuid? = null,
    val variants: List<KrithiNotationVariantWithRowsDto>,
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
data class RagaRefDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val orderIndex: Int = 0,
)

@Serializable
data class KrithiSummary(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val composerName: String,
    val primaryLanguage: LanguageCodeDto,
    val ragas: List<RagaRefDto>,
)

@Serializable
data class KrithiSearchResult(
    val items: List<KrithiSummary>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
)
