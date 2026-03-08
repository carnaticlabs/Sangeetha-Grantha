package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.RagaSectionDto
import com.sangita.grantha.backend.api.services.scraping.KrithiStructureParser
import com.sangita.grantha.backend.api.services.scraping.StructuralVotingEngine
import kotlinx.serialization.json.Json
import java.net.URI
import kotlin.uuid.Uuid

/**
 * Handles persisting lyric variants (multi-language, sectioned) for a Krithi
 * from import data and scraped metadata.
 */
class LyricVariantPersistenceService(
    private val dal: SangitaDal,
) {
    private val structuralVotingEngine = StructuralVotingEngine()
    private val composerSourcePriority = mapOf(
        "muthuswami dikshitar" to listOf("guruguha.org"),
        "tyagaraja" to listOf("thyagarajavaibhavam.blogspot.com"),
        "swathi thirunal" to listOf("swathithirunalfestival.org"),
        "general" to listOf("karnatik.com", "shivkumar.org")
    )

    suspend fun persistLyricVariants(
        krithiId: Uuid,
        importData: ImportedKrithiDto,
        overrides: com.sangita.grantha.backend.api.models.ImportOverridesDto?,
    ) {
        val sourceKey = importData.sourceKey

        if (overrides?.lyrics != null) {
            // Unstructured override lyrics
            dal.krithiLyrics.createLyricVariant(
                krithiId = krithiId,
                language = LanguageCode.EN, // Default for unstructured overrides
                script = ScriptCode.LATIN,
                lyrics = overrides.lyrics,
                isPrimary = false,
                sourceReference = sourceKey
            )
        } else if (importData.parsedPayload != null) {
            try {
                val metadata = Json.decodeFromString<ScrapedKrithiMetadata>(importData.parsedPayload!!)

                // TRACK-032: Multi-language lyric variants from scrape
                if (!metadata.lyricVariants.isNullOrEmpty()) {
                    val variants = metadata.lyricVariants.toMutableList()

                    // 1. Recover missing sections for each variant using KrithiStructureParser
                    variants.forEachIndexed { idx, v ->
                        if (v.sections.isNullOrEmpty() && !v.lyrics.isNullOrBlank()) {
                            val recovered = KrithiStructureParser().buildBlocks(v.lyrics.replace("\\n", "\n")).blocks
                                .mapNotNull { block: KrithiStructureParser.TextBlock ->
                                    val type = parseRagaSectionDto(block.label)
                                    if (type != null && type != RagaSectionDto.OTHER) {
                                        ScrapedSectionDto(type = type, text = block.lines.joinToString("\n"))
                                    } else null
                                }
                            if (recovered.isNotEmpty()) {
                                variants[idx] = v.copy(sections = recovered)
                            }
                        }
                    }

                    // 2. Ensure canonical section structure exists
                    val savedSections = dal.krithis.getSections(krithiId)
                    if (savedSections.isEmpty()) {
                        // Determine canonical section structure from metadata/variants
                        val rawSections = metadata.sections ?: emptyList()
                        val deduplicated = mutableListOf<ScrapedSectionDto>()
                        var previousWasCharanam = false
                        for (s in rawSections) {
                            if (s.type == RagaSectionDto.PALLAVI && previousWasCharanam) break
                            if (s.type == RagaSectionDto.CHARANAM || s.type == RagaSectionDto.SAMASHTI_CHARANAM) previousWasCharanam = true
                            deduplicated.add(s)
                        }

                        val authoritySource = isAuthoritySourceForComposer(sourceKey, importData.rawComposer)
                        val candidates = variants.mapNotNull { v ->
                            v.sections?.let { StructuralVotingEngine.SectionCandidate(it, authoritySource, "variant:${v.language}") }
                        }.toMutableList()
                        if (deduplicated.isNotEmpty()) {
                            candidates.add(StructuralVotingEngine.SectionCandidate(deduplicated, authoritySource, "metadata"))
                        }

                        val sectionStructure = structuralVotingEngine.pickBestStructure(candidates)
                            // Rule 1: MKS is never a top-level section — exclude from canonical skeleton
                            .filter { it.type != RagaSectionDto.MADHYAMA_KALA }
                        if (sectionStructure.isNotEmpty()) {
                            val sectionsToSave = sectionStructure.mapIndexed { index, section ->
                                Triple(section.type.name, index + 1, null as String?)
                            }
                            dal.krithis.saveSections(krithiId, sectionsToSave)
                        }
                    }

                    val updatedSections = dal.krithis.getSections(krithiId)

                    // 3. Save each variant and link to sections
                    variants.forEachIndexed { index, scraped ->
                        val lang = parseLanguageCode(scraped.language) ?: LanguageCode.TE
                        val script = parseScriptCode(scraped.script) ?: ScriptCode.LATIN
                        val lyricsText = (scraped.lyrics?.takeIf { it.isNotBlank() }
                            ?: scraped.sections?.joinToString("\n\n") { "[${it.type.name}]\n${it.text}" }.takeIf { it?.isNotBlank() == true }
                            ?: "").replace("\\n", "\n")

                        val createdVariant = dal.krithiLyrics.createLyricVariant(
                            krithiId = krithiId,
                            language = lang,
                            script = script,
                            lyrics = lyricsText,
                            isPrimary = false,
                            sourceReference = sourceKey
                        )

                        val variantSections = scraped.sections
                        if (!variantSections.isNullOrEmpty() && updatedSections.isNotEmpty()) {
                            // Match variant sections to canonical by type + sequential occurrence
                            val typeQueues = variantSections
                                .filter { it.text.isNotBlank() }
                                .groupBy { it.type }
                                .mapValues { (_, v) -> v.toMutableList() }
                            val lyricSections = updatedSections.mapNotNull { savedSection ->
                                val sectionType = parseRagaSectionDto(savedSection.sectionType)
                                val queue = if (sectionType != null) typeQueues[sectionType] else null
                                val match = queue?.removeFirstOrNull()
                                if (match != null) {
                                    savedSection.id.toJavaUuid() to match.text
                                } else null
                            }
                            if (lyricSections.isNotEmpty()) {
                                dal.krithiLyrics.saveLyricVariantSections(createdVariant.id, lyricSections)
                            }
                        }
                    }
                } else {
                    // Single primary variant (existing behaviour)
                    val effectiveSections = when {
                        !metadata.sections.isNullOrEmpty() -> metadata.sections
                        !metadata.lyrics.isNullOrBlank() -> parseSectionHeadersFromLyrics(metadata.lyrics.replace("\\n", "\n"))
                        else -> emptyList()
                    }
                    if (effectiveSections.isNotEmpty()) {
                        val lyricVariant = dal.krithiLyrics.createLyricVariant(
                            krithiId = krithiId,
                            language = parseLanguageCode(metadata.language) ?: LanguageCode.TE,
                            script = ScriptCode.LATIN,
                            lyrics = metadata.lyrics ?: effectiveSections.joinToString("\n\n") { "[${it.type.name}]\n${it.text}" },
                            isPrimary = false,
                            sourceReference = sourceKey
                        )
                        val savedSections = dal.krithis.getSections(krithiId)
                        val lyricSections = savedSections.mapNotNull { savedSection ->
                            val originalSection = effectiveSections.getOrNull(savedSection.orderIndex - 1)
                            if (originalSection != null && !originalSection.text.isBlank()) {
                                savedSection.id.toJavaUuid() to originalSection.text
                            } else null
                        }
                        if (lyricSections.isNotEmpty()) {
                            dal.krithiLyrics.saveLyricVariantSections(lyricVariant.id, lyricSections)
                        }
                    } else if (!metadata.lyrics.isNullOrBlank()) {
                        dal.krithiLyrics.createLyricVariant(
                            krithiId = krithiId,
                            language = parseLanguageCode(metadata.language) ?: LanguageCode.TE,
                            script = ScriptCode.LATIN,
                            lyrics = metadata.lyrics,
                            isPrimary = false,
                            sourceReference = sourceKey
                        )
                    }
                }
            } catch (e: Exception) {
                println("Error processing scraped metadata: ${e.message}")
            }
        }
    }

    /** TRACK-032: Map scraped language string (SA, TA, etc.) to LanguageCode. */
    fun parseLanguageCode(value: String?): LanguageCode? {
        if (value.isNullOrBlank()) return null
        return when (value.trim().uppercase()) {
            "SA" -> LanguageCode.SA
            "TA" -> LanguageCode.TA
            "TE" -> LanguageCode.TE
            "KN" -> LanguageCode.KN
            "ML" -> LanguageCode.ML
            "HI" -> LanguageCode.HI
            "EN" -> LanguageCode.EN
            else -> null
        }
    }

    /** TRACK-032: Map scraped script string (devanagari, tamil, etc.) to ScriptCode. */
    fun parseScriptCode(value: String?): ScriptCode? {
        if (value.isNullOrBlank()) return null
        return when (value.trim().lowercase()) {
            "devanagari" -> ScriptCode.DEVANAGARI
            "tamil" -> ScriptCode.TAMIL
            "telugu" -> ScriptCode.TELUGU
            "kannada" -> ScriptCode.KANNADA
            "malayalam" -> ScriptCode.MALAYALAM
            "latin" -> ScriptCode.LATIN
            else -> null
        }
    }

    /** TRACK-032: Map TextBlocker label to RagaSectionDto. */
    fun parseRagaSectionDto(label: String): RagaSectionDto? {
        return when (label.uppercase()) {
            "PALLAVI" -> RagaSectionDto.PALLAVI
            "ANUPALLAVI" -> RagaSectionDto.ANUPALLAVI
            "CHARANAM", "CARANAM" -> RagaSectionDto.CHARANAM
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

    /**
     * Parse section headers (Pallavi, Anupallavi, Charanam, Samashti Charanam, etc.) from lyrics text
     * when the scraper did not return structured sections. Used as fallback so Lyrics tab shows sections.
     */
    fun parseSectionHeadersFromLyrics(lyrics: String): List<ScrapedSectionDto> {
        val pattern = Regex("""(?mi)^\s*(Pallavi|Anupallavi|Charanam|Samashti\s+Charanam|Chittaswaram)\s*:?\s*$""")
        val matches = pattern.findAll(lyrics).toList()
        if (matches.isEmpty()) return emptyList()
        val sectionTypeMap: (String) -> RagaSectionDto = { raw ->
            when (raw.trim().lowercase()) {
                "pallavi" -> RagaSectionDto.PALLAVI
                "anupallavi" -> RagaSectionDto.ANUPALLAVI
                "charanam" -> RagaSectionDto.CHARANAM
                "samashti charanam" -> RagaSectionDto.SAMASHTI_CHARANAM
                "chittaswaram" -> RagaSectionDto.CHITTASWARAM
                else -> RagaSectionDto.OTHER
            }
        }
        return matches.mapIndexed { i, match ->
            val type = sectionTypeMap(match.groupValues[1])
            val text = lyrics.substring(
                match.range.last + 1,
                if (i + 1 < matches.size) matches[i + 1].range.first else lyrics.length
            ).trim()
            ScrapedSectionDto(type = type, text = text)
        }.filter { it.text.isNotBlank() }
    }

    fun isAuthoritySourceForComposer(sourceKey: String?, composer: String?): Boolean {
        if (sourceKey.isNullOrBlank() || composer.isNullOrBlank()) return false
        val normalizedComposer = composer.trim().lowercase()
        val authorityHosts = composerSourcePriority[normalizedComposer] ?: composerSourcePriority["general"] ?: emptyList()
        val host = try {
            URI(sourceKey).host?.lowercase()
        } catch (e: Exception) {
            null
        }
        if (host.isNullOrBlank()) return false
        return authorityHosts.any { host.contains(it) }
    }
}
