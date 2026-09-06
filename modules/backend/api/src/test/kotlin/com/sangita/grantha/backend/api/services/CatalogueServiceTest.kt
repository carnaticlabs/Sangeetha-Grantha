package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCompletenessDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricsDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import io.ktor.http.parametersOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class CatalogueServiceTest {
    private val dal: SangitaDal = mockk(relaxed = true)
    private val service = CatalogueService(dal)

    @Test
    fun unknownSearchParameterIsInvalidAndDoesNotQuery() = runTest {
        val result = service.searchKrithis(parametersOf("publishedOnly" to listOf("false")))
        assertIs<CatalogueResult.Invalid>(result)
        assertTrue(result.message.contains("publishedOnly"))
        coVerify(exactly = 0) { dal.catalogue.searchKrithis(any(), any(), any(), any(), any()) }
    }

    @Test
    fun extraDetailParameterIsInvalid() = runTest {
        val id = "66666666-6666-4666-8666-666666666666"
        val result = service.getKrithi(id, parametersOf("unlock" to listOf("1")))
        assertIs<CatalogueResult.Invalid>(result)
        coVerify(exactly = 0) { dal.catalogue.findPublishedReader(any()) }
    }

    @Test
    fun malformedPathUuidIsInvalid() = runTest {
        val result = service.getKrithi("not-a-uuid", parametersOf())
        assertIs<CatalogueResult.Invalid>(result)
        assertTrue(result.message.contains("id"))
    }

    @Test
    fun missingPublishedReaderIsNotFound() = runTest {
        val id = Uuid.parse("66666666-6666-4666-8666-666666666666")
        coEvery { dal.catalogue.findPublishedReader(id) } returns null
        val result = service.getKrithi(id.toString(), parametersOf())
        assertIs<CatalogueResult.NotFound>(result)
    }

    @Test
    fun publishedReaderIsOk() = runTest {
        val id = Uuid.parse("66666666-6666-4666-8666-666666666666")
        val reader = CatalogueKrithiReaderDto(
            id = id,
            title = "Vatapi Ganapatim",
            composer = CatalogueComposerRefDto(id, "Dikshitar"),
            ragas = emptyList(),
            originalLanguage = LanguageCodeDto.SA,
            completeness = CatalogueCompletenessDto.UNKNOWN,
        )
        coEvery { dal.catalogue.findPublishedReader(id) } returns reader
        val result = service.getKrithi(id.toString(), parametersOf())
        assertIs<CatalogueResult.Ok<CatalogueKrithiReaderDto>>(result)
        assertEquals("Vatapi Ganapatim", result.value.title)
    }

    @Test
    fun searchDelegatesParsedFilters() = runTest {
        val composerId = Uuid.parse("11111111-1111-4111-8111-111111111111")
        val page = CataloguePagedResponse<CatalogueKrithiSummaryDto>(emptyList(), 0, 0, 30)
        coEvery { dal.catalogue.searchKrithis(eq("Vatapi"), any(), isNull(), eq(0), eq(30)) } returns page
        val result = service.searchKrithis(
            parametersOf(
                "query" to listOf("Vatapi"),
                "composerId" to listOf(composerId.toString()),
            ),
        )
        assertIs<CatalogueResult.Ok<CataloguePagedResponse<CatalogueKrithiSummaryDto>>>(result)
        coVerify(exactly = 1) { dal.catalogue.searchKrithis(eq("Vatapi"), any(), isNull(), eq(0), eq(30)) }
    }

    @Test
    fun lyricsWrongOwnerIsNotFound() = runTest {
        val krithiId = Uuid.parse("66666666-6666-4666-8666-666666666666")
        val variantId = Uuid.parse("88888888-8888-4888-8888-888888888888")
        coEvery { dal.catalogue.findPublishedLyrics(krithiId, variantId) } returns null
        val result = service.getLyrics(krithiId.toString(), variantId.toString(), parametersOf())
        assertIs<CatalogueResult.NotFound>(result)
    }

    @Test
    fun lyricsOkReturnsStoredDto() = runTest {
        val krithiId = Uuid.parse("66666666-6666-4666-8666-666666666666")
        val variantId = Uuid.parse("88888888-8888-4888-8888-888888888888")
        val lyrics = CatalogueLyricsDto(
            variantId = variantId,
            krithiId = krithiId,
            language = LanguageCodeDto.SA,
            script = ScriptCodeDto.LATIN,
            unsegmentedText = "Vatapi Ganapatim Bhajeham",
        )
        coEvery { dal.catalogue.findPublishedLyrics(krithiId, variantId) } returns lyrics
        val result = service.getLyrics(krithiId.toString(), variantId.toString(), parametersOf())
        assertIs<CatalogueResult.Ok<CatalogueLyricsDto>>(result)
        assertEquals("Vatapi Ganapatim Bhajeham", result.value.unsegmentedText)
    }
}
