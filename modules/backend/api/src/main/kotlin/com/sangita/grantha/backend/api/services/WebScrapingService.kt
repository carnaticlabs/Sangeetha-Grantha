package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.GeminiApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import com.sangita.grantha.shared.domain.model.RagaSectionDto
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

interface IWebScraper {
    /**
     * Scrape and parse a krithi page into structured metadata.
     */
    suspend fun scrapeKrithi(url: String): ScrapedKrithiMetadata
}

class WebScrapingServiceImpl(
    private val geminiClient: GeminiApiClient
) : IWebScraper {
    private val logger = LoggerFactory.getLogger(javaClass)
    // Using a separate client instance for scraping to avoid config conflicts with Gemini client
    private val httpClient = HttpClient(CIO)

    override suspend fun scrapeKrithi(url: String): ScrapedKrithiMetadata {
        logger.info("Scraping URL: $url")
        val html = try {
            httpClient.get(url).bodyAsText()
        } catch (e: Exception) {
            logger.error("Failed to fetch URL: $url", e)
            throw IllegalArgumentException("Could not fetch URL: $url")
        }

        // Simplistic cleaning to reduce token usage but preserve structure (newlines)
        val cleanedHtml = html.replace(Regex("<script.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n") // Convert <br> to newline
            .replace(Regex("<p.*?>", RegexOption.IGNORE_CASE), "\n") // Convert <p> to newline
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n") // partial block structure
            .replace(Regex("<.*?>", RegexOption.DOT_MATCHES_ALL), " ") // Strip other tags
            .replace(Regex("[ \\t]+"), " ") // Collapse spaces/tabs but NOT newlines
            .replace(Regex("\\n\\s*\\n+"), "\n\n") // Max 2 newlines
            .take(30000) // Truncate if too huge to fit in context window

        val prompt = """
            Analyze the following text content extracted from a Carnatic music webpage.
            Extract the Krithi details into a structured JSON format.
            
            Text Content:
            $cleanedHtml
            
            Instructions for Section Extraction:
            - Look for keywords like "Pallavi", "Anupallavi", "Charanam", "Samashti Charanam", "Chittaswaram", "Madhyamakala", "Muktayi Swara", "Ettugada", "Solkattu", "Anubandha" (case insensitive) to identify sections.
            - The text immediately following these headers belongs to that section.
            - If explicit section headers are NOT found, assume the first stanza/paragraph of the lyrics is the PALLAVI, the second might be ANUPALLAVI (if distinct), etc.
            - If the lyrics are presented as a list of links (e.g., [Word](...)), treat the link text as the lyrics.
            - If sections are found, populating the 'sections' array is MANDATORY.
            
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
            - sections: A list of objects with 'type' (some valid values: PALLAVI, ANUPALLAVI, CHARANAM, SAMASHTI_CHARANAM, CHITTASWARAM, SWARA_SAHITYA, MADHYAMA_KALA, SOLKATTU_SWARA, ANUBANDHA, MUKTAYI_SWARA, ETTUGADA_SWARA, ETTUGADA_SAHITYA, VILOMA_CHITTASWARAM, OTHER) and 'text'.
            
            If a field is not found, use null.
            
            Return ONLY the valid JSON matching the structure.
        """.trimIndent()

        val metadata = geminiClient.generateStructured<ScrapedKrithiMetadata>(prompt)
        
        // Post-processing to ensure lyrics is populated
        return if (metadata.lyrics.isNullOrBlank() && !metadata.sections.isNullOrEmpty()) {
            val concatenatedLyrics = metadata.sections.joinToString("\n\n") { section ->
                "[${section.type}]\n${section.text}"
            }
            metadata.copy(lyrics = concatenatedLyrics)
        } else {
            metadata
        }
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
    val notation: String? = null,
    val sections: List<ScrapedSectionDto>? = null
)

@Serializable
data class ScrapedSectionDto(
    val type: RagaSectionDto,
    val text: String
)
