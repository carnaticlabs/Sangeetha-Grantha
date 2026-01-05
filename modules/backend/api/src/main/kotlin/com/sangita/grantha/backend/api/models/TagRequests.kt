package com.sangita.grantha.backend.api.models

import com.sangita.grantha.shared.domain.model.TagCategoryDto
import kotlinx.serialization.Serializable

@Serializable
data class TagCreateRequest(
    val category: TagCategoryDto,
    val slug: String,
    val displayNameEn: String,
    val descriptionEn: String? = null,
)

@Serializable
data class TagUpdateRequest(
    val category: TagCategoryDto? = null,
    val slug: String? = null,
    val displayNameEn: String? = null,
    val descriptionEn: String? = null,
)

