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

class GeminiApiClient {
    @PublishedApi
    internal val logger = LoggerFactory.getLogger(javaClass)
    private val apiKey: String
    private val client: HttpClient

    init {
        // Simple loading strategy similar to DatabaseConfigLoader
        // In a real app we might want to extend ConfigLoader to support generic keys,
        // but for now we look for env var or fallback.
        val env = System.getenv()
        apiKey = env["SG_GEMINI_API_KEY"]
            ?: env["GEMINI_API_KEY"]
            ?: throw IllegalStateException("GEMINI_API_KEY not configured")

        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    prettyPrint = true
                })
            }
            defaultRequest {
                url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey")
                contentType(ContentType.Application.Json)
            }
        }
    }

    suspend fun generateContent(prompt: String): String {
        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            )
        )

        try {
            val response: GeminiResponse = client.post("") {
                setBody(request)
            }.body()
            return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()
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
            Json { ignoreUnknownKeys = true }.decodeFromString<T>(cleanedJson)
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
    val index: Int
)
