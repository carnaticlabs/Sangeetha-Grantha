package com.sangita.grantha.backend.api.services

import com.sangita.grantha.shared.domain.support.TransliterationCollapse
import org.slf4j.LoggerFactory

/**
 * Service for normalizing composer, raga, tala, and title strings.
 *
 * TRACK-061: Added transliteration-aware collapse so that different
 * romanisation schemes (IAST, Harvard-Kyoto, ITRANS, simple ASCII)
 * all produce the same normalised matching key.
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
            // Note: after transliteration collapse in basicNormalize(),
            //   "Muthuswami Dikshitar" → "muttuswami diksitar"
            //   "Muthuswami Dikshithar" → "muttuswami diksitar"  (th→t)
            //   "Thyagaraja" → "tyagaraja"  (th→t)
            //   "Shyama Shastri" → "syama sastri"  (sh→s, sh→s)
            normalized == "tyagaraja" -> "tyagaraja"
            normalized == "tyagarajar" -> "tyagaraja"

            normalized == "diksitar" -> "muttuswami diksitar"
            normalized == "muttuswami diksitar" -> "muttuswami diksitar"
            normalized == "muttuswamy diksitar" -> "muttuswami diksitar"
            // Handle "Muthuswami" -> "mutuswami" (th->t)
            normalized == "mutuswami diksitar" -> "muttuswami diksitar"
            normalized == "muthuswami diksitar" -> "muttuswami diksitar"

            normalized == "syama sastri" -> "syama sastri"
            normalized == "syama sastry" -> "syama sastri"

            normalized == "papanasam sivan" -> "papanasam sivan"

            // Catch-all for any variant of Dikshitar not already matched above
            normalized.contains("diksitar") || normalized.contains("dikshitar") -> "muttuswami diksitar"

            // General Suffix Standardization
            normalized.endsWith(" sastry") -> normalized.replace(" sastry", " sastri")
            normalized.endsWith(" sastri") -> normalized // already canonical

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
     * 6. TRACK-061: Transliteration collapse (sh→s, th→t, ksh→ks, etc.)
     * 7. Collapse whitespace
     */
    private fun basicNormalize(value: String): String {
        var result = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        result = result.replace(Regex("\\p{M}"), "") // Remove combining diacritics
            .trim()
            .lowercase()
            .replace(Regex("\\b(saint|sri|swami|sir|dr|prof|smt)\\b", RegexOption.IGNORE_CASE), "") // Remove honorifics
            .replace(Regex("[^a-zA-Z0-9\\s]"), "") // Remove special chars
            .replace(Regex("\\s+"), " ")
            .trim()

        // TRACK-061: Transliteration collapse — longest match first
        return TransliterationCollapse.collapse(result)
    }
}
