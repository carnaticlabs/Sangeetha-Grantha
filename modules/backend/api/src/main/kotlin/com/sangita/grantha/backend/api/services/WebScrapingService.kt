package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.GeminiApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

class WebScrapingService(
    private val geminiClient: GeminiApiClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    // Using a separate client instance for scraping to avoid config conflicts with Gemini client
    private val httpClient = HttpClient(CIO)

    suspend fun scrapeShivkumarKrithi(url: String): ScrapedKrithiMetadata {
        logger.info("Scraping URL: $url")
        val html = try {
            httpClient.get(url).bodyAsText()
        } catch (e: Exception) {
            logger.error("Failed to fetch URL: $url", e)
            throw IllegalArgumentException("Could not fetch URL: $url")
        }

        // Simplistic cleaning to reduce token usage
        val cleanedHtml = html.replace(Regex("<script.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("\\s+"), " ")
            .take(30000) // Truncate if too huge to fit in context window

        val prompt = """
            Analyze the following HTML content from shivkumar.org (or similar Carnatic music archive).
            Extract the Krithi details into a structured JSON format.
            
            HTML Content:
            $cleanedHtml
            
            Extract the following fields:
            - title
            - composer (e.g., Tyagaraja, Dikshitar)
            - raga
            - tala
            - deity (if mentioned)
            - temple (if mentioned)
            - language (if mentioned)
            - lyrics (The full lyrics text, preserving intended line breaks if possible)
            - notation (The musical notation if available)
            
            If a field is not found, use null.
            
            Return ONLY the valid JSON matching the structure.
        """.trimIndent()

        return geminiClient.generateStructured<ScrapedKrithiMetadata>(prompt)
    }
}

@Serializable
data class ScrapedKrithiMetadata(
    val title: String,
    val composer: String? = null,
    val raga: String? = null,
    val tala: String? = null,
    val deity: String? = null,
    val temple: String? = null,
    val language: String? = null,
    val lyrics: String? = null,
    val notation: String? = null
)
