package com.sangita.grantha.backend.api.catalogue

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import io.ktor.http.parametersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class CatalogueParametersTest {
    @Test
    fun `defaults page and pageSize when omitted`() {
        val parsed = assertIs<CatalogueParseResult.Ok<CatalogueListQuery>>(
            CatalogueParameters.parseKrithiSearch(parametersOf()),
        )
        assertNull(parsed.value.query)
        assertEquals(CatalogueContract.DEFAULT_PAGE, parsed.value.page)
        assertEquals(CatalogueContract.DEFAULT_PAGE_SIZE, parsed.value.pageSize)
    }

    @Test
    fun `unknown query parameter is invalid`() {
        val parsed = CatalogueParameters.parseKrithiSearch(parametersOf("publishedOnly" to listOf("false")))
        assertIs<CatalogueParseResult.Invalid>(parsed)
    }

    @Test
    fun `query longer than 200 code points is invalid`() {
        val tooLong = "a".repeat(CatalogueContract.QUERY_MAX_CODE_POINTS + 1)
        val parsed = CatalogueParameters.parseKrithiSearch(parametersOf("query" to listOf(tooLong)))
        assertIs<CatalogueParseResult.Invalid>(parsed)
    }

    @Test
    fun `malformed filter UUID is invalid`() {
        val parsed = CatalogueParameters.parseKrithiSearch(parametersOf("ragaId" to listOf("not-a-uuid")))
        assertIs<CatalogueParseResult.Invalid>(parsed)
    }

    @Test
    fun `pageSize above max is invalid`() {
        val parsed = CatalogueParameters.parseKrithiSearch(
            parametersOf("pageSize" to listOf((CatalogueContract.MAX_PAGE_SIZE + 1).toString())),
        )
        assertIs<CatalogueParseResult.Invalid>(parsed)
    }

    @Test
    fun `detail routes reject unexpected query parameters`() {
        val parsed = CatalogueParameters.rejectQueryParameters(parametersOf("query" to listOf("x")))
        assertIs<CatalogueParseResult.Invalid>(parsed)
    }

    @Test
    fun `path UUID parses`() {
        val id = Uuid.parse("11111111-1111-4111-8111-111111111111")
        val parsed = assertIs<CatalogueParseResult.Ok<Uuid>>(
            CatalogueParameters.parsePathUuid(id.toString(), "id"),
        )
        assertEquals(id, parsed.value)
    }

    @Test
    fun `malformed path UUID is invalid not missing`() {
        assertIs<CatalogueParseResult.Invalid>(CatalogueParameters.parsePathUuid("abc", "id"))
    }
}
