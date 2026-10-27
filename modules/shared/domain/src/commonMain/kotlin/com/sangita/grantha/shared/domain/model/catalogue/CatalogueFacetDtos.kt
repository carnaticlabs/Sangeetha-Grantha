package com.sangita.grantha.shared.domain.model.catalogue

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lakshana facets (TRACK-153). V2 only. The serial names are the query-parameter names of
 * `GET /v2/catalogue/krithis` and `GET /v2/catalogue/facets`, so a facet is added by adding one
 * member here and one `FacetDefinition` in the DAL.
 */
@Serializable
enum class CatalogueFacetKeyDto(val wireKey: String) {
    @SerialName("vibhakti") VIBHAKTI("vibhakti"),
    @SerialName("collection") COLLECTION("collection"),
    @SerialName("tune") TUNE("tune"),
    @SerialName("mandalam") MANDALAM("mandalam"),
    @SerialName("element") ELEMENT("element"),
    ;

    companion object {
        fun fromWireKey(key: String): CatalogueFacetKeyDto? = entries.firstOrNull { it.wireKey == key }
    }
}

/**
 * One facet value. [value] is what the filter accepts (enum code, collection slug, stored tune name);
 * [label] is set for collection (display name) and tune (the stored string), null for enum facets whose
 * copy lives on the client. [count] is null unless the request asked for counts.
 */
@Serializable
data class CatalogueFacetValueDto(
    val value: String,
    val label: String? = null,
    val count: Long? = null,
)

@Serializable
data class CatalogueFacetDto(
    val key: CatalogueFacetKeyDto,
    val values: List<CatalogueFacetValueDto>,
)

@Serializable
data class CatalogueFacetListDto(val facets: List<CatalogueFacetDto> = emptyList())

/** A tappable property of one kriti; [value] is always accepted by the matching search filter. */
@Serializable
data class CatalogueFacetChipDto(
    val key: CatalogueFacetKeyDto,
    val value: String,
    val label: String? = null,
)
