package com.sangita.grantha.backend.api.services

import org.slf4j.LoggerFactory

class NameNormalizationService {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun normalizeComposer(name: String?): String? {
        if (name.isNullOrBlank()) return null
        val normalized = basicNormalize(name)

        return when {
            // Canonical Mapping
            normalized == "thyagaraja" -> "tyagaraja"
            normalized == "tyagaraja" -> "tyagaraja"
            normalized == "saint tyagaraja" -> "tyagaraja"
            
            normalized == "dikshitar" -> "muthuswami dikshitar"
            normalized == "muthuswami dikshitar" -> "muthuswami dikshitar"
            normalized == "muthuswamy dikshitar" -> "muthuswami dikshitar"
            
            normalized == "syama sastri" -> "syama sastri"
            normalized == "syama sastry" -> "syama sastri"
            normalized == "shyama sastri" -> "syama sastri"
            normalized == "shyama shastri" -> "syama sastri"
            
            normalized == "papanasam sivan" -> "papanasam sivan"
            normalized == "papanasam shivan" -> "papanasam sivan"

            // General Suffix Standardization
            normalized.endsWith(" sastry") -> normalized.replace(" sastry", " sastri")
            normalized.endsWith(" shastri") -> normalized.replace(" shastri", " sastri")
            
            else -> normalized
        }
    }

    fun normalizeRaga(name: String?): String? {
        if (name.isNullOrBlank()) return null
        var normalized = basicNormalize(name)

        // Vowel reduction and space removal for Ragas
        // "Kalyani" vs "Kalyaani" -> "kalyani"
        // "Kedara Gaula" -> "kedaragaula"
        
        normalized = normalized.replace("aa", "a")
            .replace("ee", "i")
            .replace("oo", "o")
            .replace("uu", "u")
            .replace(" ", "") // Remove spaces entirely for ragas to match "Kedaragaula" vs "Kedara Gaula"

        return normalized
    }

    fun normalizeTala(name: String?): String? {
        if (name.isNullOrBlank()) return null
        var normalized = basicNormalize(name)

        // Suffix Removal
        if (normalized.endsWith("am") && normalized.length > 4) { // Avoid removing from short words like "Sam" if valid
             // e.g. "rupakam" -> "rupaka"
             // But "ekam" -> "eka"
             normalized = normalized.removeSuffix("am").plus("a") // rupak-a, ek-a
             // Wait, "ekam" -> "eka", "rupakam" -> "rupaka". 
             // Logic: remove 'm' only? "rupaka" vs "rupakam". 
             // Actually, simplest is just remove 'm' if it ends in 'am'?
             // "rupakam" -> "rupaka". "ekam" -> "eka". "triputam" -> "triputa".
             if (normalized.endsWith("am")) {
                 normalized = normalized.removeSuffix("m")
             }
        }

        // Transliteration Standardization
        normalized = normalized
            .replace("capu", "chapu") // cApu -> chapu
            .replace("misra", "misra") // ensure lowercase
            .replace("khanda", "khanda")
            .replace("tisra", "tisra")
            .replace("sankirna", "sankirna")

        // Alias Mapping
        return when (normalized) {
            "desadi" -> "adi"
            "madhyadi" -> "adi"
            else -> normalized
        }
    }

    fun normalizeTitle(title: String?): String? {
        if (title.isNullOrBlank()) return null
        return basicNormalize(title)
    }

    private fun basicNormalize(value: String): String {
        return value.trim()
            .lowercase()
            // Fix: Use proper word boundary regex (\\b not \b which is backspace)
            .replace(Regex("\\b(saint|sri|swami|sir|dr|prof|smt)\\b", RegexOption.IGNORE_CASE), "") // Remove honorifics
            .replace(Regex("[^a-zA-Z0-9\\s]"), "") // Remove special chars
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
