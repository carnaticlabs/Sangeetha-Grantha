package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.config.JwtConfig
import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.plugins.configureCaching
import com.sangita.grantha.backend.api.plugins.configureSecurity
import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.AuditLogService
import com.sangita.grantha.backend.api.services.AutoApprovalService
import com.sangita.grantha.backend.api.services.CuratorService
import com.sangita.grantha.backend.api.services.EntityResolutionServiceImpl
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.backend.api.services.ImportReportGenerator
import com.sangita.grantha.backend.api.services.ImportReviewer
import com.sangita.grantha.backend.api.services.ImportServiceImpl
import com.sangita.grantha.backend.api.services.KrithiNotationService
import com.sangita.grantha.backend.api.services.KrithiServiceImpl
import com.sangita.grantha.backend.api.services.LyricVariantPersistenceService
import com.sangita.grantha.backend.api.services.NameNormalizationService
import com.sangita.grantha.backend.api.services.ReferenceDataServiceImpl
import com.sangita.grantha.backend.api.services.UserManagementService
import com.sangita.grantha.backend.api.support.Roles
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.authenticate
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * TRACK-112 Phase 2: money-path API scenarios (A1–A5).
 *
 * Drives the real HTTP surface via Ktor `testApplication` against a real database
 * (Testcontainers), covering the auth boundary, the public read surface, error-envelope
 * consistency, the curator workflow end to end, and OpenAPI conformance spot-checks.
 */
class MoneyPathApiTest : IntegrationTestBase() {

    private lateinit var dal: SangitaDal
    private lateinit var importService: IImportService
    private lateinit var krithiService: KrithiServiceImpl
    private lateinit var referenceDataService: ReferenceDataServiceImpl
    private lateinit var notationService: KrithiNotationService
    private lateinit var auditLogService: AuditLogService
    private lateinit var curatorService: CuratorService
    private lateinit var userManagementService: UserManagementService

