package com.sangita.grantha.backend.dal.models

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDiscoveryFeatureDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSelectionDto

object CatalogueV2DtoMappers {
    const val CATALOGUE_ORDER_HEADING = "From the collection"
    const val CATALOGUE_ORDER_SUMMARY = "Explore a composition in this library."
    const val HEADING_MAX_CODE_POINTS = 80
    const val SUMMARY_MAX_CODE_POINTS = 240

    fun catalogueOrderFeature(krithi: CatalogueKrithiSummaryDto): CatalogueDiscoveryFeatureDto =
        CatalogueDiscoveryFeatureDto(
            selection = CatalogueSelectionDto.CATALOGUE_ORDER,
            heading = clipCodePoints(CATALOGUE_ORDER_HEADING, HEADING_MAX_CODE_POINTS),
            summary = clipCodePoints(
                krithi.incipit?.trim().takeUnless { it.isNullOrBlank() } ?: CATALOGUE_ORDER_SUMMARY,
                SUMMARY_MAX_CODE_POINTS,
            ),
            krithi = krithi,
        )

    fun clipCodePoints(text: String, maxCodePoints: Int): String {
        if (maxCodePoints <= 0) return ""
        val count = text.codePointCount(0, text.length)
        if (count <= maxCodePoints) return text
        val end = text.offsetByCodePoints(0, maxCodePoints)
        return text.substring(0, end)
    }
}
