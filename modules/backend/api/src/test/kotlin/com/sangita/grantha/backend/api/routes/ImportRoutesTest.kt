package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.services.AutoApprovalService
import com.sangita.grantha.backend.api.services.EntityResolutionServiceImpl
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.backend.api.services.IWebScraper
import com.sangita.grantha.backend.api.services.ImportReviewer
import com.sangita.grantha.backend.api.services.ImportServiceImpl
import com.sangita.grantha.backend.api.services.NameNormalizationService
import com.sangita.grantha.backend.api.services.ScrapedKrithiMetadata
import com.sangita.grantha.backend.api.support.IntegrationTestBase
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ImportRoutesTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var importService: IImportService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        val dummyReviewer = object : ImportReviewer {
            override suspend fun reviewImport(
                id: kotlin.uuid.Uuid,
                request: com.sangita.grantha.backend.api.models.ImportReviewRequest
            ) = throw UnsupportedOperationException("Not used in route tests")
        }
        val autoApproval = AutoApprovalService(dummyReviewer)
        val env = ApiEnvironment(adminToken = "test", geminiApiKey = "test")
        val normalizer = NameNormalizationService()
        val entityResolver = EntityResolutionServiceImpl(dal, normalizer)
        importService = ImportServiceImpl(dal, env, entityResolver, normalizer) { autoApproval }
    }

    @Test
    fun `scrape route returns accepted and enqueues html extraction task`() = testApplication {
        val sourceUrl = "http://guru-guha.blogspot.com/2007/07/dikshitar-kriti-abhayaambaa-jagadambaa.html"
        val webScraper = mockk<IWebScraper>()
        coEvery { webScraper.scrapeKrithi(any()) } returns ScrapedKrithiMetadata(title = "unused")

        application {
            install(ContentNegotiation) { json() }
            routing {
                importRoutes(importService, webScraper)
            }
        }

        val response = client.post("/v1/admin/imports/scrape") {
            contentType(ContentType.Application.Json)
            setBody("""{"url":"$sourceUrl"}""")
        }

        assertEquals(HttpStatusCode.Accepted, response.status)
        val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(sourceUrl, payload["sourceKey"]?.jsonPrimitive?.content)
        assertNotNull(payload["id"]?.jsonPrimitive?.content)

        val (tasks, _) = dal.extractionQueue.list(format = listOf("HTML"), limit = 200)
        val matching = tasks.filter { it.sourceUrl == sourceUrl }
        assertEquals(1, matching.size)
        assertEquals("PENDING", matching.first().status)

        coVerify(exactly = 0) { webScraper.scrapeKrithi(any()) }
    }

    @Test
    fun `scrape route is idempotent for same url`() = testApplication {
        val sourceUrl = "http://guru-guha.blogspot.com/2007/07/dikshitar-kriti-akhilandesvari-raksha.html"
        val webScraper = mockk<IWebScraper>()
        coEvery { webScraper.scrapeKrithi(any()) } returns ScrapedKrithiMetadata(title = "unused")

        application {
            install(ContentNegotiation) { json() }
            routing {
                importRoutes(importService, webScraper)
            }
        }

        val first = client.post("/v1/admin/imports/scrape") {
            contentType(ContentType.Application.Json)
            setBody("""{"url":"$sourceUrl"}""")
        }
        val second = client.post("/v1/admin/imports/scrape") {
            contentType(ContentType.Application.Json)
            setBody("""{"url":"$sourceUrl"}""")
        }

        assertEquals(HttpStatusCode.Accepted, first.status)
        assertEquals(HttpStatusCode.Accepted, second.status)

        val firstPayload = Json.parseToJsonElement(first.bodyAsText()).jsonObject
        val secondPayload = Json.parseToJsonElement(second.bodyAsText()).jsonObject
        assertEquals(
            firstPayload["id"]?.jsonPrimitive?.content,
            secondPayload["id"]?.jsonPrimitive?.content,
        )

        val (tasks, _) = dal.extractionQueue.list(format = listOf("HTML"), limit = 200)
        val matching = tasks.filter { it.sourceUrl == sourceUrl }
        assertEquals(1, matching.size)

        coVerify(exactly = 0) { webScraper.scrapeKrithi(any()) }
    }
}
