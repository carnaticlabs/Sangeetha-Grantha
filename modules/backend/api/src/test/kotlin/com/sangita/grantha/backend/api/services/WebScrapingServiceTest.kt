package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.GeminiApiClient
import com.sangita.grantha.backend.api.config.ApiEnvironmentLoader
import com.sangita.grantha.backend.api.support.TestDatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDalImpl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class WebScrapingServiceTest {

    @BeforeEach
    fun setup() {
        TestDatabaseFactory.connectTestDb()
    }

    @Test
    fun `test scrapeKrithi with real API key and comprehensive Gemini check`() = runBlocking {
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

        val url = "https://guru-guha.blogspot.com/2007/08/dikshitar-kriti-abhayaambikaayaah-anyam.html"
        val metadata = service.scrapeKrithi(url)
        
        println("Scraped Metadata: $metadata")

        assertNotNull(metadata)
        assertTrue(metadata.title.isNotBlank(), "Title should not be blank")
        assertTrue(!metadata.sections.isNullOrEmpty(), "Sections should not be empty for this known URL")
        
        val sections = metadata.sections!!
        assertTrue(sections.any { it.type.name == "PALLAVI" }, "Should find PALLAVI section")
        // This specific krithi has P, MK, C, MK. So it might not have ANUPALLAVI explicitly.
        assertTrue(sections.any { it.type.name == "ANUPALLAVI" || it.type.name == "MADHYAMA_KALA" }, "Should find ANUPALLAVI or MADHYAMA_KALA section")
        assertTrue(sections.any { it.type.name == "CHARANAM" }, "Should find CHARANAM section")
        
        assertTrue(metadata.lyrics?.isNotBlank() == true, "Lyrics should not be blank")
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
        
        // Setup full stack for temple scraping
        val dal = SangitaDalImpl()
        val geo = GeocodingService(env)
        val templeService = TempleScrapingService(dal, client, geo)
        val service = WebScrapingServiceImpl(client, templeScrapingService = templeService)

        val url = "https://guru-guha.blogspot.com/2008/03/dikshitar-kriti-balambikayaa.html"
        val metadata = service.scrapeKrithi(url)
        
        println("Scraped Metadata: $metadata")
        println("Scraped Temple Details: ${metadata.templeDetails}")

        assertNotNull(metadata)
        assertNotNull(metadata.templeDetails, "Should have scraped temple details")
        
        val details = metadata.templeDetails!!
        assertTrue(details.name.contains("Vaitheeswaran", ignoreCase = true) || details.name.contains("Vaideeswaran", ignoreCase = true), "Should identify Vaitheeswaran Koil")
        assertTrue(metadata.templeUrl?.contains("templenet.com") == true || metadata.templeUrl?.contains("indiantemples.com") == true, "Should have extracted temple URL")
    }
}
