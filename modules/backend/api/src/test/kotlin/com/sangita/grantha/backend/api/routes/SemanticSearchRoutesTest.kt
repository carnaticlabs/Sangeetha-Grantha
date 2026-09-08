package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.clients.GeminiEmbeddingClient
import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.plugins.configureSecurity
import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.HybridSearchService
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.SemanticSearchResponse
import com.sangita.grantha.backend.api.config.JwtConfig
import com.sangita.grantha.backend.api.support.Roles
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

class SemanticSearchRoutesTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var embeddingClient: GeminiEmbeddingClient
    private lateinit var hybridSearchService: HybridSearchService

    private val env = ApiEnvironment(
        adminToken = "test-admin-token",
        jwtSecret = "test-jwt-secret-for-semantic-search-suite",
        geminiApiKey = "test",
    )
    private val jwtConfig = JwtConfig.fromEnvironment(env)
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        embeddingClient = GeminiEmbeddingClient(apiKey = "", allowSyntheticFallback = true)
        hybridSearchService = HybridSearchService(dal, embeddingClient)
    }

    @Test
    fun `empty query returns empty results`() = testApplication {
        application {
            configureSecurity(env)
            configureStatusPages()
            configureSerialization()
            routing { semanticSearchRoutes(hybridSearchService) }
        }

        val response = client.post("/v1/search/hybrid") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SemanticSearchRequest.serializer(), SemanticSearchRequest(query = "   ")))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<SemanticSearchResponse>(response.bodyAsText())
        assertEquals(0, body.totalMatches)
        assertEquals(0, body.items.size)
    }

    @Test
    fun `hybrid search endpoint executes and returns semantic search response`() = testApplication {
        application {
            configureSecurity(env)
            configureStatusPages()
            configureSerialization()
            routing { semanticSearchRoutes(hybridSearchService) }
        }

        val seed = TestFixtures.seedReferenceData(dal)
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Vatapi Ganapatim",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.PUBLISHED,
        )

        val response = client.post("/v1/search/hybrid") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SemanticSearchRequest.serializer(), SemanticSearchRequest(query = "Vatapi", limit = 10)))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<SemanticSearchResponse>(response.bodyAsText())
        assertNotNull(body.items)
    }

    @Test
    fun `semantic search endpoint executes with vector fallback`() = testApplication {
        application {
            configureSecurity(env)
            configureStatusPages()
            configureSerialization()
            routing { semanticSearchRoutes(hybridSearchService) }
        }

        val response = client.post("/v1/search/semantic") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SemanticSearchRequest.serializer(), SemanticSearchRequest(query = "Dikshitar Hamsadhvani", limit = 5)))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<SemanticSearchResponse>(response.bodyAsText())
        assertNotNull(body.items)
    }

    @Test
    fun `semantic search binds to the active profile and returns indexed krithi`() = testApplication {
        application {
            configureSecurity(env)
            configureStatusPages()
            configureSerialization()
            routing { semanticSearchRoutes(hybridSearchService) }
        }

        val seed = TestFixtures.seedReferenceData(dal)
        val krithi = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Vatapi Ganapatim",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.PUBLISHED,
        )
        // Same model/width as the test embedder; vector equals the synthetic fallback unit vector.
        val profileId = insertEmbeddingProfile(modelName = "gemini-embedding-2", dimensions = 768)
        insertIndexedDocument(krithi.id.toJavaUuid(), profileId)

        val response = client.post("/v1/search/semantic") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SemanticSearchRequest.serializer(), SemanticSearchRequest(query = "Ganapati", limit = 5)))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<SemanticSearchResponse>(response.bodyAsText())
        assertEquals(1, body.totalMatches)
        assertEquals(krithi.id, body.items.single().krithiId)
        assertEquals(1.0, body.items.single().similarityScore, 1e-6)
    }

    @Test
    fun `active profile from a different model is rejected instead of mixing embedding spaces`() = testApplication {
        application {
            configureSecurity(env)
            configureStatusPages()
            configureSerialization()
            routing { semanticSearchRoutes(hybridSearchService) }
        }

        insertEmbeddingProfile(modelName = "some-other-embedding-model", dimensions = 768)

        val response = client.post("/v1/search/hybrid") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SemanticSearchRequest.serializer(), SemanticSearchRequest(query = "Vatapi", limit = 5)))
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    private suspend fun insertEmbeddingProfile(modelName: String, dimensions: Int): java.util.UUID =
        DatabaseFactory.dbQuery {
            var id: java.util.UUID? = null
            exec(
                "INSERT INTO embedding_profiles (model_name, dimensions, task_type, is_active) " +
                    "VALUES ('$modelName', $dimensions, 'RETRIEVAL_DOCUMENT', true) RETURNING id",
                explicitStatementType = StatementType.SELECT,
            ) { rs -> if (rs.next()) id = java.util.UUID.fromString(rs.getString("id")) }
            assertNotNull(id)
        }

    private suspend fun insertIndexedDocument(krithiId: java.util.UUID, profileId: java.util.UUID) {
        val unitVector = List(768) { if (it == 0) "1" else "0" }.joinToString(",", "[", "]")
        DatabaseFactory.dbQuery {
            var docId: java.util.UUID? = null
            exec(
                "INSERT INTO search_documents (krithi_id, document_kind, original_content, indexed_content, content_hash) " +
                    "VALUES ('$krithiId', 'COMPOSITION_OVERVIEW', 'Vatapi Ganapatim bhaje', " +
                    "'[Title: Vatapi Ganapatim] Vatapi Ganapatim bhaje', 'hash-1') RETURNING id",
                explicitStatementType = StatementType.SELECT,
            ) { rs -> if (rs.next()) docId = java.util.UUID.fromString(rs.getString("id")) }
            exec(
                "INSERT INTO document_embeddings (document_id, profile_id, embedding, content_hash) " +
                    "VALUES ('${assertNotNull(docId)}', '$profileId', '$unitVector'::vector(768), 'hash-1')",
            )
        }
    }

    @Test
    fun `hybrid search executes for authenticated admin without filter syntax error`() = testApplication {
        application {
            configureSecurity(env)
            configureStatusPages()
            configureSerialization()
            routing { semanticSearchRoutes(hybridSearchService) }
        }

        val adminToken = jwtConfig.generateToken(Uuid.random(), listOf(Roles.ADMIN))

        val response = client.post("/v1/search/hybrid") {
            header(io.ktor.http.HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SemanticSearchRequest.serializer(), SemanticSearchRequest(query = "Swarajathi Syama Sastri", limit = 10)))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<SemanticSearchResponse>(response.bodyAsText())
        assertNotNull(body.items)
    }
}
