package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.CycleMemberRole
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithiCycleMembershipsTable
import com.sangita.grantha.backend.dal.tables.KrithiTagsTable
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.backend.testsupport.CatalogueTestFixtures
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.backend.testsupport.TestFixtures
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.domain.model.catalogue.RasikaCatalogueContract
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** TRACK-150: reviewed-only public collections and the V2 reader cycle context. */
class CatalogueCollectionsRoutesTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var service: CatalogueService

    @BeforeEach
    fun setup() {
        dal = SangitaDalImpl()
        service = CatalogueService(dal)
    }

    private data class Seeded(val ids: Map<String, UUID>)

    private suspend fun seedCycle(slug: String, attrs: String = "{}"): Seeded {
        val seed = TestFixtures.seedReferenceData(dal)
        val composer = seed.composer.id.toJavaUuid()
        val raga = seed.raga.id.toJavaUuid()

        suspend fun krithi(title: String, state: WorkflowState = WorkflowState.PUBLISHED) =
            CatalogueTestFixtures.createKrithi(
                dal,
                title = title,
                composerId = composer,
                ragaIds = listOf(raga),
                workflowState = state,
            ).id.toJavaUuid()

        val ids = mapOf(
            "dhyana" to krithi("Cycle Dhyana"),
            "first" to krithi("Cycle First"),
            "second" to krithi("Cycle Second"),
            "unreviewed" to krithi("Cycle Unreviewed"),
            "draft" to krithi("Cycle Draft", WorkflowState.DRAFT),
            "disputed" to krithi("Cycle Disputed"),
        )
        DatabaseFactory.dbQuery {
            val tagId = TagsTable.insert {
                it[category] = "STOTRA_STYLE"
                it[TagsTable.slug] = slug
                it[displayNameEn] = "Test Cycle $slug"
            }[TagsTable.id].value

            fun member(key: String, role: CycleMemberRole, seq: Int, axis: String?, reviewed: Boolean) {
                KrithiCycleMembershipsTable.insert {
                    it[krithiId] = ids.getValue(key)
                    it[KrithiCycleMembershipsTable.tagId] = tagId
                    it[sequenceOrder] = seq
                    it[KrithiCycleMembershipsTable.role] = role
                    it[axisValue] = axis
                    it[discriminativeAttributes] =
                        """{"provenance_owner":"TRACK-X","mudra":"Test Mudra","avarana":$seq,"order_convention":null}"""
                            .let { base -> if (attrs == "{}") base else attrs }
                }
                KrithiTagsTable.insert {
                    it[krithiId] = ids.getValue(key)
                    it[KrithiTagsTable.tagId] = tagId
                    it[sourceInfo] = if (reviewed) "reviewed_cycle" else "manual"
                }
            }
            member("dhyana", CycleMemberRole.DHYANA, 0, null, reviewed = true)
            member("first", CycleMemberRole.CORE, 1, "Alpha", reviewed = true)
            member("second", CycleMemberRole.CORE, 2, "Beta", reviewed = true)
            member("unreviewed", CycleMemberRole.CORE, 3, "Gamma", reviewed = false)
            member("draft", CycleMemberRole.CORE, 4, "Delta", reviewed = true)
            member("disputed", CycleMemberRole.DISPUTED_CONJECTURE, 9, null, reviewed = true)
        }
        return Seeded(ids)
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.mount() = application {
        configureStatusPages()
        configureSerialization()
        routing {
            catalogueRoutes(service)
            rasikaCatalogueRoutes(service)
        }
    }

    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun `collection lists only reviewed published members in role then sequence order`() = testApplication {
        mount()
        seedCycle("test-cycle")

        val list = parse(client.get(RasikaCatalogueContract.COLLECTIONS_PATH).bodyAsText())["items"]!!.jsonArray
        assertEquals(1, list.size)
        // dhyana + first + second + disputed; unreviewed and draft are excluded
        assertEquals("4", list[0].jsonObject["memberCount"]!!.jsonPrimitive.content)

        val detail = parse(client.get("${RasikaCatalogueContract.COLLECTIONS_PATH}/test-cycle").bodyAsText())
        val titles = detail["members"]!!.jsonArray.map {
            it.jsonObject["krithi"]!!.jsonObject["title"]!!.jsonPrimitive.content
        }
        assertEquals(listOf("Cycle Dhyana", "Cycle First", "Cycle Second", "Cycle Disputed"), titles)
        assertEquals("2", detail["coreCount"]!!.jsonPrimitive.content)
        assertEquals("SOURCE", detail["orderBasis"]!!.jsonPrimitive.content)
        val disputed = detail["members"]!!.jsonArray.last().jsonObject
        assertEquals("disputed", disputed["label"]!!.jsonPrimitive.content)
        assertNull(disputed["position"]?.takeIf { it.toString() != "null" })
        val positions = detail["members"]!!.jsonArray.map { it.jsonObject["position"]?.jsonPrimitive?.content }
        assertEquals(listOf(null, "1", "2", null), positions)
    }

    @Test
    fun `raw stored attributes never leave the api`() = testApplication {
        mount()
        seedCycle("attr-cycle")
        val body = client.get("${RasikaCatalogueContract.COLLECTIONS_PATH}/attr-cycle").bodyAsText()
        assertFalse(body.contains("provenance_owner"))
        assertFalse(body.contains("TRACK-X"))
        assertTrue(body.contains("Test Mudra"))
    }

    @Test
    fun `order convention marks a cycle as performance order`() = testApplication {
        mount()
        seedCycle("convention-cycle", attrs = """{"order_convention":"Aradhana tradition","mudra":"M"}""")
        val detail = parse(client.get("${RasikaCatalogueContract.COLLECTIONS_PATH}/convention-cycle").bodyAsText())
        assertEquals("PERFORMANCE_CONVENTION", detail["orderBasis"]!!.jsonPrimitive.content)
        assertEquals("Aradhana tradition", detail["orderConvention"]!!.jsonPrimitive.content)
    }

    @Test
    fun `an axis value alone does not claim a sequence`() = testApplication {
        mount()
        seedCycle("label-cycle", attrs = """{"mudra":"M"}""")
        val detail = parse(client.get("${RasikaCatalogueContract.COLLECTIONS_PATH}/label-cycle").bodyAsText())
        assertEquals("UNSPECIFIED", detail["orderBasis"]!!.jsonPrimitive.content)
    }

    @Test
    fun `v2 reader carries neighbours and position while v1 reader omits cycle context`() = testApplication {
        mount()
        val seeded = seedCycle("reader-cycle")
        val second = seeded.ids.getValue("second")

        val v2 = parse(client.get("${RasikaCatalogueContract.KRITHIS_PATH}/$second").bodyAsText())
        val collection = v2["cycleContext"]!!.jsonObject["collections"]!!.jsonArray.single().jsonObject
        assertEquals("2", collection["position"]!!.jsonPrimitive.content)
        assertEquals("2", collection["coreCount"]!!.jsonPrimitive.content)
        assertEquals("Cycle First", collection["previous"]!!.jsonObject["title"]!!.jsonPrimitive.content)
        assertEquals("Cycle Disputed", collection["next"]!!.jsonObject["title"]!!.jsonPrimitive.content)

        val v1 = parse(client.get("${CatalogueContract.KRITHIS_PATH}/$second").bodyAsText())
        assertFalse(v1.containsKey("cycleContext"))
    }

    @Test
    fun `unreviewed or draft kritis expose no cycle context`() = testApplication {
        mount()
        val seeded = seedCycle("hidden-cycle")
        val unreviewed = parse(
            client.get("${RasikaCatalogueContract.KRITHIS_PATH}/${seeded.ids.getValue("unreviewed")}").bodyAsText(),
        )
        assertFalse(unreviewed.containsKey("cycleContext"))
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("${RasikaCatalogueContract.KRITHIS_PATH}/${seeded.ids.getValue("draft")}").status,
        )
    }

    @Test
    fun `validation and absence`() = testApplication {
        mount()
        assertEquals(
            0,
            parse(client.get(RasikaCatalogueContract.COLLECTIONS_PATH).bodyAsText())["items"]!!.jsonArray.size,
        )
        assertEquals(HttpStatusCode.NotFound, client.get("${RasikaCatalogueContract.COLLECTIONS_PATH}/missing").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("${RasikaCatalogueContract.COLLECTIONS_PATH}/Bad_Slug").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("${RasikaCatalogueContract.COLLECTIONS_PATH}?x=1").status)
    }
}
