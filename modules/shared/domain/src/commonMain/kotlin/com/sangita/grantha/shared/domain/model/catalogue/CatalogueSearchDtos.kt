package com.sangita.grantha.shared.domain.model.catalogue

import com.sangita.grantha.shared.domain.model.SemanticSearchResultItem
import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlin.uuid.Uuid

/**
 * V2 search over cycles and kshetras (TRACK-152). Macro hits live in their own array with their
 * own element type, so a V1 or older client can never decode one as a kriti. The V1
 * `SemanticSearchResultItem` and `SemanticSearchResponse` are deliberately untouched.
 */
@Serializable
enum class CatalogueSearchHitKindDto { KRITHI, COLLECTION, KSHETRA }

@Serializable
data class CatalogueMacroHitDto(
    /** `COLLECTION` or `KSHETRA` only. */
    val kind: CatalogueSearchHitKindDto,
    val title: String,
    /** Composer name for a collection; place or region for a kshetra. */
    val subtitle: String? = null,
    /** Stored overview prefix, at most [MAX_SNIPPET_LENGTH] characters. Never generated at request time. */
    val snippet: String,
    val collectionSlug: String? = null,
    @Serializable(with = UuidSerializer::class)
    val templeId: Uuid? = null,
    val memberCount: Int? = null,
    val publishedKrithiCount: Int? = null,
    val recordedGap: Boolean = false,
    val similarityScore: Double,
    val lexicalScore: Double? = null,
    val rrfScore: Double? = null,
) {
    init {
        require(kind != CatalogueSearchHitKindDto.KRITHI) { "A macro hit is a COLLECTION or a KSHETRA" }
        require((collectionSlug != null) == (kind == CatalogueSearchHitKindDto.COLLECTION)) {
            "collectionSlug is set exactly for COLLECTION hits"
        }
        require((templeId != null) == (kind == CatalogueSearchHitKindDto.KSHETRA)) {
            "templeId is set exactly for KSHETRA hits"
        }
        require(snippet.length <= MAX_SNIPPET_LENGTH) { "snippet exceeds $MAX_SNIPPET_LENGTH characters" }
    }

    companion object {
        const val MAX_SNIPPET_LENGTH = 240
    }
}

@Serializable
data class CatalogueSearchResponseDto(
    val query: String,
    /** Kriti hits plus macro hits. */
    val totalMatches: Int,
    /** At most [MAX_MACRO_HITS]; may be empty. Render above `items`, after [exactTitleMatchId]. */
    @Serializable(with = MacroHitListSerializer::class)
    val macroHits: List<CatalogueMacroHitDto> = emptyList(),
    val items: List<SemanticSearchResultItem>,
    /** Set when the query equals a published kriti's title or incipit; render that kriti first. */
    @Serializable(with = UuidSerializer::class)
    val exactTitleMatchId: Uuid? = null,
) {
    companion object {
        const val MAX_MACRO_HITS = 3
    }
}

/**
 * Decodes `macroHits` leniently: an element whose `kind` this build does not know (a kind added
 * later) is dropped instead of failing the whole response (TRACK-152 Spec 24).
 */
object MacroHitListSerializer : JsonTransformingSerializer<List<CatalogueMacroHitDto>>(
    ListSerializer(CatalogueMacroHitDto.serializer()),
) {
    private val known = setOf(CatalogueSearchHitKindDto.COLLECTION.name, CatalogueSearchHitKindDto.KSHETRA.name)

    override fun transformDeserialize(element: JsonElement): JsonElement =
        JsonArray(
            (element as? JsonArray).orEmpty().filter {
                ((it as? kotlinx.serialization.json.JsonObject)?.get("kind") as? JsonPrimitive)?.content in known
            },
        )
}
