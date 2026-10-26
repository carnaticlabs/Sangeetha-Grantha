package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.clients.GeminiEmbeddingClient
import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.plugins.configureSecurity
import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.api.services.HybridSearchService
import com.sangita.grantha.backend.api.services.RasikaSearchService
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.CycleMemberRole
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithiCycleMembershipsTable
import com.sangita.grantha.backend.dal.tables.KrithiTagsTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.SemanticSearchResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchHitKindDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchResponseDto
import com.sangita.grantha.shared.domain.model.catalogue.RasikaCatalogueContract
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TRACK-152: V2 search with collection and kshetra cards. The synthetic query embedder returns the
 * unit vector e0, so a document embedded as e0 scores similarity 1.0 and one embedded as e1 scores 0.
 */
class RasikaSearchRoutesTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var catalogue: CatalogueService
    private lateinit var searchService: RasikaSearchService
    private lateinit var hybrid: HybridSearchService

    private val env = ApiEnvironment(
        adminToken = "test-admin-token",
        jwtSecret = "test-jwt-secret-for-rasika-search-suite",
        geminiApiKey = "test",
    )
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        catalogue = CatalogueService(dal)
        hybrid = HybridSearchService(dal, GeminiEmbeddingClient(apiKey = "", allowSyntheticFallback = true))
        searchService = RasikaSearchService(dal, hybrid)
    }

    private fun ApplicationTestBuilder.mount() = application {
        configureSecurity(env)
        configureStatusPages()
        configureSerialization()
        routing {
            semanticSearchRoutes(hybrid)
            rasikaSearchRoutes(searchService)
            catalogueRoutes(catalogue)
            rasikaCatalogueRoutes(catalogue)
        }
    }

    private suspend fun ApplicationTestBuilder.v2(
        query: String,
        mode: String = "hybrid",
        composerId: String? = null,
    ): CatalogueSearchResponseDto {
        val path = if (mode == "hybrid") RasikaCatalogueContract.SEARCH_HYBRID_PATH else RasikaCatalogueContract.SEARCH_SEMANTIC_PATH
        val body = """{"query":"$query","limit":10${composerId?.let { ""","composerId":"$it"""" } ?: ""}}"""
        val response = client.post(path) { contentType(ContentType.Application.Json); setBody(body) }
        assertEquals(HttpStatusCode.OK, response.status)
        return json.decodeFromString(response.bodyAsText())
    }

    // --- fixtures ---------------------------------------------------------------------------------

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

    /** Inserts a search document with an embedding on [axis] (0 = matches the query vector). */
    private suspend fun doc(
        profile: UUID?,
        kind: String,
        tagId: UUID? = null,
        templeId: UUID? = null,
        text: String,
        published: Boolean = true,
        chunk: Int = 0,
        axis: Int = 0,
    ) {
        DatabaseFactory.dbQuery {
            var docId: UUID? = null
            val hash = "h-${UUID.randomUUID()}"
            exec(
                "INSERT INTO search_documents (tag_id, temple_id, document_kind, original_content, indexed_content, " +
                    "content_hash, is_published, source_chunk_index) VALUES (${tagId?.let { "'$it'" } ?: "NULL"}, " +
                    "${templeId?.let { "'$it'" } ?: "NULL"}, '$kind', '$text', '$text', '$hash', $published, $chunk) RETURNING id",
                explicitStatementType = StatementType.SELECT,
            ) { rs -> if (rs.next()) docId = UUID.fromString(rs.getString("id")) }
            if (profile != null) {
                exec(
                    "INSERT INTO document_embeddings (document_id, profile_id, embedding, content_hash) " +
                        "VALUES ('${assertNotNull(docId)}', '$profile', '${vector(axis)}'::vector(768), '$hash')",
                )
            }
        }
    }

    /** A tag with one reviewed, published member (a collection `listCollections` would return). */
    private suspend fun reviewedCollection(slug: String, published: Boolean = true): UUID {
        val seed = TestFixtures.seedReferenceData(dal)
        val member = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Member of $slug",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = if (published) WorkflowState.PUBLISHED else WorkflowState.DRAFT,
        ).id.toJavaUuid()
        return DatabaseFactory.dbQuery {
            val tagId = TagsTable.insert {
                it[category] = "STOTRA_STYLE"
                it[TagsTable.slug] = slug
                it[displayNameEn] = "Cycle $slug"
            }[TagsTable.id].value
            KrithiCycleMembershipsTable.insert {
                it[krithiId] = member
                it[KrithiCycleMembershipsTable.tagId] = tagId
                it[sequenceOrder] = 1
                it[role] = CycleMemberRole.CORE
                it[axisValue] = null
                it[discriminativeAttributes] = """{"avarana":1}"""
            }
            KrithiTagsTable.insert {
                it[krithiId] = member
                it[KrithiTagsTable.tagId] = tagId
                it[sourceInfo] = "reviewed_cycle"
            }
            tagId
        }
    }

    /** A tag with a member that is not reviewed: its collection would 404. */
    private suspend fun unreviewedTag(slug: String): UUID = DatabaseFactory.dbQuery {
        TagsTable.insert {
            it[category] = "STOTRA_STYLE"
            it[TagsTable.slug] = slug
            it[displayNameEn] = "Cycle $slug"
        }[TagsTable.id].value
    }

    private suspend fun temple(name: String, parent: UUID? = null): UUID = DatabaseFactory.dbQuery {
        var id: UUID? = null
        exec(
            "INSERT INTO temples (name, name_normalized, city, parent_temple_id, place_kind) " +
                "VALUES ('$name', '${name.lowercase()}', 'Testpur', ${parent?.let { "'$it'" } ?: "NULL"}, " +
                "'${if (parent == null) "COMPLEX" else "SANNIDHI"}') RETURNING id",
            explicitStatementType = StatementType.SELECT,
        ) { rs -> if (rs.next()) id = UUID.fromString(rs.getString("id")) }
        assertNotNull(id)
    }

    private suspend fun publishedKrithiAt(templeId: UUID, title: String) {
        val seed = TestFixtures.seedReferenceData(dal)
        val created = CatalogueTestFixtures.createKrithi(
            dal,
            title = title,
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.PUBLISHED,
        )
        DatabaseFactory.dbQuery {
            KrithisTable.update({ KrithisTable.id eq created.id.toJavaUuid() }) { it[KrithisTable.templeId] = templeId }
        }
    }

    private suspend fun ApplicationTestBuilder.assertResolves(response: CatalogueSearchResponseDto) {
        response.macroHits.forEach { hit ->
            val path = when (hit.kind) {
                CatalogueSearchHitKindDto.COLLECTION -> "${RasikaCatalogueContract.COLLECTIONS_PATH}/${hit.collectionSlug}"
                CatalogueSearchHitKindDto.KSHETRA -> "${RasikaCatalogueContract.TEMPLES_PATH}/${hit.templeId}"
                CatalogueSearchHitKindDto.KRITHI -> error("a macro hit is never a KRITHI")
            }
            assertEquals(HttpStatusCode.OK, client.get(path).status, "card for ${hit.title} must resolve at $path")
        }
    }

    // --- COLLECTION -------------------------------------------------------------------------------

    @Test
    fun `a reviewed collection is returned as a COLLECTION card that resolves`() = testApplication {
        mount()
        val profile = activeProfile()
        val tag = reviewedCollection("sixth-enclosure")
        doc(profile, "CYCLE_OVERVIEW", tagId = tag, text = "The sixth enclosure of the cycle")

        val body = v2("sixth enclosure")

        val hit = body.macroHits.single()
        assertEquals(CatalogueSearchHitKindDto.COLLECTION, hit.kind)
        assertEquals("sixth-enclosure", hit.collectionSlug)
        assertNull(hit.templeId)
        assertEquals(1, hit.memberCount)
        assertEquals("The sixth enclosure of the cycle", hit.snippet)
        assertEquals(body.items.size + 1, body.totalMatches)
        assertResolves(body)
    }

    @Test
    fun `published overview without a reviewed member, and unpublished overview, are never returned`() = testApplication {
        mount()
        val profile = activeProfile()
        doc(profile, "CYCLE_OVERVIEW", tagId = unreviewedTag("no-reviewed-member"), text = "Orphan overview")
        doc(profile, "CYCLE_OVERVIEW", tagId = reviewedCollection("hidden"), text = "Hidden overview", published = false)

        assertTrue(v2("overview").macroHits.isEmpty())
    }

    @Test
    fun `a collection with two chunks yields one hit`() = testApplication {
        mount()
        val profile = activeProfile()
        val tag = reviewedCollection("two-chunks")
        doc(profile, "CYCLE_OVERVIEW", tagId = tag, text = "Chunk zero", chunk = 0, axis = 1)
        doc(profile, "CYCLE_OVERVIEW", tagId = tag, text = "Chunk one", chunk = 1, axis = 0)

        val hit = v2("chunk").macroHits.single()
        assertEquals("Chunk one", hit.snippet)
    }

    @Test
    fun `macro hits are capped at three and a weak match is below the floor`() = testApplication {
        mount()
        val profile = activeProfile()
        repeat(5) { i -> doc(profile, "CYCLE_OVERVIEW", tagId = reviewedCollection("strong-$i"), text = "Strong $i") }
        doc(profile, "CYCLE_OVERVIEW", tagId = reviewedCollection("weak"), text = "Weak", axis = 5)

        val hits = v2("zzz").macroHits
        assertEquals(3, hits.size)
        assertFalse(hits.any { it.collectionSlug == "weak" })
    }

    @Test
    fun `composer filter empties the macro group`() = testApplication {
        mount()
        val profile = activeProfile()
        doc(profile, "CYCLE_OVERVIEW", tagId = reviewedCollection("scoped"), text = "Scoped")
        val composer = TestFixtures.seedReferenceData(dal).composer.id.toString()

        assertEquals(1, v2("scoped").macroHits.size)
        assertTrue(v2("scoped", composerId = composer).macroHits.isEmpty())
    }

    @Test
    fun `semantic mode returns macro hits and with no profile returns nothing`() = testApplication {
        mount()
        val tag = reviewedCollection("semantic-card")
        doc(null, "CYCLE_OVERVIEW", tagId = tag, text = "Semantic card overview")
        val none = v2("semantic card overview", mode = "semantic")
        assertTrue(none.macroHits.isEmpty() && none.items.isEmpty())

        val profile = activeProfile()
        doc(profile, "CYCLE_OVERVIEW", tagId = reviewedCollection("semantic-card-2"), text = "Another")
        assertEquals("semantic-card-2", v2("anything", mode = "semantic").macroHits.single().collectionSlug)
    }

    @Test
    fun `without an embedding profile the lexical fallback includes macro documents`() = testApplication {
        mount()
        doc(null, "CYCLE_OVERVIEW", tagId = reviewedCollection("lexical-only"), text = "The fire lingam enclosure")
        doc(null, "CYCLE_OVERVIEW", tagId = reviewedCollection("unrelated"), text = "Something else entirely")

        val hit = v2("fire lingam").macroHits.single()
        assertEquals("lexical-only", hit.collectionSlug)
        assertNull(hit.rrfScore?.takeIf { it == 0.0 })
    }

    // --- KSHETRA ----------------------------------------------------------------------------------

    @Test
    fun `an overview-only temple is returned as a recorded-gap KSHETRA card that resolves`() = testApplication {
        mount()
        val profile = activeProfile()
        val kanya = temple("Kanyakumari Bhagavati")
        doc(profile, "KSHETRA_OVERVIEW", templeId = kanya, text = "Recorded gap shrine")

        val body = v2("kanyakumari")

        val hit = body.macroHits.single()
        assertEquals(CatalogueSearchHitKindDto.KSHETRA, hit.kind)
        assertEquals(kanya.toString(), hit.templeId.toString())
        assertNull(hit.collectionSlug)
        assertTrue(hit.recordedGap)
        assertEquals(0, hit.publishedKrithiCount)
        assertEquals("Testpur", hit.subtitle)
        assertResolves(body)
    }

    @Test
    fun `unpublished overview and a draft-only temple are never returned`() = testApplication {
        mount()
        val profile = activeProfile()
        val hidden = temple("Hidden Temple")
        doc(profile, "KSHETRA_OVERVIEW", templeId = hidden, text = "Hidden", published = false)
        val draftOnly = temple("Draft Only Temple")
        val seed = TestFixtures.seedReferenceData(dal)
        val draft = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Draft Piece",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.DRAFT,
        )
        DatabaseFactory.dbQuery {
            KrithisTable.update({ KrithisTable.id eq draft.id.toJavaUuid() }) { it[templeId] = draftOnly }
        }
        // An overview that is published but whose temple has no published kriti and is overview-only is
        // eligible; this one has neither, so only the overview document could make it appear.
        doc(profile, "KSHETRA_OVERVIEW", templeId = draftOnly, text = "Draft only overview", published = false)

        assertTrue(v2("temple").macroHits.isEmpty())
    }

    @Test
    fun `a parent eligible only through a child is returned and resolves`() = testApplication {
        mount()
        val profile = activeProfile()
        val parent = temple("Parent Complex")
        val child = temple("Child Shrine", parent = parent)
        publishedKrithiAt(child, "Child Kriti")
        doc(profile, "KSHETRA_OVERVIEW", templeId = parent, text = "Parent overview")
        doc(profile, "KSHETRA_OVERVIEW", templeId = child, text = "Child overview", axis = 3)

        val body = v2("parent")

        assertEquals(parent.toString(), body.macroHits.single().templeId.toString())
        assertFalse(body.macroHits.single().recordedGap)
        assertResolves(body)
    }

    // --- Contract and ranking ---------------------------------------------------------------------

    @Test
    fun `V1 and V2 return identical items and V1 never carries macro hits`() = testApplication {
        mount()
        val profile = activeProfile()
        doc(profile, "CYCLE_OVERVIEW", tagId = reviewedCollection("parity"), text = "Parity overview")
        val seed = TestFixtures.seedReferenceData(dal)
        CatalogueTestFixtures.createKrithi(
            dal,
            title = "Parity Kriti",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.PUBLISHED,
        )

        val v1Text = client.post("/v1/search/hybrid") {
            contentType(ContentType.Application.Json)
            setBody("""{"query":"parity","limit":10}""")
        }.bodyAsText()
        val v1 = json.decodeFromString<SemanticSearchResponse>(v1Text)
        val v2 = v2("parity")

        assertEquals(v1.items, v2.items)
        assertEquals(1, v2.macroHits.size)
        assertFalse("macroHits" in v1Text)
        assertEquals(v1.totalMatches + 1, v2.totalMatches)
    }

    @Test
    fun `a typed full title sets exactTitleMatchId and leaves kriti order alone`() = testApplication {
        mount()
        val profile = activeProfile()
        doc(profile, "CYCLE_OVERVIEW", tagId = reviewedCollection("ganapati"), text = "Ganapati cycle")
        val seed = TestFixtures.seedReferenceData(dal)
        val kriti = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Vatapi Ganapatim",
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = WorkflowState.PUBLISHED,
        )

        val exact = v2("Vatapi Ganapatim")
        assertEquals(kriti.id, exact.exactTitleMatchId)

        val partial = v2("Vatapi")
        assertNull(partial.exactTitleMatchId)
        assertEquals(exact.items.map { it.krithiId }, v2("Vatapi Ganapatim").items.map { it.krithiId })
    }

    @Test
    fun `empty query returns an empty V2 response`() = testApplication {
        mount()
        val body = v2("   ")
        assertEquals(0, body.totalMatches)
        assertTrue(body.macroHits.isEmpty() && body.items.isEmpty())
    }
}
