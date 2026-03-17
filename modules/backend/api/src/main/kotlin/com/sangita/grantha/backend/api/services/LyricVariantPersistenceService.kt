package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.RagaSectionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalSectionType
import com.sangita.grantha.backend.api.services.scraping.KrithiStructureParser
import com.sangita.grantha.backend.api.services.scraping.StructuralVotingEngine
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI
import kotlin.uuid.Uuid

/**
 * Handles persisting lyric variants (multi-language, sectioned) for a Krithi
 * from import data and scraped metadata.
 */
class LyricVariantPersistenceService(
    private val dal: SangitaDal,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val structuralVotingEngine = StructuralVotingEngine()
    private val lenientJson = Json { ignoreUnknownKeys = true }
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
            // Try CanonicalExtractionDto first (new Python extraction pipeline),
            // fall back to ScrapedKrithiMetadata (legacy Kotlin scraper)
            val canonicalResult = try {
                lenientJson.decodeFromString<CanonicalExtractionDto>(importData.parsedPayload!!)
            } catch (_: Exception) { null }

            if (canonicalResult != null) {
                persistFromCanonical(krithiId, canonicalResult, importData.sourceKey)
                return
            }

            try {
                val metadata = lenientJson.decodeFromString<ScrapedKrithiMetadata>(importData.parsedPayload!!)

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
                logger.error("Failed to process parsed payload for krithi {} — neither CanonicalExtractionDto nor ScrapedKrithiMetadata: {}", krithiId, e.message, e)
            }
        }
    }

    /**
     * Persist lyric variants from CanonicalExtractionDto (Python extraction pipeline).
     * The canonical format cleanly separates structure (sections) from text (lyricVariants).
     */
    private suspend fun persistFromCanonical(
        krithiId: Uuid,
        extraction: CanonicalExtractionDto,
        sourceKey: String?,
    ) {
        // 1. Ensure canonical section structure exists (or upgrade stale sections)
        // TRACK-097: If the extraction has proper section types (PALLAVI, ANUPALLAVI, etc.)
        // and the existing sections are all OTHER, replace them via saveSections (which diffs).
        val savedSections = dal.krithis.getSections(krithiId)
        val shouldUpdateSections = extraction.sections.isNotEmpty() && (
            savedSections.isEmpty() ||
            (savedSections.all { it.sectionType == "OTHER" } &&
             extraction.sections.any { it.type != CanonicalSectionType.OTHER })
        )
        if (shouldUpdateSections) {
            val sectionsToSave = extraction.sections
                .filter { it.type != CanonicalSectionType.MADHYAMA_KALA } // Rule 1: MKS not top-level
                .mapIndexed { index, section ->
                    Triple(
                        mapCanonicalSectionType(section.type).name,
                        index + 1,
                        section.label,
                    )
                }
            if (sectionsToSave.isNotEmpty()) {
                dal.krithis.saveSections(krithiId, sectionsToSave)
            }
        }

        val updatedSections = dal.krithis.getSections(krithiId)

        // 2. Persist each lyric variant
        for (variant in extraction.lyricVariants) {
            val lang = parseLanguageCode(variant.language) ?: LanguageCode.EN
            val script = parseScriptCode(variant.script) ?: ScriptCode.LATIN

            // Build full lyrics text from sections
            val lyricsText = variant.sections
                .sortedBy { it.sectionOrder }
                .joinToString("\n\n") { it.text }

            val createdVariant = dal.krithiLyrics.createLyricVariant(
                krithiId = krithiId,
                language = lang,
                script = script,
                lyrics = lyricsText,
                isPrimary = false,
                sourceReference = sourceKey,
            )

            // 3. Link variant sections to canonical sections by order
            if (updatedSections.isNotEmpty() && variant.sections.isNotEmpty()) {
                val lyricSections = variant.sections
                    .filter { it.text.isNotBlank() }
                    .mapNotNull { lyricSection ->
                        // Match by order — canonical sections use 1-based ordering
                        val matchedSection = updatedSections.find { saved ->
                            saved.orderIndex == lyricSection.sectionOrder
                        }
                        matchedSection?.let { it.id.toJavaUuid() to lyricSection.text }
                    }
                if (lyricSections.isNotEmpty()) {
                    dal.krithiLyrics.saveLyricVariantSections(createdVariant.id, lyricSections)
                }
            }
        }

        logger.info("Persisted {} lyric variants for krithi {} from canonical extraction", extraction.lyricVariants.size, krithiId)
    }

    /**
     * TRACK-094: Backfill lyrics for approved imports that have a parsed_payload
     * but no lyric variants persisted (e.g. due to the format schism bug).
     * Returns a report of how many were processed and any errors.
     */
    suspend fun backfillApprovedImports(dal: SangitaDal): BackfillReport {
        // Collect imports in any review-completed state (APPROVED, MAPPED, IN_REVIEW)
        // that have a mapped krithi and parsed payload but no lyric variants.
        val approved = (
            dal.imports.listImports(status = com.sangita.grantha.backend.dal.enums.ImportStatus.APPROVED) +
            dal.imports.listImports(status = com.sangita.grantha.backend.dal.enums.ImportStatus.MAPPED) +
            dal.imports.listImports(status = com.sangita.grantha.backend.dal.enums.ImportStatus.IN_REVIEW)
        )

        var processed = 0
        var skipped = 0
        var errored = 0
        val errors = mutableListOf<String>()

        for (importDto in approved) {
            val mappedId = importDto.mappedKrithiId ?: continue
            val payload = importDto.parsedPayload ?: run { skipped++; continue }

            // Check if this krithi already has lyric variants
            val existingVariants = dal.krithiLyrics.getLyricVariants(mappedId)
            if (existingVariants.isNotEmpty()) {
                skipped++
                continue
            }

            try {
                persistLyricVariants(mappedId, importDto, overrides = null)
                processed++
            } catch (e: Exception) {
                errored++
                errors.add("${importDto.id}: ${e.message}")
                logger.error("Backfill failed for import {}: {}", importDto.id, e.message, e)
            }
        }

        logger.info("Backfill complete: processed={}, skipped={}, errored={}", processed, skipped, errored)
        return BackfillReport(processed, skipped, errored, errors)
    }

    @kotlinx.serialization.Serializable
    data class BackfillReport(
        val processed: Int,
        val skipped: Int,
        val errored: Int,
        val errors: List<String>,
    )

    /** Map CanonicalSectionType to RagaSectionDto for DB persistence. */
    private fun mapCanonicalSectionType(type: CanonicalSectionType): RagaSectionDto = when (type) {
        CanonicalSectionType.PALLAVI -> RagaSectionDto.PALLAVI
        CanonicalSectionType.ANUPALLAVI -> RagaSectionDto.ANUPALLAVI
        CanonicalSectionType.CHARANAM -> RagaSectionDto.CHARANAM
        CanonicalSectionType.SAMASHTI_CHARANAM -> RagaSectionDto.SAMASHTI_CHARANAM
        CanonicalSectionType.CHITTASWARAM -> RagaSectionDto.CHITTASWARAM
        CanonicalSectionType.SWARA_SAHITYA -> RagaSectionDto.SWARA_SAHITYA
        CanonicalSectionType.MADHYAMA_KALA -> RagaSectionDto.MADHYAMA_KALA
        CanonicalSectionType.OTHER -> RagaSectionDto.OTHER
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
