package com.sangita.grantha.backend.api.clients

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory

class GeminiApiClient(
    private val apiKey: String,
    private val modelUrl: String,
    private val minIntervalMs: Long = 10_000L,
    private val qpsLimit: Double = 0.1,
    private val maxConcurrent: Int = 1,
    private val maxRetries: Int = 5,
    private val maxRetryWindowMs: Long = 120_000L,
    private val requestTimeoutMs: Long = 90_000L,
    private val fallbackModelUrl: String? = null,
    private val useSchemaMode: Boolean = false,
    private val cooldownMaxMultiplier: Double = 8.0,
    private val cooldownDecay: Double = 0.9,
    private val jitterMs: Long = 750L
) {
    @PublishedApi
    internal val logger = LoggerFactory.getLogger(javaClass)
    private val client: HttpClient
    private val rateLimiter = GeminiRateLimiter(
        minIntervalMs = minIntervalMs,
        qpsLimit = qpsLimit,
        maxConcurrent = maxConcurrent,
        cooldownMaxMultiplier = cooldownMaxMultiplier,
        cooldownDecay = cooldownDecay,
        jitterMs = jitterMs
    )

    private val requestCounter = AtomicLong(0L)
    private val successCount = LongAdder()
    private val throttled429Count = LongAdder()
    private val throttled503Count = LongAdder()
    private val failureCount = LongAdder()
    private val totalLatencyMs = LongAdder()

    val schemaModeEnabled: Boolean
        get() = useSchemaMode

    // Reusable Json instance to avoid redundant creation
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    init {
        logger.info(
            "Initializing GeminiApiClient (keyConfigured={}, modelUrl={}, qpsLimit={}, maxConcurrent={}, schemaMode={})",
            apiKey.isNotBlank(),
            modelUrl,
            qpsLimit,
            maxConcurrent,
            useSchemaMode
        )
        if (apiKey.isBlank()) {
            logger.warn("GEMINI_API_KEY is not configured or blank. Gemini features will fail.")
        }

        client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = requestTimeoutMs
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
            }
            install(ContentNegotiation) {
                json(this@GeminiApiClient.json)
            }
            defaultRequest {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
            }
            expectSuccess = false
        }
    }

    suspend fun generateContent(prompt: String): String {
        return generateContentWithFallback(prompt, generationConfig = null)
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
        val cleanedJson = generateStructuredRaw(prompt, generationConfig = null)

        return try {
            json.decodeFromString<T>(cleanedJson)
        } catch (e: Exception) {
            logger.error("Failed to parse structured response. Raw content: $cleanedJson", e)
            throw e
        }
    }

    /**
     * Schema-enforced JSON response mode (when supported by the model endpoint).
     * Falls back to prompt-only JSON if schema mode fails or is disabled.
     */
    suspend inline fun <reified T> generateStructuredSchema(prompt: String, schema: JsonObject): T {
        if (!schemaModeEnabled) return generateStructured(prompt)

        val generationConfig = GenerationConfig(
            responseMimeType = "application/json",
            responseSchema = schema
        )

        return try {
            val cleanedJson = generateStructuredRaw(prompt, generationConfig)
            json.decodeFromString<T>(cleanedJson)
        } catch (e: Exception) {
            logger.warn("Schema mode failed; falling back to prompt-only JSON.", e)
            generateStructured(prompt)
        }
    }

    @PublishedApi
    internal suspend fun generateStructuredRaw(prompt: String, generationConfig: GenerationConfig?): String {
        val jsonPrompt = """
            $prompt

            IMPORTANT: Response must be valid JSON matching the structure of the requested object.
            Do not wrap in markdown code blocks. Return ONLY the raw JSON string.
            In string fields (lyrics, text, etc.) use escaped newlines \\n for line breaks, not literal newline characters.
        """.trimIndent()

        val rawJson = generateContentWithFallback(jsonPrompt, generationConfig)
        var cleanedJson = rawJson.replace("```json", "").replace("```", "").trim()
        cleanedJson = escapeNewlinesInsideJsonStrings(cleanedJson)
        return cleanedJson
    }

    @PublishedApi
    internal suspend fun generateContentWithFallback(
        prompt: String,
        generationConfig: GenerationConfig?
    ): String {
        val requestId = nextRequestId()
        return try {
            generateContentInternal(prompt, generationConfig, modelUrl, requestId)
        } catch (e: GeminiThrottleException) {
            val fallback = fallbackModelUrl?.takeIf { it.isNotBlank() && it != modelUrl }
            if (fallback != null) {
                logger.warn("Gemini throttled on primary model. Falling back to {}.", fallback)
                generateContentInternal(prompt, generationConfig, fallback, requestId + "-fb")
            } else {
                throw e
            }
        }
    }

    private suspend fun generateContentInternal(
        prompt: String,
        generationConfig: GenerationConfig?,
        targetUrl: String,
        requestId: String
    ): String {
        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = generationConfig
        )

        val startTimeMs = System.currentTimeMillis()
        var attempt = 0
        var currentDelayMs = 2000L
        val maxDelayMs = 30_000L

        while (true) {
            attempt += 1
            val permit = rateLimiter.acquire()
            val limiterSnapshot = permit.snapshot
            logger.info(
                "Gemini request {} attempt {}/{} waitMs={} promptChars={} qps={} cooldown={} inFlight={}",
                requestId,
                attempt,
                maxRetries,
                permit.waitMs,
                prompt.length,
                limiterSnapshot.qpsLimit,
                "%.2f".format(limiterSnapshot.cooldownMultiplier),
                limiterSnapshot.inFlight
            )

            val callStartMs = System.currentTimeMillis()
            var released = false
            val response = try {
                client.post(targetUrl) {
                    timeout { requestTimeoutMillis = requestTimeoutMs }
                    setBody(request)
                }
            } catch (e: Exception) {
                rateLimiter.release()
                released = true
                if (isRetriableException(e) && shouldRetry(attempt, startTimeMs)) {
                    val waitMs = jitteredDelay(currentDelayMs)
                    logger.warn(
                        "Gemini network error for {} (attempt {}/{}). Retrying in {}ms.",
                        requestId,
                        attempt,
                        maxRetries,
                        waitMs,
                        e
                    )
                    delay(waitMs)
                    currentDelayMs = min(currentDelayMs * 2, maxDelayMs)
                    continue
                }
                failureCount.increment()
                logger.error("Gemini request failed for {}.", requestId, e)
                throw e
            } finally {
                if (!released) {
                    rateLimiter.release()
                }
            }
            val bodyText = response.bodyAsText()
            val latencyMs = System.currentTimeMillis() - callStartMs

            when (response.status.value) {
                429 -> {
                    throttled429Count.increment()
                    handleThrottle(
                        requestId,
                        attempt,
                        response.status.value,
                        bodyText,
                        currentDelayMs,
                        startTimeMs
                    )
                    val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                    rateLimiter.onThrottle(retryAfterMs)
                    if (!shouldRetry(attempt, startTimeMs)) {
                        failureCount.increment()
                        throw GeminiThrottleException("Gemini 429 after retries", 429)
                    }
                    val waitMs = retryAfterMs ?: jitteredDelay(currentDelayMs)
                    logger.warn(
                        "Gemini 429 for {} (attempt {}/{}). Retrying in {}ms.",
                        requestId,
                        attempt,
                        maxRetries,
                        waitMs
                    )
                    delay(waitMs)
                    currentDelayMs = min(currentDelayMs * 2, maxDelayMs)
                    continue
                }
                503 -> {
                    throttled503Count.increment()
                    handleThrottle(
                        requestId,
                        attempt,
                        response.status.value,
                        bodyText,
                        currentDelayMs,
                        startTimeMs
                    )
                    val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                    rateLimiter.onThrottle(retryAfterMs)
                    if (!shouldRetry(attempt, startTimeMs)) {
                        failureCount.increment()
                        throw GeminiThrottleException("Gemini 503 after retries", 503)
                    }
                    val waitMs = retryAfterMs ?: jitteredDelay(currentDelayMs)
                    logger.warn(
                        "Gemini 503 for {} (attempt {}/{}). Retrying in {}ms.",
                        requestId,
                        attempt,
                        maxRetries,
                        waitMs
                    )
                    delay(waitMs)
                    currentDelayMs = min(currentDelayMs * 2, maxDelayMs)
                    continue
                }
            }

            if (response.status.value >= 400) {
                failureCount.increment()
                logger.error(
                    "Gemini API error for {}. Status={}, Body={}",
                    requestId,
                    response.status,
                    truncate(bodyText)
                )
                throw RuntimeException("Gemini API request failed with status ${response.status}")
            }

            val parsedResponse = json.decodeFromString<GeminiResponse>(bodyText)
            val content = parsedResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()

            successCount.increment()
            totalLatencyMs.add(latencyMs)
            rateLimiter.onSuccess()

            logger.info(
                "Gemini success {} status={} latencyMs={} avgLatencyMs={} successCount={} 429s={} 503s={}",
                requestId,
                response.status.value,
                latencyMs,
                averageLatencyMs(),
                successCount.sum(),
                throttled429Count.sum(),
                throttled503Count.sum()
            )

            return content
        }
    }

    private fun shouldRetry(attempt: Int, startTimeMs: Long): Boolean {
        if (attempt >= maxRetries) return false
        return System.currentTimeMillis() - startTimeMs < maxRetryWindowMs
    }

    private fun handleThrottle(
        requestId: String,
        attempt: Int,
        status: Int,
        bodyText: String,
        currentDelayMs: Long,
        startTimeMs: Long
    ) {
        val elapsed = System.currentTimeMillis() - startTimeMs
        logger.warn(
            "Gemini throttle {} status={} attempt={}/{} elapsedMs={} nextDelayMs={} body={}",
            requestId,
            status,
            attempt,
            maxRetries,
            elapsed,
            currentDelayMs,
            truncate(bodyText)
        )
    }

    private fun parseRetryAfterMs(header: String?): Long? {
        val value = header?.trim() ?: return null
        return value.toLongOrNull()?.let { seconds -> seconds * 1000L }
    }

    private fun jitteredDelay(baseDelayMs: Long): Long {
        val jitter = Random.nextLong(0, 1000L)
        return baseDelayMs + jitter
    }

    private fun isRetriableException(e: Exception): Boolean {
        return e is IOException ||
            e is SocketTimeoutException ||
            e is ConnectException ||
            e is UnknownHostException
    }

    private fun truncate(body: String, maxChars: Int = 1200): String {
        return if (body.length <= maxChars) body else body.take(maxChars) + "..."
    }

    private fun averageLatencyMs(): Long {
        val success = successCount.sum()
        if (success == 0L) return 0L
        return totalLatencyMs.sum() / success
    }

    private fun nextRequestId(): String = "gemini-${requestCounter.incrementAndGet()}"
}

class GeminiThrottleException(message: String, val status: Int) : RuntimeException(message)

@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null
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
