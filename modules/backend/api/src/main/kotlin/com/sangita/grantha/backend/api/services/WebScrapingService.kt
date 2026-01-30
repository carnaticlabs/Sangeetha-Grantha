package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.GeminiApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
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
    private val geminiClient: GeminiApiClient,
    private val templeScrapingService: TempleScrapingService
) : IWebScraper {
    private val logger = LoggerFactory.getLogger(javaClass)
    // Using a separate client instance for scraping to avoid config conflicts with Gemini client
    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000  // 60 seconds for webpage fetching
            connectTimeoutMillis = 30_000  // 30 seconds to establish connection
            socketTimeoutMillis = 45_000   // 45 seconds between data packets
        }
    }

    override suspend fun scrapeKrithi(url: String): ScrapedKrithiMetadata {
        logger.info("Scraping URL: $url")
        val html = fetchAndClean(url)

        val prompt = """
            Analyze the following text content extracted from a Carnatic music webpage.
            Extract the Krithi details into a structured JSON format.
            
            Text Content:
            $html
            
            Instructions for Section Extraction:
            - Look for keywords like "Pallavi", "Anupallavi", "Charanam", "Samashti Charanam", "Chittaswaram", "Madhyamakala", "Muktayi Swara", "Ettugada", "Solkattu", "Anubandha" (case insensitive) to identify sections.
            - The text immediately following these headers belongs to that section.
            - If explicit section headers are NOT found, assume the first stanza/paragraph of the lyrics is the PALLAVI, the second might be ANUPALLAVI (if distinct), etc.
            - If the lyrics are presented as a list of links (e.g., [Word](...)), treat the link text as the lyrics.
            - If sections are found, populating the 'sections' array is MANDATORY.
            
            TRACK-032 Multi-language lyric extraction:
            - If the page contains lyrics in multiple languages or scripts (e.g. English, Devanagari, Tamil, Telugu, Kannada, Malayalam), also populate 'lyricVariants' with one object per distinct language/script block.
            - Each lyricVariant must have: 'language' (use codes: SA, TA, TE, KN, ML, HI, EN), 'script' (use: devanagari, tamil, telugu, kannada, malayalam, latin), 'lyrics' (full text for that script), and optionally 'sections' (same structure as top-level sections) if that script has section labels.
            - If only one language/script is present, you may still set lyricVariants with a single entry, or leave lyricVariants null and use the top-level 'lyrics' and 'sections'.
            
            Extract the following fields:
            - title
            - composer (e.g., Tyagaraja, Dikshitar)
            - raga
            - tala
            - deity (if mentioned in lyrics or header)
            - temple (if mentioned in lyrics or header)
            - templeUrl (Look for explicit links to external temple info pages like templenet.com, wikipedia, etc. within the content)
            - language (if mentioned; primary language)
            - lyrics (The full lyrics text for the primary/first script, preserving intended line breaks if possible)
            - notation (The musical notation if available)
            - sections: A list of objects with 'type' (some valid values: PALLAVI, ANUPALLAVI, CHARANAM, SAMASHTI_CHARANAM, CHITTASWARAM, SWARA_SAHITYA, MADHYAMA_KALA, SOLKATTU_SWARA, ANUBANDHA, MUKTAYI_SWARA, ETTUGADA_SWARA, ETTUGADA_SAHITYA, VILOMA_CHITTASWARAM, OTHER) and 'text'.
            - lyricVariants: (optional) List of { "language": "SA"|"TA"|"TE"|"KN"|"ML"|"HI"|"EN", "script": "devanagari"|"tamil"|"telugu"|"kannada"|"malayalam"|"latin", "lyrics": "...", "sections": [...] }.
            
            If a field is not found, use null.
            
            Return ONLY the valid JSON matching the structure.
        """.trimIndent()

        var metadata = geminiClient.generateStructured<ScrapedKrithiMetadata>(prompt)
        
        // Post-processing to ensure lyrics is populated
        if (metadata.lyrics.isNullOrBlank() && !metadata.sections.isNullOrEmpty()) {
            val concatenatedLyrics = metadata.sections.joinToString("\n\n") { section ->
                "[${section.type}]\n${section.text}"
            }
            metadata = metadata.copy(lyrics = concatenatedLyrics)
        }

        // Nested scraping for Temple Details using Dedicated Service
        if (!metadata.templeUrl.isNullOrBlank()) {
             try {
                val templeDto = templeScrapingService.getTempleDetails(metadata.templeUrl) {
                    fetchAndClean(it)
                }
                
                if (templeDto != null) {
                    val templeDetails = ScrapedTempleDetails(
                        name = templeDto.templeName,
                        deity = templeDto.deityName,
                        location = templeDto.city ?: templeDto.kshetraText,
                        latitude = templeDto.latitude,
                        longitude = templeDto.longitude,
                        description = templeDto.notes
                    )
                    metadata = metadata.copy(templeDetails = templeDetails)
                }
             } catch (e: Exception) {
                 logger.warn("Failed to scrape nested temple URL via service: ${metadata.templeUrl}", e)
                 // Continue without failing the main scrape
             }
        }
        
        return metadata
    }

    private suspend fun fetchAndClean(url: String): String {
        val html = try {
            httpClient.get(url).bodyAsText()
        } catch (e: Exception) {
            logger.error("Failed to fetch URL: $url", e)
             // For nested URLs, we might want to just return empty string instead of throwing, 
             // but fetchAndClean is generic. Exception will be caught in nested block.
            throw IllegalArgumentException("Could not fetch URL: $url")
        }

        return html.replace(Regex("<script.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<p.*?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
            // Preserve links for the LLM to extract URLs
            .replace(Regex("<a\\b[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " $2 ($1) ")
            .replace(Regex("<.*?>", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n\\s*\\n+"), "\n\n")
            .take(30000)
    }
}

/** TRACK-032: One lyric variant per language/script from multi-script pages. */
@Serializable
data class ScrapedLyricVariantDto(
    val language: String,
    val script: String,
    val lyrics: String? = null,
    val sections: List<ScrapedSectionDto>? = null
)

@Serializable
data class ScrapedKrithiMetadata(
    val title: String,
    val composer: String? = null,
    val raga: String? = null,
    val tala: String? = null,
    val deity: String? = null,
    val temple: String? = null,
    val templeUrl: String? = null,
    val language: String? = null,
    val lyrics: String? = null,
    val notation: String? = null,
    val sections: List<ScrapedSectionDto>? = null,
    val lyricVariants: List<ScrapedLyricVariantDto>? = null,
    val templeDetails: ScrapedTempleDetails? = null
)

@Serializable
data class ScrapedTempleDetails(
    val name: String,
    val deity: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null
)

@Serializable
data class ScrapedSectionDto(
    val type: RagaSectionDto,
    val text: String
)

