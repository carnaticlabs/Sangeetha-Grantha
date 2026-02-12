package com.sangita.grantha.backend.dal.tables

import com.sangita.grantha.backend.dal.enums.ExtractionIntent
import com.sangita.grantha.backend.dal.enums.ExtractionStatus
import com.sangita.grantha.backend.dal.support.jsonbText
import com.sangita.grantha.backend.dal.support.pgEnum
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

// =============================================================================
// TRACK-045: Sourcing & Extraction Monitoring — DAL Table Definitions
// =============================================================================

/**
 * Enhanced import_sources columns added by migration 23.
 * These are appended to the existing [ImportSourcesTable] via a companion object
 * referencing the same table name so they can be used in queries.
 */
object ImportSourcesEnhancedTable : Table("import_sources") {
    val id = uuid("id")
    val name = text("name")
    val baseUrl = text("base_url").nullable()
    val description = text("description").nullable()
    val contactInfo = text("contact_info").nullable()
    val sourceTier = integer("source_tier")
    val supportedFormats = text("supported_formats") // TEXT[] stored as text
    val composerAffinity = jsonbText("composer_affinity")
    val lastHarvestedAt = timestampWithTimeZone("last_harvested_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

/**
 * Extraction queue — database-backed work queue for Kotlin ↔ Python extraction.
 * Migration 27.
 */
object ExtractionQueueTable : UUIDTable("extraction_queue") {
    // Relationships
    val importBatchId = javaUUID("import_batch_id").nullable()
    val importTaskRunId = javaUUID("import_task_run_id").nullable()

    // Request
    val sourceUrl = text("source_url")
    val sourceFormat = text("source_format")
    val sourceName = text("source_name").nullable()
    val sourceTier = integer("source_tier").nullable()
    val requestPayload = jsonbText("request_payload")
    val pageRange = text("page_range").nullable()

    // Processing state
    val status = pgEnum<ExtractionStatus>("status", ExtractionStatus.DB_TYPE)
    val claimedAt = timestampWithTimeZone("claimed_at").nullable()
    val claimedBy = text("claimed_by").nullable()
    val attempts = integer("attempts")
    val maxAttempts = integer("max_attempts")

    // Result
    val resultPayload = jsonbText("result_payload").nullable()
    val resultCount = integer("result_count").nullable()
    val extractionMethod = text("extraction_method").nullable()
    val extractorVersion = text("extractor_version").nullable()
    val confidence = decimal("confidence", 5, 4).nullable()
    val durationMs = integer("duration_ms").nullable()

    // Error handling
    val errorDetail = jsonbText("error_detail").nullable()
    val lastErrorAt = timestampWithTimeZone("last_error_at").nullable()

    // Artifact tracking
    val sourceChecksum = text("source_checksum").nullable()
    val cachedArtifactPath = text("cached_artifact_path").nullable()

    // TRACK-056: Variant support
    val contentLanguage = text("content_language").nullable()
    val extractionIntent = pgEnum<ExtractionIntent>("extraction_intent", ExtractionIntent.DB_TYPE)
    val relatedExtractionId = javaUUID("related_extraction_id").nullable()

    // Audit
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

/**
 * Krithi source evidence — links Krithis to contributing sources.
 * Migration 24.
 */
object KrithiSourceEvidenceTable : UUIDTable("krithi_source_evidence") {
    val krithiId = javaUUID("krithi_id")
    val importSourceId = javaUUID("import_source_id")
    val sourceUrl = text("source_url")
    val sourceFormat = text("source_format")
    val extractionMethod = text("extraction_method")
    val pageRange = text("page_range").nullable()
    val extractedAt = timestampWithTimeZone("extracted_at")
    val checksum = text("checksum").nullable()
    val confidence = decimal("confidence", 5, 4).nullable()
    val contributedFields = text("contributed_fields") // TEXT[] stored as text
    val rawExtraction = jsonbText("raw_extraction").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}

/**
 * Structural vote log — audit trail for cross-source voting decisions.
 * Migration 25.
 */
object StructuralVoteLogTable : UUIDTable("structural_vote_log") {
    val krithiId = javaUUID("krithi_id")
    val votedAt = timestampWithTimeZone("voted_at")
    val participatingSources = jsonbText("participating_sources")
    val consensusStructure = jsonbText("consensus_structure")
    val consensusType = text("consensus_type")
    val confidence = text("confidence")
    val dissentingSources = jsonbText("dissenting_sources")
    val reviewerId = javaUUID("reviewer_id").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}

/**
 * TRACK-056: Variant match results — stores matching results between
 * enrichment extractions and existing Krithis.
 * Migration 29.
 */
object VariantMatchTable : UUIDTable("variant_match") {
    val extractionId = javaUUID("extraction_id")
    val krithiId = javaUUID("krithi_id")
    val confidence = decimal("confidence", 5, 4)
    val confidenceTier = text("confidence_tier")
    val matchSignals = jsonbText("match_signals")
    val matchStatus = text("match_status")
    val extractionPayload = jsonbText("extraction_payload").nullable()
    val isAnomaly = bool("is_anomaly")
    val structureMismatch = bool("structure_mismatch")
    val reviewedBy = javaUUID("reviewed_by").nullable()
    val reviewedAt = timestampWithTimeZone("reviewed_at").nullable()
    val reviewerNotes = text("reviewer_notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
