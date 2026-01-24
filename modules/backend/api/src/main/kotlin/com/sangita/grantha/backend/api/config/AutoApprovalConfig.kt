package com.sangita.grantha.backend.api.config

import kotlinx.serialization.Serializable

/**
 * TRACK-012: Configurable auto-approval rules
 *
 * These rules determine when an import should be automatically approved.
 * Rules can be overridden via environment variables.
 */
@Serializable
data class AutoApprovalConfig(
    /**
     * Minimum overall quality score (0.0-1.0) required for auto-approval
     * Default: 0.90 (EXCELLENT tier)
     * Env: AUTO_APPROVAL_MIN_QUALITY_SCORE
     */
    val minQualityScore: Double = 0.90,

    /**
     * Minimum composer resolution confidence (0.0-1.0)
     * Default: 0.95 (HIGH confidence)
     * Env: AUTO_APPROVAL_MIN_COMPOSER_CONFIDENCE
     */
    val minComposerConfidence: Double = 0.95,

    /**
     * Minimum raga resolution confidence (0.0-1.0)
     * Default: 0.90
     * Env: AUTO_APPROVAL_MIN_RAGA_CONFIDENCE
     */
    val minRagaConfidence: Double = 0.90,

    /**
     * Minimum tala resolution confidence (0.0-1.0)
     * Default: 0.85
     * Env: AUTO_APPROVAL_MIN_TALA_CONFIDENCE
     */
    val minTalaConfidence: Double = 0.85,

    /**
     * Require composer to be resolved with high confidence
     * Default: true
     * Env: AUTO_APPROVAL_REQUIRE_COMPOSER
     */
    val requireComposerMatch: Boolean = true,

    /**
     * Require raga to be resolved with high confidence
     * Default: true
     * Env: AUTO_APPROVAL_REQUIRE_RAGA
     */
    val requireRagaMatch: Boolean = true,

    /**
     * Allow auto-approval even if it would create new entities
     * Default: false (only auto-approve if all entities exist)
     * Env: AUTO_APPROVAL_ALLOW_NEW_ENTITIES
     */
    val allowAutoCreateEntities: Boolean = false,

    /**
     * Quality tiers eligible for auto-approval
     * Default: EXCELLENT and GOOD
     * Env: AUTO_APPROVAL_QUALITY_TIERS (comma-separated)
     */
    val qualityTiers: Set<String> = setOf("EXCELLENT", "GOOD"),

    /**
     * Require minimal metadata (title and lyrics) for auto-approval
     * Default: true
     * Env: AUTO_APPROVAL_REQUIRE_METADATA
     */
    val requireMinimalMetadata: Boolean = true
) {
    companion object {
        /**
         * Load configuration from environment variables with defaults
         */
        fun fromEnvironment(): AutoApprovalConfig {
            return AutoApprovalConfig(
                minQualityScore = System.getenv("AUTO_APPROVAL_MIN_QUALITY_SCORE")?.toDoubleOrNull() ?: 0.90,
                minComposerConfidence = System.getenv("AUTO_APPROVAL_MIN_COMPOSER_CONFIDENCE")?.toDoubleOrNull() ?: 0.95,
                minRagaConfidence = System.getenv("AUTO_APPROVAL_MIN_RAGA_CONFIDENCE")?.toDoubleOrNull() ?: 0.90,
                minTalaConfidence = System.getenv("AUTO_APPROVAL_MIN_TALA_CONFIDENCE")?.toDoubleOrNull() ?: 0.85,
                requireComposerMatch = System.getenv("AUTO_APPROVAL_REQUIRE_COMPOSER")?.toBoolean() ?: true,
                requireRagaMatch = System.getenv("AUTO_APPROVAL_REQUIRE_RAGA")?.toBoolean() ?: true,
                allowAutoCreateEntities = System.getenv("AUTO_APPROVAL_ALLOW_NEW_ENTITIES")?.toBoolean() ?: false,
                qualityTiers = System.getenv("AUTO_APPROVAL_QUALITY_TIERS")
                    ?.split(",")
                    ?.map { it.trim().uppercase() }
                    ?.toSet()
                    ?: setOf("EXCELLENT", "GOOD"),
                requireMinimalMetadata = System.getenv("AUTO_APPROVAL_REQUIRE_METADATA")?.toBoolean() ?: true
            )
        }

        /**
         * Conservative configuration for production (stricter rules)
         */
        fun conservative(): AutoApprovalConfig {
            return AutoApprovalConfig(
                minQualityScore = 0.95,
                minComposerConfidence = 0.98,
                minRagaConfidence = 0.95,
                minTalaConfidence = 0.90,
                requireComposerMatch = true,
                requireRagaMatch = true,
                allowAutoCreateEntities = false,
                qualityTiers = setOf("EXCELLENT"),
                requireMinimalMetadata = true
            )
        }

        /**
         * Permissive configuration for development/testing
         */
        fun permissive(): AutoApprovalConfig {
            return AutoApprovalConfig(
                minQualityScore = 0.80,
                minComposerConfidence = 0.85,
                minRagaConfidence = 0.80,
                minTalaConfidence = 0.75,
                requireComposerMatch = true,
                requireRagaMatch = false,
                allowAutoCreateEntities = true,
                qualityTiers = setOf("EXCELLENT", "GOOD", "FAIR"),
                requireMinimalMetadata = false
            )
        }
    }

    /**
     * Validate configuration values are in acceptable ranges
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (minQualityScore < 0.0 || minQualityScore > 1.0) {
            errors.add("minQualityScore must be between 0.0 and 1.0")
        }
        if (minComposerConfidence < 0.0 || minComposerConfidence > 1.0) {
            errors.add("minComposerConfidence must be between 0.0 and 1.0")
        }
        if (minRagaConfidence < 0.0 || minRagaConfidence > 1.0) {
            errors.add("minRagaConfidence must be between 0.0 and 1.0")
        }
        if (minTalaConfidence < 0.0 || minTalaConfidence > 1.0) {
            errors.add("minTalaConfidence must be between 0.0 and 1.0")
        }
        if (qualityTiers.isEmpty()) {
            errors.add("qualityTiers cannot be empty")
        }

        return errors
    }
}
