package com.sangita.grantha.shared.domain.model.catalogue

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Kshetra (temple) read model for Rasika (TRACK-151). V2 only: V1 payloads never carry these
 * types. Enums serialise as the database codes.
 */
@Serializable
enum class CatalogueMandalamDto { CHOLA, PANDYA, TONDAI, NADU, CHERA, KONGU, UTTARA }

@Serializable
enum class CataloguePlaceKindDto { LOCALITY, COMPLEX, SANNIDHI, MANDAPAM }

/** The `bhuta_enum` values. `APPU` is water; mobile copy glosses the codes. */
@Serializable
enum class CatalogueTempleElementDto { AKASHA, VAYU, AGNI, PRITHVI, APPU }

@Serializable
enum class CatalogueDeityPostureDto { SAYANA, STHANAKA, ASINA, TANDAVA }

@Serializable
enum class CatalogueTemplePlacePrecisionDto {
    /** The temple's own confirmed place. */
    TEMPLE,

    /** A sannidhi or mandapam that inherits its parent complex's confirmed place. */
    PARENT_COMPLEX,
}

/**
 * Optional Google place id for building a Maps link without an API key. Present only for a
 * CONFIRMED place; nothing else from Google (no coordinates, name or address) is stored or exposed.
 */
@Serializable
data class CatalogueTemplePlaceDto(
    val googlePlaceId: String,
    val precision: CatalogueTemplePlacePrecisionDto,
)

@Serializable
data class CatalogueTempleSummaryDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val city: String? = null,
    val state: String? = null,
    val placeKind: CataloguePlaceKindDto,
    val mandalam: CatalogueMandalamDto? = null,
    val element: CatalogueTempleElementDto? = null,
    /** Published kritis recorded directly at this temple (not rolled up over children). */
    val publishedKrithiCount: Long = 0,
    /** Eligible child shrines. */
    val childCount: Int = 0,
    /** Derived: no published kriti here or in any eligible child, but a published overview exists. */
    val recordedGap: Boolean = false,
)

@Serializable
data class CatalogueTempleListDto(val items: List<CatalogueTempleSummaryDto>)

@Serializable
data class CatalogueTempleDetailDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val placeKind: CataloguePlaceKindDto,
    val mandalam: CatalogueMandalamDto? = null,
    val element: CatalogueTempleElementDto? = null,
    val publishedKrithiCount: Long = 0,
    val childCount: Int = 0,
    val recordedGap: Boolean = false,
    val sthalaVriksha: String? = null,
    val sthalaTirtha: String? = null,
    val nadiTirtha: String? = null,
    val deityPosture: CatalogueDeityPostureDto? = null,
    /** Stored `KSHETRA_OVERVIEW` text, returned as written; for a recorded gap it is the reason. */
    val overview: String? = null,
    val parent: CatalogueReferenceDto? = null,
    val children: List<CatalogueTempleSummaryDto> = emptyList(),
    val krithis: List<CatalogueKrithiSummaryDto> = emptyList(),
    /** Always null until TRACK-154 supplies confirmed place ids. */
    val place: CatalogueTemplePlaceDto? = null,
)
