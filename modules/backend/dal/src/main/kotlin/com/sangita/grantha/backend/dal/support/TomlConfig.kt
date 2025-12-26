package com.sangita.grantha.backend.dal.support

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Minimal TOML reader utilities tailored for simple key-value sections.
 */
object TomlConfig {
    fun findConfigFile(relativePath: Path): Path? {
        val start = Paths.get("").toAbsolutePath().normalize()
        return generateSequence(start) { current -> current.parent }
            .map { candidate -> candidate.resolve(relativePath) }
            .firstOrNull { Files.exists(it) }
    }

    fun readSection(path: Path, sectionName: String): Map<String, String> {
        if (!Files.exists(path)) return emptyMap()
        val result = mutableMapOf<String, String>()
        var inTargetSection = false
        val expectedSection = "[$sectionName]"

        try {
            Files.newBufferedReader(path).useLines { lines ->
                lines.forEach { rawLine ->
                    val line = rawLine.trim()
                    when {
                        line.isEmpty() || line.startsWith("#") || line.startsWith("//") -> Unit
                        line.startsWith("[") && line.endsWith("]") ->
                            inTargetSection = line.equals(expectedSection, ignoreCase = true)
                        inTargetSection -> {
                            val separatorIndex = line.indexOf('=')
                            if (separatorIndex > 0) {
                                val key = line.substring(0, separatorIndex).trim()
                                if (key.isNotEmpty()) {
                                    val valuePortion = line.substring(separatorIndex + 1).trim().trimEnd(',')
                                    val value = valuePortion
                                        .trim()
                                        .trim('"')
                                        .trim('\'')
                                    if (value.isNotEmpty()) {
                                        result[key] = value
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ex: IOException) {
            throw IllegalStateException("Failed to read section '$sectionName' from $path", ex)
        }

        return result
    }
}
