package com.sangita.grantha.backend.api.services.scraping

import com.sangita.grantha.shared.domain.model.RagaSectionDto

class KrithiStructureParser {
    data class TextBlock(val label: String, val lines: List<String>)
    data class PromptBlocks(
        val metaLines: List<String>,
        val blocks: List<TextBlock>
    )
    
    data class ScrapedSection(val type: RagaSectionDto, val text: String, val label: String? = null)
    data class ScrapedVariant(
        val language: String,
        val script: String,
        val lyrics: String,
        val sections: List<ScrapedSection>
    )

    /** Deterministic metadata hints extracted from meta lines (e.g. "abhayAmbA jagadambA - rAgaM kalyANi - tALaM Adi"). */
    data class MetadataHints(
        val title: String? = null,
        val raga: String? = null,
        val tala: String? = null
    )

    // Matches "1. SrI rAgaM" or "Arabhi rAgaM"
    private val ragaPattern = Regex("""^(\d+\.\s*)?(.+?)\s+rAgaM\s*$""", RegexOption.IGNORE_CASE)
    // Matches "vilOma - mOhana rAgaM"
    private val vilomaPattern = Regex("""^vilOma\s*-\s*(.+?)\s+rAgaM\s*$""", RegexOption.IGNORE_CASE)

    // Matches guru-guha meta line: "abhayAmbA jagadambA - rAgaM kalyANi - tALaM Adi"
    private val metaRagaTalaPattern = Regex(
        """^(.+?)\s*[-–—]\s*r[Aa][Aa]?ga[Mm]?\s+(.+?)\s*[-–—]\s*t[Aa][Aa]?[lL]a[Mm]?\s+(.+?)\s*$""",
        RegexOption.IGNORE_CASE
    )

    // Matches guru-guha meta line without tala: "abhayAmbA jagadambA - rAgaM kalyANi"
    private val metaRagaPattern = Regex(
        """^(.+?)\s*[-–—]\s*r[Aa][Aa]?ga[Mm]?\s+(.+?)\s*$""",
        RegexOption.IGNORE_CASE
    )

