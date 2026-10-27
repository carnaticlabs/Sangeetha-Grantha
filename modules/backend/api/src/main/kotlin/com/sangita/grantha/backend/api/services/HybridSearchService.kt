package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.EmbeddingServiceUnavailableException
import com.sangita.grantha.backend.api.clients.GeminiEmbeddingClient
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.repositories.EmbeddingProfileRef
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.SemanticSearchRequest
import com.sangita.grantha.shared.domain.model.SemanticSearchResponse
import com.sangita.grantha.shared.domain.model.SemanticSearchResultItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

class HybridSearchService(
    private val dal: SangitaDal,
    private val embeddingClient: GeminiEmbeddingClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val queryVectorCache = LinkedHashMap<String, List<Float>>(QUERY_VECTOR_CACHE_SIZE, 0.75f, true)
    private val queryVectorCacheMutex = Mutex()

    /** The embedded query and the profile it is bound to, so two retrievals share one embedding space. */
    class PreparedQuery(val query: String, val profile: EmbeddingProfileRef?, val vector: List<Float>?)

    /** Binds to the active profile and embeds the query (cached). Both are null when nothing is indexed yet. */
    suspend fun prepareQuery(query: String): PreparedQuery {
        val profile = resolveCompatibleProfile()
        return PreparedQuery(query, profile, profile?.let { embedQueryCached(query) })
    }

    /**
     * Hybrid search degrades to lexical-only when no embedding profile is active yet
     * (nothing indexed); it never mixes embedding spaces.
     */
    suspend fun searchHybrid(
        request: SemanticSearchRequest,
        publishedOnly: Boolean = true,
    ): SemanticSearchResponse = execute(request) { prepared ->
        searchHybridItems(request, prepared, publishedOnly)
    }

    suspend fun searchSemantic(
        request: SemanticSearchRequest,
        publishedOnly: Boolean = true,
    ): SemanticSearchResponse = execute(request) { prepared ->
        searchSemanticItems(request, prepared, publishedOnly)
    }

    suspend fun searchHybridItems(
        request: SemanticSearchRequest,
        prepared: PreparedQuery,
        publishedOnly: Boolean = true,
    ): List<SemanticSearchResultItem> = dal.krithiSearch.searchHybrid(
        rawQuery = prepared.query,
        queryVector = prepared.vector,
        profileId = prepared.profile?.id,
        composerId = request.composerId?.toJavaUuid(),
        ragaId = request.ragaId?.toJavaUuid(),
        limit = request.limit,
        publishedOnly = publishedOnly,
    )

    suspend fun searchSemanticItems(
        request: SemanticSearchRequest,
        prepared: PreparedQuery,
        publishedOnly: Boolean = true,
    ): List<SemanticSearchResultItem> {
        val profile = prepared.profile
        val vector = prepared.vector
        return if (profile == null || vector == null) {
            emptyList()
        } else {
            dal.krithiSearch.searchSemantic(
                queryVector = vector,
                profileId = profile.id,
                composerId = request.composerId?.toJavaUuid(),
                ragaId = request.ragaId?.toJavaUuid(),
                limit = request.limit,
                publishedOnly = publishedOnly,
            )
        }
    }

    private suspend fun execute(
        request: SemanticSearchRequest,
        search: suspend (PreparedQuery) -> List<SemanticSearchResultItem>,
    ): SemanticSearchResponse {
        val query = request.query.trim()
        if (query.isEmpty()) {
            return SemanticSearchResponse(query = query, totalMatches = 0, items = emptyList())
        }
        val items = search(prepareQuery(query))
        return SemanticSearchResponse(query = query, totalMatches = items.size, items = items)
    }

    /**
     * Binds this request to the single active embedding profile and refuses to run when the
     * query embedder would produce vectors from a different model or width than the index holds.
     */
    private suspend fun resolveCompatibleProfile(): EmbeddingProfileRef? {
        val profile = dal.krithiSearch.activeEmbeddingProfile()
        if (profile == null) {
            logger.debug("No active embedding profile; vector retrieval disabled for this request")
            return null
        }
        if (profile.activeCount > 1) {
            logger.warn(
                "{} embedding profiles are active; using newest {} ({}, {} dims). Activate exactly one profile.",
                profile.activeCount, profile.id, profile.modelName, profile.dimensions,
            )
        }
        val compatible = profile.modelName == embeddingClient.modelName &&
            profile.dimensions == embeddingClient.outputDimensionality
        if (!compatible) {
            throw EmbeddingServiceUnavailableException(
                "Active embedding profile ${profile.id} is ${profile.modelName}/${profile.dimensions} but the query " +
                    "embedder is ${embeddingClient.modelName}/${embeddingClient.outputDimensionality}; " +
                    "refusing to mix embedding spaces.",
            )
        }
        return profile
    }

    private suspend fun embedQueryCached(query: String): List<Float> {
        queryVectorCacheMutex.withLock {
            queryVectorCache[query]?.let { return it }
        }
        val vector = embeddingClient.embedQuery(query)
        queryVectorCacheMutex.withLock {
            queryVectorCache[query] = vector
            while (queryVectorCache.size > QUERY_VECTOR_CACHE_SIZE) {
                queryVectorCache.remove(queryVectorCache.keys.first())
            }
        }
        return vector
    }

    private companion object {
        const val QUERY_VECTOR_CACHE_SIZE = 256
    }
}
