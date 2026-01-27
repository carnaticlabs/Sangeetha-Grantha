package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

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
    val sections: List<KrithiSectionDto>,
    @Serializable(with = UuidSerializer::class)
    val talaId: Uuid? = null,
    val variants: List<KrithiNotationVariantWithRowsDto>,
)
