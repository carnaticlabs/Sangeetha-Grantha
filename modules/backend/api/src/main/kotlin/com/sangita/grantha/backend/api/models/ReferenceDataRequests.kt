package com.sangita.grantha.backend.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ComposerCreateRequest(
    val name: String,
    val nameNormalized: String? = null,
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val place: String? = null,
    val notes: String? = null,
)

@Serializable
data class ComposerUpdateRequest(
    val name: String? = null,
    val nameNormalized: String? = null,
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val place: String? = null,
    val notes: String? = null,
)

@Serializable
data class RagaCreateRequest(
    val name: String,
    val nameNormalized: String? = null,
    val melakartaNumber: Int? = null,
    val parentRagaId: String? = null,
    val arohanam: String? = null,
    val avarohanam: String? = null,
    val notes: String? = null,
)

@Serializable
data class RagaUpdateRequest(
    val name: String? = null,
    val nameNormalized: String? = null,
    val melakartaNumber: Int? = null,
    val parentRagaId: String? = null,
    val arohanam: String? = null,
    val avarohanam: String? = null,
    val notes: String? = null,
)

@Serializable
data class TalaCreateRequest(
    val name: String,
    val nameNormalized: String? = null,
    val beatCount: Int? = null,
    val angaStructure: String? = null,
    val notes: String? = null,
)

@Serializable
data class TalaUpdateRequest(
    val name: String? = null,
    val nameNormalized: String? = null,
    val beatCount: Int? = null,
    val angaStructure: String? = null,
    val notes: String? = null,
)

@Serializable
data class TempleCreateRequest(
    val name: String,
    val nameNormalized: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val primaryDeityId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
)

@Serializable
data class TempleUpdateRequest(
    val name: String? = null,
    val nameNormalized: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val primaryDeityId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
)

@Serializable
data class DeityCreateRequest(
    val name: String,
    val nameNormalized: String? = null,
    val description: String? = null,
)

@Serializable
data class DeityUpdateRequest(
    val name: String? = null,
    val nameNormalized: String? = null,
    val description: String? = null,
)

@Serializable
data class SampradayaCreateRequest(
    val name: String,
    val nameNormalized: String? = null,
    val description: String? = null,
)

@Serializable
data class SampradayaUpdateRequest(
    val name: String? = null,
    val nameNormalized: String? = null,
    val description: String? = null,
)
