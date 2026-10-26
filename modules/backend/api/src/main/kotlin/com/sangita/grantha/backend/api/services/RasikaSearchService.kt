package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.repositories.CollectionSearchFacts
import com.sangita.grantha.backend.dal.repositories.MacroAnchorKind
import com.sangita.grantha.backend.dal.repositories.MacroCandidate
import com.sangita.grantha.backend.dal.repositories.TempleSearchFacts
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.SemanticSearchResultItem
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMacroHitDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchHitKindDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchResponseDto

/**
 * A macro candidate is shown only when it clears the floor: dense similarity at or above
 * [minSimilarity], or (hybrid only) a lexical score at or above [minLexical]. Without a floor
 * every query would return three cards out of ~105 documents (TRACK-152 Spec 13).
 */
data class MacroFloor(val minSimilarity: Double, val minLexical: Double)

enum class RasikaSearchMode { HYBRID, SEMANTIC }

/**
 * V2 search (TRACK-152): the unchanged kriti retrieval plus a separate, capped group of
 * collection and kshetra cards. Kriti items are produced by [HybridSearchService] exactly as for V1
 * (published only), so macro hits can never move kriti ranking.
 */
class RasikaSearchService(
    private val dal: SangitaDal,
    private val search: HybridSearchService,
    private val hybridFloor: MacroFloor = DEFAULT_HYBRID_FLOOR,
    private val semanticFloor: MacroFloor = DEFAULT_SEMANTIC_FLOOR,
) {
    suspend fun search(request: SemanticSearchRequest, mode: RasikaSearchMode): CatalogueSearchResponseDto {
        val query = request.query.trim()
        if (query.isEmpty()) return CatalogueSearchResponseDto(query = query, totalMatches = 0, items = emptyList())

        val prepared = search.prepareQuery(query)
        val items = when (mode) {
            RasikaSearchMode.HYBRID -> search.searchHybridItems(request, prepared, publishedOnly = true)
            RasikaSearchMode.SEMANTIC -> search.searchSemanticItems(request, prepared, publishedOnly = true)
        }
        // composerId / ragaId are kriti-scoped filters (Spec 16).
        val kritiScoped = request.composerId != null || request.ragaId != null
        val macroHits = if (kritiScoped) emptyList() else macroHits(query, mode, prepared)
        return CatalogueSearchResponseDto(
            query = query,
            totalMatches = items.size + macroHits.size,
            macroHits = macroHits,
            items = items,
            exactTitleMatchId = dal.macroSearch.exactTitleMatchId(query)?.toKotlinUuid(),
        )
    }

    private suspend fun macroHits(
        query: String,
        mode: RasikaSearchMode,
        prepared: HybridSearchService.PreparedQuery,
    ): List<CatalogueMacroHitDto> {
        val profile = prepared.profile
        val vector = prepared.vector
        // Semantic mode without an active profile returns nothing for both groups, as V1 does.
        if (mode == RasikaSearchMode.SEMANTIC && (profile == null || vector == null)) return emptyList()

        val collections = dal.catalogue.searchableCollections()
        val temples = dal.catalogue.searchableTemples()
        val candidates = when (mode) {
            RasikaSearchMode.HYBRID -> dal.macroSearch.searchHybrid(
                rawQuery = query,
                queryVector = vector,
                profileId = profile?.id,
                tagIds = collections.keys,
                templeIds = temples.keys,
            )
            RasikaSearchMode.SEMANTIC -> dal.macroSearch.searchSemantic(
                queryVector = vector!!,
                profileId = profile!!.id,
                tagIds = collections.keys,
                templeIds = temples.keys,
            )
        }
        val floor = if (mode == RasikaSearchMode.HYBRID) hybridFloor else semanticFloor
        return candidates
            .filter { clearsFloor(it, floor) }
            .mapNotNull { toHit(it, collections, temples) }
            .take(CatalogueSearchResponseDto.MAX_MACRO_HITS)
    }

    private fun clearsFloor(c: MacroCandidate, floor: MacroFloor): Boolean =
        (c.similarity ?: -1.0) >= floor.minSimilarity || (c.lexicalScore ?: -1.0) >= floor.minLexical

    private fun toHit(
        c: MacroCandidate,
        collections: Map<java.util.UUID, CollectionSearchFacts>,
        temples: Map<java.util.UUID, TempleSearchFacts>,
    ): CatalogueMacroHitDto? = when (c.kind) {
        MacroAnchorKind.COLLECTION -> collections[c.anchorId]?.let { f ->
            CatalogueMacroHitDto(
                kind = CatalogueSearchHitKindDto.COLLECTION,
                title = f.name,
                subtitle = f.composerName,
                snippet = snippetOf(c.content),
                collectionSlug = f.slug,
                memberCount = f.memberCount,
                similarityScore = c.similarity ?: 0.0,
                lexicalScore = c.lexicalScore,
                rrfScore = c.rrfScore,
            )
        }
        MacroAnchorKind.KSHETRA -> temples[c.anchorId]?.let { f ->
            CatalogueMacroHitDto(
                kind = CatalogueSearchHitKindDto.KSHETRA,
                title = f.name,
                subtitle = f.place,
                snippet = snippetOf(c.content),
                templeId = f.templeId.toKotlinUuid(),
                publishedKrithiCount = f.publishedKrithiCount,
                recordedGap = f.recordedGap,
                similarityScore = c.similarity ?: 0.0,
                lexicalScore = c.lexicalScore,
                rrfScore = c.rrfScore,
            )
        }
    }

    companion object {
        /**
         * Calibrated 2026-10-26 on the benchmark probes (Phase 0 scores): the six exact-title kriti probes
         * peak at dense similarity 0.787 (lexical 0.63), the macro hits sit at 0.837 and 0.854. 0.83 keeps a
         * 0.043 margin above the exact-title probes and sits 0.007 below the weakest macro hit; the lexical
         * floor admits only near-verbatim containment (the only macro route when no profile is active).
         * Chosen from the gap, not tuned to the last digit.
         */
        val DEFAULT_HYBRID_FLOOR = MacroFloor(minSimilarity = 0.83, minLexical = 0.8)
        val DEFAULT_SEMANTIC_FLOOR = MacroFloor(minSimilarity = 0.83, minLexical = Double.MAX_VALUE)

        /**
         * A prefix of the stored overview, whitespace collapsed, cut at the last sentence end (or word
         * boundary) within the limit. Never generated text.
         */
        fun snippetOf(content: String): String {
            val text = content.trim().replace(Regex("\\s+"), " ")
            val max = CatalogueMacroHitDto.MAX_SNIPPET_LENGTH
            if (text.length <= max) return text
            val window = text.substring(0, max - 1)
            val sentence = window.lastIndexOfAny(charArrayOf('.', '!', '?'))
            if (sentence >= max / 2) return window.substring(0, sentence + 1)
            val space = window.lastIndexOf(' ')
            return (if (space > 0) window.substring(0, space) else window).trimEnd(',', ';', ':', '-') + "…"
        }
    }
}
