package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.repositories.CollectionSearchFacts
import com.sangita.grantha.backend.dal.repositories.MacroAnchorKind
import com.sangita.grantha.backend.dal.repositories.MacroCandidate
import com.sangita.grantha.backend.dal.models.PreviewScope
import com.sangita.grantha.backend.dal.repositories.TempleSearchFacts
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.SemanticSearchResultItem
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMacroHitDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchHitKindDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchResponseDto

/**
 * A macro candidate is shown only when it clears the floor (TRACK-152 Spec 13, TRACK-157): either dense
 * similarity at or above [minSimilarity] *and* standing out from the pack by at least [minGap] (its
 * similarity minus the 4th-best anchor's), or (hybrid only) a lexical score at or above [minLexical].
 * Without a floor every query would return three cards out of ~105 documents; the gap condition is what
 * keeps a flat score landscape, where nothing really stands out, from producing a card.
 */
data class MacroFloor(val minSimilarity: Double, val minLexical: Double, val minGap: Double = 0.0)

data class MacroFloorPair(val hybrid: MacroFloor, val semantic: MacroFloor)

/** An embedding profile as it is stored: dense scores are only comparable within one of these. */
data class ProfileKey(val modelName: String, val dimensions: Int)

/**
 * Floors per embedding profile. Cosine similarity is specific to a model, width and document text, so a
 * profile with no entry gets no macro cards (and one WARN) rather than another profile's number.
 */
fun interface MacroFloors {
    fun forProfile(key: ProfileKey): MacroFloorPair?
}

class ProfileFloors(private val byProfile: Map<ProfileKey, MacroFloorPair>) : MacroFloors {
    override fun forProfile(key: ProfileKey): MacroFloorPair? = byProfile[key]
}

enum class RasikaSearchMode { HYBRID, SEMANTIC }

/**
 * Collects what the macro retrieval saw, for the curator preview (TRACK-158). Absent on every V2 call, so the
 * mobile path does no extra work and its results cannot depend on it.
 */
class MacroExplainCollector {
    var profile: ProfileKey? = null
    var floor: MacroFloor? = null
    var floorCalibrated: Boolean = true
    val candidates = mutableListOf<ExplainedCandidate>()
}

data class ExplainedCandidate(
    val kind: MacroAnchorKind,
    val title: String,
    val similarity: Double?,
    val gap: Double?,
    val lexicalScore: Double?,
    val cleared: Boolean,
    val reason: String?,
)

/**
 * V2 search (TRACK-152): the unchanged kriti retrieval plus a separate, capped group of
 * collection and kshetra cards. Kriti items are produced by [HybridSearchService] exactly as for V1
 * (published only), so macro hits can never move kriti ranking.
 */
