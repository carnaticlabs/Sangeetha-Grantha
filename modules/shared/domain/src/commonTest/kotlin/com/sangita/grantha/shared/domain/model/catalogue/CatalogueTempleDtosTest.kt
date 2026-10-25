package com.sangita.grantha.shared.domain.model.catalogue

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class CatalogueTempleDtosTest {
    private val server = Json { encodeDefaults = true; explicitNulls = false }
    private val id = Uuid.parse("01930000-0000-7000-8000-000000000001")

    private fun summary() = CatalogueTempleSummaryDto(
        id = id,
        name = "Tiruvarur Tyagarajaswami Temple",
        city = "Tiruvarur",
        placeKind = CataloguePlaceKindDto.COMPLEX,
        mandalam = CatalogueMandalamDto.CHOLA,
        element = CatalogueTempleElementDto.PRITHVI,
        publishedKrithiCount = 127,
        childCount = 10,
    )

    @Test
    fun enumsSerialiseAsDatabaseCodes() {
        assertEquals("\"APPU\"", server.encodeToString(CatalogueTempleElementDto.serializer(), CatalogueTempleElementDto.APPU))
        assertEquals("\"MANDAPAM\"", server.encodeToString(CataloguePlaceKindDto.serializer(), CataloguePlaceKindDto.MANDAPAM))
        assertEquals("\"UTTARA\"", server.encodeToString(CatalogueMandalamDto.serializer(), CatalogueMandalamDto.UTTARA))
        assertEquals("\"TANDAVA\"", server.encodeToString(CatalogueDeityPostureDto.serializer(), CatalogueDeityPostureDto.TANDAVA))
        assertEquals(
            "\"PARENT_COMPLEX\"",
            server.encodeToString(
                CatalogueTemplePlacePrecisionDto.serializer(),
                CatalogueTemplePlacePrecisionDto.PARENT_COMPLEX,
            ),
        )
    }

    @Test
    fun summaryRoundTripsAndOmitsNulls() {
        val text = server.encodeToString(CatalogueTempleSummaryDto.serializer(), summary())
        val obj = server.parseToJsonElement(text).jsonObject
        assertFalse("state" in obj)
        assertEquals("CHOLA", obj.getValue("mandalam").jsonPrimitive.content)
        assertEquals("false", obj.getValue("recordedGap").jsonPrimitive.content)
        assertEquals(summary(), server.decodeFromString(CatalogueTempleSummaryDto.serializer(), text))
    }

    @Test
    fun detailOmitsPlaceWhenNullAndCarriesNoGoogleFieldsOtherThanPlaceId() {
        val detail = CatalogueTempleDetailDto(
            id = id,
            name = "Kanyakumari Bhagavati Amman Temple",
            placeKind = CataloguePlaceKindDto.COMPLEX,
            recordedGap = true,
            overview = "Recorded gap.",
        )
        val obj: JsonObject = server.parseToJsonElement(
            server.encodeToString(CatalogueTempleDetailDto.serializer(), detail),
        ).jsonObject
        assertFalse("place" in obj)
        assertTrue(obj.getValue("children").toString() == "[]")
        assertTrue(obj.getValue("krithis").toString() == "[]")
        val forbidden = setOf("latitude", "longitude", "address", "googleName", "rating", "photos")
        assertTrue(obj.keys.none { it in forbidden })
        assertNull(server.decodeFromString(CatalogueTempleDetailDto.serializer(), obj.toString()).place)
    }

    @Test
    fun placeCarriesOnlyIdAndPrecision() {
        val place = CatalogueTemplePlaceDto("ChIJ-test", CatalogueTemplePlacePrecisionDto.PARENT_COMPLEX)
        val obj = server.parseToJsonElement(
            server.encodeToString(CatalogueTemplePlaceDto.serializer(), place),
        ).jsonObject
        assertEquals(setOf("googlePlaceId", "precision"), obj.keys)
        val detail = CatalogueTempleDetailDto(
            id = id,
            name = "Sannidhi",
            placeKind = CataloguePlaceKindDto.SANNIDHI,
            place = place,
        )
        val decoded = server.decodeFromString(
            CatalogueTempleDetailDto.serializer(),
            server.encodeToString(CatalogueTempleDetailDto.serializer(), detail),
        )
        assertEquals(place, decoded.place)
    }

    @Test
    fun listAndContractPath() {
        assertEquals("/v2/catalogue/temples", RasikaCatalogueContract.TEMPLES_PATH)
        val list = CatalogueTempleListDto(listOf(summary()))
        val text = server.encodeToString(CatalogueTempleListDto.serializer(), list)
        assertEquals(list, server.decodeFromString(CatalogueTempleListDto.serializer(), text))
    }
}
