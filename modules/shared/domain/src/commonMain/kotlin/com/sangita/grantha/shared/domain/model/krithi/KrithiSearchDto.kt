package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import com.sangita.grantha.shared.domain.validation.ValueRange
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

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
    @ValueRange(min = 0, max = 10_000)
    val page: Int = 0,
    @ValueRange(min = 1, max = 200)
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
