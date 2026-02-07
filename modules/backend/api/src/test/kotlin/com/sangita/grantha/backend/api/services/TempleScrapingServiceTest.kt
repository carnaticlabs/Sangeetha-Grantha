package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.GeminiApiClient
import com.sangita.grantha.backend.api.config.ApiEnvironmentLoader
import com.sangita.grantha.backend.dal.repositories.TempleSourceCacheRepository
import com.sangita.grantha.backend.dal.repositories.TempleSourceCacheDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant

class TempleScrapingServiceTest {

    @Test
    fun `test getTempleDetails with real Gemini and mocked Cache and Geocoding`() = runBlocking {
        // 1. Setup Environment
        val configDir = File("config")
        val envMap = if (configDir.exists()) {
             io.github.cdimascio.dotenv.dotenv {
                 directory = configDir.absolutePath
                 filename = "development.env"
                 ignoreIfMissing = true
             }.entries().associate { it.key to it.value }
        } else {
            emptyMap()
        }
        val env = ApiEnvironmentLoader.load(envMap)
        
        if (env.geminiApiKey.isNullOrBlank()) {
             println("Skipping test: GEMINI_API_KEY not found")
             return@runBlocking
        }

        // 2. Setup Mocks
        val mockCacheRepo = mockk<TempleSourceCacheRepository>()
        val mockGeocodingService = mockk<GeocodingService>()
        val realGeminiClient = GeminiApiClient(
            apiKey = env.geminiApiKey!!,
            modelUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        )
        
        // Mock Cache Miss
        val url = "http://templenet.com/Tamilnadu/s207.html"
        coEvery { mockCacheRepo.findByUrl(url) } returns null
        
        // Create a dummy DTO to return
        val dummyDto = TempleSourceCacheDto(
            id = java.util.UUID.randomUUID(),
            sourceUrl = url,
            sourceDomain = "templenet.com",
            templeName = "Vaitheeswaran Koil",
            deityName = "Muthukumaraswamy",
            kshetraText = "Vaitheeswaran Koil",
            city = "Vaitheeswaran Koil",
            state = "Tamil Nadu",
            country = "India",
            latitude = 11.20,
            longitude = 79.71,
            geoSource = "Manual",
            geoConfidence = "HIGH",
            notes = null,
            rawPayload = null,
            fetchedAt = Instant.now().toString(),
            error = null
        )

        // Mock save to return the dummy DTO
        coEvery { 
            mockCacheRepo.save(
                sourceUrl = any(),
                sourceDomain = any(),
                templeName = any(),
                templeNameNormalized = any(),
                deityName = any(),
                kshetraText = any(),
                city = any(),
                state = any(),
                country = any(),
                latitude = any(),
                longitude = any(),
                geoSource = any(),
                geoConfidence = any(),
                notes = any(),
                rawPayload = any(),
                error = any()
            ) 
        } returns dummyDto

        // Mock Geocoding
        coEvery { mockGeocodingService.geocode(any(), any(), any()) } returns null // Fallback to scraping logic or stay null

        // 3. Initialize Service
        // We need a way to mock the "fetchContent" lambda which is passed TO the method, 
        // but the service also has internal logic?
        // Wait, TempleScrapingService takes (dal, geminiClient, geocodingService) in constructor
        val dal = com.sangita.grantha.backend.dal.SangitaDalImpl()
        val service = TempleScrapingService(dal, realGeminiClient, mockGeocodingService)

        // 4. Execute
        val fetcher: suspend (String) -> String = { u ->
            java.net.URL(u).readText()
        }

        // Run
        val result = service.getTempleDetails(url, fetcher)

        // 5. Assertions
        assertNotNull(result)
        println("Scraped Result: $result")
        
        assertEquals(url, result?.sourceUrl)
        // Verify we got some expected data
        assertTrue(result?.templeName?.contains("Vaitheeswaran") == true, "Should identify Vaitheeswaran Koil")
        assertTrue(result?.deityName?.contains("Muthukumaraswamy") == true || result?.deityName?.contains("Selvamuthukumaraswamy") == true || result?.deityName?.contains("Shiva") == true, "Should identify deity")

        // Verify Cache was saved
        coVerify(exactly = 1) { 
            mockCacheRepo.save(
                sourceUrl = url,
                sourceDomain = any(),
                templeName = any(),
                templeNameNormalized = any(),
                deityName = any(),
                kshetraText = any(),
                city = any(),
                state = any(),
                country = any(),
                latitude = any(),
                longitude = any(),
                geoSource = any(),
                geoConfidence = any(),
                notes = any(),
                rawPayload = any(),
                error = any()
            ) 
        }
    }
}
