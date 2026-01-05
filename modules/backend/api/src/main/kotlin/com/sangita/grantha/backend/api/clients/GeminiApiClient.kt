package com.sangita.grantha.backend.api.clients

import com.sangita.grantha.backend.dal.support.DatabaseConfigLoader
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
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

class GeminiApiClient(
    private val apiKey: String
) {
    @PublishedApi
    internal val logger = LoggerFactory.getLogger(javaClass)
    private val client: HttpClient
    
    // Reusable Json instance to avoid redundant creation
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    init {
        logger.info("Initializing GeminiApiClient with key: '${apiKey.take(4)}...' (Length: ${apiKey.length})")
        if (apiKey.isBlank()) {
           logger.warn("GEMINI_API_KEY is not configured or blank. Gemini features will fail.")
        }

        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(this@GeminiApiClient.json)
            }
            defaultRequest {
                // Using gemini-2.0-flash as confirmed by available models list
                url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
                contentType(ContentType.Application.Json)
            }
            expectSuccess = false // We handle errors manually to log the body
        }
    }

    suspend fun generateContent(prompt: String): String {
        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            )
        )

        try {
            val response = client.post("") {
                setBody(request)
            }
            val bodyText = response.bodyAsText()

            if (response.status.value >= 400) {
                logger.error("Gemini API Error: Status ${response.status}, Body: $bodyText")
                throw RuntimeException("Gemini API request failed with status ${response.status}: $bodyText")
            }

            val parsedResponse = json.decodeFromString<GeminiResponse>(bodyText)
            return parsedResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()
        } catch (e: Exception) {
            logger.error("Failed to generate content from Gemini", e)
            throw e
        }
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
        """.trimIndent()

        val rawJson = generateContent(jsonPrompt)
        
        // Clean up markdown code blocks if present (despite instructions, LLMs often add them)
        val cleanedJson = rawJson.replace("```json", "").replace("```", "").trim()

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
