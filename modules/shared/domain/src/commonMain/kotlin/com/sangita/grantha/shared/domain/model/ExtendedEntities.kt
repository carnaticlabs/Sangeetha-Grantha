package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

// --- Krithi sections ---

@Serializable
data class KrithiSectionDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val sectionType: String,          // e.g. PALLAVI, ANUPALLAVI, CHARANAM, etc.
    val orderIndex: Int,
    val label: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class KrithiLyricSectionDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val lyricVariantId: Uuid,
    @Serializable(with = UuidSerializer::class)
    val sectionId: Uuid,
    val text: String,
    val normalizedText: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

// --- Tags ---

@Serializable
enum class TagCategoryDto {
    BHAVA,
    FESTIVAL,
    PHILOSOPHY,
    KSHETRA,
    STOTRA_STYLE,
    NAYIKA_BHAVA,
    OTHER,
}

@Serializable
data class TagDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val category: TagCategoryDto,
    val slug: String,
    val displayNameEn: String,
    val descriptionEn: String? = null,
    val createdAt: Instant,
)

@Serializable
data class KrithiTagDto(
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    @Serializable(with = UuidSerializer::class)
    val tagId: Uuid,
    val source: String = "manual",    // manual | import
    val confidence: Int? = null,       // 0–100
)

// --- Sampradaya ---

@Serializable
enum class SampradayaTypeDto { PATHANTARAM, BANI, SCHOOL }

@Serializable
data class SampradayaDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val type: SampradayaTypeDto,
    val description: String? = null,
    val createdAt: Instant,
)

// --- Temple names / aliases ---

@Serializable
data class TempleNameDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val templeId: Uuid,
    val languageCode: LanguageCodeDto,
    val scriptCode: ScriptCodeDto,
    val name: String,
    val normalizedName: String,
    val isPrimary: Boolean = false,
    val source: String = "manual",    // manual | import
    val createdAt: Instant,
)

// --- Lyric Variants with Sections ---

@Serializable
data class KrithiLyricVariantWithSectionsDto(
    val variant: KrithiLyricVariantDto,
    val sections: List<KrithiLyricSectionDto>,
)
