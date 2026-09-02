package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.services.AutoApprovalService
import com.sangita.grantha.backend.api.services.EntityResolutionServiceImpl
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.backend.api.services.ImportReviewer
import com.sangita.grantha.backend.api.services.ImportReportGenerator
import com.sangita.grantha.backend.api.services.ImportServiceImpl
import com.sangita.grantha.backend.api.services.LyricVariantPersistenceService
import com.sangita.grantha.backend.api.services.NameNormalizationService
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * TRACK-133: /re-extract must not silently cap matching queue rows at 1000.
 *
 * Before the fix the route did `list(limit = 1000)` then filtered the returned page
 * in Kotlin by source_url, so any row beyond the first 1000 for a URL family was
 * invisible — a re-extract returned totalMatching:0 and requeued nothing. The fix
 * pushes the source-URL match into SQL (findIdsBySourceUrlPattern) with no cap.
 */
class ReExtractCapTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var importService: IImportService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        val dummyReviewer = object : ImportReviewer {
            override suspend fun reviewImport(
                id: kotlin.uuid.Uuid,
                request: com.sangita.grantha.backend.api.models.ImportReviewRequest,
                reviewerUserId: kotlin.uuid.Uuid?
            ) = throw UnsupportedOperationException("Not used in route tests")
        }
        val autoApproval = AutoApprovalService(dummyReviewer)
        val env = ApiEnvironment(adminToken = "test", geminiApiKey = "test")
        val normalizer = NameNormalizationService()
        val entityResolver = EntityResolutionServiceImpl(dal, normalizer)
        importService = ImportServiceImpl(
            dal, env, entityResolver, normalizer,
            ImportReportGenerator(), LyricVariantPersistenceService(dal)
        ) { autoApproval }
    }

    @Test
    fun `re-extract requeues matching rows beyond the first 1000`() = testApplication {
        val pattern = "reextract-cap-test.example.com"
        val rowCount = 1001

        // Seed 1001 INGESTED queue rows sharing the URL family, in one bulk insert.
        kotlinx.coroutines.runBlocking {
            DatabaseFactory.dbQuery {
                exec(
                    """
                    INSERT INTO extraction_queue (source_url, source_format, status, attempts, max_attempts)
                    SELECT 'http://$pattern/krithi-' || g, 'PDF', 'INGESTED', 0, 3
                    FROM generate_series(1, $rowCount) g
                    """.trimIndent()
                )
            }
        }

        application {
            install(ContentNegotiation) { json() }
            routing { importRoutes(importService, dal = dal) }
        }

        val response = client.post("/v1/admin/imports/re-extract") {
            contentType(ContentType.Application.Json)
            setBody("""{"sourceUrlPattern":"$pattern"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        // Every row is seen and requeued — not capped at 1000.
        assertEquals(rowCount, payload["totalMatching"]?.jsonPrimitive?.content?.toInt())
        assertEquals(rowCount, payload["requeued"]?.jsonPrimitive?.content?.toInt())

        // All rows are now back to PENDING.
        val stillIngested = dal.extractionQueue.findIdsBySourceUrlPattern(
            pattern = pattern,
            status = listOf("INGESTED"),
        )
        assertEquals(0, stillIngested.size)
        val nowPending = dal.extractionQueue.findIdsBySourceUrlPattern(
            pattern = pattern,
            status = listOf("PENDING"),
        )
        assertEquals(rowCount, nowPending.size)
    }
}
