package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.config.JwtConfig
import com.sangita.grantha.backend.api.plugins.PREVIEW_CATALOGUE_ROOT
import com.sangita.grantha.backend.api.plugins.PREVIEW_MOBILE_ROOT
import com.sangita.grantha.backend.api.plugins.configureSecurity
import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.api.support.Roles
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.AuditLogTable
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.catalogue.RasikaCatalogueContract
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.uuid.Uuid

/** TRACK-158 A1: the admin-gated preview mount of the V2 catalogue reads. */
class PreviewCatalogueRoutesTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var catalogueService: CatalogueService

    private val env = ApiEnvironment(
        adminToken = "test-admin-token",
        jwtSecret = "test-jwt-secret-for-preview",
        geminiApiKey = "test",
    )
    private val jwtConfig = JwtConfig.fromEnvironment(env)

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        catalogueService = CatalogueService(dal)
    }

    private fun ApplicationTestBuilder.mount() = application {
        configureSecurity(env)
        configureStatusPages()
        configureSerialization()
        routing {
            rasikaCatalogueRoutes(catalogueService)
            authenticate("admin-auth") {
                requireRole(Roles.ADMIN) {
                    rasikaCatalogueRoutes(catalogueService.previewing(), PREVIEW_CATALOGUE_ROOT)
                    rasikaCatalogueRoutes(catalogueService, PREVIEW_MOBILE_ROOT)
                }
            }
        }
    }

    private suspend fun HttpClient.preview(path: String, token: String? = null) =
        get("$PREVIEW_CATALOGUE_ROOT$path") { token?.let { header(HttpHeaders.Authorization, "Bearer $it") } }

    private fun adminToken() = jwtConfig.generateToken(Uuid.random(), listOf(Roles.ADMIN))

    @Test
    fun `a draft is 404 on V2 and 200 on preview for an admin`() = testApplication {
        mount()
        val seed = TestFixtures.seedReferenceData(dal)
        val draft = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Preview Draft",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.DRAFT,
        )

        assertEquals(HttpStatusCode.NotFound, client.get("${RasikaCatalogueContract.KRITHIS_PATH}/${draft.id}").status)
        assertEquals(HttpStatusCode.OK, client.preview("/krithis/${draft.id}", adminToken()).status)

        val v2Search = Json.parseToJsonElement(
            client.get("${RasikaCatalogueContract.KRITHIS_PATH}?query=Preview").bodyAsText(),
        ).jsonObject
        val previewSearch = Json.parseToJsonElement(
            client.preview("/krithis?query=Preview", adminToken()).bodyAsText(),
        ).jsonObject
        assertEquals("0", v2Search["total"]?.jsonPrimitive?.content)
        assertEquals("1", previewSearch["total"]?.jsonPrimitive?.content)
    }

    @Test
    fun `archived stays hidden in preview`() = testApplication {
        mount()
        val seed = TestFixtures.seedReferenceData(dal)
        val archived = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Preview Archived",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.ARCHIVED,
        )
        assertEquals(HttpStatusCode.NotFound, client.preview("/krithis/${archived.id}", adminToken()).status)
    }

    @Test
    fun `for a published krithi preview returns exactly what V2 returns`() = testApplication {
        mount()
        val seed = TestFixtures.seedReferenceData(dal)
        val published = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Preview Parity",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        val token = adminToken()
        listOf(
            "/krithis/${published.id}",
            "/krithis?query=Preview+Parity",
            "/ragas",
            "/composers",
            "/discovery",
            "/collections",
        ).forEach { path ->
            val v2 = client.get("${RasikaCatalogueContract.ROOT}$path")
            val preview = client.preview(path, token)
            assertEquals(HttpStatusCode.OK, v2.status, path)
            assertEquals(v2.status, preview.status, path)
            assertEquals(v2.bodyAsText(), preview.bodyAsText(), "body differs for $path")
        }
    }

    @Test
    fun `preview needs an admin role`() = testApplication {
        mount()
        val viewer = jwtConfig.generateToken(Uuid.random(), emptyList())
        assertEquals(HttpStatusCode.Unauthorized, client.preview("/discovery").status)
        assertEquals(HttpStatusCode.Forbidden, client.preview("/discovery", viewer).status)
        assertEquals(HttpStatusCode.OK, client.preview("/discovery", adminToken()).status)
    }

    @Test
    fun `preview reads write no audit rows`() = testApplication {
        mount()
        val seed = TestFixtures.seedReferenceData(dal)
        val draft = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Preview Audit",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.DRAFT,
        )
        val before = auditRows()
        val token = adminToken()
        client.preview("/krithis/${draft.id}", token)
        client.preview("/krithis?query=Preview", token)
        client.preview("/discovery", token)
        assertEquals(before, auditRows())
        assertNotEquals(HttpStatusCode.InternalServerError, client.preview("/ragas", token).status)
    }

    private suspend fun auditRows(): Long = DatabaseFactory.dbQuery { AuditLogTable.selectAll().count() }

    @Test
    fun `the mobile mirror hides drafts exactly like V2 and matches it byte for byte`() = testApplication {
        mount()
        val seed = TestFixtures.seedReferenceData(dal)
        val draft = CatalogueTestFixtures.createKrithi(
            dal, title = "Mirror Draft", composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()), workflowState = WorkflowState.DRAFT,
        )
        CatalogueTestFixtures.createKrithi(
            dal, title = "Mirror Published", composerId = seed.composer.id.toJavaUuid(), ragaIds = listOf(seed.raga.id.toJavaUuid()),
        )
        val token = adminToken()
        suspend fun mirror(path: String) = client.get("$PREVIEW_MOBILE_ROOT$path") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.NotFound, mirror("/krithis/${draft.id}").status)
        assertEquals(HttpStatusCode.OK, client.preview("/krithis/${draft.id}", token).status)
        val v2 = client.get("${RasikaCatalogueContract.KRITHIS_PATH}?query=Mirror").bodyAsText()
        assertEquals(v2, mirror("/krithis?query=Mirror").bodyAsText())
        assertEquals(HttpStatusCode.Unauthorized, client.get("$PREVIEW_MOBILE_ROOT/discovery").status)
    }
}
