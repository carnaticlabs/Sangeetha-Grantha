package com.sangita.grantha.shared.mobile.repository

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDiscoveryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricsDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import com.sangita.grantha.shared.mobile.network.CatalogueApi
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
}
