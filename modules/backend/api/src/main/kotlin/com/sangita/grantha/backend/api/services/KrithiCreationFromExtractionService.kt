package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionMethod
import com.sangita.grantha.shared.domain.model.import.CanonicalMusicalForm
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * TRACK-053: Creates new Krithi records from extraction results when no existing
 * Krithi matches the extracted composition.
 *
 * Responsibilities:
 *  - Resolve or create reference entities (composer, raga, tala, deity)
 *  - Create a DRAFT Krithi record
 *  - Persist canonical sections and lyric variants
 *  - Create source evidence linking the Krithi to its extraction
 *  - Write audit log entries for all creations
 */
class KrithiCreationFromExtractionService(
    private val dal: SangitaDal,
    private val normalizer: NameNormalizationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Create a new Krithi from a [CanonicalExtractionDto].
     *
     * @return the newly created Krithi's [Uuid], or `null` if creation was skipped
     *         (e.g. composer could not be resolved).
     */
    suspend fun createFromExtraction(
        extraction: CanonicalExtractionDto,
        extractionTaskId: Uuid,
    ): Uuid? {
        // ── 1. Resolve composer (required) ──────────────────────────────────
        val composerNormalized = normalizer.normalizeComposer(extraction.composer)
        if (composerNormalized.isNullOrBlank()) {
            logger.warn("Cannot normalise composer '${extraction.composer}' for '${extraction.title}', skipping")
            return null
        }
        val composer = dal.composers.findOrCreate(
            name = extraction.composer,
            nameNormalized = composerNormalized,
        )
        logger.debug("Resolved composer '${extraction.composer}' -> ${composer.id} (${composer.name})")

        // ── 2. Resolve ragas ────────────────────────────────────────────────
        val ragaJavaIds = extraction.ragas.mapNotNull { ragaDto ->
            val ragaNormalized = normalizer.normalizeRaga(ragaDto.name)
                ?.takeUnless { isPlaceholderRagaNormalized(it) }
                ?: return@mapNotNull null
            val raga = dal.ragas.findOrCreate(
                name = ragaDto.name,
                nameNormalized = ragaNormalized,
            )
            logger.debug("Resolved raga '${ragaDto.name}' -> ${raga.id} (${raga.name})")
            raga.id.toJavaUuid()
        }.distinct()

        // ── 3. Resolve tala ─────────────────────────────────────────────────
        val talaNormalized = normalizer.normalizeTala(extraction.tala)
        val tala = dal.talas.findOrCreate(
            name = extraction.tala,
            nameNormalized = talaNormalized,
        )
        logger.debug("Resolved tala '${extraction.tala}' -> ${tala.id} (${tala.name})")

        // ── 4. Resolve deity (optional) ─────────────────────────────────────
        val deityJavaId: UUID? = extraction.deity?.let { deityName ->
            val deityNormalized = normalizer.normalizeDeity(deityName)
            val deity = dal.deities.findOrCreate(
                name = deityName,
                nameNormalized = deityNormalized,
            )
            logger.debug("Resolved deity '$deityName' -> ${deity.id} (${deity.name})")
            deity.id.toJavaUuid()
        }

        // ── 5. Map enums ────────────────────────────────────────────────────
        val musicalForm = when (extraction.musicalForm) {
            CanonicalMusicalForm.KRITHI -> MusicalForm.KRITHI
            CanonicalMusicalForm.VARNAM -> MusicalForm.VARNAM
            CanonicalMusicalForm.SWARAJATHI -> MusicalForm.SWARAJATHI
        }

        val isRagamalika = ragaJavaIds.size > 1

        // Infer primary language from the first lyric variant, default to Sanskrit
        val primaryLanguage = extraction.lyricVariants.firstOrNull()?.language?.let { lang ->
            runCatching { LanguageCode.valueOf(lang.uppercase()) }.getOrNull()
        } ?: LanguageCode.SA

        // ── 6. Normalise title ──────────────────────────────────────────────
        val titleNormalized = normalizer.normalizeTitle(extraction.title)
            ?.takeIf { it.isNotBlank() }
            ?: extraction.alternateTitle
                ?.let { normalizer.normalizeTitle(it) }
                ?.takeIf { it.isNotBlank() }
            ?: extraction.title.trim().lowercase()

        // ── 7. Create Krithi ────────────────────────────────────────────────
        val krithi = dal.krithis.create(
            KrithiCreateParams(
                title = extraction.title,
                titleNormalized = titleNormalized,
                composerId = composer.id.toJavaUuid(),
                musicalForm = musicalForm,
                primaryLanguage = primaryLanguage,
                primaryRagaId = ragaJavaIds.firstOrNull(),
                talaId = tala.id.toJavaUuid(),
                deityId = deityJavaId,
                isRagamalika = isRagamalika,
                ragaIds = ragaJavaIds,
                workflowState = WorkflowState.DRAFT,
                notes = "Auto-created from extraction [${extraction.sourceName}]",
            ),
        )
        logger.info("Created Krithi '${extraction.title}' -> ${krithi.id}")

        // ── 8. Create canonical sections ────────────────────────────────────
        if (extraction.sections.isNotEmpty()) {
            val sectionTriples = extraction.sections.map { section ->
                Triple(section.type.name, section.order - 1, section.label)
            }
            dal.krithis.saveSections(krithi.id, sectionTriples)
        }

        // ── 9. Create lyric variants and per-section lyrics ─────────────────
        if (extraction.lyricVariants.isNotEmpty()) {
            persistLyricVariants(krithi.id, extraction)
        }

        // ── 10. Create source evidence ──────────────────────────────────────
        val contributedFields = buildContributedFields(extraction)
        val rawExtractionJson = json.encodeToString(CanonicalExtractionDto.serializer(), extraction)

        dal.sourceEvidence.createEvidence(
            krithiId = krithi.id,
            sourceUrl = extraction.sourceUrl,
            sourceName = extraction.sourceName,
            sourceTier = extraction.sourceTier,
            sourceFormat = mapExtractionMethodToFormat(extraction.extractionMethod),
            extractionMethod = extraction.extractionMethod.name,
            pageRange = extraction.pageRange,
            checksum = extraction.checksum,
            confidence = null,
            contributedFields = contributedFields,
            rawExtraction = rawExtractionJson,
        )

        // ── 11. Audit log ───────────────────────────────────────────────────
        dal.auditLogs.append(
            action = "CREATE_KRITHI_FROM_EXTRACTION",
            entityTable = "krithis",
            entityId = krithi.id,
            metadata = buildString {
                append("""{"sourceUrl":"${extraction.sourceUrl}",""")
                append(""""sourceName":"${extraction.sourceName}",""")
                append(""""extractionTaskId":"$extractionTaskId",""")
                append(""""composerResolved":"${composer.name}",""")
                append(""""ragaCount":${ragaJavaIds.size},""")
                append(""""sectionCount":${extraction.sections.size},""")
                append(""""lyricVariantCount":${extraction.lyricVariants.size}}""")
            },
        )

        return krithi.id
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private suspend fun persistLyricVariants(
        krithiId: Uuid,
        extraction: CanonicalExtractionDto,
    ) {
        val savedSections = dal.krithis.getSections(krithiId)
        var isFirst = true

        for (variant in extraction.lyricVariants) {
            val language = runCatching { LanguageCode.valueOf(variant.language.uppercase()) }.getOrNull()
            val script = runCatching { ScriptCode.valueOf(variant.script.uppercase()) }.getOrNull()
            if (language == null || script == null) {
                logger.warn(
                    "Skipping lyric variant with unknown language='${variant.language}' / " +
                        "script='${variant.script}' for Krithi $krithiId"
                )
                continue
            }

            val fullLyrics = variant.sections
                .sortedBy { it.sectionOrder }
                .joinToString("\n\n") { it.text }

            val lyricVariant = dal.krithis.createLyricVariant(
                krithiId = krithiId,
                language = language,
                script = script,
                lyrics = fullLyrics,
                isPrimary = isFirst,
                sourceReference = extraction.sourceUrl,
            )
            isFirst = false

            // Map lyric sections to saved Krithi sections by order index
            if (savedSections.isNotEmpty() && variant.sections.isNotEmpty()) {
                val sectionPairs = variant.sections.mapNotNull { lyricSection ->
                    val matchingSection = savedSections.find {
                        it.orderIndex == lyricSection.sectionOrder - 1
                    }
                    matchingSection?.let { it.id.toJavaUuid() to lyricSection.text }
                }
                if (sectionPairs.isNotEmpty()) {
                    dal.krithis.saveLyricVariantSections(lyricVariant.id, sectionPairs)
                }
            }
        }
    }

    private fun buildContributedFields(extraction: CanonicalExtractionDto): List<String> {
        val fields = mutableListOf("title", "composer", "tala")
        if (extraction.ragas.isNotEmpty()) fields.add("raga")
        if (extraction.sections.isNotEmpty()) fields.add("sections")
        if (extraction.deity != null) fields.add("deity")
        if (extraction.temple != null) fields.add("temple")
        extraction.lyricVariants.forEach { variant ->
            fields.add("lyrics_${variant.language}")
        }
        return fields
    }

    private fun mapExtractionMethodToFormat(method: CanonicalExtractionMethod): String = when (method) {
        CanonicalExtractionMethod.PDF_PYMUPDF, CanonicalExtractionMethod.PDF_OCR -> "PDF"
        CanonicalExtractionMethod.HTML_JSOUP, CanonicalExtractionMethod.HTML_JSOUP_GEMINI -> "HTML"
        CanonicalExtractionMethod.DOCX_PYTHON -> "DOCX"
        CanonicalExtractionMethod.MANUAL -> "MANUAL"
        CanonicalExtractionMethod.TRANSLITERATION -> "HTML"
    }

    private fun isPlaceholderRagaNormalized(normalized: String): Boolean {
        val value = normalized.trim().lowercase()
        return value.isBlank() || value in setOf("unknown", "na", "n a", "none")
    }
}
