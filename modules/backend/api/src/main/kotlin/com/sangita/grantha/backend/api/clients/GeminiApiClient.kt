package com.sangita.grantha.backend.api.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GeminiApiClient(
    private val apiKey: String,
    private val modelUrl: String,
    private val minIntervalMs: Long = 10_000L
) {
    @PublishedApi
    internal val logger = LoggerFactory.getLogger(javaClass)
    private val client: HttpClient
    private val rateLimitMutex = Mutex()
    @Volatile
    private var lastCallTimeMs: Long = 0L
    
    // Reusable Json instance to avoid redundant creation
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    init {
        logger.info("Initializing GeminiApiClient (keyConfigured={}, modelUrl={})", apiKey.isNotBlank(), modelUrl)
        if (apiKey.isBlank()) {
           logger.warn("GEMINI_API_KEY is not configured or blank. Gemini features will fail.")
        }

        client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 90_000  // 90 seconds for AI processing
                connectTimeoutMillis = 30_000  // 30 seconds to establish connection
                socketTimeoutMillis = 60_000   // 60 seconds between data packets
            }
            install(ContentNegotiation) {
                json(this@GeminiApiClient.json)
            }
            defaultRequest {
                url(modelUrl)
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
            }
            expectSuccess = false // We handle errors manually to log the body
        }
    }

    /**
     * Runs the block with exclusive access to Gemini: only one request at a time,
     * and at least minIntervalMs between the end of one request and the start of the next.
     * Prevents 429 RESOURCE_EXHAUSTED when multiple scrape workers run concurrently.
     */
    private suspend fun <T> withGeminiRateLimit(block: suspend () -> T): T {
        if (minIntervalMs <= 0) return block()
        return rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastCallTimeMs
            if (lastCallTimeMs > 0 && elapsed < minIntervalMs) {
                val waitMs = minIntervalMs - elapsed
                logger.debug("Gemini rate limit: waiting {}ms", waitMs)
                delay(waitMs)
            }
            try {
                block()
            } finally {
                lastCallTimeMs = System.currentTimeMillis()
            }
        }
    }

    suspend fun generateContent(prompt: String): String {
        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            )
        )

        var currentDelay = 2000L
        val maxRetries = 5

        for (attempt in 1..maxRetries) {
            try {
                // Rate limit applies to the actual network call
                val response = withGeminiRateLimit {
                    client.post("") {
                        setBody(request)
                    }
                }
                val bodyText = response.bodyAsText()

                if (response.status.value == 429 || response.status.value == 503) {
                    if (attempt == maxRetries) {
                        logger.error("Gemini API Retries Exhausted. Last Status: ${response.status}, Body: $bodyText")
                        throw RuntimeException("Gemini API request failed after $maxRetries attempts. Status ${response.status}: $bodyText")
                    }
                    val jitter = (Math.random() * 1000).toLong()
                    val waitTime = currentDelay + jitter
                    logger.warn("Gemini API Error: Status ${response.status} (Attempt $attempt/$maxRetries). Retrying in ${waitTime}ms. Error: $bodyText")
                    delay(waitTime)
                    currentDelay *= 2
                    continue
                }

                if (response.status.value >= 400) {
                    logger.error("Gemini API Error: Status ${response.status}, Body: $bodyText")
                    throw RuntimeException("Gemini API request failed with status ${response.status}: $bodyText")
                }

                val parsedResponse = json.decodeFromString<GeminiResponse>(bodyText)
                return parsedResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()
            } catch (e: Exception) {
                // Check if it's a network/connection error that we should retry?
                // For now, only retrying on explicit 429/503 responses as requested.
                // Re-throwing other exceptions.
                logger.error("Failed to generate content from Gemini", e)
                throw e
            }
        }
        throw RuntimeException("Unreachable code")
    }

    /**
     * Escapes literal newlines and carriage returns inside double-quoted JSON string values
     * so the result is valid JSON. LLMs sometimes emit multi-line strings which break parsing.
     * Public so that the public inline generateStructured can call it.
     */
    fun escapeNewlinesInsideJsonStrings(input: String): String {
        val out = StringBuilder(input.length + 64)
        var i = 0
        var inString = false
        var escaped = false
        while (i < input.length) {
            val c = input[i]
            when {
                !inString -> {
                    if (c == '"') inString = true
                    out.append(c)
                    escaped = false
                }
                escaped -> {
                    out.append(c)
                    escaped = false
                }
                c == '\\' -> {
                    out.append(c)
                    escaped = true
                }
                c == '"' -> {
                    inString = false
                    out.append(c)
                }
                c == '\n' || c == '\r' -> {
                    out.append("\\n")
                    if (c == '\r' && i + 1 < input.length && input[i + 1] == '\n') i++
                }
                else -> out.append(c)
            }
            i++
        }
        return out.toString()
    }

    /**
     * Generates structured content by appending instructions for JSON output.
     * T is the target Serializable class.
     */
    suspend inline fun <reified T> generateStructured(prompt: String): T {
        val jsonPrompt = """
            $prompt
            
            IMPORTANT: Response must be valid JSON matching the structure of the requested object.
            Do not wrap in markdown code blocks. Return ONLY the raw JSON string.
            In string fields (lyrics, text, etc.) use escaped newlines \\n for line breaks, not literal newline characters.
        """.trimIndent()

        val rawJson = generateContent(jsonPrompt)
        
        // Clean up markdown code blocks if present (despite instructions, LLMs often add them)
        var cleanedJson = rawJson.replace("```json", "").replace("```", "").trim()
        // Fix literal newlines inside JSON string values (Gemini sometimes emits these for lyrics/text)
        cleanedJson = escapeNewlinesInsideJsonStrings(cleanedJson)

        return try {
            json.decodeFromString<T>(cleanedJson)
        } catch (e: Exception) {
            logger.error("Failed to parse structured response. Raw content: $cleanedJson", e)
            throw e
        }
    }
}

@Serializable
data class GeminiRequest(
    val contents: List<Content>
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String = "user"
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: String? = null,
    val index: Int = 0
)
