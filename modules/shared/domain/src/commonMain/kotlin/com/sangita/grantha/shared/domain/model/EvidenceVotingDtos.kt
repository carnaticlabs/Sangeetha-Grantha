package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

// =============================================================================
// TRACK-045: Source Evidence, Structural Voting, Quality & Variant Matching DTOs
// =============================================================================

// --- Source Evidence ---

@Serializable
data class SourceEvidenceSummaryDto(
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val krithiTitle: String,
    val krithiRaga: String? = null,
    val krithiTala: String? = null,
    val krithiComposer: String? = null,
    val sourceCount: Int = 0,
    val topSourceName: String = "",
    val topSourceTier: Int = 5,
    val contributedFields: List<String> = emptyList(),
    val avgConfidence: Double = 0.0,
    val votingStatus: String? = null,
)

@Serializable
data class KrithiEvidenceSourceDto(
    @Serializable(with = UuidSerializer::class)
    val importSourceId: Uuid,
    val sourceName: String,
    val sourceTier: Int,
    val sourceFormat: String,
    val sourceUrl: String,
    val extractionMethod: String? = null,
    val confidence: Double = 0.0,
    val contributedFields: List<String> = emptyList(),
    val fieldValues: Map<String, String> = emptyMap(),
    val extractedAt: Instant,
    val rawExtraction: String? = null,
)

@Serializable
data class KrithiEvidenceResponseDto(
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val krithiTitle: String,
    val krithiRaga: String? = null,
    val krithiTala: String? = null,
    val krithiComposer: String? = null,
    val workflowState: String? = null,
    val sources: List<KrithiEvidenceSourceDto> = emptyList(),
)

// --- Structural Voting ---

@Serializable
data class VotingDecisionDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val krithiTitle: String,
    val votedAt: Instant,
    val sourceCount: Int = 0,
    val consensusType: String,
    val consensusStructure: String, // JSONB
    val confidence: String,
    val dissentCount: Int = 0,
    @Serializable(with = UuidSerializer::class)
    val reviewerId: Uuid? = null,
    val reviewerName: String? = null,
    val notes: String? = null,
)

@Serializable
data class VotingDetailDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val krithiTitle: String,
    val votedAt: Instant,
    val consensusType: String,
    val confidence: String,
    val consensusStructure: String,
    val participatingSources: String, // JSONB
    val dissentingSources: String, // JSONB
    val notes: String? = null,
    @Serializable(with = UuidSerializer::class)
    val reviewerId: Uuid? = null,
    val reviewerName: String? = null,
)

@Serializable
data class ManualOverrideRequestDto(
    val structure: String, // JSON
    val notes: String,
)

@Serializable
data class VotingStatsDto(
    val total: Int = 0,
    val unanimous: Int = 0,
    val majority: Int = 0,
    val authorityOverride: Int = 0,
    val singleSource: Int = 0,
    val manual: Int = 0,
    val confidenceDistribution: ConfidenceDistributionDto = ConfidenceDistributionDto(),
)

@Serializable
data class ConfidenceDistributionDto(
    val high: Int = 0,
    val medium: Int = 0,
    val low: Int = 0,
)

// --- Quality Dashboard ---

@Serializable
data class QualitySummaryDto(
    val totalKrithis: Int = 0,
    val multiSourceCount: Int = 0,
    val multiSourcePercent: Double = 0.0,
    val consensusCount: Int = 0,
    val consensusPercent: Double = 0.0,
    val avgQualityScore: Double = 0.0,
    val enrichmentCoveragePercent: Double = 0.0,
)

// --- TRACK-056: Variant Matching ---

@Serializable
data class VariantMatchDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val extractionId: Uuid,
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val krithiTitle: String = "",
    val confidence: Double,
    val confidenceTier: String,  // HIGH, MEDIUM, LOW
    val matchSignals: String,    // JSONB
    val matchStatus: String,     // PENDING, APPROVED, REJECTED, AUTO_APPROVED
    val isAnomaly: Boolean = false,
    val structureMismatch: Boolean = false,
    val reviewerNotes: String? = null,
    val createdAt: Instant,
)

@Serializable
data class VariantMatchReviewRequestDto(
    val action: String,          // "approve", "reject", "skip"
    val notes: String? = null,
)

@Serializable
data class VariantMatchReportDto(
    @Serializable(with = UuidSerializer::class)
    val extractionId: Uuid,
    val totalMatches: Int = 0,
    val highConfidence: Int = 0,
    val mediumConfidence: Int = 0,
    val lowConfidence: Int = 0,
    val anomalies: Int = 0,
    val autoApproved: Int = 0,
)
