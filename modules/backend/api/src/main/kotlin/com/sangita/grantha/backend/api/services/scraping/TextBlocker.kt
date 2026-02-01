package com.sangita.grantha.backend.api.services.scraping

class TextBlocker {
    data class TextBlock(val label: String, val lines: List<String>)
    data class PromptBlocks(
        val metaLines: List<String>,
        val blocks: List<TextBlock>
    )

    fun buildBlocks(rawText: String): PromptBlocks {
        val lines = rawText
            .replace("\\n", "\n") // Unescape literal \n strings if present
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { isBoilerplate(it) }

        if (lines.isEmpty()) {
            return PromptBlocks(metaLines = emptyList(), blocks = emptyList())
        }

        val metaIndices = lines.indices.filter { index -> isMetaLine(lines[index]) }.toSet()
        val metaLines = metaIndices.map { lines[it] }.distinct()
        val bodyLines = lines.filterIndexed { index, _ -> index !in metaIndices }

        val blocks = splitIntoBlocks(bodyLines)
        return PromptBlocks(metaLines = metaLines, blocks = blocks)
    }

    private fun splitIntoBlocks(lines: List<String>): List<TextBlock> {
        if (lines.isEmpty()) return emptyList()

        val blocks = mutableListOf<TextBlock>()
        var currentLabel = "UNLABELED"
        val currentLines = mutableListOf<String>()

        fun flush() {
            if (currentLines.isNotEmpty()) {
                blocks.add(TextBlock(currentLabel, currentLines.toList()))
                currentLines.clear()
            }
        }

        for (line in lines) {
            val header = detectHeader(line)
            if (header != null) {
                flush()
                currentLabel = header.label
                if (header.remainder.isNotBlank()) {
                    currentLines.add(header.remainder)
                }
                continue
            }
            currentLines.add(line)
        }

        flush()
        return blocks
    }

    private data class HeaderMatch(val label: String, val remainder: String)

    private fun detectHeader(line: String): HeaderMatch? {
        val normalized = line.trim()
        if (normalized.isBlank()) return null

        val languageMatch = detectLanguageHeader(normalized)
        if (languageMatch != null) return languageMatch

        val sectionMatch = detectSectionHeader(normalized)
        if (sectionMatch != null) return sectionMatch

        return null
    }

    private fun detectLanguageHeader(line: String): HeaderMatch? {
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
            "hindi" to "HINDI"
        )

        for ((key, label) in candidates) {
            if (lowered == key || lowered.startsWith("$key:") || lowered.startsWith("$key -")) {
                val remainder = line.substringAfter(key, "").trimStart(':', '-', ' ')
                return HeaderMatch(label, remainder)
            }
        }
        return null
    }

    private fun detectSectionHeader(line: String): HeaderMatch? {
        val prefix = "^\\s*[\\-–—•*()\\[\\]]*\\s*"
        // Suffix can be word boundary, colon, dash, closing parenthesis, closing bracket, or end of line
        val suffix = "(?:\\b|:|\\-|\\)|]|$)"

        val patterns = listOf(
            Regex("${prefix}pallavi$suffix", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("${prefix}anupallavi$suffix", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("${prefix}(?:ch|c)ara?nam$suffix", RegexOption.IGNORE_CASE) to "CHARANAM",
            Regex("${prefix}samashti\\s+(?:ch|c)ara?nam$suffix", RegexOption.IGNORE_CASE) to "SAMASHTI_CHARANAM",
            Regex("${prefix}samash?ti\\s+(?:ch|c)ara?nam$suffix", RegexOption.IGNORE_CASE) to "SAMASHTI_CHARANAM",
            Regex("${prefix}chittaswaram$suffix", RegexOption.IGNORE_CASE) to "CHITTASWARAM",
            // Allow "sahityam" in Madhyama Kala header (e.g., "(madhyama kAla sAhityam)")
            Regex("${prefix}madhyama\\s+kala(?:\\s+sahityam)?$suffix", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",
            Regex("${prefix}madhyamakala(?:\\s+sahityam)?$suffix", RegexOption.IGNORE_CASE) to "MADHYAMAKALA",
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

            // Devanagari Headers
            Regex("^\\s*पल्लवि(?:\\s|:|\\-|\\)|]|\$)", RegexOption.IGNORE_CASE) to "PALLAVI",
            Regex("^\\s*अनुपल्लवि(?:\\s|:|\\-|\\)|]|\$)", RegexOption.IGNORE_CASE) to "ANUPALLAVI",
            Regex("^\\s*चरणम्(?:\\s|:|\\-|\\)|]|\$)", RegexOption.IGNORE_CASE) to "CHARANAM",
            Regex("^\\s*समष्टि\\s+चरणम्(?:\\s|:|\\-|\\)|]|\$)", RegexOption.IGNORE_CASE) to "SAMASHTI_CHARANAM",
            Regex("^\\s*[(]?मध्यम\\s+काल\\s+साहित्यम्[)]?(?:\\s|:|\\-|\\)|]|\$)", RegexOption.IGNORE_CASE) to "MADHYAMAKALA"
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

    private fun isMetaLine(line: String): Boolean {
        val lowered = line.lowercase()
        val keywords = listOf(
            "title",
            "raga",
            "ragam",
            "raagam",
            "tala",
            "talam",
            "taala",
            "composer",
            "composed by",
            "deity",
            "temple",
            "kshetra",
            "kshetram",
            "kshetra",
            "kshethra",
            "kriti",
            "krithi",
            "raagam",
            "raga",
            "language"
        )
        return keywords.any { lowered.contains(it) }
    }

    private fun isBoilerplate(line: String): Boolean {
        val lowered = line.lowercase()
        val patterns = listOf(
            "powered by blogger",
            "newer post",
            "older post",
            "home",
            "subscribe to",
            "post a comment",
            "comments",
            "blog archive",
            "link to this post",
            "labels",
            "posted by",
            "all rights reserved",
            "copyright",
            "sign out",
            "sign in",
            "skip to main",
            "subscribe",
            "share",
            "related posts"
        )

        return patterns.any { lowered.contains(it) }
    }
}
