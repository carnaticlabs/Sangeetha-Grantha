package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.repositories.MacroAnchorKind
import com.sangita.grantha.backend.dal.repositories.MacroCandidate
import java.io.File
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TRACK-157: the macro relevance floor held against the scores recorded in
 * `tools/krithi-extract-enrich-worker/evals/macro_scores.json` (one authorised run, 2026-11-02).
 * Offline: no network, no database. A change to the constants or to the recording that erodes a margin
 * fails here; re-record with `evaluate_retrieval.py --dump-macro-scores` after any profile or overview change.
 */
class MacroFloorsMarginTest {
    private val floor = RasikaSearchService.GEMINI_EMBEDDING_2_768.hybrid
    private val semanticFloor = RasikaSearchService.GEMINI_EMBEDDING_2_768.semantic

    /** Macro probes whose best card is correct (labelled in `retrieval_benchmarks.json`). */
    private val positives = listOf("EVAL-31", "SS-146-11", "TY-147-11")

    /** Queries that name a kriti outright: a card above the kriti would be noise. */
    private val exactTitle = listOf("EVAL-01", "EVAL-04", "EVAL-09", "EVAL-13", "EVAL-17", "EVAL-23")

    /** Probes whose recorded top card looks wrong (provisional reading, see the TRACK-157 review table). */
    private val wrongLooking = listOf("EVAL-08", "TY-147-04", "TY-147-10", "EVAL-29")

    private class Anchor(val similarity: Double, val lexical: Double)

    private val probes: JsonObject by lazy {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "tools/krithi-extract-enrich-worker/evals/macro_scores.json").isFile) dir = dir.parentFile
        val file = assertNotNull(dir, "macro_scores.json not found above ${File("").absolutePath}")
            .resolve("tools/krithi-extract-enrich-worker/evals/macro_scores.json")
        val doc = Json.parseToJsonElement(file.readText()).jsonObject
        val profile = doc.getValue("profile").jsonObject
        assertEquals("gemini-embedding-2", profile.getValue("model_name").jsonPrimitive.content)
        assertEquals(768, profile.getValue("dimensions").jsonPrimitive.content.toInt())
        doc.getValue("probes").jsonObject
    }

    /** Best similarity per anchor (and the best lexical over its chunks), highest first. */
    private fun anchors(probe: String): List<Anchor> {
        val rows = probes.getValue(probe).jsonObject.getValue("candidates").jsonArray.map { it.jsonObject }
        return rows.groupBy { it.getValue("anchor_id").jsonPrimitive.content }.values
            .map { chunks ->
                Anchor(
                    similarity = chunks.maxOf { it.getValue("similarity").jsonPrimitive.double },
                    lexical = chunks.maxOf { it.getValue("lexical").jsonPrimitive.double },
                )
            }
            .sortedByDescending { it.similarity }
    }

    private fun gap(a: List<Anchor>) = a[0].similarity - a[3].similarity

    private fun candidates(probe: String): List<MacroCandidate> {
        val a = anchors(probe)
        val reference = a[3].similarity
        return a.map {
            MacroCandidate(
                kind = MacroAnchorKind.COLLECTION, anchorId = UUID.randomUUID(), content = "",
                similarity = it.similarity, lexicalScore = it.lexical, rrfScore = null,
                similarityGap = it.similarity - reference,
            )
        }
    }

    private fun cleared(probe: String, f: MacroFloor) = candidates(probe).filter { RasikaSearchService.clearsFloor(it, f) }

    @Test
    fun `the recording covers every probe the floor is judged on`() {
        (positives + exactTitle + wrongLooking).forEach { assertTrue(it in probes, "$it missing from macro_scores.json") }
        assertTrue(probes.getValue("EVAL-31").jsonObject.getValue("candidates").jsonArray.size >= 100)
    }

    @Test
    fun `every labelled macro hit clears the floor with a margin on similarity and gap`() {
        for (id in positives) {
            val a = anchors(id)
            assertTrue(cleared(id, floor).isNotEmpty(), "$id must show a card")
            assertTrue(cleared(id, semanticFloor).isNotEmpty(), "$id must show a card in semantic mode")
            assertTrue(a[0].similarity - floor.minSimilarity >= 0.02, "$id similarity ${a[0].similarity} is within 0.02 of the floor")
            assertTrue(gap(a) - floor.minGap >= 0.01, "$id gap ${gap(a)} is within 0.01 of the floor")
        }
    }

    @Test
    fun `exact title probes show no card and sit well below the similarity floor`() {
        for (id in exactTitle) {
            assertTrue(cleared(id, floor).isEmpty(), "$id must show no card")
            assertTrue(cleared(id, semanticFloor).isEmpty(), "$id must show no card in semantic mode")
        }
        val peak = exactTitle.maxOf { anchors(it)[0].similarity }
        assertTrue(floor.minSimilarity - peak >= 0.02, "exact-title peak $peak is within 0.02 of the floor")
        assertTrue(exactTitle.maxOf { id -> anchors(id).maxOf { it.lexical } } <= floor.minLexical - 0.15)
    }

    @Test
    fun `cards that look wrong stay out and the gap condition is what excludes them`() {
        for (id in wrongLooking) {
            assertTrue(cleared(id, floor).isEmpty(), "$id must show no card")
            assertTrue(anchors(id)[0].similarity >= 0.81, "$id is only excluded by its gap if it clears the similarity floor")
        }
        val widest = wrongLooking.maxOf { gap(anchors(it)) }
        assertTrue(floor.minGap - widest >= 0.015, "wrong-looking gap $widest is within 0.015 of the gap floor")
    }

    @Test
    fun `a candidate needs both similarity and gap, or near-verbatim lexical containment`() {
        fun c(sim: Double?, gap: Double?, lex: Double?) = MacroCandidate(
            MacroAnchorKind.KSHETRA, UUID.randomUUID(), "", sim, lex, null, gap,
        )
        assertTrue(RasikaSearchService.clearsFloor(c(0.84, 0.04, null), floor))
        assertFalse(RasikaSearchService.clearsFloor(c(0.84, 0.01, null), floor))
        assertFalse(RasikaSearchService.clearsFloor(c(0.80, 0.06, null), floor))
        assertTrue(RasikaSearchService.clearsFloor(c(null, null, 0.85), floor))
        assertFalse(RasikaSearchService.clearsFloor(c(null, null, 0.85), semanticFloor))
        assertTrue(RasikaSearchService.clearsFloor(c(null, null, 0.85), RasikaSearchService.LEXICAL_ONLY_FLOOR))
    }

    @Test
    fun `an unknown embedding profile has no floor`() {
        assertEquals(null, RasikaSearchService.DEFAULT_FLOORS.forProfile(ProfileKey("some-future-model", 768)))
        assertEquals(null, RasikaSearchService.DEFAULT_FLOORS.forProfile(ProfileKey("gemini-embedding-2", 1536)))
        assertNotNull(RasikaSearchService.DEFAULT_FLOORS.forProfile(ProfileKey("gemini-embedding-2", 768)))
    }
}
