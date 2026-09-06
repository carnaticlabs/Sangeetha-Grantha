package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.catalogue.CatalogueParameters
import com.sangita.grantha.backend.api.catalogue.CatalogueParseResult
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricsDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import io.ktor.http.Parameters
import kotlin.uuid.Uuid

sealed class CatalogueResult<out T> {
    data class Ok<T>(val value: T) : CatalogueResult<T>()
    data class Invalid(val message: String) : CatalogueResult<Nothing>()
    data object NotFound : CatalogueResult<Nothing>()
}

class CatalogueService(
    private val dal: SangitaDal,
) {
    suspend fun searchKrithis(params: Parameters): CatalogueResult<CataloguePagedResponse<CatalogueKrithiSummaryDto>> =
        when (val parsed = CatalogueParameters.parseKrithiSearch(params)) {
            is CatalogueParseResult.Invalid -> CatalogueResult.Invalid(parsed.message)
            is CatalogueParseResult.Ok -> CatalogueResult.Ok(
                dal.catalogue.searchKrithis(
                    query = parsed.value.query,
                    composerId = parsed.value.composerId?.toJavaUuid(),
                    ragaId = parsed.value.ragaId?.toJavaUuid(),
                    page = parsed.value.page,
                    pageSize = parsed.value.pageSize,
                ),
            )
        }

    suspend fun getKrithi(idRaw: String?, params: Parameters): CatalogueResult<CatalogueKrithiReaderDto> {
        rejectExtra(params)?.let { return it }
        val id = pathUuid(idRaw, "id") ?: return pathInvalid("id")
        return dal.catalogue.findPublishedReader(id)?.let { CatalogueResult.Ok(it) } ?: CatalogueResult.NotFound
    }

    suspend fun getLyrics(
        krithiIdRaw: String?,
        variantIdRaw: String?,
        params: Parameters,
    ): CatalogueResult<CatalogueLyricsDto> {
        rejectExtra(params)?.let { return it }
        val krithiId = pathUuid(krithiIdRaw, "id") ?: return pathInvalid("id")
        val variantId = pathUuid(variantIdRaw, "variantId") ?: return pathInvalid("variantId")
        return dal.catalogue.findPublishedLyrics(krithiId, variantId)
            ?.let { CatalogueResult.Ok(it) }
            ?: CatalogueResult.NotFound
    }

    suspend fun searchRagas(params: Parameters): CatalogueResult<CataloguePagedResponse<CatalogueRagaSummaryDto>> =
        when (val parsed = CatalogueParameters.parseDirectory(params)) {
            is CatalogueParseResult.Invalid -> CatalogueResult.Invalid(parsed.message)
            is CatalogueParseResult.Ok -> CatalogueResult.Ok(
                dal.catalogue.searchRagas(parsed.value.query, parsed.value.page, parsed.value.pageSize),
            )
        }

    suspend fun getRaga(idRaw: String?, params: Parameters): CatalogueResult<CatalogueRagaDetailDto> {
        rejectExtra(params)?.let { return it }
        val id = pathUuid(idRaw, "id") ?: return pathInvalid("id")
        return dal.catalogue.findRaga(id)?.let { CatalogueResult.Ok(it) } ?: CatalogueResult.NotFound
    }

    suspend fun searchComposers(params: Parameters): CatalogueResult<CataloguePagedResponse<CatalogueComposerSummaryDto>> =
        when (val parsed = CatalogueParameters.parseDirectory(params)) {
            is CatalogueParseResult.Invalid -> CatalogueResult.Invalid(parsed.message)
            is CatalogueParseResult.Ok -> CatalogueResult.Ok(
                dal.catalogue.searchComposers(parsed.value.query, parsed.value.page, parsed.value.pageSize),
            )
        }

    suspend fun getComposer(idRaw: String?, params: Parameters): CatalogueResult<CatalogueComposerDetailDto> {
        rejectExtra(params)?.let { return it }
        val id = pathUuid(idRaw, "id") ?: return pathInvalid("id")
        return dal.catalogue.findComposer(id)?.let { CatalogueResult.Ok(it) } ?: CatalogueResult.NotFound
    }

    private fun rejectExtra(params: Parameters): CatalogueResult.Invalid? =
        when (val parsed = CatalogueParameters.rejectQueryParameters(params)) {
            is CatalogueParseResult.Invalid -> CatalogueResult.Invalid(parsed.message)
            is CatalogueParseResult.Ok -> null
        }

    private fun pathUuid(raw: String?, label: String): Uuid? =
        when (val parsed = CatalogueParameters.parsePathUuid(raw, label)) {
            is CatalogueParseResult.Ok -> parsed.value
            is CatalogueParseResult.Invalid -> null
        }

    private fun pathInvalid(label: String) = CatalogueResult.Invalid("$label must be a valid UUID")
}
