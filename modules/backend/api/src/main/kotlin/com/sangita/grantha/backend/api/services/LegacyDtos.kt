package com.sangita.grantha.backend.api.services

import com.sangita.grantha.shared.domain.model.RagaSectionDto
import kotlinx.serialization.Serializable

/**
 * TRACK-032: One lyric variant per language/script from multi-script pages.
 * Retained for backward-compatible deserialization of legacy `parsed_payload` values.
 */
@Serializable
data class ScrapedLyricVariantDto(
    val language: String,
    val script: String,
    val lyrics: String? = null,
    val sections: List<ScrapedSectionDto>? = null
)

/**
 * Scraped krithi metadata from legacy Kotlin scraper.
 * Retained for backward-compatible deserialization of legacy `parsed_payload` values.
 * New extraction pipelines MUST produce CanonicalExtractionDto — do NOT add fields here.
 */
@Serializable
data class ScrapedKrithiMetadata(
    val title: String,
    val composer: String? = null,
    val raga: String? = null,
    val tala: String? = null,
    val deity: String? = null,
    val temple: String? = null,
    val templeUrl: String? = null,
    val language: String? = null,
    val lyrics: String? = null,
    val notation: String? = null,
    val sections: List<ScrapedSectionDto>? = null,
    val lyricVariants: List<ScrapedLyricVariantDto>? = null,
    val templeDetails: ScrapedTempleDetails? = null,
    val warnings: List<String>? = null
)

/**
 * Temple details from legacy scraper.
 * Retained for backward-compatible deserialization of legacy `parsed_payload` values.
 */
@Serializable
data class ScrapedTempleDetails(
    val name: String,
    val deity: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null
)

/**
 * Section DTO from legacy scraper.
 * Retained for backward-compatible deserialization and as adapter type for StructuralVotingEngine.
 */
@Serializable
data class ScrapedSectionDto(
    val type: RagaSectionDto,
    val text: String,
    val label: String? = null
)
