package com.sangita.grantha.shared.domain.model.catalogue

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Curator-only Mobile Preview contracts (TRACK-158). They wrap, and never change, the V2 search DTOs the mobile
 * app reads. Used only by `/v1/admin/preview/...`.
 */
@Serializable
enum class PreviewScopeDto { PUBLISHED_ONLY, INCLUDE_UNPUBLISHED }

@Serializable
data class PreviewSearchRequest(
    val query: String,
    @Serializable(with = UuidSerializer::class)
    val composerId: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    val ragaId: Uuid? = null,
    val limit: Int = 30,
    /** [PreviewScopeDto.PUBLISHED_ONLY] reproduces what a mobile user gets. */
    val scope: PreviewScopeDto = PreviewScopeDto.PUBLISHED_ONLY,
    val explain: Boolean = true,
)

/** The floor a collection or kshetra card must clear for the active embedding profile. */
@Serializable
data class PreviewFloorDto(val minSimilarity: Double? = null, val minLexical: Double? = null, val minGap: Double)

/** One collection or kshetra candidate the macro query returned, whether or not it became a card. */
@Serializable
data class PreviewMacroCandidateDto(
    val kind: CatalogueSearchHitKindDto,
    val title: String,
    val similarity: Double? = null,
    val gap: Double? = null,
    val lexicalScore: Double? = null,
    val cleared: Boolean,
    /** Why a candidate did not clear: which condition failed. Null when it cleared. */
    val reason: String? = null,
)

@Serializable
data class PreviewSearchExplainDto(
    val mode: String,
    val profileModel: String? = null,
    val profileDimensions: Int? = null,
    /** False when the active profile has no calibrated floor, so no card can show (TRACK-157). */
    val floorCalibrated: Boolean = true,
    val floor: PreviewFloorDto? = null,
    val macroCandidates: List<PreviewMacroCandidateDto> = emptyList(),
)

@Serializable
data class PreviewSearchResponse(
    /** Exactly the V2 response for the same request and scope. */
    val response: CatalogueSearchResponseDto,
    val explain: PreviewSearchExplainDto? = null,
    /** Result krithis that are not published, so a mobile user can never see them. Empty for PUBLISHED_ONLY. */
    val unpublishedKrithiIds: List<@Serializable(with = UuidSerializer::class) Uuid> = emptyList(),
)

@Serializable
enum class PreviewIndexVerdict {
    /** No search documents for this krithi: All matches and Related meanings cannot find its text. */
    NOT_INDEXED,
    /** Source changed after the documents were written, or the published flag disagrees with the workflow state. */
    MAY_BE_STALE,
    /** No source change after the documents were written (does not prove the text is identical). */
    CURRENT,
}

@Serializable
data class PreviewIndexStatus(
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val verdict: PreviewIndexVerdict,
    val workflowState: String,
    val documentCount: Int,
    val publishedFlagMatches: Boolean,
    val embeddedForActiveProfile: Boolean,
    /** ISO-8601 instants, or null when there is nothing to report. */
    val indexedAt: String? = null,
    val sourceUpdatedAt: String? = null,
    val activeProfileModel: String? = null,
    val activeProfileDimensions: Int? = null,
    /** Plain-language reasons, so the curator sees why the verdict is what it is. */
    val reasons: List<String> = emptyList(),
)
