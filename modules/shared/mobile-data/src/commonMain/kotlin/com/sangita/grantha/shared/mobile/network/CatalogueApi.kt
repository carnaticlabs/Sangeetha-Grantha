package com.sangita.grantha.shared.mobile.network

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
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import kotlin.uuid.Uuid

interface CatalogueApi {
    suspend fun getDiscovery(interaction: InteractionContext): CatalogueDiscoveryDto

    suspend fun searchKrithis(
        query: String? = null,
        composerId: Uuid? = null,
        ragaId: Uuid? = null,
        page: Int = CatalogueContract.DEFAULT_PAGE,
        pageSize: Int = CatalogueContract.DEFAULT_PAGE_SIZE,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueKrithiSummaryDto>

    suspend fun getKrithi(id: Uuid, interaction: InteractionContext): CatalogueKrithiReaderDto

    suspend fun getLyrics(
        krithiId: Uuid,
        variantId: Uuid,
        interaction: InteractionContext,
    ): CatalogueLyricsDto

    suspend fun searchRagas(
        query: String? = null,
        page: Int = CatalogueContract.DEFAULT_PAGE,
        pageSize: Int = CatalogueContract.DEFAULT_PAGE_SIZE,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueRagaSummaryDto>

    suspend fun getRaga(id: Uuid, interaction: InteractionContext): CatalogueRagaDetailDto

    suspend fun searchComposers(
        query: String? = null,
        page: Int = CatalogueContract.DEFAULT_PAGE,
        pageSize: Int = CatalogueContract.DEFAULT_PAGE_SIZE,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueComposerSummaryDto>

    suspend fun getComposer(id: Uuid, interaction: InteractionContext): CatalogueComposerDetailDto
}