    fun buildBlocks(rawText: String): PromptBlocks {
        val lines = rawText
            .replace("\\n", "\n") // Unescape literal \n strings if present
            .lines()
            .map { normalizeLine(it.trim()) }
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

    /**
     * Extract raga/tala/title hints from meta lines.
     *
     * Parses the common guru-guha blogspot pattern:
     * `abhayAmbA jagadambA - rAgaM kalyANi - tALaM Adi`
     */
    fun extractMetadataHints(rawText: String): MetadataHints {
        val promptBlocks = buildBlocks(rawText)
        for (line in promptBlocks.metaLines) {
            val match = metaRagaTalaPattern.find(line.trim())
            if (match != null) {
                return MetadataHints(
                    title = cleanTitle(match.groupValues[1].trim()).takeIf { it.isNotBlank() },
                    raga = match.groupValues[2].trim().takeIf { it.isNotBlank() },
                    tala = match.groupValues[3].trim().takeIf { it.isNotBlank() }
                )
            }
            // Fallback for lines without Tala
            val ragaMatch = metaRagaPattern.find(line.trim())
            if (ragaMatch != null) {
                return MetadataHints(
                    title = cleanTitle(ragaMatch.groupValues[1].trim()).takeIf { it.isNotBlank() },
                    raga = ragaMatch.groupValues[2].trim().takeIf { it.isNotBlank() },
                    tala = null
                )
            }
        }
        return MetadataHints()
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("^Guru Guha Vaibhavam:\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^Dikshitar Kriti\\s*[-–—]\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun extractSections(rawText: String): List<ScrapedSection> {
        val blocks = buildBlocks(rawText).blocks
        if (blocks.isEmpty()) return emptyList()
        val sections = mutableListOf<ScrapedSection>()

        var foundFirstSection = false

        for (block in blocks) {
            // If we encounter a language header, it often marks the start of a new script section.
            // If we have already collected sections (from the primary block), stop to avoid duplicates.
            if (block.label in LANGUAGE_LABELS) {
                if (foundFirstSection) {
                    break
                }
                continue
            }

            val type = mapLabelToSection(block.label) ?: continue
            foundFirstSection = true

            // Parse lines within the block for Ragamalika sub-sections
            var currentLabel: String? = null
            val currentLines = mutableListOf<String>()

            fun flushSubSection() {
                if (currentLines.isNotEmpty()) {
                    sections.add(ScrapedSection(type = type, text = currentLines.joinToString("\n"), label = currentLabel))
                    currentLines.clear()
                }
            }

            for (line in block.lines) {
                val vilomaMatch = vilomaPattern.find(line)
                val ragaMatch = ragaPattern.find(line)

                if (vilomaMatch != null) {
                    flushSubSection()
                    currentLabel = "Viloma - ${vilomaMatch.groupValues[1].trim()}"
                    // The line itself is the header, don't add it to content, OR add it?
                    // Usually headers are separate. But in this text, they are line items.
                    // "vilOma - mOhana rAgaM" is the header. The next line is the lyric.
                    // BUT: "1. SrI rAgaM" is a header.
                    // We should preserve the header in the text if it's significant, but for "label" extraction, we treat it as a delimiter.
                    // Let's add it to the text for context, or skip it?
                    // The user requirement says: "And parse in tune with the lyrics".
                    // If we split sections, the header line becomes the "label". It shouldn't be in the body text of the *new* section ideally, or it's redundant.
                    // Let's SKIP adding the header line to the body, as it's now metadata (the label).
                } else if (ragaMatch != null) {
                    flushSubSection()
                    currentLabel = ragaMatch.groupValues[2].trim()
                } else {
                    currentLines.add(line)
                }
            }
            flushSubSection()
        }
        return sections
    }

    fun extractLyricVariants(rawText: String): List<ScrapedVariant> {
        val blocks = buildBlocks(rawText).blocks
        if (blocks.isEmpty()) return emptyList()

        val variants = mutableListOf<ScrapedVariant>()
        
        var currentLanguage: String? = null
        var currentScript: String? = null
        val currentSections = mutableListOf<ScrapedSection>()
        val currentLyricsBuilder = StringBuilder()

        fun flushVariant() {
            if (currentLanguage != null && (currentSections.isNotEmpty() || currentLyricsBuilder.isNotEmpty())) {
                variants.add(
                    ScrapedVariant(
                        language = currentLanguage!!,
                        script = currentScript ?: "latin",
                        lyrics = currentLyricsBuilder.toString().trim(),
                        sections = currentSections.toList()
                    )
                )
            }
            currentSections.clear()
            currentLyricsBuilder.clear()
            currentLanguage = null
            currentScript = null
        }

        // Iterate blocks to find Language Groups
        for (block in blocks) {
            // Check if this block starts a new Language Context
            // We only care about script languages, not "MEANING" or "NOTES"
            if (block.label in LANGUAGE_LABELS && block.label !in setOf("MEANING", "GIST", "NOTES", "WORD_DIVISION", "VARIATIONS")) {
                // If we were already building a variant, flush it
                flushVariant()
                
                // Start new variant
                currentLanguage = mapLabelToLanguageCode(block.label)
                currentScript = mapLabelToScriptCode(block.label)
                
                // If the block itself has text content, append it
                val blockText = block.lines.joinToString("\n").trim()
                if (blockText.isNotBlank()) {
                    currentLyricsBuilder.append(blockText).append("\n\n")
                }
                continue
            }

            // If we are inside a language context, accumulate sections/lyrics
            if (currentLanguage != null) {
                // If we encounter a meaning or notes block, stop accumulating for this language
                if (block.label in setOf("MEANING", "GIST", "NOTES", "WORD_DIVISION", "VARIATIONS")) {
                    flushVariant()
                    continue
                }

                val blockText = block.lines.joinToString("\n").trim()
                if (blockText.isBlank()) continue

                val type = mapLabelToSection(block.label)
                if (type != null) {
                    // For variants, we currently don't parse sub-sections deep inside because regexes are language-specific (Latin script).
                    // We just store the block as a whole section.
                    currentSections.add(ScrapedSection(type, blockText, null))
                }
                
                // Always append to full lyrics blob for safety/fallback
                if (currentLyricsBuilder.isNotEmpty()) {
                    currentLyricsBuilder.append("\n\n")
                }
                if (type != null) {
                    currentLyricsBuilder.append("[${type.name}]\n")
                }
                currentLyricsBuilder.append(blockText)
            }
        }
        
        flushVariant()
        return variants
    }

    private fun mapLabelToLanguageCode(label: String): String {
        return when(label.uppercase()) {
            "SANSKRIT", "DEVANAGARI" -> "SA"
            "TAMIL" -> "TA"
            "TELUGU" -> "TE"
            "KANNADA" -> "KN"
            "MALAYALAM" -> "ML"
            "HINDI" -> "HI"
            "ENGLISH", "LATIN" -> "EN" // Often transliteration
            else -> "EN"
        }
    }

    private fun mapLabelToScriptCode(label: String): String {
        return when(label.uppercase()) {
            "SANSKRIT", "DEVANAGARI", "HINDI" -> "devanagari"
            "TAMIL" -> "tamil"
            "TELUGU" -> "telugu"
            "KANNADA" -> "kannada"
            "MALAYALAM" -> "malayalam"
            "ENGLISH", "LATIN" -> "latin"
            else -> "latin"
        }
    }

    private fun mapLabelToSection(label: String): RagaSectionDto? {
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

    private fun splitIntoBlocks(lines: List<String>): List<TextBlock> {
        if (lines.isEmpty()) return emptyList()

        val blocks = mutableListOf<TextBlock>()
        var currentLabel = "UNLABELED"
        val currentLines = mutableListOf<String>()

        fun flush() {
            // Emit block if it has content OR if it's a language marker (even if empty)
            // This ensures we preserve language boundaries like "English" -> "Pallavi"
            if (currentLines.isNotEmpty() || currentLabel in LANGUAGE_LABELS) {
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
    
    private val LANGUAGE_LABELS = setOf(
        "DEVANAGARI", "TAMIL", "TELUGU", "KANNADA", "MALAYALAM", 
        "ENGLISH", "LATIN", "SANSKRIT", "HINDI",
        "WORD_DIVISION", "MEANING", "GIST", "NOTES", "VARIATIONS"
    )

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

    private fun detectSectionHeader(line: String): HeaderMatch? {
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

    private fun isMetaLine(line: String): Boolean {
        // Skip structural headers that look like meta lines (e.g. "1. SrI rAgaM")
        if (ragaPattern.containsMatchIn(line) || vilomaPattern.containsMatchIn(line)) {
            return false
        }

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

    private fun normalizeLine(line: String): String {
        if (line.isBlank()) return line
        // Normalize Tamil subscripts (₁-₄) and other Unicode subscripts used in lyric notes.
        return line.replace(Regex("[\\u2080-\\u2089]"), "")
    }

    private fun isBoilerplate(line: String): Boolean {
        val lowered = line.lowercase()
        
        // Pronunciation guide patterns common in Vaibhavam blogs
        if (lowered.contains("a i i u u")) return true
        if (lowered.contains("ch j jh")) return true
        if (lowered.contains("ph b bh m")) return true
        if (lowered.contains("pronunciation guide")) return true

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
