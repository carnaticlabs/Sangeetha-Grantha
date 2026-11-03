package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.clients.GeminiEmbeddingClient
import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.config.JwtConfig
import com.sangita.grantha.backend.api.plugins.configureRequestValidation
import com.sangita.grantha.backend.api.plugins.configureSecurity
import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.plugins.installCatalogueUsage
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.api.services.CatalogueUsageRecorder
import com.sangita.grantha.backend.api.services.HybridSearchService
import com.sangita.grantha.backend.api.services.MacroFloor
import com.sangita.grantha.backend.api.services.MacroFloorPair
import com.sangita.grantha.backend.api.services.MacroFloors
import com.sangita.grantha.backend.api.services.PreviewSearchService
import com.sangita.grantha.backend.api.services.RasikaSearchService
import com.sangita.grantha.backend.api.support.Roles
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.CycleMemberRole
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.AuditLogTable
import com.sangita.grantha.backend.dal.tables.KrithiCycleMembershipsTable
import com.sangita.grantha.backend.dal.tables.KrithiTagsTable
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchResponseDto
import com.sangita.grantha.shared.domain.model.catalogue.PreviewIndexStatus
import com.sangita.grantha.shared.domain.model.catalogue.PreviewIndexVerdict
import com.sangita.grantha.shared.domain.model.catalogue.PreviewSearchResponse
import com.sangita.grantha.shared.domain.model.catalogue.RasikaCatalogueContract
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** TRACK-158 B1: the admin-gated preview search, its explanation, rate limit, usage-log silence and index status. */
class PreviewSearchRoutesTest : IntegrationTestBase() {
    private companion object {
        val FLOORS = MacroFloors {
            MacroFloorPair(
                hybrid = MacroFloor(minSimilarity = 0.83, minLexical = 0.8),
                semantic = MacroFloor(minSimilarity = 0.83, minLexical = Double.MAX_VALUE),
            )
        }
    }

    private lateinit var dal: SangitaDal
    private lateinit var catalogue: CatalogueService
    private lateinit var rasika: RasikaSearchService
    private lateinit var preview: PreviewSearchService
    private val usageEvents = mutableListOf<String>()

