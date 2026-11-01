package com.sangita.grantha.shared.mobile.network

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleListDto
import com.sangita.grantha.shared.domain.model.catalogue.RasikaCatalogueContract
import com.sangita.grantha.shared.mobile.config.MobileApiConfig
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.fixture.KshetraFixtures
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogueKshetraClientTest {
    private val interaction get() = MobileSession().beginInteraction()

    @Test
    fun templesUseV2PathsAndDecodeFixtures() = runTest {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            val body = if (request.url.encodedPath.endsWith("/temples")) {
                catalogueJson.encodeToString(CatalogueTempleListDto.serializer(), KshetraFixtures.list)
            } else {
                catalogueJson.encodeToString(
                    CatalogueTempleDetailDto.serializer(),
                    KshetraFixtures.detail(KshetraFixtures.tiruvarurId)!!,
                )
            }
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))

        val list = api.listTemples(interaction)
        val detail = api.getTemple(KshetraFixtures.tiruvarurId, interaction)

        assertEquals(5, list.items.size)
        assertEquals(3, detail.children.size)
        assertEquals(listOf(RasikaCatalogueContract.TEMPLES_PATH, "${RasikaCatalogueContract.TEMPLES_PATH}/${KshetraFixtures.tiruvarurId}"), paths)
    }

    @Test
    fun anIneligibleTempleIs404AndSurfacesAsNotFound() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NotFound) }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))
        assertFailsWith<CatalogueFailure.NotFound> { api.getTemple(KshetraFixtures.unknownId, interaction) }
    }

    @Test
    fun fixtureListIsRootsOnlyInMandalamOrder() = runTest {
        val items = FixtureCatalogueApi().listTemples(interaction).items
        assertTrue(items.none { it.id == KshetraFixtures.kamalambaShrineId })
        assertNull(items.last().mandalam)
        assertEquals(listOf(KshetraFixtures.chidambaramId, KshetraFixtures.tiruvarurId), items.take(2).map { it.id })
    }

    @Test
    fun fixturesCoverGapAndBothPlacePrecisions() = runTest {
        val api = FixtureCatalogueApi()
        val gap = api.getTemple(KshetraFixtures.kanyakumariId, interaction)
        assertTrue(gap.recordedGap && gap.krithis.isEmpty() && gap.overview != null)
        assertEquals("TEMPLE", api.getTemple(KshetraFixtures.tiruvarurId, interaction).place?.precision?.name)
        assertEquals("PARENT_COMPLEX", api.getTemple(KshetraFixtures.kamalambaShrineId, interaction).place?.precision?.name)
    }
}
