package com.sangita.grantha.backend.api.services.scraping

import com.sangita.grantha.shared.domain.model.RagaSectionDto

class SectionHeaderDetector {

    data class HeaderMatch(val label: String, val remainder: String)

    val LANGUAGE_LABELS = setOf(
        "DEVANAGARI", "TAMIL", "TELUGU", "KANNADA", "MALAYALAM",
        "ENGLISH", "LATIN", "SANSKRIT", "HINDI",
        "WORD_DIVISION", "MEANING", "GIST", "NOTES", "VARIATIONS"
    )

    fun detectHeader(line: String): HeaderMatch? {
        val normalized = line.trim()
        if (normalized.isBlank()) return null

        val languageMatch = detectLanguageHeader(normalized)
        if (languageMatch != null) return languageMatch

        val sectionMatch = detectSectionHeader(normalized)
        if (sectionMatch != null) return sectionMatch

        return null
    }

    fun detectLanguageHeader(line: String): HeaderMatch? {
        val lowered = line.lowercase()
        val candidates = listOf(
            "devanagari" to "DEVANAGARI",
            "tamil" to "TAMIL",
            "telugu" to "TELUGU",
            "kannada" to "KANNADA",
            "malayalam" to "MALAYALAM",
            "english" to "ENGLISH",
            "roman" to "LATIN",
            "latin" to "LATIN",
            "sanskrit" to "SANSKRIT",
            "hindi" to "HINDI",
            "word division" to "WORD_DIVISION",
            "meaning" to "MEANING",
            "gist" to "GIST",
            "notes" to "NOTES",
            "variations" to "VARIATIONS"
        )

        for ((key, label) in candidates) {
            if (lowered == key || lowered.startsWith("$key:") || lowered.startsWith("$key -") || lowered.startsWith("$key –")) {
                val remainder = line.substringAfter(key, "").trimStart(':', '-', '–', ' ')
                return HeaderMatch(label, remainder)
            }
        }
        return null
    }

