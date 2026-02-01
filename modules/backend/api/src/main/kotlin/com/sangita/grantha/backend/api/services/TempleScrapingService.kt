package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.GeminiApiClient
import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.repositories.TempleSourceCacheDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class TempleScrapingService(
    private val dal: SangitaDal,
    private val geminiClient: GeminiApiClient,
    private val geocodingService: GeocodingService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getTempleDetails(url: String, fetchContent: suspend (String) -> String): TempleSourceCacheDto? {
        // 1. Check Cache (Cache-First)
        val cached = dal.templeSourceCache.findByUrl(url)
        if (cached != null) {
            logger.info("Cache hit for temple URL: $url")
            return cached
        }

        // 2. Scrape
        logger.info("Cache miss. Scraping temple URL: $url")
        return try {
            val html = fetchContent(url)
            if (html.isBlank()) {
                logger.warn("Temple scrape received empty HTML for {}", url)
                return null
            }
            
            // Extract details using Gemini
            val prompt = """
                Analyze the following text from a Temple information page.
                Extract the details into a structured JSON format.
                
                Text Content:
                $html
                
                Extract:
                - name (Official name of the temple)
                - deity (Primary deity)
                - location (City/Town/Village)
                - state (State/Province)
                - country (Default to India if not specified but context matches)
                - description (Brief description of importance, max 500 chars)
                
                Return ONLY the valid JSON.
            """.trimIndent()
            
            val details = geminiClient.generateStructured<ScrapedTempleDetailsInternal>(prompt)
            val templeName = details.name?.trim()
            if (templeName.isNullOrBlank()) {
                logger.warn("Temple scrape returned null/blank name for {}", url)
                return null
            }

            // 3. Geocode Fallback
            var lat = details.latitude
            var lon = details.longitude
            var geoSource = "Scraped"
            var geoConfidence = "LOW"

            if (lat == null || lon == null) {
                // Try geocoding
                val geoResult = geocodingService.geocode(details.location, details.state, details.country ?: "India")
                if (geoResult != null) {
                    lat = geoResult.latitude
                    lon = geoResult.longitude
                    geoSource = geoResult.source
                    geoConfidence = geoResult.confidence
                }
            }

            // 4. Save to Cache
            val domain = try {
                java.net.URI(url).host
            } catch (e: Exception) { "unknown" }

            dal.templeSourceCache.save(
                sourceUrl = url,
                sourceDomain = domain,
                templeName = templeName,
                templeNameNormalized = normalize(templeName),
                deityName = details.deity,
                kshetraText = details.location, // Assuming location implies kshetra roughly
                city = details.location,
                state = details.state,
                country = details.country,
                latitude = lat,
                longitude = lon,
                geoSource = geoSource,
                geoConfidence = geoConfidence,
                notes = details.description,
                rawPayload = Json.encodeToString(details),
                error = null
            )
        } catch (e: Exception) {
            logger.error("Failed to scrape temple URL: $url", e)
             // Save error state to cache to prevent rapid re-scraping of bad URLs?
             // For now, allow retry.
            null
        }
    }
    
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9\\s]"), "")

    @Serializable
    private data class ScrapedTempleDetailsInternal(
        val name: String? = null,
        val deity: String? = null,
        val location: String? = null,
        val state: String? = null,
        val country: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val description: String? = null
    )
}
