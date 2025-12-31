package com.sangita.grantha.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TransliterationRequest(
    val content: String,
    val sourceScript: String? = null,
    val targetScript: String
)

@Serializable
data class TransliterationResponse(
    val transliterated: String,
    val targetScript: String
)

@Serializable
data class ScrapeRequest(
    val url: String
)

@Serializable
data class ValidateKrithiRequest(
    val checkRaga: Boolean = true,
    val checkTala: Boolean = true
)

@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>
)