    private val env = ApiEnvironment(adminToken = "test-admin-token", jwtSecret = "test-jwt-secret-for-preview-search", geminiApiKey = "test")
    private val jwtConfig = JwtConfig.fromEnvironment(env)
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        catalogue = CatalogueService(dal)
        val hybrid = HybridSearchService(dal, GeminiEmbeddingClient(apiKey = "", allowSyntheticFallback = true))
        rasika = RasikaSearchService(dal, hybrid, floors = FLOORS)
        preview = PreviewSearchService(dal, rasika)
        usageEvents.clear()
    }

    private fun ApplicationTestBuilder.mount() = application {
        configureSecurity(env)
        configureStatusPages()
        configureSerialization()
        // The preview search routes name a rate limit that this plugin registers.
        configureRequestValidation()
        installCatalogueUsage(CatalogueUsageRecorder(sink = { usageEvents += it }))
        routing {
            rasikaSearchRoutes(rasika)
            rasikaCatalogueRoutes(catalogue)
            authenticate("admin-auth") {
                requireRole(Roles.ADMIN) {
                    rasikaCatalogueRoutes(catalogue.previewing(), "/v1/admin/preview/catalogue")
                    previewSearchRoutes(preview)
                }
            }
        }
    }

    private fun admin(): String = jwtConfig.generateToken(Uuid.random(), listOf(Roles.ADMIN))

    private suspend fun HttpClient.previewSearch(
        query: String, mode: String = "hybrid", scope: String = "PUBLISHED_ONLY", token: String = admin(),
    ): HttpResponse = post("/v1/admin/preview/search/$mode") {
        header(HttpHeaders.Authorization, "Bearer $token")
        contentType(ContentType.Application.Json)
        setBody("""{"query":"$query","limit":10,"scope":"$scope","explain":true}""")
    }

    private suspend fun HttpClient.v2(query: String, mode: String = "hybrid"): HttpResponse =
        post(if (mode == "hybrid") RasikaCatalogueContract.SEARCH_HYBRID_PATH else RasikaCatalogueContract.SEARCH_SEMANTIC_PATH) {
            contentType(ContentType.Application.Json)
            setBody("""{"query":"$query","limit":10}""")
        }

    // --- fixtures (same shapes as RasikaSearchRoutesTest) ------------------------------------------

    private suspend fun activeProfile(): UUID = DatabaseFactory.dbQuery {
        var id: UUID? = null
        exec(
            "INSERT INTO embedding_profiles (model_name, dimensions, task_type, is_active) " +
                "VALUES ('gemini-embedding-2', 768, 'RETRIEVAL_DOCUMENT', true) RETURNING id",
            explicitStatementType = StatementType.SELECT,
        ) { rs -> if (rs.next()) id = UUID.fromString(rs.getString("id")) }
        assertNotNull(id)
    }

    private fun vector(axis: Int) = List(768) { if (it == axis) "1" else "0" }.joinToString(",", "[", "]")

    private suspend fun doc(
        profile: UUID?, kind: String, text: String, krithiId: UUID? = null, tagId: UUID? = null,
        published: Boolean = true, axis: Int = 0,
    ) {
        DatabaseFactory.dbQuery {
            var docId: UUID? = null
            val hash = "h-${UUID.randomUUID()}"
            exec(
                "INSERT INTO search_documents (krithi_id, tag_id, document_kind, original_content, indexed_content, content_hash, is_published) " +
                    "VALUES (${krithiId?.let { "'$it'" } ?: "NULL"}, ${tagId?.let { "'$it'" } ?: "NULL"}, '$kind', '$text', '$text', '$hash', $published) RETURNING id",
                explicitStatementType = StatementType.SELECT,
            ) { rs -> if (rs.next()) docId = UUID.fromString(rs.getString("id")) }
            if (profile != null) {
                exec("INSERT INTO document_embeddings (document_id, profile_id, embedding, content_hash) VALUES ('${assertNotNull(docId)}', '$profile', '${vector(axis)}'::vector(768), '$hash')")
            }
        }
    }

    private suspend fun reviewedCollection(slug: String): UUID {
        val seed = TestFixtures.seedReferenceData(dal)
        val member = CatalogueTestFixtures.createKrithi(
            dal, title = "Member of $slug", composerId = seed.composer.id.toJavaUuid(), ragaIds = listOf(seed.raga.id.toJavaUuid()),
        ).id.toJavaUuid()
        return DatabaseFactory.dbQuery {
            val tagId = TagsTable.insert {
                it[category] = "STOTRA_STYLE"; it[TagsTable.slug] = slug; it[displayNameEn] = "Cycle $slug"
            }[TagsTable.id].value
            KrithiCycleMembershipsTable.insert {
                it[krithiId] = member; it[KrithiCycleMembershipsTable.tagId] = tagId; it[sequenceOrder] = 1
                it[role] = CycleMemberRole.CORE; it[axisValue] = null; it[discriminativeAttributes] = """{"avarana":1}"""
            }
            KrithiTagsTable.insert { it[krithiId] = member; it[KrithiTagsTable.tagId] = tagId; it[sourceInfo] = "reviewed_cycle" }
            tagId
        }
    }

    private suspend fun krithi(title: String, state: WorkflowState = WorkflowState.PUBLISHED): UUID {
        val seed = TestFixtures.seedReferenceData(dal)
        return CatalogueTestFixtures.createKrithi(
            dal, title = title, composerId = seed.composer.id.toJavaUuid(), ragaIds = listOf(seed.raga.id.toJavaUuid()), workflowState = state,
        ).id.toJavaUuid()
    }

    // --- tests -------------------------------------------------------------------------------------

    @Test
    fun `published-only preview returns exactly the V2 response and explains near misses`() = testApplication {
        mount()
        val profile = activeProfile()
        doc(profile, "CYCLE_OVERVIEW", "Strong overview", tagId = reviewedCollection("strong"), axis = 0)
        doc(profile, "CYCLE_OVERVIEW", "Weak overview", tagId = reviewedCollection("weak"), axis = 5)

        val v2 = json.decodeFromString<CatalogueSearchResponseDto>(client.v2("qqq").bodyAsText())
        val previewBody = json.decodeFromString<PreviewSearchResponse>(client.previewSearch("qqq").bodyAsText())

        assertEquals(v2, previewBody.response)
        val explain = assertNotNull(previewBody.explain)
        assertEquals("gemini-embedding-2", explain.profileModel)
        val strong = explain.macroCandidates.single { it.title == "Cycle strong" }
        val weak = explain.macroCandidates.single { it.title == "Cycle weak" }
        assertTrue(strong.cleared)
        assertNull(strong.reason)
        assertFalse(weak.cleared)
        assertTrue(assertNotNull(weak.reason).contains("below"))
    }

    @Test
    fun `include-unpublished shows an indexed draft that V2 and published-only hide`() = testApplication {
        mount()
        val draft = krithi("Preview Zebra Draft", WorkflowState.DRAFT)
        doc(null, "COMPOSITION_OVERVIEW", "zebra wolf lexical marker", krithiId = draft, published = false)

        val v2 = json.decodeFromString<CatalogueSearchResponseDto>(client.v2("zebra wolf").bodyAsText())
        val published = json.decodeFromString<PreviewSearchResponse>(client.previewSearch("zebra wolf").bodyAsText())
        val unpublished = json.decodeFromString<PreviewSearchResponse>(client.previewSearch("zebra wolf", scope = "INCLUDE_UNPUBLISHED").bodyAsText())

        assertTrue(v2.items.isEmpty())
        assertTrue(published.response.items.isEmpty())
        assertEquals(listOf("Preview Zebra Draft"), unpublished.response.items.map { it.title })
        // Only a krithi that is not published is flagged; the published-only scope flags nothing.
        assertEquals(unpublished.response.items.map { it.krithiId }, unpublished.unpublishedKrithiIds)
        assertTrue(published.unpublishedKrithiIds.isEmpty())
    }

    @Test
    fun `preview search needs an admin and the twenty-first request in a minute gets 429`() = testApplication {
        mount()
        val viewer = jwtConfig.generateToken(Uuid.random(), emptyList())
        assertEquals(HttpStatusCode.Forbidden, client.previewSearch("x", token = viewer).status)

        val token = admin()
        repeat(20) { assertEquals(HttpStatusCode.OK, client.previewSearch("x$it", token = token).status, "request ${it + 1}") }
        assertEquals(HttpStatusCode.TooManyRequests, client.previewSearch("x21", token = token).status)
        // The allowance is per curator: another admin is not affected.
        assertEquals(HttpStatusCode.OK, client.previewSearch("x", token = admin()).status)
    }

    @Test
    fun `preview calls leave no mobile usage event and no audit row, while V2 does record`() = testApplication {
        mount()
        val auditBefore = DatabaseFactory.dbQuery { AuditLogTable.selectAll().count() }
        val id = krithi("Usage Silence")
        val token = admin()

        client.previewSearch("usage")
        client.get("/v1/admin/preview/catalogue/krithis/$id") { header(HttpHeaders.Authorization, "Bearer $token") }
        client.get("/v1/admin/preview/krithis/$id/index-status") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertTrue(usageEvents.isEmpty(), "preview paths must not reach the usage log: $usageEvents")
        assertEquals(auditBefore, DatabaseFactory.dbQuery { AuditLogTable.selectAll().count() })

        client.v2("usage")
        client.get("${RasikaCatalogueContract.KRITHIS_PATH}/$id")
        assertTrue(usageEvents.isNotEmpty(), "V2 calls are still recorded")
    }

    @Test
    fun `index status reports not indexed, current, and may be stale after an edit`() = testApplication {
        mount()
        val profile = activeProfile()
        val id = krithi("Index Status")
        val token = admin()
        suspend fun status(): PreviewIndexStatus = json.decodeFromString(
            client.get("/v1/admin/preview/krithis/$id/index-status") { header(HttpHeaders.Authorization, "Bearer $token") }.bodyAsText(),
        )

        assertEquals(PreviewIndexVerdict.NOT_INDEXED, status().verdict)

        doc(profile, "COMPOSITION_OVERVIEW", "Index Status overview", krithiId = id)
        DatabaseFactory.dbQuery { exec("UPDATE krithis SET updated_at = now() - interval '1 hour' WHERE id = '$id'") }
        val current = status()
        assertEquals(PreviewIndexVerdict.CURRENT, current.verdict, current.reasons.toString())
        assertTrue(current.embeddedForActiveProfile)

        DatabaseFactory.dbQuery { exec("UPDATE krithis SET updated_at = now() + interval '1 hour' WHERE id = '$id'") }
        val stale = status()
        assertEquals(PreviewIndexVerdict.MAY_BE_STALE, stale.verdict)
        assertTrue(stale.reasons.any { it.contains("changed after") })

        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/v1/admin/preview/krithis/${UUID.randomUUID()}/index-status") { header(HttpHeaders.Authorization, "Bearer $token") }.status,
        )
    }
}
