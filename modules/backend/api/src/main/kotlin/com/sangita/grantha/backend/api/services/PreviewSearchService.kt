package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.models.PreviewScope
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchHitKindDto
import com.sangita.grantha.shared.domain.model.catalogue.PreviewFloorDto
import com.sangita.grantha.shared.domain.model.catalogue.PreviewIndexStatus
import com.sangita.grantha.shared.domain.model.catalogue.PreviewIndexVerdict
import com.sangita.grantha.shared.domain.model.catalogue.PreviewMacroCandidateDto
import com.sangita.grantha.shared.domain.model.catalogue.PreviewScopeDto
import com.sangita.grantha.shared.domain.model.catalogue.PreviewSearchExplainDto
import com.sangita.grantha.shared.domain.model.catalogue.PreviewSearchRequest
import com.sangita.grantha.shared.domain.model.catalogue.PreviewSearchResponse
import com.sangita.grantha.backend.dal.repositories.MacroAnchorKind
import kotlin.uuid.Uuid

/**
 * Read-only Mobile Preview search (TRACK-158). The search itself is [RasikaSearchService], the same object the
 * V2 routes call; this class only chooses the scope, collects the explanation and answers index-status. It
 * writes nothing.
 */
class PreviewSearchService(
    private val dal: SangitaDal,
    private val rasika: RasikaSearchService,
) {
    suspend fun search(request: PreviewSearchRequest, mode: RasikaSearchMode): PreviewSearchResponse {
        val scope = when (request.scope) {
            PreviewScopeDto.PUBLISHED_ONLY -> PreviewScope.PUBLISHED_ONLY
            PreviewScopeDto.INCLUDE_UNPUBLISHED -> PreviewScope.INCLUDE_UNPUBLISHED
        }
        val collector = if (request.explain) MacroExplainCollector() else null
        val response = rasika.search(
            SemanticSearchRequest(request.query, request.composerId, request.ragaId, request.limit),
            mode,
            scope,
            collector,
        )
        val unpublished = if (scope == PreviewScope.PUBLISHED_ONLY) emptyList() else {
            dal.previewIndex.unpublishedAmong(response.items.map { it.krithiId.toJavaUuid() }).map { it.toKotlinUuid() }
        }
        return PreviewSearchResponse(response = response, explain = collector?.let { explainOf(it, mode) }, unpublishedKrithiIds = unpublished)
    }

    private fun explainOf(c: MacroExplainCollector, mode: RasikaSearchMode) = PreviewSearchExplainDto(
        mode = mode.name,
        profileModel = c.profile?.modelName,
        profileDimensions = c.profile?.dimensions,
        floorCalibrated = c.floorCalibrated,
        floor = c.floor?.let {
            PreviewFloorDto(
                minSimilarity = it.minSimilarity.takeIf { v -> v != Double.MAX_VALUE },
                minLexical = it.minLexical.takeIf { v -> v != Double.MAX_VALUE },
                minGap = it.minGap,
            )
        },
        macroCandidates = c.candidates.map {
            PreviewMacroCandidateDto(
                kind = if (it.kind == MacroAnchorKind.COLLECTION) CatalogueSearchHitKindDto.COLLECTION else CatalogueSearchHitKindDto.KSHETRA,
                title = it.title,
                similarity = it.similarity,
                gap = it.gap,
                lexicalScore = it.lexicalScore,
                cleared = it.cleared,
                reason = it.reason,
            )
        },
    )

    /**
     * Whether All matches / Related meanings can be trusted for this krithi. Timestamps only: the content hash
     * lives in the Python worker, so a "current" verdict means no source change after the documents were
     * written, not that the text is identical.
     */
    suspend fun indexStatus(krithiId: Uuid): PreviewIndexStatus? {
        val profile = dal.krithiSearch.activeEmbeddingProfile()
        val facts = dal.previewIndex.facts(krithiId.toJavaUuid(), profile?.id) ?: return null
        val reasons = mutableListOf<String>()
        val published = facts.workflowState == "published"
        val flagMatches = if (published) facts.publishedDocumentCount == facts.documentCount else facts.publishedDocumentCount == 0
        val embedded = profile != null && facts.documentCount > 0 && facts.embeddedDocumentCount >= facts.documentCount
        val indexedAt = facts.indexedAt
        val sourceUpdatedAt = facts.sourceUpdatedAt
        val changedAfterIndex = indexedAt != null && sourceUpdatedAt != null && sourceUpdatedAt.isAfter(indexedAt)

        val verdict = when {
            facts.documentCount == 0 -> {
                reasons += "No search documents exist for this krithi."
                PreviewIndexVerdict.NOT_INDEXED
            }
            else -> {
                if (changedAfterIndex) reasons += "The krithi, its sections or its lyrics changed after its search documents were written."
                if (!flagMatches) reasons += "The documents' published flag disagrees with the workflow state (${facts.workflowState})."
                if (profile != null && !embedded) reasons += "Some documents have no embedding for the active profile."
                if (profile == null) reasons += "No embedding profile is active."
                if (reasons.isEmpty()) PreviewIndexVerdict.CURRENT else PreviewIndexVerdict.MAY_BE_STALE
            }
        }
        return PreviewIndexStatus(
            krithiId = krithiId,
            verdict = verdict,
            workflowState = facts.workflowState,
            documentCount = facts.documentCount,
            publishedFlagMatches = flagMatches,
            embeddedForActiveProfile = embedded,
            indexedAt = facts.indexedAt?.toInstant()?.toString(),
            sourceUpdatedAt = facts.sourceUpdatedAt?.toInstant()?.toString(),
            activeProfileModel = profile?.modelName,
            activeProfileDimensions = profile?.dimensions,
            reasons = reasons,
        )
    }
}
