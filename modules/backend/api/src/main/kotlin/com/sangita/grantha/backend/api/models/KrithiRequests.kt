package com.sangita.grantha.backend.api.models

import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.domain.model.WorkflowStateDto
import kotlinx.serialization.Serializable

@Serializable
data class KrithiCreateRequest(
    val title: String,
    val incipit: String? = null,
    val composerId: String,
    val musicalForm: MusicalFormDto = MusicalFormDto.KRITHI,
    val primaryLanguage: LanguageCodeDto,
    val talaId: String? = null,
    val primaryRagaId: String? = null,
    val deityId: String? = null,
    val templeId: String? = null,
    val isRagamalika: Boolean = false,
    val ragaIds: List<String> = emptyList(),
    val sahityaSummary: String? = null,
    val notes: String? = null,
)

@Serializable
data class KrithiUpdateRequest(
    val title: String? = null,
    val incipit: String? = null,
    val composerId: String? = null,
    val musicalForm: MusicalFormDto? = null,
    val primaryLanguage: LanguageCodeDto? = null,
    val talaId: String? = null,
    val primaryRagaId: String? = null,
    val deityId: String? = null,
    val templeId: String? = null,
    val isRagamalika: Boolean? = null,
    val ragaIds: List<String>? = null,
    val sahityaSummary: String? = null,
    val notes: String? = null,
    val workflowState: WorkflowStateDto? = null,
    val tagIds: List<String>? = null,
)

@Serializable
data class KrithiSectionRequest(
    val sectionType: String,
    val orderIndex: Int,
    val label: String? = null,
)

@Serializable
data class SaveKrithiSectionsRequest(
    val sections: List<KrithiSectionRequest>
)

@Serializable
data class LyricVariantCreateRequest(
    val language: LanguageCodeDto,
    val script: ScriptCodeDto,
    val transliterationScheme: String? = null,
    val sampradayaId: String? = null,
    val variantLabel: String? = null,
    val sourceReference: String? = null,
    val lyrics: String = "",
    val isPrimary: Boolean = false,
)

@Serializable
data class LyricVariantUpdateRequest(
    val language: LanguageCodeDto? = null,
    val script: ScriptCodeDto? = null,
    val transliterationScheme: String? = null,
    val sampradayaId: String? = null,
    val variantLabel: String? = null,
    val sourceReference: String? = null,
    val lyrics: String? = null,
    val isPrimary: Boolean? = null,
)

@Serializable
data class LyricVariantSectionRequest(
    val sectionId: String,
    val text: String,
)

@Serializable
data class SaveLyricVariantSectionsRequest(
    val sections: List<LyricVariantSectionRequest>
)
