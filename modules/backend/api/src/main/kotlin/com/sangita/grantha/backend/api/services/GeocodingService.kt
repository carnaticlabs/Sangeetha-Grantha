package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.config.ApiEnvironment
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class GeocodingResult(
    val latitude: Double,
    val longitude: Double,
    val source: String,
    val confidence: String
)

@Serializable
data class NominatimResult(
    val lat: String,
    val lon: String,
    val display_name: String,
    val importance: Double?
)

class GeocodingService(
    private val environment: ApiEnvironment,
    private val httpClient: HttpClient = HttpClient() {
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 30_000  // 30 seconds for geocoding requests
            connectTimeoutMillis = 15_000  // 15 seconds to establish connection
            socketTimeoutMillis = 20_000   // 20 seconds between data packets
        }
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun geocode(city: String?, state: String?, country: String = "India"): GeocodingResult? {
        if (city.isNullOrBlank()) return null
        
        val provider = environment.geoProvider
        return when (provider.lowercase()) {
            "osm", "openstreetmap" -> geocodeOsm(city, state, country)
            "google" -> {
                logger.warn("Google Geocoding not yet implemented, falling back to None")
                null
            }
            else -> {
                logger.warn("Unknown geocoding provider: $provider")
                null
            }
        }
    }

    private suspend fun geocodeOsm(city: String, state: String?, country: String): GeocodingResult? {
        try {
            val query = listOfNotNull(city, state, country).joinToString(", ")
            logger.info("Geocoding via OSM: $query")
            
            // Nominatim requires a User-Agent
            val response = httpClient.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", query)
                parameter("format", "json")
                parameter("limit", 1)
                header(HttpHeaders.UserAgent, "SangitaGrantha/1.0 (admin@sangita-grantha.com)")
            }

            if (response.status != HttpStatusCode.OK) {
                logger.warn("OSM Geocoding failed: ${response.status}")
                return null
            }

            val results = response.body<List<NominatimResult>>()
            val bestCheck = results.firstOrNull() ?: return null

            return GeocodingResult(
                latitude = bestCheck.lat.toDouble(),
                longitude = bestCheck.lon.toDouble(),
                source = "OSM-Nominatim",
                confidence = "MEDIUM" // OSM doesn't return confidence score directly, but it's generally reliable for named cities
            )

        } catch (e: Exception) {
            logger.error("Error during OSM geocoding", e)
            return null
        }
    }
}
