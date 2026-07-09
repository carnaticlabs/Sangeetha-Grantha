package com.sangita.grantha.backend.api.services

import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionMethod
import com.sangita.grantha.shared.domain.model.import.CanonicalMusicalForm
import com.sangita.grantha.shared.domain.model.import.CanonicalSectionType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Kotlin half of the W2 cross-language contract (TRACK-113).
 *
 * The Python worker's integration suite proves the golden fixture is exactly
 * what `CanonicalExtraction.to_json_dict()` emits and what lands in the
 * `extraction_queue.result_payload` jsonb column. This test proves the same
 * document decodes STRICTLY (no ignored keys) into [CanonicalExtractionDto] —
 * so any field either side adds or renames breaks one of the two suites.
 *
 * Fixture: shared/domain/model/import/fixtures/canonical-extraction-golden.json
 */
class CanonicalExtractionGoldenFixtureTest {

    /** Unknown keys are a contract violation, not noise — decode strictly. */
    private val strictJson = Json { ignoreUnknownKeys = false }

    private fun goldenFixtureText(): String {
        var dir = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (true) {
            val candidate = dir.resolve("shared/domain/model/import/fixtures/canonical-extraction-golden.json")
            if (candidate.toFile().exists()) return candidate.toFile().readText()
            dir = dir.parent ?: error("golden fixture not found walking up from user.dir")
        }
    }

    @Test
    fun `golden fixture decodes strictly into CanonicalExtractionDto`() {
        val dto = strictJson.decodeFromString<CanonicalExtractionDto>(goldenFixtureText())

        assertEquals("vAtApi gaNapatim bhajE", dto.title)
        assertEquals("muttusvAmi dIkSitar", dto.composer)
        assertEquals(CanonicalMusicalForm.KRITHI, dto.musicalForm)
        assertEquals(listOf("hamsadhvani"), dto.ragas.map { it.name })
        assertEquals("Adi", dto.tala)
        assertEquals(
            listOf(
                CanonicalSectionType.PALLAVI,
                CanonicalSectionType.ANUPALLAVI,
                CanonicalSectionType.MADHYAMA_KALA,
                CanonicalSectionType.CHARANAM,
            ),
            dto.sections.map { it.type },
        )
        assertEquals(CanonicalExtractionMethod.HTML_JSOUP, dto.extractionMethod)
        assertEquals(2, dto.sourceTier)
        assertNotNull(dto.extractionTimestamp)
    }

    @Test
    fun `snake_case normalized keys map via SerialName`() {
        val dto = strictJson.decodeFromString<CanonicalExtractionDto>(goldenFixtureText())

        assertEquals("vatapi ganapatim bhaje", dto.titleNormalized)
        assertEquals("muttusvami dikshitar", dto.composerNormalized)
        assertEquals("hamsadhvani", dto.ragaNormalized)
        assertEquals("adi", dto.talaNormalized)
    }

    @Test
    fun `lyric variants align to the canonical section skeleton`() {
        val dto = strictJson.decodeFromString<CanonicalExtractionDto>(goldenFixtureText())

        assertEquals(listOf("sa" to "devanagari", "en" to "latin"), dto.lyricVariants.map { it.language to it.script })
        val sectionOrders = dto.sections.map { it.order }.toSet()
        dto.lyricVariants.forEach { variant ->
            assertEquals(sectionOrders, variant.sections.map { it.sectionOrder }.toSet())
            assertTrue(variant.sections.all { it.text.isNotBlank() })
        }
    }

    @Test
    fun `optional enrichment and identity candidates decode fully`() {
        val dto = strictJson.decodeFromString<CanonicalExtractionDto>(goldenFixtureText())

        val enrichment = dto.metadataEnrichment
        assertNotNull(enrichment)
        assertEquals("google-genai", enrichment!!.provider)
        assertTrue(enrichment.applied)
        assertEquals(listOf("deity", "temple"), enrichment.fieldsUpdated)

        val candidates = dto.identityCandidates
        assertNotNull(candidates)
        assertEquals("Muthuswami Dikshitar", candidates!!.composers.single().name)
        assertEquals("HIGH", candidates.composers.single().confidence)
        assertEquals("Hamsadhwani", candidates.ragas.single().name)
    }

    @Test
    fun `kotlin round-trip re-decodes to an equal object`() {
        val original = strictJson.decodeFromString<CanonicalExtractionDto>(goldenFixtureText())
        val reencoded = strictJson.encodeToString(CanonicalExtractionDto.serializer(), original)
        val roundTripped = strictJson.decodeFromString<CanonicalExtractionDto>(reencoded)
        assertEquals(original, roundTripped)
    }
}
