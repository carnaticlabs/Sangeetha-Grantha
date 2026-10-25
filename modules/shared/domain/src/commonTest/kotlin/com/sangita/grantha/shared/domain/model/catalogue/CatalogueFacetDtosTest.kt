package com.sangita.grantha.shared.domain.model.catalogue

import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class CatalogueFacetDtosTest {
    private val json = Json { encodeDefaults = true; explicitNulls = false }

    private val id = Uuid.parse("01930000-0000-7000-8000-000000000001")

    private fun reader() = CatalogueKrithiReaderDto(
        id = id,
        title = "Test",
        composer = CatalogueComposerRefDto(id, "Test Composer"),
        ragas = emptyList(),
        originalLanguage = LanguageCodeDto.TE,
    )

    @Test
    fun keysSerialiseAsTheQueryParameterNames() {
        val list = CatalogueFacetListDto(CatalogueFacetKeyDto.entries.map { CatalogueFacetDto(it, emptyList()) })
        val text = json.encodeToString(list)
        CatalogueFacetKeyDto.entries.forEach { assertEquals(it, CatalogueFacetKeyDto.fromWireKey(it.wireKey)) }
        assertEquals(listOf("vibhakti", "collection", "tune", "mandalam", "element"), CatalogueFacetKeyDto.entries.map { it.wireKey })
        listOf("vibhakti", "collection", "tune", "mandalam", "element").forEach { assert("\"key\":\"$it\"" in text) }
        assertEquals(list, json.decodeFromString<CatalogueFacetListDto>(text))
        assertNull(CatalogueFacetKeyDto.fromWireKey("VIBHAKTI"))
    }

    @Test
    fun absentLabelAndCountAreNotEncoded() {
        val text = json.encodeToString(CatalogueFacetValueDto(value = "TRITIYA"))
        assertEquals("""{"value":"TRITIYA"}""", text)
        assertEquals(
            """{"value":"a","label":"A","count":0}""",
            json.encodeToString(CatalogueFacetValueDto("a", "A", 0)),
        )
    }

    @Test
    fun emptyFacetChipsAreNotEncodedOnTheReader() {
        val reader = reader()
        assertFalse("facetChips" in json.encodeToString(reader))
        val withChips = reader.copy(facetChips = listOf(CatalogueFacetChipDto(CatalogueFacetKeyDto.VIBHAKTI, "TRITIYA")))
        val text = json.encodeToString(withChips)
        assertEquals(withChips, json.decodeFromString<CatalogueKrithiReaderDto>(text))
        assertEquals(emptyList(), json.decodeFromString<CatalogueKrithiReaderDto>(json.encodeToString(reader)).facetChips)
    }
}
