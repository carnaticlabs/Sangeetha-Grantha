package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class SemanticSearchRequest(
    val query: String,
    @Serializable(with = UuidSerializer::class)
    val composerId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val ragaId: Uuid? = null,
    val limit: Int = 20,
)

@Serializable
data class SemanticSearchResultItem(
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val title: String,
    val composerName: String,
    val ragaName: String? = null,
    val talaName: String? = null,
    val documentKind: String,
    val matchedContent: String,
    val similarityScore: Double,
    val lexicalScore: Double? = null,
    val rrfScore: Double? = null,
)

@Serializable
data class SemanticSearchResponse(
    val query: String,
    val totalMatches: Int,
    val items: List<SemanticSearchResultItem>,
)
