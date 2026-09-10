package com.sangita.grantha.shared.domain.model.catalogue

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/** V2 is explicit: clients must not downgrade to the legacy known-form catalogue. */
object CatalogueV2Contract {
    const val ROOT = "/v2/catalogue"
    const val KRITHIS_PATH = "$ROOT/krithis"
    const val RAGAS_PATH = "$ROOT/ragas"
    const val COMPOSERS_PATH = "$ROOT/composers"
    const val DISCOVERY_PATH = "$ROOT/discovery"
}

@Serializable
data class CatalogueReferenceDto(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    val name: String,
)

@Serializable
enum class CatalogueSelectionDto { CATALOGUE_ORDER, EDITORIAL }

@Serializable
data class CatalogueDiscoveryFeatureDto(
    val selection: CatalogueSelectionDto,
    val heading: String,
    val summary: String,
    val krithi: CatalogueKrithiSummaryDto,
)

@Serializable
data class CatalogueDiscoveryDto(
    val feature: CatalogueDiscoveryFeatureDto? = null,
    val editorialRevision: String? = null,
)

/** IDs are UUID strings for stored entities and domain codes for enum directories. */
@Serializable
data class CatalogueMetadataSummaryDto(
    val id: String,
    val name: String,
    val publishedCompositionCount: Long,
)

@Serializable
data class CatalogueMetadataDetailDto(
    val id: String,
    val name: String,
    val publishedCompositionCount: Long,
    val aliases: List<String> = emptyList(),
    val beatCount: Int? = null,
    val angaStructure: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val deity: CatalogueReferenceDto? = null,
)
