package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.TempleEligibility
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.catalogue.RasikaCatalogueContract
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** TRACK-151 Phase 1: public kshetra list and detail, and the shared [TempleEligibility] rule. */
class CatalogueTemplesRoutesTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var service: CatalogueService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        service = CatalogueService(dal)
    }

    private fun ApplicationTestBuilder.mount() = application {
        configureStatusPages()
        configureSerialization()
        routing {
            catalogueRoutes(service)
            rasikaCatalogueRoutes(service)
        }
    }

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private suspend fun temple(
        name: String,
        parent: UUID? = null,
        placeKind: String = "LOCALITY",
        mandalam: String? = null,
        bhuta: String? = null,
        sthalaVriksha: String? = null,
    ): UUID = DatabaseFactory.dbQuery {
        var id: UUID? = null
        exec(
            "INSERT INTO temples (name, name_normalized, city, parent_temple_id, place_kind, mandalam, bhuta, sthala_vriksha) " +
                "VALUES ('$name', '${name.lowercase()}', 'Testpur', ${parent?.let { "'$it'" } ?: "NULL"}, " +
                "'$placeKind', ${mandalam?.let { "'$it'" } ?: "NULL"}, ${bhuta?.let { "'$it'" } ?: "NULL"}, " +
                "${sthalaVriksha?.let { "'$it'" } ?: "NULL"}) RETURNING id",
            explicitStatementType = StatementType.SELECT,
        ) { rs -> if (rs.next()) id = UUID.fromString(rs.getString("id")) }
        assertNotNull(id)
    }

    private suspend fun overview(templeId: UUID, text: String, published: Boolean = true, chunk: Int = 0) {
        DatabaseFactory.dbQuery {
            exec(
                "INSERT INTO search_documents (temple_id, document_kind, original_content, indexed_content, " +
                    "content_hash, is_published, source_chunk_index) VALUES ('$templeId', 'KSHETRA_OVERVIEW', " +
                    "'$text', '$text', 'h-$templeId-$chunk', $published, $chunk)",
            )
        }
    }

    private suspend fun krithi(templeId: UUID, title: String, state: WorkflowState = WorkflowState.PUBLISHED) {
        val seed = TestFixtures.seedReferenceData(dal)
        val created = CatalogueTestFixtures.createKrithi(
            dal,
            title = title,
            composerId = seed.composer.id.toJavaUuid(),
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = state,
        )
        DatabaseFactory.dbQuery {
            KrithisTable.update({ KrithisTable.id eq created.id.toJavaUuid() }) { it[KrithisTable.templeId] = templeId }
        }
    }

    private suspend fun ApplicationTestBuilder.listIds(query: String = ""): List<String> =
        parse(client.get(RasikaCatalogueContract.TEMPLES_PATH + query).bodyAsText())["items"]!!.jsonArray
            .map { it.jsonObject["id"]!!.jsonPrimitive.content }

    private suspend fun ApplicationTestBuilder.detail(id: UUID) =
        client.get("${RasikaCatalogueContract.TEMPLES_PATH}/$id")

    @Test
    fun `eligibility covers draft only, published, overview only, unpublished overview and neither`() = testApplication {
        mount()
        val draftOnly = temple("T Draft Only").also { krithi(it, "Draft Piece", WorkflowState.DRAFT) }
        val published = temple("T Published").also { krithi(it, "Published Piece") }
        val overviewOnly = temple("T Overview Only").also { overview(it, "Only an overview.") }
        val unpublishedOverview = temple("T Unpublished Overview").also { overview(it, "Hidden.", published = false) }
        val neither = temple("T Neither")

        val eligible = DatabaseFactory.dbQuery { TempleEligibility.eligibleIds() }
        assertEquals(setOf(published, overviewOnly), listOf(draftOnly, published, overviewOnly, unpublishedOverview, neither).filter { it in eligible }.toSet())

        val listed = listIds()
        assertTrue(published.toString() in listed && overviewOnly.toString() in listed)
        listOf(draftOnly, unpublishedOverview, neither).forEach {
            assertFalse(it.toString() in listed, "ineligible temple listed")
            assertEquals(HttpStatusCode.NotFound, detail(it).status)
        }
        assertEquals(HttpStatusCode.OK, detail(published).status)
    }

    @Test
    fun `detail counts only published kritis in title order and omits place`() = testApplication {
        mount()
        val id = temple("T Mixed", placeKind = "COMPLEX", mandalam = "CHOLA", bhuta = "APPU", sthalaVriksha = "Vilva")
        krithi(id, "Zeta Kriti")
        krithi(id, "Alpha Kriti")
        krithi(id, "Draft Kriti", WorkflowState.DRAFT)
        overview(id, "Second chunk.", chunk = 1)
        overview(id, "First chunk.", chunk = 0)

        val body = parse(detail(id).bodyAsText())
        assertEquals(2, body["publishedKrithiCount"]!!.jsonPrimitive.content.toInt())
        assertEquals(listOf("Alpha Kriti", "Zeta Kriti"), body["krithis"]!!.jsonArray.map { it.jsonObject["title"]!!.jsonPrimitive.content })
        assertEquals("First chunk.", body["overview"]!!.jsonPrimitive.content)
        assertEquals("APPU", body["element"]!!.jsonPrimitive.content)
        assertEquals("Vilva", body["sthalaVriksha"]!!.jsonPrimitive.content)
        assertEquals("false", body["recordedGap"]!!.jsonPrimitive.content)
        assertFalse("place" in body, "place stays absent until TRACK-154")
        assertTrue(body.keys.none { it in setOf("latitude", "longitude", "googlePlaceId") })
    }

    @Test
    fun `parent with eligible child is a container, child is nested and not listed`() = testApplication {
        mount()
        val parent = temple("T Complex", placeKind = "COMPLEX", mandalam = "CHOLA")
        val child = temple("T Shrine", parent = parent, placeKind = "SANNIDHI", mandalam = "CHOLA").also { krithi(it, "Shrine Song") }
        val emptyChild = temple("T Empty Shrine", parent = parent, placeKind = "SANNIDHI")

        val listed = listIds()
        assertTrue(parent.toString() in listed)
        assertFalse(child.toString() in listed, "children are nested, not repeated in the list")

        val body = parse(detail(parent).bodyAsText())
        assertEquals(0, body["publishedKrithiCount"]!!.jsonPrimitive.content.toInt())
        assertEquals(1, body["childCount"]!!.jsonPrimitive.content.toInt())
        assertEquals("false", body["recordedGap"]!!.jsonPrimitive.content)
        assertEquals(listOf(child.toString()), body["children"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content })
        assertEquals(HttpStatusCode.NotFound, detail(emptyChild).status)

        val childBody = parse(detail(child).bodyAsText())
        assertEquals(parent.toString(), childBody["parent"]!!.jsonObject["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `recorded gap is derived from an overview with no published kriti here or in children`() = testApplication {
        mount()
        val gap = temple("T Gap").also { overview(it, "Recorded gap. No kriti is seeded.") }
        val gapWithKritiChild = temple("T Not Gap").also { overview(it, "Has a child with a kriti.") }
        temple("T Not Gap Child", parent = gapWithKritiChild).also { krithi(it, "Child Song") }

        assertEquals("true", parse(detail(gap).bodyAsText())["recordedGap"]!!.jsonPrimitive.content)
        assertEquals("false", parse(detail(gapWithKritiChild).bodyAsText())["recordedGap"]!!.jsonPrimitive.content)
        val row = parse(client.get(RasikaCatalogueContract.TEMPLES_PATH).bodyAsText())["items"]!!.jsonArray
            .first { it.jsonObject["id"]!!.jsonPrimitive.content == gap.toString() }.jsonObject
        assertEquals("true", row["recordedGap"]!!.jsonPrimitive.content)
    }

    @Test
    fun `list is ordered by mandalam then name and filters by element and mandalam`() = testApplication {
        mount()
        val nadu = temple("A Nadu", mandalam = "NADU").also { overview(it, "x") }
        val noMandalam = temple("A Nowhere").also { overview(it, "x") }
        val pandyaB = temple("B Pandya", mandalam = "PANDYA").also { overview(it, "x") }
        val pandyaA = temple("A Pandya", mandalam = "PANDYA", bhuta = "VAYU").also { overview(it, "x") }
        val chola = temple("Z Chola", mandalam = "CHOLA").also { overview(it, "x") }
        val mine = setOf(nadu, noMandalam, pandyaB, pandyaA, chola).map { it.toString() }.toSet()

        assertEquals(
            listOf(chola, pandyaA, pandyaB, nadu, noMandalam).map { it.toString() },
            listIds().filter { it in mine },
        )
        assertEquals(listOf(pandyaA.toString()), listIds("?element=VAYU").filter { it in mine })
        assertEquals(
            listOf(pandyaA, pandyaB).map { it.toString() },
            listIds("?mandalam=PANDYA").filter { it in mine },
        )
        assertEquals(emptyList(), listIds("?mandalam=PANDYA&element=AGNI").filter { it in mine })
    }

    @Test
    fun `bad requests are 400 and unknown ids are 404`() = testApplication {
        mount()
        val base = RasikaCatalogueContract.TEMPLES_PATH
        assertEquals(HttpStatusCode.BadRequest, client.get("$base?page=1").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("$base?element=FIRE").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("$base?mandalam=chola").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("$base?element=AGNI&element=VAYU").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("$base/not-a-uuid").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("$base/${UUID.randomUUID()}?x=1").status)
        assertEquals(HttpStatusCode.NotFound, client.get("$base/${UUID.randomUUID()}").status)
    }

    @Test
    fun `v1 catalogue has no temples route`() = testApplication {
        mount()
        assertEquals(HttpStatusCode.NotFound, client.get("/v1/catalogue/temples").status)
    }
}
