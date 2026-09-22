package com.sangita.grantha.shared.mobile.network

import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.SemanticSearchResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.mobile.config.MobileApiConfig
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.usage.MobileSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiscoveryCatalogueClientTest {
    @Test
    fun hybridPostSendsLimitAndNoAuthorization() = runTest {
        var rawBody = ""
        val engine = MockEngine { request ->
            rawBody = request.body.toByteArray().decodeToString()
            val body = catalogueJson.encodeToString(
                SemanticSearchResponse.serializer(),
                SemanticSearchResponse(
                    query = "Vatapi",
                    totalMatches = 1,
                    items = listOf(
                        CatalogueFixtures.discoveryItem(CatalogueFixtures.vatapiSummary, hybrid = true),
                    ),
                ),
            )
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))
        val interaction = MobileSession().beginInteraction()
        val response = api.searchHybrid(
            SemanticSearchRequest(query = "Vatapi", composerId = null, ragaId = null, limit = 30),
            interaction,
        )
        val request = engine.requestHistory.single()
        assertEquals(1, response.items.size)
        assertEquals(CatalogueFixtures.VATAPI_RRF, response.items.single().rrfScore)
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.encodedPath.endsWith("/v1/search/hybrid"))
        assertEquals(interaction.sessionId.toString(), request.headers[CatalogueContract.SESSION_HEADER])
        assertEquals(interaction.interactionId.toString(), request.headers[CatalogueContract.INTERACTION_HEADER])
        assertNull(request.headers[HttpHeaders.Authorization])
        val decoded = catalogueJson.decodeFromString(SemanticSearchRequest.serializer(), rawBody)
        assertEquals("Vatapi", decoded.query)
        assertNull(decoded.composerId)
        assertNull(decoded.ragaId)
        assertEquals(30, decoded.limit)
        assertTrue(rawBody.contains("\"limit\":30"))
        assertFalse(rawBody.contains("composerId"))
        assertFalse(rawBody.contains("ragaId"))
    }

    @Test
    fun semanticPostUsesTheSemanticPath() = runTest {
        val engine = MockEngine { request ->
            val body = catalogueJson.encodeToString(
                SemanticSearchResponse.serializer(),
                SemanticSearchResponse(
                    query = "Vatapi",
                    totalMatches = 1,
                    items = listOf(
                        CatalogueFixtures.discoveryItem(CatalogueFixtures.vatapiSummary, hybrid = false),
                    ),
                ),
            )
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))
        val response = api.searchSemantic(
            SemanticSearchRequest(query = "Vatapi", limit = 30),
            MobileSession().beginInteraction(),
        )
        assertTrue(engine.requestHistory.single().url.encodedPath.endsWith("/v1/search/semantic"))
        assertNull(engine.requestHistory.single().headers[HttpHeaders.Authorization])
        assertNull(response.items.single().rrfScore)
        assertNull(response.items.single().lexicalScore)
        assertEquals(CatalogueFixtures.VATAPI_SIMILARITY, response.items.single().similarityScore)
    }

    @Test
    fun serviceUnavailableReadsMessageAndDoesNotReturnAnEmptyList() = runTest {
        val engine = MockEngine {
            respond(
                """{"message":"No compatible embedding profile."}""",
                HttpStatusCode.ServiceUnavailable,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))
        val failure = assertFailsWith<CatalogueFailure.Unavailable> {
            api.searchSemantic(
                SemanticSearchRequest(query = "Vatapi", limit = 30),
                MobileSession().beginInteraction(),
            )
        }
        assertEquals("No compatible embedding profile.", failure.serverMessage)
        assertEquals("No compatible embedding profile.", failure.message)
    }
}
