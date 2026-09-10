package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueV2Contract
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogueV2RoutesTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var catalogueService: CatalogueService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        catalogueService = CatalogueService(dal)
    }

    @Test
    fun `v1 excludes unestablished while v2 search and discovery include it`() = testApplication {
        application {
            configureStatusPages()
            configureSerialization()
            routing {
                catalogueRoutes(catalogueService)
                catalogueV2Routes(catalogueService)
            }
        }
        val seed = TestFixtures.seedReferenceData(dal)
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Route Known",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            musicalForm = MusicalForm.KRITHI,
        )
        val unknown = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Route Unknown",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            musicalForm = MusicalForm.UNESTABLISHED,
        )

        val v1 = client.get("${CatalogueContract.KRITHIS_PATH}?query=Route")
        assertEquals(HttpStatusCode.OK, v1.status)
        val v1Body = Json.parseToJsonElement(v1.bodyAsText()).jsonObject
        assertEquals("1", v1Body["total"]?.jsonPrimitive?.content)
        assertEquals(HttpStatusCode.NotFound, client.get("${CatalogueContract.KRITHIS_PATH}/${unknown.id}").status)

        val v2 = client.get("${CatalogueV2Contract.KRITHIS_PATH}?query=Route")
        assertEquals(HttpStatusCode.OK, v2.status)
        assertEquals("no-store", v2.headers[HttpHeaders.CacheControl])
        val v2Body = Json.parseToJsonElement(v2.bodyAsText()).jsonObject
        assertEquals("2", v2Body["total"]?.jsonPrimitive?.content)
        assertTrue(v2Body["items"]?.jsonArray?.any { it.jsonObject["title"]?.jsonPrimitive?.content == "Route Unknown" } == true)

        val discovery = client.get(CatalogueV2Contract.DISCOVERY_PATH)
        assertEquals(HttpStatusCode.OK, discovery.status)
        assertEquals("no-store", discovery.headers[HttpHeaders.CacheControl])
        val feature = Json.parseToJsonElement(discovery.bodyAsText()).jsonObject["feature"]?.jsonObject
        assertEquals("CATALOGUE_ORDER", feature?.get("selection")?.jsonPrimitive?.content)
        assertEquals("From the collection", feature?.get("heading")?.jsonPrimitive?.content)

        val repeated = client.get("${CatalogueV2Contract.KRITHIS_PATH}?query=a&query=b")
        assertEquals(HttpStatusCode.BadRequest, repeated.status)
        assertTrue(repeated.bodyAsText().contains("VALIDATION_ERROR"))
    }
}
