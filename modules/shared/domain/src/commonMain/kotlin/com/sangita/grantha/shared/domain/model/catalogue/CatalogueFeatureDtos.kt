package com.sangita.grantha.shared.domain.model.catalogue

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
enum class CatalogueFeatureStateDto { DRAFT, PUBLISHED }

@Serializable
data class CatalogueFeatureDto(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    @Serializable(with = UuidSerializer::class) val targetKrithiId: Uuid,
    val heading: String,
    val summary: String,
    val state: CatalogueFeatureStateDto,
    val revision: Long,
    val orderIndex: Int,
)

@Serializable
data class CatalogueFeatureListDto(val items: List<CatalogueFeatureDto>)

@Serializable
data class CatalogueFeatureCreateDto(
    @Serializable(with = UuidSerializer::class) val targetKrithiId: Uuid,
    val heading: String,
    val summary: String,
)

@Serializable
data class CatalogueFeatureUpdateDto(
    @Serializable(with = UuidSerializer::class) val targetKrithiId: Uuid,
    val heading: String,
    val summary: String,
    val expectedRevision: Long,
)

@Serializable
data class CatalogueFeatureRevisionDto(val expectedRevision: Long)

@Serializable
data class CatalogueFeatureOrderItemDto(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    val expectedRevision: Long,
)

@Serializable
data class CatalogueFeatureOrderDto(val items: List<CatalogueFeatureOrderItemDto>)
