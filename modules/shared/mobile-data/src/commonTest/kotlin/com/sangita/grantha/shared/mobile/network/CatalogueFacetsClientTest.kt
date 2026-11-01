package com.sangita.grantha.shared.mobile.network

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetKeyDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetListDto
import com.sangita.grantha.shared.domain.model.catalogue.RasikaCatalogueContract
import com.sangita.grantha.shared.mobile.config.MobileApiConfig
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.usage.MobileSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogueFacetsClientTest {
    private val interaction get() = MobileSession().beginInteraction()

    @Test
    fun facetsEndpointCarriesScopeSelectionAndCountsOnlyWhenAsked() = runTest {
        val urls = mutableListOf<Url>()
        val engine = MockEngine { request ->
            urls += request.url
            respond(
                catalogueJson.encodeToString(CatalogueFacetListDto.serializer(), CatalogueFixtures.facetsFor(null, false)),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))
        val selection = LakshanaFacetSelection.NONE.with(CatalogueFacetKeyDto.VIBHAKTI, "TRITIYA")

        api.listFacets(composerId = CatalogueFixtures.dikshitarId, facets = selection, interaction = interaction)
        api.listFacets(composerId = CatalogueFixtures.dikshitarId, counts = true, interaction = interaction)

        assertEquals(RasikaCatalogueContract.FACETS_PATH, urls[0].encodedPath)
        assertEquals("TRITIYA", urls[0].parameters["vibhakti"])
        assertNull(urls[0].parameters["counts"])
        assertEquals("true", urls[1].parameters["counts"])
    }

    @Test
    fun krithiSearchSendsFacetParametersByWireKey() = runTest {
        val urls = mutableListOf<Url>()
        val engine = MockEngine { request ->
            urls += request.url
            respond(
                """{"items":[],"total":0,"page":0,"pageSize":20}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))

        api.searchKrithisFaceted(
            facets = LakshanaFacetSelection.NONE.with(CatalogueFacetKeyDto.MANDALAM, "CHOLA"),
            interaction = interaction,
        )

        assertEquals(RasikaCatalogueContract.KRITHIS_PATH, urls.single().encodedPath)
        assertEquals("CHOLA", urls.single().parameters["mandalam"])
    }
}
