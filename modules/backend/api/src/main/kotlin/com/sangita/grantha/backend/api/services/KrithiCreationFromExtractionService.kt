package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.repositories.RevisionWrite
import com.sangita.grantha.backend.dal.repositories.SectionRevisionWrite
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionMethod
import com.sangita.grantha.shared.domain.model.import.CanonicalMusicalForm
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

        // ── 7. Create Krithi (with dedup guard) ─────────────────────────────
        // TRACK-059-fix: Check if a Krithi with the same normalized title + composer already exists.
        // This guards against duplicates from within-batch races or payload duplicates.
        val existingDuplicates = dal.krithiSearch.findDuplicateCandidates(
            titleNormalized = titleNormalized,
            composerId = composer.id.toJavaUuid(),
        )
        if (existingDuplicates.isNotEmpty()) {
            val existing = existingDuplicates.first()
            logger.info("Dedup guard: Krithi '${extraction.title}' already exists as ${existing.id} " +
                "(title_normalized='$titleNormalized'), skipping creation")
            return existing.id
        }

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

        // ── 10.5 Versioned canon: revision #1 + per-section provenance ─────
        // (ADR-014 / TRACK-117 — captured at creation, never backfilled)
        val sourceDocumentId = dal.revisions.ensureSourceDocumentForSource(
            sourceName = extraction.sourceName,
            sourceUrl = extraction.sourceUrl,
            sourceTier = extraction.sourceTier,
            sourceFormat = mapExtractionMethodToFormat(extraction.extractionMethod),
            checksum = extraction.checksum,
            pageRange = extraction.pageRange,
        )
        dal.revisions.appendRevision(
            RevisionWrite(
                krithiId = krithi.id.toJavaUuid(),
                changeKind = "IMPORT",
                changeReason = "Created from extraction [${extraction.sourceName}]",
                extractionId = extractionTaskId.toJavaUuid(),
                sections = buildSectionRevisionWrites(
                    extraction, extractionTaskId.toJavaUuid(), sourceDocumentId,
                ),
            ),
        )

        // ── 11. Audit log (use buildJsonObject for safe escaping) ──────────
        val auditMetadata = buildJsonObject {
            put("sourceUrl", extraction.sourceUrl)
            put("sourceName", extraction.sourceName)
            put("extractionTaskId", extractionTaskId.toString())
            put("composerResolved", composer.name)
            put("ragaCount", ragaJavaIds.size)
            put("sectionCount", extraction.sections.size)
            put("lyricVariantCount", extraction.lyricVariants.size)
        }
        dal.auditLogs.append(
            action = "CREATE_KRITHI_FROM_EXTRACTION",
            entityTable = "krithis",
            entityId = krithi.id,
            metadata = auditMetadata.toString(),
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

            val lyricVariant = dal.krithiLyrics.createLyricVariant(
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
                    dal.krithiLyrics.saveLyricVariantSections(lyricVariant.id, sectionPairs)
                }
            }
        }
    }

    /**
     * One revision-section row per (canonical section × lyric variant) — the
     * re-materializable "what" of the krithi at revision time. Structural
     * sections with no lyric text in any variant still get one skeleton row.
     */
    private fun buildSectionRevisionWrites(
        extraction: CanonicalExtractionDto,
        extractionId: UUID,
        sourceDocumentId: UUID,
    ): List<SectionRevisionWrite> {
        val sectionsByOrder = extraction.sections.associateBy { it.order }
        val writes = mutableListOf<SectionRevisionWrite>()

        for (variant in extraction.lyricVariants) {
            val language = runCatching { LanguageCode.valueOf(variant.language.uppercase()) }.getOrNull()
            val script = runCatching { ScriptCode.valueOf(variant.script.uppercase()) }.getOrNull()
            for (lyricSection in variant.sections) {
                val skeleton = sectionsByOrder[lyricSection.sectionOrder]
                writes += SectionRevisionWrite(
                    sectionType = skeleton?.type?.name ?: "OTHER",
                    orderIndex = lyricSection.sectionOrder - 1,
                    label = skeleton?.label,
                    language = language,
                    script = script,
                    text = lyricSection.text,
                    extractionId = extractionId,
                    sourceDocumentId = sourceDocumentId,
                )
            }
        }

        // Skeleton rows for sections no lyric variant covered
        val coveredOrders = writes.map { it.orderIndex }.toSet()
        for (section in extraction.sections) {
            val orderIndex = section.order - 1
            if (orderIndex !in coveredOrders) {
                writes += SectionRevisionWrite(
                    sectionType = section.type.name,
                    orderIndex = orderIndex,
                    label = section.label,
                    text = "",
                    extractionId = extractionId,
                    sourceDocumentId = sourceDocumentId,
                )
            }
        }
        return writes.sortedBy { it.orderIndex }
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
