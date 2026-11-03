package com.sangita.grantha.shared.presentation.preview

import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.SemanticSearchResultItem
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.collections.CollectionCopy
import com.sangita.grantha.shared.presentation.reader.SectionLabels
import com.sangita.grantha.shared.presentation.reader.TamilNotation
import com.sangita.grantha.shared.presentation.search.KrithiSearchMode
import com.sangita.grantha.shared.presentation.search.SearchPresenter
import com.sangita.grantha.shared.presentation.search.SearchUiState
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * TRACK-158 C1. The admin web's Mobile Preview re-implements a few presentation rules in TypeScript. Both sides read
 * the same golden files, so a change to a Kotlin rule fails here and a change to the TypeScript port fails in
 * Vitest (`readerLogic.test.ts`, `searchLogic.test.ts`) until the fixtures and both sides agree again.
 */
class PreviewGoldenFixturesTest {
    private val fixtures = File("../../frontend/sangita-admin-web/src/components/mobile-preview/fixtures")

    private fun load(name: String): JsonObject {
        val file = File(fixtures, name)
        check(file.exists()) { "Golden fixture missing: ${file.absolutePath}" }
        return Json.parseToJsonElement(file.readText()).jsonObject
    }

    private fun JsonElement.stringOrNull(): String? = if (this is JsonNull) null else jsonPrimitive.contentOrNull

    @Test
    fun `tamil digit notation matches the golden file`() {
        load("readerRules.golden.json").getValue("tamil").jsonArray.forEach { case ->
            val c = case.jsonObject
            assertEquals(c.getValue("out").jsonPrimitive.content, TamilNotation.format(c.getValue("in").jsonPrimitive.content), c.getValue("name").jsonPrimitive.content)
        }
    }

    @Test
    fun `section headings captions and preformatted sections match the golden file`() {
        val g = load("readerRules.golden.json")
        g.getValue("headings").jsonArray.forEach { case ->
            val c = case.jsonObject
            assertEquals(c.getValue("out").jsonPrimitive.content, SectionLabels.heading(c.getValue("type").jsonPrimitive.content, c.getValue("label").stringOrNull()))
        }
        g.getValue("captions").jsonArray.forEach { case ->
            val c = case.jsonObject
            assertEquals(
                c.getValue("out").stringOrNull(),
                SectionLabels.caption(c.getValue("type").jsonPrimitive.content, MusicalFormDto.valueOf(c.getValue("form").jsonPrimitive.content)),
            )
        }
        val preformatted = g.getValue("preformatted").jsonObject
        preformatted.getValue("yes").jsonArray.forEach { assertEquals(true, SectionLabels.isPreformattedSwara(it.jsonPrimitive.content)) }
        preformatted.getValue("no").jsonArray.forEach { assertEquals(false, SectionLabels.isPreformattedSwara(it.jsonPrimitive.content)) }
    }

    @Test
    fun `vibhakti labels match the golden file`() {
        load("readerRules.golden.json").getValue("vibhakti").jsonArray.forEach { case ->
            val c = case.jsonObject
            assertEquals(c.getValue("out").jsonPrimitive.content, CollectionCopy.vibhakti(c.getValue("in").jsonPrimitive.content))
        }
    }

    private fun item(id: String) = SemanticSearchResultItem(
        krithiId = Uuid.parse(id), title = id, composerName = "c", documentKind = "COMPOSITION_OVERVIEW", matchedContent = "", similarityScore = 0.5,
    )

    // Fixture ids are short labels; map them to stable UUIDs so the real SearchUiState can be used.
    private fun uuidFor(label: String): Uuid = Uuid.parse("00000000-0000-0000-0000-" + label.padStart(12, '0').map { c -> if (c.isLetterOrDigit()) "%x".format(c.code % 16) else "0" }.joinToString(""))

    @Test
    fun `exact title ordering matches the golden file`() {
        val g = load("searchRules.golden.json")
        for (key in listOf("exactTitle", "noExactTitle")) {
            val c = g.getValue(key).jsonObject
            val ids = c.getValue("ids").jsonArray.map { it.jsonPrimitive.content }
            val byUuid = ids.associateBy { uuidFor(it) }
            check(byUuid.size == ids.size) { "label to uuid mapping must be one to one" }
            val exact = c.getValue("exact").stringOrNull()
            val state = SearchUiState(
                discoveryItems = ids.map { item(uuidFor(it).toString()) },
                exactTitleMatchId = exact?.let { uuidFor(it) },
            )
            val first = state.exactTitleItem?.krithiId?.let { byUuid[it] }
            assertEquals(c.getValue("expectFirst").stringOrNull(), first, key)
            assertEquals(c.getValue("expectRest").jsonArray.map { it.jsonPrimitive.content }, state.remainingDiscoveryItems.map { byUuid.getValue(it.krithiId) }, key)
        }
    }

    @Test
    fun `paging constants and mode labels match the golden file`() {
        val g = load("searchRules.golden.json")
        val paging = g.getValue("paging").jsonObject
        assertEquals(paging.getValue("pageSize").jsonPrimitive.content.toInt(), SearchPresenter.PAGE_SIZE)
        assertEquals(paging.getValue("maxPages").jsonPrimitive.content.toInt(), SearchPresenter.MAX_PAGES)
        assertEquals(paging.getValue("discoveryLimit").jsonPrimitive.content.toInt(), SearchPresenter.DISCOVERY_LIMIT)
        val labels = g.getValue("modeLabels").jsonObject
        assertEquals(labels.getValue("Hybrid").jsonPrimitive.content, RasikaCopy.MODE_HYBRID)
        assertEquals(labels.getValue("Semantic").jsonPrimitive.content, RasikaCopy.MODE_SEMANTIC)
        assertEquals(labels.getValue("Lexical").jsonPrimitive.content, RasikaCopy.MODE_LEXICAL)
        assertNull(KrithiSearchMode.entries.firstOrNull { it.name !in labels.keys })
    }
}
