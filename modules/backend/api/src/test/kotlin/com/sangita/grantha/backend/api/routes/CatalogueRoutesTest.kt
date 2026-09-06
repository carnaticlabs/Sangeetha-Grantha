package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
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
import kotlin.uuid.Uuid

class CatalogueRoutesTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var catalogueService: CatalogueService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        catalogueService = CatalogueService(dal)
    }

    @Test
    fun `catalogue search is published-only paged and no-store`() = testApplication {
        application {
            configureStatusPages()
            configureSerialization()
            routing { catalogueRoutes(catalogueService) }
        }
        val seed = TestFixtures.seedReferenceData(dal)
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Route Published",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Route Draft",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.DRAFT,
        )

        val response = client.get("${CatalogueContract.KRITHIS_PATH}?query=Route")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("1", body["total"]?.jsonPrimitive?.content)
        assertEquals("Route Published", body["items"]?.jsonArray?.single()?.jsonObject?.get("title")?.jsonPrimitive?.content)
    }

    @Test
    fun `unknown parameter and malformed uuid are 400 catalogue errors`() = testApplication {
        application {
            configureStatusPages()
            configureSerialization()
            routing { catalogueRoutes(catalogueService) }
        }
        val unknown = client.get("${CatalogueContract.KRITHIS_PATH}?publishedOnly=false")
        assertEquals(HttpStatusCode.BadRequest, unknown.status)
        assertTrue(unknown.bodyAsText().contains("VALIDATION_ERROR"))

        val badId = client.get("${CatalogueContract.KRITHIS_PATH}/not-a-uuid")
        assertEquals(HttpStatusCode.BadRequest, badId.status)
        assertTrue(badId.bodyAsText().contains("VALIDATION_ERROR"))
    }

    @Test
    fun `missing unpublished and wrong-owner share 404 shape`() = testApplication {
        application {
            configureStatusPages()
            configureSerialization()
            routing { catalogueRoutes(catalogueService) }
        }
        val seed = TestFixtures.seedReferenceData(dal)
        val published = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Route Reader",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        val other = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Route Other",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        val draft = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Route Hidden",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.DRAFT,
        )
        val publishedVariant = CatalogueTestFixtures.createVariant(dal, published.id, "a", isPrimary = true)
        val otherVariant = CatalogueTestFixtures.createVariant(dal, other.id, "b", isPrimary = true)

        val missing = client.get("${CatalogueContract.KRITHIS_PATH}/${Uuid.random()}")
        val unpublished = client.get("${CatalogueContract.KRITHIS_PATH}/${draft.id}")
        val wrongOwner = client.get("${CatalogueContract.KRITHIS_PATH}/${published.id}/lyrics/${otherVariant.id}")
        val ok = client.get("${CatalogueContract.KRITHIS_PATH}/${published.id}/lyrics/${publishedVariant.id}")

        assertEquals(HttpStatusCode.NotFound, missing.status)
        assertEquals(HttpStatusCode.NotFound, unpublished.status)
        assertEquals(HttpStatusCode.NotFound, wrongOwner.status)
        assertEquals(HttpStatusCode.OK, ok.status)
        assertTrue(missing.bodyAsText().contains("NOT_FOUND"))
        assertTrue(unpublished.bodyAsText().contains("NOT_FOUND"))
        assertTrue(wrongOwner.bodyAsText().contains("NOT_FOUND"))
        assertEquals("no-store", ok.headers[HttpHeaders.CacheControl])
    }

    @Test
    fun `raga and composer directories expose published counts`() = testApplication {
        application {
            configureStatusPages()
            configureSerialization()
            routing { catalogueRoutes(catalogueService) }
        }
        val seed = TestFixtures.seedReferenceData(dal)
        val raga = dal.ragas.create(name = "CatalogueDirRaga")
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Directory Published",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(raga.id.toJavaUuid()),
            primaryRagaId = raga.id.toJavaUuid(),
        )

        val ragas = client.get("${CatalogueContract.RAGAS_PATH}?query=CatalogueDirRaga")
        assertEquals(HttpStatusCode.OK, ragas.status)
        val ragaPage = Json.parseToJsonElement(ragas.bodyAsText()).jsonObject
        val ragaHit = ragaPage["items"]?.jsonArray?.single()?.jsonObject
        assertEquals(raga.id.toString(), ragaHit?.get("id")?.jsonPrimitive?.content)
        assertEquals("1", ragaHit?.get("publishedCompositionCount")?.jsonPrimitive?.content)

        val composers = client.get("${CatalogueContract.COMPOSERS_PATH}?query=tyagaraja")
        assertEquals(HttpStatusCode.OK, composers.status)
        val composerPage = Json.parseToJsonElement(composers.bodyAsText()).jsonObject
        assertTrue(composerPage["items"]?.jsonArray?.isEmpty() == false)
    }
}
