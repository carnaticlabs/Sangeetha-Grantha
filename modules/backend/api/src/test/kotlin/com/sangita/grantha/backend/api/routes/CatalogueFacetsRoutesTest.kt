package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.CycleMemberRole
import com.sangita.grantha.backend.dal.enums.VibhaktiCase
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithiCycleMembershipsTable
import com.sangita.grantha.backend.dal.tables.KrithiTagsTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.TagsTable
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** TRACK-153 Phase 1: lakshana facet filters, `/v2/catalogue/facets`, counts and reader chips. */
class CatalogueFacetsRoutesTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var service: CatalogueService

    private lateinit var dikshitar: UUID
    private lateinit var tyagaraja: UUID
    private val k = mutableMapOf<String, UUID>()

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

    private suspend fun temple(name: String, parent: UUID? = null, mandalam: String?, bhuta: String? = null): UUID =
        DatabaseFactory.dbQuery {
            var id: UUID? = null
            exec(
                "INSERT INTO temples (name, name_normalized, city, parent_temple_id, place_kind, mandalam, bhuta) VALUES " +
                    "('$name', '${name.lowercase()}', 'Testpur', ${parent?.let { "'$it'" } ?: "NULL"}, " +
                    "'${if (parent == null) "COMPLEX" else "SANNIDHI"}', ${mandalam?.let { "'$it'" } ?: "NULL"}, " +
                    "${bhuta?.let { "'$it'" } ?: "NULL"}) RETURNING id",
                explicitStatementType = StatementType.SELECT,
            ) { rs -> if (rs.next()) id = UUID.fromString(rs.getString("id")) }
            assertNotNull(id)
        }

    private suspend fun krithi(
        key: String,
        composer: UUID,
        state: WorkflowState = WorkflowState.PUBLISHED,
        vibhakti: VibhaktiCase? = null,
        temple: UUID? = null,
    ) {
        val seed = TestFixtures.seedReferenceData(dal)
        val created = CatalogueTestFixtures.createKrithi(
            dal,
            title = "Facet $key",
            composerId = composer,
            ragaIds = listOf(seed.raga.id.toJavaUuid()),
            workflowState = state,
        ).id.toJavaUuid()
        k[key] = created
        if (vibhakti != null || temple != null) {
            DatabaseFactory.dbQuery {
                KrithisTable.update({ KrithisTable.id eq created }) {
                    if (vibhakti != null) it[vibhaktiCase] = vibhakti
                    if (temple != null) it[templeId] = temple
                }
            }
        }
    }

    private suspend fun cycle(slug: String, vararg members: Triple<String, String?, Boolean>) {
        DatabaseFactory.dbQuery {
            val tagId = TagsTable.insert {
                it[category] = "STOTRA_STYLE"
                it[TagsTable.slug] = slug
                it[displayNameEn] = "Cycle $slug"
            }[TagsTable.id].value
            members.forEachIndexed { index, (key, axis, reviewed) ->
                KrithiCycleMembershipsTable.insert {
                    it[krithiId] = k.getValue(key)
                    it[KrithiCycleMembershipsTable.tagId] = tagId
                    it[sequenceOrder] = index
                    it[role] = CycleMemberRole.CORE
                    it[axisValue] = axis
                }
                KrithiTagsTable.insert {
                    it[krithiId] = k.getValue(key)
                    it[KrithiTagsTable.tagId] = tagId
                    it[sourceInfo] = if (reviewed) "reviewed_cycle" else "manual"
                }
            }
        }
    }

    /**
     * Dikshitar d1 (Tritiya, Chola/Appu temple, Galopede tune, in `alpha-cycle` and the tune cycle),
     * d2 (Dvitiya, same temple, `alpha-cycle`), d5 at a child shrine (Tondai), d3 draft at a temple
     * that has only a draft (ineligible), d6 only in an unreviewed membership; Tyagaraja t1 in `tyagaraja-cycle`.
     */
    private suspend fun seed() {
        dikshitar = dal.composers.findOrCreate(name = "Muthuswami Dikshitar").id.toJavaUuid()
        tyagaraja = dal.composers.findOrCreate(name = "Tyagaraja").id.toJavaUuid()
        val parent = temple("T Parent", mandalam = "CHOLA", bhuta = "APPU")
        val child = temple("T Child", parent = parent, mandalam = "TONDAI")
        val ineligible = temple("T Draft Only", mandalam = "PANDYA", bhuta = "AGNI")
        krithi("d1", dikshitar, vibhakti = VibhaktiCase.TRITIYA, temple = parent)
        krithi("d2", dikshitar, vibhakti = VibhaktiCase.DVITIYA, temple = parent)
        krithi("d5", dikshitar, vibhakti = VibhaktiCase.DVITIYA, temple = child)
        krithi("d3", dikshitar, state = WorkflowState.DRAFT, vibhakti = VibhaktiCase.PRATHAMA, temple = ineligible)
        krithi("d6", dikshitar)
        krithi("t1", tyagaraja)
        cycle("alpha-cycle", Triple("d1", null, true), Triple("d2", null, true), Triple("d3", null, true))
        cycle("nottusvara-sahitya-x", Triple("d6", "Hidden Tune", false))
        cycle("tyagaraja-cycle", Triple("t1", null, true))
        // The reference seed may or may not have left the pinned tune cycle tag in place.
        val tuneTag = DatabaseFactory.dbQuery {
            TagsTable.select(TagsTable.id).where { TagsTable.slug eq "nottusvara-sahitya" }.singleOrNull()
                ?.get(TagsTable.id)?.value
                ?: TagsTable.insert {
                    it[category] = "STOTRA_STYLE"
                    it[TagsTable.slug] = "nottusvara-sahitya"
                    it[displayNameEn] = "Nottusvara Sahitya Compositions"
                }[TagsTable.id].value
        }
        DatabaseFactory.dbQuery {
            KrithiCycleMembershipsTable.insert {
                it[krithiId] = k.getValue("d1"); it[tagId] = tuneTag; it[axisValue] = "Galopede"; it[role] = CycleMemberRole.CORE
            }
            KrithiTagsTable.insert {
                it[krithiId] = k.getValue("d1"); it[tagId] = tuneTag; it[sourceInfo] = "reviewed_cycle"
            }
            // An unreviewed tune never counts.
            KrithiCycleMembershipsTable.insert {
                it[krithiId] = k.getValue("d6"); it[tagId] = tuneTag; it[axisValue] = "Unreviewed Air"; it[role] = CycleMemberRole.CORE
            }
            KrithiTagsTable.insert {
                it[krithiId] = k.getValue("d6"); it[tagId] = tuneTag; it[sourceInfo] = "manual"
            }
        }
    }

    private suspend fun ApplicationTestBuilder.facets(query: String = ""): JsonObject =
        parse(client.get("${RasikaCatalogueContract.FACETS_PATH}$query").bodyAsText())

    private fun JsonObject.facet(key: String): JsonObject? =
        this["facets"]!!.jsonArray.map { it.jsonObject }.firstOrNull { it["key"]!!.jsonPrimitive.content == key }

    private fun JsonObject.values(key: String): List<String> =
        facet(key)?.get("values")?.jsonArray?.map { it.jsonObject["value"]!!.jsonPrimitive.content }.orEmpty()

    private fun JsonObject.counts(key: String): Map<String, Long> =
        facet(key)!!["values"]!!.jsonArray.associate {
            it.jsonObject["value"]!!.jsonPrimitive.content to it.jsonObject["count"]!!.jsonPrimitive.content.toLong()
        }

    private suspend fun ApplicationTestBuilder.total(query: String): Long =
        parse(client.get("${RasikaCatalogueContract.KRITHIS_PATH}$query").bodyAsText())["total"]!!.jsonPrimitive.content.toLong()

    @Test
    fun `whole catalogue lists each populated facet in registry order without counts`() = testApplication {
        mount()
        seed()
        val body = facets()
        val keys = body["facets"]!!.jsonArray.map { it.jsonObject["key"]!!.jsonPrimitive.content }
        assertEquals(listOf("vibhakti", "collection", "tune", "mandalam", "element"), keys)
        assertEquals(listOf("DVITIYA", "TRITIYA"), body.values("vibhakti"))
        assertEquals(listOf("alpha-cycle", "tyagaraja-cycle", "nottusvara-sahitya"), body.values("collection"))
        assertEquals(listOf("Galopede"), body.values("tune"))
        // Enum declaration order; the draft-only PANDYA temple is ineligible and never appears.
        assertEquals(listOf("CHOLA", "TONDAI"), body.values("mandalam"))
        assertEquals(listOf("APPU"), body.values("element"))
        assertTrue(body["facets"]!!.jsonArray.all { f ->
            f.jsonObject["values"]!!.jsonArray.all { !it.jsonObject.containsKey("count") }
        })
        assertEquals("Cycle alpha-cycle", body.facet("collection")!!["values"]!!.jsonArray[0].jsonObject["label"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a composer without data sees only the facets it has`() = testApplication {
        mount()
        seed()
        val tyagarajaOnly = facets("?composerId=${tyagaraja}")
        assertEquals(listOf("collection"), tyagarajaOnly["facets"]!!.jsonArray.map { it.jsonObject["key"]!!.jsonPrimitive.content })
        assertEquals(listOf("tyagaraja-cycle"), tyagarajaOnly.values("collection"))
        assertEquals(5, facets("?composerId=$dikshitar")["facets"]!!.jsonArray.size)
        assertEquals(JsonArray(emptyList()), facets("?query=zzzznomatch")["facets"])
    }

    @Test
    fun `counts drill down and agree with the matching search totals`() = testApplication {
        mount()
        seed()
        val all = facets("?counts=true")
        assertEquals(mapOf("DVITIYA" to 2L, "TRITIYA" to 1L), all.counts("vibhakti"))
        assertEquals(mapOf("alpha-cycle" to 2L, "nottusvara-sahitya" to 1L, "tyagaraja-cycle" to 1L), all.counts("collection"))
        assertEquals(mapOf("CHOLA" to 2L, "TONDAI" to 1L), all.counts("mandalam"))

        // Own filter ignored for the facet's own counts, other facets' filters applied.
        val drill = facets("?counts=true&vibhakti=TRITIYA&mandalam=CHOLA")
        assertEquals(mapOf("DVITIYA" to 1L, "TRITIYA" to 1L), drill.counts("vibhakti")) // d5 is Tondai
        assertEquals(mapOf("CHOLA" to 1L, "TONDAI" to 0L), drill.counts("mandalam"))
        for ((value, count) in drill.counts("mandalam")) {
            assertEquals(count, total("?vibhakti=TRITIYA&mandalam=$value"))
        }
        for ((value, count) in drill.counts("vibhakti")) {
            assertEquals(count, total("?vibhakti=$value&mandalam=CHOLA"))
        }
        // Listed values ignore the facet filters, so siblings do not disappear.
        assertEquals(listOf("DVITIYA", "TRITIYA"), drill.values("vibhakti"))
    }

    @Test
    fun `filters combine with AND and a valid empty combination is 200 with total zero`() = testApplication {
        mount()
        seed()
        assertEquals(1, total("?vibhakti=TRITIYA&composerId=$dikshitar"))
        assertEquals(0, total("?vibhakti=TRITIYA&composerId=$tyagaraja"))
        assertEquals(2, total("?collection=alpha-cycle&composerId=$dikshitar")) // draft d3 excluded
        assertEquals(1, total("?tune=Galopede"))
        assertEquals(2, total("?mandalam=CHOLA"))
        assertEquals(1, total("?mandalam=TONDAI")) // child shrine uses its own column
        assertEquals(1, total("?element=APPU&vibhakti=DVITIYA"))
        assertEquals(0, total("?element=APPU&mandalam=TONDAI"))
        assertEquals(2, total("?query=facet%20d&vibhakti=DVITIYA"))
        // No facets: unchanged page of all published kritis (d1 d2 d5 d6 t1).
        assertEquals(5, total(""))
    }

    @Test
    fun `invalid facet values are 400 and V1 rejects the parameters`() = testApplication {
        mount()
        seed()
        val bad = listOf(
            "vibhakti=", "vibhakti=%20%20", "vibhakti=tritiya", "vibhakti=SARVA_VIBHAKTI",
            "mandalam=ATLANTIS", "element=EARTH", "collection=no-such-cycle", "tune=Unknown", "tune=" + "x".repeat(65),
            "collection=unreviewed-x", "vibhakti=TRITIYA&vibhakti=DVITIYA", "bogus=1",
        )
        for (q in bad) {
            assertEquals(HttpStatusCode.BadRequest, client.get("${RasikaCatalogueContract.KRITHIS_PATH}?$q").status, q)
            assertEquals(HttpStatusCode.BadRequest, client.get("${RasikaCatalogueContract.FACETS_PATH}?$q").status, q)
        }
        assertEquals(HttpStatusCode.BadRequest, client.get("${RasikaCatalogueContract.FACETS_PATH}?counts=yes").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("${RasikaCatalogueContract.FACETS_PATH}?page=0").status)
        val message = client.get("${RasikaCatalogueContract.KRITHIS_PATH}?vibhakti=NOPE").bodyAsText()
        assertTrue("vibhakti" in message && "TRITIYA" in message)
        // The unreviewed tune never became a listed value.
        assertEquals(HttpStatusCode.BadRequest, client.get("${RasikaCatalogueContract.FACETS_PATH}?tune=Unreviewed%20Air").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/v1/catalogue/krithis?vibhakti=TRITIYA").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/v1/catalogue/krithis?collection=alpha-cycle").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/v1/catalogue/facets").status)
    }

    @Test
    fun `the tune cycle slug stays pinned to the reviewed seed`() {
        // The tune facet is bound to this slug; if the seed renames the cycle the facet would vanish.
        var dir: java.io.File? = java.io.File(System.getProperty("user.dir"))
        while (dir != null && !java.io.File(dir, "database/migrations").isDirectory) dir = dir.parentFile
        val seed = java.io.File(requireNotNull(dir) { "database/migrations not found" }, "database/migrations")
            .listFiles { f -> f.name.startsWith("R__seed_09") }!!.single().readText()
        assertTrue("'nottusvara-sahitya'" in seed, "R__seed_09 no longer defines the tune cycle slug")
    }

    @Test
    fun `reader chips cover vibhakti tune and region only where stored and V1 gains no field`() = testApplication {
        mount()
        seed()
        val d1 = parse(client.get("${RasikaCatalogueContract.KRITHIS_PATH}/${k.getValue("d1")}").bodyAsText())
        val chips = d1["facetChips"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("vibhakti", "tune", "mandalam"), chips.map { it["key"]!!.jsonPrimitive.content })
        assertEquals(listOf("TRITIYA", "Galopede", "CHOLA"), chips.map { it["value"]!!.jsonPrimitive.content })
        assertNull(chips[0]["label"])
        assertEquals("Galopede", chips[1]["label"]!!.jsonPrimitive.content)
        val d5 = parse(client.get("${RasikaCatalogueContract.KRITHIS_PATH}/${k.getValue("d5")}").bodyAsText())
        assertEquals(listOf("vibhakti", "mandalam"), d5["facetChips"]!!.jsonArray.map { it.jsonObject["key"]!!.jsonPrimitive.content })
        assertEquals("TONDAI", d5["facetChips"]!!.jsonArray.last().jsonObject["value"]!!.jsonPrimitive.content)
        // No stored value, no chips, no field.
        assertFalse(parse(client.get("${RasikaCatalogueContract.KRITHIS_PATH}/${k.getValue("t1")}").bodyAsText()).containsKey("facetChips"))
        assertFalse(parse(client.get("/v1/catalogue/krithis/${k.getValue("d1")}").bodyAsText()).containsKey("facetChips"))
        // Every chip is accepted by the filter.
        chips.forEach {
            val key = it["key"]!!.jsonPrimitive.content
            val value = java.net.URLEncoder.encode(it["value"]!!.jsonPrimitive.content, "UTF-8")
            assertTrue(total("?$key=$value") >= 1)
        }
    }
}
