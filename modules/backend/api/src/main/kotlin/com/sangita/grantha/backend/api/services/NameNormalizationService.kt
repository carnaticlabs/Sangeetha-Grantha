package com.sangita.grantha.backend.api.services

import org.slf4j.LoggerFactory

/**
 * Service for normalizing composer, raga, tala, and title strings.
 *
 * Simplified in Phase 3 (Simplify and Ship): transliteration collapse
 * is now handled by the Python normalizer (single source of truth).
 * This service retains basic normalization for API search queries
 * and the Levenshtein ratio for fuzzy scoring.
 */
class NameNormalizationService {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        /**
         * Levenshtein-based string similarity ratio (0–100).
         * Shared across ImportService and ExtractionResultProcessor for consistent scoring.
         */
        fun ratio(s1: String, s2: String): Int {
            val rows = s1.length + 1
            val cols = s2.length + 1
            val distance = Array(rows) { IntArray(cols) }

            for (i in 0 until rows) distance[i][0] = i
            for (j in 0 until cols) distance[0][j] = j

            for (i in 1 until rows) {
                for (j in 1 until cols) {
                    val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                    distance[i][j] = minOf(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + cost
                    )
                }
            }

            val maxLen = kotlin.math.max(s1.length, s2.length)
            if (maxLen == 0) return 100
            val dist = distance[s1.length][s2.length]
            return ((1.0 - dist.toDouble() / maxLen) * 100).toInt()
        }
    }

    /**
     * Normalize composer names to a canonical form for matching.
     */
    fun normalizeComposer(name: String?): String? {
        if (name.isNullOrBlank()) return null
        val normalized = basicNormalize(name)

        return when {
            // Canonical Mapping — all known spellings of the Trinity + key composers.
            // Includes both collapsed (th→t, sh→s) and un-collapsed forms since
            // transliteration collapse is now handled by Python normalizer.
            normalized == "tyagaraja" || normalized == "thyagaraja" -> "tyagaraja"
            normalized == "tyagarajar" || normalized == "thyagarajar" -> "tyagaraja"

            normalized == "diksitar" || normalized == "dikshitar" -> "muttuswami diksitar"
            normalized == "muttuswami diksitar" || normalized == "muttuswami dikshitar" -> "muttuswami diksitar"
            normalized == "muttuswamy diksitar" || normalized == "muttuswamy dikshitar" -> "muttuswami diksitar"
            normalized == "mutuswami diksitar" || normalized == "muthuswami dikshitar" -> "muttuswami diksitar"
            normalized == "muthuswami diksitar" || normalized == "muthuswami dikshithar" -> "muttuswami diksitar"

            normalized == "syama sastri" || normalized == "shyama shastri" -> "syama sastri"
            normalized == "syama sastry" || normalized == "shyama shastry" -> "syama sastri"

            normalized == "papanasam sivan" -> "papanasam sivan"

            // Catch-all for any variant of Dikshitar
            normalized.contains("diksitar") || normalized.contains("dikshitar") -> "muttuswami diksitar"

            // General Suffix Standardization
            normalized.endsWith(" sastry") || normalized.endsWith(" shastry") -> normalized
                .replace(" shastry", " sastri").replace(" sastry", " sastri")
            normalized.endsWith(" sastri") || normalized.endsWith(" shastri") -> normalized
                .replace(" shastri", " sastri")

            else -> normalized
        }
    }

    /**
     * Normalize raga names to a canonical form for matching.
     */
    fun normalizeRaga(name: String?): String? {
        if (name.isNullOrBlank()) return null
        var normalized = basicNormalize(name)

        // Vowel reduction for Ragas
        // "Kalyani" vs "Kalyaani" -> "kalyani"
        normalized = normalized.replace("aa", "a")
            .replace("ee", "i")
            .replace("oo", "o")
            .replace("uu", "u")
            .replace(" ", "") // Remove spaces entirely for ragas to match "Kedaragaula" vs "Kedara Gaula"

        return normalized
    }

    /**
     * Normalize tala names to a canonical form for matching.
     */
    fun normalizeTala(name: String?): String? {
        if (name.isNullOrBlank()) return null
        var normalized = basicNormalize(name)

        // Suffix Removal: "rupakam" → "rupaka", "ekam" → "eka"
        if (normalized.endsWith("am") && normalized.length > 4) {
            normalized = normalized.removeSuffix("am").plus("a")
            if (normalized.endsWith("am")) {
                normalized = normalized.removeSuffix("m")
            }
        }

        // Alias Mapping
        // Note: after transliteration collapse, "chapu" → "capu", "khanda" → "kanda"
        return when (normalized) {
            "desadi" -> "adi"
            "madyadi" -> "adi"
            else -> normalized
        }
    }

    /**
     * Normalize deity names.
     */
    fun normalizeDeity(name: String?): String? {
        if (name.isNullOrBlank()) return null
        var normalized = basicNormalize(name)
        normalized = normalized.replace(Regex("\\b(lord|goddess|sri|sri|arulmigu)\\b", RegexOption.IGNORE_CASE), "").trim()
        return normalized.takeIf { it.isNotBlank() }
    }

    /**
     * Normalize temple names.
     */
    fun normalizeTemple(name: String?): String? {
        if (name.isNullOrBlank()) return null
        var normalized = basicNormalize(name)
        normalized = normalized.replace(Regex("\\b(sri|arulmigu|tiru)\\b", RegexOption.IGNORE_CASE), "").trim()
        return normalized.takeIf { it.isNotBlank() }
    }

    /**
     * Normalize titles for fuzzy comparisons.
     */
    fun normalizeTitle(title: String?): String? {
        if (title.isNullOrBlank()) return null
        return basicNormalize(title)
    }

    /**
     * Core normalisation pipeline:
     * 1. NFD decomposition (ā → a + combining macron)
     * 2. Strip combining marks (removes all diacritics)
     * 3. Lowercase
     * 4. Remove honorific prefixes
     * 5. Remove special characters
     * 6. Collapse whitespace
     *
     * Note: Transliteration collapse (sh→s, th→t, etc.) is now handled
     * by the Python normalizer. This method is retained for API search queries.
     */
    private fun basicNormalize(value: String): String {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .trim()
            .lowercase()
            .replace(Regex("\\b(saint|sri|swami|sir|dr|prof|smt)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
