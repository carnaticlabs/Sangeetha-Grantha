package com.sangita.grantha.backend.api.support

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.*
import com.sangita.grantha.shared.domain.model.import.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

object TestFixtures {
    data class ReferenceDataSeed(
        val composer: ComposerDto,
        val raga: RagaDto,
        val tala: TalaDto
    )

    suspend fun seedReferenceData(dal: SangitaDal): ReferenceDataSeed {
        val composer = dal.composers.create(name = "Tyagaraja")
        val raga = dal.ragas.create(name = "Kalyani")
        val tala = dal.talas.create(name = "Adi")
        return ReferenceDataSeed(composer = composer, raga = raga, tala = tala)
    }

    // ── Builder functions ─────────────────────────────────────────────────

    fun buildCanonicalExtraction(
        title: String = "Nagumomu Ganaleni",
        alternateTitle: String? = null,
        composer: String = "Tyagaraja",
        ragaName: String = "Abheri",
        tala: String = "Adi",
        musicalForm: CanonicalMusicalForm = CanonicalMusicalForm.KRITHI,
        sections: List<CanonicalSectionDto> = listOf(
            CanonicalSectionDto(CanonicalSectionType.PALLAVI, 1, "Pallavi"),
            CanonicalSectionDto(CanonicalSectionType.ANUPALLAVI, 2, "Anupallavi"),
            CanonicalSectionDto(CanonicalSectionType.CHARANAM, 3, "Charanam"),
        ),
        lyricVariants: List<CanonicalLyricVariantDto> = emptyList(),
        deity: String? = null,
        temple: String? = null,
        sourceUrl: String = "https://example.com/test",
        sourceName: String = "test-source",
        sourceTier: Int = 3,
        extractionMethod: CanonicalExtractionMethod = CanonicalExtractionMethod.HTML_JSOUP,
        pageRange: String? = null,
        checksum: String? = null,
    ): CanonicalExtractionDto = CanonicalExtractionDto(
        title = title,
        alternateTitle = alternateTitle,
        composer = composer,
        musicalForm = musicalForm,
        ragas = listOf(CanonicalRagaDto(name = ragaName)),
        tala = tala,
        sections = sections,
        lyricVariants = lyricVariants,
        deity = deity,
        temple = temple,
        sourceUrl = sourceUrl,
        sourceName = sourceName,
        sourceTier = sourceTier,
        extractionMethod = extractionMethod,
        pageRange = pageRange,
        checksum = checksum,
    )

    fun buildKrithiDto(
        id: Uuid = Uuid.random(),
        title: String = "Nagumomu Ganaleni",
        titleNormalized: String = "nagumomu ganaleni",
        composerId: Uuid = Uuid.random(),
        primaryRagaId: Uuid? = Uuid.random(),
        talaId: Uuid? = Uuid.random(),
        deityId: Uuid? = null,
        musicalForm: MusicalFormDto = MusicalFormDto.KRITHI,
        primaryLanguage: LanguageCodeDto = LanguageCodeDto.TE,
        isRagamalika: Boolean = false,
        workflowState: WorkflowStateDto = WorkflowStateDto.DRAFT,
    ): KrithiDto = KrithiDto(
        id = id,
        title = title,
        titleNormalized = titleNormalized,
        composerId = composerId,
        primaryRagaId = primaryRagaId,
        talaId = talaId,
        deityId = deityId,
        musicalForm = musicalForm,
        primaryLanguage = primaryLanguage,
        isRagamalika = isRagamalika,
        workflowState = workflowState,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
    )
}
