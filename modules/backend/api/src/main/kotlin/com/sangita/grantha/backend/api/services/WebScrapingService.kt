package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.GeminiApiClient
import com.sangita.grantha.backend.api.clients.GenerationConfig
import com.sangita.grantha.backend.api.services.scraping.HtmlTextExtractor
import com.sangita.grantha.backend.api.services.scraping.ScrapeJsonSanitizer
import com.sangita.grantha.backend.api.services.scraping.ScrapeCache
import com.sangita.grantha.backend.api.services.scraping.TextBlocker
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.slf4j.LoggerFactory

interface IWebScraper {
    /**
     * Scrape and parse a krithi page into structured metadata.
     */
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
    private val textBlocker = TextBlocker()
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
        val structuredText = buildStructuredText(url, extracted.title, promptBlocks)
        val detectedSections = textBlocker.extractSections(extracted.text).map { 
            ScrapedSectionDto(type = it.type, text = it.text) 
        }

        val prompt = buildPrompt(structuredText, detectedSections.isNotEmpty())

        logger.info(
            "Scrape sizes for {}: extractedChars={}, promptChars={}, detectedSections={}",
            url,
            extracted.text.length,
            prompt.length,
            detectedSections.size
        )

        val metadata = try {
            val rawJson = if (useSchemaMode) {
                geminiClient.generateStructuredRaw(
                    prompt,
                    GenerationConfig(
                        responseMimeType = "application/json",
                        responseSchema = scrapedKrithiSchema()
                    )
                )
            } else {
                geminiClient.generateStructuredRaw(prompt, null)
            }
            val cleanedJson = ScrapeJsonSanitizer.sanitizeScrapedKrithiJson(rawJson)
            json.decodeFromString<ScrapedKrithiMetadata>(cleanedJson)
        } catch (e: Exception) {
            logger.error("Gemini scrape failed for {}. Returning partial metadata.", url, e)
            return ScrapedKrithiMetadata(
                title = extracted.title ?: url,
                warnings = listOf("gemini_failed")
            )
        }

