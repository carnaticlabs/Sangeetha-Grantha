package com.sangita.grantha.backend.api.models

import com.sangita.grantha.shared.domain.model.ImportStatusDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ImportKrithiRequest(
    val source: String,
    val sourceKey: String? = null,
    val batchId: String? = null,
    val rawTitle: String? = null,
    val rawLyrics: String? = null,
    val rawComposer: String? = null,
    val rawRaga: String? = null,
    val rawTala: String? = null,
    val rawDeity: String? = null,
    val rawTemple: String? = null,
    val rawLanguage: String? = null,
    val rawPayload: JsonElement? = null,
)

@Serializable
data class ImportOverridesDto(
    val title: String? = null,
    val raga: String? = null,
    val composer: String? = null,
    val tala: String? = null,
    val language: String? = null,
    val lyrics: String? = null
)

@Serializable
data class ImportReviewRequest(
    val status: ImportStatusDto,
    val mappedKrithiId: String? = null,
    val reviewerNotes: String? = null,
    val overrides: ImportOverridesDto? = null
)
