package com.sangita.grantha.backend.testsupport

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiLyricVariantDto
import java.util.UUID
import kotlin.uuid.Uuid

object CatalogueTestFixtures {
    suspend fun createKrithi(
        dal: SangitaDal,
        title: String,
        composerId: UUID,
        ragaIds: List<UUID>,
        talaId: UUID? = null,
        workflowState: WorkflowState = WorkflowState.PUBLISHED,
        incipit: String? = null,
        isRagamalika: Boolean = ragaIds.size > 1,
        ragaSlots: List<Pair<Int, UUID>> = emptyList(),
        primaryRagaId: UUID? = ragaIds.firstOrNull(),
    ): KrithiDto = dal.krithis.create(
        KrithiCreateParams(
            title = title,
            titleNormalized = title.lowercase(),
            incipit = incipit,
            incipitNormalized = incipit?.lowercase(),
            composerId = composerId,
            musicalForm = MusicalForm.KRITHI,
            primaryLanguage = LanguageCode.TE,
            primaryRagaId = primaryRagaId,
            talaId = talaId,
            isRagamalika = isRagamalika,
            ragaIds = if (ragaSlots.isEmpty()) ragaIds else emptyList(),
            ragaSlots = ragaSlots,
            workflowState = workflowState,
        ),
    )

    suspend fun createVariant(
        dal: SangitaDal,
        krithiId: Uuid,
        lyrics: String,
        language: LanguageCode = LanguageCode.SA,
        script: ScriptCode = ScriptCode.LATIN,
        isPrimary: Boolean = false,
        label: String? = null,
    ): KrithiLyricVariantDto = dal.krithiLyrics.createLyricVariant(
        krithiId = krithiId,
        language = language,
        script = script,
        lyrics = lyrics,
        isPrimary = isPrimary,
        variantLabel = label,
    )
}
