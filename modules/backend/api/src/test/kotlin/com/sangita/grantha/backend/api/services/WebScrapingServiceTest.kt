package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.GeminiApiClient
import com.sangita.grantha.backend.api.config.ApiEnvironmentLoader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class WebScrapingServiceTest {

    @Test
    fun `test scrapeKrithi with real API key and comprehensive Gemini check`() = runBlocking {
        // This test authenticates with the real Gemini API using the key from .env.development
        // It uses the configured model (gemini-2.0-flash) to extract data from a real URL.
        // Load environment from .env.development to simulate dev run
        // We explicitly point to the file to bypass any workingDir ambiguity in this test
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
        
        // Ensure we ignore test if key is not present (standard CI practice, though user wants debugging)
        if (env.geminiApiKey.isNullOrBlank()) {
             println("Skipping test: GEMINI_API_KEY not found in .env.development")
             return@runBlocking
        }
        
        println("Using API Key: ${env.geminiApiKey.take(4)}...")

        val client = GeminiApiClient(
            apiKey = env.geminiApiKey!!,
            modelUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        )
        val service = WebScrapingServiceImpl(client)

//        val url = "https://www.shivkumar.org/music/abhayambikaya.htm"
        val url = "https://guru-guha.blogspot.com/2007/08/dikshitar-kriti-abhayaambikaayaah-anyam.html"
        val metadata = service.scrapeKrithi(url)
        
        println("Scraped Metadata: $metadata")
        if (!metadata.sections.isNullOrEmpty()) {
            println("Scraped Sections: ${metadata.sections}")
        }

        assertNotNull(metadata)
        assertTrue(metadata.title.isNotBlank(), "Title should not be blank")
        assertTrue(!metadata.sections.isNullOrEmpty(), "Sections should not be empty for this known URL")
        
        val sections = metadata.sections!!
        assertTrue(sections.any { it.type.name == "PALLAVI" }, "Should find PALLAVI section")
        assertTrue(sections.any { it.type.name == "ANUPALLAVI" }, "Should find ANUPALLAVI section")
        assertTrue(sections.any { it.type.name == "CHARANAM" }, "Should find CHARANAM section")
        
        assertTrue(metadata.lyrics?.isNotBlank() == true, "Lyrics (concatenated or direct) should not be blank")
    }

    @Test
    fun `test scrapeKrithi with templeNet embedded link`() = runBlocking {
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
             println("Skipping test: GEMINI_API_KEY not found in .env.development")
             return@runBlocking
        }
        
        val client = GeminiApiClient(
            apiKey = env.geminiApiKey!!,
            modelUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        )
        val service = WebScrapingServiceImpl(client)

        // URL provided by user
        val url = "https://guru-guha.blogspot.com/2008/03/dikshitar-kriti-balambikayaa.html"
        val metadata = service.scrapeKrithi(url)
        
        println("Scraped Metadata: $metadata")
        println("Scraped Temple Details: ${metadata.templeDetails}")

        assertNotNull(metadata)
        assertNotNull(metadata.templeDetails, "Should have scraped temple details")
        
        val details = metadata.templeDetails!!
        println("Temple Name: ${details.name}")
        println("Deity: ${details.deity}")
        println("Location: ${details.location}")
        println("Lat/Long: ${details.latitude}, ${details.longitude}")
        
        // Assertions based on expected content from the URL
        assertTrue(details.name.contains("Vaitheeswaran") || details.name.contains("Vaideeswaran") || details.name.contains("Vaitheesvaran"), "Should identify Vaitheeswaran Koil")
        assertTrue(metadata.templeUrl?.let { it.contains("templenet.com") || it.contains("indiantemples.com") } == true, "Should have extracted temple URL (templenet or indiantemples)")
    }
}
