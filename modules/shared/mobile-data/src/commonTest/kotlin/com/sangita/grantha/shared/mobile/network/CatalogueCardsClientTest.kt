package com.sangita.grantha.shared.mobile.network

import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMacroHitDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchHitKindDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchResponseDto
import com.sangita.grantha.shared.mobile.config.MobileApiConfig
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.usage.MobileSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** TRACK-152: the V2 search calls hit the V2 paths and decode the macro group. */
class CatalogueCardsClientTest {
    private fun response() = CatalogueSearchResponseDto(
        query = "sixth enclosure",
        totalMatches = 2,
        macroHits = listOf(
            CatalogueMacroHitDto(
                kind = CatalogueSearchHitKindDto.COLLECTION,
                title = "Kamalamba Navavarana",
                snippet = "Nine enclosures",
                collectionSlug = "kamalamba-navavarnam",
                similarityScore = 0.86,
            ),
        ),
        items = listOf(CatalogueFixtures.discoveryItem(CatalogueFixtures.vatapiSummary, hybrid = true)),
    )

    private fun api(engine: MockEngine) =
        KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))

    private fun engine() = MockEngine {
        respond(
            catalogueJson.encodeToString(CatalogueSearchResponseDto.serializer(), response()),
            HttpStatusCode.OK,
            headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    @Test
    fun hybridCardsUsesTheV2PathAndDecodesMacroHits() = runTest {
        val engine = engine()
        val decoded = api(engine).searchHybridWithCards(
            SemanticSearchRequest(query = "sixth enclosure", limit = 30),
            MobileSession().beginInteraction(),
        )
        assertTrue(engine.requestHistory.single().url.encodedPath.endsWith("/v2/catalogue/search/hybrid"))
        assertEquals("kamalamba-navavarnam", decoded.macroHits.single().collectionSlug)
        assertEquals(1, decoded.items.size)
    }

    @Test
    fun semanticCardsUsesTheV2SemanticPath() = runTest {
        val engine = engine()
        api(engine).searchSemanticWithCards(
            SemanticSearchRequest(query = "sixth enclosure", limit = 30),
            MobileSession().beginInteraction(),
        )
        assertTrue(engine.requestHistory.single().url.encodedPath.endsWith("/v2/catalogue/search/semantic"))
    }
}
