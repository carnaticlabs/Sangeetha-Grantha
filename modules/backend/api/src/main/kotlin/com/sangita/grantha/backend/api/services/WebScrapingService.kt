package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.GeminiApiClient
import com.sangita.grantha.backend.api.clients.GenerationConfig
import com.sangita.grantha.backend.api.services.scraping.HtmlTextExtractor
import com.sangita.grantha.backend.api.services.scraping.ScrapeJsonSanitizer
import com.sangita.grantha.backend.api.services.scraping.ScrapeCache
import com.sangita.grantha.backend.api.services.scraping.KrithiStructureParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import com.sangita.grantha.shared.domain.model.RagaSectionDto
import io.ktor.client.statement.bodyAsText
import io.ktor.network.tls.TlsException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * @deprecated TRACK-096: Kotlin-side HTML scraping is no longer used.
 * All extraction is now handled by the Python extraction worker.
 * This interface and its implementations are retained only for DI wiring
 * backward-compatibility and will be removed in a future cleanup.
 */
@Deprecated("TRACK-096: Extraction now handled by Python worker — this interface is unused")
interface IWebScraper {
    suspend fun scrapeKrithi(url: String): ScrapedKrithiMetadata
}

class WebScrapingServiceImpl(
    private val geminiClient: GeminiApiClient,
    private val templeScrapingService: TempleScrapingService? = null,
    private val cacheTtlHours: Long = 24,
    private val cacheMaxEntries: Long = 500,
    private val useSchemaMode: Boolean = false
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
    private val extractorVersion = "jsoup-v2|blocks-v1"
    private val textExtractor = HtmlTextExtractor(maxChars = 120_000)
    private val textBlocker = KrithiStructureParser()
    private val json = Json { ignoreUnknownKeys = true }
    private val scrapeCache = ScrapeCache<ScrapedKrithiMetadata>(
        ttl = Duration.ofHours(cacheTtlHours),
        maxEntries = cacheMaxEntries,
        cacheName = "krithi-scrape"
    )
    private val inFlight = ConcurrentHashMap<String, Deferred<ScrapedKrithiMetadata>>()

    override suspend fun scrapeKrithi(url: String): ScrapedKrithiMetadata {
        val cacheKey = "$extractorVersion|schema=$useSchemaMode::$url"
        scrapeCache.get(cacheKey)?.let {
            logger.info("Scrape cache hit for {}", url)
            return it
        }

        inFlight[cacheKey]?.let {
            logger.info("Scrape in-flight dedupe hit for {}", url)
            return it.await()
        }

        return coroutineScope {
            val deferred = async { scrapeKrithiInternal(url) }
            val existing = inFlight.putIfAbsent(cacheKey, deferred)
            if (existing != null) {
                deferred.cancel()
                return@coroutineScope existing.await()
            }

            try {
                val result = deferred.await()
                val cacheable = result.warnings
                    ?.none { it == "gemini_failed" || it == "empty_extraction" }
                    ?: true
                if (cacheable) {
                    scrapeCache.put(cacheKey, result)
                }
                result
            } finally {
                inFlight.remove(cacheKey, deferred)
            }
        }
    }

    private suspend fun scrapeKrithiInternal(url: String): ScrapedKrithiMetadata {
        logger.info("Scraping URL: {}", url)
        val extracted = fetchAndExtract(url)
        if (extracted.text.isBlank()) {
            logger.warn("Empty extraction for {}", url)
            return ScrapedKrithiMetadata(
                title = extracted.title ?: url,
                warnings = listOf("empty_extraction")
            )
        }

        val promptBlocks = textBlocker.buildBlocks(extracted.text)
        val detectedSections = textBlocker.extractSections(extracted.text).map { 
            ScrapedSectionDto(type = it.type, text = it.text, label = it.label) 
        }
        val detectedVariants = textBlocker.extractLyricVariants(extracted.text).map { v ->
            ScrapedLyricVariantDto(
                language = v.language,
                script = v.script,
                lyrics = v.lyrics,
                sections = v.sections.map { s -> ScrapedSectionDto(s.type, s.text, s.label) }
            )
        }

        // Ragamalikas require LLM intelligence to identify Raga-based sections which TextBlocker misses.
        val isRagamalika = (extracted.title?.contains("ragamalika", ignoreCase = true) == true) || 
                           (extracted.text.contains("ragamalika", ignoreCase = true))

        val hasDeterministicContent = (detectedSections.isNotEmpty() || detectedVariants.isNotEmpty()) && !isRagamalika
        val structuredText = ScrapingPromptBuilder.buildStructuredText(url, extracted.title, promptBlocks, includeContent = !hasDeterministicContent)

        val prompt = ScrapingPromptBuilder.buildPrompt(structuredText, hasDeterministicContent)

        logger.info(
            "Scrape sizes for {}: extractedChars={}, promptChars={}, detectedSections={}, detectedVariants={}, isRagamalika={}",
            url,
            extracted.text.length,
            prompt.length,
            detectedSections.size,
            detectedVariants.size,
            isRagamalika
        )

        val metadata = try {
            val rawJson = if (useSchemaMode) {
                geminiClient.generateStructuredRaw(
                    prompt,
                    GenerationConfig(
                        responseMimeType = "application/json",
                        responseSchema = ScrapingPromptBuilder.scrapedKrithiSchema()
                    )
                )
            } else {
                geminiClient.generateStructuredRaw(prompt, null)
            }
            val cleanedJson = ScrapeJsonSanitizer.sanitizeScrapedKrithiJson(rawJson)
            json.decodeFromString<ScrapedKrithiMetadata>(cleanedJson)
        } catch (e: Exception) {
            logger.error("Gemini scrape failed for {}. Using deterministic structure only.", url, e)
            ScrapedKrithiMetadata(
                title = extracted.title ?: url,
                warnings = listOf("gemini_failed")
            )
        }

        var postProcessed = metadata
        if (detectedSections.isNotEmpty()) {
            // Prioritize deterministic sections from KrithiStructureParser (now handles Ragamalika sub-sections)
            postProcessed = postProcessed.copy(sections = detectedSections)
        }
        if (detectedVariants.isNotEmpty()) {
            // Prioritize deterministic variants from KrithiStructureParser (scripts are usually reliable even in Ragamalika)
            postProcessed = postProcessed.copy(lyricVariants = detectedVariants)
        }
        
        postProcessed = postProcessMetadata(postProcessed)
        return enrichTempleDetails(url, postProcessed)
    }

    private fun postProcessMetadata(metadata: ScrapedKrithiMetadata): ScrapedKrithiMetadata {
        if (metadata.lyrics.isNullOrBlank() && !metadata.sections.isNullOrEmpty()) {
            val concatenatedLyrics = metadata.sections.joinToString("\n\n") { section ->
                "[${section.type}]\n${section.text}"
            }
            return metadata.copy(lyrics = concatenatedLyrics)
        }
        return metadata
    }

    private suspend fun enrichTempleDetails(url: String, metadata: ScrapedKrithiMetadata): ScrapedKrithiMetadata {
        val templeUrl = metadata.templeUrl
        if (templeScrapingService == null || templeUrl.isNullOrBlank()) {
            logger.info("Skipping temple enrichment: templeScrapingService is null or templeUrl is blank for {}", url)
            return metadata
        }

        return try {
            logger.info("Attempting temple enrichment for {} using URL {}", url, templeUrl)
            val templeDto = templeScrapingService.getTempleDetails(templeUrl) {
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
                metadata.copy(templeDetails = templeDetails)
            } else {
                metadata
            }
        } catch (e: Exception) {
            logger.warn("Failed to scrape nested temple URL via service: {} (source {})", templeUrl, url, e)
            metadata.copy(warnings = (metadata.warnings ?: emptyList()) + "temple_scrape_failed")
        }
    }

    private suspend fun fetchAndExtract(url: String): HtmlTextExtractor.ExtractedContent {
        val html = try {
            httpClient.get(url).bodyAsText()
        } catch (e: TlsException) {
            logger.warn("TLS validation failed for {}. Skipping content fetch.", url, e)
            return HtmlTextExtractor.ExtractedContent(text = "", title = null)
        } catch (e: Exception) {
            logger.error("Failed to fetch URL: $url", e)
             // For nested URLs, we might want to just return empty string instead of throwing, 
             // but fetchAndClean is generic. Exception will be caught in nested block.
            throw IllegalArgumentException("Could not fetch URL: $url")
        }

        return textExtractor.extract(html, url)
    }

    private suspend fun fetchAndClean(url: String): String = fetchAndExtract(url).text
}

