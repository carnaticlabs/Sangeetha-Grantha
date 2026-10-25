package com.sangita.grantha.backend.api.catalogue

import com.sangita.grantha.backend.dal.enums.VibhaktiCase
import com.sangita.grantha.backend.dal.repositories.FacetSelection
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetKeyDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMandalamDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleElementDto
import io.ktor.http.Parameters
import kotlin.uuid.Uuid

data class CatalogueListQuery(
    val query: String?,
    val composerId: Uuid?,
    val ragaId: Uuid?,
    val page: Int,
    val pageSize: Int,
    val facets: FacetSelection = FacetSelection.NONE,
)

/** `GET /v2/catalogue/facets`: the search scope, the active facet filters and whether to count. */
data class CatalogueFacetsQuery(
    val query: String?,
    val composerId: Uuid?,
    val ragaId: Uuid?,
    val facets: FacetSelection,
    val counts: Boolean,
)

data class CatalogueTempleListQuery(
    val element: CatalogueTempleElementDto?,
    val mandalam: CatalogueMandalamDto?,
)

sealed class CatalogueParseResult<out T> {
    data class Ok<T>(val value: T) : CatalogueParseResult<T>()
    data class Invalid(val message: String) : CatalogueParseResult<Nothing>()
}

object CatalogueParameters {
    private val krithiSearchParams = setOf("query", "composerId", "ragaId", "page", "pageSize")
    private val directoryParams = setOf("query", "page", "pageSize")
    private val templeListParams = setOf("element", "mandalam")
    private val facetParams = CatalogueFacetKeyDto.entries.map { it.wireKey }.toSet()
    private val facetsEndpointParams = setOf("query", "composerId", "ragaId", "counts") + facetParams

    /** Facet parameters are V2 only; V1 passes `allowFacets = false` and rejects them as unknown. */
    fun parseKrithiSearch(params: Parameters, allowFacets: Boolean = false): CatalogueParseResult<CatalogueListQuery> {
        unknown(params, if (allowFacets) krithiSearchParams + facetParams else krithiSearchParams)?.let { return it }
        repeated(params)?.let { return it }
        val facets = if (allowFacets) {
            when (val parsed = parseFacetSelection(params)) {
                is CatalogueParseResult.Invalid -> return parsed
                is CatalogueParseResult.Ok -> parsed.value
            }
        } else {
            FacetSelection.NONE
        }
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
                facets = facets,
            ),
        )
    }

    fun parseFacets(params: Parameters): CatalogueParseResult<CatalogueFacetsQuery> {
        unknown(params, facetsEndpointParams)?.let { return it }
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
        val counts = when (params["counts"]) {
            null -> false
            "true" -> true
            "false" -> false
            else -> return CatalogueParseResult.Invalid("counts must be true or false")
        }
        val facets = when (val parsed = parseFacetSelection(params)) {
            is CatalogueParseResult.Invalid -> return parsed
            is CatalogueParseResult.Ok -> parsed.value
        }
        return CatalogueParseResult.Ok(CatalogueFacetsQuery(query, composerId, ragaId, facets, counts))
    }

    /**
     * Syntax only: blank, overlong and (for enum facets) non-member values. Whether a value is
     * currently offered over the published catalogue is checked by the service.
     */
    private fun parseFacetSelection(params: Parameters): CatalogueParseResult<FacetSelection> {
        fun value(key: CatalogueFacetKeyDto, members: List<String>? = null): CatalogueParseResult<String?> {
            val raw = params[key.wireKey] ?: return CatalogueParseResult.Ok(null)
            if (raw.isBlank()) return CatalogueParseResult.Invalid("${key.wireKey} must not be blank")
            if (raw.codePointCount(0, raw.length) > FACET_VALUE_MAX_CODE_POINTS) {
                return CatalogueParseResult.Invalid(
                    "${key.wireKey} must be at most $FACET_VALUE_MAX_CODE_POINTS characters",
                )
            }
            if (members != null && raw !in members) {
                return CatalogueParseResult.Invalid("${key.wireKey} must be one of ${members.joinToString()}")
            }
            return CatalogueParseResult.Ok(raw)
        }
        fun <T> CatalogueParseResult<T>.orReturn(): T = when (this) {
            is CatalogueParseResult.Ok -> value
            is CatalogueParseResult.Invalid -> throw FacetInvalid(message)
        }
        return try {
            CatalogueParseResult.Ok(
                FacetSelection(
                    vibhakti = value(CatalogueFacetKeyDto.VIBHAKTI, VibhaktiCase.entries.map { it.name }).orReturn(),
                    collection = value(CatalogueFacetKeyDto.COLLECTION).orReturn(),
                    tune = value(CatalogueFacetKeyDto.TUNE).orReturn(),
                    mandalam = value(
                        CatalogueFacetKeyDto.MANDALAM,
                        CatalogueMandalamDto.entries.map { it.name },
                    ).orReturn(),
                    element = value(
                        CatalogueFacetKeyDto.ELEMENT,
                        CatalogueTempleElementDto.entries.map { it.name },
                    ).orReturn(),
                ),
            )
        } catch (e: FacetInvalid) {
            CatalogueParseResult.Invalid(e.message)
        }
    }

    private class FacetInvalid(override val message: String) : RuntimeException(message)

    private const val FACET_VALUE_MAX_CODE_POINTS = 64

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

    fun parseTempleList(params: Parameters): CatalogueParseResult<CatalogueTempleListQuery> {
        unknown(params, templeListParams)?.let { return it }
        repeated(params)?.let { return it }
        val element = params["element"]?.let { raw ->
            CatalogueTempleElementDto.entries.firstOrNull { it.name == raw }
                ?: return CatalogueParseResult.Invalid(
                    "element must be one of ${CatalogueTempleElementDto.entries.joinToString { it.name }}",
                )
        }
        val mandalam = params["mandalam"]?.let { raw ->
            CatalogueMandalamDto.entries.firstOrNull { it.name == raw }
                ?: return CatalogueParseResult.Invalid(
                    "mandalam must be one of ${CatalogueMandalamDto.entries.joinToString { it.name }}",
                )
        }
        return CatalogueParseResult.Ok(CatalogueTempleListQuery(element, mandalam))
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
