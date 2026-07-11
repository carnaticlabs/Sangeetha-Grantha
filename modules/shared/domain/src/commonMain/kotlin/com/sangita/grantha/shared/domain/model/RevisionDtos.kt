package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Versioned canon & provenance DTOs (ADR-014 / TRACK-117).
 *
 * A [KrithiRevisionDto] is the append-only envelope for one accepted
 * change-set; [KrithiSectionRevisionDto] rows carry the re-materializable
 * per-section content, each independently attributable to an extraction run
 * and physical source document.
 */

@Serializable
data class KrithiRevisionDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val revisionNo: Int,
    val changeKind: String,
    val changeReason: String? = null,
    @Serializable(with = UuidSerializer::class)
    val extractionId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val createdByUserId: Uuid? = null,
    val validFrom: Instant,
    val recordedAt: Instant,
    val sections: List<KrithiSectionRevisionDto> = emptyList(),
)

@Serializable
data class KrithiSectionRevisionDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val revisionId: Uuid,
    val sectionType: String,
    val orderIndex: Int,
    val label: String? = null,
    val language: String? = null,
    val script: String? = null,
    val text: String,
    val normalizedText: String? = null,
    @Serializable(with = UuidSerializer::class)
    val extractionId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val sourceDocumentId: Uuid? = null,
    val validFrom: Instant,
)

/** One row of the N5 answer: a section plus the full source lineage that produced it. */
@Serializable
data class SectionProvenanceDto(
    val sectionType: String,
    val orderIndex: Int,
    val text: String,
    val revisionNo: Int,
    val changeKind: String,
    val extractorVersion: String? = null,
    val extractionConfidence: Double? = null,
    val sourceUrl: String? = null,
    val sourceChecksum: String? = null,
    val sourceRegistryName: String? = null,
    val createdByUserId: String? = null,
)
