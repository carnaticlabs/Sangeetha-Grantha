package com.sangita.grantha.shared.mobile.repository

import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionListDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDiscoveryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricsDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchResponseDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetListDto
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.network.LakshanaFacetSelection
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import kotlin.uuid.Uuid

class CatalogueRepository(
    private val api: CatalogueApi,
) {
    suspend fun getDiscovery(interaction: InteractionContext): CatalogueDiscoveryDto =
        api.getDiscovery(interaction)

    suspend fun searchKrithis(
        query: String? = null,
        composerId: Uuid? = null,
        ragaId: Uuid? = null,
        page: Int = CatalogueContract.DEFAULT_PAGE,
        pageSize: Int = CatalogueContract.DEFAULT_PAGE_SIZE,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueKrithiSummaryDto> =
        api.searchKrithis(query, composerId, ragaId, page, pageSize, interaction)

    suspend fun searchKrithis(
        query: String? = null,
        composerId: Uuid? = null,
        ragaId: Uuid? = null,
        facets: LakshanaFacetSelection,
        page: Int = CatalogueContract.DEFAULT_PAGE,
        pageSize: Int = CatalogueContract.DEFAULT_PAGE_SIZE,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueKrithiSummaryDto> =
        api.searchKrithisFaceted(query, composerId, ragaId, facets, page, pageSize, interaction)

    suspend fun listFacets(
        query: String? = null,
        composerId: Uuid? = null,
        ragaId: Uuid? = null,
        facets: LakshanaFacetSelection = LakshanaFacetSelection.NONE,
        counts: Boolean = false,
        interaction: InteractionContext,
    ): CatalogueFacetListDto = api.listFacets(query, composerId, ragaId, facets, counts, interaction)

    suspend fun getKrithi(id: Uuid, interaction: InteractionContext): CatalogueKrithiReaderDto =
        api.getKrithi(id, interaction)

    suspend fun getLyrics(
        krithiId: Uuid,
        variantId: Uuid,
        interaction: InteractionContext,
    ): CatalogueLyricsDto = api.getLyrics(krithiId, variantId, interaction)

    suspend fun searchRagas(
        query: String? = null,
        page: Int = CatalogueContract.DEFAULT_PAGE,
        pageSize: Int = CatalogueContract.DEFAULT_PAGE_SIZE,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueRagaSummaryDto> =
        api.searchRagas(query, page, pageSize, interaction)

    suspend fun getRaga(id: Uuid, interaction: InteractionContext): CatalogueRagaDetailDto =
        api.getRaga(id, interaction)

    suspend fun searchComposers(
        query: String? = null,
        page: Int = CatalogueContract.DEFAULT_PAGE,
        pageSize: Int = CatalogueContract.DEFAULT_PAGE_SIZE,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueComposerSummaryDto> =
        api.searchComposers(query, page, pageSize, interaction)

    suspend fun getComposer(id: Uuid, interaction: InteractionContext): CatalogueComposerDetailDto =
        api.getComposer(id, interaction)

    suspend fun listCollections(interaction: InteractionContext): CatalogueCollectionListDto =
        api.listCollections(interaction)

    suspend fun getCollection(slug: String, interaction: InteractionContext): CatalogueCollectionDetailDto =
        api.getCollection(slug, interaction)

    suspend fun searchHybrid(
        request: SemanticSearchRequest,
        interaction: InteractionContext,
    ): CatalogueSearchResponseDto = api.searchHybridWithCards(request, interaction)

    suspend fun searchSemantic(
        request: SemanticSearchRequest,
        interaction: InteractionContext,
    ): CatalogueSearchResponseDto = api.searchSemanticWithCards(request, interaction)
}
