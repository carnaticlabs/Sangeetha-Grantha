package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.plugins.installCatalogueUsage
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.api.services.CatalogueUsageRecorder
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class CatalogueUsageRouteTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var catalogueService: CatalogueService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        catalogueService = CatalogueService(dal)
    }

    @Test
    fun `paged search records resultCount including zero`() = testApplication {
        val lines = mutableListOf<String>()
        val recorder = CatalogueUsageRecorder(environment = "test", sink = { lines += it })
        application {
            configureStatusPages()
            configureSerialization()
            installCatalogueUsage(recorder)
            routing { catalogueRoutes(catalogueService) }
        }
        val seed = TestFixtures.seedReferenceData(dal)
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Usage Count Hit",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )

        assertEquals(
            HttpStatusCode.OK,
            client.get("${CatalogueContract.KRITHIS_PATH}?query=Usage Count Hit").status,
        )
        assertEquals(
            HttpStatusCode.OK,
            client.get("${CatalogueContract.KRITHIS_PATH}?query=zzzz-not-a-composition").status,
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("${CatalogueContract.KRITHIS_PATH}/${Uuid.random()}").status,
        )

        val events = lines.map { Json.parseToJsonElement(it).jsonObject }
        val searches = events.filter { it["action"]?.jsonPrimitive?.content == "search" }
        assertEquals(setOf("1", "0"), searches.map { it["resultCount"]?.jsonPrimitive?.content }.toSet())
        val reader = events.single { it["action"]?.jsonPrimitive?.content == "reader" }
        assertEquals(null, reader["resultCount"])
    }
}
