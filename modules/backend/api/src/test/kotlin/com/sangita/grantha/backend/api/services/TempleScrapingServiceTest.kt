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
                 filename = ".env.development"
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
        val realGeminiClient = GeminiApiClient(env.geminiApiKey!!)
        
        // Mock Cache Miss
        val url = "http://templenet.com/Tamilnadu/s207.html"
        coEvery { mockCacheRepo.findByUrl(url) } returns null
        
        // Mock Cache Save (Capture the argument)
        val saveSlot = slot<TempleSourceCacheDto>()
        
        // Create a dummy DTO to return
        val dummyDto = TempleSourceCacheDto(
            id = java.util.UUID.randomUUID(),
            sourceUrl = url,
            sourceDomain = "templenet.com",
            templeName = "Vaitheeswaran Koil",
            deityName = null,
            city = null,
            state = null,
            country = null,
            latitude = null,
            longitude = null,
            geoSource = null,
            geoConfidence = null,
            notes = null,
            rawPayload = null,
            fetchedAt = null,
            error = null
        )

        // Mock save to return the dummy DTO (since we can't easily return the captured argument directly in the same call in a type-safe way without side effects)
        // But actually, we just need to return ANY TempleSourceCacheDto to satisfy the signature.
        coEvery { 
            mockCacheRepo.save(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            ) 
        } returns dummyDto

        // Mock Geocoding
        coEvery { mockGeocodingService.geocode(any()) } returns null // Fallback to scraping logic or stay null

        // 3. Initialize Service
        // We need a way to mock the "fetchContent" lambda which is passed TO the method, 
        // but the service also has internal logic?
        // Wait, TempleScrapingService takes (cacheRepo, geocodingService, geminiClient) in constructor
        val service = TempleScrapingService(mockCacheRepo, mockGeocodingService, realGeminiClient)

        // 4. Execute
        // We need a real fetcher that actually gets the HTML, because we want to test Gemini parsing real HTML.
        // Simple fetcher using java.net.URL or Ktor client?
        // Let's use a simple valid fake HTML for stability if we don't want to hit templenet, 
        // OR use a real fetcher if we want E2E.
        // The previous test used real URL scraping.
        // But TempleScrapingService.getTempleDetails requires a `fetchContent` lambda.
        
        // Let's use a real fetcher logic similar to WebWriter or just a simple HTTP client.
        // For the test, I'll write a simple scraper using java.net for simplicity, or just hardcode HTML if I want to test parsing only.
        // But "Integration test (mocking external calls)" usually means mocking the HTTP response too.
        // However, user liked the previous test hitting real URLs.
        // Let's hitting the real URL.
        
        val fetcher: suspend (String) -> String = { u ->
            java.net.URL(u).readText()
        }

        // Run
        val result = service.getTempleDetails(url, fetcher)

        // 5. Assertions
        assertNotNull(result)
        println("Scraped Result: $result")
        
        assertEquals(url, result?.url)
        // Verify we got some expected data
        assertTrue(result?.name?.contains("Vaitheeswaran") == true, "Should identify Vaitheeswaran Koil")
        assertTrue(result?.deity?.contains("Muthukumaraswamy") == true || result?.deity?.contains("Selvamuthukumaraswamy") == true || result?.deity?.contains("Shiva") == true, "Should identify deity")

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
