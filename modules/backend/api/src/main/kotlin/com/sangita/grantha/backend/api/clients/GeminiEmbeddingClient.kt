package com.sangita.grantha.backend.api.clients

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class GeminiEmbedContentRequest(
    val model: String,
    val content: Content,
    val taskType: String = "RETRIEVAL_QUERY",
    val outputDimensionality: Int = 768,
)

@Serializable
data class GeminiEmbedContentResponse(
    val embedding: EmbeddingVectorValues,
)

@Serializable
data class EmbeddingVectorValues(
    val values: List<Float>,
)

class EmbeddingServiceUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class GeminiEmbeddingClient(
    private val apiKey: String,
    /** Model used for query vectors; must match the active `embedding_profiles` row or retrieval refuses to run. */
    val modelName: String = "gemini-embedding-2",
    val outputDimensionality: Int = 768,
    private val customEndpoint: String? = null,
    private val httpClient: HttpClient? = null,
    private val allowSyntheticFallback: Boolean = false,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client: HttpClient = httpClient ?: HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@GeminiEmbeddingClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000L
            connectTimeoutMillis = 5_000L
            socketTimeoutMillis = 15_000L
        }
        defaultRequest {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun embedQuery(query: String): List<Float> {
        if (apiKey.isBlank()) {
            if (allowSyntheticFallback) {
                logger.warn("Gemini API key is blank; returning synthetic normalized test vector (allowSyntheticFallback=true)")
                val vec = FloatArray(outputDimensionality) { 0.0f }
                vec[0] = 1.0f // Non-zero unit vector to avoid cosine NaN
                return vec.toList()
            }
            logger.warn("Gemini API key is blank; vector embeddings are unavailable")
            throw EmbeddingServiceUnavailableException("Gemini API key is not configured; vector embeddings are unavailable.")
        }

        val endpoint = customEndpoint
            ?: "https://generativelanguage.googleapis.com/v1beta/models/$modelName:embedContent"

        val requestPayload = GeminiEmbedContentRequest(
            model = "models/$modelName",
            content = Content(parts = listOf(Part(text = query))),
            taskType = "RETRIEVAL_QUERY",
            outputDimensionality = outputDimensionality,
        )

        val response = client.post(endpoint) {
            setBody(requestPayload)
        }

        if (!response.status.isSuccess()) {
            val errBody = response.bodyAsText()
            logger.error("Gemini embedContent failed [${response.status}]: $errBody")
            throw EmbeddingServiceUnavailableException("Gemini embedding API failed with status ${response.status}: $errBody")
        }

        val embedResponse = json.decodeFromString<GeminiEmbedContentResponse>(response.bodyAsText())
        val values = embedResponse.embedding.values
        if (values.size != outputDimensionality) {
            throw IllegalStateException("Gemini embedding returned dimension ${values.size}, expected $outputDimensionality")
        }
        val isFiniteAndNonZero = values.any { it != 0.0f } && values.all { it.isFinite() }
        if (!isFiniteAndNonZero) {
            throw IllegalStateException("Gemini embedding returned all-zero or non-finite vector")
        }
        return values
    }
}