    fun detectSectionHeader(line: String): HeaderMatch? {
        val prefix = "^\\s*[\\-–—•*()=\\[\\]]*\\s*"
        // Suffix can be word boundary, colon, dash, closing parenthesis, closing bracket, or end of line
        val suffix = "(?:\\b|:|\\.|\\-|\\)|]|=|$)"

        val patterns = listOf(
            Regex("${prefix}pallavi$suffix", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("${prefix}anupallavi$suffix", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("${prefix}(?:ch|c)ara?nam$suffix", RegexOption.IGNORE_CASE) to "CHARANAM",
            Regex("${prefix}samashti\\s+(?:ch|c)ara?nam$suffix", RegexOption.IGNORE_CASE) to "SAMASHTI_CHARANAM",
            Regex("${prefix}samash?ti\\s+(?:ch|c)ara?nam$suffix", RegexOption.IGNORE_CASE) to "SAMASHTI_CHARANAM",
            Regex("${prefix}chittaswaram$suffix", RegexOption.IGNORE_CASE) to "CHITTASWARAM",
            // Allow "sahityam" in Madhyama Kala header (e.g., "(madhyama kAla sAhityam)")
            Regex("${prefix}madhyama\\s+kAla(?:\\s+sAhityam)?$suffix", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",
            Regex("${prefix}madhyama\\s+kala(?:\\s+sahityam)?$suffix", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",
            Regex("${prefix}madhyamakala(?:\\s+sahityam)?$suffix", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",
            Regex("${prefix}m\\.\\s*k(?:\\s+sahityam)?$suffix", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",
            Regex("${prefix}muktayi\\s+swara$suffix", RegexOption.IGNORE_CASE) to "MUKTAYI_SWARA",
            Regex("${prefix}ettugada\\s+swara$suffix", RegexOption.IGNORE_CASE) to "ETTUGADA_SWARA",
            Regex("${prefix}ettugada\\s+sahitya$suffix", RegexOption.IGNORE_CASE) to "ETTUGADA_SAHITYA",
            Regex("${prefix}svara\\s+sahitya$suffix", RegexOption.IGNORE_CASE) to "SWARA_SAHITYA",
            Regex("${prefix}swarasahitya$suffix", RegexOption.IGNORE_CASE) to "SWARA_SAHITYA",
            Regex("${prefix}anubandha$suffix", RegexOption.IGNORE_CASE) to "ANUBANDHA",
            Regex("${prefix}viloma\\s+chittaswaram$suffix", RegexOption.IGNORE_CASE) to "VILOMA_CHITTASWARAM",

            // Single letter abbreviations (e.g. P, A, C)
            Regex("^\\s*P(?:\\s|\\.|:|-|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*A(?:\\s|\\.|:|-|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*C(?:\\s|\\.|:|-|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",
            Regex("^\\s*Ch(?:\\s|\\.|:|-|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",

            // Indic Abbreviations
            Regex("^\\s*प(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*अ(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*च(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",

            Regex("^\\s*ப(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*அ(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*ச(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",

            Regex("^\\s*ప(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*అ(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*చ(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",

            Regex("^\\s*ಪ(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*ಅ(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*ಚ(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",

            Regex("^\\s*പ(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*അ(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*ച(?:\\.|\\s|:|-|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",

            // Devanagari Headers
            Regex("^\\s*पल्लवि(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*अनुपल्लवि(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*चरणम्(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",
            Regex("^\\s*समष्टि\\s+चरणम्(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "SAMASHTI_CHARANAM",
            Regex("^\\s*[(]?मध्यम\\s+काल\\s+साहित्यम्[)]?(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",

            // Tamil Headers
            Regex("^\\s*பல்லவி(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*அனுபல்லவி(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*சரணம்(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",
            Regex("^\\s*ஸமஷ்டி\\s+சரணம்(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "SAMASHTI_CHARANAM",
            Regex("^\\s*[(]?மத் 4 யம\\s+கால\\s+ஸாஹித்யம்[)]?(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",

            // Telugu Headers
            Regex("^\\s*పల్లవి(?:\\s|:|\\-|\\)|]|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*అనుపల్లవి(?:\\s|:|\\-|\\)|]|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*చరణం(?:\\s|:|\\-|\\)|]|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",
            Regex("^\\s*సమష్టి\\s+చరణం(?:\\s|:|\\-|\\)|]|\$)", RegexOption.IGNORE_CASE) to "SAMASHTI_CHARANAM",
            Regex("^\\s*[(]?మధ్యమ\\s+కాల\\s+సాహిత్యమ్[)]?(?:\\s|:|\\-|\\)|]|\$)", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",

            // Kannada Headers
            Regex("^\\s*ಪಲ್ಲವಿ(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*ಅನುಪಲ್ಲವಿ(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*ಚರಣ(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",
            Regex("^\\s*ಸಮಷ್ಟಿ\\s+ಚರಣ(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "SAMASHTI_CHARANAM",
            Regex("^\\s*[(]?ಮಧ್ಯಮ\\s+ಕಾಲ\\s+ಸಾಹಿತ್ಯಮ್[)]?(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",

            // Malayalam Headers
            Regex("^\\s*പല്ലവി(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*അനുപല്ലവി(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*ചരണം(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",
            Regex("^\\s*സമഷ്ടി\\s+ചരണം(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "SAMASHTI_CHARANAM",
            Regex("^\\s*[(]?മധ്യമ\\s+കാല\\s+സാഹിത്യമ്[)]?(?:\\s|:|\\-|\\.|\\)|]|\$)", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",

            // Non-Lyric Headers (Metadata/Meaning)
            Regex("${prefix}meaning$suffix", RegexOption.IGNORE_CASE) to "MEANING",
            Regex("${prefix}notes$suffix", RegexOption.IGNORE_CASE) to "NOTES",
            Regex("${prefix}gist$suffix", RegexOption.IGNORE_CASE) to "GIST",
            Regex("${prefix}word\\s+division$suffix", RegexOption.IGNORE_CASE) to "WORD_DIVISION",
            Regex("${prefix}variations$suffix", RegexOption.IGNORE_CASE) to "VARIATIONS"
        )

        for ((regex, label) in patterns) {
            if (regex.containsMatchIn(line)) {
                var remainder = line.replace(regex, "").trim()
                // Clean up any remaining closing brackets or punctuation that might have been part of the header wrapper
                // e.g. if we matched "(madhyama kAla" from "(madhyama kAla)", remainder is ")".
                // We want to strip that.
                remainder = remainder.replace(Regex("^[:\\-)\\]]+"), "").trim()
                return HeaderMatch(label, remainder)
            }
        }
        return null
    }

    fun mapLabelToSection(label: String): RagaSectionDto? {
        return when (label.uppercase()) {
            "PALLAVI" -> RagaSectionDto.PALLAVI
            "ANUPALLAVI" -> RagaSectionDto.ANUPALLAVI
            "CHARANAM" -> RagaSectionDto.CHARANAM
            "SAMASHTI_CHARANAM" -> RagaSectionDto.SAMASHTI_CHARANAM
            "CHITTASWARAM" -> RagaSectionDto.CHITTASWARAM
            "SWARA_SAHITYA" -> RagaSectionDto.SWARA_SAHITYA
            "MADHYAMAKALA", "MADHYAMA_KALA" -> RagaSectionDto.MADHYAMA_KALA
            "SOLKATTU_SWARA" -> RagaSectionDto.SOLKATTU_SWARA
            "ANUBANDHA" -> RagaSectionDto.ANUBANDHA
            "MUKTAYI_SWARA" -> RagaSectionDto.MUKTAYI_SWARA
            "ETTUGADA_SWARA" -> RagaSectionDto.ETTUGADA_SWARA
            "ETTUGADA_SAHITYA" -> RagaSectionDto.ETTUGADA_SAHITYA
            "VILOMA_CHITTASWARAM" -> RagaSectionDto.VILOMA_CHITTASWARAM
            else -> null
        }
    }
}
