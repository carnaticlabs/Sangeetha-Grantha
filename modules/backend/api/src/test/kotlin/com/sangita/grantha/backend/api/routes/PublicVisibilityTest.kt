package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.config.JwtConfig
import com.sangita.grantha.backend.api.plugins.configureSecurity
import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.api.services.IKrithiService
import com.sangita.grantha.backend.api.services.IReferenceDataService
import com.sangita.grantha.backend.api.services.KrithiNotationService
import com.sangita.grantha.backend.api.services.KrithiServiceImpl
import com.sangita.grantha.backend.api.services.ReferenceDataServiceImpl
import com.sangita.grantha.backend.api.support.Roles
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.uuid.Uuid

class PublicVisibilityTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var krithiService: IKrithiService
    private lateinit var catalogueService: CatalogueService
    private lateinit var referenceDataService: IReferenceDataService
    private lateinit var notationService: KrithiNotationService

    private val env = ApiEnvironment(
        adminToken = "test-admin-token",
        jwtSecret = "test-jwt-secret-for-visibility",
        geminiApiKey = "test",
    )
    private val jwtConfig = JwtConfig.fromEnvironment(env)

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        krithiService = KrithiServiceImpl(dal)
        catalogueService = CatalogueService(dal)
        referenceDataService = ReferenceDataServiceImpl(dal)
        notationService = KrithiNotationService(dal)
    }

    @Test
    fun `anonymous public and catalogue routes hide drafts even with unlock flags`() = testApplication {
        application {
            configureSecurity(env)
            configureStatusPages()
            configureSerialization()
            routing {
                catalogueRoutes(catalogueService)
                publicKrithiRoutes(krithiService, referenceDataService, notationService)
            }
        }
        val seed = TestFixtures.seedReferenceData(dal)
        val draft = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Visibility Draft",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.DRAFT,
        )
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Visibility Published",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )

        val publicSearch = client.get("/v1/krithis/search?query=Visibility&publishedOnly=false")
        assertEquals(HttpStatusCode.OK, publicSearch.status)
        val publicBody = Json.parseToJsonElement(publicSearch.bodyAsText()).jsonObject
        assertEquals("1", publicBody["total"]?.jsonPrimitive?.content)

        val catalogueSearch = client.get("${CatalogueContract.KRITHIS_PATH}?query=Visibility&publishedOnly=false")
        assertEquals(HttpStatusCode.BadRequest, catalogueSearch.status)

        assertEquals(HttpStatusCode.NotFound, client.get("/v1/krithis/${draft.id}").status)
        assertEquals(HttpStatusCode.NotFound, client.get("${CatalogueContract.KRITHIS_PATH}/${draft.id}").status)
    }

    @Test
    fun `non-admin JWT cannot unlock drafts but admin JWT can`() = testApplication {
        application {
            configureSecurity(env)
            configureStatusPages()
            configureSerialization()
            routing {
                publicKrithiRoutes(krithiService, referenceDataService, notationService)
                authenticate("admin-auth") {
                    requireRole(Roles.ADMIN) {
                        adminKrithiRoutes(krithiService, object : com.sangita.grantha.backend.api.services.ITransliterator {
                            override suspend fun transliterate(
                                content: String,
                                sourceScript: String?,
                                targetScript: String,
                            ) = content
                        })
                    }
                }
            }
        }
        val seed = TestFixtures.seedReferenceData(dal)
        val draft = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Admin Visible Draft",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.DRAFT,
        )
        val viewer = jwtConfig.generateToken(Uuid.random(), emptyList())
        val admin = jwtConfig.generateToken(Uuid.random(), listOf(Roles.ADMIN))

        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/v1/krithis/${draft.id}") {
                header(HttpHeaders.Authorization, "Bearer $viewer")
            }.status,
        )
        assertEquals(
            HttpStatusCode.OK,
            client.get("/v1/krithis/${draft.id}") {
                header(HttpHeaders.Authorization, "Bearer $admin")
            }.status,
        )
        assertEquals(
            HttpStatusCode.OK,
            client.get("/v1/admin/krithis/${draft.id}") {
                header(HttpHeaders.Authorization, "Bearer $admin")
            }.status,
        )
        assertFalse(
            Json.parseToJsonElement(
                client.get("/v1/krithis/search?query=Admin Visible") {
                    header(HttpHeaders.Authorization, "Bearer $viewer")
                }.bodyAsText(),
            ).jsonObject["total"]?.jsonPrimitive?.content == "1",
        )
    }
}