        var postProcessed = metadata
        if (detectedSections.isNotEmpty()) {
            // Prioritize deterministic sections from TextBlocker
            postProcessed = postProcessed.copy(sections = detectedSections)
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

    private fun buildStructuredText(
        url: String,
        title: String?,
        promptBlocks: TextBlocker.PromptBlocks
    ): String {
        val builder = StringBuilder()
        builder.appendLine("=== SOURCE URL ===")
        builder.appendLine(url)
        if (!title.isNullOrBlank()) {
            builder.appendLine()
            builder.appendLine("=== PAGE TITLE ===")
            builder.appendLine(title)
        }
        builder.appendLine()
        builder.appendLine("=== HEADER META ===")
        if (promptBlocks.metaLines.isNotEmpty()) {
            builder.appendLine(promptBlocks.metaLines.joinToString("\n"))
        } else {
            builder.appendLine("(none)")
        }
        builder.appendLine()
        builder.appendLine("=== CONTENT BLOCKS ===")
        if (promptBlocks.blocks.isEmpty()) {
            builder.appendLine("(none)")
        } else {
            promptBlocks.blocks.forEach { block ->
                builder.appendLine("=== ${block.label} ===")
                builder.appendLine(block.lines.joinToString("\n"))
                builder.appendLine()
            }
        }
        return builder.toString().trim()
    }

    private fun buildPrompt(structuredText: String, hasDeterministicSections: Boolean): String {
        val sectionInstruction = if (hasDeterministicSections) {
            "Sections have already been deterministically extracted. Focus on extracting METADATA and OTHER DETAILS (Meaning, Temple, Composer, Raga, Tala) from the text."
        } else {
            "Extract sections (Pallavi, etc.) and metadata from the text."
        }

        return """
            Extract Carnatic krithi metadata from the structured text below.
            Keep null for missing fields. Preserve line breaks in lyrics with \\n.

            Extraction Scope:
            - $sectionInstruction
            - If explicit headers are absent, infer Pallavi then Anupallavi then Charanam from stanza order.
            - If sections are found, populate the 'sections' array.

            Value-Added Enrichment:
            - Identify 'temple' (shrine/kshetra) and 'deity' (primary god/goddess) mentioned.
            - Extract 'raga' and 'tala' from the text. **Check the === HEADER META === section first**, as it often contains these details (e.g., "rAgaM ... tALaM ...").
            - Provide a concise 'meaning' or 'gist' if present in the text.

            TRACK-032 Multi-language lyric extraction:
            - If multiple scripts/languages exist, populate lyricVariants with one entry per block.
            - language codes: SA, TA, TE, KN, ML, HI, EN
            - script codes: devanagari, tamil, telugu, kannada, malayalam, latin

            Extract:
            - title, composer, raga, tala, deity, temple, templeUrl, language, lyrics, notation, sections, lyricVariants

            Structured Content:
            $structuredText
        """.trimIndent()
    }

    private fun scrapedKrithiSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put(
            "properties",
            buildJsonObject {
                put("title", stringSchema())
                put("composer", nullableStringSchema())
                put("raga", nullableStringSchema())
                put("tala", nullableStringSchema())
                put("deity", nullableStringSchema())
                put("temple", nullableStringSchema())
                put("templeUrl", nullableStringSchema())
                put("language", nullableStringSchema())
                put("lyrics", nullableStringSchema())
                put("notation", nullableStringSchema())
                put("sections", nullableArraySchema(sectionSchema()))
                put("lyricVariants", nullableArraySchema(lyricVariantSchema()))
                put("templeDetails", nullableObjectSchema(templeDetailsSchema()))
                put("warnings", nullableArraySchema(stringSchema()))
            }
        )
        put("required", JsonArray(listOf(JsonPrimitive("title"))))
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun sectionSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put(
            "properties",
            buildJsonObject {
                put("type", JsonPrimitive("string"))
                put("text", JsonPrimitive("string"))
            }
        )
        put("required", JsonArray(listOf(JsonPrimitive("type"), JsonPrimitive("text"))))
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun lyricVariantSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put(
            "properties",
            buildJsonObject {
                put("language", JsonPrimitive("string"))
                put("script", JsonPrimitive("string"))
                put("lyrics", nullableStringSchema())
                put("sections", nullableArraySchema(sectionSchema()))
            }
        )
        put("required", JsonArray(listOf(JsonPrimitive("language"), JsonPrimitive("script"))))
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun templeDetailsSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put(
            "properties",
            buildJsonObject {
                put("name", JsonPrimitive("string"))
                put("deity", nullableStringSchema())
                put("location", nullableStringSchema())
                put("latitude", nullableNumberSchema())
                put("longitude", nullableNumberSchema())
                put("description", nullableStringSchema())
            }
        )
        put("required", JsonArray(listOf(JsonPrimitive("name"))))
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun stringSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("string"))
    }

    private fun nullableStringSchema(): JsonObject = buildJsonObject {
        put("type", buildJsonArray { add(JsonPrimitive("string")); add(JsonPrimitive("null")) })
    }

    private fun nullableNumberSchema(): JsonObject = buildJsonObject {
        put("type", buildJsonArray { add(JsonPrimitive("number")); add(JsonPrimitive("null")) })
    }

    private fun nullableObjectSchema(schema: JsonObject): JsonObject = buildJsonObject {
        put("type", buildJsonArray { add(JsonPrimitive("object")); add(JsonPrimitive("null")) })
        put("properties", schema["properties"] ?: JsonObject(emptyMap()))
        put("required", schema["required"] ?: JsonArray(emptyList()))
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun nullableArraySchema(itemSchema: JsonObject): JsonObject = buildJsonObject {
        put("type", buildJsonArray { add(JsonPrimitive("array")); add(JsonPrimitive("null")) })
        put("items", itemSchema)
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
    val templeDetails: ScrapedTempleDetails? = null,
    val warnings: List<String>? = null
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
