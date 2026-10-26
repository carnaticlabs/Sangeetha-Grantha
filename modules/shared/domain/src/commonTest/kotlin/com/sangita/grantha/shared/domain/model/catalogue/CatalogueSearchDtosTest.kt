package com.sangita.grantha.shared.domain.model.catalogue

import com.sangita.grantha.shared.domain.model.SemanticSearchResponse
import com.sangita.grantha.shared.domain.model.SemanticSearchResultItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class CatalogueSearchDtosTest {
    private val server = Json { encodeDefaults = true; explicitNulls = false }

    /** The frozen V1 client decoder: strict, no unknown keys. */
    private val frozenV1 = Json { ignoreUnknownKeys = false }

    private val kritiId = Uuid.parse("01930000-0000-7000-8000-000000000010")
    private val templeId = Uuid.parse("01930000-0000-7000-8000-000000000020")

    /** Recorded V1 `/v1/search/hybrid` body (field set at the time TRACK-152 was written). */
    private val recordedV1 = """
        {"query":"vatapi","totalMatches":1,"items":[{"krithiId":"01930000-0000-7000-8000-000000000010",
        "title":"Vatapi Ganapatim","composerName":"Muthuswami Dikshitar","ragaName":"Hamsadhvani","talaName":"Adi",
        "documentKind":"COMPOSITION_OVERVIEW","matchedContent":"text","similarityScore":0.9,"lexicalScore":0.5,"rrfScore":0.03}]}
    """.trimIndent()

    private fun kriti() = SemanticSearchResultItem(
        krithiId = kritiId, title = "Vatapi Ganapatim", composerName = "Muthuswami Dikshitar",
        documentKind = "COMPOSITION_OVERVIEW", matchedContent = "text", similarityScore = 0.9,
    )

    private fun collection() = CatalogueMacroHitDto(
        kind = CatalogueSearchHitKindDto.COLLECTION, title = "Navavarana", snippet = "Nine enclosures",
        collectionSlug = "navavarana", memberCount = 11, similarityScore = 0.8,
    )

    private fun kshetra() = CatalogueMacroHitDto(
        kind = CatalogueSearchHitKindDto.KSHETRA, title = "Kanyakumari", snippet = "A shrine",
        templeId = templeId, publishedKrithiCount = 0, recordedGap = true, similarityScore = 0.85,
    )

    @Test
    fun frozenV1DecoderReadsTheRecordedV1Body() {
        val decoded = frozenV1.decodeFromString<SemanticSearchResponse>(recordedV1)
        assertEquals(kritiId, decoded.items.single().krithiId)
    }

    @Test
    fun itemsOfAV2ResponseDecodeWithTheStrictV1Decoder() {
        val v2 = CatalogueSearchResponseDto(
            query = "q", totalMatches = 3, macroHits = listOf(collection(), kshetra()),
            items = listOf(kriti()), exactTitleMatchId = kritiId,
        )
        val obj = server.parseToJsonElement(server.encodeToString(CatalogueSearchResponseDto.serializer(), v2)).jsonObject
        val items = obj.getValue("items").jsonArray.map {
            frozenV1.decodeFromString(SemanticSearchResultItem.serializer(), it.toString())
        }
        assertEquals(listOf(kriti()), items)
    }

    @Test
    fun macroHitsAreNeverDecodableAsKritiItems() {
        val text = server.encodeToString(CatalogueMacroHitDto.serializer(), collection())
        assertFailsWith<Exception> { frozenV1.decodeFromString(SemanticSearchResultItem.serializer(), text) }
    }

    @Test
    fun kindsSerialiseAsNamesAndNullsAreOmitted() {
        val obj = server.parseToJsonElement(server.encodeToString(CatalogueMacroHitDto.serializer(), kshetra())).jsonObject
        assertEquals("KSHETRA", obj.getValue("kind").jsonPrimitive.content)
        assertFalse("collectionSlug" in obj)
        assertTrue("templeId" in obj)
        assertEquals("true", obj.getValue("recordedGap").jsonPrimitive.content)
    }

    @Test
    fun exactlyOneAnchorIsEnforced() {
        assertFailsWith<IllegalArgumentException> { collection().copy(templeId = templeId) }
        assertFailsWith<IllegalArgumentException> { collection().copy(collectionSlug = null) }
        assertFailsWith<IllegalArgumentException> { kshetra().copy(collectionSlug = "x") }
        assertFailsWith<IllegalArgumentException> { kshetra().copy(templeId = null) }
        assertFailsWith<IllegalArgumentException> {
            collection().copy(kind = CatalogueSearchHitKindDto.KRITHI, collectionSlug = null)
        }
    }

    @Test
    fun snippetIsBounded() {
        assertFailsWith<IllegalArgumentException> {
            collection().copy(snippet = "x".repeat(CatalogueMacroHitDto.MAX_SNIPPET_LENGTH + 1))
        }
    }

    @Test
    fun emptyMacroGroupDefaultsAndRoundTrips() {
        val v2 = CatalogueSearchResponseDto(query = "q", totalMatches = 1, items = listOf(kriti()))
        val text = server.encodeToString(CatalogueSearchResponseDto.serializer(), v2)
        assertTrue(server.parseToJsonElement(text).jsonObject.getValue("macroHits").jsonArray.isEmpty())
        assertEquals(v2, server.decodeFromString(CatalogueSearchResponseDto.serializer(), text))
    }
}
