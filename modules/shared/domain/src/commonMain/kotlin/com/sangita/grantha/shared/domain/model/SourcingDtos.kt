package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

// =============================================================================
// TRACK-045: Sourcing & Extraction Monitoring — Shared Domain DTOs
// Source Registry + Extraction Queue + Pagination
// (Evidence, Voting, Quality & Variant DTOs moved to EvidenceVotingDtos.kt)
// =============================================================================

// --- Source Registry ---

@Serializable
data class ImportSourceDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val baseUrl: String? = null,
    val description: String? = null,
    val contactInfo: String? = null,
    val sourceTier: Int = 5,
    val supportedFormats: List<String> = emptyList(),
    val composerAffinity: Map<String, Double> = emptyMap(),
    val lastHarvestedAt: Instant? = null,
    val isActive: Boolean = true,
    val krithiCount: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class ImportSourceDetailDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val baseUrl: String? = null,
    val description: String? = null,
    val sourceTier: Int = 5,
    val supportedFormats: List<String> = emptyList(),
    val composerAffinity: Map<String, Double> = emptyMap(),
    val lastHarvestedAt: Instant? = null,
    val isActive: Boolean = true,
    val krithiCount: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
    val contributionStats: ContributionStatsDto? = null,
)

@Serializable
data class ContributionStatsDto(
    val totalKrithis: Int = 0,
    val fieldBreakdown: Map<String, Int> = emptyMap(),
    val avgConfidence: Double = 0.0,
    val extractionSuccessRate: Double = 0.0,
)

@Serializable
data class CreateSourceRequestDto(
    val name: String,
    val baseUrl: String,
    val description: String? = null,
    val sourceTier: Int = 5,
    val supportedFormats: List<String> = listOf("HTML"),
    val composerAffinity: Map<String, Double> = emptyMap(),
    val isActive: Boolean = true,
)

@Serializable
data class UpdateSourceRequestDto(
    val name: String? = null,
    val baseUrl: String? = null,
    val description: String? = null,
    val sourceTier: Int? = null,
    val supportedFormats: List<String>? = null,
    val composerAffinity: Map<String, Double>? = null,
    val isActive: Boolean? = null,
)

// --- Extraction Queue ---

@Serializable
data class ExtractionTaskDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val sourceUrl: String,
    val sourceFormat: String,
    val sourceName: String? = null,
    val sourceTier: Int? = null,
    @Serializable(with = UuidSerializer::class)
    val importBatchId: Uuid? = null,
    val status: String,
    val resultCount: Int? = null,
    val confidence: Double? = null,
    val extractionMethod: String? = null,
    val extractorVersion: String? = null,
    val durationMs: Int? = null,
    val attempts: Int = 0,
    val maxAttempts: Int = 3,
    val claimedBy: String? = null,
    val claimedAt: Instant? = null,
    val pageRange: String? = null,
    val lastErrorAt: Instant? = null,
    // TRACK-056: Variant support
    val contentLanguage: String? = null,
    val extractionIntent: String = "PRIMARY",
    @Serializable(with = UuidSerializer::class)
    val relatedExtractionId: Uuid? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class ExtractionDetailDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val sourceUrl: String,
    val sourceFormat: String,
    val sourceName: String? = null,
    val sourceTier: Int? = null,
    @Serializable(with = UuidSerializer::class)
    val importBatchId: Uuid? = null,
    val status: String,
    val resultCount: Int? = null,
    val confidence: Double? = null,
    val extractionMethod: String? = null,
    val extractorVersion: String? = null,
    val durationMs: Int? = null,
    val attempts: Int = 0,
    val maxAttempts: Int = 3,
    val claimedBy: String? = null,
    val claimedAt: Instant? = null,
    val pageRange: String? = null,
    val lastErrorAt: Instant? = null,
    val requestPayload: String? = null,
    val resultPayload: String? = null,
    val errorDetail: String? = null,
    val sourceChecksum: String? = null,
    val cachedArtifactPath: String? = null,
    // TRACK-056: Variant support
    val contentLanguage: String? = null,
    val extractionIntent: String = "PRIMARY",
    @Serializable(with = UuidSerializer::class)
    val relatedExtractionId: Uuid? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class CreateExtractionRequestDto(
    val sourceUrl: String,
    val sourceFormat: String,
    val sourceName: String? = null,
    @Serializable(with = UuidSerializer::class)
    val importSourceId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val importBatchId: Uuid? = null,
    val pageRange: String? = null,
    val composerHint: String? = null,
    val expectedKrithiCount: Int? = null,
    val maxAttempts: Int = 3,
    // TRACK-056: Variant support
    val contentLanguage: String? = null,        // ISO 639-1 (e.g. "en", "sa")
    val extractionIntent: String = "PRIMARY",   // "PRIMARY" or "ENRICH"
    @Serializable(with = UuidSerializer::class)
    val relatedExtractionId: Uuid? = null,      // For ENRICH: the primary extraction
)

@Serializable
data class ExtractionStatsDto(
    val pending: Int = 0,
    val processing: Int = 0,
    val done: Int = 0,
    val failed: Int = 0,
    val cancelled: Int = 0,
    val total: Int = 0,
    val throughputPerHour: Double = 0.0,
)

// --- Pagination ---

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
)
