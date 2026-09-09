package com.sangita.grantha.backend.api.catalogue

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import io.ktor.http.Parameters
import kotlin.uuid.Uuid

data class CatalogueListQuery(
    val query: String?,
    val composerId: Uuid?,
    val ragaId: Uuid?,
    val page: Int,
    val pageSize: Int,
)

sealed class CatalogueParseResult<out T> {
    data class Ok<T>(val value: T) : CatalogueParseResult<T>()
    data class Invalid(val message: String) : CatalogueParseResult<Nothing>()
}

object CatalogueParameters {
    private val krithiSearchParams = setOf("query", "composerId", "ragaId", "page", "pageSize")
    private val directoryParams = setOf("query", "page", "pageSize")

    fun parseKrithiSearch(params: Parameters): CatalogueParseResult<CatalogueListQuery> {
        unknown(params, krithiSearchParams)?.let { return it }
        repeated(params)?.let { return it }
        val query = when (val parsed = parseQuery(params["query"])) {
            is OptionalString.Missing -> null
            is OptionalString.Present -> parsed.value
            OptionalString.Invalid -> return queryTooLong()
        }
        val composerId = when (val parsed = parseOptionalUuid(params["composerId"])) {
            is OptionalUuid.Missing -> null
            is OptionalUuid.Present -> parsed.value
            OptionalUuid.Invalid -> return CatalogueParseResult.Invalid("composerId must be a valid UUID")
        }
        val ragaId = when (val parsed = parseOptionalUuid(params["ragaId"])) {
            is OptionalUuid.Missing -> null
            is OptionalUuid.Present -> parsed.value
            OptionalUuid.Invalid -> return CatalogueParseResult.Invalid("ragaId must be a valid UUID")
        }
        val page = parsePage(params["page"])
            ?: return CatalogueParseResult.Invalid("page must be an integer >= 0")
        val pageSize = parsePageSize(params["pageSize"])
            ?: return CatalogueParseResult.Invalid(
                "pageSize must be an integer from 1 to ${CatalogueContract.MAX_PAGE_SIZE}",
            )
        return CatalogueParseResult.Ok(
            CatalogueListQuery(
                query = query,
                composerId = composerId,
                ragaId = ragaId,
                page = page,
                pageSize = pageSize,
            ),
        )
    }

    fun parseDirectory(params: Parameters): CatalogueParseResult<CatalogueListQuery> {
        unknown(params, directoryParams)?.let { return it }
        repeated(params)?.let { return it }
        val query = when (val parsed = parseQuery(params["query"])) {
            is OptionalString.Missing -> null
            is OptionalString.Present -> parsed.value
            OptionalString.Invalid -> return queryTooLong()
        }
        val page = parsePage(params["page"])
            ?: return CatalogueParseResult.Invalid("page must be an integer >= 0")
        val pageSize = parsePageSize(params["pageSize"])
            ?: return CatalogueParseResult.Invalid(
                "pageSize must be an integer from 1 to ${CatalogueContract.MAX_PAGE_SIZE}",
            )
        return CatalogueParseResult.Ok(
            CatalogueListQuery(
                query = query,
                composerId = null,
                ragaId = null,
                page = page,
                pageSize = pageSize,
            ),
        )
    }

    fun parsePathUuid(raw: String?, label: String): CatalogueParseResult<Uuid> {
        if (raw.isNullOrBlank()) {
            return CatalogueParseResult.Invalid("$label is required")
        }
        return runCatching { Uuid.parse(raw) }
            .fold(
                onSuccess = { CatalogueParseResult.Ok(it) },
                onFailure = { CatalogueParseResult.Invalid("$label must be a valid UUID") },
            )
    }

    fun rejectQueryParameters(params: Parameters): CatalogueParseResult<Unit> {
        unknown(params, emptySet())?.let { return it }
        repeated(params)?.let { return it }
        return CatalogueParseResult.Ok(Unit)
    }

    private fun repeated(params: Parameters): CatalogueParseResult.Invalid? {
        val keys = params.entries().filter { it.value.size > 1 }.map { it.key }.sorted()
        if (keys.isEmpty()) return null
        return CatalogueParseResult.Invalid("Repeated parameter: ${keys.joinToString()}")
    }

    private fun unknown(params: Parameters, allowed: Set<String>): CatalogueParseResult.Invalid? {
        val extra = params.entries().map { it.key }.toSet() - allowed
        if (extra.isEmpty()) return null
        return CatalogueParseResult.Invalid("Unsupported parameter: ${extra.sorted().joinToString()}")
    }

    private fun queryTooLong() =
        CatalogueParseResult.Invalid("query must be at most ${CatalogueContract.QUERY_MAX_CODE_POINTS} characters")

    private fun parseQuery(raw: String?): OptionalString {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return OptionalString.Missing
        val codePoints = trimmed.codePointCount(0, trimmed.length)
        if (codePoints > CatalogueContract.QUERY_MAX_CODE_POINTS) return OptionalString.Invalid
        return OptionalString.Present(trimmed)
    }

    private fun parsePage(raw: String?): Int? {
        if (raw.isNullOrBlank()) return CatalogueContract.DEFAULT_PAGE
        val value = raw.toIntOrNull() ?: return null
        return value.takeIf { it >= 0 }
    }

    private fun parsePageSize(raw: String?): Int? {
        if (raw.isNullOrBlank()) return CatalogueContract.DEFAULT_PAGE_SIZE
        val value = raw.toIntOrNull() ?: return null
        return value.takeIf { it in 1..CatalogueContract.MAX_PAGE_SIZE }
    }

    private fun parseOptionalUuid(raw: String?): OptionalUuid {
        if (raw.isNullOrBlank()) return OptionalUuid.Missing
        return runCatching { Uuid.parse(raw) }
            .fold(onSuccess = { OptionalUuid.Present(it) }, onFailure = { OptionalUuid.Invalid })
    }

    private sealed class OptionalString {
        data object Missing : OptionalString()
        data object Invalid : OptionalString()
        data class Present(val value: String) : OptionalString()
    }

    private sealed class OptionalUuid {
        data object Missing : OptionalUuid()
        data object Invalid : OptionalUuid()
        data class Present(val value: Uuid) : OptionalUuid()
    }
}
