package com.sangita.grantha.shared.mobile.network

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetKeyDto

/**
 * The lakshana facet values a rasika has applied (TRACK-153): at most one value per facet. Distinct from
 * the unrelated Explore composer/raga labels.
 */
data class LakshanaFacetSelection(val values: Map<CatalogueFacetKeyDto, String> = emptyMap()) {
    val isEmpty: Boolean get() = values.isEmpty()

    operator fun get(key: CatalogueFacetKeyDto): String? = values[key]

    fun with(key: CatalogueFacetKeyDto, value: String): LakshanaFacetSelection =
        LakshanaFacetSelection(values + (key to value))

    fun without(key: CatalogueFacetKeyDto): LakshanaFacetSelection =
        LakshanaFacetSelection(values - key)

    companion object {
        val NONE = LakshanaFacetSelection()
    }
}
