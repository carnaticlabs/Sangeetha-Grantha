package com.sangita.grantha.backend.api.models

import com.sangita.grantha.shared.domain.model.NotationTypeDto
import kotlinx.serialization.Serializable

@Serializable
data class NotationVariantCreateRequest(
    val notationType: NotationTypeDto,
    val talaId: String? = null,
    val kalai: Int = 1,
    val eduppuOffsetBeats: Int? = null,
    val variantLabel: String? = null,
    val sourceReference: String? = null,
    val isPrimary: Boolean = false,
)

@Serializable
data class NotationVariantUpdateRequest(
    val notationType: NotationTypeDto? = null,
    val talaId: String? = null,
    val kalai: Int? = null,
    val eduppuOffsetBeats: Int? = null,
    val variantLabel: String? = null,
    val sourceReference: String? = null,
    val isPrimary: Boolean? = null,
)

@Serializable
data class NotationRowCreateRequest(
    val sectionId: String,
    val orderIndex: Int = 0,
    val swaraText: String,
    val sahityaText: String? = null,
    val talaMarkers: String? = null,
)

@Serializable
data class NotationRowUpdateRequest(
    val sectionId: String? = null,
    val orderIndex: Int? = null,
    val swaraText: String? = null,
    val sahityaText: String? = null,
    val talaMarkers: String? = null,
)
