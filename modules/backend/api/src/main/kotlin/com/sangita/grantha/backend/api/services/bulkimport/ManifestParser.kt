package com.sangita.grantha.backend.api.services.bulkimport

import java.net.URI
import java.nio.file.Path
import kotlinx.serialization.Serializable
import org.apache.commons.csv.CSVFormat
import org.slf4j.Logger

@Serializable
data class CsvRow(
    val krithi: String,
    val raga: String?,
    val hyperlink: String,
)

class ManifestParser(private val logger: Logger) {
    fun parse(path: Path): List<CsvRow> {
        path.toFile().bufferedReader(Charsets.UTF_8).use { reader ->
            val parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build()
                .parse(reader)

            val headerMap = parser.headerMap
            if (headerMap != null) {
                val keys = headerMap.keys.map { it.lowercase() }.toSet()
                val required = listOf("krithi", "hyperlink")
                val missing = required.filter { !keys.contains(it) }

                if (missing.isNotEmpty()) {
                    throw IllegalArgumentException(
                        "Missing required columns: ${missing.map { it.replaceFirstChar(Char::titlecase) }}. Found: ${headerMap.keys}"
                    )
                }
            }

            return parser.mapNotNull { record ->
                val krithi = if (record.isMapped("Krithi")) record.get("Krithi") else null
                val hyperlink = if (record.isMapped("Hyperlink")) record.get("Hyperlink") else null

                if (krithi.isNullOrBlank() || hyperlink.isNullOrBlank()) return@mapNotNull null

                if (!isValidUrl(hyperlink)) {
                    logger.warn("Skipping invalid URL in manifest: {}", hyperlink)
                    return@mapNotNull null
                }

                val raga = if (record.isMapped("Raga")) record.get("Raga")?.takeIf { it.isNotBlank() } else null

                CsvRow(krithi = krithi, raga = raga, hyperlink = hyperlink)
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            val scheme = uri.scheme
            val host = uri.host
            (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) && !host.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }
}