    /** Deterministic secrets — the suite must not depend on ambient environment. */
    private val env = ApiEnvironment(
        adminToken = "test-admin-token",
        jwtSecret = "test-jwt-secret-for-money-path-suite",
        geminiApiKey = "test",
    )
    private val jwtConfig = JwtConfig.fromEnvironment(env)

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        val dummyReviewer = object : ImportReviewer {
            override suspend fun reviewImport(
                id: Uuid,
                request: ImportReviewRequest,
                reviewerUserId: Uuid?
            ) = throw UnsupportedOperationException("Not used in API tests")
        }
        val autoApproval = AutoApprovalService(dummyReviewer)
        val normalizer = NameNormalizationService()
        val entityResolver = EntityResolutionServiceImpl(dal, normalizer)
        importService = ImportServiceImpl(
            dal, env, entityResolver, normalizer,
            ImportReportGenerator(), LyricVariantPersistenceService(dal)
        ) { autoApproval }
        krithiService = KrithiServiceImpl(dal)
        referenceDataService = ReferenceDataServiceImpl(dal)
        notationService = KrithiNotationService(dal)
        auditLogService = AuditLogService(dal)
        curatorService = CuratorService(dal)
        userManagementService = UserManagementService(dal)
    }

    /** Create a real user row — JWTs carry a `userId` that FKs into `users`. */
    private suspend fun aUser(name: String = "API Test User"): Uuid =
        dal.users.create(email = "api-${Uuid.random()}@example.test", fullName = name).id

    /** A user actually granted the admin role, as `bootstrap-admin` provisions it. */
    private suspend fun anAdminUser(name: String = "API Test Admin"): Uuid {
        val id = aUser(name)
        dal.users.assignRole(id, Roles.ADMIN)
        return id
    }

    private fun tokenFor(userId: Uuid, roles: List<String>): String =
        jwtConfig.generateToken(userId, roles)

    /** A token carrying the admin role — what a real login now mints for an admin user. */
    private fun adminToken(userId: Uuid = Uuid.random()): String =
        jwtConfig.generateToken(userId, listOf(Roles.ADMIN))

    /**
     * Wire the slice of the real application under test: the same security, status-pages,
     * caching and serialization plugins `Application.module()` installs, and the routes
     * inside the same `authenticate("admin-auth")` block they live in production.
     */
    private fun ApplicationTestBuilder.sangitaApp() {
        application {
            configureSecurity(env)
            configureStatusPages()
            configureCaching()
            configureSerialization()
            routing {
                authRoutes(env, jwtConfig, userManagementService)
                publicKrithiRoutes(krithiService, referenceDataService, notationService)
                authenticate("admin-auth") {
                    authRefreshRoutes(jwtConfig, userManagementService)
                    // Mirrors Routing.kt: authentication proves identity, requireRole proves rights.
                    requireRole(Roles.ADMIN) {
                        importRoutes(importService)
                        auditRoutes(auditLogService)
                        curatorRoutes(curatorService)
                    }
                }
            }
        }
    }

    private suspend fun HttpResponse.json() = Json.parseToJsonElement(bodyAsText()).jsonObject

    // =========================================================================
    // A1: AuthN/Z boundary
    // =========================================================================

    @Nested
    @DisplayName("A1: Authentication boundary at the HTTP edge")
    inner class AuthBoundary {

        @Test
        fun `anonymous requests to admin routes are rejected with 401`() = testApplication {
            sangitaApp()
            listOf("/v1/admin/imports", "/v1/audit/logs").forEach { path ->
                assertEquals(
                    HttpStatusCode.Unauthorized, client.get(path).status,
                    "$path must reject anonymous callers"
                )
            }
        }

        @Test
        fun `a garbage or wrong-secret token is rejected with 401`() = testApplication {
            sangitaApp()
            val foreign = JwtConfig(
                secret = "a-different-secret",
                issuer = env.jwtIssuer,
                audience = env.jwtAudience,
                realm = env.jwtRealm,
                tokenTtlSeconds = 3600,
            ).generateToken(Uuid.random(), listOf(Roles.ADMIN))

            listOf("not-a-jwt", foreign).forEach { bad ->
                assertEquals(
                    HttpStatusCode.Unauthorized,
                    client.get("/v1/admin/imports") { header(HttpHeaders.Authorization, "Bearer $bad") }.status,
                    "a token not signed by this service must be rejected"
                )
            }
        }

        @Test
        fun `a validly-signed token is accepted`() = testApplication {
            sangitaApp()
            val token = adminToken()
            assertEquals(
                HttpStatusCode.OK,
                client.get("/v1/admin/imports") { header(HttpHeaders.Authorization, "Bearer $token") }.status
            )
        }

        /**
         * F3 (fixed) — the 403 tier.
         *
         * `authenticate("admin-auth")` proves only *who* the caller is: signature, audience and a
         * `userId` claim. No route checked the `roles` claim, so any validly-signed token —
         * including one with no roles at all — reached every admin route. `requireRole` now gates
         * them, so authentication and authorisation are separate answers.
         */
        @Test
        fun `a token with no admin role is refused with 403`() = testApplication {
            sangitaApp()
            val cases = mapOf(
                "no roles at all" to tokenFor(Uuid.random(), emptyList()),
                "an unrelated role" to tokenFor(Uuid.random(), listOf("VIEWER")),
            )
            cases.forEach { (label, token) ->
                assertEquals(
                    HttpStatusCode.Forbidden,
                    client.get("/v1/admin/imports") { header(HttpHeaders.Authorization, "Bearer $token") }.status,
                    "a token with $label must be authenticated but not authorised"
                )
            }
        }

        @Test
        fun `401 and 403 are distinguished — unauthenticated is not the same as unauthorised`() =
            testApplication {
                sangitaApp()
                assertEquals(
                    HttpStatusCode.Unauthorized,
                    client.get("/v1/admin/imports").status,
                    "no credentials at all is a 401"
                )
                assertEquals(
                    HttpStatusCode.Forbidden,
                    client.get("/v1/admin/imports") {
                        header(HttpHeaders.Authorization, "Bearer ${tokenFor(Uuid.random(), emptyList())}")
                    }.status,
                    "valid credentials without the role is a 403"
                )
            }

        /**
         * F3 (fixed) — the escalation hole.
         *
         * `/v1/auth/token` used to copy the caller's `roles` list straight into the JWT, so anyone
         * holding the shared ADMIN_TOKEN could mint an admin token for any user. Roles now come
         * from the user's stored `role_assignments`; the request field is gone.
         */
        @Test
        fun `the token endpoint ignores caller-supplied roles and reads them from storage`() =
            testApplication {
                sangitaApp()
                val plainUser = aUser("No Roles Granted")

                val response = client.post("/v1/auth/token") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"adminToken":"${env.adminToken}","userId":"$plainUser","roles":["${Roles.ADMIN}"]}""")
                }
                assertEquals(HttpStatusCode.OK, response.status)
                val minted = assertNotNull(response.json()["token"]?.jsonPrimitive?.content)

                // The token was issued, but asking for a role does not grant it.
                assertEquals(
                    HttpStatusCode.Forbidden,
                    client.get("/v1/admin/imports") { header(HttpHeaders.Authorization, "Bearer $minted") }.status,
                    "a self-declared role in the request body must not become a real one"
                )
            }

        @Test
        fun `a user actually granted the admin role receives a working token`() = testApplication {
            sangitaApp()
            val admin = anAdminUser()

            val response = client.post("/v1/auth/token") {
                contentType(ContentType.Application.Json)
                setBody("""{"adminToken":"${env.adminToken}","userId":"$admin"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val token = assertNotNull(response.json()["token"]?.jsonPrimitive?.content)

            assertEquals(
                HttpStatusCode.OK,
                client.get("/v1/admin/imports") { header(HttpHeaders.Authorization, "Bearer $token") }.status,
                "a stored role assignment must produce a token that works"
            )
        }

        /** Refresh must re-read roles, not carry the old claim forward — else revocation never lands. */
        @Test
        fun `refresh re-reads roles from storage so a revoked role stops working`() = testApplication {
            sangitaApp()
            val admin = anAdminUser("Soon To Be Revoked")
            val token = adminToken(admin)

            // Role revoked after the token was issued.
            dal.users.removeRole(admin, Roles.ADMIN)

            val refreshed = client.post("/v1/auth/refresh") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(HttpStatusCode.OK, refreshed.status, "refresh itself stays reachable")
            val newToken = assertNotNull(refreshed.json()["token"]?.jsonPrimitive?.content)

            assertEquals(
                HttpStatusCode.Forbidden,
                client.get("/v1/admin/imports") { header(HttpHeaders.Authorization, "Bearer $newToken") }.status,
                "the refreshed token must not carry the revoked role forward"
            )
        }

        @Test
        fun `a wrong shared admin token is rejected`() = testApplication {
            sangitaApp()
            val response = client.post("/v1/auth/token") {
                contentType(ContentType.Application.Json)
                setBody("""{"adminToken":"wrong","email":"nobody@example.test","roles":["ADMIN"]}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // =========================================================================
    // A2: Public read surface
    // =========================================================================

    @Nested
    @DisplayName("A2: Public read surface — DTO shape and ETag round-trip")
    inner class PublicReadSurface {

        @Test
        fun `krithi search is reachable anonymously and returns a paged envelope`() = testApplication {
            sangitaApp()
            val response = client.get("/v1/krithis/search")
            assertEquals(HttpStatusCode.OK, response.status, "public search must not require auth")

            val body = response.json()
            listOf("items", "total", "page", "pageSize").forEach { field ->
                assertTrue(body.containsKey(field), "search envelope must expose '$field', got ${body.keys}")
            }
        }

        @Test
        fun `reference data serves an ETag and honours If-None-Match with 304`() = testApplication {
            sangitaApp()
            val first = client.get("/v1/ragas")
            assertEquals(HttpStatusCode.OK, first.status)

            val etag = assertNotNull(
                first.headers[HttpHeaders.ETag],
                "reference-data routes must emit an ETag for client caching"
            )

            val second = client.get("/v1/ragas") { header(HttpHeaders.IfNoneMatch, etag) }
            assertEquals(
                HttpStatusCode.NotModified, second.status,
                "an unchanged ETag must short-circuit to 304, not re-send the payload"
            )
            assertTrue(second.bodyAsText().isEmpty(), "a 304 must carry no body")
        }

        @Test
        fun `an unknown krithi id returns 404, not 500`() = testApplication {
            sangitaApp()
            assertEquals(HttpStatusCode.NotFound, client.get("/v1/krithis/${Uuid.random()}").status)
        }

        @Test
        fun `a malformed krithi id returns 4xx, not 500`() = testApplication {
            sangitaApp()
            val status = client.get("/v1/krithis/not-a-uuid").status
            assertTrue(
                status.value in 400..499,
                "a malformed path parameter is a client error, got $status"
            )
        }
    }

    // =========================================================================
    // A3: Error-envelope consistency
    // =========================================================================

    @Nested
    @DisplayName("A3: Error envelopes are consistent and leak nothing")
    inner class ErrorEnvelopes {

        @Test
        fun `malformed JSON on an admin route yields a 4xx with a message envelope`() = testApplication {
            sangitaApp()
            val token = adminToken()
            val response = client.post("/v1/admin/imports") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("{ this is not json")
            }

            assertTrue(response.status.value in 400..499, "malformed JSON is a client error, got ${response.status}")
            val body = response.bodyAsText()
            assertFalse(
                body.contains("kotlinx.serialization") || body.contains("\tat "),
                "an error body must not expose serializer internals or a stack trace: $body"
            )
        }

        @Test
        fun `error bodies never contain a stack trace`() = testApplication {
            sangitaApp()
            val token = adminToken()
            val bodies = listOf(
                client.get("/v1/krithis/${Uuid.random()}").bodyAsText(),
                client.get("/v1/krithis/not-a-uuid").bodyAsText(),
                client.post("/v1/admin/imports") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody("{ broken")
                }.bodyAsText(),
            )
            bodies.forEach { body ->
                assertFalse(body.contains("\tat "), "stack frame leaked into an error body: $body")
                assertFalse(body.contains("Exception:"), "exception class leaked into an error body: $body")
            }
        }
    }

    // =========================================================================
    // A4: Curator workflow over HTTP
    // =========================================================================

    @Nested
    @DisplayName("A4: Curator workflow — queue → approve → public → audited")
    inner class CuratorWorkflow {

        @Test
        fun `an import approved over HTTP becomes publicly visible and leaves an audit row`() =
            testApplication {
                sangitaApp()
                val curatorId = aUser("HTTP Curator")
                val token = adminToken(curatorId)
                val auth: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

                // Seed one pending import through the service (the scrape path is network-bound).
                val importId = importService.submitImports(
                    listOf(
                        ImportKrithiRequest(
                            source = "api-money-path",
                            sourceKey = "http://example.com/a4-krithi",
                            rawTitle = "Api Workflow Krithi",
                            rawComposer = "Tyagaraja",
                            rawRaga = "Kalyani",
                            rawTala = "Adi",
                        )
                    )
                ).first().id

                // 1. The review queue shows it.
                val queue = client.get("/v1/admin/imports?status=PENDING", auth)
                assertEquals(HttpStatusCode.OK, queue.status)
                assertTrue(
                    queue.bodyAsText().contains(importId.toString()),
                    "the pending import must appear in the curator queue"
                )

                // 2. Approve it over HTTP.
                val approve = client.post("/v1/admin/imports/$importId/review") {
                    auth()
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"APPROVED"}""")
                }
                assertEquals(HttpStatusCode.OK, approve.status, "approval failed: ${approve.bodyAsText()}")

                val krithiId = assertNotNull(
                    approve.json()["mappedKrithiId"]?.jsonPrimitive?.content,
                    "an approved import must report the canonical krithi it produced"
                )

                // 3. It is visible on the public surface.
                val public = client.get("/v1/krithis/$krithiId")
                assertEquals(
                    HttpStatusCode.OK, public.status,
                    "the approved krithi must be readable via the public route"
                )
                assertEquals(
                    "Api Workflow Krithi",
                    public.json()["title"]?.jsonPrimitive?.content,
                    "the public DTO must carry the imported title"
                )

                // 4. The mutation is audited (Critical Rule #3).
                val audit = dal.auditLogs.listByEntity("krithis", Uuid.parse(krithiId))
                assertTrue(
                    audit.isNotEmpty(),
                    "approving over HTTP must leave an audit row for the created krithi"
                )
            }

        /**
         * Regression test for F4 (fixed).
         *
         * `reviewImport` used to catch every exception and re-wrap it as
         * `RuntimeException("Failed to create krithi: …")`, which hid the
         * `NoSuchElementException` that StatusPages maps to 404. A missing import therefore
         * returned **500** and leaked the internal phrasing to the caller. The catch is now
         * narrowed so client-error types propagate with their own identity.
         */
        @Test
        fun `approving a non-existent import returns 404, not 500`() = testApplication {
            sangitaApp()
            val token = adminToken(aUser())
            val response = client.post("/v1/admin/imports/${Uuid.random()}/review") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("""{"status":"APPROVED"}""")
            }
            assertEquals(
                HttpStatusCode.NotFound, response.status,
                "a missing import is a client error; body was: ${response.bodyAsText()}"
            )
            assertFalse(
                response.bodyAsText().contains("Failed to create krithi"),
                "the 404 must not leak the internal wrapper phrasing: ${response.bodyAsText()}"
            )
        }

        /** A composer-less import is a 400, not a 500 — same narrowed-catch fix. */
        @Test
        fun `approving an import with no composer returns 400, not 500`() = testApplication {
            sangitaApp()
            val token = adminToken(aUser())
            val importId = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "api-money-path",
                        sourceKey = "http://example.com/a4-no-composer",
                        rawTitle = "No Composer Over Http",
                        rawComposer = null,
                        rawRaga = "Kalyani",
                    )
                )
            ).first().id

            val response = client.post("/v1/admin/imports/$importId/review") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("""{"status":"APPROVED"}""")
            }
            assertEquals(
                HttpStatusCode.BadRequest, response.status,
                "a missing required field is a client error; body was: ${response.bodyAsText()}"
            )
        }

        @Test
        fun `a rejected import exposes no krithi on the public surface`() = testApplication {
            sangitaApp()
            val token = adminToken(aUser())
            val importId = importService.submitImports(
                listOf(
                    ImportKrithiRequest(
                        source = "api-money-path",
                        sourceKey = "http://example.com/a4-rejected",
                        rawTitle = "Rejected Over Http",
                        rawComposer = "Tyagaraja",
                        rawRaga = "Kalyani",
                    )
                )
            ).first().id

            val response = client.post("/v1/admin/imports/$importId/review") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("""{"status":"REJECTED"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)

            val search = client.get("/v1/krithis/search").json()
            assertEquals(
                0, search["total"]?.jsonPrimitive?.content?.toInt(),
                "a rejected import must not surface any krithi publicly"
            )
        }
    }

    // =========================================================================
    // A5: OpenAPI conformance spot-checks
    // =========================================================================

    @Nested
    @DisplayName("A5: Responses conform to the published OpenAPI contract")
    inner class OpenApiConformance {

        @Test
        fun `the auth token response matches the documented AuthTokenResponse shape`() = testApplication {
            sangitaApp()
            val userId = aUser("Contract Probe")
            val body = client.post("/v1/auth/token") {
                contentType(ContentType.Application.Json)
                setBody("""{"adminToken":"${env.adminToken}","userId":"$userId","roles":["ADMIN"]}""")
            }.json()

            assertTrue(body.containsKey("token"), "AuthTokenResponse.token is required by the spec")
            assertTrue(
                body.containsKey("expiresInSeconds"),
                "AuthTokenResponse.expiresInSeconds is required by the spec, got ${body.keys}"
            )
        }

        @Test
        fun `the search envelope matches the documented pagination contract`() = testApplication {
            sangitaApp()
            val body = client.get("/v1/krithis/search?page=1&pageSize=5").json()
            assertEquals("1", body["page"]?.jsonPrimitive?.content, "page must echo the request")
            assertEquals("5", body["pageSize"]?.jsonPrimitive?.content, "pageSize must echo the request")
        }

        /**
         * INCONSISTENCY found by TRACK-112 (A3/A5).
         *
         * `StatusPages` serialises every *exception-mapped* failure as the JSON envelope
         * `{"message": ...}` ([configureStatusPages]), but routes that handle their own misses
         * call `respondText` and emit a bare string — `/v1/krithis/{id}` returns the plain body
         * `Not found` with no content type of its own. A client cannot parse errors uniformly:
         * the same 404 is JSON from one route and text from another.
         *
         * Pinned as current behaviour. When the public routes are moved onto the shared envelope
         * (throw `NoSuchElementException`, or respond with `ErrorResponse`), this test should fail
         * — replace it with an assertion that every error body parses as `{ message }`.
         */
        @Test
        fun `GAP - route-level 404s bypass the JSON error envelope`() = testApplication {
            sangitaApp()
            val body = client.get("/v1/krithis/${Uuid.random()}").bodyAsText()
            assertEquals(
                "Not found", body,
                "current behaviour: this route emits a bare string, not the { message } envelope"
            )
        }
    }
}
