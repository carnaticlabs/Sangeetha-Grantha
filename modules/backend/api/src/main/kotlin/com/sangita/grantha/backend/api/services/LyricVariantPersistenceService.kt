package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.RagaSectionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalSectionType
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

/**
 * Persists lyric variants for a Krithi from a canonical extraction payload.
 */
class LyricVariantPersistenceService(
    private val dal: SangitaDal,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val lenientJson = Json { ignoreUnknownKeys = true }

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
            val canonicalResult = try {
                lenientJson.decodeFromString<CanonicalExtractionDto>(importData.parsedPayload!!)
            } catch (e: Exception) {
                logger.error(
                    "Failed to process parsed payload for krithi {} — expected CanonicalExtractionDto: {}",
                    krithiId,
                    e.message,
                    e,
                )
                null
            }
            if (canonicalResult != null) {
                persistFromCanonical(krithiId, canonicalResult, importData.sourceKey)
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
        // TRACK-133: also replace when the section *count* changed (e.g. 1 OTHER blob →
        // 10 ragamalika OTHER stanzas). Without this, reingest mapped variant text onto
        // the old canonical rows and trailing stanzas are dropped.
        val savedSections = dal.krithis.getSections(krithiId)
        val sectionsToSave = extraction.sections
            .filter { it.type != CanonicalSectionType.MADHYAMA_KALA } // Rule 1: MKS not top-level
            .mapIndexed { index, section ->
                Triple(
                    mapCanonicalSectionType(section.type).name,
                    index + 1,
                    section.label,
                )
            }
        val shouldUpdateSections = sectionsToSave.isNotEmpty() && (
            savedSections.isEmpty() ||
            savedSections.size != sectionsToSave.size ||
            (savedSections.all { it.sectionType == "OTHER" } &&
             extraction.sections.any { it.type != CanonicalSectionType.OTHER })
        )
        if (shouldUpdateSections) {
            dal.krithis.saveSections(krithiId, sectionsToSave)
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
}
