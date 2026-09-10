package com.sangita.grantha.shared.mobile.network

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDiscoveryDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueV2Contract
import com.sangita.grantha.shared.mobile.config.MobileApiConfig
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.MobileSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogueV2ClientTest {
    @Test
    fun searchAndDiscoveryUseV2PathsOnly() = runTest {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            val body = if (request.url.encodedPath.endsWith("/discovery")) {
                catalogueJson.encodeToString(
                    CatalogueDiscoveryDto.serializer(),
                    CatalogueDiscoveryDto(feature = null),
                )
            } else {
                catalogueJson.encodeToString(
                    CataloguePagedResponse.serializer(
                        com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto.serializer(),
                    ),
                    CataloguePagedResponse(listOf(CatalogueFixtures.vatapiSummary), 1, 0, 30),
                )
            }
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))
        val interaction = MobileSession().beginInteraction()
        val page = api.searchKrithis(query = "Vatapi", interaction = interaction)
        val discovery = api.getDiscovery(interaction)
        assertEquals(1, page.total)
        assertNull(discovery.feature)
        assertTrue(paths.all { it.startsWith(CatalogueV2Contract.ROOT) })
        assertTrue(paths.none { it.contains("/v1/catalogue") })
        assertTrue(paths.any { it.endsWith(CatalogueV2Contract.KRITHIS_PATH) || it.endsWith("/v2/catalogue/krithis") })
        assertTrue(paths.any { it.endsWith(CatalogueV2Contract.DISCOVERY_PATH) || it.endsWith("/v2/catalogue/discovery") })
    }

    @Test
    fun cancelledRequestDoesNotDeliverAResult() = runTest {
        val engine = MockEngine {
            delay(10_000)
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))
        val deferred = async {
            api.searchKrithis(query = "Vatapi", interaction = MobileSession().beginInteraction())
        }
        deferred.cancel()
        assertFailsWith<CancellationException> { deferred.await() }
    }

    @Test
    fun fixtureDiscoveryDoesNotPersistCatalogueBodies() = runTest {
        val discovery = CatalogueRepository(FixtureCatalogueApi()).getDiscovery(MobileSession().beginInteraction())
        assertEquals("From the collection", discovery.feature?.heading)
        assertEquals(CatalogueFixtures.vatapiId, discovery.feature?.krithi?.id)
    }
}
