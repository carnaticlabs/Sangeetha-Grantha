package com.sangita.grantha.shared.mobile.network

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDiscoveryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueErrorDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricsDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueV2Contract
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

class KtorCatalogueApi(
    private val client: HttpClient,
) : CatalogueApi {
    override suspend fun getDiscovery(interaction: InteractionContext): CatalogueDiscoveryDto =
        get(CatalogueV2Contract.DISCOVERY_PATH, interaction)

    override suspend fun searchKrithis(
        query: String?,
        composerId: Uuid?,
        ragaId: Uuid?,
        page: Int,
        pageSize: Int,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueKrithiSummaryDto> =
        get(CatalogueV2Contract.KRITHIS_PATH, interaction) {
            optionalQuery("query", query)
            optionalUuid("composerId", composerId)
            optionalUuid("ragaId", ragaId)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }

    override suspend fun getKrithi(id: Uuid, interaction: InteractionContext): CatalogueKrithiReaderDto =
        get("${CatalogueV2Contract.KRITHIS_PATH}/$id", interaction)

    override suspend fun getLyrics(
        krithiId: Uuid,
        variantId: Uuid,
        interaction: InteractionContext,
    ): CatalogueLyricsDto =
        get("${CatalogueV2Contract.KRITHIS_PATH}/$krithiId/lyrics/$variantId", interaction)

    override suspend fun searchRagas(
        query: String?,
        page: Int,
        pageSize: Int,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueRagaSummaryDto> =
        get(CatalogueV2Contract.RAGAS_PATH, interaction) {
            optionalQuery("query", query)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }

    override suspend fun getRaga(id: Uuid, interaction: InteractionContext): CatalogueRagaDetailDto =
        get("${CatalogueV2Contract.RAGAS_PATH}/$id", interaction)

    override suspend fun searchComposers(
        query: String?,
        page: Int,
        pageSize: Int,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueComposerSummaryDto> =
        get(CatalogueV2Contract.COMPOSERS_PATH, interaction) {
            optionalQuery("query", query)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }

    override suspend fun getComposer(id: Uuid, interaction: InteractionContext): CatalogueComposerDetailDto =
        get("${CatalogueV2Contract.COMPOSERS_PATH}/$id", interaction)

    private suspend inline fun <reified T> get(
        path: String,
        interaction: InteractionContext,
        crossinline extra: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = try {
            client.get(path) {
                header(CatalogueContract.SESSION_HEADER, interaction.sessionId.toString())
                header(CatalogueContract.INTERACTION_HEADER, interaction.interactionId.toString())
                extra()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (timeout: HttpRequestTimeoutException) {
            throw CatalogueFailure.Timeout(cause = timeout)
        } catch (cause: Exception) {
            throw CatalogueFailure.Unavailable(cause = cause)
        }
        return decode(response)
    }

    private suspend inline fun <reified T> decode(response: HttpResponse): T {
        if (response.status.isSuccess()) {
            return response.body()
        }
        val error = runCatching { response.body<CatalogueErrorDto>() }.getOrNull()
        throw when (response.status) {
            HttpStatusCode.BadRequest ->
                CatalogueFailure.Validation(error?.message ?: "That search is not valid.")
            HttpStatusCode.NotFound ->
                CatalogueFailure.NotFound(error?.message ?: "This composition is not available.")
            else ->
                CatalogueFailure.Unavailable(error?.message ?: "The catalogue could not be reached.")
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.optionalQuery(name: String, value: String?) {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isNotEmpty()) parameter(name, trimmed)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.optionalUuid(name: String, value: Uuid?) {
        if (value != null) parameter(name, value.toString())
    }
}
