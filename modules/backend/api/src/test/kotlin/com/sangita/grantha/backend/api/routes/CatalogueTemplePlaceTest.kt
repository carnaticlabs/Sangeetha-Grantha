package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.plugins.configureSerialization
import com.sangita.grantha.backend.api.plugins.configureStatusPages
import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * TRACK-154 Plan proof 6: the V2 temple detail exposes `place` only for a CONFIRMED place. Synthetic
 * place ids only; no Google content.
 */
class CatalogueTemplePlaceTest : IntegrationTestBase() {
    private val service = CatalogueService(SangitaDalImpl())

    private fun ApplicationTestBuilder.mount() = application {
        configureStatusPages()
        configureSerialization()
        routing { rasikaCatalogueRoutes(service) }
    }

    private suspend fun temple(
        name: String,
        kind: String = "COMPLEX",
        parent: UUID? = null,
        placeId: String? = null,
        state: String? = null,
    ): UUID = DatabaseFactory.dbQuery {
        var id: UUID? = null
        exec(
            "INSERT INTO temples (name, name_normalized, city, parent_temple_id, place_kind, google_place_id, " +
                "place_review_state, place_reviewed_by, place_reviewed_at) VALUES ('$name', '${name.lowercase()}', " +
                "'Testpur', ${parent?.let { "'$it'" } ?: "NULL"}, '$kind', ${placeId?.let { "'$it'" } ?: "NULL"}, " +
                "${state?.let { "'$it'" } ?: "NULL"}, ${if (state == null) "NULL" else "'curator@test'"}, " +
                "${if (state == null) "NULL" else "now()"}) RETURNING id",
            explicitStatementType = StatementType.SELECT,
        ) { rs -> if (rs.next()) id = UUID.fromString(rs.getString("id")) }
        assertNotNull(id)
        // A published overview makes the temple eligible for the public detail.
        exec(
            "INSERT INTO search_documents (temple_id, document_kind, original_content, indexed_content, " +
                "content_hash, is_published, source_chunk_index) VALUES ('$id', 'KSHETRA_OVERVIEW', 'o', 'o', 'h-$id', true, 0)",
        )
        id
    }

    private suspend fun ApplicationTestBuilder.detail(id: UUID): JsonObject {
        val response = client.get("${RasikaCatalogueContract.TEMPLES_PATH}/$id")
        assertEquals(HttpStatusCode.OK, response.status)
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    private fun JsonObject.placeOrNull(): JsonObject? = this["place"]?.takeIf { it.toString() != "null" }?.jsonObject

    @Test
    fun `confirmed temple exposes only its place id and TEMPLE precision`() = testApplication {
        mount()
        val id = temple("Confirmed Temple", placeId = "ChIJ-synthetic-1", state = "CONFIRMED")
        val body = detail(id)
        val place = assertNotNull(body.placeOrNull())
        assertEquals(setOf("googlePlaceId", "precision"), place.keys)
        assertEquals("ChIJ-synthetic-1", place["googlePlaceId"]!!.jsonPrimitive.content)
        assertEquals("TEMPLE", place["precision"]!!.jsonPrimitive.content)
        listOf("placeReviewState", "placeReviewedBy", "latitude", "longitude", "notes").forEach {
            assertFalse(body.containsKey(it), "$it must not be on the detail DTO")
        }
    }

    @Test
    fun `locality only, unresolved and unreviewed temples expose no place`() = testApplication {
        mount()
        val locality = temple("Locality Temple", placeId = "ChIJ-synthetic-town", state = "LOCALITY_ONLY")
        val unresolved = temple("Unresolved Temple", state = "UNRESOLVED")
        val unreviewed = temple("Unreviewed Temple")
        listOf(locality, unresolved, unreviewed).forEach { assertNull(detail(it).placeOrNull()) }
    }

    @Test
    fun `sannidhi and mandapam inherit a confirmed parent as PARENT_COMPLEX`() = testApplication {
        mount()
        val parent = temple("Parent Complex", placeId = "ChIJ-synthetic-parent", state = "CONFIRMED")
        listOf("SANNIDHI", "MANDAPAM").forEach { kind ->
            val child = temple("Child $kind", kind = kind, parent = parent)
            val place = assertNotNull(detail(child).placeOrNull())
            assertEquals("ChIJ-synthetic-parent", place["googlePlaceId"]!!.jsonPrimitive.content)
            assertEquals("PARENT_COMPLEX", place["precision"]!!.jsonPrimitive.content)
        }
    }

    @Test
    fun `sub-shrines of unconfirmed parents have no place`() = testApplication {
        mount()
        listOf("LOCALITY_ONLY" to "ChIJ-synthetic-town", "UNRESOLVED" to null, null to null).forEachIndexed { i, (state, pid) ->
            val parent = temple("Unconfirmed Parent $i", placeId = pid, state = state)
            val child = temple("Unconfirmed Child $i", kind = "SANNIDHI", parent = parent)
            assertNull(detail(child).placeOrNull(), "parent state $state")
        }
    }

    @Test
    fun `the database refuses a sub-shrine with its own place, so the mapper never sees one`() = testApplication {
        mount()
        val parent = temple("Plain Parent")
        assertFails {
            temple("Rogue Child", kind = "SANNIDHI", parent = parent, placeId = "ChIJ-synthetic-own", state = "CONFIRMED")
        }
    }
}
