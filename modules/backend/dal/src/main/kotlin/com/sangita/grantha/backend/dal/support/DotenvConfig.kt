package com.sangita.grantha.backend.dal.support

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Minimal .env reader used for local/test database configuration.
 *
 * Each line is expected to be in KEY=VALUE format, with optional quotes.
 */
object DotenvConfig {
    fun read(path: Path): Map<String, String> {
        if (!Files.exists(path)) return emptyMap()

        val result = mutableMapOf<String, String>()
        try {
            Files.newBufferedReader(path).useLines { lines ->
                lines.forEach { rawLine ->
                    val line = rawLine.trim()
                    if (line.isEmpty() || line.startsWith("#")) return@forEach

                    val separatorIndex = line.indexOf('=')
                    if (separatorIndex <= 0) return@forEach

                    val key = line.substring(0, separatorIndex).trim()
                    if (key.isEmpty()) return@forEach

                    val valuePortion = line.substring(separatorIndex + 1).trim().trimEnd(',')
                    val value = valuePortion
                        .removePrefix("\"").removeSuffix("\"")
                        .removePrefix("'").removeSuffix("'")
                        .trim()
                    if (value.isNotEmpty()) {
                        result[key] = value
                    }
                }
            }
        } catch (ex: IOException) {
            throw IllegalStateException("Failed to read environment variables from $path", ex)
        }

        return result
    }
}
