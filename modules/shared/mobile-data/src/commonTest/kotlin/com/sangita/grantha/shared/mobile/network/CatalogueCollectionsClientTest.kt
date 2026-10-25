package com.sangita.grantha.shared.mobile.network

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionListDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionRoleDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueOrderBasisDto
import com.sangita.grantha.shared.domain.model.catalogue.RasikaCatalogueContract
import com.sangita.grantha.shared.mobile.config.MobileApiConfig
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.CollectionFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogueCollectionsClientTest {
    private val interaction get() = MobileSession().beginInteraction()

    @Test
    fun collectionsUseV2PathsAndDecodeFixtures() = runTest {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            val body = if (request.url.encodedPath.endsWith("/collections")) {
                catalogueJson.encodeToString(CatalogueCollectionListDto.serializer(), CollectionFixtures.list)
            } else {
                catalogueJson.encodeToString(
                    CatalogueCollectionDetailDto.serializer(),
                    CollectionFixtures.detail(CollectionFixtures.navavaranaSlug)!!,
                )
            }
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = KtorCatalogueApi(createMobileHttpClient(engine, MobileApiConfig.debugAndroidEmulator()))

        val list = api.listCollections(interaction)
        val detail = api.getCollection(CollectionFixtures.navavaranaSlug, interaction)

        assertEquals(3, list.items.size)
        assertEquals(11, detail.members.size)
        assertEquals(9, detail.coreCount)
        assertTrue(paths.all { it.startsWith(RasikaCatalogueContract.COLLECTIONS_PATH) })
    }

    @Test
    fun navavaranaOrdersDhyanaCoreMangalamAndNumbersOnlyCore() = runTest {
        val detail = FixtureCatalogueApi().getCollection(CollectionFixtures.navavaranaSlug, interaction)
        assertEquals(CatalogueCollectionRoleDto.DHYANA, detail.members.first().role)
        assertEquals(CatalogueCollectionRoleDto.MANGALAM, detail.members.last().role)
        assertEquals(CatalogueOrderBasisDto.SOURCE, detail.orderBasis)
        assertEquals((1..9).toList(), detail.members.mapNotNull { it.position })
        assertNull(detail.members.first().axisValue)
    }

    @Test
    fun navagrahaLabelsDisputedMembersAndPancharatnamIsPerformanceOrder() = runTest {
        val api = FixtureCatalogueApi()
        val navagraha = api.getCollection(CollectionFixtures.navagrahaSlug, interaction)
        val disputed = navagraha.members.filter { it.role == CatalogueCollectionRoleDto.DISPUTED_CONJECTURE }
        assertEquals(2, disputed.size)
        assertTrue(disputed.all { it.label == "disputed" && it.position == null })
        assertEquals(7, navagraha.coreCount)

        val pancharatnam = api.getCollection(CollectionFixtures.pancharatnamSlug, interaction)
        assertEquals(CatalogueOrderBasisDto.PERFORMANCE_CONVENTION, pancharatnam.orderBasis)
        assertNotNull(pancharatnam.orderConvention)
    }

    @Test
    fun readerCarriesNeighboursAndV1StyleReaderHasNoCycleContext() = runTest {
        val api = FixtureCatalogueApi()
        val sixth = api.getKrithi(CollectionFixtures.navavaranaSixthId, interaction)
        val ref = sixth.cycleContext!!.collections.single()
        assertEquals(6, ref.position)
        assertEquals(9, ref.coreCount)
        assertNotNull(ref.previous)
        assertNotNull(ref.next)

        // Absent field decodes to null, so a V1 payload without cycleContext stays valid.
        val vatapi = api.getKrithi(CatalogueFixtures.vatapiId, interaction)
        assertNull(vatapi.cycleContext)
        val json = catalogueJson.encodeToString(CatalogueKrithiReaderDto.serializer(), vatapi)
        assertTrue(!json.contains("cycleContext"))
    }

    @Test
    fun clientsWithoutCollectionSupportReportUnavailableNotCrash() = runTest {
        val bare = object : CatalogueApi by FixtureCatalogueApi() {
            override suspend fun listCollections(interaction: com.sangita.grantha.shared.mobile.usage.InteractionContext) =
                throw CatalogueFailure.Unavailable()
        }
        assertFailsWith<CatalogueFailure.Unavailable> { bare.listCollections(interaction) }
    }
}