/**
 * TRACK-032: One lyric variant per language/script from multi-script pages.
 * @deprecated TRACK-096: Use [com.sangita.grantha.shared.domain.model.import.CanonicalLyricVariantDto] instead.
 * Retained for backward-compatible deserialization of legacy `parsed_payload` values.
 */
@Deprecated("TRACK-096: Use CanonicalLyricVariantDto from shared domain model", replaceWith = ReplaceWith("CanonicalLyricVariantDto", "com.sangita.grantha.shared.domain.model.import.CanonicalLyricVariantDto"))
@Serializable
data class ScrapedLyricVariantDto(
    val language: String,
    val script: String,
    val lyrics: String? = null,
    val sections: List<ScrapedSectionDto>? = null
)

/**
 * @deprecated TRACK-096: Use [com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto] instead.
 * Retained for backward-compatible deserialization of legacy `parsed_payload` values.
 * New extraction pipelines MUST produce CanonicalExtractionDto — do NOT add fields here.
 */
@Deprecated("TRACK-096: Use CanonicalExtractionDto from shared domain model", replaceWith = ReplaceWith("CanonicalExtractionDto", "com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto"))
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
    val templeDetails: ScrapedTempleDetails? = null,
    val warnings: List<String>? = null
)

/**
 * @deprecated TRACK-096: Temple details should be part of CanonicalExtractionDto or a dedicated DTO.
 * Retained for backward-compatible deserialization of legacy `parsed_payload` values.
 */
@Deprecated("TRACK-096: Migrate temple details to CanonicalExtractionDto")
@Serializable
data class ScrapedTempleDetails(
    val name: String,
    val deity: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null
)

/**
 * @deprecated TRACK-096: Use [com.sangita.grantha.shared.domain.model.import.CanonicalSectionDto] instead.
 * Retained for backward-compatible deserialization and as adapter type for StructuralVotingEngine.
 */
@Deprecated("TRACK-096: Use CanonicalSectionDto from shared domain model", replaceWith = ReplaceWith("CanonicalSectionDto", "com.sangita.grantha.shared.domain.model.import.CanonicalSectionDto"))
@Serializable
data class ScrapedSectionDto(
    val type: RagaSectionDto,
    val text: String,
    val label: String? = null
)
