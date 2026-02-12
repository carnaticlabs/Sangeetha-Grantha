package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.services.scraping.HtmlTextExtractor
import com.sangita.grantha.backend.api.services.scraping.KrithiStructureParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.network.tls.TlsException
import org.slf4j.LoggerFactory

/**
 * A deterministic-only [IWebScraper] implementation that skips all Gemini API calls.
 *
 * Performs the same HTML fetch → text extraction → section detection pipeline as
 * [WebScrapingServiceImpl] but returns metadata fields (raga, tala, composer, deity,
 * temple) as null. This allows the full extraction/dedup/ingestion pipeline to be
 * validated without hitting Gemini API rate limits (429 errors).
 *
 * Enable via `SG_GEMINI_STUB_MODE=true` in the environment configuration.
 *
 * Recoverable: once the pipeline is validated, disable stub mode and re-scrape
 * to back-fill metadata from Gemini.
 */
class DeterministicWebScraper : IWebScraper {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 45_000
        }
    }

    private val textExtractor = HtmlTextExtractor(maxChars = 120_000)
    private val textBlocker = KrithiStructureParser()

    // Regex to parse raga from HTML title: "Dikshitar Kriti - Abhayaambaa Jagadambaa - Raga Kalyaani"
    private val titleRagaPattern = Regex(
        """Dikshitar\s+Krit[ih]*\s*[-–—]\s*(.+?)\s*[-–—]\s*Raga\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    override suspend fun scrapeKrithi(url: String): ScrapedKrithiMetadata {
        logger.info("[STUB MODE] Scraping URL deterministically (no Gemini): {}", url)

        val extracted = try {
            val html = httpClient.get(url).bodyAsText()
            textExtractor.extract(html, url)
        } catch (e: TlsException) {
            logger.warn("[STUB MODE] TLS validation failed for {}. Returning empty.", url, e)
            return ScrapedKrithiMetadata(
                title = url,
                warnings = listOf("gemini_stub_mode", "tls_failed")
            )
        } catch (e: Exception) {
            logger.error("[STUB MODE] Failed to fetch URL: {}", url, e)
            return ScrapedKrithiMetadata(
                title = url,
                warnings = listOf("gemini_stub_mode", "fetch_failed")
            )
        }

        if (extracted.text.isBlank()) {
            logger.warn("[STUB MODE] Empty extraction for {}", url)
            return ScrapedKrithiMetadata(
                title = extracted.title ?: url,
                warnings = listOf("gemini_stub_mode", "empty_extraction")
            )
        }

        // Use the same deterministic parsers as WebScrapingServiceImpl
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

        // Concatenate lyrics from sections if available
        val lyrics = if (detectedSections.isNotEmpty()) {
            detectedSections.joinToString("\n\n") { section ->
                "[${section.type}]\n${section.text}"
            }
        } else {
            null
        }

        // --- Deterministic metadata extraction ---
        // 1. Parse raga/tala from meta lines (e.g. "abhayAmbA jagadambA - rAgaM kalyANi - tALaM Adi")
        val metadataHints = textBlocker.extractMetadataHints(extracted.text)

        // 2. Fallback raga from HTML <title> (e.g. "Dikshitar Kriti - Abhayaambaa - Raga Kalyaani")
        val titleRaga = extracted.title?.let { titleRagaPattern.find(it)?.groupValues?.get(2)?.trim() }

        // 3. Derive composer from URL domain or HTML title
        val composer = deriveComposer(url, extracted.title)

        val raga = metadataHints.raga ?: titleRaga
        val tala = metadataHints.tala
        val title = metadataHints.title ?: extracted.title ?: url

        logger.info(
            "[STUB MODE] Deterministic scrape for {}: title={}, raga={}, tala={}, composer={}, sections={}, variants={}",
            url,
            title,
            raga,
            tala,
            composer,
            detectedSections.size,
            detectedVariants.size
        )

        return ScrapedKrithiMetadata(
            title = title,
            raga = raga,
            tala = tala,
            composer = composer,
            // deity, temple still require Gemini
            lyrics = lyrics,
            sections = detectedSections.ifEmpty { null },
            lyricVariants = detectedVariants.ifEmpty { null },
            warnings = listOf("gemini_stub_mode")
        )
    }

    /**
     * Derive composer from known URL domain patterns and HTML title keywords.
     */
    private fun deriveComposer(url: String, title: String?): String? {
        // guru-guha.blogspot.com is exclusively Muttuswami Dikshitar
        if (url.contains("guru-guha.blogspot", ignoreCase = true)) {
            return "Muttuswami Dikshitar"
        }
        // Fallback: check if title mentions Dikshitar
        if (title?.contains("Dikshitar", ignoreCase = true) == true) {
            return "Muttuswami Dikshitar"
        }
        // thyagaraja-vaibhavam would be Thyagaraja, etc. — extend as needed
        if (url.contains("thyagaraja", ignoreCase = true)) {
            return "Thyagaraja"
        }
        return null
    }
}
