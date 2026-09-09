package com.sangita.grantha.shared.mobile.network

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogueApiTest {
    @Test
    fun searchEncodesQueryAndHeaders() = runTest {
        var capturedPath = ""
        var capturedQuery = ""
        var sessionHeader = ""
        val engine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedQuery = request.url.encodedQuery
            sessionHeader = request.headers[CatalogueContract.SESSION_HEADER].orEmpty()
            val body = catalogueJson.encodeToString(
                CataloguePagedResponse.serializer(com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto.serializer()),
                CataloguePagedResponse(listOf(CatalogueFixtures.vatapiSummary), 1, 0, 30),
            )
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, com.sangita.grantha.shared.mobile.config.MobileApiConfig.debugAndroidEmulator()))
        val interaction = MobileSession().beginInteraction()
        val page = api.searchKrithis(query = "Vatapi", interaction = interaction)
        assertEquals(1, page.items.size)
        assertTrue(capturedPath.endsWith("/v2/catalogue/krithis"))
        assertTrue(capturedQuery.contains("query=Vatapi"))
        assertEquals(interaction.sessionId.toString(), sessionHeader)
    }

    @Test
    fun notFoundMapsToCatalogueFailure() = runTest {
        val engine = MockEngine {
            respond(
                """{"code":"NOT_FOUND","message":"This composition is not available."}""",
                HttpStatusCode.NotFound,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, com.sangita.grantha.shared.mobile.config.MobileApiConfig.debugAndroidEmulator()))
        assertFailsWith<CatalogueFailure.NotFound> {
            api.getKrithi(CatalogueFixtures.vatapiId, MobileSession().beginInteraction())
        }
    }

    @Test
    fun laterGenerationIsCallerResponsibilityForStaleResults() = runTest {
        val engine = MockEngine {
            respond(
                catalogueJson.encodeToString(
                    CataloguePagedResponse.serializer(com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto.serializer()),
                    CataloguePagedResponse(emptyList(), 0, 0, 30),
                ),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, com.sangita.grantha.shared.mobile.config.MobileApiConfig.debugAndroidEmulator()))
        val empty = api.searchKrithis(query = "zzzz", interaction = MobileSession().beginInteraction())
        assertEquals(0, empty.total)
    }
}