class RasikaSearchService(
    private val dal: SangitaDal,
    private val search: HybridSearchService,
    private val floors: MacroFloors = DEFAULT_FLOORS,
) {
    private val warnedProfiles = java.util.concurrent.ConcurrentHashMap.newKeySet<ProfileKey>()

    /**
     * [scope] and [explain] exist for the admin-gated Mobile Preview (TRACK-158). The defaults are what the V2
     * routes use: published only, nothing collected. `publishedOnly` reaches only the kriti retrieval; macro
     * cards and `exactTitleMatchId` follow their own published rules in both scopes.
     */
    suspend fun search(
        request: SemanticSearchRequest,
        mode: RasikaSearchMode,
        scope: PreviewScope = PreviewScope.PUBLISHED_ONLY,
        explain: MacroExplainCollector? = null,
    ): CatalogueSearchResponseDto {
        val query = request.query.trim()
        if (query.isEmpty()) return CatalogueSearchResponseDto(query = query, totalMatches = 0, items = emptyList())

        val publishedOnly = scope == PreviewScope.PUBLISHED_ONLY
        val prepared = search.prepareQuery(query)
        val items = when (mode) {
            RasikaSearchMode.HYBRID -> search.searchHybridItems(request, prepared, publishedOnly = publishedOnly)
            RasikaSearchMode.SEMANTIC -> search.searchSemanticItems(request, prepared, publishedOnly = publishedOnly)
        }
        // composerId / ragaId are kriti-scoped filters (Spec 16).
        val kritiScoped = request.composerId != null || request.ragaId != null
        val macroHits = if (kritiScoped) emptyList() else macroHits(query, mode, prepared, explain)
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
        explain: MacroExplainCollector?,
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
        val profileKey = profile?.let { ProfileKey(it.modelName, it.dimensions) }
        explain?.profile = profileKey
        val floor = floorFor(mode, profileKey)
        if (floor == null) {
            explain?.floorCalibrated = false
            return emptyList()
        }
        explain?.let { sink ->
            sink.floor = floor
            candidates.forEach { c ->
                sink.candidates += ExplainedCandidate(
                    kind = c.kind,
                    title = titleOf(c, collections, temples),
                    similarity = c.similarity,
                    gap = c.similarityGap,
                    lexicalScore = c.lexicalScore,
                    cleared = Companion.clearsFloor(c, floor),
                    reason = if (Companion.clearsFloor(c, floor)) null else Companion.failedCondition(c, floor),
                )
            }
        }
        return candidates
            .filter { Companion.clearsFloor(it, floor) }
            .mapNotNull { toHit(it, collections, temples) }
            .take(CatalogueSearchResponseDto.MAX_MACRO_HITS)
    }

    /**
     * The floor for the active profile, or null (with one WARN per profile) when it has none. With no active
     * profile only the lexical route exists, and lexical scores do not depend on an embedding space.
     */
    private fun floorFor(mode: RasikaSearchMode, key: ProfileKey?): MacroFloor? {
        if (key == null) return LEXICAL_ONLY_FLOOR
        val pair = floors.forProfile(key)
        if (pair == null) {
            if (warnedProfiles.add(key)) {
                logger.warn(
                    "No macro relevance floor for embedding profile {}/{}; macro cards are off until one is " +
                        "calibrated (TRACK-157)",
                    key.modelName, key.dimensions,
                )
            }
            return null
        }
        return if (mode == RasikaSearchMode.HYBRID) pair.hybrid else pair.semantic
    }

    private fun titleOf(
        c: MacroCandidate,
        collections: Map<java.util.UUID, CollectionSearchFacts>,
        temples: Map<java.util.UUID, TempleSearchFacts>,
    ): String = when (c.kind) {
        MacroAnchorKind.COLLECTION -> collections[c.anchorId]?.name
        MacroAnchorKind.KSHETRA -> temples[c.anchorId]?.name
    } ?: c.anchorId.toString()

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
                snippet = snippetOf(thinOverviewWithFacts(c.content, f)),
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
        private val logger = org.slf4j.LoggerFactory.getLogger(RasikaSearchService::class.java)

        /**
         * Calibrated 2026-11-02 on `evals/macro_scores.json` (53 probes against all 106 macro anchors,
         * `gemini-embedding-2` / 768): the labelled macro hits score 0.836 to 0.852 with a gap to the 4th
         * anchor of 0.044 to 0.050; the six exact-title kriti probes peak at 0.787; four cards that look wrong
         * (`EVAL-08`, `TY-147-04`, `TY-147-10`, `EVAL-29`) score 0.819 to 0.823 with a gap of 0.004 to 0.013.
         * Similarity 0.81 sits 0.026 below the weakest hit and 0.023 above the exact-title peak; gap 0.03
         * sits 0.014 below the weakest hit's and 0.017 above the wrong-looking cards'. `MacroFloorsMarginTest`
         * holds these margins against the recorded file. The lexical floor admits only near-verbatim
         * containment (exact-title probes reach 0.63 at most; the no-profile fallback relies on it).
         */
        val GEMINI_EMBEDDING_2_768 = MacroFloorPair(
            hybrid = MacroFloor(minSimilarity = 0.81, minLexical = 0.8, minGap = 0.03),
            semantic = MacroFloor(minSimilarity = 0.81, minLexical = Double.MAX_VALUE, minGap = 0.03),
        )

        val DEFAULT_FLOORS: MacroFloors = ProfileFloors(
            mapOf(ProfileKey("gemini-embedding-2", 768) to GEMINI_EMBEDDING_2_768),
        )

        /** Hybrid with no active embedding profile: lexical containment only. */
        val LEXICAL_ONLY_FLOOR = MacroFloor(minSimilarity = Double.MAX_VALUE, minLexical = 0.8)

        /** True when [c] clears [floor]; the vector route needs both similarity and a gap. */
        fun clearsFloor(c: MacroCandidate, floor: MacroFloor): Boolean {
            val vectorRoute = (c.similarity ?: -1.0) >= floor.minSimilarity &&
                (c.similarityGap ?: -1.0) >= floor.minGap
            return vectorRoute || (c.lexicalScore ?: -1.0) >= floor.minLexical
        }

        /** Which part of the floor kept [c] out, in words a curator can act on. */
        fun failedCondition(c: MacroCandidate, floor: MacroFloor): String {
            val parts = mutableListOf<String>()
            val sim = c.similarity
            if (floor.minSimilarity == Double.MAX_VALUE) {
                // Lexical-only floor (no active embedding profile): no vector condition to report.
            } else if (sim == null) {
                parts += "no vector score"
            } else {
                if (sim < floor.minSimilarity) parts += "similarity %.3f is below %.3f".format(sim, floor.minSimilarity)
                val gap = c.similarityGap
                if (gap != null && gap < floor.minGap) parts += "gap %.3f is below %.3f".format(gap, floor.minGap)
            }
            if (floor.minLexical != Double.MAX_VALUE) {
                parts += "lexical %.3f is below %.3f".format(c.lexicalScore ?: 0.0, floor.minLexical)
            }
            return parts.joinToString("; ")
        }

        /** A stored overview shorter than this mostly repeats the temple's name and place. */
        const val THIN_OVERVIEW_LENGTH = 60

        /**
         * Enriches a one-line overview with facts already recorded for the temple (region, sacred water
         * and tree, deity posture, published kritis and their composers); nothing is invented, and a
         * longer overview is left alone. Display only: the stored text and its embedding are unchanged.
         */
        fun thinOverviewWithFacts(overview: String, f: TempleSearchFacts): String {
            val base = overview.trim()
            if (base.length >= THIN_OVERVIEW_LENGTH) return overview
            val parts = buildList {
                f.mandalam?.let { add("In the ${it.lowercase().replaceFirstChar(Char::uppercase)} region.") }
                val details = listOfNotNull(
                    f.sthalaTirtha?.let { "sacred water: $it" },
                    f.sthalaVriksha?.let { "sacred tree: $it" },
                    f.deityPosture?.let(::postureWords)?.let { "$it deity" },
                )
                if (details.isNotEmpty()) add(details.joinToString("; ").replaceFirstChar(Char::uppercase) + ".")
                if (f.publishedKrithiCount > 0) {
                    val noun = if (f.publishedKrithiCount == 1) "kriti" else "kritis"
                    add("${f.publishedKrithiCount} $noun recorded here" + composersPhrase(f.composerNames) + ".")
                }
            }
            if (parts.isEmpty()) return overview
            val sentence = if (base.last() in ".!?") base else "$base."
            return "$sentence ${parts.joinToString(" ")}"
        }

        private fun postureWords(name: String): String? = when (name) {
            "SAYANA" -> "reclining"
            "STHANAKA" -> "standing"
            "ASINA" -> "seated"
            "TANDAVA" -> "dancing"
            else -> null
        }

        private fun composersPhrase(names: List<String>): String = when (names.size) {
            0 -> ""
            1 -> ", by ${names[0]}"
            else -> ", by ${names.dropLast(1).joinToString(", ")} and ${names.last()}"
        }

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
