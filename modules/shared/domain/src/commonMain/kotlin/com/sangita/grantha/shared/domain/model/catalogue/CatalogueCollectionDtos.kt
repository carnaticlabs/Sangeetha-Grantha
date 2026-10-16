package com.sangita.grantha.shared.domain.model.catalogue

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Reviewed cycle collections exposed to Rasika (TRACK-150). V2 only: V1 payloads never carry
 * these fields. Attribute maps are an allow-list of reviewed keys, never raw stored JSON.
 */
@Serializable
enum class CatalogueOrderBasisDto {
    /** Position is attested by the cycle's own structure (for example the nine Avaranas). */
    SOURCE,

    /** Order is a later performance convention, not a composer sequence. */
    PERFORMANCE_CONVENTION,

    /** No attested sequence; clients must not show ordinals. */
    UNSPECIFIED,
}

@Serializable
enum class CatalogueCollectionRoleDto { DHYANA, CORE, OPTIONAL, MANGALAM, DISPUTED_CONJECTURE }

@Serializable
data class CatalogueCollectionNeighbourDto(
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val title: String,
    val ragaName: String? = null,
)

/** One collection a kriti belongs to, with its place in the ordered members. */
@Serializable
data class CatalogueCollectionRefDto(
    @Serializable(with = UuidSerializer::class)
    val collectionId: Uuid,
    val slug: String,
    val name: String,
    val role: CatalogueCollectionRoleDto,
    val sequenceOrder: Int,
    /** 1-based rank among CORE members; null for other roles. */
    val position: Int? = null,
    /** Number of visible CORE members. */
    val coreCount: Int,
    val axisValue: String? = null,
    val orderBasis: CatalogueOrderBasisDto = CatalogueOrderBasisDto.UNSPECIFIED,
    val orderConvention: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val previous: CatalogueCollectionNeighbourDto? = null,
    val next: CatalogueCollectionNeighbourDto? = null,
)

/** Reader enrichment. Null on V1 and when the kriti has no reviewed cycle context. */
@Serializable
data class CatalogueCycleContextDto(
    val collections: List<CatalogueCollectionRefDto> = emptyList(),
    val vibhaktiCase: String? = null,
    val occasionNote: String? = null,
)

@Serializable
data class CatalogueCollectionSummaryDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val slug: String,
    val name: String,
    val composer: CatalogueComposerRefDto? = null,
    val memberCount: Int,
    val orderBasis: CatalogueOrderBasisDto = CatalogueOrderBasisDto.UNSPECIFIED,
    val hasOverview: Boolean = false,
)

@Serializable
data class CatalogueCollectionListDto(val items: List<CatalogueCollectionSummaryDto>)

@Serializable
data class CatalogueCollectionMemberDto(
    val krithi: CatalogueKrithiSummaryDto,
    val role: CatalogueCollectionRoleDto,
    val sequenceOrder: Int,
    val position: Int? = null,
    val axisValue: String? = null,
    /** "disputed" for DISPUTED_CONJECTURE members. */
    val label: String? = null,
    val attributes: Map<String, String> = emptyMap(),
)

@Serializable
data class CatalogueCollectionDetailDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val slug: String,
    val name: String,
    val composer: CatalogueComposerRefDto? = null,
    val overview: String? = null,
    val orderBasis: CatalogueOrderBasisDto = CatalogueOrderBasisDto.UNSPECIFIED,
    val orderConvention: String? = null,
    /** False for open sets such as Shodasa Ganapati and Nottusvara. */
    val isComplete: Boolean = true,
    val coreCount: Int,
    val members: List<CatalogueCollectionMemberDto>,
)
